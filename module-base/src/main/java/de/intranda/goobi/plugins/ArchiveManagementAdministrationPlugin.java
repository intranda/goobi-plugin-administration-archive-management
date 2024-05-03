package de.intranda.goobi.plugins;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
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

import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.event.AjaxBehaviorEvent;
import javax.servlet.ServletContext;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Part;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.commons.configuration.XMLConfiguration;
import org.apache.commons.configuration.reloading.FileChangedReloadingStrategy;
import org.apache.commons.configuration.tree.xpath.XPathExpressionEngine;
import org.apache.commons.lang3.StringUtils;
import org.goobi.beans.Process;
import org.goobi.beans.Step;
import org.goobi.beans.User;
import org.goobi.interfaces.IArchiveManagementAdministrationPlugin;
import org.goobi.interfaces.IConfiguration;
import org.goobi.interfaces.IEadEntry;
import org.goobi.interfaces.IFieldValue;
import org.goobi.interfaces.IMetadataField;
import org.goobi.interfaces.INodeType;
import org.goobi.production.cli.helper.StringPair;
import org.goobi.production.enums.PluginType;
import org.goobi.vocabulary.Field;
import org.goobi.vocabulary.VocabRecord;
import org.goobi.vocabulary.Vocabulary;
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
import de.sub.goobi.helper.exceptions.SwapException;
import de.sub.goobi.persistence.managers.MetadataManager;
import de.sub.goobi.persistence.managers.ProcessManager;
import de.sub.goobi.persistence.managers.VocabularyManager;
import io.goobi.workflow.locking.LockingBean;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.log4j.Log4j2;
import net.xeoh.plugins.base.annotations.PluginImplementation;
import ugh.dl.DigitalDocument;
import ugh.dl.DocStruct;
import ugh.dl.Fileformat;
import ugh.dl.Metadata;
import ugh.dl.MetadataType;
import ugh.dl.Prefs;
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

    public static final Namespace ns = Namespace.getNamespace("ead", "urn:isbn:1-931666-22-9");
    private static XPathFactory xFactory = XPathFactory.instance();

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

    private List<StringPair> eventList = new ArrayList<>();
    private List<String> editorList = new ArrayList<>();
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
     * Get the database names and file names from the basex databases
     * 
     * @return
     */

    @Override
    public List<String> getPossibleDatabases() {

        List<RecordGroup> allRecordGroups = ArchiveManagementManager.getAllRecordGroups();

        List<String> databases = new ArrayList<>();
        for (RecordGroup rec : allRecordGroups) {
            databases.add(rec.getTitle());
        }
        return databases;
    }

    /**
     * Get the database names without file names from the basex databases
     * 
     * @return
     */

    public List<String> getPossibleDatabaseNames() {
        List<String> databases = new ArrayList<>();
        List<RecordGroup> allRecordGroups = ArchiveManagementManager.getAllRecordGroups();

        for (RecordGroup rec : allRecordGroups) {
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

            // remove whitespaces from filename
            databaseName = databaseName.replace(" ", "_");
            readConfiguration();

            recordGroup = new RecordGroup();
            recordGroup.setTitle(databaseName);
            ArchiveManagementManager.saveRecordGroup(recordGroup);

            rootElement = new EadEntry(0, 0);
            rootElement.setId(String.valueOf(UUID.randomUUID()));

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
    private void parseEadFile(Document document) {
        eventList = new ArrayList<>();
        editorList = new ArrayList<>();
        Element eadElement = null;
        Element collection = document.getRootElement();
        if ("collection".equals(collection.getName())) {
            eadElement = collection.getChild("ead", ns);
        } else {
            eadElement = collection;
        }

        rootElement = parseElement(0, 0, eadElement, false);
        INodeType rootType = new NodeType("root", null, "fa fa-home", 0);
        rootElement.setNodeType(rootType);
        rootElement.setDisplayChildren(true);

        Element archdesc = eadElement.getChild("archdesc", ns);
        if (archdesc != null) {
            Element processinfoElement = archdesc.getChild("processinfo", ns);
            if (processinfoElement != null) {
                Element list = processinfoElement.getChild("list", ns);
                List<Element> entries = list.getChildren("item", ns);
                IMetadataField editor =
                        new EadMetadataField("editorName", 7, null, null, false, true, true, "readonly", null, false, null, null, false);

                for (Element item : entries) {
                    editorList.add(item.getText());
                    IFieldValue fv = new FieldValue(editor);
                    fv.setValue(item.getText());
                    editor.addFieldValue(fv);
                }
                rootElement.getDescriptionControlAreaList().add(editor);
            }
        }
        Element control = eadElement.getChild("control", ns);
        if (control != null) {
            Element maintenancehistory = control.getChild("maintenancehistory", ns);
            if (maintenancehistory != null) {
                List<Element> events = maintenancehistory.getChildren("maintenanceevent", ns);
                for (Element event : events) {
                    String eventtype = event.getChildText("eventtype", ns);
                    String eventdatetime = event.getChildText("eventdatetime", ns);
                    eventList.add(new StringPair(eventtype, eventdatetime));
                }
            }
        }
        getDuplicationConfiguration();
    }

    /**
     * read the metadata for the current xml node. - create an {@link EadEntry} - execute the configured xpaths on the current node - add the metadata
     * to one of the 7 levels - check if the node has sub nodes - call the method recursively for all sub nodes
     */
    private IEadEntry parseElement(int order, int hierarchy, Element element, boolean fastMode) {
        IEadEntry entry = new EadEntry(order, hierarchy);

        for (IMetadataField emf : configuredFields) {

            List<String> stringValues = new ArrayList<>();
            if ("text".equalsIgnoreCase(emf.getXpathType())) {
                XPathExpression<Text> engine = xFactory.compile(emf.getXpath(), Filters.text(), null, ns);
                List<Text> values = engine.evaluate(element);
                if (emf.isRepeatable()) {
                    for (Text value : values) {
                        String stringValue = value.getValue();
                        stringValues.add(stringValue);
                    }
                } else if (!values.isEmpty()) {
                    Text value = values.get(0);
                    String stringValue = value.getValue();
                    stringValues.add(stringValue);
                }
            } else if ("attribute".equalsIgnoreCase(emf.getXpathType())) {
                XPathExpression<Attribute> engine = xFactory.compile(emf.getXpath(), Filters.attribute(), null, ns);
                List<Attribute> values = engine.evaluate(element);

                if (emf.isRepeatable()) {
                    for (Attribute value : values) {
                        String stringValue = value.getValue();
                        stringValues.add(stringValue);
                    }
                } else if (!values.isEmpty()) {
                    Attribute value = values.get(0);
                    String stringValue = value.getValue();
                    stringValues.add(stringValue);
                }
            } else {
                XPathExpression<Element> engine = xFactory.compile(emf.getXpath(), Filters.element(), null, ns);
                List<Element> values = engine.evaluate(element);
                if (emf.isRepeatable()) {
                    for (Element value : values) {
                        String stringValue = value.getValue();
                        stringValues.add(stringValue);
                    }
                } else if (!values.isEmpty()) {
                    Element value = values.get(0);
                    String stringValue = value.getValue();
                    stringValues.add(stringValue);
                }
            }
            addFieldToEntry(entry, emf, stringValues);
        }

        Element eadheader = element.getChild("eadheader", ns);

        entry.setId(element.getAttributeValue("id"));
        if (eadheader != null) {
            try {
                Element filedesc = eadheader.getChild("filedesc", ns);
                if (filedesc != null) {
                    Element titlestmt = filedesc.getChild("titlestmt", ns);
                    if (titlestmt != null) {
                        String titleproper = titlestmt.getChildText("titleproper", ns);
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
        Element archdesc = element.getChild("archdesc", ns);
        if (archdesc != null) {
            String nodeTypeName = archdesc.getAttributeValue("localtype");
            for (INodeType nt : configuredNodes) {
                if (nt.getNodeName().equals(nodeTypeName)) {
                    entry.setNodeType(nt);
                }
            }
            Element dsc = archdesc.getChild("dsc", ns);
            if (dsc != null) {
                // read process title
                List<Element> altformavailList = dsc.getChildren("altformavail", ns);
                for (Element altformavail : altformavailList) {
                    if ("goobi_process".equals(altformavail.getAttributeValue("localtype"))) {
                        entry.setGoobiProcessTitle(altformavail.getText());
                    }
                }
                clist = dsc.getChildren("c", ns);
            }

        } else {
            String nodeTypeName = element.getAttributeValue("otherlevel");
            for (INodeType nt : configuredNodes) {
                if (nt.getNodeName().equals(nodeTypeName)) {
                    entry.setNodeType(nt);
                }
            }
            List<Element> altformavailList = element.getChildren("altformavail", ns);
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
            clist = element.getChildren("c", ns);
        }
        if (clist != null) {
            int subOrder = 0;
            int subHierarchy = hierarchy + 1;
            for (Element c : clist) {

                IEadEntry child = parseElement(subOrder, subHierarchy, c, fastMode);
                entry.addSubEntry(child);
                child.setParentNode(entry);
                subOrder++;
            }
        }

        // generate new id, if id is null
        if (entry.getId() == null) {
            entry.setId(String.valueOf(UUID.randomUUID()));
        }

        return entry;
    }

    /**
     * Add the metaata to the configured level
     * 
     * @param entry
     * @param emf
     * @param stringValue
     */

    private void addFieldToEntry(IEadEntry entry, IMetadataField emf, List<String> stringValues) {
        if (StringUtils.isBlank(entry.getLabel()) && emf.getXpath().contains("unittitle") && stringValues != null && !stringValues.isEmpty()) {
            entry.setLabel(stringValues.get(0));
        }
        IMetadataField toAdd = new EadMetadataField(emf.getName(), emf.getLevel(), emf.getXpath(), emf.getXpathType(), emf.isRepeatable(),
                emf.isVisible(), emf.isShowField(), emf.getFieldType(), emf.getMetadataName(), emf.isImportMetadataInChild(), emf.getValidationType(),
                emf.getRegularExpression(), emf.isSearchable());
        toAdd.setValidationError(emf.getValidationError());
        toAdd.setSelectItemList(emf.getSelectItemList());
        toAdd.setEadEntry(entry);

        if (stringValues != null && !stringValues.isEmpty()) {
            toAdd.setShowField(true);

            // split single value into multiple fields
            for (String stringValue : stringValues) {
                IFieldValue fv = new FieldValue(toAdd);

                if ("multiselect".equals(toAdd.getFieldType()) && StringUtils.isNotBlank(stringValue)) {
                    String[] splittedValues = stringValue.split("; ");
                    for (String val : splittedValues) {
                        fv.setMultiselectValue(val);
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
    private void readConfiguration() {
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
        for (HierarchicalConfiguration hc : config.configurationsAt("/metadata")) {
            IMetadataField field = new EadMetadataField(hc.getString("@name"), hc.getInt("@level"), hc.getString("@xpath"),
                    hc.getString("@xpathType", "element"), hc.getBoolean("@repeatable", false), hc.getBoolean("@visible", true),
                    hc.getBoolean("@showField", false), hc.getString("@fieldType", "input"), hc.getString("@rulesetName", null),
                    hc.getBoolean("@importMetadataInChild", false), hc.getString("@validationType", null), hc.getString("@regularExpression"),
                    hc.getBoolean("@searchable", false));
            configuredFields.add(field);
            if (field.isSearchable()) {
                advancedSearchFields.add(field.getName());
            }

            field.setValidationError(hc.getString("/validationError"));
            if ("dropdown".equals(field.getFieldType()) || "multiselect".equals(field.getFieldType())) {
                List<String> valueList = Arrays.asList(hc.getStringArray("/value"));
                field.setSelectItemList(valueList);
            } else if ("vocabulary".equals(field.getFieldType())) {
                String vocabularyName = hc.getString("/vocabulary");
                List<String> searchParameter = Arrays.asList(hc.getStringArray("/searchParameter"));
                List<String> iFieldValueList = new ArrayList<>();
                if (searchParameter == null) {
                    Vocabulary currentVocabulary = VocabularyManager.getVocabularyByTitle(vocabularyName);
                    if (currentVocabulary != null) {
                        VocabularyManager.getAllRecords(currentVocabulary);
                        if (currentVocabulary != null && currentVocabulary.getId() != null) {
                            for (VocabRecord vr : currentVocabulary.getRecords()) {
                                for (Field f : vr.getFields()) {
                                    if (f.getDefinition().isMainEntry()) {
                                        iFieldValueList.add(f.getValue());
                                        break;
                                    }
                                }
                            }
                        }
                    }
                } else {
                    List<StringPair> vocabularySearchFields = new ArrayList<>();
                    for (String fieldname : searchParameter) {
                        String[] parts = fieldname.trim().split("=");
                        if (parts.length > 1) {
                            String fieldName = parts[0];
                            String value = parts[1];
                            StringPair sp = new StringPair(fieldName, value);
                            vocabularySearchFields.add(sp);
                        }
                    }
                    List<VocabRecord> records = VocabularyManager.findRecords(vocabularyName, vocabularySearchFields);
                    if (records != null && !records.isEmpty()) {
                        for (VocabRecord vr : records) {
                            for (Field f : vr.getFields()) {
                                if (f.getDefinition().isMainEntry()) {
                                    iFieldValueList.add(f.getValue());
                                    break;
                                }
                            }
                        }
                    }
                }
                field.setSelectItemList(iFieldValueList);
            }

        }
        ArchiveManagementManager.setConfiguredNodes(configuredNodes);
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
        resetFlatList();
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
            entry.setId(String.valueOf(UUID.randomUUID()));
            // initial metadata values
            List<String> titleData = new ArrayList<>();
            if (StringUtils.isNotBlank(nodeDefaultTitle)) {
                titleData.add(nodeDefaultTitle);
                entry.setLabel(nodeDefaultTitle);
            }
            for (IMetadataField emf : configuredFields) {
                if (emf.getXpath().contains("unittitle")) {
                    addFieldToEntry(entry, emf, titleData);
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

            // select the parent node
            setSelectedEntry(parentNode);

        }

    }

    /**
     * upload the selected file
     */
    public void upload() {
        if (uploadFile == null || StringUtils.isBlank(databaseName)) {
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

        // save nodes
        recordGroup = new RecordGroup();
        recordGroup.setTitle(uploadedFileName);
        ArchiveManagementManager.saveRecordGroup(recordGroup);
        List<IEadEntry> nodes = rootElement.getAllNodes();
        ArchiveManagementManager.saveNodes(recordGroup.getId(), nodes);
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

        RecordGroup rg = ArchiveManagementManager.getRecordGroupByTitle(uploadedFileName);

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
        String uploadedFileName = Paths.get(file.getSubmittedFileName()).getFileName().toString(); // MSIE fix.

        // filename must end with xml
        if (!uploadedFileName.endsWith(".xml")) {
            uploadedFileName = uploadedFileName + ".xml";
        }
        // remove whitespaces from filename
        uploadedFileName = uploadedFileName.replace(" ", "_");

        return uploadedFileName;
    }

    private void addMetadata(Element xmlElement, IEadEntry node) {
        boolean isMainElement = false;
        if ("ead".equals(xmlElement.getName())) {
            isMainElement = true;
        }

        for (IMetadataField emf : node.getIdentityStatementAreaList()) {
            for (IFieldValue fv : emf.getValues()) {
                if (StringUtils.isNotBlank(fv.getValuesForXmlExport())) {
                    createEadXmlField(xmlElement, isMainElement, emf, fv.getValuesForXmlExport());

                }
            }
        }
        for (IMetadataField emf : node.getContextAreaList()) {
            for (IFieldValue fv : emf.getValues()) {
                if (StringUtils.isNotBlank(fv.getValuesForXmlExport())) {
                    createEadXmlField(xmlElement, isMainElement, emf, fv.getValuesForXmlExport());

                }
            }
        }
        for (IMetadataField emf : node.getContentAndStructureAreaAreaList()) {
            for (IFieldValue fv : emf.getValues()) {
                if (StringUtils.isNotBlank(fv.getValuesForXmlExport())) {
                    createEadXmlField(xmlElement, isMainElement, emf, fv.getValuesForXmlExport());

                }
            }
        }
        for (IMetadataField emf : node.getAccessAndUseAreaList()) {
            for (IFieldValue fv : emf.getValues()) {
                if (StringUtils.isNotBlank(fv.getValuesForXmlExport())) {
                    createEadXmlField(xmlElement, isMainElement, emf, fv.getValuesForXmlExport());

                }
            }
        }
        for (IMetadataField emf : node.getAlliedMaterialsAreaList()) {
            for (IFieldValue fv : emf.getValues()) {
                if (StringUtils.isNotBlank(fv.getValuesForXmlExport())) {
                    createEadXmlField(xmlElement, isMainElement, emf, fv.getValuesForXmlExport());

                }
            }
        }
        for (IMetadataField emf : node.getNotesAreaList()) {
            for (IFieldValue fv : emf.getValues()) {
                if (StringUtils.isNotBlank(fv.getValuesForXmlExport())) {
                    createEadXmlField(xmlElement, isMainElement, emf, fv.getValuesForXmlExport());

                }
            }
        }
        for (IMetadataField emf : node.getDescriptionControlAreaList()) {
            for (IFieldValue fv : emf.getValues()) {
                if (StringUtils.isNotBlank(fv.getValuesForXmlExport())) {
                    createEadXmlField(xmlElement, isMainElement, emf, fv.getValuesForXmlExport());

                }
            }
        }
        Element dsc = null;
        if (isMainElement) {
            if (StringUtils.isNotBlank(node.getId())) {
                xmlElement.setAttribute("id", node.getId());
            }
            Element archdesc = xmlElement.getChild("archdesc", ns);
            if (archdesc == null) {
                archdesc = new Element("archdesc", ns);
                xmlElement.addContent(archdesc);
            }
            dsc = archdesc.getChild("dsc", ns);
            if (dsc == null) {
                dsc = new Element("dsc", ns);
                archdesc.addContent(dsc);
            }

            if (StringUtils.isNotBlank(node.getGoobiProcessTitle())) {
                Element altformavail = new Element("altformavail", ns);
                altformavail.setAttribute("localtype", "goobi_process");
                altformavail.setText(node.getGoobiProcessTitle());
                dsc.addContent(altformavail);
            }

        } else {
            dsc = xmlElement;
            if (StringUtils.isNotBlank(node.getId())) {
                xmlElement.setAttribute("id", node.getId());
            }
            if (node.getNodeType() != null) {
                xmlElement.setAttribute("otherlevel", node.getNodeType().getNodeName());
            }
        }

        for (IEadEntry subNode : node.getSubEntryList()) {
            if (dsc == null) {
                dsc = new Element("dsc", ns);
                xmlElement.addContent(dsc);
            }

            Element c = new Element("c", ns);
            dsc.addContent(c);
            if (StringUtils.isNotBlank(subNode.getGoobiProcessTitle())) {
                Element altformavail = new Element("altformavail", ns);
                altformavail.setAttribute("localtype", "goobi_process");
                altformavail.setText(subNode.getGoobiProcessTitle());
                c.addContent(altformavail);
            }

            addMetadata(c, subNode);
        }

    }

    private void createEadXmlField(Element xmlElement, boolean isMainElement, IMetadataField emf, String metadataValue) {
        Element currentElement = xmlElement;
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
                    Element duplicate = new Element(currentElement.getName(), ns);
                    for (Attribute attr : currentElement.getAttributes()) {
                        if (!attr.getName().equals(field)) {
                            duplicate.setAttribute(attr.getName(), attr.getValue());
                        }
                    }
                    currentElement.getParent().addContent(duplicate);
                    currentElement = duplicate;
                }

                currentElement.setAttribute(field, metadataValue);
                written = true;
            } else {
                // remove namespace
                field = field.replace("ead:", "");

                String conditions = null;
                if (field.contains("[")) {
                    conditions = field.substring(field.indexOf("["));
                    field = field.substring(0, field.indexOf("["));
                }

                // check if element exists, re-use if possible
                Element element = currentElement.getChild(field, ns);
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

            }
        }
        if (!written) {
            // duplicate current element if not empty
            if (StringUtils.isNotBlank(currentElement.getText()) || !currentElement.getChildren().isEmpty()) {
                Element duplicate = new Element(currentElement.getName(), ns);
                for (Attribute attr : currentElement.getAttributes()) {
                    duplicate.setAttribute(attr.getName(), attr.getValue());
                }

                if (!currentElement.getChildren().isEmpty()) {
                    for (Element child : currentElement.getChildren()) {
                        Element duplicateChild = new Element(child.getName(), ns);
                        duplicateChild.setText(child.getText());
                        duplicate.addContent(duplicateChild);
                    }
                }

                currentElement.getParent().addContent(duplicate);
                currentElement = duplicate;
            }

            currentElement.setText(metadataValue);
        }
    }

    private Element createXmlElement(Element currentElement, String field, String conditions) {
        Element element;
        element = new Element(field, ns);
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
                        Element eltChild = new Element(attr[0], ns);
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
        selectedEntry.getParentNode().reOrderElements();
        ArchiveManagementManager.saveNode(recordGroup.getId(), selectedEntry);
        setSelectedEntry(selectedEntry);
        displayMode = "";
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
        newParent.reOrderElements();
        // move node to one position after current parent
        if (selectedEntry.getOrderNumber().intValue() != oldParent.getOrderNumber().intValue() + 1) {
            Collections.swap(selectedEntry.getParentNode().getSubEntryList(), selectedEntry.getOrderNumber(), oldParent.getOrderNumber() + 1);
            selectedEntry.getParentNode().reOrderElements();
        }

    }

    public void searchAdvanced() {
        // TODO replace this with db search
        // get a list of all existing nodes
        resetSearch();
        List<IEadEntry> allNodes = rootElement.getAllNodes();

        for (StringPair p : advancedSearch) {
            if (StringUtils.isNotBlank(p.getOne()) && StringUtils.isNotBlank(p.getTwo())) {
                // filter the node list, return only fields containing the data
                allNodes = filterNodeList(allNodes, p.getOne(), p.getTwo());
            }
        }

        // finally mark all found nodes
        for (IEadEntry node : allNodes) {
            node.markAsFound();
        }

        flatEntryList = rootElement.getSearchList();
    }

    private List<IEadEntry> filterNodeList(List<IEadEntry> nodes, String searchField, String searchValues) {
        List<IEadEntry> filteredEntries = new ArrayList<>();
        for (IEadEntry node : nodes) {
            if (containsSearchString(node, searchField, searchValues)) {
                filteredEntries.add(node);
            }
        }
        return filteredEntries;
    }

    public void search() {
        if (StringUtils.isNotBlank(searchValue)) {
            // hide all elements
            rootElement.resetFoundList();
            // search in all/some metadata fields of all elements?

            // for now: search only labels
            searchInNode(rootElement);

            // fill flatList with displayable fields
            flatEntryList = rootElement.getSearchList();
        } else {
            resetSearch();
        }

    }

    private void searchInNode(IEadEntry node) {

        if (containsSearchString(node, null, searchValue.toLowerCase())) {
            // mark element + all parents as displayable
            node.markAsFound();
        }
        if (node.getSubEntryList() != null) {
            for (IEadEntry child : node.getSubEntryList()) {
                searchInNode(child);
            }
        }

    }

    private boolean containsSearchString(IEadEntry node, String fieldname, String searchString) {
        // TODO replace this with db search
        // search in all/configured fields
        for (IMetadataField field : node.getIdentityStatementAreaList()) {
            if (field.isSearchable() && (StringUtils.isBlank(fieldname) || field.getName().equals(fieldname))) {
                for (IFieldValue value : field.getValues()) {
                    if (value.getValue() != null && value.getValue().toLowerCase().contains(searchString)) {
                        return true;
                    }
                }
            }
        }
        for (IMetadataField field : node.getContextAreaList()) {
            if (field.isSearchable() && (StringUtils.isBlank(fieldname) || field.getName().equals(fieldname))) {
                for (IFieldValue value : field.getValues()) {
                    if (value.getValue() != null && value.getValue().toLowerCase().contains(searchString)) {
                        return true;
                    }
                }
            }
        }

        for (IMetadataField field : node.getContentAndStructureAreaAreaList()) {
            if (field.isSearchable() && (StringUtils.isBlank(fieldname) || field.getName().equals(fieldname))) {
                for (IFieldValue value : field.getValues()) {
                    if (value.getValue() != null && value.getValue().toLowerCase().contains(searchString)) {
                        return true;
                    }
                }
            }
        }

        for (IMetadataField field : node.getAccessAndUseAreaList()) {
            if (field.isSearchable() && (StringUtils.isBlank(fieldname) || field.getName().equals(fieldname))) {
                for (IFieldValue value : field.getValues()) {
                    if (value.getValue() != null && value.getValue().toLowerCase().contains(searchString)) {
                        return true;
                    }
                }
            }
        }
        for (IMetadataField field : node.getAlliedMaterialsAreaList()) {
            if (field.isSearchable() && (StringUtils.isBlank(fieldname) || field.getName().equals(fieldname))) {
                for (IFieldValue value : field.getValues()) {
                    if (value.getValue() != null && value.getValue().toLowerCase().contains(searchString)) {
                        return true;
                    }
                }
            }
        }
        for (IMetadataField field : node.getNotesAreaList()) {
            if (field.isSearchable() && (StringUtils.isBlank(fieldname) || field.getName().equals(fieldname))) {
                for (IFieldValue value : field.getValues()) {
                    if (value.getValue() != null && value.getValue().toLowerCase().contains(searchString)) {
                        return true;
                    }
                }
            }
        }
        for (IMetadataField field : node.getDescriptionControlAreaList()) {
            if (field.isSearchable() && (StringUtils.isBlank(fieldname) || field.getName().equals(fieldname))) {
                for (IFieldValue value : field.getValues()) {
                    if (value.getValue() != null && value.getValue().toLowerCase().contains(searchString)) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    public void searchListener(AjaxBehaviorEvent event) { //NOSONAR method signature must include the parameter, even if it is not needed
        search();
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

    public void createProcess() {
        // abort if no node is selected
        if (selectedEntry == null) {
            return;
        }
        if (selectedEntry.getNodeType() == null) {
            return;
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
            return;
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
                return;
            }
        }
        log.debug("processTitle = " + processTitle);

        // create process based on configured process template
        Process process = new Process();
        process.setTitel(processTitle);
        selectedEntry.setGoobiProcessTitle(processTitle);
        process.setIstTemplate(false);
        process.setInAuswahllisteAnzeigen(false);
        process.setProjekt(processTemplate.getProjekt());
        process.setRegelsatz(processTemplate.getRegelsatz());
        process.setDocket(processTemplate.getDocket());

        bhelp.SchritteKopieren(processTemplate, process);
        bhelp.ScanvorlagenKopieren(processTemplate, process);
        bhelp.WerkstueckeKopieren(processTemplate, process);
        bhelp.EigenschaftenKopieren(processTemplate, process);

        bhelp.EigenschaftHinzufuegen(process, "Template", processTemplate.getTitel());
        bhelp.EigenschaftHinzufuegen(process, "TemplateID", selectedTemplate);

        // save process
        try {
            ProcessManager.saveProcess(process);
        } catch (DAOException e1) {
            log.error(e1);
        }
        try {
            DocStruct physical = digDoc.createDocStruct(prefs.getDocStrctTypeByName("BoundBook"));
            digDoc.setPhysicalDocStruct(physical);
            Metadata imageFiles = new Metadata(prefs.getMetadataTypeByName("pathimagefiles"));
            imageFiles.setValue(process.getImagesTifDirectory(false));
            physical.addMetadata(imageFiles);
            // save fileformat
            process.writeMetadataFile(fileformat);
        } catch (UGHException | IOException | SwapException e) {
            log.error(e);

        }

        // save current node
        ArchiveManagementManager.saveNode(recordGroup.getId(), selectedEntry);

        // start any open automatic tasks
        for (Step s : process.getSchritteList()) {
            if (StepStatus.OPEN.equals(s.getBearbeitungsstatusEnum()) && s.isTypAutomatisch()) {
                ScriptThreadWithoutHibernate myThread = new ScriptThreadWithoutHibernate(s);
                myThread.startOrPutToQueue();
            }
        }

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
        if (StringUtils.isNotBlank(emf.getMetadataName())) {
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
                        logical.addMetadata(md);
                    } catch (UGHException e) {
                        log.error(e);
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
        process.downloadDocket();

    }

    public void downloadArchive() {
        // validate
        validateArchive();

        // create ead document
        Document document = new Document();

        Element eadRoot = new Element("ead", ns);
        document.setRootElement(eadRoot);

        addMetadata(eadRoot, rootElement);
        createEventFields(eadRoot);
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

    private void createEventFields(Element eadElement) {

        Element archdesc = eadElement.getChild("archdesc", ns);
        if (archdesc == null) {
            archdesc = new Element("archdesc", ns);
            eadElement.addContent(archdesc);
        }
        Element processinfoElement = archdesc.getChild("processinfo", ns);
        if (processinfoElement == null) {
            processinfoElement = new Element("processinfo", ns);
            archdesc.addContent(processinfoElement);
        }
        Element list = processinfoElement.getChild("list", ns);
        if (list == null) {
            list = new Element("list", ns);
            processinfoElement.addContent(list);
        }
        if (!editorList.contains(username)) {
            editorList.add(username);
        }
        for (String editor : editorList) {
            Element item = new Element("item", ns);
            item.setText(editor);
            list.addContent(item);
        }
        String eventType;
        if (eventList.isEmpty()) {
            eventType = "Created";
        } else {
            eventType = "Modified";
        }
        String date = formatter.format(new Date());
        eventList.add(new StringPair(eventType, date));

        Element control = eadElement.getChild("control", ns);
        if (control == null) {
            control = new Element("control", ns);
            eadElement.addContent(control);
        }

        Element maintenancehistory = control.getChild("maintenancehistory", ns);

        if (maintenancehistory == null) {
            maintenancehistory = new Element("maintenancehistory", ns);
            control.addContent(maintenancehistory);
        }
        for (StringPair pair : eventList) {
            Element maintenanceevent = new Element("maintenanceevent", ns);
            maintenancehistory.addContent(maintenanceevent);
            Element eventtype = new Element("eventtype", ns);
            eventtype.setText(pair.getOne());
            maintenanceevent.addContent(eventtype);
            Element eventdatetime = new Element("eventdatetime", ns);
            eventdatetime.setText(pair.getTwo());
            maintenanceevent.addContent(eventdatetime);
        }

    }

    public String saveArchiveAndLeave() {
        // save current node
        if (selectedEntry != null) {
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

        createProcessesForChildren(selectedEntry);
    }

    private void createProcessesForChildren(IEadEntry currentEntry) {

        setSelectedEntry(currentEntry);

        if (!currentEntry.isHasChildren() && currentEntry.getGoobiProcessTitle() == null) {
            try {
                createProcess();
                Helper.setMeldung("Created " + currentEntry.getGoobiProcessTitle() + " for " + currentEntry.getLabel());
            } catch (Exception e) {
                Helper.setFehlerMeldung(e.getMessage());
                log.error(e);
            }
        } else if (currentEntry.isHasChildren()) {

            for (IEadEntry childEntry : currentEntry.getSubEntryList()) {
                createProcessesForChildren(childEntry);
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

            ArchiveManagementManager.saveNode(recordGroup.getId(), copy);
        }
    }

    public void duplicateEadFile() {
        // TODO
        // save current node
        // get all nodes from db
        // load metadata for all nodes
        // create new RecordGroup
        // duplicate all nodes using the deepCopy option
        // save new RecordGroup and nodes

        // replace node ids
        // OR create ead file with a new name, import file
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
            ArchiveManagementManager.saveNode(recordGroup.getId(), selectedEntry);
        }
    }

    public void saveInDatabase() {
        ArchiveManagementManager.createTables();

        ArchiveManagementManager.saveRecordGroup(recordGroup);

        IEadEntry rootElementFromDb = ArchiveManagementManager.loadRecordGroup(recordGroup.getId());

        rootElement = rootElementFromDb;
        rootElement.setDisplayChildren(true);
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

            Map<String, List<String>> metadata = ArchiveManagementManager.loadMetadataForNode(entry.getDatabaseId());

            for (IMetadataField emf : configuredFields) {
                List<String> values = metadata.get(emf.getName());
                addFieldToEntry(entry, emf, values);

            }
        }
    }

}
