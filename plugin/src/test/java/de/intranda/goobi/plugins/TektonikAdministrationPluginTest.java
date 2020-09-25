package de.intranda.goobi.plugins;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import org.easymock.EasyMock;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.input.SAXBuilder;
import org.jdom2.input.sax.XMLReaders;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.powermock.api.easymock.PowerMock;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import de.intranda.goobi.plugins.model.EadEntry;
import de.intranda.goobi.plugins.model.EadMetadataField;
import de.sub.goobi.config.ConfigurationHelper;
import de.sub.goobi.helper.HttpClientHelper;

@RunWith(PowerMockRunner.class)
@PrepareForTest({ ConfigurationHelper.class, HttpClientHelper.class })
@PowerMockIgnore({ "javax.management.*", "javax.net.ssl.*" })

public class TektonikAdministrationPluginTest {

    private String resourcesFolder;

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    @Before
    public void setUp() throws Exception {
        resourcesFolder = "src/test/resources/"; // for junit tests in eclipse

        if (!Files.exists(Paths.get(resourcesFolder))) {
            resourcesFolder = "target/test-classes/"; // to run mvn test from cli or in jenkins
        }
        PowerMock.mockStatic(HttpClientHelper.class);
        EasyMock.expect(HttpClientHelper.getStringFromUrl("http://localhost:8984/databases")).andReturn(getDatabaseResponse()).anyTimes();
        EasyMock.expect(HttpClientHelper.getStringFromUrl("http://localhost:8984/db/fixture/ead.xml")).andReturn(readDatabaseResponse()).anyTimes();
        EasyMock.expect(HttpClientHelper.getStringFromUrl("http://localhost:8984/import/fixture/ead.xml"))
        .andReturn(readDatabaseResponse())
        .anyTimes();
        PowerMock.replay(HttpClientHelper.class);

        PowerMock.mockStatic(ConfigurationHelper.class);
        ConfigurationHelper configurationHelper = EasyMock.createMock(ConfigurationHelper.class);
        EasyMock.expect(ConfigurationHelper.getInstance()).andReturn(configurationHelper).anyTimes();
        EasyMock.expect(configurationHelper.getConfigurationFolder()).andReturn(resourcesFolder).anyTimes();

        EasyMock.replay(configurationHelper);
        PowerMock.replay(ConfigurationHelper.class);
    }

    private String getDatabaseResponse() {
        StringBuilder sb = new StringBuilder();
        sb.append("<databases>");
        sb.append("  <database>");
        sb.append("    <name>first database</name>");
        sb.append("    <details>");
        sb.append("      <resource>first_file.xml</resource>");
        sb.append("      <resource>another_file.xml</resource>");
        sb.append("    </details>");
        sb.append("  </database>");
        sb.append("  <database>");
        sb.append("    <name>second database</name>");
        sb.append("    <details>");
        sb.append("      <resource>first_file.xml</resource>");
        sb.append("    </details>");
        sb.append("  </database>");
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
        assertEquals(3, databases.size());
        assertEquals("first database - first_file.xml", databases.get(0));
        assertEquals("first database - another_file.xml", databases.get(1));
        assertEquals("second database - first_file.xml", databases.get(2));
    }

    @Test
    public void testFlatList() {
        TektonikAdministrationPlugin plugin = new TektonikAdministrationPlugin();
        plugin.setDatastoreUrl("http://localhost:8984/");
        plugin.getPossibleDatabases();
        plugin.setSelectedDatabase("fixture - ead.xml");
        plugin.loadSelectedDatabase();
        //  hierarchy contains only root element
        EadEntry root = plugin.getRootElement();
        root.setDisplayChildren(true);
        // flat list contains root element + first hierarchy
        List<EadEntry> flat = plugin.getFlatEntryList();
        assertEquals(2, flat.size());
        // display second hierarchy
        flat.get(1).setDisplayChildren(true);
        plugin.resetFlatList();
        flat = plugin.getFlatEntryList();
        // now flat list contains root element + first + second hierarchy
        assertEquals(7, flat.size());
    }

    @Test
    public void testLoadDatabase() {
        TektonikAdministrationPlugin plugin = new TektonikAdministrationPlugin();
        plugin.setDatastoreUrl("http://localhost:8984/");
        plugin.getPossibleDatabases();
        plugin.setSelectedDatabase("fixture - ead.xml");
        plugin.loadSelectedDatabase();

        EadEntry entry = plugin.getRootElement();

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

        for (EadMetadataField emf : entry.getIdentityStatementAreaList()) {
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
        EadMetadataField origination = null;
        EadMetadataField bioghist = null;
        EadMetadataField custodhist = null;
        EadMetadataField acqinfo = null;
        for (EadMetadataField emf : entry.getContextAreaList()) {
            switch (emf.getName()) {
                case "origination":
                    origination = emf;
                    break;
                case "bioghist":
                    bioghist = emf;
                    break;
                case "custodhist":
                    custodhist = emf;
                    break;
                case "acqinfo":
                    acqinfo = emf;
                    break;
            }
        }

        EadMetadataField scopecontent = null;
        EadMetadataField appraisal = null;
        EadMetadataField accruals = null;
        EadMetadataField arrangement = null;
        for (EadMetadataField emf : entry.getContentAndStructureAreaAreaList()) {
            switch (emf.getName()) {
                case "scopecontent":
                    scopecontent = emf;
                    break;
                case "appraisal":
                    appraisal = emf;
                    break;
                case "accruals":
                    accruals = emf;
                    break;
                case "arrangement":
                    arrangement = emf;
                    break;
            }
        }
        EadMetadataField accessrestrict = null;
        EadMetadataField userestrict = null;
        EadMetadataField langmaterial = null;
        EadMetadataField fonttype = null;
        EadMetadataField phystech = null;
        EadMetadataField otherfindaid = null;
        for (EadMetadataField emf : entry.getAccessAndUseAreaList()) {
            switch (emf.getName()) {
                case "accessrestrict":
                    accessrestrict = emf;
                    break;
                case "userestrict":
                    userestrict = emf;
                    break;
                case "langmaterial":
                    langmaterial = emf;
                    break;
                case "font":
                    fonttype = emf;
                    break;
                case "phystech":
                    phystech = emf;
                    break;
                case "otherfindaid":
                    otherfindaid = emf;
                    break;
            }

        }
        EadMetadataField originalsloc = null;
        EadMetadataField altformavail = null;
        EadMetadataField separatedmaterial = null;
        EadMetadataField bibliography = null;
        for (EadMetadataField emf : entry.getAlliedMaterialsAreaList()) {
            switch (emf.getName()) {
                case "originalsloc":
                    originalsloc = emf;
                    break;
                case "altformavail":
                    altformavail = emf;
                    break;
                case "separatedmaterial":
                    separatedmaterial = emf;
                    break;
                case "bibliography":
                    bibliography = emf;
                    break;
            }
        }

        EadMetadataField didnote = null;
        EadMetadataField odd = null;
        for (EadMetadataField emf : entry.getNotesAreaList()) {
            switch (emf.getName()) {
                case "didnote":
                    didnote = emf;
                    break;
                case "oddnote":
                    odd = emf;
                    break;
            }
        }

        EadMetadataField processinfo = null;
        EadMetadataField maintenanceevent = null;
        EadMetadataField eventdatetime = null;
        for (EadMetadataField emf : entry.getDescriptionControlAreaList()) {
            switch (emf.getName()) {
                case "processinfo":
                    processinfo = emf;
                    break;
                case "maintenanceevent":
                    maintenanceevent = emf;
                    break;
                case "eventdatetime":
                    eventdatetime = emf;
                    break;

            }
        }

        assertEquals(1, entry.getOrderNumber().intValue());
        assertEquals(0, entry.getHierarchy().intValue());
        assertEquals("tectonics header title", entry.getLabel());

        assertEquals("eadid", eadid.getValues().get(0).getValue());
        assertEquals("recordid", recordid.getValues().get(0).getValue());
        assertEquals("agencycode", agencycode.getValues().get(0).getValue());
        assertEquals("unitid", unitid.getValues().get(0).getValue());
        assertEquals("unittitle", unittitle.getValues().get(0).getValue());

        assertEquals("eventtype", maintenanceevent.getValues().get(0).getValue());
        assertEquals("eventdatetime", eventdatetime.getValues().get(0).getValue());
        assertEquals("collection", descriptionLevel.getValues().get(0).getValue());

        assertEquals("unitdate", unitdate.getValues().get(0).getValue());
        assertEquals("unitdatestructured", unitdatestructured.getValues().get(0).getValue());
        assertEquals("physdesc", physdesc.getValues().get(0).getValue());
        assertEquals("physdescstructured", physdescstructured.getValues().get(0).getValue());
        assertEquals("origination", origination.getValues().get(0).getValue());
        assertEquals("ger", langmaterial.getValues().get(0).getMultiselectSelectedValues().get(0));
        assertEquals("handwritten", fonttype.getValues().get(0).getMultiselectSelectedValues().get(0));
        assertEquals("didnote", didnote.getValues().get(0).getValue());

        assertEquals("bioghist", bioghist.getValues().get(0).getValue());
        assertEquals("custodhist", custodhist.getValues().get(0).getValue());
        assertEquals("acqinfo", acqinfo.getValues().get(0).getValue());
        assertEquals("scopecontent", scopecontent.getValues().get(0).getValue());
        assertEquals("appraisal", appraisal.getValues().get(0).getValue());
        assertEquals("accruals", accruals.getValues().get(0).getValue());
        assertEquals("arrangement", arrangement.getValues().get(0).getValue());
        assertEquals("accessrestrict", accessrestrict.getValues().get(0).getValue());
        assertEquals("userestrict", userestrict.getValues().get(0).getValue());
        assertEquals("phystech", phystech.getValues().get(0).getValue());
        assertEquals("otherfindaid", otherfindaid.getValues().get(0).getValue());

        assertEquals("originalsloc", originalsloc.getValues().get(0).getValue());
        assertEquals("altformavail", altformavail.getValues().get(0).getValue());
        assertEquals("separatedmaterial", separatedmaterial.getValues().get(0).getValue());
        assertEquals("bibliography", bibliography.getValues().get(0).getValue());
        assertEquals("odd", odd.getValues().get(0).getValue());
        assertEquals("processinfo", processinfo.getValues().get(0).getValue());

        EadEntry firstSub = entry.getSubEntryList().get(0);
        assertEquals(0, firstSub.getOrderNumber().intValue());
        assertEquals(1, firstSub.getHierarchy().intValue());
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

        assertEquals("file", descriptionLevel.getValues().get(0).getValue());
        assertEquals("1234uniqueId", firstSub.getId());
        assertEquals("first level id", unitid.getValues().get(0).getValue());
        assertEquals("first level title", unittitle.getValues().get(0).getValue());

        List<EadEntry> secondSubList = firstSub.getSubEntryList();

        EadEntry second = secondSubList.get(0);
        assertEquals(0, second.getOrderNumber().intValue());
        assertEquals(2, second.getHierarchy().intValue());
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

        assertEquals("1 Werke", unittitle.getValues().get(0).getValue());
        second = secondSubList.get(1);
        assertEquals(1, second.getOrderNumber().intValue());
        assertEquals(2, second.getHierarchy().intValue());
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
        assertEquals("2 Korrespondenz", unittitle.getValues().get(0).getValue());

        second = secondSubList.get(4);
        assertEquals(4, second.getOrderNumber().intValue());
        assertEquals(2, second.getHierarchy().intValue());
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
        assertEquals("5 Sammlungen / Objekte", unittitle.getValues().get(0).getValue());

        EadEntry third = second.getSubEntryList().get(2);
        assertEquals(2, third.getOrderNumber().intValue());
        assertEquals(3, third.getHierarchy().intValue());
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
        assertEquals("5.3 Fotosammlung", unittitle.getValues().get(0).getValue());

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
        assertEquals("5.3.3 Porträts, andere", unittitle.getValues().get(0).getValue());
        assertEquals("class", descriptionLevel.getValues().get(0).getValue());

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
        assertEquals("file", descriptionLevel.getValues().get(0).getValue());
    }

    @Test
    public void testCreateEadDocument() throws Exception {
        TektonikAdministrationPlugin plugin = new TektonikAdministrationPlugin();
        plugin.setDatastoreUrl("http://localhost:8984/");
        plugin.getPossibleDatabases();
        plugin.setSelectedDatabase("fixture - ead.xml");
        plugin.loadSelectedDatabase();

        // save file
        File exportFolder = folder.newFolder("export");
        plugin.setExportFolder(exportFolder.toString());
        plugin.createEadDocument();

        Path createdEadFile = Paths.get(exportFolder.toString(), "ead.xml");
        assertTrue(Files.exists(createdEadFile));

        // open file
        Document doc = new SAXBuilder(XMLReaders.NONVALIDATING).build(createdEadFile.toFile());

        Element ead = doc.getRootElement();
        Element eadHeader = ead.getChild("eadheader", TektonikAdministrationPlugin.ns);
        assertEquals("eadid", eadHeader.getChildText("eadid", TektonikAdministrationPlugin.ns));

        assertEquals("tectonics header title",
                eadHeader.getChild("filedesc", TektonikAdministrationPlugin.ns)
                .getChild("titlestmt", TektonikAdministrationPlugin.ns)
                .getChildText("titleproper", TektonikAdministrationPlugin.ns));
        Element archdesc = ead.getChild("archdesc", TektonikAdministrationPlugin.ns);
        Element did = archdesc.getChild("did", TektonikAdministrationPlugin.ns);
        Element dsc = archdesc.getChild("dsc", TektonikAdministrationPlugin.ns);

        assertEquals(10, did.getChildren().size());
        assertEquals("unitid", did.getChildren().get(0).getText());
        assertEquals(18, dsc.getChildren().size());

        Element c = dsc.getChildren().get(17);
        Element subDid = c.getChild("did", TektonikAdministrationPlugin.ns);
        assertEquals("first level id", subDid.getChildText("unitid", TektonikAdministrationPlugin.ns));
        assertEquals("first level title", subDid.getChildText("unittitle", TektonikAdministrationPlugin.ns));

    }

    @Test
    public void testSetSelectedEntry() {
        TektonikAdministrationPlugin plugin = new TektonikAdministrationPlugin();

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
        root.setDisplayChildren(true);
        firstRootChild.setDisplayChildren(true);
        secondRootChild.setDisplayChildren(true);
        plugin.setRootElement(root);
        plugin.getFlatEntryList();

        plugin.setSelectedEntry(root);
        assertTrue(root.isSelected());
        assertFalse(firstRootChild.isSelected());

        plugin.setSelectedEntry(firstRootChild);
        assertTrue(firstRootChild.isSelected());
        assertFalse(root.isSelected());
    }

    @Test
    public void testAddNode() {
        TektonikAdministrationPlugin plugin = new TektonikAdministrationPlugin();
        plugin.setDatastoreUrl("http://localhost:8984/");
        plugin.getPossibleDatabases();
        plugin.setSelectedDatabase("fixture - ead.xml");
        plugin.loadSelectedDatabase();
        EadEntry root = plugin.getRootElement();
        // do nothing
        plugin.addNode();

        // add new child element
        root.setDisplayChildren(true);
        plugin.setRootElement(root);
        plugin.getFlatEntryList();

        plugin.setSelectedEntry(root);
        plugin.addNode();

        EadEntry fixture = plugin.getSelectedEntry();
        assertEquals(root, fixture.getParentNode());
    }

    @Test
    public void testDeleteNode() {
        TektonikAdministrationPlugin plugin = new TektonikAdministrationPlugin();
        plugin.setDatastoreUrl("http://localhost:8984/");
        plugin.getPossibleDatabases();
        plugin.setSelectedDatabase("fixture - ead.xml");
        plugin.loadSelectedDatabase();

        EadEntry root = plugin.getRootElement();
        root.setDisplayChildren(true);
        EadEntry firstChild = root.getSubEntryList().get(0);
        // do nothing
        plugin.deleteNode();

        // delete first node
        plugin.getFlatEntryList();
        plugin.setSelectedEntry(firstChild);
        plugin.deleteNode();

        assertEquals(root, plugin.getSelectedEntry());
        assertEquals(0, root.getSubEntryList().size());
    }

    @Test
    public void testPrepareMoveNode() {
        TektonikAdministrationPlugin plugin = new TektonikAdministrationPlugin();
        plugin.setDatastoreUrl("http://localhost:8984/");
        plugin.getPossibleDatabases();
        plugin.setSelectedDatabase("fixture - ead.xml");
        plugin.loadSelectedDatabase();

        // no node selected
        plugin.setDisplayMode("move");
        plugin.prepareMoveNode();
        assertEquals("", plugin.getDisplayMode());

        EadEntry root = plugin.getRootElement();
        EadEntry firstChild = root.getSubEntryList().get(0);
        root.setDisplayChildren(true);
        firstChild.setDisplayChildren(true);
        plugin.getFlatEntryList();
        plugin.setSelectedEntry(root);

        // root node selected
        plugin.setDisplayMode("move");
        plugin.prepareMoveNode();
        assertEquals("", plugin.getDisplayMode());

        // first and only child is selected
        plugin.setDisplayMode("move");
        plugin.setSelectedEntry(firstChild);
        plugin.prepareMoveNode();
        assertEquals("", plugin.getDisplayMode());

        // other element is selected
        plugin.setDisplayMode("move");
        plugin.setSelectedEntry(firstChild.getSubEntryList().get(0));
        plugin.prepareMoveNode();
        assertEquals("move", plugin.getDisplayMode());

    }

    @Test
    public void testMoveNode() {

        TektonikAdministrationPlugin plugin = new TektonikAdministrationPlugin();
        plugin.setDatastoreUrl("http://localhost:8984/");
        plugin.getPossibleDatabases();
        plugin.setSelectedDatabase("fixture - ead.xml");
        plugin.loadSelectedDatabase();
        plugin.setDisplayMode("move");
        EadEntry root = plugin.getRootElement();
        EadEntry firstChild = root.getSubEntryList().get(0);
        root.setDisplayChildren(true);
        firstChild.setDisplayChildren(true);
        plugin.getFlatEntryList();
        EadEntry second = firstChild.getSubEntryList().get(0);
        plugin.setSelectedEntry(second);
        assertEquals("move", plugin.getDisplayMode());

        assertEquals(firstChild, second.getParentNode());
        plugin.setDestinationEntry(root);
        plugin.moveNode();
        assertEquals("", plugin.getDisplayMode());
        assertEquals(root, second.getParentNode());
    }

    @Test
    public void testMoveNodeUp() {
        TektonikAdministrationPlugin plugin = new TektonikAdministrationPlugin();
        plugin.setDatastoreUrl("http://localhost:8984/");
        plugin.getPossibleDatabases();
        plugin.setSelectedDatabase("fixture - ead.xml");
        plugin.loadSelectedDatabase();
        EadEntry root = plugin.getRootElement();
        root.setDisplayChildren(true);
        EadEntry firstChild = root.getSubEntryList().get(0);
        firstChild.setDisplayChildren(true);
        EadEntry firstInSecond = firstChild.getSubEntryList().get(0);
        firstInSecond.setDisplayChildren(true);
        EadEntry secondInSecond = firstChild.getSubEntryList().get(1);
        secondInSecond.setDisplayChildren(true);
        plugin.getFlatEntryList();

        // no node selected - do nothing
        plugin.moveNodeUp();

        // root node selected - do nothing
        plugin.setSelectedEntry(root);
        plugin.moveNodeUp();

        // only child node selected - do nothing
        plugin.setSelectedEntry(firstChild);
        plugin.moveNodeUp();

        // first child node selected - do nothing
        plugin.setSelectedEntry(firstInSecond);
        plugin.moveNodeUp();

        // second child node selected
        plugin.setSelectedEntry(secondInSecond);
        assertEquals(1, secondInSecond.getOrderNumber().intValue());
        plugin.moveNodeUp();
        assertEquals(0, secondInSecond.getOrderNumber().intValue());
    }

    @Test
    public void testMoveNodeDown() {
        TektonikAdministrationPlugin plugin = new TektonikAdministrationPlugin();
        plugin.setDatastoreUrl("http://localhost:8984/");
        plugin.getPossibleDatabases();
        plugin.setSelectedDatabase("fixture - ead.xml");
        plugin.loadSelectedDatabase();
        EadEntry root = plugin.getRootElement();
        root.setDisplayChildren(true);
        EadEntry firstChild = root.getSubEntryList().get(0);
        firstChild.setDisplayChildren(true);
        EadEntry firstInSecond = firstChild.getSubEntryList().get(0);
        firstInSecond.setDisplayChildren(true);
        EadEntry lastInSecond = firstChild.getSubEntryList().get(4);
        lastInSecond.setDisplayChildren(true);
        plugin.getFlatEntryList();

        // no node selected - do nothing
        plugin.moveNodeDown();

        // root node selected - do nothing
        plugin.setSelectedEntry(root);
        plugin.moveNodeDown();

        // only child node selected - do nothing
        plugin.setSelectedEntry(firstChild);
        plugin.moveNodeDown();

        // last child node selected - do nothing
        plugin.setSelectedEntry(lastInSecond);
        plugin.moveNodeDown();

        // first child node selected
        plugin.setSelectedEntry(firstInSecond);
        assertEquals(0, firstInSecond.getOrderNumber().intValue());
        plugin.moveNodeDown();
        assertEquals(1, firstInSecond.getOrderNumber().intValue());
    }

    @Test
    public void testMoveHierarchyDown() {
        TektonikAdministrationPlugin plugin = new TektonikAdministrationPlugin();
        plugin.setDatastoreUrl("http://localhost:8984/");
        plugin.getPossibleDatabases();
        plugin.setSelectedDatabase("fixture - ead.xml");
        plugin.loadSelectedDatabase();
        EadEntry root = plugin.getRootElement();
        root.setDisplayChildren(true);
        EadEntry firstChild = root.getSubEntryList().get(0);
        firstChild.setDisplayChildren(true);
        EadEntry firstInSecond = firstChild.getSubEntryList().get(0);
        firstInSecond.setDisplayChildren(true);
        EadEntry secondInSecond = firstChild.getSubEntryList().get(1);
        secondInSecond.setDisplayChildren(true);
        plugin.getFlatEntryList();
        // no node selected - do nothing
        plugin.moveHierarchyDown();

        // root node selected - do nothing
        plugin.setSelectedEntry(root);
        plugin.moveHierarchyDown();

        // first child element selected - do nothing
        plugin.setSelectedEntry(firstChild);
        plugin.moveHierarchyDown();

        // second child element selected - move it to last child element of current node
        plugin.setSelectedEntry(secondInSecond);
        plugin.moveHierarchyDown();
        plugin.getFlatEntryList();
        assertEquals(firstInSecond, secondInSecond.getParentNode());
    }

    @Test
    public void testMoveHierarchyUp() {
        TektonikAdministrationPlugin plugin = new TektonikAdministrationPlugin();
        plugin.setDatastoreUrl("http://localhost:8984/");
        plugin.getPossibleDatabases();
        plugin.setSelectedDatabase("fixture - ead.xml");
        plugin.loadSelectedDatabase();
        EadEntry root = plugin.getRootElement();
        root.setDisplayChildren(true);
        EadEntry firstChild = root.getSubEntryList().get(0);
        firstChild.setDisplayChildren(true);
        EadEntry firstInSecond = firstChild.getSubEntryList().get(0);
        firstInSecond.setDisplayChildren(true);
        EadEntry thirdInSecond = firstChild.getSubEntryList().get(3);
        thirdInSecond.setDisplayChildren(true);
        plugin.getFlatEntryList();
        // no node selected - do nothing
        plugin.moveHierarchyUp();

        // root node selected - do nothing
        plugin.setSelectedEntry(root);
        plugin.moveHierarchyUp();

        // first child element selected - do nothing
        plugin.setSelectedEntry(firstChild);
        plugin.moveHierarchyUp();

        // second child element selected - move it to last child element of current node
        plugin.setSelectedEntry(thirdInSecond);
        assertEquals(3, thirdInSecond.getOrderNumber().intValue());
        assertEquals(2, thirdInSecond.getHierarchy().intValue());
        plugin.moveHierarchyUp();
        plugin.getFlatEntryList();
        assertEquals(root, thirdInSecond.getParentNode());
        assertEquals(1, thirdInSecond.getOrderNumber().intValue());
        assertEquals(1, thirdInSecond.getHierarchy().intValue());
    }

    @Test
    public void testSearch() {
        TektonikAdministrationPlugin plugin = new TektonikAdministrationPlugin();
        plugin.setDatastoreUrl("http://localhost:8984/");
        plugin.getPossibleDatabases();
        plugin.setSelectedDatabase("fixture - ead.xml");
        plugin.loadSelectedDatabase();
        assertEquals(2, plugin.getFlatEntryList().size());
        plugin.setSearchValue("Milzbrand");
        plugin.search();
        assertEquals(6, plugin.getFlatEntryList().size());

        plugin.setSearchValue("");
        plugin.search();
        assertEquals(2, plugin.getFlatEntryList().size());
    }

}
