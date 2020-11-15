package de.intranda.goobi.plugins;

import java.io.IOException;
import java.io.StringReader;
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

import javax.faces.context.FacesContext;
import javax.servlet.ServletContext;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.commons.configuration.XMLConfiguration;
import org.apache.commons.configuration.reloading.FileChangedReloadingStrategy;
import org.apache.commons.configuration.tree.xpath.XPathExpressionEngine;
import org.apache.commons.lang.StringUtils;
import org.goobi.beans.Process;
import org.goobi.beans.User;
import org.goobi.production.cli.helper.StringPair;
import org.goobi.production.enums.PluginType;
import org.goobi.production.plugin.interfaces.IAdministrationPlugin;
import org.goobi.vocabulary.Field;
import org.goobi.vocabulary.VocabRecord;
import org.goobi.vocabulary.Vocabulary;
import org.jdom2.Attribute;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.Namespace;
import org.jdom2.Text;
import org.jdom2.filter.Filters;
import org.jdom2.input.SAXBuilder;
import org.jdom2.input.sax.XMLReaders;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;
import org.jdom2.xpath.XPathExpression;
import org.jdom2.xpath.XPathFactory;

import de.intranda.goobi.plugins.model.EadEntry;
import de.intranda.goobi.plugins.model.EadMetadataField;
import de.intranda.goobi.plugins.model.FieldValue;
import de.schlichtherle.io.FileOutputStream;
import de.sub.goobi.config.ConfigurationHelper;
import de.sub.goobi.helper.BeanHelper;
import de.sub.goobi.helper.FacesContextHelper;
import de.sub.goobi.helper.Helper;
import de.sub.goobi.helper.HttpClientHelper;
import de.sub.goobi.helper.exceptions.DAOException;
import de.sub.goobi.helper.exceptions.SwapException;
import de.sub.goobi.persistence.managers.ProcessManager;
import de.sub.goobi.persistence.managers.VocabularyManager;
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
public class ArchiveManagementAdministrationPlugin implements IAdministrationPlugin {

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
    private String datastoreUrl = "http://localhost:8984/";

    @Setter
    private String exportFolder = "/tmp/";

    @Getter
    @Setter
    private String selectedDatabase;

    @Getter
    @Setter
    private String databaseName;

    @Getter
    @Setter
    private String fileName;

    @Getter
    @Setter
    private EadEntry rootElement = null;

    private List<EadEntry> flatEntryList;

    @Getter
    private EadEntry selectedEntry;

    @Getter
    private List<EadEntry> moveList;

    @Getter
    @Setter
    private EadEntry destinationEntry;

    public static final Namespace ns = Namespace.getNamespace("ead", "urn:isbn:1-931666-22-9");
    private static XPathFactory xFactory = XPathFactory.instance();

    private XMLConfiguration xmlConfig;
    private List<EadMetadataField> configuredFields;

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

    private Integer processTemplateId;
    @Setter
    private Process processTemplate;
    private BeanHelper bhelp = new BeanHelper();

    private List<StringPair> eventList;
    private List<String> editorList;
    private SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    /**
     * Constructor
     */
    public ArchiveManagementAdministrationPlugin() {
        try {
            xmlConfig =
                    new XMLConfiguration(ConfigurationHelper.getInstance().getConfigurationFolder() + "plugin_intranda_administration_archive_management.xml");
            xmlConfig.setListDelimiter('&');
            xmlConfig.setReloadingStrategy(new FileChangedReloadingStrategy());
            xmlConfig.setExpressionEngine(new XPathExpressionEngine());

            datastoreUrl = xmlConfig.getString("/basexUrl", "http://localhost:8984/");
            exportFolder = xmlConfig.getString("/eadExportFolder", "/tmp");
        } catch (ConfigurationException e2) {
            log.error(e2);
        }
    }

    /**
     * Get the database names and file names from the basex databases
     * 
     * @return
     */

    public List<String> getPossibleDatabases() {
        List<String> databases = new ArrayList<>();
        String response = HttpClientHelper.getStringFromUrl(datastoreUrl + "databases");
        if (StringUtils.isNotBlank(response)) {

            Document document = openDocument(response);
            if (document != null) {
                Element root = document.getRootElement();
                List<Element> databaseList = root.getChildren("database");
                for (Element db : databaseList) {
                    String dbName = db.getChildText("name");

                    Element details = db.getChild("details");
                    for (Element resource : details.getChildren()) {
                        databases.add(dbName + " - " + resource.getText());
                    }

                }
            }
        }
        return databases;
    }

    /**
     * open the selected database and load the file
     */

    public void loadSelectedDatabase() {
        // open selected database
        if (StringUtils.isNotBlank(selectedDatabase)) {
            String[] parts = selectedDatabase.split(" - ");

            String response = HttpClientHelper.getStringFromUrl(datastoreUrl + "db/" + parts[0] + "/" + parts[1]);
            // get xml root element
            Document document = openDocument(response);
            if (document != null) {
                // get field definitions from config file
                readConfiguration();

                // parse ead file
                parseEadFile(document);
            }
        }
    }

    public List<String> getDistinctDatabaseNames() {
        List<String> answer = new ArrayList<>();
        List<String> completeList = getPossibleDatabases();
        for (String s : completeList) {
            String[] parts = s.split(" - ");
            String dbName = parts[0];
            if (!answer.contains(dbName)) {
                answer.add(dbName);
            }
        }

        return answer;
    }

    public void createNewDatabase() {
        if (StringUtils.isNotBlank(databaseName) && StringUtils.isNotBlank(fileName)) {
            selectedDatabase = databaseName + " - " + fileName;
            readConfiguration();

            Document document = new Document();
            Element eadElement = new Element("ead", ns);
            document.setRootElement(eadElement);
            rootElement = parseElement(1, 0, eadElement);
            rootElement.setDisplayChildren(true);
            displayMode = "";
        }
    }

    /*
     * get ead root element from document
     */
    private void parseEadFile(Document document) {
        eventList = new ArrayList<>();
        editorList = new ArrayList<>();

        Element collection = document.getRootElement();
        Element eadElement = collection.getChild("ead", ns);
        rootElement = parseElement(1, 0, eadElement);
        rootElement.setDisplayChildren(true);

        Element archdesc = eadElement.getChild("archdesc", ns);
        if (archdesc != null) {
            Element processinfoElement = archdesc.getChild("processinfo", ns);
            if (processinfoElement != null) {
                Element list = processinfoElement.getChild("list", ns);
                List<Element> entries = list.getChildren("item", ns);
                for (Element item : entries) {
                    editorList.add(item.getText());
                }
            }
        }
        Element control = eadElement.getChild("control", ns);
        if (control != null) {
            Element maintenancehistory = control.getChild("maintenancehistory", ns);
            if (maintenancehistory != null) {
                List<Element> events = maintenancehistory.getChildren("maintenancehistory", ns);
                for (Element event : events) {
                    String type = event.getChildText("eventtype", ns);
                    String date = event.getChildText("eventdatetime", ns);
                    eventList.add(new StringPair(type, date));
                }
            }
        }
    }

    /**
     * read the metadata for the current xml node. - create an {@link EadEntry} - execute the configured xpaths on the current node - add the metadata
     * to one of the 7 levels - check if the node has sub nodes - call the method recursively for all sub nodes
     */
    private EadEntry parseElement(int order, int hierarchy, Element element) {
        EadEntry entry = new EadEntry(order, hierarchy);

        for (EadMetadataField emf : configuredFields) {

            List<String> stringValues = new ArrayList<>();
            if ("text".equalsIgnoreCase(emf.getXpathType())) {
                XPathExpression<Text> engine = xFactory.compile(emf.getXpath(), Filters.text(), null, ns);
                List<Text> values = engine.evaluate(element);
                if (emf.isRepeatable()) {
                    for (Text value : values) {
                        String stringValue = value.getValue();
                        stringValues.add(stringValue);
                    }
                } else {
                    if (!values.isEmpty()) {
                        Text value = values.get(0);
                        String stringValue = value.getValue();
                        stringValues.add(stringValue);
                    }
                }
            } else if ("attribute".equalsIgnoreCase(emf.getXpathType())) {
                XPathExpression<Attribute> engine = xFactory.compile(emf.getXpath(), Filters.attribute(), null, ns);
                List<Attribute> values = engine.evaluate(element);

                if (emf.isRepeatable()) {
                    for (Attribute value : values) {
                        String stringValue = value.getValue();
                        stringValues.add(stringValue);
                    }
                } else {
                    if (!values.isEmpty()) {
                        Attribute value = values.get(0);
                        String stringValue = value.getValue();
                        stringValues.add(stringValue);
                    }
                }
            } else {
                XPathExpression<Element> engine = xFactory.compile(emf.getXpath(), Filters.element(), null, ns);
                List<Element> values = engine.evaluate(element);
                if (emf.isRepeatable()) {
                    for (Element value : values) {
                        String stringValue = value.getValue();
                        stringValues.add(stringValue);
                    }
                } else {
                    if (!values.isEmpty()) {
                        Element value = values.get(0);
                        String stringValue = value.getValue();
                        stringValues.add(stringValue);
                    }
                }
            }
            addFieldToEntry(entry, emf, stringValues);
        }

        Element eadheader = element.getChild("eadheader", ns);

        entry.setId(element.getAttributeValue("id"));
        if (eadheader != null) {
            entry.setLabel(eadheader.getChild("filedesc", ns).getChild("titlestmt", ns).getChildText("titleproper", ns));
        }

        // nodeType
        // get child elements
        List<Element> clist = null;
        Element archdesc = element.getChild("archdesc", ns);
        if (archdesc != null) {
            entry.setNodeType(archdesc.getAttributeValue("localtype"));
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
            entry.setNodeType(element.getAttributeValue("otherlevel"));
            List<Element> altformavailList = element.getChildren("altformavail", ns);
            for (Element altformavail : altformavailList) {
                if ("goobi_process".equals(altformavail.getAttributeValue("localtype"))) {
                    entry.setGoobiProcessTitle(altformavail.getText());
                }
            }
        }

        if (StringUtils.isBlank(entry.getNodeType())) {
            entry.setNodeType("folder");
        }
        if (clist == null) {
            clist = element.getChildren("c", ns);
        }
        if (clist != null) {
            int subOrder = 0;
            int subHierarchy = hierarchy + 1;
            for (Element c : clist) {

                EadEntry child = parseElement(subOrder, subHierarchy, c);
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

    private void addFieldToEntry(EadEntry entry, EadMetadataField emf, List<String> stringValues) {
        if (StringUtils.isBlank(entry.getLabel()) && emf.getXpath().contains("unittitle") && stringValues != null && !stringValues.isEmpty()) {
            entry.setLabel(stringValues.get(0));
        }
        EadMetadataField toAdd = new EadMetadataField(emf.getName(), emf.getLevel(), emf.getXpath(), emf.getXpathType(), emf.isRepeatable(),
                emf.isVisible(), emf.isShowField(), emf.getFieldType(), emf.getMetadataName(), emf.isImportMetadataInChild(), emf.getValidationType(),
                emf.getRegularExpression());
        toAdd.setValidationError(emf.getValidationError());
        toAdd.setSelectItemList(emf.getSelectItemList());
        toAdd.setEadEntry(entry);

        if (stringValues != null && !stringValues.isEmpty()) {
            toAdd.setShowField(true);

            // split single value into multiple fields
            for (String stringValue : stringValues) {
                FieldValue fv = new FieldValue(toAdd);

                if (toAdd.getFieldType().equals("multiselect") && StringUtils.isNotBlank(stringValue)) {
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
            FieldValue fv = new FieldValue(toAdd);
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
        }

    }

    /**
     * Parse the string response from the basex database into a xml document
     * 
     * @param response
     * @return
     */
    private Document openDocument(String response) {
        // read response
        SAXBuilder builder = new SAXBuilder(XMLReaders.NONVALIDATING);
        builder.setFeature("http://xml.org/sax/features/validation", false);
        builder.setFeature("http://apache.org/xml/features/nonvalidating/load-dtd-grammar", false);
        builder.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);

        try {
            Document document = builder.build(new StringReader(response), "utf-8");
            return document;

        } catch (JDOMException | IOException e) {
            log.error(e);
        }
        return null;
    }

    /**
     * read in all parameters from the configuration file
     * 
     */
    private void readConfiguration() {
        configuredFields = new ArrayList<>();
        HierarchicalConfiguration config = null;

        try {
            config = xmlConfig.configurationAt("//config[./archive = '" + selectedDatabase + "']");
        } catch (IllegalArgumentException e) {
            try {
                config = xmlConfig.configurationAt("//config[./archive = '*']");
            } catch (IllegalArgumentException e1) {

            }
        }

        processTemplateId = config.getInteger("/processTemplateId", null);

        for (HierarchicalConfiguration hc : config.configurationsAt("/metadata")) {
            EadMetadataField field = new EadMetadataField(hc.getString("@name"), hc.getInt("@level"), hc.getString("@xpath"),
                    hc.getString("@xpathType", "element"), hc.getBoolean("@repeatable", false), hc.getBoolean("@visible", true),
                    hc.getBoolean("@showField", false), hc.getString("@fieldType", "input"), hc.getString("@rulesetName", null),
                    hc.getBoolean("@importMetadataInChild", false), hc.getString("@validationType", null), hc.getString("@regularExpression"));
            configuredFields.add(field);
            field.setValidationError(hc.getString("/validationError"));
            if (field.getFieldType().equals("dropdown") || field.getFieldType().equals("multiselect")) {
                List<String> valueList = Arrays.asList(hc.getStringArray("/value"));
                field.setSelectItemList(valueList);
            } else if (field.getFieldType().equals("vocabulary")) {
                String vocabularyName = hc.getString("/vocabulary");
                List<String> searchParameter = Arrays.asList(hc.getStringArray("/searchParameter"));
                List<String> fieldValueList = new ArrayList<>();
                if (searchParameter == null) {
                    Vocabulary currentVocabulary = VocabularyManager.getVocabularyByTitle(vocabularyName);
                    if (currentVocabulary != null) {
                        VocabularyManager.loadRecordsForVocabulary(currentVocabulary);
                        if (currentVocabulary != null && currentVocabulary.getId() != null) {
                            for (VocabRecord vr : currentVocabulary.getRecords()) {
                                for (Field f : vr.getFields()) {
                                    if (f.getDefinition().isMainEntry()) {
                                        fieldValueList.add(f.getValue());
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
                    if (records != null && records.size() > 0) {
                        for (VocabRecord vr : records) {
                            for (Field f : vr.getFields()) {
                                if (f.getDefinition().isMainEntry()) {
                                    fieldValueList.add(f.getValue());
                                    break;
                                }
                            }
                        }
                    }
                }
                field.setSelectItemList(fieldValueList);
            }

        }
    }

    public void resetFlatList() {
        flatEntryList = null;
    }

    /**
     * Get the hierarchical tree as a flat list
     * 
     * @return
     */

    public List<EadEntry> getFlatEntryList() {
        if (flatEntryList == null) {
            if (rootElement != null) {
                flatEntryList = new LinkedList<>();
                flatEntryList.addAll(rootElement.getAsFlatList());
            }
        }
        return flatEntryList;
    }

    public void setSelectedEntry(EadEntry entry) {
        for (EadEntry other : flatEntryList) {
            other.setSelected(false);
        }
        entry.setSelected(true);
        this.selectedEntry = entry;
    }

    public void addNode() {
        if (selectedEntry != null) {
            EadEntry entry =
                    new EadEntry(selectedEntry.isHasChildren() ? selectedEntry.getSubEntryList().size() + 1 : 0, selectedEntry.getHierarchy() + 1);
            entry.setId(String.valueOf(UUID.randomUUID()));
            // initial metadata values
            for (EadMetadataField emf : configuredFields) {
                addFieldToEntry(entry, emf, null);
            }
            selectedEntry.addSubEntry(entry);
            selectedEntry.setDisplayChildren(true);
            selectedEntry = entry;
            flatEntryList = null;
        }
    }

    public void deleteNode() {
        if (selectedEntry != null) {
            // find parent node
            EadEntry parentNode = selectedEntry.getParentNode();
            if (parentNode == null) {
                // we found the root node, this node cant be deleted
                return;
            }
            // remove current element from parent node
            parentNode.removeSubEntry(selectedEntry);
            // set selectedEntry to parent node
            selectedEntry = parentNode;
            flatEntryList = null;
        }
    }

    /**
     * Create a new ead xml document and store it in the configured folder The document is stored in the configured folder and the basex import
     * routine is called
     * 
     */

    public void createEadDocument() {

        Document document = new Document();

        Element eadRoot = new Element("ead", ns);
        document.setRootElement(eadRoot);

        addMetadata(eadRoot, rootElement);
        createEventFields(eadRoot);
        String[] nameParts = selectedDatabase.split(" - ");
        XMLOutputter out = new XMLOutputter(Format.getPrettyFormat());
        try {
            out.output(document, new FileOutputStream(exportFolder + "/" + nameParts[1]));
        } catch (IOException e) {
            log.error(e);
        }

        // call function to import created ead file
        String importUrl = datastoreUrl + "import/" + nameParts[0] + "/" + nameParts[1];

        HttpClientHelper.getStringFromUrl(importUrl);

        // delete created file ?

    }

    private void addMetadata(Element xmlElement, EadEntry node) {
        boolean isMainElement = false;
        if (xmlElement.getName().equals("ead")) {
            isMainElement = true;
        }

        for (EadMetadataField emf : node.getIdentityStatementAreaList()) {
            for (FieldValue fv : emf.getValues()) {
                if (StringUtils.isNotBlank(fv.getValuesForXmlExport())) {
                    createEadXmlField(xmlElement, isMainElement, emf, fv.getValuesForXmlExport());

                }
            }
        }
        for (EadMetadataField emf : node.getContextAreaList()) {
            for (FieldValue fv : emf.getValues()) {
                if (StringUtils.isNotBlank(fv.getValuesForXmlExport())) {
                    createEadXmlField(xmlElement, isMainElement, emf, fv.getValuesForXmlExport());

                }
            }
        }
        for (EadMetadataField emf : node.getContentAndStructureAreaAreaList()) {
            for (FieldValue fv : emf.getValues()) {
                if (StringUtils.isNotBlank(fv.getValuesForXmlExport())) {
                    createEadXmlField(xmlElement, isMainElement, emf, fv.getValuesForXmlExport());

                }
            }
        }
        for (EadMetadataField emf : node.getAccessAndUseAreaList()) {
            for (FieldValue fv : emf.getValues()) {
                if (StringUtils.isNotBlank(fv.getValuesForXmlExport())) {
                    createEadXmlField(xmlElement, isMainElement, emf, fv.getValuesForXmlExport());

                }
            }
        }
        for (EadMetadataField emf : node.getAlliedMaterialsAreaList()) {
            for (FieldValue fv : emf.getValues()) {
                if (StringUtils.isNotBlank(fv.getValuesForXmlExport())) {
                    createEadXmlField(xmlElement, isMainElement, emf, fv.getValuesForXmlExport());

                }
            }
        }
        for (EadMetadataField emf : node.getNotesAreaList()) {
            for (FieldValue fv : emf.getValues()) {
                if (StringUtils.isNotBlank(fv.getValuesForXmlExport())) {
                    createEadXmlField(xmlElement, isMainElement, emf, fv.getValuesForXmlExport());

                }
            }
        }
        for (EadMetadataField emf : node.getDescriptionControlAreaList()) {
            for (FieldValue fv : emf.getValues()) {
                if (StringUtils.isNotBlank(fv.getValuesForXmlExport())) {
                    createEadXmlField(xmlElement, isMainElement, emf, fv.getValuesForXmlExport());

                }
            }
        }
        Element dsc = null;
        if (isMainElement) {
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
            if (StringUtils.isNotBlank(node.getId())) {
                archdesc.setAttribute("id", node.getId());
            }
            if (StringUtils.isNotBlank(node.getNodeType())) {
                archdesc.setAttribute("type", node.getNodeType());
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
            if (StringUtils.isNotBlank(node.getNodeType())) {
                xmlElement.setAttribute("otherlevel", node.getNodeType());
            }
        }

        for (EadEntry subNode : node.getSubEntryList()) {
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

    private void createEadXmlField(Element xmlElement, boolean isMainElement, EadMetadataField emf, String metadataValue) {
        Element currentElement = xmlElement;
        String xpath = emf.getXpath();
        if (xpath.endsWith("[1]")) {
            xpath = xpath.replace("[1]", "");
        }
        if (xpath.matches("\\(.+\\|.*\\)")) {
            String[] parts = xpath.substring(1, xpath.lastIndexOf(")")).split("\\|");

            for (String part : parts) {
                if (isMainElement) {
                    if (part.contains("archdesc")) {
                        xpath = part;
                    }
                } else {
                    if (!part.contains("archdesc")) {
                        xpath = part;
                    }
                }
            }
        }
        // split xpath on "/"
        String[] fields = xpath.split("/");
        boolean written = false;
        for (String field : fields) {
            field = field.trim();

            // ignore .
            if (field.equals(".")) {
                continue;
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
                            continue;
                        } else if (condition.trim().startsWith("not")) {
                            condition = condition.substring(condition.indexOf("(" + 2), condition.indexOf(")"));
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
            if (StringUtils.isNotBlank(currentElement.getText())) {
                Element duplicate = new Element(currentElement.getName(), ns);
                for (Attribute attr : currentElement.getAttributes()) {
                    duplicate.setAttribute(attr.getName(), attr.getValue());
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
                if (condition.contains("]")) {
                    condition = condition.substring(1, condition.lastIndexOf("]"));
                } else {
                    condition = condition.substring(1);
                }
                condition = condition.trim();

                String[] attr = condition.split("=");
                element.setAttribute(attr[0], attr[1].replace("'", ""));
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

        // remove element from parent node
        EadEntry parentNode = selectedEntry.getParentNode();
        parentNode.removeSubEntry(selectedEntry);

        // add it to new parent
        destinationEntry.addSubEntry(selectedEntry);
        selectedEntry.setParentNode(destinationEntry);

        // set new hierarchy level to element and all children
        selectedEntry.updateHierarchy();
        flatEntryList = null;

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

        Collections.swap(selectedEntry.getParentNode().getSubEntryList(), selectedEntry.getOrderNumber(), selectedEntry.getOrderNumber() - 1);
        selectedEntry.getParentNode().reOrderElements();
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

        Collections.swap(selectedEntry.getParentNode().getSubEntryList(), selectedEntry.getOrderNumber(), selectedEntry.getOrderNumber() + 1);
        selectedEntry.getParentNode().reOrderElements();
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
        EadEntry previousNode = selectedEntry.getParentNode().getSubEntryList().get(selectedEntry.getOrderNumber().intValue() - 1);
        // move node to prev.
        destinationEntry = previousNode;
        moveNode();
        destinationEntry.setDisplayChildren(true);

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
        EadEntry oldParent = selectedEntry.getParentNode();
        EadEntry newParent = oldParent.getParentNode();

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

    private void searchInNode(EadEntry node) {
        if (node.getLabel() != null && node.getLabel().toLowerCase().contains(searchValue.toLowerCase())) {
            // mark element + all parents as displayable
            node.markAsFound();
        }
        if (node.getSubEntryList() != null) {
            for (EadEntry child : node.getSubEntryList()) {
                searchInNode(child);
            }
        }
    }

    public void resetSearch() {
        searchValue = null;
        rootElement.resetFoundList();
        flatEntryList = null;
    }

    public void createProcess() {
        // abort if no node is selected
        if (selectedEntry == null) {
            return;
        }
        if (StringUtils.isBlank(selectedEntry.getNodeType())) {
            // TODO error
            return;
        }

        // abort if process template is not defined
        if (processTemplateId == null || processTemplateId == 0) {
            return;
        }
        if (processTemplate == null) {
            // load process template
            processTemplate = ProcessManager.getProcessById(processTemplateId);
            if (processTemplate == null) {
                // TODO error
                return;
            }
        }

        // TODO configure process title rule?
        StringBuilder processTitleBuilder = new StringBuilder();
        //        processTitleBuilder.append(selectedEntry.getHierarchy());
        //        processTitleBuilder.append("_");
        //        processTitleBuilder.append(selectedEntry.getOrderNumber());
        //        processTitleBuilder.append("_");
        processTitleBuilder.append(selectedEntry.getId());
        processTitleBuilder.append("_");
        processTitleBuilder.append(selectedEntry.getLabel().toLowerCase());
        String processTitle = processTitleBuilder.toString().replaceAll("[\\W]", "_");

        // TODO check if title is unique, abort if not

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

        // save process
        try {
            ProcessManager.saveProcess(process);
        } catch (DAOException e1) {
            log.error(e1);
        }

        Prefs prefs = processTemplate.getRegelsatz().getPreferences();

        String publicationType = null;
        switch (selectedEntry.getNodeType()) {
            case "folder":
                publicationType = "Folder";
                break;
            case "file":
                publicationType = "File";
                break;
            case "image":
                publicationType = "Picture";
                break;
            case "audio":
                publicationType = "Audio";
                break;
            case "video":
                publicationType = "Video";
                break;
            case "other":
                publicationType = "Other";
                break;
        }

        try {
            // create mets file based on selected node type
            Fileformat fileformat = new MetsMods(prefs);
            DigitalDocument digDoc = new DigitalDocument();
            fileformat.setDigitalDocument(digDoc);
            DocStruct logical = digDoc.createDocStruct(prefs.getDocStrctTypeByName(publicationType));
            digDoc.setLogicalDocStruct(logical);
            Metadata identifier = new Metadata(prefs.getMetadataTypeByName("CatalogIDDigital"));
            identifier.setValue(selectedEntry.getId());
            logical.addMetadata(identifier);
            try {
                MetadataType eadIdType = prefs.getMetadataTypeByName("NodeId");
                if (eadIdType != null) {
                    Metadata eadid = new Metadata(eadIdType);
                    eadid.setValue(selectedEntry.getId());
                    logical.addMetadata(identifier);
                }
            } catch (UGHException e) {
                log.error(e);
            }

            // import configured metadata
            for (EadMetadataField emf : selectedEntry.getIdentityStatementAreaList()) {
                createModsMetadata(prefs, emf, logical);
            }
            for (EadMetadataField emf : selectedEntry.getContextAreaList()) {
                createModsMetadata(prefs, emf, logical);
            }
            for (EadMetadataField emf : selectedEntry.getContentAndStructureAreaAreaList()) {
                createModsMetadata(prefs, emf, logical);
            }
            for (EadMetadataField emf : selectedEntry.getAccessAndUseAreaList()) {
                createModsMetadata(prefs, emf, logical);
            }
            for (EadMetadataField emf : selectedEntry.getAlliedMaterialsAreaList()) {
                createModsMetadata(prefs, emf, logical);
            }
            for (EadMetadataField emf : selectedEntry.getNotesAreaList()) {
                createModsMetadata(prefs, emf, logical);
            }
            for (EadMetadataField emf : selectedEntry.getDescriptionControlAreaList()) {
                createModsMetadata(prefs, emf, logical);
            }
            EadEntry parent = selectedEntry.getParentNode();
            while (parent != null) {
                for (EadMetadataField emf : parent.getIdentityStatementAreaList()) {
                    if (emf.isImportMetadataInChild()) {
                        createModsMetadata(prefs, emf, logical);
                    }
                }
                for (EadMetadataField emf : parent.getContextAreaList()) {
                    if (emf.isImportMetadataInChild()) {
                        createModsMetadata(prefs, emf, logical);
                    }
                }
                for (EadMetadataField emf : parent.getContentAndStructureAreaAreaList()) {
                    if (emf.isImportMetadataInChild()) {
                        createModsMetadata(prefs, emf, logical);
                    }
                }
                for (EadMetadataField emf : parent.getAccessAndUseAreaList()) {
                    if (emf.isImportMetadataInChild()) {
                        createModsMetadata(prefs, emf, logical);
                    }
                }
                for (EadMetadataField emf : parent.getAlliedMaterialsAreaList()) {
                    if (emf.isImportMetadataInChild()) {
                        createModsMetadata(prefs, emf, logical);
                    }
                }
                for (EadMetadataField emf : parent.getNotesAreaList()) {
                    if (emf.isImportMetadataInChild()) {
                        createModsMetadata(prefs, emf, logical);
                    }
                }
                for (EadMetadataField emf : parent.getDescriptionControlAreaList()) {
                    if (emf.isImportMetadataInChild()) {
                        createModsMetadata(prefs, emf, logical);
                    }
                }

                parent = parent.getParentNode();
            }

            DocStruct physical = digDoc.createDocStruct(prefs.getDocStrctTypeByName("BoundBook"));
            digDoc.setPhysicalDocStruct(physical);
            Metadata imageFiles = new Metadata(prefs.getMetadataTypeByName("pathimagefiles"));
            imageFiles.setValue(process.getImagesTifDirectory(false));
            physical.addMetadata(imageFiles);
            // save fileformat
            process.writeMetadataFile(fileformat);
        } catch (UGHException | IOException | InterruptedException | SwapException | DAOException e) {
            log.error(e);
        }
    }

    //  create metadata, add it to logical element
    private void createModsMetadata(Prefs prefs, EadMetadataField emf, DocStruct logical) {
        if (StringUtils.isNotBlank(emf.getMetadataName())) {
            for (FieldValue fv : emf.getValues()) {
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
        String fileName = selectedDatabase.replaceAll("[\\W]", "_");
        if (!fileName.endsWith(".xml")) {
            fileName = fileName + ".xml";
        }
        FacesContext facesContext = FacesContextHelper.getCurrentFacesContext();
        if (!facesContext.getResponseComplete()) {
            HttpServletResponse response = (HttpServletResponse) facesContext.getExternalContext().getResponse();
            ServletContext servletContext = (ServletContext) facesContext.getExternalContext().getContext();
            String contentType = servletContext.getMimeType(fileName);
            response.setContentType(contentType);

            response.setHeader("Content-Disposition", "attachment;filename=\"" + fileName + "\"");

            try {
                ServletOutputStream out = response.getOutputStream();
                XMLOutputter outputter = new XMLOutputter(Format.getPrettyFormat());
                try {
                    outputter.output(document, out);
                } catch (IOException e) {
                    log.error(e);
                }
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
        User user = Helper.getCurrentUser();
        String username = user != null ? user.getNachVorname() : "-";
        if (!editorList.contains(username)) {
            editorList.add(username);
        }
        for (String editor : editorList) {
            Element item = new Element("item", ns);
            item.setText(editor);
            list.addContent(item);
        }
        String type;
        if (eventList.isEmpty()) {
            type = "Created";
        } else {
            type = "Modified";
        }
        String date = formatter.format(new Date());
        eventList.add(new StringPair(type, date));

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
        createEadDocument();

        return cancelEdition();

    }

    public String cancelEdition() {
        // reset current settings
        selectedDatabase = null;
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
        processTemplateId = null;
        processTemplate = null;

        // return to start screen
        return "";
    }

    public void validateArchive() {

        Map<String, List<String>> valueMap = new HashMap<>();

        validateNode(rootElement, valueMap);

    }

    private void validateNode(EadEntry node, Map<String, List<String>> valueMap) {
        node.setValid(true);
        for (EadMetadataField emf : node.getIdentityStatementAreaList()) {
            validateMetadataField(node, valueMap, emf);
        }
        for (EadMetadataField emf : node.getContextAreaList()) {
            validateMetadataField(node, valueMap, emf);
        }
        for (EadMetadataField emf : node.getContentAndStructureAreaAreaList()) {
            validateMetadataField(node, valueMap, emf);
        }
        for (EadMetadataField emf : node.getAccessAndUseAreaList()) {
            validateMetadataField(node, valueMap, emf);
        }
        for (EadMetadataField emf : node.getAlliedMaterialsAreaList()) {
            validateMetadataField(node, valueMap, emf);
        }
        for (EadMetadataField emf : node.getNotesAreaList()) {
            validateMetadataField(node, valueMap, emf);
        }
        for (EadMetadataField emf : node.getDescriptionControlAreaList()) {
            validateMetadataField(node, valueMap, emf);
        }

        for (EadEntry child : node.getSubEntryList()) {
            validateNode(child, valueMap);
        }

    }

    private void validateMetadataField(EadEntry node, Map<String, List<String>> valueMap, EadMetadataField emf) {
        emf.setValid(true);
        if (emf.getValidationType() != null) {
            if (emf.getValidationType().contains("unique")) {
                for (FieldValue fv : emf.getValues()) {
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
                for (FieldValue fv : emf.getValues()) {
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
                for (FieldValue fv : emf.getValues()) {
                    if (StringUtils.isNotBlank(fv.getValue())) {
                        if (!fv.getValue().matches(regex)) {
                            emf.setValid(false);
                            node.setValid(false);
                        }
                    }
                }
            }
        }
    }

}
