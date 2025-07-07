package de.intranda.goobi.plugins.model;

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.goobi.interfaces.IMetadataField;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import de.intranda.digiverso.normdataimporter.model.NormData;
import de.sub.goobi.config.ConfigurationHelper;

@RunWith(PowerMockRunner.class)
@PrepareForTest({ FieldValue.class, ConfigurationHelper.class, StringUtils.class })
@PowerMockIgnore({ "javax.management.*", "javax.net.ssl.*", "jdk.internal.reflect.*", "com.sun.org.apache.xerces.*", "javax.xml.*", "org.xml.*",
        "org.w3c.*" })
public class FieldValueTest {

    private FieldValue fieldValue;
    private IMetadataField field;

    @Before
    public void setUp() {
        field = createMock(IMetadataField.class);
        fieldValue = new FieldValue(field);
    }

    @Test
    public void testDefaultValues() {
        assertNull(fieldValue.getValue());
        assertNotNull(fieldValue.getMultiselectSelectedValues());
        assertTrue(fieldValue.getMultiselectSelectedValues().isEmpty());
        assertNull(fieldValue.getAuthorityType());
        assertNull(fieldValue.getAuthorityValue());
        assertNull(fieldValue.getSearchValue());
        assertNull(fieldValue.getSearchOption());
        assertNull(fieldValue.getDataList());
        assertNull(fieldValue.getCurrentData());
        assertFalse(fieldValue.isShowNoHits());
        assertNull(fieldValue.getCurrentToponym());
        assertNull(fieldValue.getResultList());
        assertEquals(0, fieldValue.getTotalResults());
        assertNotNull(fieldValue.getViafSearch());
    }

    @Test
    public void testSettersAndGetters() {
        fieldValue.setValue("TestValue");
        assertEquals("TestValue", fieldValue.getValue());

        List<String> multiselectValues = new ArrayList<>();
        multiselectValues.add("Value1");
        multiselectValues.add("Value2");
        fieldValue.setMultiselectSelectedValues(multiselectValues);
        assertEquals(multiselectValues, fieldValue.getMultiselectSelectedValues());

        fieldValue.setAuthorityType("gnd");
        assertEquals("gnd", fieldValue.getAuthorityType());

        fieldValue.setAuthorityValue("12345");
        assertEquals("12345", fieldValue.getAuthorityValue());

        fieldValue.setSearchValue("search");
        assertEquals("search", fieldValue.getSearchValue());

        fieldValue.setSearchOption("option");
        assertEquals("option", fieldValue.getSearchOption());

        List<NormData> currentData = new ArrayList<>();
        fieldValue.setCurrentData(currentData);
        assertEquals(currentData, fieldValue.getCurrentData());

        fieldValue.setShowNoHits(true);
        assertTrue(fieldValue.isShowNoHits());

        fieldValue.setTotalResults(10);
        assertEquals(10, fieldValue.getTotalResults());
    }

    @Test
    public void testAddAndRemoveSelectedValue() {
        fieldValue.setMultiselectValue("Value1");
        assertEquals(1, fieldValue.getMultiselectSelectedValues().size());
        assertTrue(fieldValue.getMultiselectSelectedValues().contains("Value1"));

        fieldValue.removeSelectedValue("Value1");
        assertTrue(fieldValue.getMultiselectSelectedValues().isEmpty());
    }

    @Test
    public void testGetPossibleValues() {
        List<String> selectItemList = new ArrayList<>();
        selectItemList.add("Value1");
        selectItemList.add("Value2");
        selectItemList.add("Value3");

        expect(field.getSelectItemList()).andReturn(selectItemList);
        replay(field);

        fieldValue.setMultiselectValue("Value1");
        List<String> possibleValues = fieldValue.getPossibleValues();
        assertEquals(2, possibleValues.size());
        assertTrue(possibleValues.contains("Value2"));
        assertTrue(possibleValues.contains("Value3"));
    }

    @Test
    public void testGetValuesForXmlExport() {
        expect(field.getXpath()).andReturn("someXpath").anyTimes();
        replay(field);

        fieldValue.setValue("Value");
        assertEquals("Value", fieldValue.getValuesForXmlExport());

        fieldValue.setMultiselectValue("Value1");
        fieldValue.setMultiselectValue("Value2");
        assertEquals("Value1; Value2", fieldValue.getValuesForXmlExport());
    }

    @Test
    public void testEquals() {
        assertNotEquals(fieldValue, null);
        assertEquals(fieldValue, fieldValue);
        FieldValue other = new FieldValue(field);
        assertEquals(fieldValue, other);

        other.setValue("value");
        assertNotEquals(fieldValue, other);
    }

    @Test
    public void testHashCode() {
        assertEquals(-1919486655, fieldValue.hashCode());
        fieldValue.setValue("value");
        assertEquals(-1038419209, fieldValue.hashCode());
    }

    @Test
    public void testToString() {
        assertEquals(
                "FieldValue(value=null, multiselectSelectedValues=[], authorityType=null, authorityValue=null, searchValue=null, searchOption=null, showNoHits=false, fieldType=metadata, firstname=null, lastname=null, mainname=null, subname=null, totalResults=0)",
                fieldValue.toString());

    }

}
