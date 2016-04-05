/*******************************************************************************
 * Copyright (c) 2016 AT&T, Inc.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package com.cisco.yangide.ext.model.editor;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import javax.xml.stream.XMLStreamException;

import org.eclipse.core.resources.IFile;
import org.eclipse.ui.IFileEditorInput;
import org.opendaylight.yangtools.yang.model.export.YinExportUtils;
import org.opendaylight.yangtools.yang.model.parser.api.YangSyntaxErrorException;
import org.opendaylight.yangtools.yang.model.repo.api.SchemaSourceException;
import org.opendaylight.yangtools.yang.model.repo.api.SourceIdentifier;
import org.opendaylight.yangtools.yang.model.repo.api.YangTextSchemaSource;
import org.opendaylight.yangtools.yang.parser.repo.YangTextSchemaContextResolver;

import com.cisco.yangide.core.dom.ModuleImport;
import com.cisco.yangide.core.dom.SubModuleInclude;
import com.cisco.yangide.core.parser.IYangValidationListener;
import com.cisco.yangide.core.parser.YangParserUtil;
import com.cisco.yangide.editor.editors.YangEditor;
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
        com.cisco.yangide.core.dom.Module   module  =
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
        
        List<SourceIdentifier>  sourceIdentifiers   = collectSourceIds(module);
        if (sourceIdentifiers.size() == 1) {
            resolver.registerSource(YangTextSchemaSource.delegateForByteSource(sourceIdentifiers.get(0),
                    ByteSource.wrap(yangSourceEditor.getDocument().get().getBytes())));
        }
        else {
            for (SourceIdentifier id : sourceIdentifiers) {
                //System.out.println("id[" + id + "]");
                // delegate will be a ByteArrayByteSource.
                //                YangTextSchemaSource    source  = YangTextSchemaSource.delegateForByteSource(id, delegate);
                //                resolver.registerSource(source);
            }
        }
        YinExportUtils.writeModuleToOutputStream(resolver.getSchemaContext().get(), new ModuleApiProxy(module), outputStream);
    }

    private List<SourceIdentifier> collectSourceIds(com.cisco.yangide.core.dom.Module module) {
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