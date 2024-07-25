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
import java.util.Optional;
import java.util.UUID;

import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.servlet.ServletContext;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Part;

import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.commons.configuration.XMLConfiguration;
import org.apache.commons.configuration.reloading.FileChangedReloadingStrategy;
import org.apache.commons.configuration.tree.xpath.XPathExpressionEngine;
import org.apache.commons.lang3.StringUtils;
import org.goobi.beans.Batch;
import org.goobi.beans.Process;
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
import org.goobi.interfaces.IRecordGroup;
import org.goobi.interfaces.IValue;
import org.goobi.managedbeans.VocabularyRecordsBean;
import org.goobi.model.ExtendendValue;
import org.goobi.model.GroupValue;
import org.goobi.production.cli.helper.StringPair;
import org.goobi.production.enums.PluginType;
import org.jdom2.Attribute;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.Namespace;
import org.jdom2.Text;
import org.jdom2.filter.Filters;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;
import org.jdom2.xpath.XPathExpression;
import org.jdom2.xpath.XPathFactory;

import de.intranda.goobi.plugins.model.DuplicationConfiguration;
import de.intranda.goobi.plugins.model.DuplicationParameter;
import de.intranda.goobi.plugins.model.EadEntry;
import de.intranda.goobi.plugins.model.EadMetadataField;
import de.intranda.goobi.plugins.model.FieldValue;
import de.intranda.goobi.plugins.model.NodeType;
import de.intranda.goobi.plugins.model.RecordGroup;
import de.intranda.goobi.plugins.model.TitleComponent;
import de.intranda.goobi.plugins.persistence.ArchiveManagementManager;
import de.sub.goobi.config.ConfigurationHelper;
import de.sub.goobi.helper.BeanHelper;
import de.sub.goobi.helper.FacesContextHelper;
import de.sub.goobi.helper.Helper;
import de.sub.goobi.helper.ProcessTitleGenerator;
import de.sub.goobi.helper.ScriptThreadWithoutHibernate;
import de.sub.goobi.helper.XmlTools;
import de.sub.goobi.helper.enums.ManipulationType;
import de.sub.goobi.helper.enums.StepStatus;
import de.sub.goobi.helper.exceptions.DAOException;
import de.sub.goobi.persistence.managers.MetadataManager;
import de.sub.goobi.persistence.managers.ProcessManager;
import de.sub.goobi.validator.ExtendedDateTimeFormatLexer;
import de.sub.goobi.validator.ExtendedDateTimeFormatParser;
import io.goobi.vocabulary.exchange.FieldDefinition;
import io.goobi.vocabulary.exchange.Vocabulary;
import io.goobi.vocabulary.exchange.VocabularySchema;
import io.goobi.workflow.api.vocabulary.APIException;
import io.goobi.workflow.api.vocabulary.VocabularyAPIManager;
import io.goobi.workflow.api.vocabulary.jsfwrapper.JSFVocabulary;
import io.goobi.workflow.api.vocabulary.jsfwrapper.JSFVocabularyRecord;
import io.goobi.workflow.locking.LockingBean;
import io.goobi.workflow.xslt.XsltToPdf;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.log4j.Log4j2;
import net.xeoh.plugins.base.annotations.PluginImplementation;
import ugh.dl.DigitalDocument;
import ugh.dl.DocStruct;
import ugh.dl.Fileformat;
import ugh.dl.Metadata;
import ugh.dl.MetadataGroup;
import ugh.dl.MetadataType;
import ugh.dl.Prefs;
import ugh.exceptions.DocStructHasNoTypeException;
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

    private Namespace nameSpaceRead;
    @Getter
    @Setter
    private Namespace nameSpaceWrite;
    private static XPathFactory xFactory = XPathFactory.instance();

    @Getter
    @Setter
    private XMLConfiguration xmlConfig;
    @Getter
    private transient List<IMetadataField> configuredFields;

    @Getter
    private transient List<INodeType> configuredNodes;

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

    private String nodeDefaultTitle;

    private SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    private String username = "";

    @Getter
    @Setter
    private transient Part uploadFile;

    private List<StringPair> processTemplates = new ArrayList<>();
    @Getter
    @Setter
    private String selectedTemplate;

    // maximum length of each component that is to be used to generate the process title
    private int lengthLimit;
    // separator that will be used to join all components into a process title
    private String separator;

    private transient List<TitleComponent> titleParts = new ArrayList<>();
    // true if the id to be used should be taken from current node's parent node, false if it should be of the current node
    private boolean useIdFromParent;

    // true if the selected file to upload already has an older version in the same path, false otherwise
    @Getter
    @Setter
    private boolean fileToUploadExists = false;

    private String identifierMetadataName = "NodeId";
    private String identifierNodeName = "id";

    private transient IConfiguration duplicationConfiguration = null;

    @Getter
    private List<String> advancedSearchFields = new ArrayList<>();

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
    @Getter
    private List<String> metadataFieldNames = new ArrayList<>();

    private boolean readOnlyMode = true;

    @Getter
    private boolean allowFileUpload = false;

    @Getter
    private boolean allowCreation = false;

    @Getter
    private boolean allowVocabularyEdition = false;

    private boolean allowAllInventories;
    private List<String> inventoryList = new ArrayList<>();

    @Getter
    private String exportFolder;

    private transient VocabularyAPIManager vocabularyAPI = VocabularyAPIManager.getInstance();

    @Getter
    @Setter
    private transient IMetadataField vocabularyField;

    /**
     * Constructor
     */
    public ArchiveManagementAdministrationPlugin() {
        try {
            xmlConfig = new XMLConfiguration(
                    ConfigurationHelper.getInstance().getConfigurationFolder() + "plugin_intranda_administration_archive_management.xml");
            xmlConfig.setListDelimiter('&');
            xmlConfig.setReloadingStrategy(new FileChangedReloadingStrategy());
            xmlConfig.setExpressionEngine(new XPathExpressionEngine());

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

                if ((user.isSuperAdmin() || user.getAllUserRoles().contains("Plugin_Administration_Archive_Management_Write"))) {
                    readOnlyMode = false;
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
            if (username == null || allowAllInventories || inventoryList.contains(rec.getTitle())) {
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
                readConfiguration();
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

            readConfiguration();
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
            INodeType rootType = new NodeType("root", null, "fa fa-home", 0);
            rootElement.setNodeType(rootType);

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
    public void parseEadFile(Document document) {
        Element eadElement = null;
        Element collection = document.getRootElement();
        if ("collection".equals(collection.getName())) {
            eadElement = collection.getChild("ead", nameSpaceRead);
        } else {
            eadElement = collection;
        }

        rootElement = parseElement(0, 0, eadElement);
        INodeType rootType = new NodeType("root", null, "fa fa-home", 0);
        rootElement.setNodeType(rootType);
        rootElement.setDisplayChildren(true);

        getDuplicationConfiguration();
    }

    /**
     * read the metadata for the current xml node. - create an {@link EadEntry} - execute the configured xpaths on the current node - add the metadata
     * to one of the 7 levels - check if the node has sub nodes - call the method recursively for all sub nodes
     */
    private IEadEntry parseElement(int order, int hierarchy, Element element) {
        IEadEntry entry = new EadEntry(order, hierarchy);

        for (IMetadataField emf : configuredFields) {
            if (emf.isGroup()) {
                // find group root element
                XPathExpression<Element> engine = xFactory.compile(emf.getXpath(), Filters.element(), null, nameSpaceRead);
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
                loadGroupMetadata(entry, emf, groups);
            } else {
                List<IValue> valueList = getValuesFromXml(element, emf);
                IMetadataField toAdd = addFieldToEntry(entry, emf, valueList);
                addFieldToNode(entry, toAdd);
            }
        }

        Element eadheader = element.getChild("eadheader", nameSpaceRead);

        entry.setId(element.getAttributeValue("id"));
        if (eadheader != null) {
            try {
                Element filedesc = eadheader.getChild("filedesc", nameSpaceRead);
                if (filedesc != null) {
                    Element titlestmt = filedesc.getChild("titlestmt", nameSpaceRead);
                    if (titlestmt != null) {
                        String titleproper = titlestmt.getChildText("titleproper", nameSpaceRead);
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
        Element archdesc = element.getChild("archdesc", nameSpaceRead);
        if (archdesc != null) {
            String nodeTypeName = archdesc.getAttributeValue("localtype");
            for (INodeType nt : configuredNodes) {
                if (nt.getNodeName().equals(nodeTypeName)) {
                    entry.setNodeType(nt);
                }
            }
            Element dsc = archdesc.getChild("dsc", nameSpaceRead);
            if (dsc != null) {
                // read process title
                List<Element> altformavailList = dsc.getChildren("altformavail", nameSpaceRead);
                for (Element altformavail : altformavailList) {
                    if ("goobi_process".equals(altformavail.getAttributeValue("localtype"))) {
                        entry.setGoobiProcessTitle(altformavail.getText());
                    }
                }
                clist = dsc.getChildren("c", nameSpaceRead);
            }

        } else {
            String nodeTypeName = element.getAttributeValue("otherlevel");
            for (INodeType nt : configuredNodes) {
                if (nt.getNodeName().equals(nodeTypeName)) {
                    entry.setNodeType(nt);
                }
            }
            List<Element> altformavailList = element.getChildren("altformavail", nameSpaceRead);
            for (Element altformavail : altformavailList) {
                if ("goobi_process".equals(altformavail.getAttributeValue("localtype"))) {
                    entry.setGoobiProcessTitle(altformavail.getText());
                }
            }
        }
        if (entry.getNodeType() == null) {
            entry.setNodeType(configuredNodes.get(0));
        }
        if (clist == null) {
            clist = element.getChildren("c", nameSpaceRead);
        }
        if (clist != null) {
            int subOrder = 0;
            int subHierarchy = hierarchy + 1;
            for (Element c : clist) {

                IEadEntry child = parseElement(subOrder, subHierarchy, c);
                entry.addSubEntry(child);
                child.setParentNode(entry);
                subOrder++;
            }
        }

        // generate new id, if id is null
        if (entry.getId() == null) {
            entry.setId("id_" + UUID.randomUUID());
        }
        entry.calculateFingerprint();
        return entry;
    }

    private List<IValue> getValuesFromXml(Element element, IMetadataField emf) {
        List<IValue> valueList = new ArrayList<>();
        if ("text".equalsIgnoreCase(emf.getXpathType())) {
            XPathExpression<Text> engine = xFactory.compile(emf.getXpath(), Filters.text(), null, nameSpaceRead);
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
            XPathExpression<Attribute> engine = xFactory.compile(emf.getXpath(), Filters.attribute(), null, nameSpaceRead);
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
        } else {
            XPathExpression<Element> engine = xFactory.compile(emf.getXpath(), Filters.element(), null, nameSpaceRead);
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

    /**
     * Add the metadata to the configured level
     */

    private IMetadataField addFieldToEntry(IEadEntry entry, IMetadataField emf, List<IValue> values) {

        IMetadataField toAdd = new EadMetadataField(emf.getName(), emf.getLevel(), emf.getXpath(), emf.getXpathType(), emf.isRepeatable(),
                emf.isVisible(), emf.isShowField(), emf.getFieldType(), emf.getMetadataName(), emf.isImportMetadataInChild(), emf.getValidationType(),
                emf.getRegularExpression(), emf.isSearchable(), emf.getViafSearchFields(), emf.getViafDisplayFields(), emf.isGroup(),
                emf.getVocabularyName());
        toAdd.setValidationError(emf.getValidationError());
        toAdd.setSelectItemList(emf.getSelectItemList());
        toAdd.setSearchParameter(emf.getSearchParameter());
        if (entry != null) {
            if (StringUtils.isBlank(entry.getLabel()) && emf.getXpath().contains("unittitle") && values != null && !values.isEmpty()) {
                IValue value = values.get(0);
                entry.setLabel(((ExtendendValue) value).getValue());
            }
            toAdd.setEadEntry(entry);
        }

        if (values != null && !values.isEmpty()) {
            toAdd.setShowField(true);

            // split single value into multiple fields
            for (IValue value : values) {
                ExtendendValue val = (ExtendendValue) value;
                IFieldValue fv = new FieldValue(toAdd);
                String stringValue = val.getValue();
                fv.setAuthorityType(val.getAuthorityType());
                fv.setAuthorityValue(val.getAuthorityValue());

                if ("multiselect".equals(toAdd.getFieldType()) && StringUtils.isNotBlank(stringValue)) {
                    String[] splittedValues = stringValue.split("; ");
                    for (String s : splittedValues) {
                        fv.setMultiselectValue(s);
                    }
                } else {
                    fv.setValue(stringValue);
                }
                toAdd.addFieldValue(fv);
            }
        } else {
            IFieldValue fv = new FieldValue(toAdd);
            toAdd.addFieldValue(fv);
        }
        return toAdd;

    }

    private void addFieldToNode(IEadEntry entry, IMetadataField toAdd) {
        switch (toAdd.getLevel()) {
            case 1:
                entry.getIdentityStatementAreaList().add(toAdd);
                break;
            case 2:
                entry.getContextAreaList().add(toAdd);
                break;
            case 3:
                entry.getContentAndStructureAreaAreaList().add(toAdd);
                break;
            case 4:
                entry.getAccessAndUseAreaList().add(toAdd);
                break;
            case 5:
                entry.getAlliedMaterialsAreaList().add(toAdd);
                break;
            case 6:
                entry.getNotesAreaList().add(toAdd);
                break;
            case 7:
                entry.getDescriptionControlAreaList().add(toAdd);
                break;
            default:
        }
    }

    /**
     * read in all parameters from the configuration file
     * 
     */
    public void readConfiguration() {
        log.debug("reading configuration");
        configuredFields = new ArrayList<>();
        configuredNodes = new ArrayList<>();

        HierarchicalConfiguration config = null;

        try {
            config = xmlConfig.configurationAt("//config[./archive = '" + databaseName + "']");
        } catch (IllegalArgumentException e) {
            try {
                config = xmlConfig.configurationAt("//config[./archive = '*']");
            } catch (IllegalArgumentException e1) {
                // do nothing
            }
        }

        exportFolder = xmlConfig.getString("/export/folder");

        nameSpaceRead = Namespace.getNamespace("ead", config.getString("/eadNamespaceRead", "urn:isbn:1-931666-22-9"));
        nameSpaceWrite = Namespace.getNamespace("ead", config.getString("/eadNamespaceWrite", "urn:isbn:1-931666-22-9"));

        for (String level : config.getStringArray("/showGroup/@level")) {
            switch (level) {
                case "1":
                    displayIdentityStatementArea = true;
                    break;
                case "2":
                    displayContextArea = true;
                    break;
                case "3":
                    displayContentArea = true;
                    break;
                case "4":
                    displayAccessArea = true;
                    break;
                case "5":
                    displayMaterialsArea = true;
                    break;
                case "6":
                    displayNotesArea = true;
                    break;
                case "7":
                    displayControlArea = true;
                    break;
                default:
                    break;
            }
        }
        identifierMetadataName = config.getString("/processIdentifierField", "NodeId");
        identifierNodeName = config.getString("/nodeIdentifierField", "id");

        nodeDefaultTitle = config.getString("/nodeDefaultTitle", "-");
        for (HierarchicalConfiguration hc : config.configurationsAt("/node")) {
            INodeType nt = new NodeType(hc.getString("@name"), hc.getString("@ruleset"), hc.getString("@icon"), hc.getInt("@processTemplateId"));
            configuredNodes.add(nt);
        }

        // configurations for generating process title
        lengthLimit = config.getInt("/lengthLimit", 0);
        separator = config.getString("/separator", "_");
        useIdFromParent = config.getBoolean("/useIdFromParent", false);

        for (HierarchicalConfiguration hc : config.configurationsAt("/title")) {
            String name = hc.getString("@name");
            String manipulationType = hc.getString("@type", null);
            String value = hc.getString("@value", "");
            TitleComponent comp = new TitleComponent(name, manipulationType, value);
            titleParts.add(comp);
        }
        advancedSearchFields = new ArrayList<>();
        advancedSearch = new ArrayList<>();
        advancedSearch.add(new StringPair());
        advancedSearch.add(new StringPair());
        advancedSearch.add(new StringPair());
        advancedSearch.add(new StringPair());
        advancedSearch.add(new StringPair());

        // configurations for metadata
        for (HierarchicalConfiguration fieldConfig : config.configurationsAt("/metadata")) {
            IMetadataField field = new EadMetadataField(fieldConfig.getString("@name"), fieldConfig.getInt("@level"), fieldConfig.getString("@xpath"),
                    fieldConfig.getString("@xpathType", "element"), fieldConfig.getBoolean("@repeatable", false),
                    fieldConfig.getBoolean("@visible", true),
                    fieldConfig.getBoolean("@showField", false), fieldConfig.getString("@fieldType", "input"),
                    fieldConfig.getString("@rulesetName", null),
                    fieldConfig.getBoolean("@importMetadataInChild", false), fieldConfig.getString("@validationType", null),
                    fieldConfig.getString("@regularExpression"),
                    fieldConfig.getBoolean("@searchable", false), fieldConfig.getString("@searchFields", null),
                    fieldConfig.getString("@displayFields", null),
                    fieldConfig.getBoolean("@group", false),
                    fieldConfig.getString("/vocabulary"));
            configureField(fieldConfig, field);
            configuredFields.add(field);
            // groups
            if (field.isGroup()) {
                for (HierarchicalConfiguration subfieldConfig : fieldConfig.configurationsAt("/metadata")) {
                    IMetadataField subfield = new EadMetadataField(subfieldConfig.getString("@name"), subfieldConfig.getInt("@level"),
                            subfieldConfig.getString("@xpath"),
                            subfieldConfig.getString("@xpathType", "element"), subfieldConfig.getBoolean("@repeatable", false),
                            subfieldConfig.getBoolean("@visible", true),
                            subfieldConfig.getBoolean("@showField", false), subfieldConfig.getString("@fieldType", "input"),
                            subfieldConfig.getString("@rulesetName", null),
                            subfieldConfig.getBoolean("@importMetadataInChild", false), subfieldConfig.getString("@validationType", null),
                            subfieldConfig.getString("@regularExpression"),
                            subfieldConfig.getBoolean("@searchable", false), subfieldConfig.getString("@searchFields", null),
                            subfieldConfig.getString("@displayFields", null),
                            false, subfieldConfig.getString("/vocabulary"));
                    configureField(subfieldConfig, subfield);
                    field.addSubfield(subfield);
                }
            }
            if (field.isVisible()) {
                metadataFieldNames.add(field.getName());
            }

        }
        ArchiveManagementManager.setConfiguredNodes(configuredNodes);
    }

    private void configureField(HierarchicalConfiguration fieldConfig, IMetadataField field) {
        if (field.isSearchable()) {
            advancedSearchFields.add(field.getName());
        }

        field.setValidationError(fieldConfig.getString("/validationError"));
        field.setSearchParameter(Arrays.asList(fieldConfig.getStringArray("/searchParameter")));

        if ("dropdown".equals(field.getFieldType()) || "multiselect".equals(field.getFieldType())) {
            List<String> valueList = Arrays.asList(fieldConfig.getStringArray("/value"));
            if (valueList == null || valueList.isEmpty()) {
                loadVocabulary(field);
            } else {
                field.setSelectItemList(valueList);
            }
        } else if ("vocabulary".equals(field.getFieldType())) {
            loadVocabulary(field);
        }
    }

    private void loadVocabulary(IMetadataField field) {
        List<String> iFieldValueList = new ArrayList<>();

        try {
            Vocabulary vocabulary = vocabularyAPI.vocabularies().findByName(field.getVocabularyName());
            if (field.getSearchParameter().isEmpty()) {
                List<JSFVocabularyRecord> records = vocabularyAPI.vocabularyRecords().all(vocabulary.getId());
                for (JSFVocabularyRecord rec : records) {
                    iFieldValueList.add(rec.getMainValue());
                }
            } else {
                if (field.getSearchParameter().size() > 1) {
                    Helper.setFehlerMeldung("vocabularyList with multiple fields is not supported right now");
                    return;
                }

                String[] parts = field.getSearchParameter().get(0).trim().split("=");
                if (parts.length != 2) {
                    Helper.setFehlerMeldung("Wrong field format");
                    return;
                }

                String searchFieldName = parts[0];
                String searchFieldValue = parts[1];

                VocabularySchema schema = vocabularyAPI.vocabularySchemas().get(vocabulary.getSchemaId());
                Optional<FieldDefinition> searchField = schema.getDefinitions()
                        .stream()
                        .filter(d -> d.getName().equals(searchFieldName))
                        .findFirst();

                if (searchField.isEmpty()) {
                    Helper.setFehlerMeldung("Field " + searchFieldName + " not found in vocabulary " + vocabulary.getName());
                    return;
                }

                List<JSFVocabularyRecord> records = vocabularyAPI.vocabularyRecords()
                        .search(vocabulary.getId(), searchField.get().getId() + ":" + searchFieldValue)
                        .getContent();
                for (JSFVocabularyRecord rec : records) {
                    iFieldValueList.add(rec.getMainValue());
                }
            }
        } catch (APIException e) {
            log.error(e);
            field.setVocabularyName(null);
        }
        Collections.sort(iFieldValueList);
        field.setSelectItemList(iFieldValueList);
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
            if (StringUtils.isNotBlank(nodeDefaultTitle)) {
                titleData.add(new ExtendendValue(null, nodeDefaultTitle, null, null));
                entry.setLabel(nodeDefaultTitle);
            }
            for (IMetadataField emf : configuredFields) {
                if (emf.isGroup()) {
                    loadGroupMetadata(entry, emf, null);
                } else if (emf.getXpath().contains("unittitle")) {
                    IMetadataField toAdd = addFieldToEntry(entry, emf, titleData);
                    addFieldToNode(entry, toAdd);
                } else {
                    addFieldToEntry(entry, emf, null);
                }
            }
            entry.setNodeType(selectedEntry.getNodeType());
            selectedEntry.addSubEntry(entry);
            selectedEntry.setDisplayChildren(true);
            selectedEntry.setSelected(false);
            selectedEntry = entry;
            selectedEntry.setSelected(true);
            flatEntryList = null;
            selectedEntry.calculateFingerprint();
            ArchiveManagementManager.saveNode(recordGroup.getId(), entry);
        }
    }

    public void deleteNode() {
        if (selectedEntry != null) {
            IEadEntry parentNode = selectedEntry.getParentNode();
            if (parentNode == null) {
                // abort, root node cannot be deleted
                return;
            }

            // remove the current node from parent
            parentNode.getSubEntryList().remove(selectedEntry);
            parentNode.reOrderElements();

            // get the current node and all of its children
            List<IEadEntry> nodesToDelete = selectedEntry.getAllNodes();

            // delete elements in the database
            ArchiveManagementManager.deleteNodes(nodesToDelete);
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
        readConfiguration();

        String uploadedFileName = processUploadedFileName(uploadFile);
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

                parseEadFile(document);

            }
        } catch (IOException e) {
            log.error(e);
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

        // save nodes
        ArchiveManagementManager.saveRecordGroup(recordGroup);
        List<IEadEntry> nodes = rootElement.getAllNodes();
        ArchiveManagementManager.saveNodes(recordGroup.getId(), nodes);
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
     * @param file the selected file as a javax.servlet.http.Part object
     * @return the corrected file name as a string
     */
    private String processUploadedFileName(Part file) {
        if (file == null) {
            return "";
        }

        String uploadedFileName = Paths.get(file.getSubmittedFileName()).getFileName().toString(); // MSIE fix.

        // filename must end with xml
        if (!uploadedFileName.endsWith(".xml")) {
            uploadedFileName = uploadedFileName + ".xml";
        }
        // remove whitespaces from filename
        uploadedFileName = uploadedFileName.replace(" ", "_");

        return uploadedFileName;
    }

    public void addMetadata(Element currentElement, IEadEntry node, Element xmlRootElement, boolean updateHistory) {
        boolean isMainElement = false;
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
        Element dsc = null;
        if (isMainElement) {
            if (StringUtils.isNotBlank(node.getId())) {
                currentElement.setAttribute("id", node.getId());
            }
            Element archdesc = currentElement.getChild("archdesc", nameSpaceWrite);
            if (archdesc == null) {
                archdesc = new Element("archdesc", nameSpaceWrite);
                currentElement.addContent(archdesc);
            }
            dsc = archdesc.getChild("dsc", nameSpaceWrite);
            if (dsc == null) {
                dsc = new Element("dsc", nameSpaceWrite);
                archdesc.addContent(dsc);
            }

            if (StringUtils.isNotBlank(node.getGoobiProcessTitle())) {
                Element altformavail = new Element("altformavail", nameSpaceWrite);
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
                dsc = new Element("dsc", nameSpaceWrite);
                currentElement.addContent(dsc);
            }

            Element c = new Element("c", nameSpaceWrite);
            dsc.addContent(c);
            if (StringUtils.isNotBlank(subNode.getGoobiProcessTitle())) {
                Element altformavail = new Element("altformavail", nameSpaceWrite);
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
                if (StringUtils.isNotBlank(fv.getValuesForXmlExport())) {
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
                addGroupData(field, grps);
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
            if (xpath.contains("ead:control")) {
                groupElement = xmlRootElement;
            }

            String lastElement = fields[fields.length - 1];

            String[] prevElements = Arrays.copyOf(fields, fields.length - 1);

            for (int i = 0; i < prevElements.length; i++) {
                String field = fields[i];
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
                    if (StringUtils.isNotBlank(fv.getValuesForXmlExport())) {
                        createEadXmlField(groupElement, isMainElement, subfield, fv, xmlRootElement);
                    }
                }
            }
        }
    }

    private void createEadXmlField(Element xmlElement, boolean isMainElement, IMetadataField emf, IFieldValue metadataValue, Element xmlRootElement) {
        if (StringUtils.isBlank(emf.getXpath())) {
            // don't export internal fields
            return;
        }

        Element currentElement = xmlElement;
        String xpath = getXpath(isMainElement, emf);

        if (xpath.contains("ead:control")) {
            currentElement = xmlRootElement;
        }

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
                    Element duplicate = new Element(currentElement.getName(), nameSpaceWrite);
                    for (Attribute attr : currentElement.getAttributes()) {
                        if (!attr.getName().equals(field)) {
                            duplicate.setAttribute(attr.getName(), attr.getValue());
                        }
                    }
                    currentElement.getParent().addContent(duplicate);
                    currentElement = duplicate;
                }

                currentElement.setAttribute(field, metadataValue.getValuesForXmlExport());
                written = true;
            } else {
                currentElement = findElement(field, currentElement);
            }
        }
        if (!written) {
            // duplicate current element if not empty
            if (StringUtils.isNotBlank(currentElement.getText()) || !currentElement.getChildren().isEmpty()) {
                Element duplicate = new Element(currentElement.getName(), nameSpaceWrite);
                for (Attribute attr : currentElement.getAttributes()) {
                    duplicate.setAttribute(attr.getName(), attr.getValue());
                }

                if (!currentElement.getChildren().isEmpty()) {
                    for (Element child : currentElement.getChildren()) {
                        Element duplicateChild = new Element(child.getName(), nameSpaceWrite);
                        duplicateChild.setText(child.getText());
                        duplicate.addContent(duplicateChild);
                    }
                }

                currentElement.getParent().addContent(duplicate);
                currentElement = duplicate;
            }

            currentElement.setText(metadataValue.getValuesForXmlExport());

            if (StringUtils.isNotBlank(metadataValue.getAuthorityType()) && StringUtils.isNotBlank(metadataValue.getAuthorityValue())) {
                currentElement.setAttribute("SOURCE", metadataValue.getAuthorityType());
                currentElement.setAttribute("AUTHFILENUMBER", metadataValue.getAuthorityValue());
            }
        }
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
        Element element = currentElement.getChild(field, nameSpaceWrite);
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
        Element element = new Element(field, nameSpaceWrite);
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
                        Element eltChild = new Element(attr[0], nameSpaceWrite);
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
        selectedEntry.getParentNode().getSubEntryList().remove(selectedEntry);

        selectedEntry.setParentNode(destinationEntry);
        destinationEntry.addSubEntry(selectedEntry);
        destinationEntry.reOrderElements();
        selectedEntry.updateHierarchy();
        ArchiveManagementManager.saveNode(recordGroup.getId(), selectedEntry);
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
        ArchiveManagementManager.saveNode(recordGroup.getId(), previousNode);
        ArchiveManagementManager.saveNode(recordGroup.getId(), selectedEntry);
        selectedEntry.getParentNode().sortElements();

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
        ArchiveManagementManager.saveNode(recordGroup.getId(), followingNode);
        ArchiveManagementManager.saveNode(recordGroup.getId(), selectedEntry);
        selectedEntry.getParentNode().sortElements();

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

    public List<StringPair> getProcessTemplates() {
        if (processTemplates.isEmpty()) {
            StringBuilder sql = new StringBuilder();
            sql.append("select ProzesseID, Titel from prozesse where IstTemplate = true and ProjekteID in ");
            sql.append("(select projekteId from projektbenutzer where BenutzerID=");
            sql.append(Helper.getCurrentUser().getId());
            sql.append(") order by titel;");
            @SuppressWarnings("unchecked")
            List<Object> rawData = ProcessManager.runSQL(sql.toString());
            for (int i = 0; i < rawData.size(); i++) {
                Object[] rowData = (Object[]) rawData.get(i);
                String processId = (String) rowData[0];
                String processTitle = (String) rowData[1];
                processTemplates.add(new StringPair(processId, processTitle));
            }
        }
        return processTemplates;
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

        // create process based on configured process template
        Process process = bhelp.createAndSaveNewProcess(processTemplate, processTitle, fileformat);

        // save current node
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
        titleGenerator.setSeparator(separator);
        titleGenerator.setBodyTokenLengthLimit(lengthLimit);
        for (TitleComponent comp : titleParts) {
            // set title type
            ManipulationType manipulationType = null;
            // check for special types
            switch (comp.getType().toLowerCase()) {
                case "camelcase":
                case "camel_case":
                case "camelcaselenghtlimited":
                case "camel_case_lenght_limited":
                    if (lengthLimit > 0) {
                        manipulationType = ManipulationType.CAMEL_CASE_LENGTH_LIMITED;
                    } else {
                        manipulationType = ManipulationType.CAMEL_CASE;
                    }

                    break;
                case "afterlastseparator":
                case "after_last_separator":
                    manipulationType = ManipulationType.AFTER_LAST_SEPARATOR;
                    break;
                case "beforefirstseparator":
                case "before_first_separator":
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
            IEadEntry idEntry = useIdFromParent ? selectedEntry.getParentNode() : selectedEntry;
            String valueOfFirstToken = idEntry.getId();
            titleGenerator.addToken(valueOfFirstToken, ManipulationType.BEFORE_FIRST_SEPARATOR);

            ManipulationType labelTokenType = lengthLimit > 0 ? ManipulationType.CAMEL_CASE_LENGTH_LIMITED : ManipulationType.CAMEL_CASE;
            String label = selectedEntry.getLabel();
            titleGenerator.addToken(label, labelTokenType);
        }
        return titleGenerator;
    }

    //  create metadata, add it to logical element
    private void createModsMetadata(Prefs prefs, IMetadataField emf, DocStruct logical) {
        //  groups
        if (StringUtils.isNotBlank(emf.getMetadataName())) {
            if (emf.isGroup()) {

                for (IMetadataGroup group : emf.getGroups()) {
                    try {
                        MetadataGroup mg = new MetadataGroup(prefs.getMetadataGroupTypeByName(emf.getMetadataName()));

                        for (IMetadataField subfield : group.getFields()) {

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
                    if (!fv.getMultiselectSelectedValues().isEmpty()) {
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
        // reload all nodes from db to get every change
        rootElement = ArchiveManagementManager.loadRecordGroup(recordGroup.getId());
        loadMetadataForAllNodes();

        Document document = new Document();

        Element eadRoot = new Element("ead", nameSpaceWrite);
        document.setRootElement(eadRoot);
        addMetadata(eadRoot, rootElement, eadRoot, true);

        return document;
    }

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
        configuredFields = null;
        searchValue = null;
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

        if (!currentEntry.isHasChildren() && goobiProcessTitle != null) {
            try {
                String strProcessTitle = currentEntry.getGoobiProcessTitle();
                Process process = ProcessManager.getProcessByTitle(strProcessTitle);
                if (process == null) {
                    currentEntry.setGoobiProcessTitle(null);
                    lstNodesWithoutIds.add(currentEntry.getId());

                    Helper.setMeldung("Removing " + strProcessTitle + " from " + currentEntry.getLabel());
                }
            } catch (Exception e) {
                Helper.setFehlerMeldung(e.getMessage());
                log.error(e);
            }
        } else if (currentEntry.isHasChildren()) {

            for (IEadEntry childEntry : currentEntry.getSubEntryList()) {
                lstNodesWithoutIds.addAll(removeInvalidProcessIdsForChildren(childEntry));
            }
        } else if (goobiProcessTitle == null) {
            lstNodesWithoutIds.add(currentEntry.getId());
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

            String strNodeId = MetadataManager.getMetadataValue(processId, identifierMetadataName);
            IEadEntry node = getNodeWithId(strNodeId, rootElement);
            if (node != null) {
                String strProcessTitle = ProcessManager.getProcessById(processId).getTitel();
                node.setGoobiProcessTitle(strProcessTitle);
                lstNodesWithNewIds.add(node.getLabel());
                Helper.setMeldung("Node '" + node.getLabel() + "' has been given Goobi process ID: " + strProcessTitle);
            } else {

                //perhaps remove the NodeId from the process?
            }
        }

        return lstNodesWithNewIds;
    }

    private IEadEntry getNodeWithId(String strNodeId, IEadEntry node) {

        if ("id".equalsIgnoreCase(identifierNodeName)) {
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
        if (field.getName().equals(identifierNodeName)) {
            for (IFieldValue fv : field.getValues()) {
                if (fv.getValue().equals(strNodeId)) {
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

        sql.append("SELECT processid FROM metadata WHERE name = '" + identifierMetadataName + "' and value in ");
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
            // update node history, if node was changed
            String oldFingerPrint = selectedEntry.getFingerprint();
            selectedEntry.calculateFingerprint();
            String newFingerPrint = selectedEntry.getFingerprint();

            if (StringUtils.isNotBlank(oldFingerPrint) && StringUtils.isNotBlank(newFingerPrint) && !oldFingerPrint.equals(newFingerPrint)) {
                updateChangeHistory(selectedEntry);
            }
            ArchiveManagementManager.saveNode(recordGroup.getId(), copy);
            resetFlatList();
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

    public void updateSingleNode() {
        if (selectedEntry != null) {
            if (selectedEntry.getNodeType() == null && configuredNodes != null) {
                selectedEntry.setNodeType(configuredNodes.get(0));
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

            for (IMetadataField emf : configuredFields) {
                if (emf.isGroup()) {
                    List<IValue> groups = metadata.get(emf.getName());
                    loadGroupMetadata(entry, emf, groups);
                } else {
                    List<IValue> values = metadata.get(emf.getName());
                    IMetadataField toAdd = addFieldToEntry(entry, emf, values);
                    addFieldToNode(entry, toAdd);
                }
            }
            entry.calculateFingerprint();
        }
    }

    private void loadGroupMetadata(IEadEntry entry, IMetadataField template, List<IValue> groups) {
        IMetadataField instance = new EadMetadataField(template.getName(), template.getLevel(), template.getXpath(), template.getXpathType(),
                template.isRepeatable(),
                template.isVisible(), template.isShowField(), template.getFieldType(), template.getMetadataName(), template.isImportMetadataInChild(),
                template.getValidationType(),
                template.getRegularExpression(), template.isSearchable(), template.getViafSearchFields(), template.getViafDisplayFields(),
                template.isGroup(), template.getVocabularyName());
        instance.setValidationError(template.getValidationError());
        instance.setSelectItemList(template.getSelectItemList());
        instance.setSearchParameter(template.getSearchParameter());
        instance.setEadEntry(entry);

        // sub fields
        for (IMetadataField sub : template.getSubfields()) {
            IMetadataField toAdd = addFieldToEntry(entry, sub, null);
            instance.getSubfields().add(toAdd);
        }

        addGroupData(instance, groups);
        addFieldToNode(entry, instance);
    }

    private void addGroupData(IMetadataField instance, List<IValue> groups) {
        if (groups != null) {
            for (IValue groupData : groups) {
                IMetadataGroup eadGroup = instance.createGroup();
                GroupValue gv = (GroupValue) groupData;
                Map<String, List<IValue>> groupMetadata = gv.getSubfields();

                for (IMetadataField sub : eadGroup.getFields()) {
                    List<IValue> values = groupMetadata.get(sub.getName());

                    if (values != null && !values.isEmpty()) {
                        instance.setShowField(true);

                        // split single value into multiple fields
                        for (IValue value : values) {
                            ExtendendValue val = (ExtendendValue) value;
                            String stringValue = val.getValue();
                            IFieldValue fv = null;
                            for (IFieldValue v : sub.getValues()) {
                                if (StringUtils.isBlank(v.getValue())) {
                                    fv = v;
                                }
                            }
                            if (fv == null) {
                                fv = new FieldValue(sub);
                                sub.addFieldValue(fv);
                            }
                            fv.setAuthorityType(val.getAuthorityType());
                            fv.setAuthorityValue(val.getAuthorityValue());

                            if ("multiselect".equals(sub.getFieldType()) && StringUtils.isNotBlank(stringValue)) {
                                String[] splittedValues = stringValue.split("; ");
                                for (String s : splittedValues) {
                                    fv.setMultiselectValue(s);
                                }
                            } else {
                                fv.setValue(stringValue);
                            }

                        }
                    } else {
                        IFieldValue fv = new FieldValue(sub);
                        sub.addFieldValue(fv);
                    }
                }
            }
        } else {
            instance.createGroup();
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
            for (IMetadataField emf : configuredFields) {
                List<IValue> values = metadata.get(emf.getName());
                if (emf.isGroup()) {
                    loadGroupMetadata(entry, emf, values);

                } else {
                    IMetadataField toAdd = addFieldToEntry(entry, emf, values);
                    addFieldToNode(entry, toAdd);
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
        addGroupData(selectedGroup, null);
    }

    public void showAllFields() {
        if (selectedEntry != null) {
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
            for (INodeType node : configuredNodes) {
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
                for (IMetadataField emf : configuredFields) {
                    if (emf.isGroup()) {
                        loadGroupMetadata(entry, emf, null);
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
                            IValue val = new ExtendendValue(emf.getName(), value, null, null);
                            metadataValues.add(val);
                        }

                        IMetadataField toAdd = addFieldToEntry(entry, emf, metadataValues);
                        addFieldToNode(entry, toAdd);
                    }
                }
                entry.calculateFingerprint();
                // save new node
                ArchiveManagementManager.saveNode(recordGroup.getId(), entry);
            }

            selectedEntry.setDisplayChildren(true);
            resetFlatList();
        }

    }

    public boolean isReadOnlyModus() {
        return readOnlyMode;
    }

    public void eadExport() {
        if (StringUtils.isBlank(exportFolder)) {
            Helper.setFehlerMeldung("plugin_administration_archive_eadExportNotConfigured");
            return;
        }

        Path downloadFile = Paths.get(exportFolder, databaseName.replace(" ", "_"));
        Document document = createEadFile();
        XMLOutputter outputter = new XMLOutputter(Format.getPrettyFormat());
        try {
            outputter.output(document, Files.newOutputStream(downloadFile));
        } catch (IOException e) {
            log.error(e);
        }
    }

    public void initializeRecord() {

        VocabularyRecordsBean recordsBean = Helper.getBeanByClass(VocabularyRecordsBean.class);

        JSFVocabulary vocabulary = null;
        try {
            List<JSFVocabulary> records = vocabularyAPI.vocabularies().all();
            for (JSFVocabulary rec : records) {
                if (rec.getName().equals(vocabularyField.getVocabularyName())) {
                    vocabulary = rec;
                    break;
                }
            }
        } catch (Exception e) {
            displayVocabularyModal = false;

        }
        // configured vocabulary does not exist or vocabulary server is down
        if (vocabulary == null) {
            displayVocabularyModal = false;
            return;

        }
        recordsBean.load(vocabulary);
        recordsBean.createEmpty(null);

    }

    public void addEntry() {
        // save new record
        VocabularyRecordsBean recordsBean = Helper.getBeanByClass(VocabularyRecordsBean.class);
        recordsBean.saveRecord(recordsBean.getCurrentRecord());

        // populate new item list
        loadVocabulary(vocabularyField);

        // update template field
        for (IMetadataField template : configuredFields) {
            if (template.getName().equals(vocabularyField.getName())) {
                template.setSelectItemList(vocabularyField.getSelectItemList());
            }

        }
    }
}
