package de.intranda.goobi.plugins.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.commons.configuration.XMLConfiguration;
import org.apache.commons.configuration.reloading.FileChangedReloadingStrategy;
import org.apache.commons.configuration.tree.xpath.XPathExpressionEngine;
import org.goobi.interfaces.IMetadataField;
import org.goobi.interfaces.INodeType;
import org.jdom2.Namespace;

import de.intranda.goobi.plugins.persistence.ArchiveManagementManager;
import de.sub.goobi.config.ConfigurationHelper;
import de.sub.goobi.helper.Helper;
import io.goobi.vocabulary.exchange.FieldDefinition;
import io.goobi.vocabulary.exchange.VocabularySchema;
import io.goobi.workflow.api.vocabulary.APIException;
import io.goobi.workflow.api.vocabulary.VocabularyAPIManager;
import io.goobi.workflow.api.vocabulary.helper.ExtendedVocabulary;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.log4j.Log4j2;

@Log4j2
@Getter
public class ArchiveManagementConfiguration {

    @Setter
    private XMLConfiguration xmlConfig;

    @Setter
    private List<IMetadataField> configuredFields;

    @Setter
    private List<INodeType> configuredNodes;
    private Map<String, List<String>> exportConfiguration;
    @Setter
    private Namespace nameSpaceRead;
    @Setter
    private Namespace nameSpaceWrite;
    private boolean showProjectSelection;
    private boolean showNodeIdInTree;
    private VocabularyAPIManager vocabularyAPI = null;
    private boolean displayIdentityStatementArea;
    private boolean displayContextArea;
    private boolean displayContentArea;
    private boolean displayAccessArea;
    private boolean displayMaterialsArea;
    private boolean displayNotesArea;
    private boolean displayControlArea;
    private String identifierMetadataName = "NodeId";
    private String identifierNodeName = "id";
    private String nodeDefaultTitle;
    private List<String> metadataFieldNames = new ArrayList<>();
    private List<String> advancedSearchFields = new ArrayList<>();

    // maximum length of each component that is to be used to generate the process title
    private int lengthLimit;
    // separator that will be used to join all components into a process title
    private String separator;

    private List<TitleComponent> titleParts = new ArrayList<>();
    // true if the id to be used should be taken from current node's parent node, false if it should be of the current node
    private boolean useIdFromParent;

    public ArchiveManagementConfiguration() throws ConfigurationException {

        xmlConfig = new XMLConfiguration(
                ConfigurationHelper.getInstance().getConfigurationFolder() + "plugin_intranda_administration_archive_management.xml");
        xmlConfig.setListDelimiter('&');
        xmlConfig.setReloadingStrategy(new FileChangedReloadingStrategy());
        xmlConfig.setExpressionEngine(new XPathExpressionEngine());
        vocabularyAPI = VocabularyAPIManager.getInstance();
    }

    public void readExportConfiguration() {
        exportConfiguration = new HashMap<>();
        List<HierarchicalConfiguration> subconfig = xmlConfig.configurationsAt("/export/file");

        for (HierarchicalConfiguration hc : subconfig) {
            String filename = hc.getString("@name");
            List<String> exportFolders = Arrays.asList(hc.getStringArray("/folder"));
            exportConfiguration.put(filename, exportFolders);
        }
    }

    /**
     * read in all parameters from the configuration file
     * 
     */
    public void readConfiguration(String databaseName) {
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

        readExportConfiguration();

        nameSpaceRead = Namespace.getNamespace("ead", config.getString("/eadNamespaceRead", "urn:isbn:1-931666-22-9"));
        nameSpaceWrite = Namespace.getNamespace("ead", config.getString("/eadNamespaceWrite", "urn:isbn:1-931666-22-9"));

        showProjectSelection = config.getBoolean("/showProjectSelection", false);

        showNodeIdInTree = config.getBoolean("/treeView/showNodeId", false);

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
            List<String> allowedChildren = Arrays.asList(hc.getStringArray("/child"));
            INodeType nt = new NodeType(hc.getString("@name"), hc.getString("@ruleset"), hc.getString("@icon"), hc.getInt("@processTemplateId"),
                    hc.getBoolean("@rootNode", false), hc.getBoolean("@allowProcessCreation", false), allowedChildren);
            configuredNodes.add(nt);
        }

        // configurations for generating process title
        lengthLimit = config.getInt("/lengthLimit", 0);
        separator = config.getString("/separator", "_");
        useIdFromParent = config.getBoolean("/useIdFromParent", false);
        titleParts.clear();
        for (HierarchicalConfiguration hc : config.configurationsAt("/title")) {
            String name = hc.getString("@name");
            String manipulationType = hc.getString("@type", null);
            String value = hc.getString("@value", "");
            TitleComponent comp = new TitleComponent(name, manipulationType, value);
            titleParts.add(comp);
        }
        advancedSearchFields = new ArrayList<>();
        metadataFieldNames.clear();
        // configurations for metadata
        for (HierarchicalConfiguration fieldConfig : config.configurationsAt("/metadata")) {

            IMetadataField field = createField(fieldConfig);
            configuredFields.add(field);
            // groups
            if (field.isGroup()) {
                for (HierarchicalConfiguration subfieldConfig : fieldConfig.configurationsAt("/metadata")) {
                    IMetadataField subfield = createField(subfieldConfig);
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

    public IMetadataField createField(HierarchicalConfiguration fieldConfig) {
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
        if ("person".equalsIgnoreCase(field.getFieldType())) {
            String lastnameXpath = fieldConfig.getString("/lastname/@xpath");
            String lastnameXpathType = fieldConfig.getString("/lastname/@xpathType", "element");
            String firstnameXpath = fieldConfig.getString("/firstname/@xpath");
            String firstnameXpathType = fieldConfig.getString("/firstname/@xpathType", "element");
            String authorityValueXpath = fieldConfig.getString("/authorityValue/@xpath");
            String authorityValueXpathType = fieldConfig.getString("/authorityValue/@xpathType", "element");

            Map<String, String> subElementMap = new HashMap<>();
            subElementMap.put("lastnameXpath", lastnameXpath);
            subElementMap.put("lastnameXpathType", lastnameXpathType);
            subElementMap.put("firstnameXpath", firstnameXpath);
            subElementMap.put("firstnameXpathType", firstnameXpathType);
            subElementMap.put("authorityValueXpath", authorityValueXpath);
            subElementMap.put("authorityValueXpathType", authorityValueXpathType);
            field.setSubfieldMap(subElementMap);
        } else if ("corporate".equalsIgnoreCase(field.getFieldType())) {
            String mainValueXpath = fieldConfig.getString("/value/@xpath");
            String mainValueXpathType = fieldConfig.getString("/value/@xpathType", "element");

            String subValueXpath = fieldConfig.getString("/subvalue/@xpath");
            String subValueXpathType = fieldConfig.getString("/subvalue/@xpathType", "element");

            String partValueXpath = fieldConfig.getString("/partvalue/@xpath");
            String partValueXpathType = fieldConfig.getString("/partvalue/@xpathType", "element");

            String authorityValueXpath = fieldConfig.getString("/authorityValue/@xpath");
            String authorityValueXpathType = fieldConfig.getString("/authorityValue/@xpathType", "element");

            Map<String, String> subElementMap = new HashMap<>();
            subElementMap.put("mainValueXpath", mainValueXpath);
            subElementMap.put("mainValueXpathType", mainValueXpathType);
            subElementMap.put("subValueXpath", subValueXpath);
            subElementMap.put("subValueXpathType", subValueXpathType);
            subElementMap.put("partValueXpath", partValueXpath);
            subElementMap.put("partValueXpathType", partValueXpathType);
            subElementMap.put("authorityValueXpath", authorityValueXpath);
            subElementMap.put("authorityValueXpathType", authorityValueXpathType);
            field.setSubfieldMap(subElementMap);

        }

        configureField(fieldConfig, field);
        return field;
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

    public void loadVocabulary(IMetadataField field) {
        List<String> iFieldValueList = Collections.emptyList();
        if (vocabularyAPI != null) {
            try {
                ExtendedVocabulary vocabulary = vocabularyAPI.vocabularies().findByName(field.getVocabularyName());
                if (field.getSearchParameter().isEmpty()) {
                    iFieldValueList = vocabularyAPI.vocabularyRecords()
                            .getRecordMainValues(vocabulary.getId());
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

                    iFieldValueList = vocabularyAPI.vocabularyRecords()
                            .getRecordMainValues(vocabularyAPI.vocabularyRecords()
                                    .list(vocabulary.getId())
                                    .search(searchField.get().getId() + ":" + searchFieldValue));
                }
            } catch (APIException e) {
                log.error(e);
                field.setVocabularyName(null);
            }
        } else {
            field.setVocabularyName(null);
        }
        field.setSelectItemList(iFieldValueList);
    }

}
