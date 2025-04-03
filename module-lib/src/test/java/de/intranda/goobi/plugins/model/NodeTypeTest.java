package de.intranda.goobi.plugins.model;

import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;

public class NodeTypeTest {

    private NodeType nodeType;

    @Before
    public void setUp() {
        nodeType = new NodeType("NodeName", "DocumentType", "Icon", 123, true, false, null);
    }

    @Test
    public void testConstructorAndGetters() {
        assertEquals("NodeName", nodeType.getNodeName());
        assertEquals("DocumentType", nodeType.getDocumentType());
        assertEquals("Icon", nodeType.getIcon());
        assertEquals(Integer.valueOf(123), nodeType.getProcessTemplateId());
    }

    @Test
    public void testSetters() {
        nodeType.setNodeName("NewNodeName");
        nodeType.setDocumentType("NewDocumentType");
        nodeType.setIcon("NewIcon");
        nodeType.setProcessTemplateId(456);

        assertEquals("NewNodeName", nodeType.getNodeName());
        assertEquals("NewDocumentType", nodeType.getDocumentType());
        assertEquals("NewIcon", nodeType.getIcon());
        assertEquals(Integer.valueOf(456), nodeType.getProcessTemplateId());
    }
}
