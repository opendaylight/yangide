/*******************************************************************************
 * Copyright (c) 2014, 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.opendaylight.yangide.ext.model.editor.diagram;

import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.Path;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.graphiti.dt.IDiagramTypeProvider;
import org.eclipse.graphiti.features.IAddFeature;
import org.eclipse.graphiti.features.ICreateFeature;
import org.eclipse.graphiti.features.IDirectEditingFeature;
import org.eclipse.graphiti.features.ILayoutFeature;
import org.eclipse.graphiti.features.IRemoveFeature;
import org.eclipse.graphiti.features.IUpdateFeature;
import org.eclipse.graphiti.features.context.IAddConnectionContext;
import org.eclipse.graphiti.features.context.IAddContext;
import org.eclipse.graphiti.features.context.ICreateContext;
import org.eclipse.graphiti.features.context.ICustomContext;
import org.eclipse.graphiti.features.context.IDirectEditingContext;
import org.eclipse.graphiti.features.context.ILayoutContext;
import org.eclipse.graphiti.features.context.IRemoveContext;
import org.eclipse.graphiti.features.context.IUpdateContext;
import org.eclipse.graphiti.features.custom.ICustomFeature;
import org.eclipse.graphiti.mm.algorithms.Text;
import org.eclipse.graphiti.mm.pictograms.Connection;
import org.eclipse.graphiti.mm.pictograms.Diagram;
import org.eclipse.graphiti.mm.pictograms.Shape;
import org.eclipse.graphiti.pattern.CreateFeatureForPattern;
import org.eclipse.graphiti.pattern.DefaultFeatureProviderWithPatterns;
import org.eclipse.graphiti.pattern.IPattern;
import org.eclipse.swt.graphics.Point;

import org.opendaylight.yangide.core.YangCorePlugin;
import org.opendaylight.yangide.ext.model.ContainingNode;
import org.opendaylight.yangide.ext.model.editor.Activator;
import org.opendaylight.yangide.ext.model.editor.editors.ISourceModelManager;
import org.opendaylight.yangide.ext.model.editor.editors.YangDiagramBehavior;
import org.opendaylight.yangide.ext.model.editor.features.AddReferenceConnectionFeature;
import org.opendaylight.yangide.ext.model.editor.features.DiagramLayoutFeature;
import org.opendaylight.yangide.ext.model.editor.features.ExtractGroupingCustomFeature;
import org.opendaylight.yangide.ext.model.editor.features.RemoveConnectionFeature;
import org.opendaylight.yangide.ext.model.editor.features.TextDirectEditingFeature;
import org.opendaylight.yangide.ext.model.editor.features.UpdateTextFeature;
import org.opendaylight.yangide.ext.model.editor.patterns.objects.AnyxmlPattern;
import org.opendaylight.yangide.ext.model.editor.patterns.objects.AugmentPattern;
import org.opendaylight.yangide.ext.model.editor.patterns.objects.ChoiceCasePattern;
import org.opendaylight.yangide.ext.model.editor.patterns.objects.ChoicePattern;
import org.opendaylight.yangide.ext.model.editor.patterns.objects.ContainerPattern;
import org.opendaylight.yangide.ext.model.editor.patterns.objects.DeviationPattern;
import org.opendaylight.yangide.ext.model.editor.patterns.objects.ExtensionPattern;
import org.opendaylight.yangide.ext.model.editor.patterns.objects.FeaturePattern;
import org.opendaylight.yangide.ext.model.editor.patterns.objects.GroupingPattern;
import org.opendaylight.yangide.ext.model.editor.patterns.objects.IdentityPattern;
import org.opendaylight.yangide.ext.model.editor.patterns.objects.LeafListPattern;
import org.opendaylight.yangide.ext.model.editor.patterns.objects.LeafPattern;
import org.opendaylight.yangide.ext.model.editor.patterns.objects.ListKeyPattern;
import org.opendaylight.yangide.ext.model.editor.patterns.objects.ListPattern;
import org.opendaylight.yangide.ext.model.editor.patterns.objects.NotificationPattern;
import org.opendaylight.yangide.ext.model.editor.patterns.objects.RpcIOPattern;
import org.opendaylight.yangide.ext.model.editor.patterns.objects.RpcPattern;
import org.opendaylight.yangide.ext.model.editor.patterns.objects.TypedefPattern;
import org.opendaylight.yangide.ext.model.editor.patterns.objects.UsesPattern;
import org.opendaylight.yangide.ext.model.editor.util.YangModelUIUtil;

public class EditorFeatureProvider extends DefaultFeatureProviderWithPatterns {
    private static Map<String, String> TEMPLATES = new HashMap<>();

    private ISourceModelManager sourceModelManager;

    private int diagramWidth;

    private int diagramHeight;

    public EditorFeatureProvider(IDiagramTypeProvider dtp) {
        super(dtp);
        // objects
        addPattern(new GroupingPattern());
        addPattern(new LeafPattern());
        addPattern(new ContainerPattern());
        addPattern(new AnyxmlPattern());
        addPattern(new RpcPattern());
        addPattern(new RpcIOPattern(true));
        addPattern(new RpcIOPattern(false));
        addPattern(new UsesPattern());
        addPattern(new NotificationPattern());
        addPattern(new AugmentPattern());
        addPattern(new DeviationPattern());
        addPattern(new ExtensionPattern());
        addPattern(new FeaturePattern());
        addPattern(new IdentityPattern());
        addPattern(new LeafListPattern());
        addPattern(new ListPattern());
        addPattern(new ChoicePattern());
        addPattern(new ChoiceCasePattern());
        addPattern(new TypedefPattern());
        addPattern(new ListKeyPattern());
    }

    @Override
    public IAddFeature getAddFeature(IAddContext context) {
        if (context instanceof IAddConnectionContext) {
            return new AddReferenceConnectionFeature(this);
        }
        return super.getAddFeature(context);
    }

    @Override
    public IDirectEditingFeature getDirectEditingFeature(IDirectEditingContext context) {
        if (context.getPictogramElement() instanceof Shape
                && ((Shape) context.getPictogramElement()).getGraphicsAlgorithm() instanceof Text) {
            return new TextDirectEditingFeature(this);
        }
        return super.getDirectEditingFeature(context);
    }

    @Override
    public IUpdateFeature getUpdateFeature(IUpdateContext context) {
        if (context.getPictogramElement() instanceof Shape
                && ((Shape) context.getPictogramElement()).getGraphicsAlgorithm() instanceof Text) {
            return new UpdateTextFeature(this);
        }
        return super.getUpdateFeature(context);
    }

    @Override
    public ILayoutFeature getLayoutFeature(ILayoutContext context) {
        if (context.getPictogramElement() instanceof Diagram) {
            return new DiagramLayoutFeature(this);
        }
        return super.getLayoutFeature(context);
    }

    @Override
    public ICustomFeature[] getCustomFeatures(ICustomContext context) {
        return new ICustomFeature[] { new ExtractGroupingCustomFeature(this, sourceModelManager) };
    }

    @Override
    public ICreateFeature[] getCreateFeatures() {
        ICreateFeature[] ret = new ICreateFeature[0];
        List<ICreateFeature> retList = new ArrayList<ICreateFeature>();

        for (final IPattern pattern : getPatterns()) {
            if (pattern.isPaletteApplicable()) {
                retList.add(new CreateFeatureForPattern(this, pattern) {

                    @Override
                    public boolean canCreate(ICreateContext context) {
                        return pattern.canCreate(context);
                    }

                    @Override
                    public Object[] create(ICreateContext context) {
                        YangDiagramBehavior behavior = (YangDiagramBehavior) EditorFeatureProvider.this
                                .getDiagramTypeProvider().getDiagramBehavior();
                        behavior.setCreatePosition(new Point(context.getX(), context.getY()));
                        String name = this.getPattern().getCreateName();
                        if (!TEMPLATES.containsKey(name)) {
                            TEMPLATES.put(name, getTemplateContent(name));
                        }
                        EObject parent = (EObject) getBusinessObjectForPictogramElement(context.getTargetContainer());
                        int position = YangModelUIUtil.getPositionInParent(context.getTargetContainer(), context.getY(),
                                EditorFeatureProvider.this);
                        if (parent instanceof ContainingNode) {
                            ContainingNode node = (ContainingNode) parent;
                            String template = TEMPLATES.get(name);
                            template = template.replaceAll("@name@",
                                    name.replaceAll("\\W", "-") + node.getChildren().size());
                            sourceModelManager.createSourceElement(node, position, template);
                        }

                        return new Object[0];
                    }
                });
            }
        }

        ICreateFeature[] a = getCreateFeaturesAdditional();
        for (ICreateFeature element : a) {
            retList.add(element);
        }

        return retList.toArray(ret);
    }

    @Override
    public IRemoveFeature getRemoveFeature(IRemoveContext context) {
        if (context.getPictogramElement() instanceof Connection) {
            return new RemoveConnectionFeature(this);
        }
        return super.getRemoveFeature(context);
    }

    /**
     * @param sourceModelManager the sourceModelManager to set
     */
    public void setSourceModelManager(ISourceModelManager sourceModelManager) {
        this.sourceModelManager = sourceModelManager;
    }

    /**
     * @return InputStream of template with replaced placeholders.
     * @throws IOException read errors
     */
    private String getTemplateContent(String name) {
        StringBuilder sb = new StringBuilder();

        char[] buff = new char[1024];
        int len = 0;
        Path templatePath = new Path("templates/nodes/" + name + ".txt");
        try (InputStreamReader in = new InputStreamReader(
                FileLocator.openStream(Activator.getDefault().getBundle(), templatePath, false), "UTF-8")) {
            while ((len = in.read(buff)) > 0) {
                sb.append(buff, 0, len);
            }
        } catch (IOException e) {
            YangCorePlugin.log(e);
        }

        return sb.toString();
    }

    public void updateDiagramSize(int x, int y) {
        this.diagramWidth = x;
        this.diagramHeight = y;
    }

    public int getDiagramHeight() {
        return diagramHeight;
    }

    public int getDiagramWidth() {
        return diagramWidth;
    }

}
