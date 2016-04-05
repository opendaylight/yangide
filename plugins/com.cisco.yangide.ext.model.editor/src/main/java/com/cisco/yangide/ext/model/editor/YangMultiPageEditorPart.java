/*******************************************************************************
 * Copyright (c) 2014, 2015 Cisco Systems, Inc. and others.  All rights reserved.
 * Copyright (c) 2016 AT&T, Inc.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package com.cisco.yangide.ext.model.editor;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;

import javax.xml.stream.XMLStreamException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IStorage;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.emf.common.util.URI;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.graphics.Point;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.IPersistableElement;
import org.eclipse.ui.IStorageEditorInput;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.part.MultiPageEditorPart;
import org.eclipse.ui.part.MultiPageEditorSite;
import org.eclipse.wst.sse.ui.StructuredTextEditor;

import com.cisco.yangide.core.YangCorePlugin;
import com.cisco.yangide.editor.YangEditorPlugin;
import com.cisco.yangide.editor.editors.IYangEditor;
import com.cisco.yangide.editor.editors.YangEditor;
import com.cisco.yangide.editor.editors.YangSourceViewer;
import com.cisco.yangide.ext.model.Module;
import com.cisco.yangide.ext.model.editor.editors.YangDiagramEditor;
import com.cisco.yangide.ext.model.editor.editors.YangDiagramEditorInput;
import com.cisco.yangide.ext.model.editor.sync.ModelSynchronizer;

import org.opendaylight.yangtools.yang.model.parser.api.YangSyntaxErrorException;
import org.opendaylight.yangtools.yang.model.repo.api.SchemaSourceException;
import org.opendaylight.yangtools.yang.parser.spi.source.SourceException;

/**
 * @author Konstantin Zaitsev
 * date: Aug 7, 2014
 */
public class YangMultiPageEditorPart extends MultiPageEditorPart implements IYangEditor {

    private YangEditor yangSourceEditor;
    private YangSourceViewer yangSourceViewer;
    private YangDiagramEditor yangDiagramEditor;
    private ModelSynchronizer modelSynchronizer;
    private StructuredTextEditor    yinSourcePage;
    private YinViewInput    yinViewInput;
    private boolean disposed;
    
    private static final int    INDEX_SOURCE_PAGE   = 0;
    private static final int    INDEX_DIAGRAM_PAGE  = 1;
    private static final int    INDEX_YIN_PAGE      = 2;
    private static final String MSG_LOADING_YIN_VIEW = "Loading Yin View ...";
    private static final String MSG_SOURCE_INVALID_CANNOT_RENDER    = "Source code invalid, cannot render Yin view.";
    
    @Override
    protected void createPages() {
        yangSourceEditor = new YangEditor();
        yangDiagramEditor = new YangDiagramEditor(yangSourceEditor);
        modelSynchronizer = new ModelSynchronizer(yangSourceEditor, yangDiagramEditor);
        yinSourcePage   = new StructuredTextEditor();
        initSourcePage();
        initDiagramPage();
        initYinPage();
        modelSynchronizer.init();
        modelSynchronizer.enableNotification();
    }

    @Override
    public void doSave(IProgressMonitor monitor) {
        yangSourceEditor.doSave(monitor);
    }

    @Override
    public void doSaveAs() {
        yangSourceEditor.doSaveAs();
    }

    @Override
    public boolean isSaveAsAllowed() {
        return yangSourceEditor.isSaveAsAllowed();
    }

    @Override
    public boolean isDirty() {
        return yangSourceEditor.isDirty();
    }

    @Override
    protected IEditorSite createSite(IEditorPart editor) {
        return new MultiPageEditorSite(this, editor) {
            @Override
            protected void handlePostSelectionChanged(SelectionChangedEvent event) {
                if ((event.getSelection() instanceof StructuredSelection && getActivePage() == 1)
                        || (getActivePage() == 0 && !(event.getSelection() instanceof StructuredSelection))) {
                    super.handlePostSelectionChanged(event);
                }
            }
        };
    }

    private void initDiagramPage() {
        try {
            Module diagModule = modelSynchronizer.getDiagramModule();
            YangDiagramEditorInput input = new YangDiagramEditorInput(URI.createURI("tmp:/local"), getFile(),
                    "com.cisco.yangide.ext.model.editor.editorDiagramTypeProvider", diagModule);
            addPage(INDEX_DIAGRAM_PAGE, yangDiagramEditor, input);
            setPageText(INDEX_DIAGRAM_PAGE, "Diagram");

            yangDiagramEditor.setSourceModelManager(modelSynchronizer.getSourceModelManager());
        } catch (PartInitException e) {
            YangEditorPlugin.log(e);
        }
    }

    private IFile getFile() {
        if (null != yangSourceEditor && null != yangSourceEditor.getEditorInput()) {
            if (yangSourceEditor.getEditorInput() instanceof IFileEditorInput) {
                IFileEditorInput fileEI = (IFileEditorInput) yangSourceEditor.getEditorInput();
                return fileEI.getFile();
            }
        }
        return null;
    }

    private void initSourcePage() {
        try {
            addPage(INDEX_SOURCE_PAGE, yangSourceEditor, getEditorInput());
            setPageText(INDEX_SOURCE_PAGE, "Source");
            yangSourceViewer = (YangSourceViewer) yangSourceEditor.getViewer();
        } catch (PartInitException e) {
            YangEditorPlugin.log(e);
        }
        setPartName(yangSourceEditor.getPartName());
    }

    private void initYinPage() {
        yinSourcePage.setEditorPart(this);
        try {
            // Second parameter is an IEditorPart.  Third parameter is an IEditorInput.
            addPage(INDEX_YIN_PAGE, yinSourcePage, getYinViewInput());
            setPageText(INDEX_YIN_PAGE, "Yin");
        } catch (PartInitException e) {
            YangEditorPlugin.log(e);
        }
    }
    
    private IEditorInput getYinViewInput() {
        if (yinViewInput == null) {
            String  content = MSG_LOADING_YIN_VIEW;
            String  name    = getPartName() + "yin";
            try {
                yinViewInput    = new YinViewInput(name, name, content.getBytes("UTF-8"));
            } catch (UnsupportedEncodingException e) {
                yinViewInput    = new YinViewInput(name, name, content.getBytes());
            }
        }
        return yinViewInput;
    }
    
    @Override
    protected void pageChange(int newPageIndex) {
        if (newPageIndex != INDEX_DIAGRAM_PAGE) {
            yangDiagramEditor.stopSourceSelectionUpdater();
        }
        else if (newPageIndex != INDEX_SOURCE_PAGE) {
            yangSourceViewer.disableProjection();
            if (yangSourceViewer.getReconciler() != null) {
                yangSourceViewer.getReconciler().uninstall();
            }
            yangSourceViewer.disableTextListeners();
        }
        
        if (newPageIndex == INDEX_DIAGRAM_PAGE) {
            modelSynchronizer.syncWithSource();
            if (modelSynchronizer.isSourceInvalid()) {
                MessageDialog.openWarning(getSite().getShell(), "Yang source is invalid",
                        "Yang source has syntax error and diagram view cannot be synchronized correctly.\n"
                                + "Please correct syntax error first.");
            }
            try {
                getEditorSite().getPage().showView("org.eclipse.ui.views.PropertySheet");
            } catch (PartInitException e) {
                YangEditorPlugin.log(e);
            }
            yangDiagramEditor.startSourceSelectionUpdater();
        }
        else if (newPageIndex == INDEX_SOURCE_PAGE) {
            IRegion highlightRange = yangSourceEditor.getHighlightRange();
            yangSourceViewer.enableTextListeners();
            yangSourceViewer.updateDocument();

            yangSourceViewer.enableProjection();
            if (yangSourceViewer.getReconciler() != null) {
                yangSourceViewer.getReconciler().install(yangSourceEditor.getViewer());
            }
            setSourceSelection(highlightRange);
        }
        else if (newPageIndex == INDEX_YIN_PAGE) {
            if (modelSynchronizer.isSourceInvalid()) {
                storeContentInYinView(MSG_SOURCE_INVALID_CANNOT_RENDER);
            }
            else {
                storeContentInYinView(MSG_LOADING_YIN_VIEW);
                loadYinView();
            }
        }
        super.pageChange(newPageIndex);
    }

    public void loadYinView() {
        if (disposed)
            return;
    
        LoadYinViewJob  job = new LoadYinViewJob(this, yangSourceEditor, "loadYinView");
        job.schedule();
    }
    
    public void storeContentInYinView(String content) {
        IDocument   doc     = yinSourcePage.getDocumentProvider().getDocument(getYinViewInput());
        doc.set(content);
    }
    
    private void setSourceSelection(IRegion highlightRange) {
        if (highlightRange != null) {
            Point selectedRange = yangSourceViewer.getSelectedRange();
            if (selectedRange.x != highlightRange.getOffset() && selectedRange.y != highlightRange.getLength()) {
                yangSourceEditor.selectAndReveal(highlightRange.getOffset(), highlightRange.getLength());
            }
        }
    }

    /**
     * @return the yangSourceEditor
     */
    public YangEditor getYangSourceEditor() {
        return yangSourceEditor;
    }

    /**
     * @return the yangDiagramEditor
     */
    public YangDiagramEditor getYangDiagramEditor() {
        return yangDiagramEditor;
    }

    @Override
    public void dispose() {
        disposed    = true;
        try {
            modelSynchronizer.dispose();
        } catch (Exception e) {
            YangCorePlugin.log(e);
        }
        super.dispose();
    }

    @Override
    public void selectAndReveal(int offset, int length) {
        yangSourceEditor.selectAndReveal(offset, length);
    }

    public static class YinViewInput implements IStorageEditorInput {
        private final String name;
        private final String tooltip;
        private final byte[] content;

        public YinViewInput(String name, String tooltip, byte[] content) {
            this.name       = name;
            this.tooltip    = tooltip;
            this.content    = content;
        }

        @Override
        public boolean exists() { return true; }

        @Override
        public ImageDescriptor getImageDescriptor() { return null; }

        @Override
        public String getName() { return this.name; }

        @Override
        public IPersistableElement getPersistable() { return null; }

        @Override
        public String getToolTipText() { return this.tooltip; }

        @Override
        public <T> T getAdapter(Class<T> adapter) { return null; }

        @Override
        public IStorage getStorage() throws CoreException { return new YinStorage(name, content); }
    }
    
    public static class YinStorage implements IStorage {
        private String  name;
        private byte[]  content;
        
        public YinStorage(String name, byte[] content) {
            this.name       = name;
            this.content    = content;
        }
        
        @Override
        public <T> T getAdapter(Class<T> adapter) { return null; }

        @Override
        public InputStream getContents() throws CoreException { return new ByteArrayInputStream(this.content); }

        @Override
        public IPath getFullPath() { return null; }

        @Override
        public String getName() { return this.name; }

        @Override
        public boolean isReadOnly() { return true; }
    }
    
    public static class LoadYinViewJob extends Job {
        private YinBuilder              yinBuilder;
        private YangMultiPageEditorPart editor;
        public LoadYinViewJob(YangMultiPageEditorPart editor, YangEditor yangSourceEditor, String name) {
            super(name);
            this.editor     = editor;
            this.yinBuilder = new YinBuilder(editor, yangSourceEditor);
        }
        
        @Override
        protected IStatus run(IProgressMonitor monitor) {
            // Now we start the real work of generating the Yin view.
            // Get the module from parsing the content of the source view.  If that fails, present
            // simple content that says it failed.
            // There's a YangIDE "Module" class and a Yangtools "Module" class.  Convert from the former
            // to the latter to use it.
            // Construct a SharedSchemaRepository.
            // This will use YinExportUtils to write the Yin file to an OutputStream.
            // Specifically, YinExportUtils.writeModuleToOutputStream(SchemaContext, Module, OutputStream)

            try {
                ByteArrayOutputStream   baos    = new ByteArrayOutputStream();
                yinBuilder.build(baos);
                editor.storeContentInYinView(prettyPrintXML(baos.toString()));
            }
            catch (XMLStreamException | SchemaSourceException | IOException | YangSyntaxErrorException | SourceException ex) {
                YangCorePlugin.log(ex);
                return new Status(Status.ERROR, YangCorePlugin.PLUGIN_ID, "Failed to generate Yin file");
            }

            return Status.OK_STATUS;
        }
        
        private String prettyPrintXML(String xml) {
            String  result  = null;
            try {
                TransformerFactory  factory = TransformerFactory.newInstance();
                Transformer         transformer = factory.newTransformer();
                transformer.setOutputProperty(OutputKeys.INDENT, "yes");
                transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");
                StringWriter formattedStringWriter = new StringWriter();
                transformer.transform(new StreamSource(new StringReader(xml)), new StreamResult(formattedStringWriter));
                result  = formattedStringWriter.getBuffer().toString();; 
            } catch (TransformerException e) {
                YangCorePlugin.log(e);
            }
            return result;
        }
    }
}
