package de.intranda.goobi.plugins;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.lang3.StringUtils;
import org.goobi.beans.Batch;
import org.goobi.beans.Process;
import org.goobi.beans.Project;
import org.goobi.beans.Step;
import org.goobi.beans.User;
import org.goobi.interfaces.IArchiveManagementAdministrationPlugin;
import org.goobi.interfaces.IConfiguration;
import org.goobi.interfaces.IEadEntry;
import org.goobi.interfaces.IFieldValue;
import org.goobi.interfaces.IMetadataField;
import org.goobi.interfaces.IMetadataGroup;
import org.goobi.interfaces.INodeType;
import org.goobi.interfaces.IParameter;
import org.goobi.interfaces.IProcessTemplate;
import org.goobi.interfaces.IRecordGroup;
import org.goobi.interfaces.IValue;
import org.goobi.model.CorporateValue;
import org.goobi.model.ExtendendValue;
import org.goobi.model.GroupValue;
import org.goobi.model.PersonValue;
import org.goobi.production.cli.helper.StringPair;
import org.goobi.production.enums.PluginType;
import org.jdom2.Attribute;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.Text;
import org.jdom2.filter.Filters;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;
import org.jdom2.xpath.XPathExpression;
import org.jdom2.xpath.XPathFactory;

import de.intranda.goobi.plugins.model.ArchiveManagementConfiguration;
import de.intranda.goobi.plugins.model.DuplicationConfiguration;
import de.intranda.goobi.plugins.model.DuplicationParameter;
import de.intranda.goobi.plugins.model.EadEntry;
import de.intranda.goobi.plugins.model.FieldValue;
import de.intranda.goobi.plugins.model.ProcessTemplate;
import de.intranda.goobi.plugins.model.RecordGroup;
import de.intranda.goobi.plugins.model.TitleComponent;
import de.intranda.goobi.plugins.persistence.ArchiveManagementManager;
import de.intranda.goobi.plugins.persistence.NodeInitializer;
import de.sub.goobi.config.ConfigurationHelper;
import de.sub.goobi.helper.BeanHelper;
import de.sub.goobi.helper.FacesContextHelper;
import de.sub.goobi.helper.Helper;
import de.sub.goobi.helper.ProcessTitleGenerator;
import de.sub.goobi.helper.ScriptThreadWithoutHibernate;
import de.sub.goobi.helper.StorageProvider;
import de.sub.goobi.helper.XmlTools;
import de.sub.goobi.helper.enums.ManipulationType;
import de.sub.goobi.helper.enums.StepStatus;
import de.sub.goobi.helper.exceptions.DAOException;
import de.sub.goobi.persistence.managers.MetadataManager;
import de.sub.goobi.persistence.managers.ProcessManager;
import de.sub.goobi.persistence.managers.ProjectManager;
import de.sub.goobi.validator.ExtendedDateTimeFormatLexer;
import de.sub.goobi.validator.ExtendedDateTimeFormatParser;
import io.goobi.workflow.api.vocabulary.APIException;
import io.goobi.workflow.api.vocabulary.helper.ExtendedVocabulary;
import io.goobi.workflow.api.vocabulary.helper.ExtendedVocabularyRecord;
import io.goobi.workflow.locking.LockingBean;
import io.goobi.workflow.xslt.XsltToPdf;
import jakarta.faces.component.UIComponent;
import jakarta.faces.context.FacesContext;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.Part;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.log4j.Log4j2;
import net.xeoh.plugins.base.annotations.PluginImplementation;
import ugh.dl.Corporate;
import ugh.dl.DigitalDocument;
import ugh.dl.DocStruct;
import ugh.dl.Fileformat;
import ugh.dl.Metadata;
import ugh.dl.MetadataGroup;
import ugh.dl.MetadataType;
import ugh.dl.NamePart;
import ugh.dl.Person;
import ugh.dl.Prefs;
import ugh.exceptions.DocStructHasNoTypeException;
import ugh.exceptions.MetadataTypeNotAllowedException;
import ugh.exceptions.UGHException;
import ugh.fileformats.mets.MetsMods;

@PluginImplementation
@Log4j2
public class ArchiveManagementAdministrationPlugin implements IArchiveManagementAdministrationPlugin {

    private static final long serialVersionUID = -6745728159636602782L;

    @Setter
    private boolean testMode;

    @Getter
    @Setter
    private String displayMode = "";

    @Getter
    private String title = "intranda_administration_archive_management";

    @Getter
    private PluginType type = PluginType.Administration;

    @Getter
    private String gui = "/uii/plugin_administration_archive_management.xhtml";

    @Getter
    @Setter
    private String databaseName;

    @Getter
    @Setter
    private transient IEadEntry rootElement = null;

    @Getter
    @Setter
    private transient RecordGroup recordGroup;

    private transient List<IEadEntry> flatEntryList;

    @Getter
    private transient IEadEntry selectedEntry;

    @Getter
    private transient List<IEadEntry> moveList;

    @Getter
    @Setter
    private transient IEadEntry destinationEntry;

    private static XPathFactory xFactory = XPathFactory.instance();

    @Getter
    @Setter
    private String searchValue;

    @Getter
    @Setter
    private boolean displayIdentityStatementArea;

    @Getter
    @Setter
    private boolean displayContextArea;

    @Getter
    @Setter
    private boolean displayContentArea;

    @Getter
    @Setter
    private boolean displayAccessArea;

    @Getter
    @Setter
    private boolean displayMaterialsArea;

    @Getter
    @Setter
    private boolean displayNotesArea;

    @Getter
    @Setter
    private boolean displayControlArea;

    @Setter
    private Process processTemplate;
    private BeanHelper bhelp = new BeanHelper();

    private SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    private String username = "";

    @Getter
    @Setter
    private transient Part uploadFile;

    private List<IProcessTemplate> processTemplates = new ArrayList<>();
    @Getter
    @Setter
    private String selectedTemplate;

    @Getter
    @Setter
    private String selectedProject;

    private List<StringPair> projects = new ArrayList<>();

    // true if the selected file to upload already has an older version in the same path, false otherwise
    @Getter
    @Setter
    private boolean fileToUploadExists = false;

    private transient IConfiguration duplicationConfiguration = null;

    @Getter
    @Setter
    private List<StringPair> advancedSearch = new ArrayList<>();

    @Getter
    @Setter
    private boolean displayLinkedModal = false;

    @Getter
    @Setter
    private boolean displayCreateNodesModal = false;

    @Getter
    @Setter
    private boolean displayVocabularyModal = false;

    @Getter
    @Setter
    private transient IFieldValue fieldToLink;

    @Getter
    @Setter
    private transient IMetadataField selectedGroup;

    private transient List<IEadEntry> linkNodeList;

    @Getter
    @Setter
    private String numberOfNodes;
    @Getter
    @Setter
    private String nodeType;

    private transient List<IParameter> metadataToAdd;

    private boolean readOnlyMode = true;

    @Getter
    private boolean allowProcessCreation = false;

    @Getter
    private boolean allowFileUpload = false;

    @Getter
    private boolean allowCreation = false;

    @Getter
    private boolean allowVocabularyEdition = false;

    @Getter
    private boolean allowDeletion = false;

    private boolean allowAllInventories;
    private List<String> inventoryList = new ArrayList<>();

    @Getter
    private transient ExtendedVocabularyRecord newRecord;

    @Getter
    @Setter
    private transient IMetadataField vocabularyField;

    @Getter
    @Setter
    private boolean displayAllFields = false;

    @Getter
    @Setter
    private transient ArchiveManagementConfiguration config;

    /**
     * Constructor
     */
    public ArchiveManagementAdministrationPlugin() {
        try {

            ArchiveManagementManager.createTables();

        } catch (APIException e) {
            // api is not running
        }
        advancedSearch = new ArrayList<>();
        advancedSearch.add(new StringPair());
        advancedSearch.add(new StringPair());
        advancedSearch.add(new StringPair());
        advancedSearch.add(new StringPair());
        advancedSearch.add(new StringPair());
        try {
            config = new ArchiveManagementConfiguration();

            User user = Helper.getCurrentUser();
            if (user != null) {
                username = user.getNachVorname();

                // role for read access: Plugin_Administration_Archive_Management
                // role for write access: Plugin_Administration_Archive_Management_Write
                // role for file upload access: Plugin_Administration_Archive_Management_Upload
                // role for creation access: Plugin_Administration_Archive_Management_New
                // role to edit vocabularies: Plugin_Administration_Archive_Management_Vocabulary
                // role to access all: Plugin_Administration_Archive_Management_All_Inventories
                // role to access the inventory 'XYZ':  Plugin_Administration_Archive_Management_Inventory_XYZ
                // role for process creation: Plugin_Administration_Archive_Management_Process
                // role to delete inventory: Plugin_Administration_Archive_Management_Delete

                if ((user.isSuperAdmin() || user.getAllUserRoles().contains("Plugin_Administration_Archive_Management_Write"))) {
                    readOnlyMode = false;
                }

                if (user.isSuperAdmin() || (user.getAllUserRoles().contains("Plugin_Administration_Archive_Management_Process"))) {
                    allowProcessCreation = true;
                }

                if (user.isSuperAdmin() || (user.getAllUserRoles().contains("Plugin_Administration_Archive_Management_Upload"))) {
                    allowFileUpload = true;
                }

                if (user.isSuperAdmin() || (user.getAllUserRoles().contains("Plugin_Administration_Archive_Management_New"))) {
                    allowCreation = true;
                }
                if (user.isSuperAdmin() || (user.getAllUserRoles().contains("Plugin_Administration_Archive_Management_Vocabulary"))) {
                    allowVocabularyEdition = true;
                }

                if (user.isSuperAdmin() || (user.getAllUserRoles().contains("Plugin_Administration_Archive_Management_Delete"))) {
                    allowDeletion = true;
                }

                if (user.isSuperAdmin() || (user.getAllUserRoles().contains("Plugin_Administration_Archive_Management_All_Inventories"))) {
                    allowAllInventories = true;
                } else {
                    for (String role : user.getAllUserRoles()) {
                        if (role.startsWith("Plugin_Administration_Archive_Management_Inventory_")) {
                            inventoryList.add(role.replace("Plugin_Administration_Archive_Management_Inventory_", ""));
                        }
                    }
                }
            }
            config.readExportConfiguration();
            displayIdentityStatementArea = config.isDisplayIdentityStatementArea();
            displayContextArea = config.isDisplayContextArea();
            displayContentArea = config.isDisplayContentArea();
            displayAccessArea = config.isDisplayAccessArea();
            displayMaterialsArea = config.isDisplayMaterialsArea();
            displayNotesArea = config.isDisplayNotesArea();
            displayControlArea = config.isDisplayControlArea();

        } catch (ConfigurationException e2) {
            log.error(e2);
        }
    }

    public String checkDBConnection() {
        getPossibleDatabases();
        return null;
    }

    /**
     * fieldType Get the database names and file names from the basex databases
     * 
     * @return
     */

    @Override
    public List<String> getPossibleDatabases() {
        List<IRecordGroup> allRecordGroups = getRecordGroups();

        List<String> databases = new ArrayList<>();
        for (IRecordGroup rec : allRecordGroups) {
            // allow access if username is null (api request), access is granted to all or to the specific record
            if (StringUtils.isBlank(username) || allowAllInventories || inventoryList.contains(rec.getTitle())) {
                databases.add(rec.getTitle());
            }
        }
        return databases;
    }

    @Override
    public List<IRecordGroup> getRecordGroups() {
        return ArchiveManagementManager.getAllRecordGroups();
    }

    /**
     * Get the database names without file names from the basex databases
     * 
     * @return
     */

    public List<String> getPossibleDatabaseNames() {
        List<String> databases = new ArrayList<>();
        List<IRecordGroup> allRecordGroups = getRecordGroups();

        for (IRecordGroup rec : allRecordGroups) {
            databases.add(rec.getTitle());
        }

        if (databases.isEmpty()) {
            Helper.setFehlerMeldung("plugin_administration_archive_databaseMustBeCreated");
        }

        return databases;
    }

    /**
     * open the selected database and load the file
     */

    @Override
    public void loadSelectedDatabase() {

        try {
            // open selected database
            if (StringUtils.isNotBlank(databaseName)) {
                recordGroup = ArchiveManagementManager.getRecordGroupByTitle(databaseName);
                // get field definitions from config file
                config.readConfiguration(databaseName);
                rootElement = ArchiveManagementManager.loadRecordGroup(recordGroup.getId());
                loadMetadataForNode(rootElement);
                rootElement.setDisplayChildren(true);
            } else {
                Helper.setFehlerMeldung("plugin_administration_archive_creation_noRecordGroupSelected");

                //this may write an error message if necessary
                if (!getPossibleDatabaseNames().isEmpty()) {
                    List<String> databases = getPossibleDatabases();

                    if (databases.isEmpty()) {
                        Helper.setFehlerMeldung("plugin_administration_archive_databaseFileMustBeCreated");
                    }
                }
                databaseName = null;
            }
        } catch (Exception e) {
            log.error(e);
            Helper.setFehlerMeldung("plugin_administration_archive_databaseCannotBeLoaded");
            databaseName = null;
        }
    }

    @Override
    public void createNewDatabase() {
        if (StringUtils.isNotBlank(databaseName)) {

            config.readConfiguration(databaseName);
            if (ArchiveManagementManager.getRecordGroupByTitle(databaseName) != null) {
                Helper.setFehlerMeldung("plugin_administration_archive_recordGroupExists");
                databaseName = null;
                displayMode = "createArchive";
                return;
            }

            recordGroup = new RecordGroup();
            recordGroup.setTitle(databaseName);
            ArchiveManagementManager.saveRecordGroup(recordGroup);

            rootElement = new EadEntry(0, 0);
            rootElement.setId("id_" + UUID.randomUUID());

            rootElement.setDisplayChildren(true);
            for (INodeType type : config.getConfiguredNodes()) {
                if (type.isRootNode()) {
                    rootElement.setNodeType(type);
                    break;
                }
            }

            ArchiveManagementManager.saveNode(recordGroup.getId(), rootElement);
            loadMetadataForNode(rootElement);

            selectedEntry = rootElement;

            displayMode = "";
            getDuplicationConfiguration();
        } else {
            //this may write an error message if necessary
            List<String> databases = getPossibleDatabaseNames();

            if (!databases.isEmpty() && StringUtils.isBlank(databaseName)) {
                Helper.setFehlerMeldung("plugin_administration_archive_creation_selectDatabase");
            }
        }
    }

    /*
     * get root node from ead document
     */
    public void parseEadFile(Document document, Integer recordGroupId) {
        Element eadElement = null;
        Element collection = document.getRootElement();
        if ("collection".equals(collection.getName())) {
            eadElement = collection.getChild("ead", config.getNameSpaceRead());
        } else {
            eadElement = collection;
        }
        rootElement = parseElement(0, 0, eadElement, recordGroupId, null);
        for (INodeType type : config.getConfiguredNodes()) {
            if (type.isRootNode()) {
                rootElement.setNodeType(type);
                break;
            }
        }
        rootElement.setDisplayChildren(true);

        getDuplicationConfiguration();
    }

    /*
     * get root node from ead document
     */
    public void parseEadFile(Document document) {
        parseEadFile(document, null);
    }

    /**
     * read the metadata for the current xml node. - create an {@link EadEntry} - execute the configured xpaths on the current node - add the metadata
     * to one of the 7 levels - check if the node has sub nodes - call the method recursively for all sub nodes
     */
    private IEadEntry parseElement(int order, int hierarchy, Element element, Integer recordGroupId, IEadEntry parent) {
        IEadEntry entry = new EadEntry(order, hierarchy);

        for (IMetadataField emf : config.getConfiguredFields()) {
            if (emf.isGroup()) {
                // find group root element
                XPathExpression<Element> engine = xFactory.compile(emf.getXpath(), Filters.element(), null, config.getNameSpaceRead());
                List<IValue> groups = new ArrayList<>();
                List<Element> values = engine.evaluate(element);
                for (Element groupElement : values) {

                    GroupValue gv = new GroupValue();
                    groups.add(gv);
                    gv.setGroupName(emf.getName());
                    // for each sub element
                    for (IMetadataField sub : emf.getSubfields()) {
                        List<IValue> valueList = getValuesFromXml(groupElement, sub);
                        gv.getSubfields().put(sub.getName(), valueList);
                    }
                }
                NodeInitializer.loadGroupMetadata(entry, emf, groups);
            } else {
                List<IValue> valueList = getValuesFromXml(element, emf);
                IMetadataField toAdd = NodeInitializer.addFieldToEntry(entry, emf, valueList);
                NodeInitializer.addFieldToNode(entry, toAdd);
            }
        }

        Element eadheader = element.getChild("eadheader", config.getNameSpaceRead());

        entry.setId(element.getAttributeValue("id"));
        if (eadheader != null) {
            try {
                Element filedesc = eadheader.getChild("filedesc", config.getNameSpaceRead());
                if (filedesc != null) {
                    Element titlestmt = filedesc.getChild("titlestmt", config.getNameSpaceRead());
                    if (titlestmt != null) {
                        String titleproper = titlestmt.getChildText("titleproper", config.getNameSpaceRead());
                        entry.setLabel(titleproper);
                    }
                }
            } catch (Exception e) {
                log.error("Error while setting the label", e);
            }
        }

        // nodeType
        // get child elements
        List<Element> clist = null;
        Element archdesc = element.getChild("archdesc", config.getNameSpaceRead());
        if (archdesc != null) {
            String nodeTypeName = archdesc.getAttributeValue("localtype");
            for (INodeType nt : config.getConfiguredNodes()) {
                if (nt.getNodeName().equals(nodeTypeName)) {
                    entry.setNodeType(nt);
                }
            }
            Element dsc = archdesc.getChild("dsc", config.getNameSpaceRead());
            if (dsc != null) {
                // read process title
                List<Element> altformavailList = dsc.getChildren("altformavail", config.getNameSpaceRead());
                for (Element altformavail : altformavailList) {
                    if ("goobi_process".equals(altformavail.getAttributeValue("localtype"))) {
                        entry.setGoobiProcessTitle(altformavail.getText());
                    }
                }
                clist = dsc.getChildren("c", config.getNameSpaceRead());
            }

        } else {
            String nodeTypeName = element.getAttributeValue("otherlevel");
            for (INodeType nt : config.getConfiguredNodes()) {
                if (nt.getNodeName().equals(nodeTypeName)) {
                    entry.setNodeType(nt);
                }
            }
            List<Element> altformavailList = element.getChildren("altformavail", config.getNameSpaceRead());
            for (Element altformavail : altformavailList) {
                if ("goobi_process".equals(altformavail.getAttributeValue("localtype"))) {
                    entry.setGoobiProcessTitle(altformavail.getText());
                }
            }
        }
        if (entry.getNodeType() == null) {
            entry.setNodeType(config.getConfiguredNodes().get(0));
        }
        // generate new id, if id is null
        if (entry.getId() == null) {
            entry.setId("id_" + UUID.randomUUID());
        }

        entry.calculateFingerprint();

        if (parent != null) {
            entry.setParentNode(parent);
        }

        if (recordGroupId != null) {
            // save current node
            ArchiveManagementManager.saveNode(recordGroupId, entry);
            // clear saved metadata to reduce memory usage
            entry.getIdentityStatementAreaList().clear();
            entry.getContextAreaList().clear();
            entry.getContentAndStructureAreaAreaList().clear();
            entry.getAccessAndUseAreaList().clear();
            entry.getAlliedMaterialsAreaList().clear();
            entry.getNotesAreaList().clear();
            entry.getDescriptionControlAreaList().clear();
        }

        if (clist == null) {
            clist = element.getChildren("c", config.getNameSpaceRead());
        }
        if (clist != null) {
            int subOrder = 0;
            int subHierarchy = hierarchy + 1;
            for (Element c : clist) {

                IEadEntry child = parseElement(subOrder, subHierarchy, c, recordGroupId, entry);
                entry.addSubEntry(child);
                subOrder++;
            }
        }

        return entry;
    }

    private List<IValue> getValuesFromXml(Element element, IMetadataField emf) {
        List<IValue> valueList = new ArrayList<>();
        if ("text".equalsIgnoreCase(emf.getXpathType())) {
            XPathExpression<Text> engine = xFactory.compile(emf.getXpath(), Filters.text(), null, config.getNameSpaceRead());
            List<Text> values = engine.evaluate(element);
            if (emf.isRepeatable()) {
                for (Text value : values) {
                    String stringValue = value.getValue();
                    valueList.add(new ExtendendValue(emf.getName(), stringValue, null, null));
                }
            } else if (!values.isEmpty()) {
                Text value = values.get(0);
                String stringValue = value.getValue();
                valueList.add(new ExtendendValue(emf.getName(), stringValue, null, null));
            }
        } else if ("attribute".equalsIgnoreCase(emf.getXpathType())) {
            XPathExpression<Attribute> engine = xFactory.compile(emf.getXpath(), Filters.attribute(), null, config.getNameSpaceRead());
            List<Attribute> values = engine.evaluate(element);

            if (emf.isRepeatable()) {
                for (Attribute value : values) {
                    String stringValue = value.getValue();
                    valueList.add(new ExtendendValue(emf.getName(), stringValue, null, null));
                }
            } else if (!values.isEmpty()) {
                Attribute value = values.get(0);
                String stringValue = value.getValue();
                valueList.add(new ExtendendValue(emf.getName(), stringValue, null, null));
            }
        } else if ("person".equals(emf.getFieldType())) {
            XPathExpression<Element> engine = xFactory.compile(emf.getXpath(), Filters.element(), null, config.getNameSpaceRead());
            List<Element> values = engine.evaluate(element);
            if (emf.isRepeatable()) {
                for (Element value : values) {
                    PersonValue pv = getPersonFromElement(emf, value);
                    if (pv != null) {
                        valueList.add(pv);
                    }
                }
            } else {
                Element value = values.get(0);
                PersonValue pv = getPersonFromElement(emf, value);
                if (pv != null) {
                    valueList.add(pv);
                }
            }
        } else if ("corporate".equals(emf.getFieldType())) {
            XPathExpression<Element> engine = xFactory.compile(emf.getXpath(), Filters.element(), null, config.getNameSpaceRead());
            List<Element> values = engine.evaluate(element);
            if (emf.isRepeatable()) {
                for (Element value : values) {
                    CorporateValue pv = getCorporateFromElement(emf, value);
                    if (pv != null) {
                        valueList.add(pv);
                    }
                }
            } else {
                Element value = values.get(0);
                CorporateValue pv = getCorporateFromElement(emf, value);
                if (pv != null) {
                    valueList.add(pv);
                }
            }
        } else {
            XPathExpression<Element> engine = xFactory.compile(emf.getXpath(), Filters.element(), null, config.getNameSpaceRead());
            List<Element> values = engine.evaluate(element);
            if (emf.isRepeatable()) {
                for (Element value : values) {
                    String authorityType = value.getAttributeValue("SOURCE");
                    String authorityValue = value.getAttributeValue("AUTHFILENUMBER");
                    String stringValue = value.getValue();
                    valueList.add(new ExtendendValue(emf.getName(), stringValue, authorityType, authorityValue));
                }
            } else if (!values.isEmpty()) {
                Element value = values.get(0);
                String authorityType = value.getAttributeValue("SOURCE");
                String authorityValue = value.getAttributeValue("AUTHFILENUMBER");
                String stringValue = value.getValue();
                valueList.add(new ExtendendValue(emf.getName(), stringValue, authorityType, authorityValue));
            }
        }
        return valueList;
    }

    private PersonValue getPersonFromElement(IMetadataField field, Element element) {
        List<String> lastnameValues =
                getValuesFromElement(field.getSubfieldMap().get("lastnameXpathType"), field.getSubfieldMap().get("lastnameXpath"), element);
        List<String> firstnameValues =
                getValuesFromElement(field.getSubfieldMap().get("firstnameXpathType"), field.getSubfieldMap().get("firstnameXpath"), element);
        List<String> idValues = getValuesFromElement(field.getSubfieldMap().get("authorityValueXpathType"),
                field.getSubfieldMap().get("authorityValueXpath"), element);
        String lastname = null;
        String firstname = null;
        String id = null;
        if (!lastnameValues.isEmpty()) {
            lastname = lastnameValues.get(0);
        }
        if (!firstnameValues.isEmpty()) {
            firstname = firstnameValues.get(0);
        }
        String idType = "";
        if (!idValues.isEmpty()) {
            id = idValues.get(0);
            if (id.contains("gnd")) {
                idType = "gnd";
            } else if (id.contains("viaf")) {
                idType = "viaf";
            } else {
                idType = "-";
            }
        }
        if (StringUtils.isNotBlank(firstname) || StringUtils.isNotBlank(lastname)) {
            return new PersonValue(field.getName(), firstname, lastname, idType, id);
        }
        return null;
    }

    private CorporateValue getCorporateFromElement(IMetadataField field, Element element) {

        List<String> mainValues =
                getValuesFromElement(field.getSubfieldMap().get("mainValueXpathType"), field.getSubfieldMap().get("mainValueXpath"), element);
        List<String> subnameValues =
                getValuesFromElement(field.getSubfieldMap().get("subValueXpathType"), field.getSubfieldMap().get("subValueXpath"), element);
        List<String> partnameValues =
                getValuesFromElement(field.getSubfieldMap().get("partValueXpathType"), field.getSubfieldMap().get("partValueXpath"), element);

        List<String> idValues = getValuesFromElement(field.getSubfieldMap().get("authorityValueXpathType"),
                field.getSubfieldMap().get("authorityValueXpath"), element);

        String mainName = null;
        String subName = null;
        String partName = null;
        String id = null;
        if (!mainValues.isEmpty()) {
            mainName = mainValues.get(0);
        }
        if (!subnameValues.isEmpty()) {
            subName = subnameValues.get(0);
        }
        if (!partnameValues.isEmpty()) {
            partName = partnameValues.get(0);
        }

        String idType = "";
        if (!idValues.isEmpty()) {
            id = idValues.get(0);
            if (id.contains("gnd")) {
                idType = "gnd";
            } else if (id.contains("viaf")) {
                idType = "viaf";
            } else {
                idType = "-";
            }
        }
        if (StringUtils.isNotBlank(mainName)) {
            return new CorporateValue(field.getName(), mainName, subName, partName, idType, id);
        }
        return null;
    }

    private List<String> getValuesFromElement(String xpathType, String xpath, Element element) {
        List<String> answer = new ArrayList<>();

        if ("text".equalsIgnoreCase(xpathType)) {
            XPathExpression<Text> engine =
                    xFactory.compile(xpath, Filters.text(), null, config.getNameSpaceRead());
            List<Text> values = engine.evaluate(element);
            for (Text value : values) {
                String stringValue = value.getValue();
                answer.add(stringValue);
            }
        } else if ("attribute".equalsIgnoreCase(xpathType)) {
            XPathExpression<Attribute> engine =
                    xFactory.compile(xpath, Filters.attribute(), null, config.getNameSpaceRead());
            List<Attribute> values = engine.evaluate(element);
            for (Attribute val : values) {
                String stringValue = val.getValue();
                answer.add(stringValue);
            }
        } else {
            XPathExpression<Element> engine = xFactory.compile(xpath, Filters.element(), null, config.getNameSpaceRead());
            List<Element> values = engine.evaluate(element);
            for (Element val : values) {
                String stringValue = val.getValue();
                answer.add(stringValue);
            }
        }
        return answer;
    }

    public void resetFlatList() {
        flatEntryList = null;
    }

    /**
     * Get the hierarchical tree as a flat list
     * 
     * @return
     */

    public List<IEadEntry> getFlatEntryList() {
        if (flatEntryList == null && rootElement != null) {
            flatEntryList = new LinkedList<>();
            flatEntryList.addAll(rootElement.getAsFlatList());
        }
        return flatEntryList;
    }

    @Override
    public void setSelectedEntry(IEadEntry entry) {

        if (selectedEntry != null) {
            // auto save current node - if not, remove this line
            updateSingleNode();
            selectedEntry.setSelected(false);
            // unlock last entry and its children
            List<IEadEntry> entriesToUnlock = selectedEntry.getAllNodes();
            for (IEadEntry e : entriesToUnlock) {
                LockingBean.freeObject(e.getId());
            }
        }
        // deselect the old entry
        selectedEntry = null;

        // replace the node with the latest version from basex
        IEadEntry updatedEntry = updateNode(entry);
        if (updatedEntry == null) {
            return;
        }
        entry = updatedEntry;
        entry.setSelected(true);

        // check if entry or any of its children is locked by another person
        List<IEadEntry> entriesToLock = entry.getAllNodes();
        for (IEadEntry e : entriesToLock) {
            if (LockingBean.isLockedByAnotherUser(e.getId(), username)) {
                Helper.setFehlerMeldung("Someone is working on this node or its children");
                return;
            }
        }

        // lock new node, if no-one is working on it or its children
        for (IEadEntry e : entriesToLock) {
            if (!LockingBean.lockObject(e.getId(), username)) {
                // this error cannot not occur
            }
        }

        this.selectedEntry = entry;
    }

    private IEadEntry updateNode(IEadEntry entry) {
        if (!testMode) {
            loadMetadataForNode(entry);
        }
        return entry;
    }

    @Override
    public void addNode() {
        if (selectedEntry != null) {
            EadEntry entry =
                    new EadEntry(selectedEntry.isHasChildren() ? selectedEntry.getSubEntryList().size() : 0, selectedEntry.getHierarchy() + 1);
            entry.setId("id_" + UUID.randomUUID());
            // initial metadata values
            List<IValue> titleData = new ArrayList<>();
            if (StringUtils.isNotBlank(config.getNodeDefaultTitle())) {
                titleData.add(new ExtendendValue(null, config.getNodeDefaultTitle(), null, null));
                entry.setLabel(config.getNodeDefaultTitle());
            }
            for (IMetadataField emf : config.getConfiguredFields()) {
                if (emf.isGroup()) {
                    NodeInitializer.loadGroupMetadata(entry, emf, null);
                } else if (emf.getXpath().contains("unittitle")) {
                    IMetadataField toAdd = NodeInitializer.addFieldToEntry(entry, emf, titleData);
                    NodeInitializer.addFieldToNode(entry, toAdd);
                } else {
                    NodeInitializer.addFieldToEntry(entry, emf, null);
                }
            }
            entry.setNodeType(selectedEntry.getNodeType());
            selectedEntry.addSubEntry(entry);
            selectedEntry.setDisplayChildren(true);
            selectedEntry.calculateFingerprint();
            ArchiveManagementManager.saveNode(recordGroup.getId(), entry);
            setSelectedEntry(entry);

            resetFlatList();
        }
    }

    @Override
    public void deleteNode() {
        if (selectedEntry != null) {
            IEadEntry parentNode = selectedEntry.getParentNode();
            if (parentNode == null) {
                // abort, root node cannot be deleted
                return;
            }

            // remove the current node from parent
            parentNode.removeSubEntry(selectedEntry);

            // get the current node and all of its children
            List<IEadEntry> nodesToDelete = selectedEntry.getAllNodes();

            // delete elements in the database
            ArchiveManagementManager.deleteNodes(nodesToDelete);

            ArchiveManagementManager.updateNodeHierarchy(recordGroup.getId(), parentNode.getAllNodes());
            selectedEntry = null;
            // select the parent node
            setSelectedEntry(parentNode);
            flatEntryList = null;

        }

    }

    /**
     * upload the selected file
     */
    public void upload() {
        if (uploadFile == null) {
            Helper.setFehlerMeldung("plugin_administration_archive_missing_Data");
            return;
        }

        String uploadedFileName = processUploadedFileName(uploadFile);
        databaseName = uploadedFileName;
        config.readConfiguration(databaseName);

        // open document, parse it
        try (InputStream input = uploadFile.getInputStream()) {
            Document document = XmlTools.readDocumentFromStream(input);
            if (document != null) {

                Element mainElement = document.getRootElement();
                if ("collection".equals(mainElement.getName())) {
                    mainElement = mainElement.getChildren().get(0);
                }

                if (!"ead".equals(mainElement.getName())) {
                    // file is not an ead file
                    String message = "plugin_administration_archive_notEadFile";
                    throw new FileNotFoundException(message);
                }
                // check, if uploaded file was used before
                recordGroup = ArchiveManagementManager.getRecordGroupByTitle(uploadedFileName);
                if (recordGroup == null) {
                    recordGroup = new RecordGroup();
                    recordGroup.setTitle(uploadedFileName);
                } else {
                    // replace existing records
                    ArchiveManagementManager.deleteAllNodes(recordGroup.getId());
                }
                ArchiveManagementManager.saveRecordGroup(recordGroup);
                parseEadFile(document, recordGroup.getId());
            }
        } catch (IOException e) {
            log.error(e);
        }

        // save nodes
        databaseName = recordGroup.getTitle();
        displayMode = "";
        // update existing process ids if the uploaded EAD file has an older version
        if (fileToUploadExists) {
            log.debug("updating existing processes' ids");
            updateGoobiIds();
        }
    }

    /**
     * check the existence of the selected file and save it to the private field fileToUploadExists
     * 
     * @param ctx FacesContext
     * @param comp UIComponent
     * @param value Object
     */
    public void checkExistenceOfSelectedFile(FacesContext ctx, UIComponent comp, Object value) {
        log.debug("checking existence of the selected file");
        Part file = (Part) value;

        String uploadedFileName = processUploadedFileName(file);

        IRecordGroup rg = ArchiveManagementManager.getRecordGroupByTitle(uploadedFileName);

        fileToUploadExists = rg != null;
    }

    /**
     * process the file name of the selected file in such way that the file name is assured to contain no white spaces as well as it has the correct
     * extension .xml
     * 
     * @param file the selected file as a jakarta.servlet.http.Part object
     * @return the corrected file name as a string
     */
    private String processUploadedFileName(Part file) {
        if (file == null) {
            return "";
        }

        String uploadedFileName = Paths.get(file.getSubmittedFileName()).getFileName().toString(); // MSIE fix.

        // filename must end with xml
        if (!uploadedFileName.toLowerCase().endsWith(".xml")) {
            uploadedFileName = uploadedFileName + ".xml";
        }
        // remove whitespaces from filename
        uploadedFileName = uploadedFileName.replace(" ", "_");

        return uploadedFileName;
    }

    public void addMetadata(Element currentElement, IEadEntry node, Element xmlRootElement, boolean updateHistory) {
        boolean isMainElement = false;

        Map<String, List<IValue>> metadata = ArchiveManagementManager.convertStringToMap(node.getData());
        node.getIdentityStatementAreaList().clear();
        node.getContextAreaList().clear();
        node.getContentAndStructureAreaAreaList().clear();
        node.getAccessAndUseAreaList().clear();
        node.getAlliedMaterialsAreaList().clear();
        node.getNotesAreaList().clear();
        node.getDescriptionControlAreaList().clear();

        for (IMetadataField emf : config.getConfiguredFields()) {
            List<IValue> values = metadata.get(emf.getName());
            if (emf.isGroup()) {
                NodeInitializer.loadGroupMetadata(node, emf, values);

            } else {
                IMetadataField toAdd = NodeInitializer.addFieldToEntry(node, emf, values);
                NodeInitializer.addFieldToNode(node, toAdd);
            }
        }

        if ("ead".equals(currentElement.getName())) {
            isMainElement = true;
            if (updateHistory) {
                updateChangeHistory(node);
            }
        }

        for (IMetadataField emf : node.getIdentityStatementAreaList()) {
            createEadElement(currentElement, isMainElement, emf, xmlRootElement);
        }
        for (IMetadataField emf : node.getContextAreaList()) {
            createEadElement(currentElement, isMainElement, emf, xmlRootElement);
        }
        for (IMetadataField emf : node.getContentAndStructureAreaAreaList()) {
            createEadElement(currentElement, isMainElement, emf, xmlRootElement);
        }
        for (IMetadataField emf : node.getAccessAndUseAreaList()) {
            createEadElement(currentElement, isMainElement, emf, xmlRootElement);
        }
        for (IMetadataField emf : node.getAlliedMaterialsAreaList()) {
            createEadElement(currentElement, isMainElement, emf, xmlRootElement);
        }
        for (IMetadataField emf : node.getNotesAreaList()) {
            createEadElement(currentElement, isMainElement, emf, xmlRootElement);
        }
        for (IMetadataField emf : node.getDescriptionControlAreaList()) {
            createEadElement(currentElement, isMainElement, emf, xmlRootElement);
        }

        node.getIdentityStatementAreaList().clear();
        node.getContextAreaList().clear();
        node.getContentAndStructureAreaAreaList().clear();
        node.getAccessAndUseAreaList().clear();
        node.getAlliedMaterialsAreaList().clear();
        node.getNotesAreaList().clear();
        node.getDescriptionControlAreaList().clear();

        Element dsc = null;
        if (isMainElement) {
            if (StringUtils.isNotBlank(node.getId())) {
                currentElement.setAttribute("id", node.getId());
            }
            Element archdesc = currentElement.getChild("archdesc", config.getNameSpaceWrite());
            if (archdesc == null) {
                archdesc = new Element("archdesc", config.getNameSpaceWrite());
                currentElement.addContent(archdesc);
            }
            dsc = archdesc.getChild("dsc", config.getNameSpaceWrite());
            if (dsc == null) {
                dsc = new Element("dsc", config.getNameSpaceWrite());
                archdesc.addContent(dsc);
            }

            if (StringUtils.isNotBlank(node.getGoobiProcessTitle())) {
                Element altformavail = new Element("altformavail", config.getNameSpaceWrite());
                altformavail.setAttribute("localtype", "goobi_process");
                altformavail.setText(node.getGoobiProcessTitle());
                dsc.addContent(altformavail);
            }

        } else {
            dsc = currentElement;
            if (StringUtils.isNotBlank(node.getId())) {
                currentElement.setAttribute("id", node.getId());
            }
            if (node.getNodeType() != null) {
                currentElement.setAttribute("otherlevel", node.getNodeType().getNodeName());
            }
        }

        for (IEadEntry subNode : node.getSubEntryList()) {
            if (dsc == null) {
                dsc = new Element("dsc", config.getNameSpaceWrite());
                currentElement.addContent(dsc);
            }

            Element c = new Element("c", config.getNameSpaceWrite());
            dsc.addContent(c);
            if (StringUtils.isNotBlank(subNode.getGoobiProcessTitle())) {
                Element altformavail = new Element("altformavail", config.getNameSpaceWrite());
                altformavail.setAttribute("localtype", "goobi_process");
                altformavail.setText(subNode.getGoobiProcessTitle());
                c.addContent(altformavail);
            }

            addMetadata(c, subNode, xmlRootElement, updateHistory);
        }
    }

    private void createEadElement(Element xmlElement, boolean isMainElement, IMetadataField emf, Element xmlRootElement) {
        if (emf.isGroup()) {
            createEadGroupField(xmlElement, emf, isMainElement, xmlRootElement);
        } else {
            for (IFieldValue fv : emf.getValues()) {
                if ("person".equals(emf.getFieldType())) {
                    createEadXmlPersonField(xmlElement, isMainElement, emf, fv, xmlRootElement);
                } else if ("corporate".equals(emf.getFieldType())) {
                    createEadXmlCorporateField(xmlElement, isMainElement, emf, fv, xmlRootElement);
                } else if (StringUtils.isNotBlank(fv.getValuesForXmlExport())) {
                    createEadXmlField(xmlElement, isMainElement, emf, fv, xmlRootElement);
                }
            }
        }
    }

    private void updateChangeHistory(IEadEntry node) {
        // set maintenancestatus
        for (IMetadataField field : node.getIdentityStatementAreaList()) {
            if ("maintenancestatus".equals(field.getName())) {
                IFieldValue value = field.getValues().get(0);
                if (StringUtils.isBlank(value.getValue())) {
                    value.setValue("revised");
                }
            }
        }
        for (IMetadataField field : node.getIdentityStatementAreaList()) {
            // maintenancehistory
            if ("maintenancehistory".equals(field.getName())) {
                GroupValue newHistoryEvent = new GroupValue();
                newHistoryEvent.setGroupName(field.getName());

                List<IValue> val = new ArrayList<>();
                val.add(new ExtendendValue("eventtype", "revised", null, null));
                newHistoryEvent.getSubfields().put("eventtype", val);

                String date = formatter.format(new Date());
                val = new ArrayList<>();
                val.add(new ExtendendValue("eventdatetime", date, null, null));
                newHistoryEvent.getSubfields().put("eventdatetime", val);

                val = new ArrayList<>();
                val.add(new ExtendendValue("agent", username, null, null));
                newHistoryEvent.getSubfields().put("agent", val);
                List<IValue> grps = new ArrayList<>();
                grps.add(newHistoryEvent);
                NodeInitializer.addGroupData(field, grps);
                break;
            }
        }

        // add current user to editor list
        for (IMetadataField field : node.getDescriptionControlAreaList()) {
            if ("editorName".equals(field.getName())) {
                boolean match = false;
                for (IFieldValue value : field.getValues()) {
                    if (username.equals(value.getValue())) {
                        match = true;
                    }
                }
                if (!match) {
                    IFieldValue val = new FieldValue(field);
                    val.setValue(username);
                    field.addFieldValue(val);
                }
            }
        }
        // save updated node
        ArchiveManagementManager.saveNode(recordGroup.getId(), node);
    }

    private void createEadGroupField(Element xmlElement, IMetadataField groupField, boolean isMainElement, Element xmlRootElement) {

        if (!groupField.isFilled()) {
            return;
        }

        for (IMetadataGroup group : groupField.getGroups()) {

            String xpath = getXpath(isMainElement, groupField);
            if (StringUtils.isBlank(xpath)) {
                // dont export internal fields
                return;
            }

            String strRegex = "/(?=[^\\]]*(?:\\[|$))";
            String[] fields = xpath.split(strRegex);

            Element groupElement = xmlElement;
            if (xpath.contains("ead:control/")) {
                groupElement = xmlRootElement;
            }

            String lastElement = fields[fields.length - 1];

            String[] prevElements = Arrays.copyOf(fields, fields.length - 1);

            for (int i = 0; i < prevElements.length; i++) {
                String field = fields[i].trim();
                if (!".".equals(field)) {
                    // reuse elements until the last element
                    groupElement = findElement(field, groupElement);
                }
            }

            // always create a new entry for the last element
            String conditions = null;
            if (lastElement.contains("[")) {
                conditions = lastElement.substring(lastElement.indexOf("["));
                lastElement = lastElement.substring(0, lastElement.indexOf("["));
            }
            lastElement = lastElement.replace("ead:", "");
            groupElement = createXmlElement(groupElement, lastElement, conditions);

            for (IMetadataField subfield : group.getFields()) {
                for (IFieldValue fv : subfield.getValues()) {
                    if ("person".equals(subfield.getFieldType())) {
                        createEadXmlPersonField(groupElement, isMainElement, subfield, fv, xmlRootElement);
                    } else if ("corporate".equals(subfield.getFieldType())) {
                        createEadXmlCorporateField(groupElement, isMainElement, subfield, fv, xmlRootElement);
                    } else if (StringUtils.isNotBlank(fv.getValuesForXmlExport())) {
                        createEadXmlField(groupElement, isMainElement, subfield, fv, xmlRootElement);
                    }
                }
            }
        }
    }

    private void createEadXmlCorporateField(Element xmlElement, boolean isMainElement, IMetadataField emf, IFieldValue metadataValue,
            Element xmlRootElement) {
        if (StringUtils.isBlank(emf.getXpath())) {
            // don't export internal fields
            return;
        }

        if (StringUtils.isBlank(metadataValue.getMainName())) {
            // don't export empty fields
            return;
        }
        // create person root element
        Element currentElement = xmlElement;
        String xpath = getXpath(isMainElement, emf);
        String strRegex = "/(?=[^\\]]*(?:\\[|$))";
        String[] fields = xpath.split(strRegex);
        for (String field : fields) {
            field = field.trim();
            if (".".equals(field)) {
                // do nothing
            }
            // check if its an element or attribute
            else if (field.startsWith("@")) {
                field = field.substring(1);
                // create attribute on current element
                // duplicate current element if attribute is not empty
                if (currentElement.getAttribute(field) != null) {
                    Element duplicate = new Element(currentElement.getName(), config.getNameSpaceWrite());
                    for (Attribute attr : currentElement.getAttributes()) {
                        if (!attr.getName().equals(field)) {
                            duplicate.setAttribute(attr.getName(), attr.getValue());
                        }
                    }
                    currentElement.getParent().addContent(duplicate);
                    currentElement = duplicate;
                }
            } else {
                currentElement = findElement(field, currentElement);
            }
        }

        // sub element for main name
        if (StringUtils.isNotBlank(metadataValue.getMainName())) {
            String mainValueXpath = emf.getSubfieldMap().get("mainValueXpath");
            Element mainValueElement = currentElement;
            fields = mainValueXpath.split(strRegex);
            createElement(mainValueElement, mainValueXpath, metadataValue.getMainName());
        }

        if (StringUtils.isNotBlank(metadataValue.getSubName())) {
            String subValueXpath = emf.getSubfieldMap().get("subValueXpath");
            Element subValueElement = currentElement;
            fields = subValueXpath.split(strRegex);
            createElement(subValueElement, subValueXpath, metadataValue.getSubName());
        }

        if (StringUtils.isNotBlank(metadataValue.getPartName())) {
            String partValueXpath = emf.getSubfieldMap().get("partValueXpath");
            Element partValueElement = currentElement;
            fields = partValueXpath.split(strRegex);
            createElement(partValueElement, partValueXpath, metadataValue.getPartName());
        }

        // get element for id
        if (StringUtils.isNotBlank(metadataValue.getAuthorityValue()) && !"null".equals(metadataValue.getAuthorityValue())) {
            Element idElement = currentElement;
            String idXpath = emf.getSubfieldMap().get("authorityValueXpath");
            createElement(idElement, idXpath, metadataValue.getAuthorityValue());
        }
    }

    private void createEadXmlPersonField(Element xmlElement, boolean isMainElement, IMetadataField emf, IFieldValue metadataValue,
            Element xmlRootElement) {
        if (StringUtils.isBlank(emf.getXpath())) {
            // don't export internal fields
            return;
        }

        if (StringUtils.isBlank(metadataValue.getFirstname()) && StringUtils.isBlank(metadataValue.getLastname())) {
            // don't export empty fields
            return;
        }
        // create person root element
        Element currentElement = xmlElement;
        String xpath = getXpath(isMainElement, emf);
        String strRegex = "/(?=[^\\]]*(?:\\[|$))";
        String[] fields = xpath.split(strRegex);
        for (String field : fields) {
            field = field.trim();
            if (".".equals(field)) {
                // do nothing
            }
            // check if its an element or attribute
            else if (field.startsWith("@")) {
                field = field.substring(1);
                // create attribute on current element
                // duplicate current element if attribute is not empty
                if (currentElement.getAttribute(field) != null) {
                    Element duplicate = new Element(currentElement.getName(), config.getNameSpaceWrite());
                    for (Attribute attr : currentElement.getAttributes()) {
                        if (!attr.getName().equals(field)) {
                            duplicate.setAttribute(attr.getName(), attr.getValue());
                        }
                    }
                    currentElement.getParent().addContent(duplicate);
                    currentElement = duplicate;
                }
            } else {
                currentElement = findElement(field, currentElement);
            }
        }

        // sub element for firstname
        if (StringUtils.isNotBlank(metadataValue.getFirstname())) {
            String firstnameXpath = emf.getSubfieldMap().get("firstnameXpath");
            Element firstnameElement = currentElement;
            fields = firstnameXpath.split(strRegex);
            createElement(firstnameElement, firstnameXpath, metadataValue.getFirstname());
        }

        // sub element for lastname
        if (StringUtils.isNotBlank(metadataValue.getLastname())) {
            Element lastnameElement = currentElement;
            String lastnameXpath = emf.getSubfieldMap().get("lastnameXpath");
            fields = lastnameXpath.split(strRegex);
            createElement(lastnameElement, lastnameXpath, metadataValue.getLastname());

        }

        // get element for id
        if (StringUtils.isNotBlank(metadataValue.getAuthorityValue()) && !"null".equals(metadataValue.getAuthorityValue())) {
            Element idElement = currentElement;
            String idXpath = emf.getSubfieldMap().get("authorityValueXpath");
            createElement(idElement, idXpath, metadataValue.getAuthorityValue());
        }
    }

    private void createEadXmlField(Element xmlElement, boolean isMainElement, IMetadataField emf, IFieldValue metadataValue,
            Element xmlRootElement) {
        if (StringUtils.isBlank(emf.getXpath())) {
            // don't export internal fields
            return;
        }

        Element currentElement = xmlElement;
        String xpath = getXpath(isMainElement, emf);

        if (xpath.contains("ead:control/")) {
            currentElement = xmlRootElement;
        }
        String value = metadataValue.getValuesForXmlExport();
        currentElement = createElement(currentElement, xpath, value);
        if (StringUtils.isNotBlank(metadataValue.getAuthorityType())
                && StringUtils.isNotBlank(metadataValue.getAuthorityValue())) {
            currentElement.setAttribute("SOURCE", metadataValue.getAuthorityType());
            currentElement.setAttribute("AUTHFILENUMBER", metadataValue.getAuthorityValue());
        }
    }

    public Element createElement(Element currentElement, String xpath, String value) {

        // split xpath on URL_SEPARATOR, unless within square brackets
        String strRegex = "/(?=[^\\]]*(?:\\[|$))";
        String[] fields = xpath.split(strRegex);
        boolean written = false;
        for (String field : fields) {
            field = field.trim();

            // ignore .
            if (".".equals(field)) {
                // do nothing
            }
            // check if its an element or attribute
            else if (field.startsWith("@")) {
                field = field.substring(1);
                // create attribute on current element
                // duplicate current element if attribute is not empty
                if (currentElement.getAttribute(field) != null) {
                    Element duplicate = new Element(currentElement.getName(), config.getNameSpaceWrite());
                    for (Attribute attr : currentElement.getAttributes()) {
                        if (!attr.getName().equals(field)) {
                            duplicate.setAttribute(attr.getName(), attr.getValue());
                        }
                    }
                    currentElement.getParent().addContent(duplicate);
                    currentElement = duplicate;
                }

                currentElement.setAttribute(field, value);
                written = true;
            } else {
                currentElement = findElement(field, currentElement);
            }
        }
        if (!written) {
            // duplicate current element if not empty
            if (StringUtils.isNotBlank(currentElement.getText()) || !currentElement.getChildren().isEmpty()) {
                Element duplicate = new Element(currentElement.getName(), config.getNameSpaceWrite());
                for (Attribute attr : currentElement.getAttributes()) {
                    duplicate.setAttribute(attr.getName(), attr.getValue());
                }

                if (!currentElement.getChildren().isEmpty()) {
                    for (Element child : currentElement.getChildren()) {
                        Element duplicateChild = new Element(child.getName(), config.getNameSpaceWrite());
                        duplicateChild.setText(child.getText());
                        duplicate.addContent(duplicateChild);
                    }
                }

                currentElement.getParent().addContent(duplicate);
                currentElement = duplicate;
            }

            currentElement.setText(value);

        }
        return currentElement;
    }

    private Element findElement(String field, Element currentElement) {
        // remove namespace

        field = field.replace("ead:", "");

        String conditions = null;
        if (field.contains("[")) {
            conditions = field.substring(field.indexOf("["));
            field = field.substring(0, field.indexOf("["));
        }

        // check if element exists, re-use if possible
        Element element = currentElement.getChild(field, config.getNameSpaceWrite());
        if (element == null) {
            element = createXmlElement(currentElement, field, conditions);
        } else if (conditions != null) {
            // check if conditions are fulfilled
            String[] conditionArray = conditions.split("\\[");
            // add each condition
            boolean conditionsMatch = true;
            for (String condition : conditionArray) {
                if (StringUtils.isBlank(condition)) {
                    // do nothing, no condition is given
                } else if (condition.trim().startsWith("not")) {
                    int start = condition.indexOf("(");
                    int end = condition.indexOf(")");
                    condition = condition.substring(start + 2, end);
                    if (condition.contains("=")) {
                        // [not(@type='abc')]
                        String value = element.getAttributeValue(condition.substring(0, condition.indexOf("=")));
                        if (StringUtils.isNotBlank(value) && value.equals(condition.substring(condition.indexOf("=") + 1).replace("'", ""))) {
                            conditionsMatch = false;
                        }
                    } else {
                        // [not(@type)]
                        String value = element.getAttributeValue(condition);
                        if (StringUtils.isNotBlank(value)) {
                            conditionsMatch = false;
                        }
                    }
                    continue;
                } else if (condition.contains("]")) {
                    condition = condition.substring(1, condition.lastIndexOf("]"));
                } else {
                    condition = condition.substring(1);
                }
                condition = condition.trim();
                if (condition.contains("=")) {
                    String value = element.getAttributeValue(condition.substring(0, condition.indexOf("=")));
                    if (StringUtils.isBlank(value) || !value.equals(condition.substring(condition.indexOf("=") + 1).replace("'", ""))) {
                        conditionsMatch = false;
                    }
                } else {
                    String value = element.getAttributeValue(condition);
                    if (StringUtils.isBlank(value)) {
                        conditionsMatch = false;
                    }
                }
            }
            // if not create new element
            if (!conditionsMatch) {
                element = createXmlElement(currentElement, field, conditions);
            }
        }
        currentElement = element;
        return currentElement;
    }

    private String getXpath(boolean isMainElement, IMetadataField emf) {
        String xpath = emf.getXpath();
        if (xpath.endsWith("[1]")) {
            xpath = xpath.replace("[1]", "");
        }
        if (xpath.matches("\\(.+\\|.*\\)")) {
            String[] parts = xpath.substring(1, xpath.lastIndexOf(")")).split("\\|");

            for (String part : parts) {
                if (isMainElement) {
                    if (part.contains("archdesc") || part.contains("eadheader")) {
                        xpath = part;
                    }
                } else if (!part.contains("archdesc") && !part.contains("eadheader")) {
                    xpath = part;
                }
            }
        }
        return xpath;
    }

    private Element createXmlElement(Element currentElement, String field, String conditions) {
        Element element = new Element(field, config.getNameSpaceWrite());
        currentElement.addContent(element);
        if (conditions != null) {
            String[] conditionArray = conditions.split("\\[");
            // add each condition
            for (String condition : conditionArray) {
                if (StringUtils.isBlank(condition) || condition.trim().startsWith("not")) {
                    continue;
                }

                boolean boAttribute = condition.startsWith("@");
                //is it an attribute or a subelement?
                if (boAttribute) {

                    if (condition.contains("]")) {
                        condition = condition.substring(1, condition.lastIndexOf("]"));
                    } else {
                        condition = condition.substring(1);
                    }
                    condition = condition.trim();

                    String[] attr = condition.split("=");
                    if (attr.length > 1) {
                        element.setAttribute(attr[0], attr[1].replace("'", ""));
                    }

                } else {

                    if (condition.contains("]")) {
                        condition = condition.substring(0, condition.lastIndexOf("]"));
                    }
                    condition = condition.trim();

                    String[] attr = condition.split("=");
                    if (attr.length > 1) {
                        String strName = attr[1].replace("'", "");
                        Element eltChild = new Element(attr[0], config.getNameSpaceWrite());
                        eltChild.setText(strName);
                        element.addContent(eltChild);
                    }
                }
            }

        }
        return element;
    }

    public void prepareMoveNode() {

        // abort if no node is selected
        if (selectedEntry == null) {
            displayMode = "";
            return;
        }

        // abort if root node is selected
        if (selectedEntry.getParentNode() == null) {
            displayMode = "";
            return;
        }

        // abort if first and only child of root node is selected
        if (selectedEntry.getParentNode().getParentNode() == null && selectedEntry.getParentNode().getSubEntryList().size() == 1) {
            displayMode = "";
            return;
        }

        // provide new flat list
        // mark parent of current node as not selectable
        // don't show child nodes of selected node
        moveList = rootElement.getMoveToDestinationList(selectedEntry);
    }

    public void moveNode() {

        // abort if new parent is locked

        if (LockingBean.isLockedByAnotherUser(destinationEntry.getId(), username)) {
            Helper.setFehlerMeldung("plugin_administration_archive_destinationLocked");
            return;
        }

        // remove element from old parent list
        IEadEntry oldParent = selectedEntry.getParentNode();
        oldParent.removeSubEntry(selectedEntry);
        oldParent.updateHierarchy();

        selectedEntry.setParentNode(destinationEntry);
        destinationEntry.addSubEntry(selectedEntry);
        destinationEntry.reOrderElements();
        selectedEntry.updateHierarchy();
        List<IEadEntry> nodesToUpdate = selectedEntry.getAllNodes();
        nodesToUpdate.addAll(oldParent.getAllNodes());

        ArchiveManagementManager.updateNodeHierarchy(recordGroup.getId(), nodesToUpdate);

        setSelectedEntry(selectedEntry);
        displayMode = "";
        flatEntryList = null;
    }

    public void moveNodeUp() {
        // abort if no node is selected
        if (selectedEntry == null) {
            return;
        }

        // abort if root node is selected
        if (selectedEntry.getParentNode() == null) {
            return;
        }

        // abort if parent node has only one child
        if (selectedEntry.getParentNode().getSubEntryList().size() == 1) {
            return;
        }
        // abort if selected node is first child of parent
        if (selectedEntry.getOrderNumber().intValue() == 0) {
            return;
        }

        // swap node order
        IEadEntry previousNode = selectedEntry.getParentNode().getSubEntryList().get(selectedEntry.getOrderNumber() - 1);
        int otherOrderNumber = previousNode.getOrderNumber();
        int currentOrderNumber = selectedEntry.getOrderNumber();
        selectedEntry.setOrderNumber(otherOrderNumber);
        previousNode.setOrderNumber(currentOrderNumber);

        // save nodes
        List<IEadEntry> nodes = new ArrayList<>();
        nodes.add(previousNode);
        nodes.add(selectedEntry);
        ArchiveManagementManager.updateNodeHierarchy(recordGroup.getId(), nodes);

        selectedEntry.getParentNode().sortElements();
        selectedEntry.getParentNode().updateHierarchy();

        List<IEadEntry> nodesToUpdate = selectedEntry.getParentNode().getAllNodes();
        ArchiveManagementManager.updateNodeHierarchy(recordGroup.getId(), nodesToUpdate);

        flatEntryList = null;

    }

    public void moveNodeDown() {
        // abort if no node is selected
        if (selectedEntry == null) {
            return;
        }

        // abort if root node is selected
        if (selectedEntry.getParentNode() == null) {
            return;
        }

        // abort if parent node has only one child
        if (selectedEntry.getParentNode().getSubEntryList().size() == 1) {
            return;
        }
        // abort if selected node is last child of parent
        if (selectedEntry.getOrderNumber().intValue() == selectedEntry.getParentNode().getSubEntryList().size() - 1) {
            return;
        }

        IEadEntry followingNode = selectedEntry.getParentNode().getSubEntryList().get(selectedEntry.getOrderNumber() + 1);
        int currentOrderNumber = selectedEntry.getOrderNumber();
        int otherOrderNumber = followingNode.getOrderNumber();
        selectedEntry.setOrderNumber(otherOrderNumber);
        followingNode.setOrderNumber(currentOrderNumber);

        // save nodes
        List<IEadEntry> nodes = new ArrayList<>();
        nodes.add(followingNode);
        nodes.add(selectedEntry);
        ArchiveManagementManager.updateNodeHierarchy(recordGroup.getId(), nodes);

        selectedEntry.getParentNode().sortElements();
        selectedEntry.getParentNode().updateHierarchy();

        List<IEadEntry> nodesToUpdate = selectedEntry.getParentNode().getAllNodes();
        ArchiveManagementManager.updateNodeHierarchy(recordGroup.getId(), nodesToUpdate);
        flatEntryList = null;

    }

    public void moveHierarchyDown() {
        // abort if no node is selected
        if (selectedEntry == null) {
            return;
        }
        // abort if root node is selected
        if (selectedEntry.getParentNode() == null) {
            return;
        }

        // abort if selected node is first child of parent
        if (selectedEntry.getOrderNumber().intValue() == 0) {
            return;
        }

        // find previous sibling
        IEadEntry previousNode = selectedEntry.getParentNode().getSubEntryList().get(selectedEntry.getOrderNumber().intValue() - 1);
        // move node to prev.
        destinationEntry = previousNode;
        destinationEntry.setDisplayChildren(true);
        moveNode();
    }

    public void moveHierarchyUp() {
        // abort if no node is selected
        if (selectedEntry == null) {
            return;
        }
        // abort if root node is selected
        if (selectedEntry.getParentNode() == null) {
            return;
        }
        // abort if child of root node is selected
        if (selectedEntry.getParentNode().getParentNode() == null) {
            return;
        }

        // get parent node of parent
        IEadEntry oldParent = selectedEntry.getParentNode();
        IEadEntry newParent = oldParent.getParentNode();

        // move current node to parents parent
        destinationEntry = newParent;
        moveNode();
        // move node to one position after current parent
        if (selectedEntry.getOrderNumber().intValue() != oldParent.getOrderNumber().intValue() + 1) {
            Collections.swap(selectedEntry.getParentNode().getSubEntryList(), selectedEntry.getOrderNumber(), oldParent.getOrderNumber() + 1);
            selectedEntry.getParentNode().reOrderElements();
            selectedEntry.getParentNode().updateHierarchy();
            List<IEadEntry> nodesToUpdate = selectedEntry.getParentNode().getAllNodes();
            ArchiveManagementManager.updateNodeHierarchy(recordGroup.getId(), nodesToUpdate);
        }

    }

    public void searchAdvanced() {
        resetSearch();
        List<Integer> searchResults = new ArrayList<>();
        for (StringPair p : advancedSearch) {
            if (StringUtils.isNotBlank(p.getOne()) && StringUtils.isNotBlank(p.getTwo())) {
                // filter the node list, return only fields containing the data
                searchResults = ArchiveManagementManager.advancedSearch(recordGroup.getId(), p.getOne(), p.getTwo(), searchResults);
            }
        }

        // finally mark all found nodes
        for (IEadEntry entry : rootElement.getAllNodes()) {
            if (searchResults.contains(entry.getDatabaseId())) {
                entry.markAsFound();
            }
        }

        flatEntryList = rootElement.getSearchList();
    }

    public void search() {
        if (StringUtils.isNotBlank(searchValue)) {
            // hide all elements
            rootElement.resetFoundList();
            // search in all/some metadata fields of all elements?

            List<Integer> searchResults = ArchiveManagementManager.simpleSearch(recordGroup.getId(), null, searchValue);

            for (IEadEntry entry : rootElement.getAllNodes()) {
                if (searchResults.contains(entry.getDatabaseId())) {
                    entry.markAsFound();
                }
            }

            // fill flatList with displayable fields
            flatEntryList = rootElement.getSearchList();
        } else {
            resetSearch();
        }

    }

    public void resetSearch() {
        searchValue = null;
        rootElement.resetFoundList();
        flatEntryList = null;
    }

    public List<IProcessTemplate> getProcessTemplates() {
        if (processTemplates.isEmpty()) {
            StringBuilder sql = new StringBuilder();
            sql.append("select ProzesseID, Titel, ProjekteID from prozesse where IstTemplate = true ");
            if (!config.isShowProjectSelection()) {
                sql.append("and ProjekteID in (select projekteId from projektbenutzer where BenutzerID=");
                sql.append(Helper.getCurrentUser().getId());
                sql.append(") ");
            }
            sql.append("order by titel;");

            @SuppressWarnings("unchecked")
            List<Object> rawData = ProcessManager.runSQL(sql.toString());
            for (int i = 0; i < rawData.size(); i++) {
                Object[] rowData = (Object[]) rawData.get(i);
                String processId = (String) rowData[0];
                String processTitle = (String) rowData[1];
                String projectId = (String) rowData[2];
                processTemplates.add(new ProcessTemplate(processId, processTitle, projectId));
            }
        }
        return processTemplates;
    }

    public List<StringPair> getProjectNames() {
        if (projects.isEmpty()) {
            StringBuilder sql = new StringBuilder();
            sql.append("select ProjekteID, Titel from projekte where ProjekteID in (select projekteId from projektbenutzer where BenutzerID=");
            sql.append(Helper.getCurrentUser().getId());
            sql.append(") order by titel;");
            @SuppressWarnings("unchecked")
            List<Object> rawData = ProcessManager.runSQL(sql.toString());
            for (int i = 0; i < rawData.size(); i++) {
                Object[] rowData = (Object[]) rawData.get(i);
                String processId = (String) rowData[0];
                String processTitle = (String) rowData[1];
                projects.add(new StringPair(processId, processTitle));
            }
        }
        return projects;
    }

    public Process createProcess() {
        // abort if no node is selected
        if (selectedEntry == null) {
            return null;
        }
        if (selectedEntry.getNodeType() == null) {
            return null;
        }
        Integer processTemplateId = null;
        if (StringUtils.isBlank(selectedTemplate)) {
            processTemplateId = selectedEntry.getNodeType().getProcessTemplateId();
        } else {
            processTemplateId = Integer.parseInt(selectedTemplate);
        }

        // load process template
        processTemplate = ProcessManager.getProcessById(processTemplateId);
        if (processTemplate == null) {
            return null;
        }

        Prefs prefs = processTemplate.getRegelsatz().getPreferences();

        String publicationType = selectedEntry.getNodeType().getDocumentType();
        Fileformat fileformat = null;
        DigitalDocument digDoc = null;
        try {
            fileformat = new MetsMods(prefs);
            digDoc = new DigitalDocument();
            // create mets file based on selected node type
            fileformat.setDigitalDocument(digDoc);
            DocStruct logical = digDoc.createDocStruct(prefs.getDocStrctTypeByName(publicationType));
            digDoc.setLogicalDocStruct(logical);
            Metadata identifier = new Metadata(prefs.getMetadataTypeByName("CatalogIDDigital"));
            identifier.setValue(selectedEntry.getId());
            logical.addMetadata(identifier);
            addNoteId(prefs, logical);

            // import configured metadata
            for (IMetadataField emf : selectedEntry.getIdentityStatementAreaList()) {
                createModsMetadata(prefs, emf, logical);
            }
            for (IMetadataField emf : selectedEntry.getContextAreaList()) {
                createModsMetadata(prefs, emf, logical);
            }
            for (IMetadataField emf : selectedEntry.getContentAndStructureAreaAreaList()) {
                createModsMetadata(prefs, emf, logical);
            }
            for (IMetadataField emf : selectedEntry.getAccessAndUseAreaList()) {
                createModsMetadata(prefs, emf, logical);
            }
            for (IMetadataField emf : selectedEntry.getAlliedMaterialsAreaList()) {
                createModsMetadata(prefs, emf, logical);
            }
            for (IMetadataField emf : selectedEntry.getNotesAreaList()) {
                createModsMetadata(prefs, emf, logical);
            }
            for (IMetadataField emf : selectedEntry.getDescriptionControlAreaList()) {
                createModsMetadata(prefs, emf, logical);
            }
            IEadEntry parent = selectedEntry.getParentNode();
            while (parent != null) {
                for (IMetadataField emf : parent.getIdentityStatementAreaList()) {
                    if (emf.isImportMetadataInChild()) {
                        createModsMetadata(prefs, emf, logical);
                    }
                }
                for (IMetadataField emf : parent.getContextAreaList()) {
                    if (emf.isImportMetadataInChild()) {
                        createModsMetadata(prefs, emf, logical);
                    }
                }
                for (IMetadataField emf : parent.getContentAndStructureAreaAreaList()) {
                    if (emf.isImportMetadataInChild()) {
                        createModsMetadata(prefs, emf, logical);
                    }
                }
                for (IMetadataField emf : parent.getAccessAndUseAreaList()) {
                    if (emf.isImportMetadataInChild()) {
                        createModsMetadata(prefs, emf, logical);
                    }
                }
                for (IMetadataField emf : parent.getAlliedMaterialsAreaList()) {
                    if (emf.isImportMetadataInChild()) {
                        createModsMetadata(prefs, emf, logical);
                    }
                }
                for (IMetadataField emf : parent.getNotesAreaList()) {
                    if (emf.isImportMetadataInChild()) {
                        createModsMetadata(prefs, emf, logical);
                    }
                }
                for (IMetadataField emf : parent.getDescriptionControlAreaList()) {
                    if (emf.isImportMetadataInChild()) {
                        createModsMetadata(prefs, emf, logical);
                    }
                }

                parent = parent.getParentNode();
            }

        } catch (UGHException e) {
            log.error(e);
        }

        // generate process title via ProcessTitleGenerator
        ProcessTitleGenerator titleGenerator = prepareTitleGenerator(digDoc.getLogicalDocStruct());

        // check if the generated process name is unique
        // if it is not unique, then get the full uuid version name from the generator
        // check its uniqueness again, if still not, report the failure and abort
        String processTitle = titleGenerator.generateTitle();
        if (ProcessManager.getNumberOfProcessesWithTitle(processTitle) > 0) {
            String message1 = "A process named " + processTitle + " already exists. Trying to get an alternative title.";
            log.debug(message1);
            Helper.setMeldung(message1);
            processTitle = titleGenerator.getAlternativeTitle();
            if (ProcessManager.getNumberOfProcessesWithTitle(processTitle) > 0) {
                // title is not unique in this scenario, abort
                String message2 = "Uniqueness of the generated process name is not guaranteed, aborting.";
                log.error(message2);
                Helper.setFehlerMeldung(message2);
                return null;
            }
        }
        log.debug("processTitle = " + processTitle);

        try {
            DocStruct physical = digDoc.createDocStruct(prefs.getDocStrctTypeByName("BoundBook"));
            digDoc.setPhysicalDocStruct(physical);
            Metadata imageFiles = new Metadata(prefs.getMetadataTypeByName("pathimagefiles"));
            imageFiles.setValue(processTitle + "_media");
            physical.addMetadata(imageFiles);
        } catch (DocStructHasNoTypeException | UGHException e) {
            log.error(e);
        }
        // set project based on selection
        if (config.isShowProjectSelection() && StringUtils.isNotBlank(selectedProject)) {
            try {
                Integer projectId = Integer.parseInt(selectedProject);

                Project p = ProjectManager.getProjectById(projectId);
                processTemplate.setProjekt(p);
            } catch (DAOException e) {
                log.error(e);
            }
        }

        // create process based on configured process template
        Process process = bhelp.createAndSaveNewProcess(processTemplate, processTitle, fileformat);

        // save current node
        selectedEntry.setGoobiProcessTitle(processTitle);
        ArchiveManagementManager.saveNode(recordGroup.getId(), selectedEntry);

        // start any open automatic tasks
        for (Step s : process.getSchritteList()) {
            if (StepStatus.OPEN.equals(s.getBearbeitungsstatusEnum()) && s.isTypAutomatisch()) {
                ScriptThreadWithoutHibernate myThread = new ScriptThreadWithoutHibernate(s);
                myThread.startOrPutToQueue();
            }
        }
        return process;
    }

    private void addNoteId(Prefs prefs, DocStruct logical) {
        try {
            MetadataType eadIdType = prefs.getMetadataTypeByName("NodeId");
            if (eadIdType != null) {
                Metadata eadid = new Metadata(eadIdType);
                eadid.setValue(selectedEntry.getId());
                logical.addMetadata(eadid);
            }
        } catch (UGHException e) {
            log.error(e);
        }
    }

    /**
     * prepare a ProcessTitleGenerator object suitable for this scenario
     *
     * @param fileformat
     * 
     * @return ProcessTitleGenerator object
     */
    private ProcessTitleGenerator prepareTitleGenerator(DocStruct docstruct) {

        ProcessTitleGenerator titleGenerator = new ProcessTitleGenerator();
        titleGenerator.setSeparator(config.getSeparator());
        titleGenerator.setBodyTokenLengthLimit(config.getLengthLimit());
        for (TitleComponent comp : config.getTitleParts()) {
            // set title type
            ManipulationType manipulationType = null;
            // check for special types
            switch (comp.getType().toLowerCase()) {
                case "camelcase", "camel_case", "camelcaselenghtlimited", "camel_case_lenght_limited":
                    if (config.getLengthLimit() > 0) {
                        manipulationType = ManipulationType.CAMEL_CASE_LENGTH_LIMITED;
                    } else {
                        manipulationType = ManipulationType.CAMEL_CASE;
                    }

                    break;
                case "afterlastseparator", "after_last_separator":
                    manipulationType = ManipulationType.AFTER_LAST_SEPARATOR;
                    break;
                case "beforefirstseparator", "before_first_separator":
                    manipulationType = ManipulationType.BEFORE_FIRST_SEPARATOR;
                    break;
                case "normal":
                default:
                    manipulationType = ManipulationType.NORMAL;
            }

            // get actual value
            String val = null;
            if (StringUtils.isNotBlank(comp.getValue())) {
                // static text
                val = comp.getValue();
            } else {
                // get value from metadata
                String metadataName = comp.getName();

                for (Metadata md : docstruct.getAllMetadata()) {
                    if (md.getType().getName().equals(metadataName)) {
                        val = md.getValue();
                        break;
                    }
                }
            }
            if (StringUtils.isNotBlank(val)) {
                titleGenerator.addToken(val, manipulationType);
            }
        }

        if (StringUtils.isBlank(titleGenerator.generateTitle())) {
            // default title, if nothing is configured
            IEadEntry idEntry = config.isUseIdFromParent() ? selectedEntry.getParentNode() : selectedEntry;
            String valueOfFirstToken = idEntry.getId();
            titleGenerator.addToken(valueOfFirstToken, ManipulationType.BEFORE_FIRST_SEPARATOR);

            ManipulationType labelTokenType = config.getLengthLimit() > 0 ? ManipulationType.CAMEL_CASE_LENGTH_LIMITED : ManipulationType.CAMEL_CASE;
            String label = selectedEntry.getLabel();
            titleGenerator.addToken(label, labelTokenType);
        }
        return titleGenerator;
    }

    //  create metadata, add it to logical element
    private void createModsMetadata(Prefs prefs, IMetadataField emf, DocStruct logical) {
        if (StringUtils.isNotBlank(emf.getMetadataName())) {
            //  groups
            if (emf.isGroup()) {

                for (IMetadataGroup group : emf.getGroups()) {
                    try {
                        MetadataGroup mg = new MetadataGroup(prefs.getMetadataGroupTypeByName(emf.getMetadataName()));

                        for (IMetadataField subfield : group.getFields()) {
                            //TODO person, corp
                            for (IFieldValue fv : subfield.getValues()) {
                                Metadata metadata = null;
                                for (Metadata md : mg.getMetadataList()) {
                                    if (md.getType().getName().equals(subfield.getMetadataName())) {
                                        metadata = md;
                                    }
                                }
                                if (metadata == null || (StringUtils.isNotBlank(metadata.getValue()))) {
                                    metadata = new Metadata(prefs.getMetadataTypeByName(subfield.getMetadataName()));
                                    mg.addMetadata(metadata);
                                }
                                metadata.setValue(fv.getValue());
                            }
                        }

                        logical.addMetadataGroup(mg);
                    } catch (UGHException e) {
                        log.error(e);
                    }
                }
            } else {
                // regular metadata
                for (IFieldValue fv : emf.getValues()) {

                    if ("person".equals(emf.getFieldType())) {
                        try {
                            Person p = new Person(prefs.getMetadataTypeByName(emf.getMetadataName()));
                            p.setFirstname(fv.getFirstname());
                            p.setLastname(fv.getLastname());
                            p.setAuthorityValue(fv.getAuthorityValue());
                            logical.addPerson(p);
                        } catch (MetadataTypeNotAllowedException e) {
                            log.error(e);
                        }

                    } else if ("corporate".equals(emf.getFieldType())) {
                        try {
                            Corporate c = new Corporate(prefs.getMetadataTypeByName(emf.getMetadataName()));
                            c.setMainName(fv.getMainName());
                            if (StringUtils.isBlank(fv.getSubName())) {
                                c.addSubName(new NamePart("subname", fv.getSubName()));
                            }
                            c.setPartName(fv.getPartName());
                            c.setAuthorityValue(fv.getAuthorityValue());
                            logical.addCorporate(c);
                        } catch (MetadataTypeNotAllowedException e) {
                            log.error(e);
                        }

                    }

                    else if (!fv.getMultiselectSelectedValues().isEmpty()) {
                        for (String value : fv.getMultiselectSelectedValues()) {
                            try {
                                Metadata md = new Metadata(prefs.getMetadataTypeByName(emf.getMetadataName()));
                                md.setValue(value);
                                logical.addMetadata(md);
                            } catch (UGHException e) {
                                log.error(e);
                            }
                        }
                    } else if (StringUtils.isNotBlank(fv.getValue())) {
                        try {

                            Metadata md = new Metadata(prefs.getMetadataTypeByName(emf.getMetadataName()));
                            md.setValue(fv.getValue());
                            if (StringUtils.isNotBlank(fv.getAuthorityValue())) {
                                md.setAuthorityFile(fv.getAuthorityType(), "", fv.getAuthorityValue());
                            }
                            logical.addMetadata(md);
                        } catch (UGHException e) {
                            log.error(e);
                        }
                    }
                }
            }
        }
    }

    public void downloadDocket() {
        if (selectedEntry == null) {
            return;
        }
        if (StringUtils.isBlank(selectedEntry.getGoobiProcessTitle())) {
            return;
        }
        Process process = ProcessManager.getProcessByExactTitle(selectedEntry.getGoobiProcessTitle());

        if (process.getBatch() == null) {
            process.downloadDocket();
        } else {
            // load all processes of the batch

            List<Process> docket = ProcessManager.getProcesses(null, " istTemplate = false AND batchID = " + process.getBatch().getBatchId(), 0,
                    Integer.MAX_VALUE, null);
            if (docket.size() == 1) {
                process.downloadDocket();
            } else {
                FacesContext facesContext = FacesContextHelper.getCurrentFacesContext();
                Path xsltfile = Paths.get(ConfigurationHelper.getInstance().getXsltFolder(), "docket_multipage.xsl");
                HttpServletResponse response = (HttpServletResponse) facesContext.getExternalContext().getResponse();
                String fileName = "batch_docket" + ".pdf";
                ServletContext servletContext = (ServletContext) facesContext.getExternalContext().getContext();
                String contentType = servletContext.getMimeType(fileName);
                response.setContentType(contentType);
                response.setHeader("Content-Disposition", "attachment;filename=\"" + fileName + "\"");

                try {
                    ServletOutputStream out = response.getOutputStream();
                    XsltToPdf ern = new XsltToPdf();
                    ern.startExport(docket, out, xsltfile.toString());
                    out.flush();
                } catch (IOException e) {
                    log.error("IOException while exporting run note", e);
                }

                facesContext.responseComplete();
            }

        }

    }

    public void downloadArchive() {
        // validate
        validateArchive();
        if (selectedEntry != null) {
            // save current element
            ArchiveManagementManager.saveNode(recordGroup.getId(), selectedEntry);
        }
        Document document = createEadFile();

        //  write document to servlet output stream
        String downloadFileName = recordGroup.getTitle().replace(" ", "_");
        if (!downloadFileName.endsWith(".xml")) {
            downloadFileName = downloadFileName + ".xml";
        }
        FacesContext facesContext = FacesContextHelper.getCurrentFacesContext();
        if (!facesContext.getResponseComplete()) {
            HttpServletResponse response = (HttpServletResponse) facesContext.getExternalContext().getResponse();
            ServletContext servletContext = (ServletContext) facesContext.getExternalContext().getContext();
            String contentType = servletContext.getMimeType(downloadFileName);
            response.setContentType(contentType);

            response.setHeader("Content-Disposition", "attachment;filename=\"" + downloadFileName + "\"");

            try {
                ServletOutputStream out = response.getOutputStream();
                XMLOutputter outputter = new XMLOutputter(Format.getPrettyFormat());
                outputter.output(document, out);
                out.flush();
            } catch (IOException e) {
                log.error("IOException while exporting run note", e);
            }

            facesContext.responseComplete();
        }
    }

    @Override
    public Document createEadFile() {
        config.readConfiguration(databaseName);
        // reload all nodes from db to get every change
        rootElement = ArchiveManagementManager.loadRecordGroup(recordGroup.getId());

        Document document = new Document();
        Element eadRoot = new Element("ead", config.getNameSpaceWrite());
        document.setRootElement(eadRoot);

        addMetadata(eadRoot, rootElement, eadRoot, true);
        return document;
    }

    @Override
    public Document createEadFileForNodeAndAncestors(IEadEntry entry) {

        // clone node and its ancestors
        IEadEntry newEntry = entry.copyWithAncestors();

        while (newEntry.getParentNode() != null) {
            newEntry = newEntry.getParentNode();
        }

        Document document = new Document();
        Element eadRoot = new Element("ead", config.getNameSpaceWrite());
        document.setRootElement(eadRoot);

        addMetadata(eadRoot, newEntry, eadRoot, true);
        return document;
    }

    @Override
    public String saveArchiveAndLeave() {
        // save current node
        if (selectedEntry != null) {
            // update node history, if node was changed
            String oldFingerPrint = selectedEntry.getFingerprint();
            selectedEntry.calculateFingerprint();
            String newFingerPrint = selectedEntry.getFingerprint();

            if (StringUtils.isNotBlank(oldFingerPrint) && StringUtils.isNotBlank(newFingerPrint) && !oldFingerPrint.equals(newFingerPrint)) {
                updateChangeHistory(selectedEntry);
            }

            ArchiveManagementManager.saveNode(recordGroup.getId(), selectedEntry);
        }

        return cancelEdition();

    }

    @Override
    public String cancelEdition() {
        // reset current settings
        if (selectedEntry != null) {
            List<IEadEntry> entriesToUnlock = selectedEntry.getAllNodes();
            for (IEadEntry e : entriesToUnlock) {
                LockingBean.freeObject(e.getId());
            }
        }

        databaseName = null;
        selectedEntry = null;
        rootElement = null;
        displayMode = "";
        flatEntryList = null;
        searchValue = null;
        config.setConfiguredFields(null);
        displayIdentityStatementArea = false;
        displayContextArea = false;
        displayContentArea = false;
        displayAccessArea = false;
        displayMaterialsArea = false;
        displayNotesArea = false;
        displayControlArea = false;
        processTemplate = null;

        // return to start screen
        return "";
    }

    public void validateArchive() {

        Map<String, List<String>> valueMap = new HashMap<>();

        validateNode(rootElement, valueMap);

    }

    private void validateNode(IEadEntry node, Map<String, List<String>> valueMap) {
        node.setValid(true);
        for (IMetadataField emf : node.getIdentityStatementAreaList()) {
            validateMetadataField(node, valueMap, emf);
        }
        for (IMetadataField emf : node.getContextAreaList()) {
            validateMetadataField(node, valueMap, emf);
        }
        for (IMetadataField emf : node.getContentAndStructureAreaAreaList()) {
            validateMetadataField(node, valueMap, emf);
        }
        for (IMetadataField emf : node.getAccessAndUseAreaList()) {
            validateMetadataField(node, valueMap, emf);
        }
        for (IMetadataField emf : node.getAlliedMaterialsAreaList()) {
            validateMetadataField(node, valueMap, emf);
        }
        for (IMetadataField emf : node.getNotesAreaList()) {
            validateMetadataField(node, valueMap, emf);
        }
        for (IMetadataField emf : node.getDescriptionControlAreaList()) {
            validateMetadataField(node, valueMap, emf);
        }

        for (IEadEntry child : node.getSubEntryList()) {
            validateNode(child, valueMap);
        }

    }

    private void validateMetadataField(IEadEntry node, Map<String, List<String>> valueMap, IMetadataField emf) {
        emf.setValid(true);
        if (emf.getValidationType() != null) {
            if (emf.getValidationType().contains("date")) {
                for (IFieldValue fv : emf.getValues()) {
                    if (StringUtils.isNotBlank(fv.getValue())) {
                        CharStream in = CharStreams.fromString(fv.getValue());
                        ExtendedDateTimeFormatLexer lexer = new ExtendedDateTimeFormatLexer(in);
                        lexer.removeErrorListeners();
                        CommonTokenStream tokens = new CommonTokenStream(lexer);
                        ExtendedDateTimeFormatParser parser = new ExtendedDateTimeFormatParser(tokens);
                        parser.removeErrorListeners();
                        parser.edtf();
                        if (parser.getNumberOfSyntaxErrors() > 0) {
                            emf.setValid(false);
                            node.setValid(false);
                        }
                    }
                }
            }
            if (emf.getValidationType().contains("unique")) {
                for (IFieldValue fv : emf.getValues()) {
                    if (StringUtils.isNotBlank(fv.getValue())) {
                        List<String> values = valueMap.get(emf.getName());
                        if (values == null) {
                            values = new ArrayList<>();
                            valueMap.put(emf.getName(), values);
                        }
                        if (values.contains(fv.getValue())) {
                            emf.setValid(false);
                            node.setValid(false);
                        } else {
                            values.add(fv.getValue());
                        }
                    }
                }

            }
            if (emf.getValidationType().contains("required")) {
                boolean filled = false;
                for (IFieldValue fv : emf.getValues()) {
                    if (StringUtils.isNotBlank(fv.getValue())) {
                        filled = true;
                    }
                }
                if (!filled) {
                    emf.setValid(false);
                    node.setValid(false);
                }
            }
            if (emf.getValidationType().contains("regex")) {
                String regex = emf.getRegularExpression();
                for (IFieldValue fv : emf.getValues()) {
                    if (StringUtils.isNotBlank(fv.getValue()) && !fv.getValue().matches(regex)) {
                        emf.setValid(false);
                        node.setValid(false);
                    }
                }
            }

            // list validation
            if (emf.getValidationType().contains("list")) {
                List<String> possibleValues = emf.getSelectItemList();

                for (IFieldValue fv : emf.getValues()) {

                    if (!fv.getMultiselectSelectedValues().isEmpty()) {
                        for (String value : fv.getMultiselectSelectedValues()) {
                            if (!possibleValues.contains(value)) {
                                emf.setValid(false);
                                node.setValid(false);
                            }
                        }
                    } else if (StringUtils.isNotBlank(fv.getValue()) && !possibleValues.contains(fv.getValue())) {
                        emf.setValid(false);
                        node.setValid(false);
                    }

                }
            }
        }

    }

    public void updateAreaDisplay(int level) {
        switch (level) {
            case 1:
                displayIdentityStatementArea = true;
                break;
            case 2:
                displayContextArea = true;
                break;
            case 3:
                displayContentArea = true;
                break;
            case 4:
                displayAccessArea = true;
                break;
            case 5:
                displayMaterialsArea = true;
                break;
            case 6:
                displayNotesArea = true;
                break;
            case 7:
                displayControlArea = true;
                break;
            default:
        }
    }

    //Create process for all leaves of the selected node which do not have processes
    public void createProcesses() {

        // abort if no node is selected
        if (selectedEntry == null) {
            Helper.setFehlerMeldung("plugin_administration_archive_please_select_node");
            return;
        }
        if (selectedEntry.getNodeType() == null) {
            return;
        }

        List<Process> processList = new ArrayList<>();
        createProcessesForChildren(selectedEntry, processList);
        // if more than one process was created, put them into a batch
        if (processList.size() > 1) {
            Batch batch = new Batch();
            ProcessManager.saveBatch(batch);
            for (Process proc : processList) {
                proc.setBatch(batch);
                try {
                    ProcessManager.saveProcess(proc);
                } catch (DAOException e) {
                    log.error(e);
                }
            }
        }
    }

    private void createProcessesForChildren(IEadEntry currentEntry, List<Process> processList) {

        setSelectedEntry(currentEntry);
        if (!currentEntry.isHasChildren() && currentEntry.getGoobiProcessTitle() == null) {
            try {
                Process proc = createProcess();
                processList.add(proc);
                Helper.setMeldung("Created " + currentEntry.getGoobiProcessTitle() + " for " + currentEntry.getLabel());
            } catch (Exception e) {
                Helper.setFehlerMeldung(e.getMessage());
                log.error(e);
            }
        } else if (currentEntry.isHasChildren()) {

            for (IEadEntry childEntry : currentEntry.getSubEntryList()) {
                createProcessesForChildren(childEntry, processList);
            }
        }
    }

    public void updateGoobiIds() {

        IEadEntry selected = getSelectedEntry();
        if (selected == null) {
            selectedEntry = rootElement;
            selected = rootElement;
        }
        List<String> lstNodesWithoutIds = removeInvalidProcessIds();

        checkGoobiProcessesForArchiveRefs(lstNodesWithoutIds);

        setSelectedEntry(selected);
    }

    /**
     * Remove goobi ids for processes which have been deleted
     * 
     * @return A list of all node ids which have no valid goobi Id
     */
    public List<String> removeInvalidProcessIds() {

        List<String> lstNodesWithoutIds = new ArrayList<>();

        // abort if no node is selected
        if (selectedEntry == null) {
            Helper.setFehlerMeldung("plugin_administration_archive_please_select_node");
            return lstNodesWithoutIds;
        }
        if (selectedEntry.getNodeType() == null) {
            return lstNodesWithoutIds;
        }

        lstNodesWithoutIds = removeInvalidProcessIdsForChildren(selectedEntry);

        return lstNodesWithoutIds;
    }

    private List<String> removeInvalidProcessIdsForChildren(IEadEntry currentEntry) {

        List<String> lstNodesWithoutIds = new ArrayList<>();

        String goobiProcessTitle = currentEntry.getGoobiProcessTitle();

        // check if process exists, otherwise remove link
        if (!currentEntry.isHasChildren() && goobiProcessTitle != null) {
            try {
                String strProcessTitle = currentEntry.getGoobiProcessTitle();
                if (ProcessManager.countProcessTitle(strProcessTitle, null) == 0) {
                    currentEntry.setGoobiProcessTitle(null);
                    ArchiveManagementManager.updateProcessLink(currentEntry);
                    Helper.setMeldung("Removing " + strProcessTitle + " from " + currentEntry.getLabel());
                }
            } catch (Exception e) {
                Helper.setFehlerMeldung(e.getMessage());
                log.error(e);
            }
        }

        if (currentEntry.isHasChildren()) {

            for (IEadEntry childEntry : currentEntry.getSubEntryList()) {
                lstNodesWithoutIds.addAll(removeInvalidProcessIdsForChildren(childEntry));
            }
        } else if (goobiProcessTitle == null) {
            // default option, use id to match nodes
            if ("id".equalsIgnoreCase(config.getIdentifierNodeName())) {
                lstNodesWithoutIds.add(currentEntry.getId());
            } else {
                // otherwise find configured metadata
                String value = ArchiveManagementManager.getMetadataValue(config.getIdentifierNodeName(), recordGroup.getId(), currentEntry.getId());
                if (StringUtils.isNotBlank(value)) {
                    lstNodesWithoutIds.add(value);

                }
                //                IMetadataField metadataField = null;
                //                for (IMetadataField field : currentEntry.getIdentityStatementAreaList()) {
                //                    if (field.getName().equals(identifierNodeName)) {
                //                        metadataField = field;
                //                    }
                //                }
                //
                //                for (IMetadataField field : currentEntry.getContextAreaList()) {
                //                    if (field.getName().equals(identifierNodeName)) {
                //                        metadataField = field;
                //                    }
                //                }
                //                for (IMetadataField field : currentEntry.getContentAndStructureAreaAreaList()) {
                //                    if (field.getName().equals(identifierNodeName)) {
                //                        metadataField = field;
                //                    }
                //                }
                //                for (IMetadataField field : currentEntry.getAccessAndUseAreaList()) {
                //                    if (field.getName().equals(identifierNodeName)) {
                //                        metadataField = field;
                //                    }
                //                }
                //                for (IMetadataField field : currentEntry.getAlliedMaterialsAreaList()) {
                //                    if (field.getName().equals(identifierNodeName)) {
                //                        metadataField = field;
                //                    }
                //                }
                //                for (IMetadataField field : currentEntry.getNotesAreaList()) {
                //                    if (field.getName().equals(identifierNodeName)) {
                //                        metadataField = field;
                //                    }
                //                }
                //                for (IMetadataField field : currentEntry.getDescriptionControlAreaList()) {
                //                    if (field.getName().equals(identifierNodeName)) {
                //                        metadataField = field;
                //                    }
                //                }
                //                if (metadataField != null) {
                //                    lstNodesWithoutIds.add(metadataField.getValues().get(0).getValue());
                //                }
            }
        }

        return lstNodesWithoutIds;
    }

    /**
     * For each node id in the list, find if there is a goobi process with that id as metadatum, and if so set the goobi id in the node.
     * 
     * @param lstNodesWithoutIds
     * @return A list of labels for any nodes which have been given a new goobi id
     */
    private List<String> checkGoobiProcessesForArchiveRefs(List<String> lstNodesWithoutIds) {

        List<String> lstNodesWithNewIds = new ArrayList<>();

        if (lstNodesWithoutIds.isEmpty()) {
            return lstNodesWithNewIds;
        }

        List<Integer> lstProcessIds = getProcessWithNodeIds(lstNodesWithoutIds);

        if (lstProcessIds == null || lstProcessIds.isEmpty()) {
            return lstNodesWithNewIds;
        }

        //otherwise
        for (Integer processId : lstProcessIds) {

            String strNodeId = MetadataManager.getMetadataValue(processId, config.getIdentifierMetadataName());
            IEadEntry node = getNodeWithId(strNodeId, rootElement);
            if (node != null) {
                String strProcessTitle = ProcessManager.getProcessById(processId).getTitel();
                node.setGoobiProcessTitle(strProcessTitle);
                lstNodesWithNewIds.add(node.getLabel());
                Helper.setMeldung("Node '" + node.getLabel() + "' has been given Goobi process ID: " + strProcessTitle);
                ArchiveManagementManager.updateProcessLink(node);
            }
        }

        return lstNodesWithNewIds;
    }

    private IEadEntry getNodeWithId(String strNodeId, IEadEntry node) {
        loadMetadataForNode(node);
        if ("id".equalsIgnoreCase(config.getIdentifierNodeName())) {
            if (node.getId() != null && node.getId().contentEquals(strNodeId)) {
                return node;
            }
        } else {
            for (IMetadataField field : node.getIdentityStatementAreaList()) {

                if (findMatchingNode(strNodeId, field)) {
                    return node;
                }
            }

            for (IMetadataField field : node.getContextAreaList()) {
                if (findMatchingNode(strNodeId, field)) {
                    return node;
                }
            }
            for (IMetadataField field : node.getContentAndStructureAreaAreaList()) {
                if (findMatchingNode(strNodeId, field)) {
                    return node;
                }
            }
            for (IMetadataField field : node.getAccessAndUseAreaList()) {
                if (findMatchingNode(strNodeId, field)) {
                    return node;
                }
            }
            for (IMetadataField field : node.getAlliedMaterialsAreaList()) {
                if (findMatchingNode(strNodeId, field)) {
                    return node;
                }
            }
            for (IMetadataField field : node.getNotesAreaList()) {
                if (findMatchingNode(strNodeId, field)) {
                    return node;
                }
            }
            for (IMetadataField field : node.getDescriptionControlAreaList()) {
                if (findMatchingNode(strNodeId, field)) {
                    return node;
                }
            }
        }

        if (node.getSubEntryList() != null) {
            for (IEadEntry child : node.getSubEntryList()) {
                IEadEntry found = getNodeWithId(strNodeId, child);
                if (found != null) {
                    return found;
                }
            }
        }

        //otherwise
        return null;
    }

    private boolean findMatchingNode(String strNodeId, IMetadataField field) {
        if (strNodeId == null) {
            return false;
        }
        if (config.getIdentifierNodeName().equals(field.getName())) {
            for (IFieldValue fv : field.getValues()) {
                //TODO person, corp
                if (strNodeId.equals(fv.getValue())) {
                    return true;
                }
            }
        }
        return false;
    }

    public List<Integer> getProcessWithNodeIds(List<String> lstNodesWithoutIds) {

        List<Integer> processIds = new ArrayList<>();

        StringBuilder strSQLNodes = new StringBuilder("('").append(lstNodesWithoutIds.get(0)).append("'");

        for (int i = 1; i < lstNodesWithoutIds.size(); i++) {

            strSQLNodes.append(", ").append("'").append(lstNodesWithoutIds.get(i)).append("'");
        }
        strSQLNodes.append(")");

        StringBuilder sql = new StringBuilder();

        sql.append("SELECT processid FROM metadata WHERE name = '" + config.getIdentifierMetadataName() + "' and value in ");
        sql.append(strSQLNodes.toString());
        sql.append(" order by processid;");
        @SuppressWarnings("unchecked")
        List<Object> rawData = ProcessManager.runSQL(sql.toString());
        for (int i = 0; i < rawData.size(); i++) {
            Object[] rowData = (Object[]) rawData.get(i);
            String processid = (String) rowData[0];
            processIds.add(Integer.parseInt(processid));
        }

        return processIds;
    }

    /**
     * duplicate selected node and insert it as the last sibling
     */

    public void duplicateNode() {
        // abort if no node is selected
        if (selectedEntry == null) {
            return;
        }
        if (selectedEntry.getNodeType() == null) {
            return;
        }
        // abort if root node is selected
        if (selectedEntry.getParentNode() == null) {
            return;
        }
        IEadEntry copy = selectedEntry.deepCopy(duplicationConfiguration);
        if (copy != null) {
            // add new field at the last position
            copy.setOrderNumber(selectedEntry.getParentNode().getSubEntryList().size() + 1);
            selectedEntry.getParentNode().addSubEntry(copy);

            saveNodeAndChildren(copy);

            setSelectedEntry(copy);

            resetFlatList();
        }
    }

    private void saveNodeAndChildren(IEadEntry copy) {
        copy.calculateFingerprint();
        ArchiveManagementManager.saveNode(recordGroup.getId(), copy);
        for (IEadEntry child : copy.getSubEntryList()) {
            saveNodeAndChildren(child);
        }

    }

    public void duplicateEadFile() {

        if (selectedEntry != null) {
            // save current data
            // update node history, if node was changed
            String oldFingerPrint = selectedEntry.getFingerprint();
            selectedEntry.calculateFingerprint();
            String newFingerPrint = selectedEntry.getFingerprint();

            if (StringUtils.isNotBlank(oldFingerPrint) && StringUtils.isNotBlank(newFingerPrint) && !oldFingerPrint.equals(newFingerPrint)) {
                updateChangeHistory(selectedEntry);
            }
            ArchiveManagementManager.saveNode(recordGroup.getId(), selectedEntry);
        }
        // create new recordGroup
        RecordGroup clonedRecordGroup = new RecordGroup();
        clonedRecordGroup.setTitle(recordGroup.getTitle() + " - copy");

        // load all metadata and all nodes for current record group
        loadMetadataForAllNodes();

        // duplicate rootElement and its children
        IEadEntry newRootElement = rootElement.deepCopy(getDuplicationConfiguration());

        // save new data
        ArchiveManagementManager.saveRecordGroup(clonedRecordGroup);
        ArchiveManagementManager.saveNodes(clonedRecordGroup.getId(), newRootElement.getAllNodes());
    }

    public void duplicateEadFileFromOverview() {
        // load selected file
        loadSelectedDatabase();
        // duplicate
        duplicateEadFile();

        // stay in overview
        databaseName = null;
    }

    public IConfiguration getDuplicationConfiguration() {
        if (duplicationConfiguration == null && rootElement != null) {
            duplicationConfiguration = new DuplicationConfiguration(rootElement);
        }
        return duplicationConfiguration;
    }

    /**
     * Replace an existing node in the EAD document in <baseX with the selected node.
     * 
     * The method searches for a node with the same id in the selected XML file and replaces the complete node and its children with the new content
     * 
     */

    @Override
    public void updateSingleNode() {
        if (selectedEntry != null) {
            if (selectedEntry.getNodeType() == null && config.getConfiguredNodes() != null) {
                selectedEntry.setNodeType(config.getConfiguredNodes().get(0));
            }
            // update node history, if node was changed
            String oldFingerPrint = selectedEntry.getFingerprint();
            selectedEntry.calculateFingerprint();
            String newFingerPrint = selectedEntry.getFingerprint();

            if (StringUtils.isNotBlank(oldFingerPrint) && StringUtils.isNotBlank(newFingerPrint) && !oldFingerPrint.equals(newFingerPrint)) {
                updateChangeHistory(selectedEntry);
            }
            ArchiveManagementManager.saveNode(recordGroup.getId(), selectedEntry);
        }
    }

    private void loadMetadataForNode(IEadEntry entry) {
        // clear old, outdated metadata
        if (entry.getDatabaseId() != null) {
            entry.getIdentityStatementAreaList().clear();
            entry.getContextAreaList().clear();
            entry.getContentAndStructureAreaAreaList().clear();
            entry.getAccessAndUseAreaList().clear();
            entry.getAlliedMaterialsAreaList().clear();
            entry.getNotesAreaList().clear();
            entry.getDescriptionControlAreaList().clear();

            Map<String, List<IValue>> metadata = ArchiveManagementManager.loadMetadataForNode(entry.getDatabaseId());

            for (IMetadataField emf : config.getConfiguredFields()) {
                if (emf.isGroup()) {
                    List<IValue> groups = metadata.get(emf.getName());
                    NodeInitializer.loadGroupMetadata(entry, emf, groups);
                } else {
                    List<IValue> values = metadata.get(emf.getName());
                    IMetadataField toAdd = NodeInitializer.addFieldToEntry(entry, emf, values);
                    NodeInitializer.addFieldToNode(entry, toAdd);
                }
            }
            entry.calculateFingerprint();
        }
    }

    private void loadMetadataForAllNodes() {

        List<IEadEntry> allNodes = rootElement.getAllNodes();

        for (IEadEntry entry : allNodes) {
            Map<String, List<IValue>> metadata = ArchiveManagementManager.convertStringToMap(entry.getData());
            entry.getIdentityStatementAreaList().clear();
            entry.getContextAreaList().clear();
            entry.getContentAndStructureAreaAreaList().clear();
            entry.getAccessAndUseAreaList().clear();
            entry.getAlliedMaterialsAreaList().clear();
            entry.getNotesAreaList().clear();
            entry.getDescriptionControlAreaList().clear();
            for (IMetadataField emf : config.getConfiguredFields()) {
                List<IValue> values = metadata.get(emf.getName());
                if (emf.isGroup()) {
                    NodeInitializer.loadGroupMetadata(entry, emf, values);
                } else {
                    IMetadataField toAdd = NodeInitializer.addFieldToEntry(entry, emf, values);
                    NodeInitializer.addFieldToNode(entry, toAdd);
                }
            }
        }
    }

    // link nodes to other nodes

    public List<IEadEntry> getLinkNodeList() {
        if (isDisplayLinkedModal() && linkNodeList == null) {
            linkNodeList = rootElement.getAllNodes();
        }

        return linkNodeList;
    }

    public void linkNode() {
        String idToLink = destinationEntry.getId();
        fieldToLink.setValue(idToLink);
        displayLinkedModal = false;
    }

    public void addGroup() {
        NodeInitializer.addGroupData(selectedGroup, null);
    }

    public void showAllFields() {
        if (selectedEntry != null) {

            if (displayAllFields) {
                hideAllFields();
            } else {

                displayIdentityStatementArea = true;
                displayContextArea = true;
                displayContentArea = true;
                displayAccessArea = true;
                displayMaterialsArea = true;
                displayNotesArea = true;
                displayControlArea = true;

                for (IMetadataField field : selectedEntry.getIdentityStatementAreaList()) {
                    field.setShowField(true);
                }
                for (IMetadataField field : selectedEntry.getContextAreaList()) {
                    field.setShowField(true);
                }
                for (IMetadataField field : selectedEntry.getContentAndStructureAreaAreaList()) {
                    field.setShowField(true);
                }
                for (IMetadataField field : selectedEntry.getAccessAndUseAreaList()) {
                    field.setShowField(true);
                }
                for (IMetadataField field : selectedEntry.getAlliedMaterialsAreaList()) {
                    field.setShowField(true);
                }
                for (IMetadataField field : selectedEntry.getNotesAreaList()) {
                    field.setShowField(true);
                }
                for (IMetadataField field : selectedEntry.getDescriptionControlAreaList()) {
                    field.setShowField(true);
                }
            }
        }
        displayAllFields = !displayAllFields;
    }

    public void hideAllFields() {
        if (selectedEntry != null) {
            displayIdentityStatementArea = false;
            displayContextArea = false;
            displayContentArea = false;
            displayAccessArea = false;
            displayMaterialsArea = false;
            displayNotesArea = false;
            displayControlArea = false;

            for (IMetadataField field : selectedEntry.getIdentityStatementAreaList()) {
                if (!field.isFilled()) {
                    field.setShowField(false);
                }
            }
            for (IMetadataField field : selectedEntry.getContextAreaList()) {
                if (!field.isFilled()) {
                    field.setShowField(false);
                }
            }
            for (IMetadataField field : selectedEntry.getContentAndStructureAreaAreaList()) {
                if (!field.isFilled()) {
                    field.setShowField(false);
                }
            }
            for (IMetadataField field : selectedEntry.getAccessAndUseAreaList()) {
                if (!field.isFilled()) {
                    field.setShowField(false);
                }
            }
            for (IMetadataField field : selectedEntry.getAlliedMaterialsAreaList()) {
                if (!field.isFilled()) {
                    field.setShowField(false);
                }
            }
            for (IMetadataField field : selectedEntry.getNotesAreaList()) {
                if (!field.isFilled()) {
                    field.setShowField(false);
                }
            }
            for (IMetadataField field : selectedEntry.getDescriptionControlAreaList()) {
                if (!field.isFilled()) {
                    field.setShowField(false);
                }
            }
        }
    }

    // add multiple nodes to current node

    public List<IParameter> getMetadataToAdd() {
        if (metadataToAdd == null && rootElement != null) {
            metadataToAdd = new ArrayList<>();
            metadataToAdd.add(new DuplicationParameter(""));
            metadataToAdd.add(new DuplicationParameter(""));
            metadataToAdd.add(new DuplicationParameter(""));
        }
        return metadataToAdd;
    }

    public void addMetadataRow() {
        metadataToAdd.add(new DuplicationParameter(""));
    }

    public void removeMetadataRow(IParameter row) {
        metadataToAdd.remove(row);
    }

    public void addNodes() {
        if (selectedEntry != null) {
            INodeType selectedNodeType = null;
            for (INodeType node : config.getConfiguredNodes()) {
                if (node.getNodeName().equals(nodeType)) {
                    selectedNodeType = node;
                }
            }
            int nodes = Integer.parseInt(numberOfNodes);
            for (int counter = 0; counter < nodes; counter++) {
                IEadEntry entry =
                        new EadEntry(selectedEntry.isHasChildren() ? selectedEntry.getSubEntryList().size() : 0,
                                selectedEntry.getHierarchy() + 1);
                entry.setId("id_" + UUID.randomUUID());
                entry.setNodeType(selectedNodeType);
                selectedEntry.addSubEntry(entry);

                // initialize all metadata fields
                for (IMetadataField emf : config.getConfiguredFields()) {
                    if (emf.isGroup()) {
                        NodeInitializer.loadGroupMetadata(entry, emf, null);
                    } else {
                        IParameter configuredField = null;
                        for (IParameter param : metadataToAdd) {
                            if (emf.getName().equals(param.getFieldName())) {
                                configuredField = param;
                            }
                        }
                        List<IValue> metadataValues = new ArrayList<>();
                        if (configuredField != null) {
                            String value = null;
                            switch (configuredField.getFieldType()) {
                                case "generated":
                                    value = "id_" + UUID.randomUUID();
                                    break;
                                case "text":
                                    value = configuredField.getPrefix();
                                    break;
                                case "counter":
                                    value = configuredField.getPrefix()
                                            + String.format(configuredField.getCounterFormat(), configuredField.getCounterStartValue() + counter);
                                    break;
                                default:
                                    break;
                            }

                            IValue val = null;
                            if ("person".equalsIgnoreCase(emf.getFieldType())) {
                                val = new PersonValue(emf.getName(), value, "", null, null);
                            } else if ("corporate".equalsIgnoreCase(emf.getFieldType())) {
                                val = new CorporateValue(emf.getName(), value, "", "", null, null);
                            } else {
                                val = new ExtendendValue(emf.getName(), value, null, null);
                            }
                            metadataValues.add(val);
                        }

                        IMetadataField toAdd = NodeInitializer.addFieldToEntry(entry, emf, metadataValues);
                        NodeInitializer.addFieldToNode(entry, toAdd);
                    }
                }
                saveNodeAndChildren(entry);
            }

            selectedEntry.setDisplayChildren(true);
            resetFlatList();
        }

    }

    public boolean isReadOnlyModus() {
        return readOnlyMode;
    }

    public void eadExport() {
        List<String> exportFolders = config.getExportConfiguration().get(databaseName);
        if (exportFolders == null || exportFolders.isEmpty()) {
            exportFolders = config.getExportConfiguration().get("*");
            if (exportFolders == null || exportFolders.isEmpty()) {
                Helper.setFehlerMeldung("plugin_administration_archive_eadExportNotConfigured");
                databaseName = null;
                return;
            }
        }

        Document document = createEadFile();
        XMLOutputter outputter = new XMLOutputter(Format.getPrettyFormat());

        for (String exportFolder : exportFolders) {
            Path folder = Paths.get(exportFolder);
            if (!StorageProvider.getInstance().isFileExists(folder)) {
                try {
                    StorageProvider.getInstance().createDirectories(folder);
                } catch (IOException e) {
                    log.error(e);
                }
            }
            try {
                // export into temporary file, copy temp file to destination, remove temp file
                Path downloadFile = Paths.get(exportFolder, databaseName.replace(" ", "_"));
                Path tempFile = StorageProvider.getInstance().createTemporaryFile(databaseName.replace(" ", "_"), "xml");
                outputter.output(document, Files.newOutputStream(tempFile));
                StorageProvider.getInstance().copyFile(tempFile, downloadFile);
                StorageProvider.getInstance().deleteFile(tempFile);
            } catch (IOException e) {
                log.error(e);
            }
        }
    }

    public void eadExportFromOverview() {
        recordGroup = ArchiveManagementManager.getRecordGroupByTitle(databaseName);
        eadExport();
        databaseName = null;
    }

    public void initializeRecord() {
        try {
            ExtendedVocabulary vocabulary = config.getVocabularyAPI().vocabularies().findByName(vocabularyField.getVocabularyName());
            newRecord = config.getVocabularyAPI().vocabularyRecords().createEmptyRecord(vocabulary.getId(), null, false);
        } catch (Exception e) {
            // configured vocabulary does not exist or vocabulary server is down
            newRecord = null;
            displayVocabularyModal = false;
        }
    }

    public void addEntry() {
        // save new record
        config.getVocabularyAPI().vocabularyRecords().save(newRecord);

        // populate new item list
        config.loadVocabulary(vocabularyField);

        // update template field
        for (IMetadataField template : config.getConfiguredFields()) {
            if (template.getName().equals(vocabularyField.getName())) {
                template.setSelectItemList(vocabularyField.getSelectItemList());
            }

        }
    }

    public void deleteSelectedDatabase() {
        if (allowDeletion) {
            if (StringUtils.isBlank(databaseName) || "null".equals(databaseName)) {
                // show error text
                databaseName = null;
                Helper.setFehlerMeldung("plugin_administration_archive_noArchiveSelected");
                return;
            }
            // delete selected data
            ArchiveManagementManager.deleteRecordGroup(databaseName);
            // clean current selection
            databaseName = null;
        }
    }

    public void clearAdvancedSearch() {
        for (StringPair sp : advancedSearch) {
            sp.setOne("");
            sp.setTwo("");
        }
    }

    public void abortFileUpload() {
        uploadFile = null;
    }

}
