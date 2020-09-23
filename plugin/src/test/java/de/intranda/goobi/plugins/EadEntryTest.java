package de.intranda.goobi.plugins;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.Test;

import de.intranda.goobi.plugins.model.EadEntry;

public class EadEntryTest {

    @Test
    public void testConstructor() {
        EadEntry entry = new EadEntry(0, 0);
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
    public void testSelectable() {
        EadEntry entry = new EadEntry(1, 1);
        assertFalse(entry.isSelectable());
        entry.setSelectable(true);
        assertTrue(entry.isSelectable());
    }

    @Test
    public void testNodeType() {
        EadEntry entry = new EadEntry(1, 1);
        entry.setNodeType("fixture");
        assertEquals("fixture", entry.getNodeType());
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

        List<EadEntry> firstRootChildDestinations = root.getMoveToDestinationList(firstRootChild);
        assertEquals(5, firstRootChildDestinations.size());

        List<EadEntry> sub4Destinations = root.getMoveToDestinationList(sub4);
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
        List<EadEntry> flatList = root.getAsFlatList();
        assertEquals(1, flatList.size());
        root.setDisplayChildren(true);
        flatList = root.getAsFlatList();
        assertEquals(3, flatList.size());
        firstRootChild.setDisplayChildren(true);
        flatList = root.getAsFlatList();
        assertEquals(5, flatList.size());
    }
}