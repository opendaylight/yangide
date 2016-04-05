package com.cisco.yangide.ext.model.editor;

import static org.assertj.core.api.Assertions.*;

import org.eclipse.core.resources.IFile;
import org.eclipse.jface.text.IDocument;
import org.eclipse.ui.IFileEditorInput;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.mockito.Mockito.*;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.stream.XMLStreamException;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.opendaylight.yangtools.yang.model.parser.api.YangSyntaxErrorException;
import org.opendaylight.yangtools.yang.model.repo.api.SchemaSourceException;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import com.cisco.yangide.editor.editors.YangEditor;

@RunWith(MockitoJUnitRunner.class)
public class YinBuilderTest {
    private YangEditor yangSourceEditor;
    private YangMultiPageEditorPart editor;
    private YinBuilder yinBuilder;
    @Mock
    private IDocument   document;
    @Mock
    private IFileEditorInput    input;
    @Mock
    private IFile               file;

    @Before
    public void setup() {
        yangSourceEditor    = mock(YangEditor.class, RETURNS_MOCKS);
        editor              = mock(YangMultiPageEditorPart.class, RETURNS_MOCKS);
//        yangSourceEditor    = mock(YangEditor.class);
//        editor              = mock(YangMultiPageEditorPart.class);
        yinBuilder  = new YinBuilder(editor, yangSourceEditor);

        when(yangSourceEditor.getDocument()).thenReturn(document);
        when(editor.getEditorInput()).thenReturn(input);
        when(input.getFile()).thenReturn(file);
    }
    
    @Test
    public void testSimpleBuild() throws Exception {
        String  text    =
                "module a {" +
                " namespace \"ns\"; prefix \"pfx\";" +
                "}";
        
        ensureContent(text);
        //when(document.get()).thenReturn("module a { namespace \"ns\"; prefix \"pfx\"; }");
//        when(yangSourceEditor.getDocument().get()).thenReturn("module a { namespace \"a\"; prefix \"a\"; }");
        
        Document    xmldoc  = buildYinDoc();
        
        assertThat(getXPathResult(xmldoc, "/module/namespace/@uri")).isEqualTo("ns");
        assertThat(getXPathResult(xmldoc, "/module/prefix/@value")).isEqualTo("pfx");
    }
    
    @Test
    public void testSimpleContainer() throws Exception {
        String  text    =
                "module a {" +
                " namespace \"ns\"; prefix \"pfx\"; " +
                " container cntnr {" +
                " }" +
                "}";

        ensureContent(text);
        
        Document    xmldoc  = buildYinDoc();

        assertThat(getXPathResult(xmldoc, "/module/container/@name")).isEqualTo("cntnr");
    }

    @Test
    public void testContainerPresence() throws Exception {
        String  text    =
                "module a {" +
                " namespace \"ns\"; prefix \"pfx\"; " +
                " container cntnr {" +
                "  presence \"godlike\"; " +
                " }" +
                "}";

        ensureContent(text);
        
        Document    xmldoc  = buildYinDoc();

        assertThat(getXPathResult(xmldoc, "/module/container/presence/@value")).isEqualTo("true");
    }

    @Test
    public void testContainerConfig() throws Exception {
        String  text    =
                "module a {" +
                " namespace \"ns\"; prefix \"pfx\"; " +
                " container cntnr {" +
                "  config \"true\"; " +
                " }" +
                "}";

        ensureContent(text);
        
        Document    xmldoc  = buildYinDoc();

        assertThat(getXPathResult(xmldoc, "/module/container/config/@value")).isEqualTo("true");
    }

    @Test
    public void testContainerWithLeaf() throws Exception {
        String  text    =
                "module a {" +
                " namespace \"ns\"; prefix \"pfx\"; " +
                " container cntnr {" +
                "  leaf lf {" +
                "    type string;" +
                "  }" +
                " }" +
                "}";

        ensureContent(text);
        
        Document    xmldoc  = buildYinDoc();

        assertThat(getXPathResult(xmldoc, "/module/container[@name='cntnr']/leaf/@name")).isEqualTo("lf");
    }

    @Test
    public void tesContainerLeafConfig() throws Exception {
        String  text    =
                "module a {" +
                " namespace \"ns\"; prefix \"pfx\"; " +
                " container cntnr {" +
                "  leaf lf {" +
                "   type string; " +
                "   config \"true\"; " +
                "  }" +
                " }" +
                "}";

        ensureContent(text);
        
        Document    xmldoc  = buildYinDoc();

        assertThat(getXPathResult(xmldoc, "/module/container/leaf/config/@value")).isEqualTo("true");
    }

    private Document buildYinDoc() throws XMLStreamException, SchemaSourceException, IOException, YangSyntaxErrorException, SAXException, ParserConfigurationException {
        ByteArrayOutputStream   baos    = new ByteArrayOutputStream();
        yinBuilder.build(baos);
        return createDOMDoc(baos.toString());
    }
    
    private void ensureContent(String content) {
        when(document.get()).thenReturn(content);
    }
    
    private String      getXPathResult(Document domdoc, String expression) throws XPathExpressionException {
        return XPathFactory.newInstance().newXPath().compile(expression).evaluate(domdoc);
    }
    
    private Document    createDOMDoc(String xmltext) throws SAXException, IOException, ParserConfigurationException {
        DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = builderFactory.newDocumentBuilder();
        Document    domdoc  = builder.parse(new ByteArrayInputStream(xmltext.getBytes()));
        return domdoc;
    }

}
