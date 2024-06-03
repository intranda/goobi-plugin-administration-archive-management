package de.intranda.goobi.plugins.model;

import static org.easymock.EasyMock.expect;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;

import org.goobi.interfaces.IEadEntry;
import org.goobi.interfaces.IMetadataField;
import org.junit.Before;
import org.junit.Test;
import org.powermock.api.easymock.PowerMock;
import org.powermock.core.classloader.annotations.PrepareForTest;

@PrepareForTest(DuplicationConfiguration.class)
public class DuplicationConfigurationTest {

    private IEadEntry mockEadEntry;
    private IMetadataField mockField1;
    private IMetadataField mockField2;

    @Before
    public void setUp() {
        mockEadEntry = PowerMock.createMock(IEadEntry.class);
        mockField1 = PowerMock.createMock(IMetadataField.class);
        mockField2 = PowerMock.createMock(IMetadataField.class);
    }

    @Test
    public void testConstructor() {
        expect(mockField1.isVisible()).andReturn(true).anyTimes();
        expect(mockField1.getName()).andReturn("field1").anyTimes();
        expect(mockField2.isVisible()).andReturn(false).anyTimes();
        expect(mockField2.getName()).andReturn("field2").anyTimes();

        expect(mockEadEntry.getIdentityStatementAreaList()).andReturn(Arrays.asList(mockField1, mockField2)).anyTimes();
        expect(mockEadEntry.getContextAreaList()).andReturn(Arrays.asList(mockField1)).anyTimes();
        expect(mockEadEntry.getContentAndStructureAreaAreaList()).andReturn(Arrays.asList(mockField2)).anyTimes();
        expect(mockEadEntry.getAccessAndUseAreaList()).andReturn(Arrays.asList(mockField1, mockField2)).anyTimes();
        expect(mockEadEntry.getAlliedMaterialsAreaList()).andReturn(Arrays.asList(mockField1)).anyTimes();
        expect(mockEadEntry.getNotesAreaList()).andReturn(Arrays.asList(mockField2)).anyTimes();
        expect(mockEadEntry.getDescriptionControlAreaList()).andReturn(Arrays.asList(mockField1, mockField2)).anyTimes();

        PowerMock.replay(mockEadEntry, mockField1, mockField2);

        DuplicationConfiguration config = new DuplicationConfiguration(mockEadEntry);

        assertEquals(1, config.getIdentityStatementArea().size());
        assertEquals("field1", config.getIdentityStatementArea().get(0).getFieldName());

        assertEquals(1, config.getContextArea().size());
        assertEquals("field1", config.getContextArea().get(0).getFieldName());

        assertTrue(config.getContentArea().isEmpty());

        assertEquals(1, config.getAccessArea().size());
        assertEquals("field1", config.getAccessArea().get(0).getFieldName());

        assertEquals(1, config.getAlliedMaterialsArea().size());
        assertEquals("field1", config.getAlliedMaterialsArea().get(0).getFieldName());

        assertTrue(config.getNotesArea().isEmpty());

        assertEquals(1, config.getDescriptionArea().size());
        assertEquals("field1", config.getDescriptionArea().get(0).getFieldName());

        PowerMock.verify(mockEadEntry, mockField1, mockField2);
    }
}
