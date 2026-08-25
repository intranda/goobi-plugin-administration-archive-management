package de.intranda.goobi.plugins.persistence;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;

import org.goobi.interfaces.IEadEntry;
import org.junit.Test;

import de.intranda.goobi.plugins.model.EadEntry;

public class ArchiveManagementManagerTest {

    /**
     * every node contributes two placeholders to the statement, so every node has to contribute two parameters as well. Passing only the values of
     * the last node made the statement fail with 'Wrong number of parameters' and nothing was stored at all.
     */
    @Test
    public void testEveryNodeContributesItsOwnParameters() {
        List<IEadEntry> nodes = createNodes(3);
        List<Object> parameters = new ArrayList<>();

        String values = ArchiveManagementManager.createNodeValues(1, nodes, parameters);

        assertEquals(6, countPlaceholders(values));
        assertEquals(6, parameters.size());
    }

    /**
     * the parameters have to be in the same order as the rows they belong to
     */
    @Test
    public void testParametersAreInTheOrderOfTheirRows() {
        List<IEadEntry> nodes = createNodes(3);
        List<Object> parameters = new ArrayList<>();

        ArchiveManagementManager.createNodeValues(1, nodes, parameters);

        assertEquals("label 0", parameters.get(0));
        assertEquals("label 1", parameters.get(2));
        assertEquals("label 2", parameters.get(4));
    }

    @Test
    public void testValuesOfASingleNode() {
        List<IEadEntry> nodes = createNodes(1);
        List<Object> parameters = new ArrayList<>();

        String values = ArchiveManagementManager.createNodeValues(42, nodes, parameters);

        assertEquals(2, countPlaceholders(values));
        assertEquals(1, values.split("\\), \\(").length);
        assertEquals("label 0", parameters.get(0));
    }

    private List<IEadEntry> createNodes(int number) {
        List<IEadEntry> nodes = new ArrayList<>();
        for (int i = 0; i < number; i++) {
            IEadEntry entry = new EadEntry(i, 1);
            entry.setId("id_" + i);
            entry.setDatabaseId(i + 1);
            entry.setLabel("label " + i);
            nodes.add(entry);
        }
        return nodes;
    }

    private int countPlaceholders(String values) {
        int count = 0;
        for (char c : values.toCharArray()) {
            if (c == '?') {
                count++;
            }
        }
        return count;
    }
}
