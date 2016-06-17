/*******************************************************************************
 * Copyright (c) 2014, 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.opendaylight.yangide.ext.refactoring.actions;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.ltk.core.refactoring.CheckConditionsOperation;
import org.eclipse.ltk.core.refactoring.PerformRefactoringOperation;
import org.eclipse.ltk.core.refactoring.participants.RenameRefactoring;
import org.eclipse.ltk.ui.refactoring.RefactoringWizardOpenOperation;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbenchWindow;

import org.opendaylight.yangide.core.YangTypeUtil;
import org.opendaylight.yangide.core.dom.ASTNamedNode;
import org.opendaylight.yangide.core.dom.ASTNode;
import org.opendaylight.yangide.core.dom.ASTVisitor;
import org.opendaylight.yangide.core.dom.BaseReference;
import org.opendaylight.yangide.core.dom.GroupingDefinition;
import org.opendaylight.yangide.core.dom.IdentitySchemaNode;
import org.opendaylight.yangide.core.dom.Module;
import org.opendaylight.yangide.core.dom.ModuleImport;
import org.opendaylight.yangide.core.dom.QName;
import org.opendaylight.yangide.core.dom.SimpleNode;
import org.opendaylight.yangide.core.dom.SubModule;
import org.opendaylight.yangide.core.dom.SubModuleInclude;
import org.opendaylight.yangide.core.dom.TypeDefinition;
import org.opendaylight.yangide.core.dom.TypeReference;
import org.opendaylight.yangide.core.dom.UsesNode;
import org.opendaylight.yangide.ext.refactoring.YangRefactoringPlugin;
import org.opendaylight.yangide.ext.refactoring.rename.RenameGroupingProcessor;
import org.opendaylight.yangide.ext.refactoring.rename.RenameIdentityProcessor;
import org.opendaylight.yangide.ext.refactoring.rename.RenameModuleProcessor;
import org.opendaylight.yangide.ext.refactoring.rename.RenameSubModuleProcessor;
import org.opendaylight.yangide.ext.refactoring.rename.RenameTypeProcessor;
import org.opendaylight.yangide.ext.refactoring.rename.YangRenameProcessor;
import org.opendaylight.yangide.ext.refactoring.ui.RenameRefactoringWizard;

/**
 * Methods to perform Rename refactoring with dialogs or silent.
 *
 * @author Konstantin Zaitsev
 * date: Aug 4, 2014
 */
public class RenameSupport {

    private String newName;
    private ASTNamedNode node;
    private IFile file;

    /**
     * @param file file where node is declared
     * @param node node with type or group definition
     * @param newName new name of node
     */
    public RenameSupport(IFile file, ASTNamedNode node, String newName) {
        this.file = file;
        this.node = node;
        this.newName = newName;
    }

    /**
     * @param shell
     */
    public void openDialog(Shell shell) {
        openDialog(shell, false);
    }

    /**
     * @param shell
     * @param showPreview if <code>true</code> will display preview without the first page
     * @return refactoring status
     */
    public boolean openDialog(Shell shell, boolean showPreview) {

        YangRenameProcessor<?> processor = getProcessor(shell);
        if (processor == null) {
            return false;
        }
        RenameRefactoring refactoring = new RenameRefactoring(processor);
        processor.setNewName(newName);
        processor.setUpdateReferences(true);
        processor.setFile(file);
        RenameRefactoringWizard wizard = null;
        if (showPreview) {
            wizard = new RenameRefactoringWizard(refactoring) {
                @Override
                protected void addUserInputPages() {
                }
            };
            wizard.setForcePreviewReview(showPreview);
        } else {
            wizard = new RenameRefactoringWizard(refactoring);
        }
        RefactoringWizardOpenOperation op = new RefactoringWizardOpenOperation(wizard);
        try {
            return op.run(shell, "Rename") == IDialogConstants.OK_ID;
        } catch (InterruptedException e) {
            // do nothing
        }
        return false;
    }

    /**
     * Performs rename refactoring.
     *
     * @param shell
     * @param window
     */
    public void perform(Shell shell, IWorkbenchWindow window) {
        YangRenameProcessor<?> processor = getProcessor(shell);
        if (processor != null) {
            RenameRefactoring refactoring = new RenameRefactoring(processor);
            processor.setNewName(newName);
            processor.setUpdateReferences(true);
            processor.setFile(file);
            PerformRefactoringOperation op = new PerformRefactoringOperation(refactoring,
                    CheckConditionsOperation.ALL_CONDITIONS);
            try {
                op.run(null);
            } catch (CoreException e) {
                YangRefactoringPlugin.log(e);
            }
        }
    }

    /**
     * @param shell shell
     * @return create appropriate refactor processor by node type
     */
    private YangRenameProcessor<?> getProcessor(Shell shell) {
        YangRenameProcessor<?> processor = null;

        if (node instanceof GroupingDefinition) {
            processor = new RenameGroupingProcessor((GroupingDefinition) node);
        } else if (node instanceof TypeDefinition) {
            processor = new RenameTypeProcessor((TypeDefinition) node);
        } else if (node instanceof IdentitySchemaNode) {
            processor = new RenameIdentityProcessor((IdentitySchemaNode) node);
        } else if (node instanceof SubModule) {
            processor = new RenameSubModuleProcessor((SubModule) node);
        } else if (node instanceof Module) {
            processor = new RenameModuleProcessor((Module) node);
        }

        return processor;
    }

    /**
     * @param node node to inspect
     * @return <code>true</code> if node available to rename
     */
    public static boolean isDirectRename(ASTNode node) {
        return node instanceof GroupingDefinition || node instanceof TypeDefinition
                || node instanceof IdentitySchemaNode || node instanceof Module || node instanceof SubModule;
    }

    /**
     * @param node node to inspect
     * @return <code>true</code> if node is reference to perform indirect renaming
     */
    public static boolean isIndirectRename(ASTNode node) {
        return node instanceof UsesNode
                || (node instanceof TypeReference && !YangTypeUtil.isBuiltInType(((TypeReference) node).getName()))
                || node instanceof BaseReference || node instanceof ModuleImport || node instanceof SubModuleInclude;
    }

    private static String nameWithoutPrefix(String name) {
        return ((name != null && name.indexOf(':') != -1) ? name.substring(name.indexOf(':') + 1) : name);
    }
    
    private static boolean prefixMatchesModulePrefix(QName component, SimpleNode<String> modulePrefix) {
        return (component.getPrefix() != null && modulePrefix != null && component.getPrefix().equals(modulePrefix.getValue()));
    }
    
    /**
     * @param module module to find
     * @param node original node with definition
     * @return arrays of referenced node with original node
     */
    public static ASTNamedNode[] findLocalReferences(Module module, final ASTNamedNode node) {
        final List<ASTNamedNode> nodes = new ArrayList<>();
        final String name = node.getName();
//        final String nameWithoutPrefix  =
//                ((name != null && name.indexOf(':') != -1) ? name.substring(name.indexOf(':') + 1) : name);
        final SimpleNode<String> modulePrefix = module.getPrefix();
        module.accept(new ASTVisitor() {
            @Override
            public void preVisit(ASTNode n) {
                if (n instanceof ASTNamedNode) {
                    ASTNamedNode nn = (ASTNamedNode) n;
                    if ((nn instanceof TypeDefinition || nn instanceof GroupingDefinition || nn instanceof Module
                            || nn instanceof SubModule || nn instanceof IdentitySchemaNode)
                            && nn.getName().equals(name)) {
                        nodes.add(nn);
                    } else if (nn instanceof TypeReference && nameWithoutPrefix(((TypeReference) nn).getType().getName()).equals(nameWithoutPrefix(name))) {
                        if (prefixMatchesModulePrefix(((TypeReference) nn).getType(), modulePrefix)) {
                            nodes.add(nn);
                        }
                    } else if (nn instanceof UsesNode && nameWithoutPrefix(((UsesNode) nn).getGrouping().getName()).equals(nameWithoutPrefix(name))) {
                        // We add the node if the prefix on the UsesNode matches the module prefix,
                        // either implicitly or explicitly.
                        if (prefixMatchesModulePrefix(((UsesNode) nn).getGrouping(), modulePrefix)) {
//                        if (((UsesNode) nn).getGrouping().getPrefix() != null && modulePrefix != null
//                                && ((UsesNode) nn).getGrouping().getPrefix().equals(modulePrefix.getValue())) {
                            nodes.add(nn);
                        }
                    } else if (nn instanceof BaseReference && ((BaseReference) nn).getType().getName().equals(name)) {
                        nodes.add(nn);
                    } else if (nn instanceof ModuleImport && ((ModuleImport) nn).getName().equals(name)) {
                        nodes.add(nn);
                    } else if (nn instanceof SubModuleInclude && ((SubModuleInclude) nn).getName().equals(name)) {
                        nodes.add(nn);
                    }
                }
            }
        });
        return nodes.toArray(new ASTNamedNode[nodes.size()]);
    }
}
