package de.intranda.goobi.plugins;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import org.easymock.EasyMock;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.easymock.PowerMock;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import de.intranda.goobi.plugins.model.EadEntry;
import de.sub.goobi.config.ConfigurationHelper;
import de.sub.goobi.helper.HttpClientHelper;

@RunWith(PowerMockRunner.class)
@PrepareForTest({ ConfigurationHelper.class, HttpClientHelper.class })
@PowerMockIgnore({ "javax.management.*", "javax.net.ssl.*" })

public class TektonikAdministrationPluginTest {

    private String resourcesFolder;

    @Before
    public void setUp() throws IOException {
        resourcesFolder = "src/test/resources/"; // for junit tests in eclipse

        if (!Files.exists(Paths.get(resourcesFolder))) {
            resourcesFolder = "target/test-classes/"; // to run mvn test from cli or in jenkins
        }

        PowerMock.mockStatic(HttpClientHelper.class);
        EasyMock.expect(HttpClientHelper.getStringFromUrl("http://localhost:8984/databases")).andReturn(getDatabaseResponse()).anyTimes();
        EasyMock.expect(HttpClientHelper.getStringFromUrl("http://localhost:8984/db/fixture")).andReturn(readDatabaseResponse()).anyTimes();
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

    private String readDatabaseResponse() throws IOException {
        Path eadSource = Paths.get(resourcesFolder + "EAD.XML");

        List<String> lines = Files.readAllLines(eadSource, StandardCharsets.UTF_8);
        StringBuilder sb = new StringBuilder();
        for (String line : lines) {
            sb.append(line);
        }

        return sb.toString();
    }

    @Test
    public void testConstructor() throws IOException {
        TektonikAdministrationPlugin plugin = new TektonikAdministrationPlugin();
        assertNotNull(plugin);
    }

    @Test
    public void testListDatabases() {
        TektonikAdministrationPlugin plugin = new TektonikAdministrationPlugin();
        plugin.setDatastoreUrl("http://localhost:8984/");
        List<String> databases = plugin.getPossibleDatabases();
        assertEquals(2, databases.size());
        assertEquals("first database", databases.get(0));
        assertEquals("second database", databases.get(1));
    }

    @Test
    public void testLoadDatabase() {
        TektonikAdministrationPlugin plugin = new TektonikAdministrationPlugin();
        plugin.setDatastoreUrl("http://localhost:8984/");
        plugin.getPossibleDatabases();
        plugin.setSelectedDatabase("fixture");
        plugin.loadSelectedDatabase();

        List<EadEntry> el = plugin.getEntryList();
        assertEquals(1, el.size());

        EadEntry entry = el.get(0);
        assertEquals(1, entry.getOrderNumber().intValue());
        assertEquals("tectonics header", entry.getLabel());
        assertEquals("eadid", entry.getEadid());
        assertEquals("recordid", entry.getRecordid());
        assertEquals("agencycode", entry.getAgencycode());
        assertEquals("conventiondeclaration", entry.getConventiondeclaration());
        assertEquals("eventtype", entry.getMaintenanceevent());
        assertEquals("eventdatetime", entry.getEventdatetime());
        assertEquals("collection", entry.getDescriptionLevel());

        assertEquals("unitid", entry.getUnitid());
        assertEquals("unittitle", entry.getUnittitle());
        assertEquals("unitdate", entry.getUnitdate());
        assertEquals("unitdatestructured", entry.getUnitdatestructured());
        assertEquals("physdesc", entry.getPhysdesc());
        assertEquals("physdescstructured", entry.getPhysdescstructured());
        assertEquals("origination", entry.getOrigination());
        assertEquals("langmaterial", entry.getLangmaterial());
        assertEquals("didnote", entry.getDidnote());

        assertEquals("bioghist", entry.getBioghist());
        assertEquals("custodhist", entry.getCustodhist());
        assertEquals("acqinfo", entry.getAcqinfo());
        assertEquals("scopecontent", entry.getScopecontent());
        assertEquals("appraisal", entry.getAppraisal());
        assertEquals("accruals", entry.getAccruals());
        assertEquals("arrangement", entry.getArrangement());
        assertEquals("accessrestrict", entry.getAccessrestrict());
        assertEquals("userestrict", entry.getUserestrict());
        assertEquals("phystech", entry.getPhystech());
        assertEquals("otherfindaid", entry.getOtherfindaid());
        assertEquals("originalsloc", entry.getOriginalsloc());
        assertEquals("altformavail", entry.getAltformavail());
        assertEquals("relatedmaterial", entry.getRelatedmaterial());
        assertEquals("separatedmaterial", entry.getSeparatedmaterial());
        assertEquals("bibliography", entry.getBibliography());
        assertEquals("odd", entry.getOdd());
        assertEquals("processinfo", entry.getProcessinfo());

    }
}
