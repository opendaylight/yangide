/*******************************************************************************
 * Copyright (c) 2016 AT&T, Inc.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.opendaylight.yangide.ext.model.editor;

//import static org.opendaylight.yangide.core.model.YangModelManager.search;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import javax.xml.stream.XMLStreamException;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.Path;
import org.eclipse.ui.IFileEditorInput;
import org.opendaylight.yangtools.yang.model.export.YinExportUtils;
import org.opendaylight.yangtools.yang.model.parser.api.YangSyntaxErrorException;
import org.opendaylight.yangtools.yang.model.repo.api.SchemaSourceException;
import org.opendaylight.yangtools.yang.model.repo.api.SourceIdentifier;
import org.opendaylight.yangtools.yang.model.repo.api.YangTextSchemaSource;
import org.opendaylight.yangtools.yang.parser.repo.YangTextSchemaContextResolver;

import org.opendaylight.yangide.core.YangCorePlugin;
import org.opendaylight.yangide.core.YangModelException;
import org.opendaylight.yangide.core.dom.ModuleImport;
import org.opendaylight.yangide.core.dom.SubModuleInclude;
import org.opendaylight.yangide.core.indexing.ElementIndexInfo;
import org.opendaylight.yangide.core.indexing.ElementIndexType;
import org.opendaylight.yangide.core.model.YangFile;
import org.opendaylight.yangide.core.model.YangJarEntry;
import org.opendaylight.yangide.core.model.YangModelManager;
import org.opendaylight.yangide.core.parser.IYangValidationListener;
import org.opendaylight.yangide.core.parser.YangParserUtil;
import org.opendaylight.yangide.editor.editors.YangEditor;
import com.google.common.io.ByteSource;

public class YinBuilder {
    private YangMultiPageEditorPart editor;
    private YangEditor  yangSourceEditor;
    
    public YinBuilder(YangMultiPageEditorPart editor, YangEditor yangSourceEditor) {
        this.editor             = editor;
        this.yangSourceEditor   = yangSourceEditor;
    }

    public void build(OutputStream outputStream) throws XMLStreamException, SchemaSourceException, IOException, YangSyntaxErrorException {
        YangTextSchemaContextResolver   resolver    = YangTextSchemaContextResolver.create("yangide");

        IFile file = ((IFileEditorInput) editor.getEditorInput()).getFile();
        final IntHolder errorCountHolder    = new IntHolder();
        org.opendaylight.yangide.core.dom.Module   module  =
                YangParserUtil.parseYangFile(yangSourceEditor.getDocument().get().toCharArray(),
                        file.getProject(),
                        new IYangValidationListener() {
                    @Override
                    public void validationError(String msg, int lineNumber, int charStart, int charEnd) {
                        ++ errorCountHolder.value;
                    }

                    @Override
                    public void syntaxError(String msg, int lineNumber, int charStart, int charEnd) {
                        ++ errorCountHolder.value;
                    }
                });
        
        // If module is null or there were errors, then don't continue to render the view.  It would be hard for this to happen, as the
        // YMPE doesn't run this job if the synchronizer says the source is invalid.
        if (module == null || (errorCountHolder.value > 0))
            return;

        // Now have to register the source contents for all the referenced files.  The
        // easy one is the contents of the current file.  Getting the contents of the
        // imported files requires a little more work.  The files will be indexed in the
        // IndexManager, but access to that is encapsulated in the YangModelManager.
        
        List<SourceIdentifier>  sourceIdentifiers   = collectSourceIds(module);
        
        resolver.registerSource(YangTextSchemaSource.delegateForByteSource(sourceIdentifiers.get(0),
                ByteSource.wrap(yangSourceEditor.getDocument().get().getBytes())));
        
        for (int ctr = 1; ctr < sourceIdentifiers.size(); ++ ctr) {
            String  name        = sourceIdentifiers.get(ctr).getName();
            String  revision    =  sourceIdentifiers.get(ctr).getRevision();
            ElementIndexInfo[]  infos   =
                    YangModelManager.search(null, revision, name, ElementIndexType.MODULE,
                                            file.getProject(), null);
            try {
                // Just use the first element of the array and quit.
                for (ElementIndexInfo info : infos) {
                    // Pretty much only two choices.  It's either an entry in a jar file, or
                    // a raw file.
                    if (info.getEntry() != null && info.getEntry().length() > 0) {
                        YangJarEntry    jarEntry    =
                                YangCorePlugin.createJarEntry(new Path(info.getPath()), info.getEntry());
                        resolver.registerSource(YangTextSchemaSource.delegateForByteSource(sourceIdentifiers.get(0),
                                ByteSource.wrap(new String(jarEntry.getContent()).getBytes())));
                    }
                    else {
                        YangFile    yangFile    =
                                YangCorePlugin.createYangFile(info.getPath());
                        resolver.registerSource(YangTextSchemaSource.delegateForByteSource(sourceIdentifiers.get(0),
                                ByteSource.wrap(new String(yangFile.getContents()).getBytes())));
                    }
                    break;
                }
            }
            catch (YangModelException ex) {
                YangCorePlugin.log(ex);
            }
        }

        YinExportUtils.writeModuleToOutputStream(resolver.getSchemaContext().get(), new ModuleApiProxy(module), outputStream);
    }

    private List<SourceIdentifier> collectSourceIds(org.opendaylight.yangide.core.dom.Module module) {
        List<SourceIdentifier>  sourceIdList    = new ArrayList<>();
        sourceIdList.add(new SourceIdentifier(module.getName(), module.getRevision()));
        for (ModuleImport moduleImport : module.getImports().values()) {
            sourceIdList.add(new SourceIdentifier(moduleImport.getName(), moduleImport.getRevision()));
        }
        for (SubModuleInclude subModuleInclude : module.getIncludes().values()) {
            sourceIdList.add(new SourceIdentifier(subModuleInclude.getName(), subModuleInclude.getRevision()));
        }
        return sourceIdList;
    }
    
    public static class IntHolder {
        public int value;
    }
}