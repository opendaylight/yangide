/*******************************************************************************
 * Copyright (c) 2016 AT&T, Inc.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package com.cisco.yangide.ext.model.editor;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.common.QNameModule;
import org.opendaylight.yangtools.yang.model.api.AugmentationSchema;
import org.opendaylight.yangtools.yang.model.api.ConstraintDefinition;
import org.opendaylight.yangtools.yang.model.api.ContainerSchemaNode;
import org.opendaylight.yangtools.yang.model.api.DataSchemaNode;
import org.opendaylight.yangtools.yang.model.api.Deviation;
import org.opendaylight.yangtools.yang.model.api.ExtensionDefinition;
import org.opendaylight.yangtools.yang.model.api.FeatureDefinition;
import org.opendaylight.yangtools.yang.model.api.GroupingDefinition;
import org.opendaylight.yangtools.yang.model.api.IdentitySchemaNode;
import org.opendaylight.yangtools.yang.model.api.MustDefinition;
import org.opendaylight.yangtools.yang.model.api.NotificationDefinition;
import org.opendaylight.yangtools.yang.model.api.RevisionAwareXPath;
import org.opendaylight.yangtools.yang.model.api.RpcDefinition;
import org.opendaylight.yangtools.yang.model.api.SchemaPath;
import org.opendaylight.yangtools.yang.model.api.TypeDefinition;
import org.opendaylight.yangtools.yang.model.api.UnknownSchemaNode;
import org.opendaylight.yangtools.yang.model.api.UsesNode;
import org.opendaylight.yangtools.yang.model.util.ModuleImportImpl;

import com.cisco.yangide.core.YangCorePlugin;
import com.cisco.yangide.core.dom.ASTCompositeNode;
import com.cisco.yangide.core.dom.ASTNode;
import com.cisco.yangide.core.dom.ContrainerSchemaNode;
import com.cisco.yangide.core.dom.LeafListSchemaNode;
import com.cisco.yangide.core.dom.LeafSchemaNode;
import com.cisco.yangide.core.dom.ListSchemaNode;
import com.cisco.yangide.core.dom.RevisionNode;
import com.cisco.yangide.core.dom.SimpleNode;

public class ModuleApiProxy implements org.opendaylight.yangtools.yang.model.api.Module {
    private com.cisco.yangide.core.dom.Module module;
    
    public static final String NODE_NAME_PRESENCE   = "presence";
    public static final String NODE_NAME_CONFIG     = "config";
    
    public ModuleApiProxy(com.cisco.yangide.core.dom.Module module) {
        this.module = module;
    }

    private static Date revisionStringToDate(String revision) {
        Date    result  = null;
        SimpleDateFormat    sdf = new SimpleDateFormat("yyyy-mm-dd");
        try {
            result  = sdf.parse(revision);
        }
        catch (ParseException ex) {
            YangCorePlugin.log(ex);
        }
        return result;
    }

    private static boolean verifyPresenceOfNamedNode(ASTCompositeNode parentNode, String value) {
        boolean foundNode   = false;
        for (ASTNode node : parentNode.getChildren()) {
            if (node instanceof SimpleNode) {
                if (value.equals(((SimpleNode) node).getNodeName())) {
                    foundNode   = true;
                }
            }
        }
        
        return foundNode;
    }

    @Override
    public Collection<DataSchemaNode> getChildNodes() {
        Collection<DataSchemaNode>  childNodes  = new ArrayList<>();
        for (ASTNode node : module.getChildren()) {
            if (node instanceof RevisionNode) {
                RevisionNode    revisionNode    = (RevisionNode) node;
            }
            else if (node instanceof ContrainerSchemaNode) {
                ContrainerSchemaNode    containerNode   = (ContrainerSchemaNode) node;
                // Create ContainerSchemaNode.
                ContainerSchemaNode containerSchemaNode  =
                        constructContainerSchemaNode(getQNameModule(), containerNode);
                childNodes.add(containerSchemaNode);
            }
            else {
                //System.out.println("Actual type is not handled: " + node.getClass().getName());
            }
        }
        return childNodes;
    }

    @Override
    public DataSchemaNode getDataChildByName(QName name) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public DataSchemaNode getDataChildByName(String name) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Set<GroupingDefinition> getGroupings() {
        Set<GroupingDefinition> groupings   = new HashSet<>();
        // TODO populate this.
        return groupings;
    }

    @Override
    public Set<TypeDefinition<?>> getTypeDefinitions() {
        Set<TypeDefinition<?>>  typeDefinitions = new HashSet<>();
        // TODO populate this.
        return typeDefinitions;
    }

    @Override
    public Set<UsesNode> getUses() {
        Set<UsesNode>   uses    = new HashSet<>();
        // TODO populate this.
        return uses;
    }

    @Override
    public String getModuleSourcePath() { return module.getSourcePath().getValue(); }

    @Override
    public String getName() { return module.getName(); }

    @Override
    public java.net.URI getNamespace() { return java.net.URI.create(module.getNamespace()); }

    @Override
    public QNameModule getQNameModule() {
        return QNameModule.create(java.net.URI.create(module.getNamespace()),
                                  revisionStringToDate(module.getRevision()));
    }

    @Override
    public Date getRevision() {
        return revisionStringToDate(module.getRevision());
    }

    @Override
    public Set<AugmentationSchema> getAugmentations() {
        Set<AugmentationSchema> augmentations   = new HashSet<>();
        // TODO populate this.
        return augmentations;
    }

    @Override
    public String getContact() {
        // This is optional, so allow for null.
        if (module.getContact() != null) {
            return module.getContact().getValue();
        }
        else {
            return null;
        }
    }

    @Override
    public String getDescription() { return module.getDescription(); }

    @Override
    public Set<Deviation> getDeviations() {
        Set<Deviation>  deviations  = new HashSet<>();
        // TODO populate this.
        return deviations;
    }

    @Override
    public List<ExtensionDefinition> getExtensionSchemaNodes() {
        List<ExtensionDefinition>   extensionSchemaNodes    = new ArrayList<>();
        // TODO Populate this.
        return extensionSchemaNodes;
    }

    @Override
    public Set<FeatureDefinition> getFeatures() {
        Set<FeatureDefinition>  features    = new HashSet<>();
        // TODO populate this.
        return features;
    }

    @Override
    public Set<IdentitySchemaNode> getIdentities() {
        Set<IdentitySchemaNode> identities  = new HashSet<>();
        // TODO populate this.
        return identities;
    }

    @Override
    public Set<org.opendaylight.yangtools.yang.model.api.ModuleImport> getImports() {
        Set<org.opendaylight.yangtools.yang.model.api.ModuleImport> imports = new HashSet<>();
        for (Map.Entry<String, com.cisco.yangide.core.dom.ModuleImport> entry : module.getImports().entrySet()) {
            imports.add(new ModuleImportImpl(entry.getValue().getName(),
                                             revisionStringToDate(entry.getValue().getRevision()),
                                             entry.getValue().getPrefix()));
        }
        return imports;
    }

    @Override
    public Set<NotificationDefinition> getNotifications() {
        Set<NotificationDefinition> notifications   = new HashSet<>();
        // TODO populate this.
        return notifications;
    }

    @Override
    public String getOrganization() {
        // This is optional, so allow for null.
        if (module.getOrganization() != null) {
            return module.getOrganization().getValue();
        }
        else {
            return null;
        }
    }

    @Override
    public String getPrefix() { return module.getPrefix().getValue(); }

    @Override
    public String getReference() { return module.getReference(); }

    @Override
    public Set<RpcDefinition> getRpcs() {
        Set<RpcDefinition>  rpcs    = new HashSet<>();
        // TODO populate this.
        return rpcs;
    }

    @Override
    public String getSource() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Set<org.opendaylight.yangtools.yang.model.api.Module> getSubmodules() {
        Set<org.opendaylight.yangtools.yang.model.api.Module>   subModules  = new HashSet<>();
        // TODO populate this.
        return subModules;
    }

    @Override
    public List<UnknownSchemaNode> getUnknownSchemaNodes() {
        List<UnknownSchemaNode> unknownSchemaNodes  = new ArrayList<>();
        // TODO populate this.
        return unknownSchemaNodes;
    }

    @Override
    public String getYangVersion() {
        // The RFC says that "yang-version" is OPTIONAL, but the yangtools machinery expects a parsed module to contain
        // the default value of "1" if it doesn't find it.
        if (module.getYangVersion() != null)
            return module.getYangVersion().getValue();
        else
            return "1";
    }
    
    private org.opendaylight.yangtools.yang.model.api.Status    constructStatus(String status) {
        if (status == null)
            return null;
        switch (status) {
        case    "CURRENT":      return org.opendaylight.yangtools.yang.model.api.Status.CURRENT;
        case    "DEPRECATED":   return org.opendaylight.yangtools.yang.model.api.Status.DEPRECATED;
        case    "OBSOLETE":     return org.opendaylight.yangtools.yang.model.api.Status.OBSOLETE;
        default:
            return null;
        }
    }
    
    private ContainerSchemaNode constructContainerSchemaNode(final QNameModule qnameModule,
                                                             final ContrainerSchemaNode containerNode) {
        return new ContainerSchemaNode() {
            @Override
            public org.opendaylight.yangtools.yang.model.api.Status getStatus() {
                return constructStatus(containerNode.getStatus());
            }
            
            @Override
            public String getReference() { return containerNode.getReference(); }
            
            @Override
            public String getDescription() { return containerNode.getDescription(); }
            
            @Override
            public List<UnknownSchemaNode> getUnknownSchemaNodes() {
                List<UnknownSchemaNode> unknownSchemaNodes  = new ArrayList<>();
                // TODO populate this.
                return unknownSchemaNodes;
            }
            
            @Override
            public QName getQName() { return QName.create(qnameModule, containerNode.getName()); }
            
            @Override
            public SchemaPath getPath() {
                SchemaPath  schemaPath  = SchemaPath.create(true, getQName());
                // TODO fix this.
                return schemaPath;
            }
            
            @Override
            public boolean isConfiguration() {
                return verifyPresenceOfNamedNode(containerNode, NODE_NAME_CONFIG);
            }
            
            @Override
            public boolean isAugmenting() {
                // TODO Auto-generated method stub
                return false;
            }
            
            @Override
            public boolean isAddedByUses() {
                // TODO Auto-generated method stub
                return false;
            }
            
            @Override
            public ConstraintDefinition getConstraints() {
                return constructConstraints(qnameModule, containerNode);
            }
            
            @Override
            public Set<AugmentationSchema> getAvailableAugmentations() {
                Set<AugmentationSchema> availableAugmentations  = new HashSet<>();
                // TODO populate this.
                return availableAugmentations;
            }
            
            @Override
            public Set<UsesNode> getUses() {
                Set<UsesNode>   uses    = new HashSet<>();
                // TODO populate this.
                return uses;
            }
            
            @Override
            public Set<TypeDefinition<?>> getTypeDefinitions() {
                Set<TypeDefinition<?>>  typeDefinitions = new HashSet<>();
                // TODO populate this.
                return typeDefinitions;
            }
            
            @Override
            public Set<GroupingDefinition> getGroupings() {
                Set<GroupingDefinition> groupings   = new HashSet<>();
                // TODO populate this.
                return groupings;
            }
            
            @Override
            public DataSchemaNode getDataChildByName(String paramString) {
                // TODO Auto-generated method stub
                return null;
            }
            
            @Override
            public DataSchemaNode getDataChildByName(QName paramQName) {
                // TODO Auto-generated method stub
                return null;
            }
            
            @Override
            public Collection<DataSchemaNode> getChildNodes() {
                Collection<DataSchemaNode>  childNodes  = new ArrayList<>();
                for (ASTNode node : containerNode.getChildren()) {
                    if (node instanceof ContrainerSchemaNode) {
                        ContrainerSchemaNode    containerNode       = (ContrainerSchemaNode) node;
                        ContainerSchemaNode     containerSchemaNode =
                                constructContainerSchemaNode(qnameModule, containerNode);
                        childNodes.add(containerSchemaNode);
                    }
                    else if (node instanceof LeafSchemaNode) {
                        LeafSchemaNode  leafNode    = (LeafSchemaNode) node;
                        org.opendaylight.yangtools.yang.model.api.LeafSchemaNode    leafSchemaNode  =
                                constructLeafSchemaNode(qnameModule, leafNode);
                        childNodes.add(leafSchemaNode);
                    }
                    else if (node instanceof LeafListSchemaNode) {
                        LeafListSchemaNode  leafListNode    = (LeafListSchemaNode) node;
                        org.opendaylight.yangtools.yang.model.api.LeafListSchemaNode    leafListSchemaNode  =
                                constructLeafListSchemaNode(qnameModule, leafListNode);
                        childNodes.add(leafListSchemaNode);
                    }
                    else if (node instanceof ListSchemaNode) {
                        ListSchemaNode  listNode    = (ListSchemaNode) node;
                        org.opendaylight.yangtools.yang.model.api.ListSchemaNode    listSchemaNode  =
                                constructListSchemaNode(qnameModule, listNode);
                        childNodes.add(listSchemaNode);
                    }
                    else {
                        //System.out.println("Unexpected node type of \"" + node.getClass().getName() + "\".");
                    }
                    
                }
                return childNodes;
            }
            
            @Override
            public boolean isPresenceContainer() {
                return verifyPresenceOfNamedNode(containerNode, NODE_NAME_PRESENCE);
            }
        };
    }
    
    private ConstraintDefinition constructConstraints(QNameModule qnameModule, ContrainerSchemaNode containerNode) {
        return new ConstraintDefinition() {
            @Override
            public boolean isMandatory() {
                // TODO Auto-generated method stub
                return false;
            }
            
            @Override
            public RevisionAwareXPath getWhenCondition() {
                // TODO Auto-generated method stub
                return null;
            }
            
            @Override
            public Set<MustDefinition> getMustConstraints() {
                Set<MustDefinition> mustConstraints = new HashSet<>();
                // TODO populate this.
                return mustConstraints;
            }
            
            @Override
            public Integer getMinElements() {
                // TODO Auto-generated method stub
                return null;
            }
            
            @Override
            public Integer getMaxElements() {
                // TODO Auto-generated method stub
                return null;
            }
        };
    }

    private ConstraintDefinition constructConstraints(QNameModule qnameModule, LeafSchemaNode leafNode) {
        return new ConstraintDefinition() {
            @Override
            public boolean isMandatory() {
                // TODO Auto-generated method stub
                return false;
            }
            
            @Override
            public RevisionAwareXPath getWhenCondition() {
                // TODO Auto-generated method stub
                return null;
            }
            
            @Override
            public Set<MustDefinition> getMustConstraints() {
                Set<MustDefinition> mustConstraints = new HashSet<>();
                return mustConstraints;
            }
            
            @Override
            public Integer getMinElements() {
                // TODO Auto-generated method stub
                return null;
            }
            
            @Override
            public Integer getMaxElements() {
                // TODO Auto-generated method stub
                return null;
            }
        };
    }

    private ConstraintDefinition constructConstraints(QNameModule qnameModule, LeafListSchemaNode leafListNode) {
        return new ConstraintDefinition() {
            @Override
            public boolean isMandatory() {
                // TODO Auto-generated method stub
                return false;
            }
            
            @Override
            public RevisionAwareXPath getWhenCondition() {
                // TODO Auto-generated method stub
                return null;
            }
            
            @Override
            public Set<MustDefinition> getMustConstraints() {
                Set<MustDefinition> mustConstraints = new HashSet<>();
                return mustConstraints;
            }
            
            @Override
            public Integer getMinElements() {
                // TODO Auto-generated method stub
                return null;
            }
            
            @Override
            public Integer getMaxElements() {
                // TODO Auto-generated method stub
                return null;
            }
        };
    }

    private ConstraintDefinition constructConstraints(QNameModule qnameModule, ListSchemaNode listNode) {
        return new ConstraintDefinition() {
            @Override
            public boolean isMandatory() {
                // TODO Auto-generated method stub
                return false;
            }
            
            @Override
            public RevisionAwareXPath getWhenCondition() {
                // TODO Auto-generated method stub
                return null;
            }
            
            @Override
            public Set<MustDefinition> getMustConstraints() {
                Set<MustDefinition> mustConstraints = new HashSet<>();
                return mustConstraints;
            }
            
            @Override
            public Integer getMinElements() {
                // TODO Auto-generated method stub
                return null;
            }
            
            @Override
            public Integer getMaxElements() {
                // TODO Auto-generated method stub
                return null;
            }
        };
    }

    private org.opendaylight.yangtools.yang.model.api.LeafSchemaNode    constructLeafSchemaNode(final QNameModule qnameModule, final LeafSchemaNode leafNode) {
        return new org.opendaylight.yangtools.yang.model.api.LeafSchemaNode() {
            @Override
            public org.opendaylight.yangtools.yang.model.api.Status getStatus() {
                return constructStatus(leafNode.getStatus());
            }
            
            @Override
            public String getReference() { return leafNode.getReference(); }
            
            @Override
            public String getDescription() { return leafNode.getDescription(); }
            
            @Override
            public List<UnknownSchemaNode> getUnknownSchemaNodes() {
                List<UnknownSchemaNode> unknownSchemaNodes  = new ArrayList<>();
                // TODO populate this.
                return unknownSchemaNodes;
           }
            
            @Override
            public QName getQName() { return QName.create(qnameModule, leafNode.getName()); }
            
            @Override
            public SchemaPath getPath() {
                SchemaPath  schemaPath  = SchemaPath.create(true, getQName());
                // TODO fix this.
                return schemaPath;
            }
            
            @Override
            public boolean isConfiguration() {
                return verifyPresenceOfNamedNode(leafNode, NODE_NAME_CONFIG);
            }
            
            @Override
            public boolean isAugmenting() {
                // TODO Auto-generated method stub
                return false;
            }
            
            @Override
            public boolean isAddedByUses() {
                // TODO Auto-generated method stub
                return false;
            }
            
            @Override
            public ConstraintDefinition getConstraints() { return constructConstraints(qnameModule, leafNode); }
            
            @Override
            public String getUnits() {
                // TODO Auto-generated method stub
                return null;
            }
            
            @Override
            public TypeDefinition<?> getType() {
                for (ASTNode node : leafNode.getChildren()) {
                    if (node instanceof com.cisco.yangide.core.dom.TypeReference) {
                        com.cisco.yangide.core.dom.TypeReference    typeReferenceNode   = (com.cisco.yangide.core.dom.TypeReference) node;
                        return constructTypeDefinitionSchemaNode(qnameModule, typeReferenceNode);
                    }
                }
                // TODO fix this.
                return null;
            }

            @Override
            public String getDefault() {
                // TODO Auto-generated method stub
                return null;
            }
        };
    }

    private org.opendaylight.yangtools.yang.model.api.LeafListSchemaNode    constructLeafListSchemaNode(final QNameModule qnameModule, final LeafListSchemaNode leafListNode) {
        return new org.opendaylight.yangtools.yang.model.api.LeafListSchemaNode() {
            @Override
            public org.opendaylight.yangtools.yang.model.api.Status getStatus() {
                return constructStatus(leafListNode.getStatus());
            }
            
            @Override
            public String getReference() { return leafListNode.getReference(); }
            
            @Override
            public String getDescription() { return leafListNode.getDescription(); }
            
            @Override
            public List<UnknownSchemaNode> getUnknownSchemaNodes() {
                List<UnknownSchemaNode> unknownSchemaNodes  = new ArrayList<>();
                // TODO populate this.
                return unknownSchemaNodes;
           }
            
            @Override
            public QName getQName() { return QName.create(qnameModule, leafListNode.getName()); }
            
            @Override
            public SchemaPath getPath() {
                SchemaPath  schemaPath  = SchemaPath.create(true, getQName());
                // TODO fix this.
                return schemaPath;
            }
            
            @Override
            public boolean isConfiguration() {
                return verifyPresenceOfNamedNode(leafListNode, NODE_NAME_CONFIG);
            }
            
            @Override
            public boolean isAugmenting() {
                // TODO Auto-generated method stub
                return false;
            }
            
            @Override
            public boolean isAddedByUses() {
                // TODO Auto-generated method stub
                return false;
            }
            
            @Override
            public ConstraintDefinition getConstraints() { return constructConstraints(qnameModule, leafListNode); }
            
            @Override
            public TypeDefinition<?> getType() {
                for (ASTNode node : leafListNode.getChildren()) {
                    if (node instanceof com.cisco.yangide.core.dom.TypeReference) {
                        com.cisco.yangide.core.dom.TypeReference    typeReferenceNode   = (com.cisco.yangide.core.dom.TypeReference) node;
                        return constructTypeDefinitionSchemaNode(qnameModule, typeReferenceNode);
                    }
                }
                // TODO fix this.
                return null;
            }

            @Override
            public boolean isUserOrdered() {
                // TODO Auto-generated method stub
                return false;
            }
        };
    }

    private org.opendaylight.yangtools.yang.model.api.ListSchemaNode    constructListSchemaNode(final QNameModule qnameModule,
                                                                                                final ListSchemaNode listNode) {
        return new org.opendaylight.yangtools.yang.model.api.ListSchemaNode() {
            @Override
            public org.opendaylight.yangtools.yang.model.api.Status getStatus() {
                return constructStatus(listNode.getStatus());
            }
            
            @Override
            public String getReference() { return listNode.getReference(); }
            
            @Override
            public String getDescription() { return listNode.getDescription(); }
            
            @Override
            public List<UnknownSchemaNode> getUnknownSchemaNodes() {
                List<UnknownSchemaNode> unknownSchemaNodes  = new ArrayList<>();
                // TODO populate this.
                return unknownSchemaNodes;
           }
            
            @Override
            public QName getQName() { return QName.create(qnameModule, listNode.getName()); }
            
            @Override
            public SchemaPath getPath() {
                SchemaPath  schemaPath  = SchemaPath.create(true, getQName());
                // TODO fix this.
                return schemaPath;
            }
            
            @Override
            public boolean isConfiguration() {
                return verifyPresenceOfNamedNode(listNode, NODE_NAME_CONFIG);
            }
            
            @Override
            public boolean isAugmenting() {
                // TODO Auto-generated method stub
                return false;
            }
            
            @Override
            public boolean isAddedByUses() {
                // TODO Auto-generated method stub
                return false;
            }
            
            @Override
            public ConstraintDefinition getConstraints() { return constructConstraints(qnameModule, listNode); }
            
            @Override
            public boolean isUserOrdered() {
                // TODO Auto-generated method stub
                return false;
            }

            @Override
            public Set<TypeDefinition<?>> getTypeDefinitions() {
                Set<TypeDefinition<?>>  typeDefinitions = new HashSet<>();
                // TODO populate this.
                return typeDefinitions;
            }

            @Override
            public Collection<DataSchemaNode> getChildNodes() {
                Collection<DataSchemaNode>  childNodes  = new ArrayList<>();
                for (ASTNode node : listNode.getChildren()) {
                    //System.out.println("node.type[" + node.getClass().getName() + "]");
                    if (node instanceof ContrainerSchemaNode) {
                        ContrainerSchemaNode    containerNode   = (ContrainerSchemaNode) node;
                        ContainerSchemaNode containerSchemaNode  =
                                constructContainerSchemaNode(getQNameModule(), containerNode);
                        childNodes.add(containerSchemaNode);
                    }
                    else if (node instanceof LeafSchemaNode) {
                        LeafSchemaNode  leafNode    = (LeafSchemaNode) node;
                        org.opendaylight.yangtools.yang.model.api.LeafSchemaNode leafSchemaNode =
                                constructLeafSchemaNode(getQNameModule(), leafNode);
                        childNodes.add(leafSchemaNode);
                    }
                    else {
                        //System.out.printlns("Actual type is not handled: " + node.getClass().getName());
                    }
                }
                return childNodes;
            }

            @Override
            public Set<GroupingDefinition> getGroupings() {
                Set<GroupingDefinition> groupings   = new HashSet<>();
                // TODO populate this.
                return groupings;
            }

            @Override
            public DataSchemaNode getDataChildByName(QName paramQName) {
                // TODO Auto-generated method stub
                return null;
            }

            @Override
            public DataSchemaNode getDataChildByName(String paramString) {
                // TODO Auto-generated method stub
                return null;
            }

            @Override
            public Set<UsesNode> getUses() {
                Set<UsesNode>   uses    = new HashSet<>();
                // TODO populate this.
                return uses;
            }

            @Override
            public Set<AugmentationSchema> getAvailableAugmentations() {
                Set<AugmentationSchema> availableAugmentations  = new HashSet<>();
                // TODO populate this.
                return availableAugmentations;
            }

            @Override
            public List<QName> getKeyDefinition() { 
                List<QName> keyDefinitionList   = new ArrayList<>();
                keyDefinitionList.add(QName.create(listNode.getKey().getName()));
                return keyDefinitionList;
            }
        };
    }

    public TypeDefinition<?> constructTypeDefinitionSchemaNode(final QNameModule qnameModule, final com.cisco.yangide.core.dom.TypeDefinition typeDefinitionNode) {
        return new TypeDefinition() {
            @Override
            public QName getQName() { return QName.create(qnameModule, typeDefinitionNode.getName()); }

            @Override
            public SchemaPath getPath() {
                SchemaPath  schemaPath  = SchemaPath.create(true, getQName());
                // TODO fix this.
                return schemaPath;
            }

            @Override
            public List<UnknownSchemaNode> getUnknownSchemaNodes() {
                List<UnknownSchemaNode> unknownSchemaNodes  = new ArrayList<>();
                // TODO populate this.
                return unknownSchemaNodes;
            }

            @Override
            public String getDescription() { return typeDefinitionNode.getDescription(); }

            @Override
            public String getReference() { return typeDefinitionNode.getReference(); }

            @Override
            public org.opendaylight.yangtools.yang.model.api.Status getStatus() { return constructStatus(typeDefinitionNode.getStatus()); }

            @Override
            public TypeDefinition getBaseType() {
                // TODO Auto-generated method stub
                return null;
            }

            @Override
            public String getUnits() {
                // TODO Auto-generated method stub
                return null;
            }

            @Override
            public Object getDefaultValue() {
                // TODO Auto-generated method stub
                return null;
            }
        };
    }

    public TypeDefinition<?> constructTypeDefinitionSchemaNode(final QNameModule qnameModule, final com.cisco.yangide.core.dom.TypeReference typeReferenceNode) {
        return new TypeDefinition() {
            @Override
            public QName getQName() { return QName.create(qnameModule, typeReferenceNode.getName()); }

            @Override
            public SchemaPath getPath() {
                SchemaPath  schemaPath  = SchemaPath.create(true, getQName());
                // TODO fix this.
                return schemaPath;
            }

            @Override
            public List<UnknownSchemaNode> getUnknownSchemaNodes() {
                List<UnknownSchemaNode> unknownSchemaNodes  = new ArrayList<>();
                // TODO populate this.
                return unknownSchemaNodes;
            }

            @Override
            public String getDescription() { return typeReferenceNode.getDescription(); }

            @Override
            public String getReference() { return typeReferenceNode.getReference(); }

            @Override
            public org.opendaylight.yangtools.yang.model.api.Status getStatus() { return constructStatus(typeReferenceNode.getStatus()); }

            @Override
            public TypeDefinition getBaseType() {
                // TODO Auto-generated method stub
                return null;
            }

            @Override
            public String getUnits() {
                // TODO Auto-generated method stub
                return null;
            }

            @Override
            public Object getDefaultValue() {
                // TODO Auto-generated method stub
                return null;
            }
        };
    }

}