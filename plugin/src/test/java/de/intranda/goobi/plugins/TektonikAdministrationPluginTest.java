package de.intranda.goobi.plugins;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import org.apache.commons.configuration.XMLConfiguration;
import org.apache.commons.configuration.reloading.FileChangedReloadingStrategy;
import org.easymock.EasyMock;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.easymock.PowerMock;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import de.intranda.goobi.plugins.model.EadEntry;
import de.intranda.goobi.plugins.model.EadMetadataField;
import de.sub.goobi.config.ConfigPlugins;
import de.sub.goobi.config.ConfigurationHelper;
import de.sub.goobi.helper.HttpClientHelper;

@RunWith(PowerMockRunner.class)
@PrepareForTest({ ConfigurationHelper.class, HttpClientHelper.class, ConfigPlugins.class })
@PowerMockIgnore({ "javax.management.*", "javax.net.ssl.*" })

public class TektonikAdministrationPluginTest {

    private String resourcesFolder;

    @Before
    public void setUp() throws Exception {
        resourcesFolder = "src/test/resources/"; // for junit tests in eclipse

        if (!Files.exists(Paths.get(resourcesFolder))) {
            resourcesFolder = "target/test-classes/"; // to run mvn test from cli or in jenkins
        }

        PowerMock.mockStatic(HttpClientHelper.class);
        EasyMock.expect(HttpClientHelper.getStringFromUrl("http://localhost:8984/databases")).andReturn(getDatabaseResponse()).anyTimes();
        EasyMock.expect(HttpClientHelper.getStringFromUrl("http://localhost:8984/db/fixture")).andReturn(readDatabaseResponse()).anyTimes();

        XMLConfiguration config = getConfig();
        PowerMock.mockStatic(ConfigPlugins.class);
        EasyMock.expect(ConfigPlugins.getPluginConfig(EasyMock.anyString())).andReturn(config).anyTimes();

        PowerMock.replay(HttpClientHelper.class);
        PowerMock.replay(ConfigPlugins.class);
    }

    private XMLConfiguration getConfig() throws Exception {
        XMLConfiguration config = new XMLConfiguration("plugin_intranda_administration_tektonik.xml");
        config.setListDelimiter('&');
        config.setReloadingStrategy(new FileChangedReloadingStrategy());
        return config;
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
        List<EadMetadataField> fieldList = entry.getIdentityStatementAreaList();
        EadMetadataField agencycode = null;
        EadMetadataField eadid = null;
        EadMetadataField recordid = null;
        EadMetadataField unitid = null;
        EadMetadataField unittitle = null;
        EadMetadataField unitdate = null;
        EadMetadataField unitdatestructured = null;
        EadMetadataField descriptionLevel = null;
        EadMetadataField physdesc = null;
        EadMetadataField physdescstructured = null;

        for (EadMetadataField emf : fieldList) {
            switch (emf.getName()) {
                case "agencycode":
                    agencycode = emf;
                    break;
                case "eadid":
                    eadid = emf;
                    break;
                case "recordid":
                    recordid = emf;
                    break;
                case "unitid":
                    unitid = emf;
                    break;
                case "unittitle":
                    unittitle = emf;
                    break;
                case "unitdate":
                    unitdate = emf;
                    break;
                case "physdesc":
                    physdesc = emf;
                    break;
                case "unitdatestructured":
                    unitdatestructured = emf;
                    break;
                case "descriptionLevel":
                    descriptionLevel = emf;
                    break;
                case "physdescstructured":
                    physdescstructured = emf;
                    break;

            }
        }

        assertEquals(1, entry.getOrderNumber().intValue());
        assertEquals("tectonics header", entry.getLabel());

        assertEquals("eadid", eadid.getValue());
        assertEquals("recordid", recordid.getValue());
        assertEquals("agencycode", agencycode.getValue());
        assertEquals("unitid", unitid.getValue());
        assertEquals("unittitle", unittitle.getValue());

        assertEquals("conventiondeclaration", entry.getConventiondeclaration());
        assertEquals("eventtype", entry.getMaintenanceevent());
        assertEquals("eventdatetime", entry.getEventdatetime());
        assertEquals("collection", descriptionLevel.getValue());

        assertEquals("unitdate", unitdate.getValue());
        assertEquals("unitdatestructured", unitdatestructured.getValue());
        assertEquals("physdesc", physdesc.getValue());
        assertEquals("physdescstructured", physdescstructured.getValue());
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

        EadEntry firstSub = entry.getSubEntryList().get(0);

        fieldList = firstSub.getIdentityStatementAreaList();
        for (EadMetadataField emf : fieldList) {
            switch (emf.getName()) {
                case "unitid":
                    unitid = emf;
                    break;
                case "unittitle":
                    unittitle = emf;
                    break;
                case "descriptionLevel":
                    descriptionLevel = emf;
                    break;
            }
        }

        assertEquals("file", descriptionLevel.getValue());
        assertEquals("1234uniqueId", firstSub.getId());
        assertEquals("first level id", unitid.getValue());
        assertEquals("first level title", unittitle.getValue());

        List<EadEntry> secondSubList = firstSub.getSubEntryList();

        EadEntry second = secondSubList.get(0);
        fieldList = second.getIdentityStatementAreaList();
        for (EadMetadataField emf : fieldList) {
            switch (emf.getName()) {
                case "unitid":
                    unitid = emf;
                    break;
                case "unittitle":
                    unittitle = emf;
                    break;
            }
        }

        assertEquals("1 Werke", unittitle.getValue());
        second = secondSubList.get(1);

        fieldList = second.getIdentityStatementAreaList();
        for (EadMetadataField emf : fieldList) {
            switch (emf.getName()) {
                case "unitid":
                    unitid = emf;
                    break;
                case "unittitle":
                    unittitle = emf;
                    break;
            }
        }
        assertEquals("2 Korrespondenz", unittitle.getValue());

        second = secondSubList.get(4);
        fieldList = second.getIdentityStatementAreaList();
        for (EadMetadataField emf : fieldList) {
            switch (emf.getName()) {
                case "unitid":
                    unitid = emf;
                    break;
                case "unittitle":
                    unittitle = emf;
                    break;
            }
        }
        assertEquals("5 Sammlungen / Objekte", unittitle.getValue());

        EadEntry third = second.getSubEntryList().get(2);
        fieldList = third.getIdentityStatementAreaList();
        for (EadMetadataField emf : fieldList) {
            switch (emf.getName()) {
                case "unitid":
                    unitid = emf;
                    break;
                case "unittitle":
                    unittitle = emf;
                    break;
            }
        }
        assertEquals("5.3 Fotosammlung", unittitle.getValue());

        EadEntry fourth = third.getSubEntryList().get(2);
        fieldList = fourth.getIdentityStatementAreaList();
        for (EadMetadataField emf : fieldList) {
            switch (emf.getName()) {
                case "unitid":
                    unitid = emf;
                    break;
                case "unittitle":
                    unittitle = emf;
                    break;
                case "descriptionLevel":
                    descriptionLevel = emf;
                    break;
            }
        }
        assertEquals("5.3.3 Porträts, andere", unittitle.getValue());
        assertEquals("class", descriptionLevel.getValue());

        EadEntry fifth = fourth.getSubEntryList().get(0);

        fieldList = fifth.getIdentityStatementAreaList();
        for (EadMetadataField emf : fieldList) {
            switch (emf.getName()) {
                case "unitid":
                    unitid = emf;
                    break;
                case "unittitle":
                    unittitle = emf;
                    break;
                case "descriptionLevel":
                    descriptionLevel = emf;
                    break;
            }
        }
        assertEquals("file", descriptionLevel.getValue());
    }
}
