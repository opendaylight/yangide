/*******************************************************************************
 * Copyright (c) 2014, 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package com.cisco.yangide.core.indexing;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.NavigableSet;
import java.util.Set;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.mapdb.DB;
import org.mapdb.DBMaker;
import org.mapdb.Fun;
import org.mapdb.Fun.Tuple3;
import org.mapdb.Fun.Tuple4;
import org.mapdb.Fun.Tuple6;

import com.cisco.yangide.core.YangCorePlugin;
import com.cisco.yangide.core.YangModelException;
import com.cisco.yangide.core.dom.ASTVisitor;
import com.cisco.yangide.core.dom.BaseReference;
import com.cisco.yangide.core.dom.GroupingDefinition;
import com.cisco.yangide.core.dom.IdentitySchemaNode;
import com.cisco.yangide.core.dom.Module;
import com.cisco.yangide.core.dom.ModuleImport;
import com.cisco.yangide.core.dom.QName;
import com.cisco.yangide.core.dom.SubModule;
import com.cisco.yangide.core.dom.SubModuleInclude;
import com.cisco.yangide.core.dom.TypeDefinition;
import com.cisco.yangide.core.dom.TypeReference;
import com.cisco.yangide.core.dom.UsesNode;
import com.cisco.yangide.core.model.YangProjectInfo;

/**
 * Provides functionality to index AST nodes and search item in index.
 *
 * @author Konstantin Zaitsev
 * date: Jun 25, 2014
 */
public class IndexManager extends JobManager {

    /**
     * Stores index version, it is required increment version on each major changes of indexing
     * algorithm or indexed data.
     */
    private static final int INDEX_VERSION = 9;

    /**
     * Index DB file path.
     */
    private static final String INDEX_PATH = "index_" + INDEX_VERSION + ".db";

    /**
     * Empty result array.
     */
    private static final ElementIndexInfo[] NO_ELEMENTS = new ElementIndexInfo[0];

    /**
     * Empty result array.
     */
    private static final ElementIndexReferenceInfo[] NO_REF_ELEMENTS = new ElementIndexReferenceInfo[0];

    /**
     * Index database.
     */
    private DB db;

    /**
     * Keywords index contains the following values:
     * <ul>
     * <li>module</li>
     * <li>revision</li>
     * <li>name</li>
     * <li>type</li>
     * <li>file path (scope, for JAR entries path to project)</li>
     * <li>ast info</li>
     * </ul>
     */
    private NavigableSet<Fun.Tuple6<String, String, String, ElementIndexType, String, ElementIndexInfo>> idxKeywords;

    /**
     * References index contains the following values:
     * <ul>
     * <li>qname</li>
     * <li>type</li>
     * <li>file path (scope, for JAR entries path to project)</li>
     * <li>ast info</li>
     * </ul>
     */
    private NavigableSet<Fun.Tuple4<QName, ElementIndexReferenceType, String, ElementIndexReferenceInfo>> idxReferences;

    /**
     * Resources index that contains relation of indexed resource and modification stamp of resource
     * when indexed was performed.
     */
    private NavigableSet<Fun.Tuple3<String, String, Long>> idxResources;

    public IndexManager() {
        File indexFile = YangCorePlugin.getDefault().getStateLocation().append(INDEX_PATH).toFile();
        try {
            initDB(indexFile, false);

            if (!idxKeywords.isEmpty() && !(idxKeywords.first() instanceof Fun.Tuple6)) {
                initDB(indexFile, true);
            }
        } catch (Throwable e) {
            initDB(indexFile, true);
        }
    }

    /**
     * Inits database by cleans old version of DB and recreate current index file if necessary.
     *
     * @param indexFile index file
     * @param cleanAll if <code>true</code> remove old version and current index also otherwise
     * remove only old version of index DB.
     */
    private void initDB(File indexFile, final boolean cleanAll) {
        // delete index db in case if index is broken and reopen with clean state
        if (this.db != null) {
            this.db.close();
        }
        File[] files = indexFile.getParentFile().listFiles((FilenameFilter) (dir, name) -> name.startsWith("index") && (cleanAll || !name.startsWith("index_" + INDEX_VERSION)));
        if (files != null) {
            for (File file : files) {
                file.delete();
            }
        }
        this.db = DBMaker.newFileDB(indexFile).closeOnJvmShutdown().make();
        this.idxKeywords = db.getTreeSet("keywords");
        this.idxReferences = db.getTreeSet("references");
        this.idxResources = db.getTreeSet("resources");
        indexAllProjects();
    }

    @Override
    public String processName() {
        return "Yang indexer";
    }

    /**
     * Indexes project only on project open event.
     *
     * @param project project to index
     */
    public void indexAll(IProject project) {
        request(new IndexAllProject(project, this));
    }

    public void addSource(IFile file) {
        // this workaround need in case of old project that has target copied yang file but this
        // files not ignored by JDT yet.
        String path = file.getProjectRelativePath().toString();
        if (path.contains("target/") || path.contains("target-ide/")) {
            return;
        }
        // in case of file not change, skip indexing
        Iterable<Long> it = Fun.filter(idxResources, file.getProject().getName(), file.getFullPath().toString());
        for (Long modStamp : it) {
            if (modStamp == file.getModificationStamp()) {
                // System.err.println("[x] " + file);
                return;
            }
        }
        request(new IndexFileRequest(file, this));
    }

    public void addWorkingCopy(IFile file) {
        request(new IndexFileRequest(file, this));
    }

    public void addJarFile(IProject project, IPath file) {
        // in case of file not change, skip indexing
        Iterable<Long> it = Fun.filter(idxResources, project.getName(), file.toString());
        for (Long modStamp : it) {
            if (modStamp == file.toFile().lastModified()) {
                // System.err.println("[x] " + file);
                return;
            }
        }
        request(new IndexJarFileRequest(project, file, this));
    }

    @Override
    public void shutdown() {
        super.shutdown();
        db.commit();
        db.compact();
        db.close();
    }

    public synchronized void removeIndexFamily(IProject project) {
        Iterator<Tuple6<String, String, String, ElementIndexType, String, ElementIndexInfo>> iterator = idxKeywords
                .iterator();
        while (iterator.hasNext()) {
            Tuple6<String, String, String, ElementIndexType, String, ElementIndexInfo> entry = iterator.next();
            if (project.getName().equals(entry.f.getProject())) {
                iterator.remove();
            }
        }

        Iterator<Tuple4<QName, ElementIndexReferenceType, String, ElementIndexReferenceInfo>> itRef = idxReferences
                .iterator();
        while (itRef.hasNext()) {
            Tuple4<QName, ElementIndexReferenceType, String, ElementIndexReferenceInfo> entry = itRef.next();
            if (project.getName().equals(entry.d.getProject())) {
                itRef.remove();
            }
        }

        Iterator<Long> it = Fun.filter(idxResources, project.getName(), null).iterator();
        while (it.hasNext()) {
            it.next();
            it.remove();
        }
    }

    public synchronized void remove(IFile file) {
        removeIndex(file.getProject(), file.getFullPath());
    }

    public synchronized void jobWasCancelled(IPath containerPath) {
    }

    public synchronized void removeIndex(IProject project, IPath containerPath) {
        Iterator<Tuple6<String, String, String, ElementIndexType, String, ElementIndexInfo>> iterator = idxKeywords
                .iterator();
        while (iterator.hasNext()) {
            Tuple6<String, String, String, ElementIndexType, String, ElementIndexInfo> entry = iterator.next();
            if (project.getName().equals(entry.f.getProject()) && containerPath.isPrefixOf(new Path(entry.e))) {
                iterator.remove();
            }
        }

        Iterator<Tuple4<QName, ElementIndexReferenceType, String, ElementIndexReferenceInfo>> itRef = idxReferences
                .iterator();
        while (itRef.hasNext()) {
            Tuple4<QName, ElementIndexReferenceType, String, ElementIndexReferenceInfo> entry = itRef.next();
            if (project.getName().equals(entry.d.getProject()) && containerPath.isPrefixOf(new Path(entry.c))) {
                itRef.remove();
            }
        }

        Iterator<Tuple3<String, String, Long>> it = idxResources.iterator();
        while (it.hasNext()) {
            Tuple3<String, String, Long> idxr = it.next();
            if (project.getName().equals(idxr.a) && containerPath.isPrefixOf(new Path(idxr.b))) {
                it.remove();
            }
        }
    }

    public synchronized void addElementIndexInfo(ElementIndexInfo info) {
        // System.err.println("[I] " + info.getModule() + "@" + info.getRevision() + " - " + info.getName() + " - " + info.getType());
        idxKeywords.add(Fun.t6(info.getModule(), info.getRevision(), info.getName(), info.getType(), info.getPath(), info));
    }

    public synchronized void addElementIndexReferenceInfo(ElementIndexReferenceInfo info) {
        // System.err.println("[IR] " + info.getReference() + " : " + info.getType() + " - " + info.getProject() + "@" + info.getPath());
        idxReferences.add(Fun.t4(info.getReference(), info.getType(), info.getPath(), info));
    }

    public void addModule(Module module, final IProject project, final IPath path, final String entry) {
        if (module != null && module.getRevision() != null && module.getRevision() != null) {
            final String revision = module.getRevision();
            final String moduleName = module.getName();
            module.accept(new ASTVisitor() {
                @Override
                public boolean visit(Module module) {
                    addElementIndexInfo(new ElementIndexInfo(module, moduleName, revision, ElementIndexType.MODULE,
                            project, path, entry));
                    return true;
                }

                @Override
                public boolean visit(SubModule module) {
                    addElementIndexInfo(new ElementIndexInfo(module, moduleName, revision, ElementIndexType.SUBMODULE,
                            project, path, entry));
                    return true;
                }

                @Override
                public boolean visit(TypeDefinition typeDefinition) {
                    addElementIndexInfo(new ElementIndexInfo(typeDefinition, moduleName, revision,
                            ElementIndexType.TYPE, project, path, entry));
                    return true;
                }

                @Override
                public boolean visit(GroupingDefinition groupingDefinition) {
                    addElementIndexInfo(new ElementIndexInfo(groupingDefinition, moduleName, revision,
                            ElementIndexType.GROUPING, project, path, entry));
                    return true;
                }

                @Override
                public boolean visit(IdentitySchemaNode identity) {
                    addElementIndexInfo(new ElementIndexInfo(identity, moduleName, revision, ElementIndexType.IDENTITY,
                            project, path, entry));
                    return true;
                }

                @Override
                public boolean visit(UsesNode uses) {
                    // index in case if not JAR
                    if (entry == null || entry.isEmpty()) {
                        addElementIndexReferenceInfo(new ElementIndexReferenceInfo(uses, uses.getGrouping(),
                                ElementIndexReferenceType.USES, project, path));
                    }
                    return true;
                }

                @Override
                public boolean visit(TypeReference ref) {
                    // index in case if not JAR
                    if (entry == null || entry.isEmpty()) {
                        addElementIndexReferenceInfo(new ElementIndexReferenceInfo(ref, ref.getType(),
                                ElementIndexReferenceType.TYPE_REF, project, path));
                    }
                    return true;
                }

                @Override
                public boolean visit(BaseReference ref) {
                    // index in case if not JAR
                    if (entry == null || entry.isEmpty()) {
                        addElementIndexReferenceInfo(new ElementIndexReferenceInfo(ref, ref.getType(),
                                ElementIndexReferenceType.IDENTITY_REF, project, path));
                    }
                    return true;
                }

                @Override
                public boolean visit(ModuleImport moduleImport) {
                    // index in case if not JAR
                    if (entry == null || entry.isEmpty()) {
                        QName qname = new QName(moduleImport.getName(), moduleImport.getPrefix(), moduleImport
                                .getName(), moduleImport.getRevision());
                        addElementIndexReferenceInfo(new ElementIndexReferenceInfo(moduleImport, qname,
                                ElementIndexReferenceType.IMPORT, project, path));
                    }
                    return true;
                }

                @Override
                public boolean visit(SubModuleInclude subModuleInclude) {
                    // index in case if not JAR
                    if (entry == null || entry.isEmpty()) {
                        QName qname = new QName(subModuleInclude.getName(), null, subModuleInclude.getName(),
                                subModuleInclude.getRevision());
                        addElementIndexReferenceInfo(new ElementIndexReferenceInfo(subModuleInclude, qname,
                                ElementIndexReferenceType.INCLUDE, project, path));
                    }
                    return true;
                }
            });
            db.commit();
        }
    }

    public synchronized ElementIndexInfo[] search(String module, String revision, String name, ElementIndexType type,
            IProject project, IPath scope) {
        ArrayList<ElementIndexInfo> infos = null;
        Set<String> projectScope = null;

        if (project != null) {
            try {
                projectScope = ((YangProjectInfo) YangCorePlugin.create(project).getElementInfo(null))
                        .getProjectScope();
            } catch (YangModelException e) {
                // ignore
            }
        }

        String  nameWithoutPrefix   = name;
        int colonIndex  = nameWithoutPrefix != null ? nameWithoutPrefix.indexOf(':') : -1;
        if (colonIndex != -1) {
            nameWithoutPrefix   = nameWithoutPrefix.substring(colonIndex + 1);
        }

        for (Tuple6<String, String, String, ElementIndexType, String, ElementIndexInfo> entry : idxKeywords) {
            if (module != null && module.length() > 0 && !module.equals(entry.a)) {
                continue;
            }

            if (revision != null && revision.length() > 0 && !revision.equals(entry.b)) {
                continue;
            }

            if (type != null && type != entry.d) {
                continue;
            }

            if (nameWithoutPrefix != null && nameWithoutPrefix.length() > 0 && !entry.c.equals(nameWithoutPrefix)) {
                continue;
            }

            if (projectScope != null && !projectScope.contains(entry.f.getProject())) {
                continue;
            }

            if (scope != null && !scope.isPrefixOf(new Path(entry.e))) {
                continue;
            }

            if (infos == null) {
                infos = new ArrayList<>();
            }
            infos.add(entry.f);
        }

        if (infos != null) {
            return infos.toArray(new ElementIndexInfo[infos.size()]);
        }
        return NO_ELEMENTS;
    }

    public synchronized ElementIndexReferenceInfo[] searchReference(QName reference, ElementIndexReferenceType type,
            IProject project) {
        ArrayList<ElementIndexReferenceInfo> infos = null;
        Set<String> indirectScope = null;

        if (project != null) {
            try {
                indirectScope = ((YangProjectInfo) YangCorePlugin.create(project).getElementInfo(null))
                        .getIndirectScope();
            } catch (YangModelException e) {
                // ignore
            }
        }

        String  nameWithoutPrefix   = reference.getName();
        int colonIndex  = nameWithoutPrefix != null ? nameWithoutPrefix.indexOf(':') : -1;
        if (colonIndex != -1) {
            nameWithoutPrefix   = nameWithoutPrefix.substring(colonIndex + 1);
        }

        for (Tuple4<QName, ElementIndexReferenceType, String, ElementIndexReferenceInfo> entry : idxReferences) {
            if (type != null && type != entry.b) {
                continue;
            }

            if (indirectScope != null && !indirectScope.contains(entry.d.getProject())) {
                continue;
            }

            if (reference.getModule() != null && !reference.getModule().equals(entry.a.getModule())) {
                continue;
            }

            if (reference.getRevision() != null && entry.a.getRevision() != null
                    && !reference.getRevision().equals(entry.a.getRevision())) {
                continue;
            }

            if (nameWithoutPrefix != null && !nameWithoutPrefix.equals(entry.a.getName())) {
                continue;
            }

            if (infos == null) {
                infos = new ArrayList<>();
            }

            if (!infos.contains(entry.d)) {
                infos.add(entry.d);
            }
        }

        if (infos != null) {
            return infos.toArray(new ElementIndexReferenceInfo[infos.size()]);
        }
        return NO_REF_ELEMENTS;
    }

    private void indexAllProjects() {
        // reindex all projects
        for (IProject project : ResourcesPlugin.getWorkspace().getRoot().getProjects()) {
            if (YangCorePlugin.isYangProject(project)) {
                indexAll(project);
            }
        }
    }

    protected void fileAddedToIndex(IProject project, IPath path, long modificationStamp) {
        idxResources.add(Fun.t3(project.getName(), path.toString(), modificationStamp));
    }
}
