package de.intranda.goobi.plugins.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.goobi.interfaces.IConfiguration;
import org.goobi.interfaces.IEadEntry;
import org.goobi.interfaces.IMetadataField;
import org.goobi.interfaces.IMetadataGroup;
import org.junit.Test;

public class EadEntryTest {

    @Test
    public void testConstructor() {
        IEadEntry entry = new EadEntry(0, 0);
        assertNotNull(entry);
    }

    @Test
    public void testParentNode() {
        EadEntry entry1 = new EadEntry(0, 0);
        EadEntry entry2 = new EadEntry(1, 1);
        entry2.setParentNode(entry1);
        assertEquals(entry1, entry2.getParentNode());
    }

    @Test
    public void testOrderNumber() {
        EadEntry entry = new EadEntry(1, 1);
        assertEquals(1, entry.getOrderNumber().intValue());
        entry.setOrderNumber(5);
        assertEquals(5, entry.getOrderNumber().intValue());
    }

    @Test
    public void testHierarchy() {
        EadEntry entry = new EadEntry(1, 1);
        assertEquals(1, entry.getHierarchy().intValue());
        entry.setHierarchy(5);
        assertEquals(5, entry.getHierarchy().intValue());
    }

    @Test
    public void testId() {
        EadEntry entry = new EadEntry(1, 1);
        entry.setId("fixture");
        assertEquals("fixture", entry.getId());
    }

    @Test
    public void testLabel() {
        EadEntry entry = new EadEntry(1, 1);
        entry.setLabel("fixture");
        assertEquals("fixture", entry.getLabel());
    }

    @Test
    public void testDisplayChildren() {
        EadEntry entry = new EadEntry(1, 1);
        assertFalse(entry.isDisplayChildren());
        entry.setDisplayChildren(true);
        assertTrue(entry.isDisplayChildren());
    }

    @Test
    public void testSelected() {
        EadEntry entry = new EadEntry(1, 1);
        assertFalse(entry.isSelected());
        entry.setSelected(true);
        assertTrue(entry.isSelected());
    }

    @Test
    public void testSearchFound() {
        EadEntry entry = new EadEntry(1, 1);
        assertFalse(entry.isSearchFound());
        entry.setSearchFound(true);
        assertTrue(entry.isSearchFound());
    }

    @Test
    public void testSelectable() {
        EadEntry entry = new EadEntry(1, 1);
        assertFalse(entry.isSelectable());
        entry.setSelectable(true);
        assertTrue(entry.isSelectable());
    }

    @Test
    public void testNodeType() {
        NodeType nt = new NodeType("fixture", "fixture", "fixture", 1);
        EadEntry entry = new EadEntry(1, 1);
        entry.setNodeType(nt);
        assertEquals("fixture", entry.getNodeType().getNodeName());
    }

    @Test
    public void testAddSubEntry() {
        EadEntry entry1 = new EadEntry(0, 0);
        EadEntry entry2 = new EadEntry(1, 1);
        assertFalse(entry1.isHasChildren());
        entry1.addSubEntry(entry2);
        assertTrue(entry1.isHasChildren());
        assertEquals(entry1, entry2.getParentNode());
        assertEquals(entry2, entry1.getSubEntryList().get(0));
    }

    @Test
    public void testRemoveSubEntry() {
        EadEntry entry1 = new EadEntry(0, 0);
        EadEntry entry2 = new EadEntry(1, 1);
        entry1.addSubEntry(entry2);
        assertTrue(entry1.isHasChildren());
        assertEquals(1, entry1.getSubEntryList().size());
        entry1.removeSubEntry(entry2);
        assertEquals(0, entry1.getSubEntryList().size());
        assertFalse(entry1.isHasChildren());
    }

    @Test
    public void testReOrderElements() {
        EadEntry entry1 = new EadEntry(0, 0);
        EadEntry entry2 = new EadEntry(3, 1);
        EadEntry entry3 = new EadEntry(5, 1);

        entry1.addSubEntry(entry2);
        entry1.addSubEntry(entry3);
        entry1.reOrderElements();
        assertEquals(0, entry1.getSubEntryList().get(0).getOrderNumber().intValue());
        assertEquals(1, entry1.getSubEntryList().get(1).getOrderNumber().intValue());
    }

    @Test
    public void testAllNodes() {

        EadEntry entry = new EadEntry(0, 0);
        EadEntry second = new EadEntry(0, 1);
        EadEntry third = new EadEntry(0, 2);
        entry.addSubEntry(second);
        second.addSubEntry(third);

        List<IEadEntry> list = entry.getAllNodes();
        assertEquals(3, list.size());
    }

    @Test
    public void testGetMoveToDestinationList() {
        EadEntry root = new EadEntry(0, 0);
        EadEntry firstRootChild = new EadEntry(0, 1);
        EadEntry secondRootChild = new EadEntry(1, 1);
        root.addSubEntry(firstRootChild);
        root.addSubEntry(secondRootChild);

        EadEntry sub1 = new EadEntry(0, 2);
        EadEntry sub2 = new EadEntry(1, 2);
        firstRootChild.addSubEntry(sub1);
        firstRootChild.addSubEntry(sub2);

        EadEntry sub3 = new EadEntry(0, 2);
        EadEntry sub4 = new EadEntry(1, 2);
        secondRootChild.addSubEntry(sub3);
        secondRootChild.addSubEntry(sub4);

        List<IEadEntry> firstRootChildDestinations = root.getMoveToDestinationList(firstRootChild);
        assertEquals(5, firstRootChildDestinations.size());

        List<IEadEntry> sub4Destinations = root.getMoveToDestinationList(sub4);
        assertEquals(7, sub4Destinations.size());
    }

    @Test
    public void testGetAsFlatList() {
        EadEntry root = new EadEntry(0, 0);
        EadEntry firstRootChild = new EadEntry(0, 1);
        EadEntry secondRootChild = new EadEntry(1, 1);
        root.addSubEntry(firstRootChild);
        root.addSubEntry(secondRootChild);

        EadEntry sub1 = new EadEntry(0, 2);
        EadEntry sub2 = new EadEntry(1, 2);
        firstRootChild.addSubEntry(sub1);
        firstRootChild.addSubEntry(sub2);

        EadEntry sub3 = new EadEntry(0, 2);
        EadEntry sub4 = new EadEntry(1, 2);
        secondRootChild.addSubEntry(sub3);
        secondRootChild.addSubEntry(sub4);
        List<IEadEntry> flatList = root.getAsFlatList();
        assertEquals(1, flatList.size());
        root.setDisplayChildren(true);
        flatList = root.getAsFlatList();
        assertEquals(3, flatList.size());
        firstRootChild.setDisplayChildren(true);
        flatList = root.getAsFlatList();
        assertEquals(5, flatList.size());
    }

    @Test
    public void testEadMetadataFieldAddValue() {
        EadEntry entry = new EadEntry(0, 0);
        EadMetadataField title =
                new EadMetadataField("input", 1, "unittitle", "element", false, true, true, "input", null, false, null, null, false, null, null,
                        false);
        title.setEadEntry(entry);
        assertFalse(title.isFilled());
        title.addValue();
        assertEquals(1, title.getValues().size());
        // add another value
        title.addValue();
        // value list is still 1, because field is not repeatable
        assertEquals(1, title.getValues().size());
        assertFalse(title.isFilled());
        assertNull(entry.getLabel());
        title.getValues().get(0).setValue("title");
        assertTrue(title.isFilled());
        assertEquals("title", entry.getLabel());

        assertEquals("input", title.getName());
        assertEquals(1, title.getLevel().intValue());
        assertEquals("unittitle", title.getXpath());
        assertEquals("element", title.getXpathType());
        assertFalse(title.isRepeatable());
        assertTrue(title.isVisible());
        assertTrue(title.isShowField());
        assertEquals("input", title.getFieldType());
        assertNull(title.getMetadataName());
        assertFalse(title.isImportMetadataInChild());
        assertNull(title.getValidationType());
        assertNull(title.getRegularExpression());

        EadMetadataField repeatable =
                new EadMetadataField("input", 1, "something", "element", true, true, true, "input", null, false, null, null, false, null, null,
                        false);
        repeatable.setEadEntry(entry);
        assertFalse(repeatable.isFilled());
        repeatable.addValue();
        assertEquals(1, repeatable.getValues().size());
        repeatable.addValue();
        assertEquals(2, repeatable.getValues().size());
    }

    @Test
    public void testEadMetadataFieldAddFieldValue() {
        IEadEntry entry = new EadEntry(0, 0);
        EadMetadataField title =
                new EadMetadataField("input", 1, "something", "element", false, true, true, "input", null, false, null, null, false, null, null,
                        false);
        title.setEadEntry(entry);
        assertFalse(title.isFilled());
        title.addFieldValue(new FieldValue(title));
        assertEquals(1, title.getValues().size());
        // add another value
        title.addFieldValue(new FieldValue(title));
        // value list is still 1, because field is not repeatable
        assertEquals(1, title.getValues().size());

        EadMetadataField repeatable =
                new EadMetadataField("dropdown", 1, "something", "element", true, true, true, "dropdown", null, false, null, null, false, null, null,
                        false);
        repeatable.setEadEntry(entry);
        assertFalse(repeatable.isFilled());
        repeatable.addFieldValue(new FieldValue(repeatable));
        assertEquals(1, repeatable.getValues().size());
        repeatable.addFieldValue(new FieldValue(repeatable));
        assertEquals(2, repeatable.getValues().size());
    }

    @Test
    public void testEquals() {
        IEadEntry entry = new EadEntry(0, 0);

        // same object
        assertEquals(entry, entry);

        // different classes
        assertNotEquals(entry, "string class");

        // different hierarchy
        assertNotEquals(entry, new EadEntry(0, 1));

        // different order
        assertNotEquals(entry, new EadEntry(1, 0));

        // same hierarchy and order
        assertEquals(entry, new EadEntry(0, 0));
    }

    @Test
    public void testHashCode() {
        EadEntry entry = new EadEntry(1, 1);
        assertEquals(954273, entry.hashCode());
    }

    @Test
    public void testUpdateHierarchy() {
        EadEntry entry = new EadEntry(2, 2);
        EadEntry sub = new EadEntry(2, 2);
        entry.addSubEntry(sub);

        assertEquals(2, entry.getHierarchy().intValue());
        assertEquals(2, sub.getHierarchy().intValue());

        entry.updateHierarchy();

        assertEquals(0, entry.getHierarchy().intValue());
        assertEquals(1, sub.getHierarchy().intValue());
    }

    @Test
    public void testMarkAsFound() {
        EadEntry entry = new EadEntry(0, 0);
        EadEntry sub = new EadEntry(1, 0);
        EadEntry third = new EadEntry(2, 0);
        entry.addSubEntry(sub);
        sub.addSubEntry(third);

        // none is marked to be displayed
        assertFalse(entry.isDisplaySearch());
        assertFalse(sub.isDisplaySearch());
        assertFalse(third.isDisplaySearch());

        // now we 'find' the second entry
        sub.markAsFound();

        // the marked entry and the parent are displayed, the child is still hidden
        assertTrue(entry.isDisplaySearch());
        assertTrue(sub.isDisplaySearch());
        assertFalse(third.isDisplaySearch());

        // reset search list
        entry.resetFoundList();
        // none is marked to be displayed
        assertFalse(entry.isDisplaySearch());
        assertFalse(sub.isDisplaySearch());
        assertFalse(third.isDisplaySearch());
    }

    @Test
    public void testSearchList() {
        EadEntry entry = new EadEntry(0, 0);
        EadEntry sub = new EadEntry(1, 0);
        EadEntry third = new EadEntry(2, 0);
        entry.addSubEntry(sub);
        sub.addSubEntry(third);

        // nothing is marked, list is empty
        assertTrue(entry.getSearchList().isEmpty());

        // second record is marked
        sub.markAsFound();
        assertEquals(2, entry.getSearchList().size());
    }

    @Test
    public void testAreaVisible() {
        EadEntry entry = new EadEntry(0, 0);
        assertFalse(entry.isIdentityStatementAreaVisible());
        assertFalse(entry.isContextAreaVisible());
        assertFalse(entry.isContentAndStructureAreaAreaVisible());
        assertFalse(entry.isAccessAndUseAreaVisible());
        assertFalse(entry.isAlliedMaterialsAreaVisible());
        assertFalse(entry.isNotesAreaVisible());
        assertFalse(entry.isDescriptionControlAreaVisible());

        IMetadataField field = new EadMetadataField("name", 1, "xpath", "text", false, true, true, "input", "metadataName", false, "required",
                "regex", true, "viafSearchFields", "viafDisplayFields", false);
        field.addValue();
        List<IMetadataField> list = new ArrayList<>();
        list.add(field);
        entry.setIdentityStatementAreaList(list);
        entry.setContextAreaList(list);
        entry.setContentAndStructureAreaAreaList(list);
        entry.setAccessAndUseAreaList(list);
        entry.setAccessAndUseAreaList(list);
        entry.setAlliedMaterialsAreaList(list);
        entry.setNotesAreaList(list);
        entry.setDescriptionControlAreaList(list);

        assertTrue(entry.isIdentityStatementAreaVisible());
        assertTrue(entry.isContextAreaVisible());
        assertTrue(entry.isContentAndStructureAreaAreaVisible());
        assertTrue(entry.isAccessAndUseAreaVisible());
        assertTrue(entry.isAlliedMaterialsAreaVisible());
        assertTrue(entry.isNotesAreaVisible());
        assertTrue(entry.isDescriptionControlAreaVisible());
    }

    @Test
    public void testDeepCopy() {
        EadEntry entry = new EadEntry(4, 4);
        IMetadataField field = new EadMetadataField("name", 1, "xpath", "text", false, true, true, "input", "metadataName", false, "required",
                "regex", true, "viafSearchFields", "viafDisplayFields", false);
        field.addValue();
        List<IMetadataField> list = new ArrayList<>();
        list.add(field);
        entry.setIdentityStatementAreaList(list);
        entry.setContextAreaList(list);
        entry.setContentAndStructureAreaAreaList(list);
        entry.setAccessAndUseAreaList(list);
        entry.setAccessAndUseAreaList(list);

        entry.setAlliedMaterialsAreaList(list);
        entry.setNotesAreaList(list);
        entry.setDescriptionControlAreaList(list);

        IConfiguration configuration = new DuplicationConfiguration(entry);

        IEadEntry other = entry.deepCopy(configuration);
        assertEquals(entry.getHierarchy(), other.getHierarchy());
        assertEquals(entry.getOrderNumber(), other.getOrderNumber());
    }

    @Test
    public void testCompareTo() {
        EadEntry entry = new EadEntry(0, 0);
        EadEntry other = new EadEntry(1, 1);

        assertEquals(0, entry.compareTo(entry));
        assertEquals(-1, entry.compareTo(other));
        assertEquals(1, other.compareTo(entry));
    }

    @Test
    public void testSequence() {
        EadEntry entry = new EadEntry(0, 0);
        EadEntry sub = new EadEntry(0, 1);
        EadEntry third = new EadEntry(0, 2);
        entry.addSubEntry(sub);
        sub.addSubEntry(third);

        assertEquals("", entry.getSequence());
        assertEquals("0", sub.getSequence());
        assertEquals("0.0", third.getSequence());
    }

    @Test
    public void testDataAsXml() {
        EadEntry entry = new EadEntry(4, 4);
        IMetadataField field = new EadMetadataField("name", 1, "xpath", "text", false, true, true, "input", "metadataName", false, "required",
                "regex", true, "viafSearchFields", "viafDisplayFields", false);
        field.addValue();
        field.getValues().get(0).setValue("value");
        List<IMetadataField> list = new ArrayList<>();
        list.add(field);
        entry.setIdentityStatementAreaList(list);
        assertEquals("<xml><name>value</name></xml>", entry.getDataAsXml());
    }

    @Test
    public void testGroupDataAsXml() {
        EadEntry entry = new EadEntry(4, 4);

        IMetadataField grp = new EadMetadataField("group", 1, "xpath", "text", false, true, true, "input", "metadataName", false, "required",
                "regex", true, "viafSearchFields", "viafDisplayFields", true);

        IMetadataGroup group = grp.createGroup();

        List<IMetadataField> list = new ArrayList<>();
        list.add(grp);
        entry.setIdentityStatementAreaList(list);

        IMetadataField field = new EadMetadataField("name", 1, "xpath", "text", false, true, true,
                "input", "metadataName", false, "required",
                "regex", true, "viafSearchFields", "viafDisplayFields", false);
        field.addValue();
        field.getValues().get(0).setValue("value");
        group.getFields().add(field);

        assertEquals("<xml><group name='group'><field name='name'>value</field></group></xml>", entry.getDataAsXml());
    }

    @Test
    public void testFingerprint() {
        EadEntry entry = new EadEntry(4, 4);
        IMetadataField field = new EadMetadataField("name", 1, "xpath", "text", false, true, true, "input", "metadataName", false, "required",
                "regex", true, "viafSearchFields", "viafDisplayFields", false);
        field.addValue();
        field.getValues().get(0).setValue("value");

        IMetadataField grp = new EadMetadataField("group", 1, "xpath", "text", false, true, true, "input", "metadataName", false, "required", "regex",
                true, "viafSearchFields", "viafDisplayFields", true);

        IMetadataGroup group = grp.createGroup();
        group.getFields().add(field);
        List<IMetadataField> list = new ArrayList<>();
        list.add(grp);

        list.add(field);
        entry.setIdentityStatementAreaList(list);
        assertNull(entry.getFingerprint());
        entry.calculateFingerprint();
        assertEquals("namevaluenamevalue", entry.getFingerprint());
    }

    @Test
    public void testDeleteGroup() {
        EadEntry entry = new EadEntry(4, 4);

        IMetadataField grp = new EadMetadataField("group", 1, "xpath", "text", false, true, true, "input", "metadataName", false, "required", "regex",
                true, "viafSearchFields", "viafDisplayFields", true);
        List<IMetadataField> list = new ArrayList<>();
        list.add(grp);
        entry.setIdentityStatementAreaList(list);
        IMetadataGroup group = grp.createGroup();

        IMetadataField field = new EadMetadataField("name", 1, "xpath", "text", false, true, true,
                "input", "metadataName", false, "required",
                "regex", true, "viafSearchFields", "viafDisplayFields", false);
        field.addValue();
        field.getValues().get(0).setValue("value");
        group.getFields().add(field);

        assertTrue(grp.isFilled());
        grp.deleteGroup(group);
        assertFalse(grp.isFilled());

    }

    @Test
    public void testChildrenHaveProcesses() {

        EadEntry entry = new EadEntry(0, 0);
        EadEntry second = new EadEntry(0, 1);
        EadEntry third = new EadEntry(0, 2);
        entry.addSubEntry(second);
        second.addSubEntry(third);

        // third one has no children and no process
        assertFalse(third.isChildrenHaveProcesses());
        // second has a child without process
        assertFalse(second.isChildrenHaveProcesses());
        // first one has no children with a process
        assertFalse(entry.isChildrenHaveProcesses());

        // now the third process has a process
        third.setGoobiProcessTitle("title");
        // still false as the node has a process itself
        assertFalse(third.isChildrenHaveProcesses());
        assertTrue(second.isChildrenHaveProcesses());
        assertTrue(entry.isChildrenHaveProcesses());

    }
}
