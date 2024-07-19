package de.intranda.goobi.plugins.model;

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.easymock.EasyMock;
import org.goobi.interfaces.IFieldValue;
import org.goobi.interfaces.IMetadataField;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.easymock.PowerMock;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
@PrepareForTest({ StringUtils.class })
@PowerMockIgnore({ "javax.management.*", "javax.net.ssl.*", "jdk.internal.reflect.*", "com.sun.org.apache.xerces.*", "javax.xml.*", "org.xml.*",
        "org.w3c.*" })
public class EadMetadataFieldTest {

    private EadMetadataField eadMetadataField;
    private IFieldValue fieldValueMock;

    @Before
    public void setUp() {
        eadMetadataField = new EadMetadataField("name", 1, "xpath", "text", false, true, true,
                "input", "metadataName", false, "required",
                "regex", true, "viafSearchFields", "viafDisplayFields", false, null);
        fieldValueMock = createMock(IFieldValue.class);
    }

    @Test
    public void testAddFieldValue() {
        expect(fieldValueMock.getValue()).andReturn("value").anyTimes();
        replay(fieldValueMock);

        eadMetadataField.addFieldValue(fieldValueMock);

        assertEquals(1, eadMetadataField.getValues().size());
        assertEquals("value", eadMetadataField.getValues().get(0).getValue());
    }

    @Test
    public void testIsFilled() {
        PowerMock.mockStatic(StringUtils.class);
        expect(StringUtils.isNotBlank(null)).andReturn(false);
        expect(StringUtils.isNotBlank("value")).andReturn(true);
        PowerMock.replay(StringUtils.class);

        assertFalse(eadMetadataField.isFilled());

        List<IFieldValue> values = new ArrayList<>();
        values.add(fieldValueMock);
        eadMetadataField.setValues(values);

        expect(fieldValueMock.getValue()).andReturn("value");
        replay(fieldValueMock);

        assertTrue(eadMetadataField.isFilled());
    }

    @Test
    public void testCopy() {
        eadMetadataField.addFieldValue(fieldValueMock);

        expect(fieldValueMock.getValue()).andReturn("value").anyTimes();
        expect(fieldValueMock.getAuthorityType()).andReturn("type").anyTimes();
        expect(fieldValueMock.getAuthorityValue()).andReturn("authValue").anyTimes();
        replay(fieldValueMock);

        IMetadataField copiedField = eadMetadataField.copy("prefix", "suffix");

        assertEquals("prefixvaluesuffix", copiedField.getValues().get(0).getValue());
        assertEquals("type", copiedField.getValues().get(0).getAuthorityType());
        assertEquals("authValue", copiedField.getValues().get(0).getAuthorityValue());
    }

    @Test
    public void testDeleteValue() {
        eadMetadataField.addFieldValue(fieldValueMock);
        fieldValueMock.setValue(EasyMock.anyString());
        fieldValueMock.setAuthorityValue(EasyMock.anyString());
        fieldValueMock.setAuthorityType(EasyMock.anyString());
        expect(fieldValueMock.getValue()).andReturn("").anyTimes();
        replay(fieldValueMock);

        eadMetadataField.deleteValue(fieldValueMock);

        assertEquals(1, eadMetadataField.getValues().size());
        assertEquals("", eadMetadataField.getValues().get(0).getValue());
    }

    @Test
    public void testEquals() {
        // null is not equals
        assertNotEquals(eadMetadataField, null);
        // same object equals
        assertEquals(eadMetadataField, eadMetadataField);

        // another object with the same values
        assertEquals(eadMetadataField, new EadMetadataField("name", 1, "xpath", "text", false, true, true,
                "input", "metadataName", false, "required",
                "regex", true, "viafSearchFields", "viafDisplayFields", false, null));

        // another object with different values
        assertNotEquals(eadMetadataField, new EadMetadataField("other", 1, "xpath", "text", false, true, true,
                "input", "metadataName", false, "required",
                "regex", true, "viafSearchFields", "viafDisplayFields", false, null));

        assertNotEquals(eadMetadataField, new EadMetadataField("name", 1, "xpath", "text", false, true, true,
                "input", "metadataName", false, "required",
                "regex", true, "viafSearchFields", "viafDisplayFields", true, null));
    }

    @Test
    public void testHashCode() {
        assertEquals(-2140113554, eadMetadataField.hashCode());
        eadMetadataField.setFieldType("other");
        assertEquals(939726416, eadMetadataField.hashCode());
        eadMetadataField.setMetadataName("metadata");
        assertEquals(-760088929, eadMetadataField.hashCode());
    }

    @Test
    public void testToString() {
        assertEquals(
                "EadMetadataField(name=name, level=1, xpath=xpath, xpathType=text, repeatable=false, values=null, visible=true, showField=true, selectItemList=null, fieldType=input, metadataName=metadataName, importMetadataInChild=false, validationType=required, regularExpression=regex, valid=true, validationError=null, searchable=true, viafSearchFields=viafSearchFields, viafDisplayFields=viafDisplayFields, subfields=[], group=false, groups=[])",
                eadMetadataField.toString());
    }

}