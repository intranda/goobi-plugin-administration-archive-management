package de.intranda.goobi.plugins.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.junit.Test;

public class RecordGroupTest {

    @Test
    public void testConstructor() {
        RecordGroup recordGroup = new RecordGroup();
        assertNotNull(recordGroup);
    }

    @Test
    public void testArgsConstructor() {
        RecordGroup recordGroup = new RecordGroup(null, null);
        assertNotNull(recordGroup);

        recordGroup = new RecordGroup(1, null);
        assertNotNull(recordGroup);

        recordGroup = new RecordGroup(null, "");
        assertNotNull(recordGroup);

        recordGroup = new RecordGroup(1, "title");
        assertNotNull(recordGroup);
    }

    @Test
    public void testId() {
        RecordGroup recordGroup = new RecordGroup(null, null);
        assertNull(recordGroup.getId());
        recordGroup = new RecordGroup(1, null);
        assertEquals(1, recordGroup.getId().intValue());

        recordGroup.setId(2);
        assertEquals(2, recordGroup.getId().intValue());
    }

    @Test
    public void testTitle() {
        RecordGroup recordGroup = new RecordGroup(null, null);
        assertNull(recordGroup.getTitle());
        recordGroup = new RecordGroup(1, "");
        assertEquals("", recordGroup.getTitle());

        recordGroup.setTitle("title");
        assertEquals("title", recordGroup.getTitle());
    }

}
