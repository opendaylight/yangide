/*******************************************************************************
 * Copyright (c) 2014, 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.opendaylight.yangide.editor.actions;

import java.util.ResourceBundle;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.part.FileEditorInput;
import org.eclipse.ui.texteditor.ITextEditor;
import org.eclipse.ui.texteditor.TextEditorAction;

import org.opendaylight.yangide.core.dom.ASTNode;
import org.opendaylight.yangide.core.dom.BaseReference;
import org.opendaylight.yangide.core.dom.Module;
import org.opendaylight.yangide.core.dom.ModuleImport;
import org.opendaylight.yangide.core.dom.QName;
import org.opendaylight.yangide.core.dom.TypeReference;
import org.opendaylight.yangide.core.dom.UsesNode;
import org.opendaylight.yangide.core.indexing.ElementIndexInfo;
import org.opendaylight.yangide.core.indexing.ElementIndexType;
import org.opendaylight.yangide.core.model.YangModelManager;
import org.opendaylight.yangide.core.parser.YangParserUtil;
import org.opendaylight.yangide.editor.EditorUtility;
import org.opendaylight.yangide.editor.editors.YangEditor;
import org.opendaylight.yangide.ui.YangUIPlugin;

/**
 * Open type declaration.
 *
 * @author Konstantin Zaitsev
 * date: Jul 4, 2014
 */
public class OpenDeclarationAction extends TextEditorAction {

    public OpenDeclarationAction(ResourceBundle bundle, String prefix, ITextEditor editor) {
        super(bundle, prefix, editor);
    }

    @Override
    public void run() {
        YangEditor editor = (YangEditor) getTextEditor();

        try {
            ISelection selection = editor.getSelectionProvider().getSelection();
            Module module = YangParserUtil.parseYangFile(editor.getDocument().get().toCharArray());
            ASTNode node = module.getNodeAtPosition(((ITextSelection) selection).getOffset());
            IEditorInput    editorInput = editor.getEditorInput();

            // Determine the current project so definitions from the current project or dependent projects, are preferred.
            IProject    project = null;
            if (editorInput instanceof FileEditorInput) {
                if (((FileEditorInput) editorInput).getFile() != null)
                    project = ((FileEditorInput) editorInput).getFile().getProject();
            }
            else {
                YangUIPlugin.log(IStatus.WARNING,
                                 "Could not determine project, because editorInput not FileEditorInput, but \"" + editorInput.getClass().getName() + "\".");
            }
            
            ElementIndexInfo[] searchResult = null;

            if (node instanceof ModuleImport) {
                ModuleImport importNode = (ModuleImport) node;
                searchResult = YangModelManager.search(null, importNode.getRevision(), importNode.getName(),
                        ElementIndexType.MODULE, project, null);
            } else if (node instanceof TypeReference) {
                TypeReference ref = (TypeReference) node;
                QName type = ref.getType();
                searchResult = YangModelManager.search(type.getModule(), type.getRevision(), type.getName(),
                        ElementIndexType.TYPE, project, null);
                if (searchResult.length == 0) {
                    searchResult = YangModelManager.search(type.getModule(), type.getRevision(), type.getName(),
                            ElementIndexType.IDENTITY, project, null);
                }
            } else if (node instanceof UsesNode) {
                UsesNode usesNode = (UsesNode) node;
                QName ref = usesNode.getGrouping();
                searchResult = YangModelManager.search(ref.getModule(), ref.getRevision(), ref.getName(),
                        ElementIndexType.GROUPING, project, null);
            } else if (node instanceof BaseReference) {
                BaseReference base = (BaseReference) node;
                QName ref = base.getType();
                searchResult = YangModelManager.search(ref.getModule(), ref.getRevision(), ref.getName(),
                        ElementIndexType.IDENTITY, project, null);
            }

            if (searchResult != null && searchResult.length > 0) {
                EditorUtility.openInEditor(searchResult[0]);
            }
        } catch (Exception e) {
            YangUIPlugin.log(e);
        }
    }
}
