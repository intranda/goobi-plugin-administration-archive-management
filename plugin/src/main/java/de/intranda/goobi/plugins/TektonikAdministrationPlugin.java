package de.intranda.goobi.plugins;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

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
import org.jdom2.xpath.XPathExpression;
import org.jdom2.xpath.XPathFactory;

import de.intranda.goobi.plugins.model.EadEntry;
import de.intranda.goobi.plugins.model.EadMetadataField;
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
    private String title = "intranda_administration_tektonik";

    @Getter
    private String value;

    @Getter
    private PluginType type = PluginType.Administration;

    @Getter
    private String gui = "/uii/plugin_administration_tektonik.xhtml";

    @Getter
    @Setter
    private String datastoreUrl = "http://localhost:8984/"; // TODO get this from config

    @Getter
    @Setter
    private EadEntry rootElement = null;

    private List<EadEntry> flatEntryList;

    @Getter
    private EadEntry selectedEntry;

    private static final Namespace ns = Namespace.getNamespace("ead", "urn:isbn:1-931666-22-9");
    private static XPathFactory xFactory = XPathFactory.instance();

    private List<EadMetadataField> configuredFields;

    /**
     * Constructor
     */
    public TektonikAdministrationPlugin() {
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
     * read the metadata for the current xml node.
     * - create an {@link EadEntry}
     * - execute the configured xpaths on the current node
     * - add the metadata to one of the 7 levels
     * - check if the node has sub nodes
     * - call the method recursively for all sub nodes
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

        // get child elements
        List<Element> clist = null;
        Element archdesc = element.getChild("archdesc", ns);
        if (archdesc != null) {
            Element dsc = archdesc.getChild("dsc", ns);
            if (dsc != null) {
                clist = dsc.getChildren("c", ns);
            }
        }

        if (clist == null) {
            clist = element.getChildren("c", ns);
        }
        if (clist != null) {
            int subOrder = 1;
            int subHierarchy = hierarchy + 1;
            for (Element c : clist) {

                EadEntry child = parseElement(subOrder, subHierarchy, c);
                entry.addSubEntry(child);
                subOrder++;
            }
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

        EadMetadataField toAdd =
                new EadMetadataField(emf.getName(), emf.getLevel(), emf.getXpath(), emf.getXpathType(), emf.isRepeatable(), emf.isVisible());

        toAdd.setValue(stringValue);

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
        XMLConfiguration xmlConfig;
        try {
            xmlConfig =
                    new XMLConfiguration(ConfigurationHelper.getInstance().getConfigurationFolder() + "plugin_intranda_administration_tektonik.xml");
            xmlConfig.setListDelimiter('&');
            xmlConfig.setReloadingStrategy(new FileChangedReloadingStrategy());
            xmlConfig.setExpressionEngine(new XPathExpressionEngine());

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
                        hc.getString("@xpathType", "element"), hc.getBoolean("@repeatable", false), hc.getBoolean("@visible", true));
                configuredFields.add(field);
            }
        } catch (ConfigurationException e2) {
            log.error(e2);
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

}
