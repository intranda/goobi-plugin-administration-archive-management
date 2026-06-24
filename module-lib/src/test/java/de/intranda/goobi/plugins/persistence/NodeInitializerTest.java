package de.intranda.goobi.plugins.persistence;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;

import org.goobi.interfaces.IMetadataField;
import org.goobi.interfaces.IValue;
import org.goobi.model.GroupValue;
import org.junit.Test;

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
}
