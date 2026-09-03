package de.intranda.goobi.plugins.persistence;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.goobi.interfaces.IEadEntry;
import org.goobi.interfaces.IMetadataField;
import org.goobi.interfaces.IValue;
import org.goobi.model.GroupValue;
import org.junit.Test;

import de.intranda.goobi.plugins.model.EadEntry;
import de.intranda.goobi.plugins.model.EadMetadataField;

public class NodeInitializerTest {

    @Test
    public void testAddGroupDataNoDuplicateEmptyValue() {
        IMetadataField group = new EadMetadataField("group", 1, "xpath", "text", false, true, true, "group", "metadataName", false, "required",
                "regex", true, "viaf", "viaf", true, null);
        // repeatable subfield without any stored value
        IMetadataField sub = new EadMetadataField("sub", 1, "xpath", "text", true, true, true, "input", "metadataName", false, "required",
                "regex", true, "viaf", "viaf", false, null);
        group.addSubfield(sub);

        // one stored group instance that contains no value for the subfield
        GroupValue gv = new GroupValue();
        gv.setGroupName("group");
        List<IValue> groups = new ArrayList<>();
        groups.add(gv);

        NodeInitializer.addGroupData(group, groups);

        assertEquals(1, group.getGroups().size());
        IMetadataField createdSub = group.getGroups().get(0).getFields().get(0);
        // exactly one empty value, not a duplicated one
        assertEquals(1, createdSub.getValues().size());
    }

    @Test
    public void testAddFieldToEntryKeepsInheritValueFromParent() {
        IMetadataField template = new EadMetadataField("field", 1, "xpath", "text", false, true, true, "input", "metadataName", false, "required",
                "regex", true, "viaf", "viaf", false, null);
        template.setInheritValueFromParent(true);

        IMetadataField copy = NodeInitializer.addFieldToEntry(null, template, null);

        assertTrue(copy.isInheritValueFromParent());
    }

    @Test
    public void testLoadGroupMetadataKeepsInheritValueFromParent() {
        IMetadataField template = new EadMetadataField("group", 1, "xpath", "text", false, true, true, "group", "metadataName", false, "required",
                "regex", true, "viaf", "viaf", true, null);
        template.setInheritValueFromParent(true);
        EadEntry entry = new EadEntry(0, 0);

        NodeInitializer.loadGroupMetadata(entry, template, new ArrayList<>());

        assertTrue(entry.getFieldByName("group").isInheritValueFromParent());
    }

    /**
     * Create a node whose metadata was already loaded, so it holds one field.
     */
    private EadEntry createInitializedEntry(int order, int hierarchy) {
        EadEntry entry = new EadEntry(order, hierarchy);
        IMetadataField field = new EadMetadataField("field", 1, "xpath", "text", false, true, true, "input", "metadataName", false, null, null, false,
                null, null, false, null);
        List<IMetadataField> fields = new ArrayList<>();
        fields.add(field);
        entry.setIdentityStatementAreaList(fields);
        return entry;
    }

    @Test
    public void testCollectNodesToInitializeContainsNodeAndUnloadedAncestors() {
        EadEntry root = createInitializedEntry(0, 0);
        EadEntry middle = new EadEntry(0, 1);
        EadEntry leaf = new EadEntry(0, 2);
        root.addSubEntry(middle);
        middle.addSubEntry(leaf);

        List<IEadEntry> nodes = NodeInitializer.collectNodesToInitialize(leaf);

        // the root was initialized before and must not be loaded a second time
        assertEquals(2, nodes.size());
        assertSame(leaf, nodes.get(0));
        assertSame(middle, nodes.get(1));
    }

    @Test
    public void testCollectNodesToInitializeIsEmptyForInitializedNodes() {
        EadEntry root = createInitializedEntry(0, 0);
        EadEntry leaf = createInitializedEntry(0, 1);
        root.addSubEntry(leaf);

        assertTrue(NodeInitializer.collectNodesToInitialize(leaf).isEmpty());
    }
}
