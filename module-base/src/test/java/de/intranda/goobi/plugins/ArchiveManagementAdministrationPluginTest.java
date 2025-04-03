package de.intranda.goobi.plugins;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.easymock.EasyMock;
import org.goobi.beans.User;
import org.goobi.interfaces.IEadEntry;
import org.goobi.interfaces.IFieldValue;
import org.goobi.interfaces.IMetadataField;
import org.goobi.interfaces.INodeType;
import org.goobi.interfaces.IRecordGroup;
import org.goobi.interfaces.IValue;
import org.goobi.model.ExtendendValue;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.input.SAXBuilder;
import org.jdom2.input.sax.XMLReaders;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.omnifaces.io.DefaultServletOutputStream;
import org.powermock.api.easymock.PowerMock;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import de.intranda.goobi.plugins.model.EadEntry;
import de.intranda.goobi.plugins.model.RecordGroup;
import de.intranda.goobi.plugins.persistence.ArchiveManagementManager;
import de.sub.goobi.config.ConfigurationHelper;
import de.sub.goobi.helper.FacesContextHelper;
import de.sub.goobi.helper.Helper;
import de.sub.goobi.persistence.managers.ProcessManager;
import io.goobi.workflow.api.vocabulary.VocabularyAPI;
import io.goobi.workflow.api.vocabulary.VocabularyAPIManager;
import io.goobi.workflow.api.vocabulary.VocabularyRecordAPI;
import io.goobi.workflow.api.vocabulary.hateoas.VocabularyRecordPageResult;
import io.goobi.workflow.api.vocabulary.helper.ExtendedVocabulary;
import io.goobi.workflow.api.vocabulary.helper.ExtendedVocabularyRecord;
import io.goobi.workflow.locking.LockingBean;
import jakarta.faces.context.ExternalContext;
import jakarta.faces.context.FacesContext;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.Part;

@RunWith(PowerMockRunner.class)
@PrepareForTest({ ConfigurationHelper.class, ArchiveManagementManager.class, VocabularyAPIManager.class, VocabularyAPI.class, ProcessManager.class,
        Helper.class,
        IEadEntry.class, INodeType.class })
@PowerMockIgnore({ "javax.management.*", "javax.net.ssl.*", "jdk.internal.reflect.*", "com.sun.org.apache.xerces.*", "javax.xml.*", "org.xml.*",
        "org.w3c.*" })
public class ArchiveManagementAdministrationPluginTest {

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    private static String resourcesFolder;

    private IEadEntry selectedEntry;
    private IEadEntry parentNode;
    private IEadEntry copyNode;
    private INodeType nodeType;

    @BeforeClass
    public static void setUpClass() {
        resourcesFolder = "src/test/resources/"; // for junit tests in eclipse
        if (!Files.exists(Paths.get(resourcesFolder))) {
            resourcesFolder = "target/test-classes/"; // to run mvn test from cli or in jenkins
        }
        String log4jFile = resourcesFolder + "log4j2.xml"; // for junit tests in eclipse
        System.setProperty("log4j.configurationFile", log4jFile);
    }

    @Before
    public void setUp() throws Exception {

        PowerMock.mockStatic(ArchiveManagementManager.class);
        List<IRecordGroup> grps = new ArrayList<>();
        grps.add(new RecordGroup(1, "first database"));
        grps.add(new RecordGroup(2, "another_file"));
        grps.add(new RecordGroup(3, "something"));

        EasyMock.expect(ArchiveManagementManager.getAllRecordGroups()).andReturn(grps).anyTimes();

        EasyMock.expect(ArchiveManagementManager.getRecordGroupByTitle("fixture - ead.xml"))
                .andReturn(new RecordGroup(1, "fixture - ead.xml"))
                .anyTimes();

        for (int i = 0; i < 10; i++) {
            ArchiveManagementManager.saveNode(EasyMock.anyInt(), EasyMock.anyObject());
            ArchiveManagementManager.saveNodes(EasyMock.anyInt(), EasyMock.anyObject());
            ArchiveManagementManager.updateNodeHierarchy(EasyMock.anyInt(), EasyMock.anyObject());
        }
        ArchiveManagementManager.saveRecordGroup(EasyMock.anyObject());
        for (int i = 0; i < 3; i++) {
            ArchiveManagementManager.setConfiguredNodes(EasyMock.anyObject());
        }
        ArchiveManagementManager.createTables();
        ArchiveManagementManager.deleteNodes(EasyMock.anyObject());
        EasyMock.expect(ArchiveManagementManager.loadMetadataForNode(EasyMock.anyInt())).andReturn(new HashMap<>()).anyTimes();
        EasyMock.expect(ArchiveManagementManager.loadRecordGroup(EasyMock.anyInt())).andReturn(getSampleData()).anyTimes();

        EasyMock.expect(ArchiveManagementManager.getRecordGroupByTitle(EasyMock.anyString())).andReturn(null);
        RecordGroup rg = new RecordGroup(4, "sa_mple.xml");
        EasyMock.expect(ArchiveManagementManager.getRecordGroupByTitle(EasyMock.anyString())).andReturn(rg);

        List<Integer> searchList = new ArrayList<>();
        searchList.add(4);
        searchList.add(6);
        EasyMock.expect(ArchiveManagementManager.simpleSearch(1, null, "Milzbrand")).andReturn(searchList);

        Map<String, List<IValue>> metadataMap = new HashMap<>();
        List<IValue> meta = new ArrayList<>();
        meta.add(new ExtendendValue("title", "value", null, null));
        metadataMap.put("mainagencycode", meta);
        metadataMap.put("titlestmt", meta);
        metadataMap.put("recordid", meta);
        metadataMap.put("unitid", meta);
        metadataMap.put("unittitle", meta);

        EasyMock.expect(ArchiveManagementManager.convertStringToMap(EasyMock.anyString())).andReturn(metadataMap).anyTimes();

        PowerMock.replay(ArchiveManagementManager.class);

        PowerMock.mockStatic(Helper.class);
        User user = new User();
        user.setSuperAdmin(true);
        user.setId(1);
        user.setNachname("lastname");
        user.setVorname("firstname");

        EasyMock.expect(Helper.getCurrentUser()).andReturn(user).anyTimes();
        for (int i = 0; i < 4; i++) {
            Helper.setFehlerMeldung(EasyMock.anyString());
        }

        EasyMock.expect(Helper.getTranslation(EasyMock.anyString())).andReturn("").anyTimes();
        PowerMock.replay(Helper.class);

        PowerMock.mockStatic(ConfigurationHelper.class);
        ConfigurationHelper configurationHelper = EasyMock.createMock(ConfigurationHelper.class);
        EasyMock.expect(ConfigurationHelper.getInstance()).andReturn(configurationHelper).anyTimes();
        EasyMock.expect(configurationHelper.getConfigurationFolder()).andReturn(resourcesFolder).anyTimes();
        EasyMock.expect(configurationHelper.useS3()).andReturn(false).anyTimes();

        PowerMock.mockStatic(VocabularyAPIManager.class);
        VocabularyAPIManager vocabularyAPIManager = EasyMock.createMock(VocabularyAPIManager.class);
        EasyMock.expect(VocabularyAPIManager.getInstance()).andReturn(vocabularyAPIManager).anyTimes();

        VocabularyAPI vocabularyAPI = EasyMock.createMock(VocabularyAPI.class);
        ExtendedVocabulary vocabulary = EasyMock.createMock(ExtendedVocabulary.class);
        EasyMock.expect(vocabulary.getId()).andReturn(1l).anyTimes();

        EasyMock.expect(vocabularyAPIManager.vocabularies()).andReturn(vocabularyAPI).anyTimes();
        EasyMock.expect(vocabularyAPI.findByName(EasyMock.anyString())).andReturn(vocabulary).anyTimes();

        VocabularyRecordAPI vocabularyRecordAPI = EasyMock.createMock(VocabularyRecordAPI.class);
        EasyMock.expect(vocabularyAPIManager.vocabularyRecords()).andReturn(vocabularyRecordAPI).anyTimes();

        ExtendedVocabularyRecord rec = EasyMock.createMock(ExtendedVocabularyRecord.class);
        EasyMock.expect(rec.getMainValue()).andReturn("val").anyTimes();

        VocabularyRecordPageResult recordPageResult = EasyMock.createMock(VocabularyRecordPageResult.class);
        EasyMock.expect(recordPageResult.getContent()).andReturn(Collections.singletonList(rec)).anyTimes();

        VocabularyRecordAPI.VocabularyRecordQueryBuilder queryBuilder = EasyMock.createMock(VocabularyRecordAPI.VocabularyRecordQueryBuilder.class);
        EasyMock.expect(queryBuilder.request()).andReturn(recordPageResult).anyTimes();

        EasyMock.expect(vocabularyRecordAPI.list(EasyMock.anyLong())).andReturn(queryBuilder).anyTimes();

        EasyMock.expect(vocabularyRecordAPI.getRecordMainValues(EasyMock.anyLong())).andReturn(Collections.singletonList("val")).anyTimes();
        EasyMock.replay(vocabularyAPI, vocabularyRecordAPI, vocabulary, rec);

        PowerMock.mockStatic(ProcessManager.class);
        ProcessManager.saveProcess(EasyMock.anyObject());
        PowerMock.replay(ProcessManager.class);

        EasyMock.replay(configurationHelper, vocabularyAPIManager);
        PowerMock.replay(ConfigurationHelper.class);
        PowerMock.replay(VocabularyAPIManager.class);

        selectedEntry = PowerMock.createMock(IEadEntry.class);
        parentNode = PowerMock.createMock(IEadEntry.class);
        copyNode = PowerMock.createMock(IEadEntry.class);
        nodeType = PowerMock.createMock(INodeType.class);

        // Setting up mocks for selectedEntry
        EasyMock.expect(nodeType.getNodeName()).andReturn("someType").anyTimes();
        EasyMock.expectLastCall();

        EasyMock.expect(selectedEntry.getNodeType()).andReturn(nodeType).anyTimes();
        EasyMock.expect(selectedEntry.getParentNode()).andReturn(parentNode).anyTimes();
        EasyMock.expect(selectedEntry.deepCopy(EasyMock.anyObject())).andReturn(copyNode).anyTimes();
        EasyMock.expect(selectedEntry.getDatabaseId()).andReturn(1).anyTimes();
        EasyMock.expect(selectedEntry.getLabel()).andReturn("label").anyTimes();
        EasyMock.expect(selectedEntry.getAllNodes()).andReturn(Collections.emptyList()).anyTimes();
        EasyMock.expect(selectedEntry.getFingerprint()).andReturn("").anyTimes();
        selectedEntry.calculateFingerprint();
        selectedEntry.calculateFingerprint();
        selectedEntry.calculateFingerprint();
        selectedEntry.setSelected(EasyMock.anyBoolean());
        selectedEntry.setSelected(EasyMock.anyBoolean());
        EasyMock.expect(selectedEntry.getIdentityStatementAreaList()).andReturn(new ArrayList<>()).anyTimes();
        EasyMock.expect(selectedEntry.getContextAreaList()).andReturn(new ArrayList<>()).anyTimes();
        EasyMock.expect(selectedEntry.getContentAndStructureAreaAreaList()).andReturn(new ArrayList<>()).anyTimes();
        EasyMock.expect(selectedEntry.getAccessAndUseAreaList()).andReturn(new ArrayList<>()).anyTimes();
        EasyMock.expect(selectedEntry.getAlliedMaterialsAreaList()).andReturn(new ArrayList<>()).anyTimes();
        EasyMock.expect(selectedEntry.getNotesAreaList()).andReturn(new ArrayList<>()).anyTimes();
        EasyMock.expect(selectedEntry.getDescriptionControlAreaList()).andReturn(new ArrayList<>()).anyTimes();

        // Setting up mocks for parentNode
        EasyMock.expect(parentNode.getSubEntryList()).andReturn(Collections.singletonList(selectedEntry)).anyTimes();
        parentNode.addSubEntry(copyNode);
        EasyMock.expectLastCall().once();

        // Setting up mocks for copyNode
        copyNode.setOrderNumber(2);
        EasyMock.expectLastCall().once();
        EasyMock.expect(copyNode.getDatabaseId()).andReturn(1).anyTimes();

        EasyMock.expect(copyNode.getIdentityStatementAreaList()).andReturn(new ArrayList<>()).anyTimes();
        EasyMock.expect(copyNode.getContextAreaList()).andReturn(new ArrayList<>()).anyTimes();
        EasyMock.expect(copyNode.getContentAndStructureAreaAreaList()).andReturn(new ArrayList<>()).anyTimes();
        EasyMock.expect(copyNode.getAccessAndUseAreaList()).andReturn(new ArrayList<>()).anyTimes();
        EasyMock.expect(copyNode.getAlliedMaterialsAreaList()).andReturn(new ArrayList<>()).anyTimes();
        EasyMock.expect(copyNode.getNotesAreaList()).andReturn(new ArrayList<>()).anyTimes();
        EasyMock.expect(copyNode.getDescriptionControlAreaList()).andReturn(new ArrayList<>()).anyTimes();
        copyNode.calculateFingerprint();
        copyNode.setSelected(EasyMock.anyBoolean());
        EasyMock.expect(copyNode.getAllNodes()).andReturn(Collections.emptyList()).anyTimes();
        EasyMock.expect(copyNode.getLabel()).andReturn("label").anyTimes();
        PowerMock.replay(selectedEntry, parentNode, copyNode);

    }

    @Test
    public void testConstructor() {
        ArchiveManagementAdministrationPlugin plugin = new ArchiveManagementAdministrationPlugin();
        assertNotNull(plugin);
    }

    @Test
    public void testListDatabases() {
        ArchiveManagementAdministrationPlugin plugin = new ArchiveManagementAdministrationPlugin();
        List<String> databases = plugin.getPossibleDatabases();
        assertEquals(3, databases.size());
        assertEquals("first database", databases.get(0));
        assertEquals("another_file", databases.get(1));
        assertEquals("something", databases.get(2));
    }

    @Test
    public void testFlatList() {
        LockingBean.resetAllLocks();
        ArchiveManagementAdministrationPlugin plugin = new ArchiveManagementAdministrationPlugin();
        plugin.getPossibleDatabases();
        plugin.setDatabaseName("fixture - ead.xml");
        plugin.loadSelectedDatabase();
        //  hierarchy contains only root element
        IEadEntry root = plugin.getRootElement();
        root.setDisplayChildren(true);
        // flat list contains root element + first hierarchy
        List<IEadEntry> flat = plugin.getFlatEntryList();
        assertEquals(3, flat.size());
        // display second hierarchy
        flat.get(1).setDisplayChildren(true);
        plugin.resetFlatList();
        flat = plugin.getFlatEntryList();
        // now flat list contains root element + first + second hierarchy
        assertEquals(5, flat.size());
    }

    @Test
    public void testLoadDatabase() {
        LockingBean.resetAllLocks();
        ArchiveManagementAdministrationPlugin plugin = new ArchiveManagementAdministrationPlugin();
        plugin.getPossibleDatabases();
        plugin.setDatabaseName("fixture - ead.xml");
        plugin.loadSelectedDatabase();

        IEadEntry entry = plugin.getRootElement();

        assertEquals("label", entry.getLabel());
    }

    @Test
    public void testUpLoadXmlFile() { //NOSONAR
        Part part = prepareFileUpload();

        LockingBean.resetAllLocks();
        ArchiveManagementAdministrationPlugin plugin = new ArchiveManagementAdministrationPlugin();
        plugin.setDatabaseName("sample");
        plugin.setUploadFile(part);
        plugin.upload();

        IEadEntry entry = plugin.getRootElement();

        IMetadataField agencycode = null;
        IMetadataField recordid = null;
        IMetadataField unitid = null;
        IMetadataField unittitle = null;
        IMetadataField unitdate = null;
        IMetadataField unitdatestructured = null;
        IMetadataField descriptionLevel = null;
        IMetadataField physdesc = null;
        IMetadataField physdescstructured = null;

        for (IMetadataField emf : entry.getIdentityStatementAreaList()) {
            switch (emf.getName()) {
                case "agencycode":
                    agencycode = emf;
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
        IMetadataField origination = null;
        IMetadataField bioghist = null;
        IMetadataField custodhist = null;
        IMetadataField acqinfo = null;
        for (IMetadataField emf : entry.getContextAreaList()) {
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

        IMetadataField scopecontent = null;
        IMetadataField appraisal = null;
        IMetadataField accruals = null;
        IMetadataField arrangement = null;
        for (IMetadataField emf : entry.getContentAndStructureAreaAreaList()) {
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
        IMetadataField accessrestrict = null;
        IMetadataField userestrict = null;
        IMetadataField langmaterial = null;
        IMetadataField fonttype = null;
        IMetadataField phystech = null;
        IMetadataField otherfindaid = null;
        for (IMetadataField emf : entry.getAccessAndUseAreaList()) {
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
        IMetadataField originalsloc = null;
        IMetadataField altformavail = null;
        IMetadataField separatedmaterial = null;
        IMetadataField bibliography = null;
        for (IMetadataField emf : entry.getAlliedMaterialsAreaList()) {
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

        IMetadataField didnote = null;
        IMetadataField odd = null;
        for (IMetadataField emf : entry.getNotesAreaList()) {
            switch (emf.getName()) {
                case "didnote":
                    didnote = emf;
                    break;
                case "oddnote":
                    odd = emf;
                    break;
            }
        }

        assertEquals(0, entry.getOrderNumber().intValue());
        assertEquals(0, entry.getHierarchy().intValue());
        assertEquals("archive header title", entry.getLabel());

        assertEquals("recordid", recordid.getValues().get(0).getValue());
        assertEquals("agencycode", agencycode.getValues().get(0).getValue());
        assertEquals("unitid", unitid.getValues().get(0).getValue());
        assertEquals("unittitle", unittitle.getValues().get(0).getValue());

        assertEquals("collection", descriptionLevel.getValues().get(0).getValue());

        assertEquals("unitdate", unitdate.getValues().get(0).getValue());
        assertEquals("regex", unitdate.getValidationType());
        assertEquals("\\d{4}", unitdate.getRegularExpression());
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

        IEadEntry firstSub = entry.getSubEntryList().get(0);
        assertEquals(0, firstSub.getOrderNumber().intValue());
        assertEquals(1, firstSub.getHierarchy().intValue());
        List<IMetadataField> fieldList = firstSub.getIdentityStatementAreaList();
        for (IMetadataField emf : fieldList) {
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

        List<IEadEntry> secondSubList = firstSub.getSubEntryList();

        IEadEntry second = secondSubList.get(0);
        assertEquals(0, second.getOrderNumber().intValue());
        assertEquals(2, second.getHierarchy().intValue());
        fieldList = second.getIdentityStatementAreaList();
        for (IMetadataField emf : fieldList) {
            switch (emf.getName()) {
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
        for (IMetadataField emf : fieldList) {
            switch (emf.getName()) {
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
        for (IMetadataField emf : fieldList) {
            switch (emf.getName()) {
                case "unittitle":
                    unittitle = emf;
                    break;
            }
        }
        assertEquals("5 Sammlungen / Objekte", unittitle.getValues().get(0).getValue());

        IEadEntry third = second.getSubEntryList().get(2);
        assertEquals(2, third.getOrderNumber().intValue());
        assertEquals(3, third.getHierarchy().intValue());
        fieldList = third.getIdentityStatementAreaList();
        for (IMetadataField emf : fieldList) {
            switch (emf.getName()) {
                case "unittitle":
                    unittitle = emf;
                    break;
            }
        }
        assertEquals("5.3 Fotosammlung", unittitle.getValues().get(0).getValue());

        IEadEntry fourth = third.getSubEntryList().get(2);
        fieldList = fourth.getIdentityStatementAreaList();
        for (IMetadataField emf : fieldList) {
            switch (emf.getName()) {
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

        IEadEntry fifth = fourth.getSubEntryList().get(0);

        fieldList = fifth.getIdentityStatementAreaList();
        for (IMetadataField emf : fieldList) {
            switch (emf.getName()) {
                case "descriptionLevel":
                    descriptionLevel = emf;
                    break;
            }
        }
        assertEquals("file", descriptionLevel.getValues().get(0).getValue());
    }

    @Test
    public void testDownloadArchive() throws Exception {
        Part part = prepareFileUpload();

        LockingBean.resetAllLocks();
        ArchiveManagementAdministrationPlugin plugin = new ArchiveManagementAdministrationPlugin();
        plugin.setDatabaseName("sample");
        plugin.setUploadFile(part);
        plugin.upload();
        plugin.setRecordGroup(new RecordGroup(1, "sample"));

        // mock download, save response into temporary file
        FacesContext facesContext = EasyMock.createMock(FacesContext.class);
        EasyMock.expect(facesContext.getResponseComplete()).andReturn(false).anyTimes();
        FacesContextHelper.setFacesContext(facesContext);
        ExternalContext externalContext = EasyMock.createMock(ExternalContext.class);
        ServletContext servletContext = EasyMock.createMock(ServletContext.class);
        HttpServletResponse response = EasyMock.createMock(HttpServletResponse.class);

        EasyMock.expect(facesContext.getExternalContext()).andReturn(externalContext).anyTimes();
        EasyMock.expect(externalContext.getResponse()).andReturn(response).anyTimes();

        EasyMock.expect(externalContext.getContext()).andReturn(servletContext).anyTimes();
        EasyMock.expect(servletContext.getMimeType(EasyMock.anyString())).andReturn("application/xml").anyTimes();

        response.setContentType(EasyMock.anyString());

        response.setHeader(EasyMock.anyString(), EasyMock.anyString());

        File file = folder.newFile("ead.xml");
        ServletOutputStream out = new DefaultServletOutputStream(new FileOutputStream(file));

        EasyMock.expect(response.getOutputStream()).andReturn(out);

        facesContext.responseComplete();

        EasyMock.replay(response);
        EasyMock.replay(servletContext);
        EasyMock.replay(externalContext);
        EasyMock.replay(facesContext);

        plugin.downloadArchive();

        // open file
        Document doc = new SAXBuilder(XMLReaders.NONVALIDATING).build(file);

        Element ead = doc.getRootElement();
        Element eadHeader = ead.getChild("eadheader", plugin.getConfig().getNameSpaceWrite());

        assertEquals("value",
                eadHeader.getChild("filedesc", plugin.getConfig().getNameSpaceWrite())
                        .getChild("titlestmt", plugin.getConfig().getNameSpaceWrite())
                        .getChildText("titleproper", plugin.getConfig().getNameSpaceWrite()));
        Element archdesc = ead.getChild("archdesc", plugin.getConfig().getNameSpaceWrite());
        Element did = archdesc.getChild("did", plugin.getConfig().getNameSpaceWrite());
        Element dsc = archdesc.getChild("dsc", plugin.getConfig().getNameSpaceWrite());

        assertEquals(2, did.getChildren().size());
        assertEquals("value", did.getChildren().get(0).getText());
        assertEquals(2, dsc.getChildren().size());

        Element c = dsc.getChildren().get(1);
        Element subDid = c.getChild("did", plugin.getConfig().getNameSpaceWrite());
        assertEquals("value", subDid.getChildText("unitid", plugin.getConfig().getNameSpaceWrite()));
        assertEquals("value", subDid.getChildText("unittitle", plugin.getConfig().getNameSpaceWrite()));

        Element event = ead.getChild("control", plugin.getConfig().getNameSpaceWrite())
                .getChild("maintenancehistory", plugin.getConfig().getNameSpaceWrite())
                .getChild("maintenanceevent", plugin.getConfig().getNameSpaceWrite());
        assertEquals("revised", event.getChild("eventtype", plugin.getConfig().getNameSpaceWrite()).getText());
        assertNotNull(event.getChild("eventdatetime", plugin.getConfig().getNameSpaceWrite()).getText());

    }

    @Test
    public void testSetSelectedEntry() {
        LockingBean.resetAllLocks();
        ArchiveManagementAdministrationPlugin plugin = new ArchiveManagementAdministrationPlugin();
        plugin.setRecordGroup(new RecordGroup(1, "sample"));
        plugin.setDatabaseName("fixture - ead.xml");
        plugin.setTestMode(true);
        IEadEntry root = new EadEntry(0, 0);
        IEadEntry firstRootChild = new EadEntry(0, 1);
        IEadEntry secondRootChild = new EadEntry(1, 1);
        root.addSubEntry(firstRootChild);
        root.addSubEntry(secondRootChild);
        IEadEntry sub1 = new EadEntry(0, 2);
        IEadEntry sub2 = new EadEntry(1, 2);
        firstRootChild.addSubEntry(sub1);
        firstRootChild.addSubEntry(sub2);
        IEadEntry sub3 = new EadEntry(0, 2);
        IEadEntry sub4 = new EadEntry(1, 2);
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
        LockingBean.resetAllLocks();
        ArchiveManagementAdministrationPlugin plugin = new ArchiveManagementAdministrationPlugin();
        plugin.setTestMode(true);
        plugin.getPossibleDatabases();
        plugin.setDatabaseName("fixture - ead.xml");
        plugin.loadSelectedDatabase();
        IEadEntry root = plugin.getRootElement();
        // do nothing
        plugin.addNode();

        // add new child element
        root.setDisplayChildren(true);
        plugin.setRootElement(root);
        plugin.getFlatEntryList();

        plugin.setSelectedEntry(root);
        plugin.addNode();

        IEadEntry fixture = plugin.getSelectedEntry();
        assertEquals(root, fixture.getParentNode());
    }

    @Test
    public void testDeleteNode() {
        LockingBean.resetAllLocks();
        ArchiveManagementAdministrationPlugin plugin = new ArchiveManagementAdministrationPlugin();
        plugin.setTestMode(true);
        plugin.getPossibleDatabases();
        plugin.setDatabaseName("fixture - ead.xml");
        plugin.loadSelectedDatabase();

        IEadEntry root = plugin.getRootElement();
        plugin.setSelectedEntry(root);
        root.setDisplayChildren(true);
        assertEquals(2, root.getSubEntryList().size());
        IEadEntry firstChild = root.getSubEntryList().get(0);
        // do nothing
        plugin.deleteNode();
        assertEquals(2, root.getSubEntryList().size());

        // delete first node
        plugin.getFlatEntryList();
        plugin.setSelectedEntry(firstChild);
        plugin.deleteNode();

        assertEquals(root, plugin.getSelectedEntry());
        assertEquals(1, root.getSubEntryList().size());
    }

    @Test
    public void testPrepareMoveNode() {
        LockingBean.resetAllLocks();
        ArchiveManagementAdministrationPlugin plugin = new ArchiveManagementAdministrationPlugin();
        plugin.setTestMode(true);
        plugin.getPossibleDatabases();
        plugin.setDatabaseName("fixture - ead.xml");
        plugin.loadSelectedDatabase();

        // no node selected
        plugin.setDisplayMode("move");
        plugin.prepareMoveNode();
        assertEquals("", plugin.getDisplayMode());

        IEadEntry root = plugin.getRootElement();

        // delete second root child for this test
        plugin.setSelectedEntry(root.getSubEntryList().get(1));
        plugin.deleteNode();

        plugin.setSelectedEntry(root);
        IEadEntry firstChild = root.getSubEntryList().get(0);
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
        LockingBean.resetAllLocks();
        LockingBean.resetAllLocks();
        ArchiveManagementAdministrationPlugin plugin = new ArchiveManagementAdministrationPlugin();
        plugin.setTestMode(true);
        plugin.getPossibleDatabases();
        plugin.setDatabaseName("fixture - ead.xml");
        plugin.loadSelectedDatabase();
        plugin.setDisplayMode("move");
        IEadEntry root = plugin.getRootElement();
        IEadEntry firstChild = root.getSubEntryList().get(0);
        root.setDisplayChildren(true);
        firstChild.setDisplayChildren(true);
        plugin.getFlatEntryList();
        IEadEntry second = firstChild.getSubEntryList().get(0);
        plugin.setSelectedEntry(second);
        assertEquals("move", plugin.getDisplayMode());

        assertEquals(firstChild, second.getParentNode());
        plugin.setDestinationEntry(root);
        plugin.moveNode();
        assertEquals("", plugin.getDisplayMode());
    }

    @Test
    public void testMoveNodeUp() {
        LockingBean.resetAllLocks();
        ArchiveManagementAdministrationPlugin plugin = new ArchiveManagementAdministrationPlugin();
        plugin.setTestMode(true);
        plugin.getPossibleDatabases();
        plugin.setDatabaseName("fixture - ead.xml");
        plugin.loadSelectedDatabase();
        IEadEntry root = plugin.getRootElement();
        root.setDisplayChildren(true);
        IEadEntry firstChild = root.getSubEntryList().get(0);
        firstChild.setDisplayChildren(true);
        IEadEntry firstInSecond = firstChild.getSubEntryList().get(0);
        firstInSecond.setDisplayChildren(true);
        IEadEntry secondInSecond = firstChild.getSubEntryList().get(1);
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
        assertEquals("", plugin.getDisplayMode());
    }

    @Test
    public void testMoveNodeDown() {
        LockingBean.resetAllLocks();
        ArchiveManagementAdministrationPlugin plugin = new ArchiveManagementAdministrationPlugin();
        plugin.setTestMode(true);
        plugin.getPossibleDatabases();
        plugin.setDatabaseName("fixture - ead.xml");
        plugin.loadSelectedDatabase();
        IEadEntry root = plugin.getRootElement();
        root.setDisplayChildren(true);
        IEadEntry firstChild = root.getSubEntryList().get(0);
        firstChild.setDisplayChildren(true);
        IEadEntry firstInSecond = firstChild.getSubEntryList().get(0);
        firstInSecond.setDisplayChildren(true);
        IEadEntry lastInSecond = firstChild.getSubEntryList().get(1);
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
        assertEquals("", plugin.getDisplayMode());
    }

    @Test
    public void testMoveHierarchyDown() {
        Part part = prepareFileUpload();

        LockingBean.resetAllLocks();
        ArchiveManagementAdministrationPlugin plugin = new ArchiveManagementAdministrationPlugin();
        plugin.setDatabaseName("sample");
        plugin.setUploadFile(part);
        plugin.upload();
        IEadEntry root = plugin.getRootElement();
        root.setDisplayChildren(true);
        IEadEntry firstChild = root.getSubEntryList().get(0);
        firstChild.setDisplayChildren(true);
        IEadEntry firstInSecond = firstChild.getSubEntryList().get(0);
        firstInSecond.setDisplayChildren(true);
        IEadEntry secondInSecond = firstChild.getSubEntryList().get(4);
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
        assertEquals("", plugin.getDisplayMode());
    }

    @Test
    public void testMoveHierarchyUp() {
        Part part = prepareFileUpload();

        LockingBean.resetAllLocks();
        ArchiveManagementAdministrationPlugin plugin = new ArchiveManagementAdministrationPlugin();
        plugin.setDatabaseName("sample");
        plugin.setUploadFile(part);
        plugin.upload();
        IEadEntry root = plugin.getRootElement();
        root.setDisplayChildren(true);
        IEadEntry firstChild = root.getSubEntryList().get(0);
        firstChild.setDisplayChildren(true);
        IEadEntry firstInSecond = firstChild.getSubEntryList().get(0);
        firstInSecond.setDisplayChildren(true);
        IEadEntry thirdInSecond = firstChild.getSubEntryList().get(2);
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
        assertEquals(2, thirdInSecond.getOrderNumber().intValue());
        assertEquals(2, thirdInSecond.getHierarchy().intValue());
        plugin.moveHierarchyUp();
        plugin.getFlatEntryList();
        assertEquals("", plugin.getDisplayMode());
    }

    @Test
    public void testSimpleSearch() {

        ArchiveManagementAdministrationPlugin plugin = new ArchiveManagementAdministrationPlugin();
        plugin.getPossibleDatabases();
        plugin.setDatabaseName("fixture - ead.xml");
        plugin.loadSelectedDatabase();

        assertEquals(3, plugin.getFlatEntryList().size());
        plugin.setSearchValue("Milzbrand");
        plugin.setRecordGroup(new RecordGroup(1, "sample"));
        plugin.search();
        assertEquals(5, plugin.getFlatEntryList().size());

        plugin.setSearchValue("");
        plugin.search();
        assertEquals(3, plugin.getFlatEntryList().size());
    }

    @Test
    public void testCreateNewDatabase() {
        LockingBean.resetAllLocks();
        ArchiveManagementAdministrationPlugin plugin = new ArchiveManagementAdministrationPlugin();
        plugin.getPossibleDatabases();
        plugin.createNewDatabase();
        // do nothing because databaseName is not set
        assertNull(plugin.getRootElement());
        plugin.setDatabaseName("");
        plugin.createNewDatabase();
        assertNull(plugin.getRootElement());
        // now it is set
        plugin.setDatabaseName("db");
        plugin.createNewDatabase();

        assertNotNull(plugin.getRootElement());

    }

    @Test
    public void testCancelEdition() {
        LockingBean.resetAllLocks();
        ArchiveManagementAdministrationPlugin plugin = new ArchiveManagementAdministrationPlugin();
        plugin.getPossibleDatabases();
        plugin.setDatabaseName("fixture - ead.xml");
        plugin.loadSelectedDatabase();
        plugin.getFlatEntryList();
        IEadEntry entry = plugin.getRootElement();
        plugin.setDisplayMode("something");
        plugin.setSearchValue("something");

        plugin.setDisplayIdentityStatementArea(true);
        plugin.setDisplayContextArea(true);
        plugin.setDisplayContentArea(true);
        plugin.setDisplayAccessArea(true);
        plugin.setDisplayMaterialsArea(true);
        plugin.setDisplayNotesArea(true);
        plugin.setDisplayControlArea(true);

        assertEquals("fixture - ead.xml", plugin.getDatabaseName());
        assertEquals(entry, plugin.getRootElement());
        assertEquals("something", plugin.getDisplayMode());
        assertNotNull(plugin.getFlatEntryList());
        assertEquals("something", plugin.getSearchValue());

        assertTrue(plugin.isDisplayIdentityStatementArea());
        assertTrue(plugin.isDisplayContextArea());
        assertTrue(plugin.isDisplayContentArea());
        assertTrue(plugin.isDisplayAccessArea());
        assertTrue(plugin.isDisplayMaterialsArea());
        assertTrue(plugin.isDisplayNotesArea());
        assertTrue(plugin.isDisplayControlArea());

        plugin.cancelEdition();
        assertNull(plugin.getDatabaseName());
        assertNull(plugin.getSelectedEntry());
        assertNull(plugin.getRootElement());
        assertEquals("", plugin.getDisplayMode());
        assertNull(plugin.getFlatEntryList());

        assertNull(plugin.getSearchValue());
        assertFalse(plugin.isDisplayIdentityStatementArea());
        assertFalse(plugin.isDisplayContextArea());
        assertFalse(plugin.isDisplayContentArea());
        assertFalse(plugin.isDisplayAccessArea());
        assertFalse(plugin.isDisplayMaterialsArea());
        assertFalse(plugin.isDisplayNotesArea());
        assertFalse(plugin.isDisplayControlArea());

    }

    @Test
    public void testValidateArchive() { //NOSONAR
        Part part = prepareFileUpload();

        LockingBean.resetAllLocks();
        ArchiveManagementAdministrationPlugin plugin = new ArchiveManagementAdministrationPlugin();
        plugin.setDatabaseName("sample");
        plugin.setUploadFile(part);
        plugin.upload();
        plugin.getFlatEntryList();
        IEadEntry entry = plugin.getRootElement();
        entry.setDisplayChildren(true);
        IEadEntry firstChild = entry.getSubEntryList().get(0);
        firstChild.setDisplayChildren(true);

        IMetadataField rootMetadata = entry.getIdentityStatementAreaList().get(0);
        IMetadataField childMetadata = firstChild.getIdentityStatementAreaList().get(0);
        assertEquals(rootMetadata.getName(), childMetadata.getName());
        IFieldValue rootValue = rootMetadata.getValues().get(0);
        IFieldValue childValue = childMetadata.getValues().get(0);
        // no validation rules - fields are valid
        plugin.validateArchive();
        assertTrue(rootMetadata.isValid());
        assertTrue(childMetadata.isValid());
        // check unique
        rootMetadata.setValidationType("unique");
        childMetadata.setValidationType("unique");
        // different values
        rootValue.setValue("value 1");
        childValue.setValue("value 2");
        plugin.validateArchive();
        assertTrue(rootMetadata.isValid());
        assertTrue(childMetadata.isValid());

        // identical values
        childValue.setValue("value 1");
        plugin.validateArchive();
        assertTrue(rootMetadata.isValid());
        assertFalse(childMetadata.isValid());

        // check unique+required, with different values
        rootMetadata.setValidationType("unique+required");
        childMetadata.setValidationType("unique+required");

        // missing value
        rootValue.setValue("");
        childValue.setValue("value 2");
        plugin.validateArchive();
        assertFalse(rootMetadata.isValid());
        assertTrue(childMetadata.isValid());

        // different values
        rootValue.setValue("value 1");
        childValue.setValue("value 2");
        plugin.validateArchive();
        assertTrue(rootMetadata.isValid());
        assertTrue(childMetadata.isValid());

        // identical values
        childValue.setValue("value 1");
        plugin.validateArchive();
        assertTrue(rootMetadata.isValid());
        assertFalse(childMetadata.isValid());

        // check required
        rootMetadata.setValidationType("required");
        childMetadata.setValidationType("required");

        // missing value
        rootValue.setValue("");
        childValue.setValue("value 2");
        plugin.validateArchive();
        assertFalse(rootMetadata.isValid());
        assertTrue(childMetadata.isValid());

        // different values
        rootValue.setValue("value 1");
        childValue.setValue("value 2");
        plugin.validateArchive();
        assertTrue(rootMetadata.isValid());
        assertTrue(childMetadata.isValid());

        // identical values
        childValue.setValue("value 1");
        plugin.validateArchive();
        assertTrue(rootMetadata.isValid());
        assertTrue(childMetadata.isValid());

        // check regex
        rootMetadata.setValidationType("regex");
        rootMetadata.setRegularExpression("\\d{4}");

        // no value
        rootValue.setValue("");
        plugin.validateArchive();
        assertTrue(rootMetadata.isValid());
        // matching value
        rootValue.setValue("1234");
        plugin.validateArchive();
        assertTrue(rootMetadata.isValid());
        // wrong value
        rootValue.setValue("12345");
        plugin.validateArchive();
        assertFalse(rootMetadata.isValid());

        // check regex+required
        rootMetadata.setValidationType("regex+required");
        rootMetadata.setRegularExpression("\\d{4}");

        // no value
        rootValue.setValue("");
        plugin.validateArchive();
        assertFalse(rootMetadata.isValid());
        // matching value
        rootValue.setValue("1234");
        plugin.validateArchive();
        assertTrue(rootMetadata.isValid());
        // wrong value
        rootValue.setValue("12345");
        plugin.validateArchive();
        assertFalse(rootMetadata.isValid());
    }

    @Test
    public void testCheckExistenceOfSelectedFile() {
        LockingBean.resetAllLocks();
        Part part = prepareFileUpload();
        ArchiveManagementAdministrationPlugin plugin = new ArchiveManagementAdministrationPlugin();
        assertFalse(plugin.isFileToUploadExists());

        // uploaded file does not exist
        plugin.checkExistenceOfSelectedFile(null, null, part);
        assertFalse(plugin.isFileToUploadExists());

        // second attempt: uploaded file does exist
        plugin.checkExistenceOfSelectedFile(null, null, part);
        assertTrue(plugin.isFileToUploadExists());
    }

    @Test
    public void testUpdateAreaDisplay() { //NOSONAR
        ArchiveManagementAdministrationPlugin plugin = new ArchiveManagementAdministrationPlugin();

        // Initial checks to ensure all display flags are false
        assertFalse(plugin.isDisplayIdentityStatementArea());
        assertFalse(plugin.isDisplayContextArea());
        assertFalse(plugin.isDisplayContentArea());
        assertFalse(plugin.isDisplayAccessArea());
        assertFalse(plugin.isDisplayMaterialsArea());
        assertFalse(plugin.isDisplayNotesArea());
        assertFalse(plugin.isDisplayControlArea());

        // Test level 1
        plugin.updateAreaDisplay(1);
        assertTrue(plugin.isDisplayIdentityStatementArea());
        assertFalse(plugin.isDisplayContextArea());
        assertFalse(plugin.isDisplayContentArea());
        assertFalse(plugin.isDisplayAccessArea());
        assertFalse(plugin.isDisplayMaterialsArea());
        assertFalse(plugin.isDisplayNotesArea());
        assertFalse(plugin.isDisplayControlArea());

        // Reset and test level 2
        resetDisplayFlags(plugin);
        plugin.updateAreaDisplay(2);
        assertFalse(plugin.isDisplayIdentityStatementArea());
        assertTrue(plugin.isDisplayContextArea());
        assertFalse(plugin.isDisplayContentArea());
        assertFalse(plugin.isDisplayAccessArea());
        assertFalse(plugin.isDisplayMaterialsArea());
        assertFalse(plugin.isDisplayNotesArea());
        assertFalse(plugin.isDisplayControlArea());

        // Reset and test level 3
        resetDisplayFlags(plugin);
        plugin.updateAreaDisplay(3);
        assertFalse(plugin.isDisplayIdentityStatementArea());
        assertFalse(plugin.isDisplayContextArea());
        assertTrue(plugin.isDisplayContentArea());
        assertFalse(plugin.isDisplayAccessArea());
        assertFalse(plugin.isDisplayMaterialsArea());
        assertFalse(plugin.isDisplayNotesArea());
        assertFalse(plugin.isDisplayControlArea());

        // Reset and test level 4
        resetDisplayFlags(plugin);
        plugin.updateAreaDisplay(4);
        assertFalse(plugin.isDisplayIdentityStatementArea());
        assertFalse(plugin.isDisplayContextArea());
        assertFalse(plugin.isDisplayContentArea());
        assertTrue(plugin.isDisplayAccessArea());
        assertFalse(plugin.isDisplayMaterialsArea());
        assertFalse(plugin.isDisplayNotesArea());
        assertFalse(plugin.isDisplayControlArea());

        // Reset and test level 5
        resetDisplayFlags(plugin);
        plugin.updateAreaDisplay(5);
        assertFalse(plugin.isDisplayIdentityStatementArea());
        assertFalse(plugin.isDisplayContextArea());
        assertFalse(plugin.isDisplayContentArea());
        assertFalse(plugin.isDisplayAccessArea());
        assertTrue(plugin.isDisplayMaterialsArea());
        assertFalse(plugin.isDisplayNotesArea());
        assertFalse(plugin.isDisplayControlArea());

        // Reset and test level 6
        resetDisplayFlags(plugin);
        plugin.updateAreaDisplay(6);
        assertFalse(plugin.isDisplayIdentityStatementArea());
        assertFalse(plugin.isDisplayContextArea());
        assertFalse(plugin.isDisplayContentArea());
        assertFalse(plugin.isDisplayAccessArea());
        assertFalse(plugin.isDisplayMaterialsArea());
        assertTrue(plugin.isDisplayNotesArea());
        assertFalse(plugin.isDisplayControlArea());

        // Reset and test level 7
        resetDisplayFlags(plugin);
        plugin.updateAreaDisplay(7);
        assertFalse(plugin.isDisplayIdentityStatementArea());
        assertFalse(plugin.isDisplayContextArea());
        assertFalse(plugin.isDisplayContentArea());
        assertFalse(plugin.isDisplayAccessArea());
        assertFalse(plugin.isDisplayMaterialsArea());
        assertFalse(plugin.isDisplayNotesArea());
        assertTrue(plugin.isDisplayControlArea());

        // Reset and test default case (invalid level)
        resetDisplayFlags(plugin);
        plugin.updateAreaDisplay(0);
        assertFalse(plugin.isDisplayIdentityStatementArea());
        assertFalse(plugin.isDisplayContextArea());
        assertFalse(plugin.isDisplayContentArea());
        assertFalse(plugin.isDisplayAccessArea());
        assertFalse(plugin.isDisplayMaterialsArea());
        assertFalse(plugin.isDisplayNotesArea());
        assertFalse(plugin.isDisplayControlArea());
    }

    private void resetDisplayFlags(ArchiveManagementAdministrationPlugin plugin) {
        plugin.setDisplayIdentityStatementArea(false);
        plugin.setDisplayContextArea(false);
        plugin.setDisplayContentArea(false);
        plugin.setDisplayAccessArea(false);
        plugin.setDisplayMaterialsArea(false);
        plugin.setDisplayNotesArea(false);
        plugin.setDisplayControlArea(false);
    }

    private IEadEntry getSampleData() {
        IEadEntry rootNode = new EadEntry(0, 0);
        rootNode.setLabel("label");
        rootNode.setDatabaseId(1);
        rootNode.setDisplayChildren(true);
        IEadEntry firstChild = new EadEntry(0, 1);
        firstChild.setParentNode(rootNode);
        firstChild.setDatabaseId(2);
        rootNode.getSubEntryList().add(firstChild);
        {
            IEadEntry firstGrandChild = new EadEntry(0, 1);
            firstGrandChild.setParentNode(firstChild);
            firstGrandChild.setDatabaseId(3);
            firstChild.getSubEntryList().add(firstGrandChild);

            IEadEntry secondGrandChild = new EadEntry(1, 1);
            secondGrandChild.setParentNode(firstChild);
            secondGrandChild.setDatabaseId(4);
            firstChild.getSubEntryList().add(secondGrandChild);
        }
        IEadEntry secondChild = new EadEntry(1, 1);
        secondChild.setParentNode(rootNode);
        secondChild.setDatabaseId(5);
        rootNode.getSubEntryList().add(secondChild);

        {
            IEadEntry firstGrandChild = new EadEntry(0, 1);
            firstGrandChild.setParentNode(secondChild);
            firstGrandChild.setDatabaseId(6);
            secondChild.getSubEntryList().add(firstGrandChild);

            IEadEntry secondGrandChild = new EadEntry(1, 1);
            secondGrandChild.setParentNode(secondChild);
            secondGrandChild.setDatabaseId(7);
            secondChild.getSubEntryList().add(secondGrandChild);
        }

        return rootNode;
    }

    private Part prepareFileUpload() {
        Path eadSource = Paths.get(resourcesFolder + "EAD.XML");
        Part part = new Part() {
            @Override
            public void write(String fileName) throws IOException {
            }

            @Override
            public String getSubmittedFileName() {
                return "/path/to/sam ple.xml";
            }

            @Override
            public long getSize() {
                return 0;
            }

            @Override
            public String getName() {
                return getSubmittedFileName();
            }

            @Override
            public InputStream getInputStream() throws IOException {
                return Files.newInputStream(eadSource);
            }

            @Override
            public Collection<String> getHeaders(String name) {
                return null;
            }

            @Override
            public Collection<String> getHeaderNames() {
                return null;
            }

            @Override
            public String getHeader(String name) {
                return null;
            }

            @Override
            public String getContentType() {
                return null;
            }

            @Override
            public void delete() throws IOException {
            }
        };
        return part;
    }

    @Test
    public void testDuplicateNode() {

        ArchiveManagementAdministrationPlugin plugin = new ArchiveManagementAdministrationPlugin();
        plugin.getPossibleDatabases();
        plugin.setDatabaseName("fixture - ead.xml");
        plugin.loadSelectedDatabase();

        // Set the selected entry
        plugin.setSelectedEntry(selectedEntry);

        // Invoke the method
        plugin.duplicateNode();

        IEadEntry duplicatedNode = plugin.getSelectedEntry();
        assertNotNull(duplicatedNode);
    }

    @Test
    public void testLinkNodeList() {
        ArchiveManagementAdministrationPlugin plugin = new ArchiveManagementAdministrationPlugin();
        plugin.getPossibleDatabases();
        plugin.setDatabaseName("fixture - ead.xml");
        plugin.loadSelectedDatabase();

        // don't show node modal
        List<IEadEntry> fixture = plugin.getLinkNodeList();
        assertNull(fixture);

        plugin.setDisplayLinkedModal(true);

        fixture = plugin.getLinkNodeList();
        assertEquals(7, fixture.size());
    }

    @Test
    public void testShowAllFields() {
        ArchiveManagementAdministrationPlugin plugin = new ArchiveManagementAdministrationPlugin();
        plugin.getPossibleDatabases();
        plugin.setDatabaseName("fixture - ead.xml");
        plugin.loadSelectedDatabase();

        assertFalse(plugin.isDisplayControlArea());

        // no node selected
        plugin.showAllFields();
        assertFalse(plugin.isDisplayControlArea());

        // select a node
        plugin.setDisplayAllFields(false);
        plugin.setSelectedEntry(plugin.getRootElement());
        plugin.showAllFields();
        assertTrue(plugin.isDisplayControlArea());
    }

}
