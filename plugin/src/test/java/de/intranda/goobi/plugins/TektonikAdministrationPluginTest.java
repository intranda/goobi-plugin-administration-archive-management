package de.intranda.goobi.plugins;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.IOException;
import java.util.List;

import org.easymock.EasyMock;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.easymock.PowerMock;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import de.sub.goobi.config.ConfigurationHelper;
import de.sub.goobi.helper.HttpClientHelper;

@RunWith(PowerMockRunner.class)
@PrepareForTest({ ConfigurationHelper.class, HttpClientHelper.class })
@PowerMockIgnore({ "javax.management.*", "javax.net.ssl.*" })

public class TektonikAdministrationPluginTest {

    @Before
    public void setUp() {

        PowerMock.mockStatic(HttpClientHelper.class);
        EasyMock.expect(HttpClientHelper.getStringFromUrl("http://localhost:8984/databases")).andReturn(getDatabaseResponse()).anyTimes();

        //EasyMock.replay(httpClientHelper);
        PowerMock.replay(HttpClientHelper.class);
    }

    private String getDatabaseResponse() {
        StringBuilder sb = new StringBuilder();
        sb.append("<databases xmlns=\"urn:isbn:1-931666-22-9\">");
        sb.append("    <database>first database</database>");
        sb.append("    <database>second database</database>");
        sb.append("</databases>");
        return sb.toString();
    }

    @Test
    public void testConstructor() throws IOException {
        TektonikAdministrationPlugin plugin = new TektonikAdministrationPlugin();
        assertNotNull(plugin);
    }

    @Test
    public void testListdatabases() {
        TektonikAdministrationPlugin plugin = new TektonikAdministrationPlugin();
        plugin.setDatastoreUrl("http://localhost:8984/");
        List<String> databases=   plugin.getPossibleDatabases();
        assertEquals(2, databases.size());
        assertEquals("first database", databases.get(0));
        assertEquals("second database", databases.get(1));
    }

}
