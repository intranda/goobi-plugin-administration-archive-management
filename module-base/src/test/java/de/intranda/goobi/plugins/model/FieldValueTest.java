package de.intranda.goobi.plugins.model;

import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.geonames.Toponym;
import org.geonames.ToponymSearchCriteria;
import org.geonames.ToponymSearchResult;
import org.geonames.WebService;
import org.goobi.interfaces.IMetadataField;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.easymock.PowerMock;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import de.intranda.digiverso.normdataimporter.model.NormData;
import de.sub.goobi.config.ConfigurationHelper;

@RunWith(PowerMockRunner.class)
@PrepareForTest({ FieldValue.class, ConfigurationHelper.class, WebService.class, StringUtils.class })
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

        Toponym toponym = new Toponym();
        fieldValue.setCurrentToponym(toponym);
        assertEquals(toponym, fieldValue.getCurrentToponym());

        List<Toponym> resultList = new ArrayList<>();
        fieldValue.setResultList(resultList);
        assertEquals(resultList, fieldValue.getResultList());

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
        expect(field.getXpath()).andReturn("someXpath").times(2);
        replay(field);

        fieldValue.setValue("Value");
        assertEquals("Value", fieldValue.getValuesForXmlExport());

        fieldValue.setMultiselectValue("Value1");
        fieldValue.setMultiselectValue("Value2");
        assertEquals("Value1; Value2", fieldValue.getValuesForXmlExport());
    }

    @Test
    public void testSearchGeonames() throws Exception {
        PowerMock.mockStatic(ConfigurationHelper.class);
        ConfigurationHelper configHelperMock = createMock(ConfigurationHelper.class);
        expect(ConfigurationHelper.getInstance()).andReturn(configHelperMock).anyTimes();
        expect(configHelperMock.getGeonamesCredentials()).andReturn("username");

        PowerMock.mockStatic(WebService.class);
        ToponymSearchResult searchResultMock = createMock(ToponymSearchResult.class);
        expect(WebService.search(anyObject(ToponymSearchCriteria.class))).andReturn(searchResultMock);
        expect(searchResultMock.getToponyms()).andReturn(new ArrayList<>());
        expect(searchResultMock.getTotalResultsCount()).andReturn(0);

        replay(configHelperMock, searchResultMock);
        PowerMock.replay(ConfigurationHelper.class, WebService.class);

        fieldValue.setSearchValue("Berlin");
        fieldValue.searchGeonames();

        assertTrue(fieldValue.isShowNoHits());

        verify(configHelperMock, searchResultMock);
        PowerMock.verify(ConfigurationHelper.class, WebService.class);
    }
}
