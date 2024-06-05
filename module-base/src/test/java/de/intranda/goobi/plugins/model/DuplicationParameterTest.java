package de.intranda.goobi.plugins.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.Serializable;

import org.junit.Before;
import org.junit.Test;

public class DuplicationParameterTest {

    private DuplicationParameter duplicationParameter;

    @Before
    public void setUp() {
        duplicationParameter = new DuplicationParameter("fieldName");
    }

    @Test
    public void testConstructor() {
        assertEquals("fieldName", duplicationParameter.getFieldName());
        assertEquals("", duplicationParameter.getPrefix());
        assertEquals("", duplicationParameter.getSuffix());
        assertFalse(duplicationParameter.isCounter());
        assertEquals("%04d", duplicationParameter.getCounterFormat());
        assertEquals(1, duplicationParameter.getCounterStartValue());
        assertFalse(duplicationParameter.isGenerated());
        assertNull(duplicationParameter.getFieldType());
    }

    @Test
    public void testSetPrefix() {
        duplicationParameter.setPrefix("prefix");
        assertEquals("prefix", duplicationParameter.getPrefix());
    }

    @Test
    public void testSetSuffix() {
        duplicationParameter.setSuffix("suffix");
        assertEquals("suffix", duplicationParameter.getSuffix());
    }

    @Test
    public void testSetFieldName() {
        duplicationParameter.setFieldName("newFieldName");
        assertEquals("newFieldName", duplicationParameter.getFieldName());
    }

    @Test
    public void testSetCounter() {
        duplicationParameter.setCounter(true);
        assertTrue(duplicationParameter.isCounter());
    }

    @Test
    public void testSetCounterFormat() {
        duplicationParameter.setCounterFormat("###");
        assertEquals("###", duplicationParameter.getCounterFormat());
    }

    @Test
    public void testSetCounterStartValue() {
        duplicationParameter.setCounterStartValue(5);
        assertEquals(5, duplicationParameter.getCounterStartValue());
    }

    @Test
    public void testSetGenerated() {
        duplicationParameter.setGenerated(true);
        assertTrue(duplicationParameter.isGenerated());
    }

    @Test
    public void testSetFieldType() {
        duplicationParameter.setFieldType("type");
        assertEquals("type", duplicationParameter.getFieldType());
    }

    @Test
    public void testSerializable() {
        assertTrue(duplicationParameter instanceof Serializable);
    }
}
