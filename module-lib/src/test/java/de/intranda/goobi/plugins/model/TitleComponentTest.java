package de.intranda.goobi.plugins.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;

import org.junit.Test;

import de.a9d3.testing.checks.DefensiveCopyingCheck;
import de.a9d3.testing.checks.EmptyCollectionCheck;
import de.a9d3.testing.checks.GetterIsSetterCheck;
import de.a9d3.testing.checks.PublicVariableCheck;
import de.a9d3.testing.executer.SingleThreadExecutor;

public class TitleComponentTest {

    @Test
    public void baseTest() {
        SingleThreadExecutor executor = new SingleThreadExecutor();

        assertTrue(executor.execute(TitleComponent.class,
                Arrays.asList(new DefensiveCopyingCheck(), new EmptyCollectionCheck(), new GetterIsSetterCheck(), new PublicVariableCheck())));
    }

    @Test
    public void testConstructor() {
        TitleComponent component = new TitleComponent(null, null, null);
        assertNotNull(component);
    }

    @Test
    public void testName() {
        TitleComponent component = new TitleComponent(null, null, null);
        assertNull(component.getName());
        component = new TitleComponent("", null, null);
        assertEquals("", component.getName());
        component = new TitleComponent("name", null, null);
        assertEquals("name", component.getName());
    }

    @Test
    public void testType() {
        TitleComponent component = new TitleComponent(null, null, null);
        assertNull(component.getType());
        component = new TitleComponent(null, "", null);
        assertEquals("", component.getType());
        component = new TitleComponent(null, "type", null);
        assertEquals("type", component.getType());
    }

    @Test
    public void testValue() {
        TitleComponent component = new TitleComponent(null, null, null);
        assertNull(component.getValue());
        component = new TitleComponent(null, null, "");
        assertEquals("", component.getValue());
        component = new TitleComponent(null, null, "value");
        assertEquals("value", component.getValue());
    }

}
