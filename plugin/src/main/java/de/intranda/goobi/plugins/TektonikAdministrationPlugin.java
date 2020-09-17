package de.intranda.goobi.plugins;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.commons.configuration.XMLConfiguration;
import org.apache.commons.configuration.reloading.FileChangedReloadingStrategy;
import org.apache.commons.configuration.tree.xpath.XPathExpressionEngine;
import org.apache.commons.lang.StringUtils;
import org.goobi.production.enums.PluginType;
import org.goobi.production.plugin.interfaces.IAdministrationPlugin;
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
import de.schlichtherle.io.FileOutputStream;
import de.sub.goobi.config.ConfigurationHelper;
import de.sub.goobi.helper.HttpClientHelper;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.log4j.Log4j2;
import net.xeoh.plugins.base.annotations.PluginImplementation;

@PluginImplementation
@Log4j2
public class TektonikAdministrationPlugin implements IAdministrationPlugin {

    @Getter
    @Setter
    private String displayMode = "";

    @Getter
    private String title = "intranda_administration_tektonik";

    @Getter
    private PluginType type = PluginType.Administration;

    @Getter
    private String gui = "/uii/plugin_administration_tektonik.xhtml";

    @Getter
    @Setter
    private String datastoreUrl = "http://localhost:8984/";
    @Setter
    private String exportFolder = "/tmp/";

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

    /**
     * Constructor
     */
    public TektonikAdministrationPlugin() {
        try {
            xmlConfig =
                    new XMLConfiguration(ConfigurationHelper.getInstance().getConfigurationFolder() + "plugin_intranda_administration_tektonik.xml");
            xmlConfig.setListDelimiter('&');
            xmlConfig.setReloadingStrategy(new FileChangedReloadingStrategy());
            xmlConfig.setExpressionEngine(new XPathExpressionEngine());

            datastoreUrl = xmlConfig.getString("/basexUrl", "http://localhost:8984/");
            exportFolder = xmlConfig.getString("/eadExportFolder", "/tmp");
        } catch (ConfigurationException e2) {
            log.error(e2);
        }
    }

    @Getter
    @Setter
    private String selectedDatabase;

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

    /*
     * get ead root element from document
     */
    private void parseEadFile(Document document) {
        Element collection = document.getRootElement();
        Element eadElement = collection.getChild("ead", ns);
        rootElement = parseElement(1, 0, eadElement);
        rootElement.setDisplayChildren(true);
    }

    /**
     * read the metadata for the current xml node. - create an {@link EadEntry} - execute the configured xpaths on the current node - add the metadata
     * to one of the 7 levels - check if the node has sub nodes - call the method recursively for all sub nodes
     */
    private EadEntry parseElement(int order, int hierarchy, Element element) {
        EadEntry entry = new EadEntry(order, hierarchy);
        for (EadMetadataField emf : configuredFields) {
            //            if (hierarchy == 0 && emf.getElementType().equals("child")) {
            //                continue;
            //            }
            boolean added = false;
            if ("text".equalsIgnoreCase(emf.getXpathType())) {
                XPathExpression<Text> engine = xFactory.compile(emf.getXpath(), Filters.text(), null, ns);
                List<Text> values = engine.evaluate(element);
                if (emf.isRepeatable()) {
                    for (Text value : values) {
                        String stringValue = value.getValue();
                        addFieldToEntry(entry, emf, stringValue);
                        added = true;
                    }
                } else {
                    if (!values.isEmpty()) {
                        Text value = values.get(0);
                        String stringValue = value.getValue();
                        addFieldToEntry(entry, emf, stringValue);
                        added = true;
                    }
                }

            } else if ("attribute".equalsIgnoreCase(emf.getXpathType())) {
                XPathExpression<Attribute> engine = xFactory.compile(emf.getXpath(), Filters.attribute(), null, ns);
                List<Attribute> values = engine.evaluate(element);
                if (emf.isRepeatable()) {
                    for (Attribute value : values) {
                        String stringValue = value.getValue();
                        addFieldToEntry(entry, emf, stringValue);
                        added = true;
                    }
                } else {
                    if (!values.isEmpty()) {
                        Attribute value = values.get(0);
                        String stringValue = value.getValue();
                        addFieldToEntry(entry, emf, stringValue);
                        added = true;
                    }
                }
            } else {
                XPathExpression<Element> engine = xFactory.compile(emf.getXpath(), Filters.element(), null, ns);
                List<Element> values = engine.evaluate(element);
                if (emf.isRepeatable()) {
                    for (Element value : values) {
                        String stringValue = value.getValue();
                        addFieldToEntry(entry, emf, stringValue);
                        added = true;
                    }
                } else {
                    if (!values.isEmpty()) {
                        Element value = values.get(0);
                        String stringValue = value.getValue();
                        addFieldToEntry(entry, emf, stringValue);
                        added = true;
                    }
                }
            }
            if (!added) {
                // nothing found, add it as empty field
                addFieldToEntry(entry, emf, null);
            }
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
            entry.setNodeType(archdesc.getAttributeValue("type"));
            Element dsc = archdesc.getChild("dsc", ns);
            if (dsc != null) {
                clist = dsc.getChildren("c", ns);
            }
        } else {
            entry.setNodeType(element.getAttributeValue("otherlevel"));
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

    private void addFieldToEntry(EadEntry entry, EadMetadataField emf, String stringValue) {
        if (StringUtils.isBlank(entry.getLabel()) && emf.getXpath().contains("unittitle")) {
            entry.setLabel(stringValue);
        }

        EadMetadataField toAdd = new EadMetadataField(emf.getName(), emf.getLevel(), emf.getXpath(), emf.getXpathType(), emf.isRepeatable(),
                emf.isVisible(), emf.isShowField(), emf.getFieldType());
        toAdd.setSelectItemList(emf.getSelectItemList());
        toAdd.setValue(stringValue);

        // split single value into multiple fields
        if (toAdd.getFieldType().equals("multiselect") && StringUtils.isNotBlank(stringValue)) {
            String[] splittedValues = stringValue.split("; ");
            for (String val : splittedValues) {
                toAdd.setMultiselectValue(val);
            }
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
            config = xmlConfig.configurationAt("//config[./tectonics = '" + selectedDatabase + "']");
        } catch (IllegalArgumentException e) {
            try {
                config = xmlConfig.configurationAt("//config[./tectonics = '*']");
            } catch (IllegalArgumentException e1) {

            }
        }
        for (HierarchicalConfiguration hc : config.configurationsAt("/metadata")) {
            EadMetadataField field = new EadMetadataField(hc.getString("@name"), hc.getInt("@level"), hc.getString("@xpath"),
                    hc.getString("@xpathType", "element"), hc.getBoolean("@repeatable", false), hc.getBoolean("@visible", true),
                    hc.getBoolean("@showField", false), hc.getString("@fieldType", "input"));
            configuredFields.add(field);

            if (field.getFieldType().equals("dropdown") || field.getFieldType().equals("multiselect")) {
                List<String> valueList = Arrays.asList(hc.getStringArray("/value"));
                field.setSelectItemList(valueList);
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
            if (StringUtils.isNotBlank(emf.getValue())) {
                createEadXmlField(xmlElement, isMainElement, emf);
            }
        }
        for (EadMetadataField emf : node.getContextAreaList()) {
            if (StringUtils.isNotBlank(emf.getValue())) {
                createEadXmlField(xmlElement, isMainElement, emf);
            }
        }
        for (EadMetadataField emf : node.getContentAndStructureAreaAreaList()) {
            if (StringUtils.isNotBlank(emf.getValue())) {
                createEadXmlField(xmlElement, isMainElement, emf);
            }
        }
        for (EadMetadataField emf : node.getAccessAndUseAreaList()) {
            if (StringUtils.isNotBlank(emf.getValue())) {
                createEadXmlField(xmlElement, isMainElement, emf);
            }
        }
        for (EadMetadataField emf : node.getAlliedMaterialsAreaList()) {
            if (StringUtils.isNotBlank(emf.getValue())) {
                createEadXmlField(xmlElement, isMainElement, emf);
            }
        }
        for (EadMetadataField emf : node.getNotesAreaList()) {
            if (StringUtils.isNotBlank(emf.getValue())) {
                createEadXmlField(xmlElement, isMainElement, emf);
            }
        }
        for (EadMetadataField emf : node.getDescriptionControlAreaList()) {
            if (StringUtils.isNotBlank(emf.getValue())) {
                createEadXmlField(xmlElement, isMainElement, emf);
            }
        }
        Element dsc = null;
        if (isMainElement) {
            Element archdesc = xmlElement.getChild("archdesc", ns);

            dsc = archdesc.getChild("dsc", ns);

            if (StringUtils.isNotBlank(node.getId())) {
                archdesc.setAttribute("id", node.getId());
            }
            if (StringUtils.isNotBlank(node.getNodeType())) {
                archdesc.setAttribute("type", node.getNodeType());
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
            addMetadata(c, subNode);
        }

    }

    private void createEadXmlField(Element xmlElement, boolean isMainElement, EadMetadataField emf) {
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
                currentElement.setAttribute(field, emf.getValueForXmlExport());
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
            currentElement.setText(emf.getValueForXmlExport());
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
        if (selectedEntry.getOrderNumber().intValue() == selectedEntry.getParentNode().getSubEntryList().size()-1) {
            return;
        }

        Collections.swap(selectedEntry.getParentNode().getSubEntryList(), selectedEntry.getOrderNumber(), selectedEntry.getOrderNumber()+1);
        selectedEntry.getParentNode().reOrderElements();
        flatEntryList = null;
    }

}
