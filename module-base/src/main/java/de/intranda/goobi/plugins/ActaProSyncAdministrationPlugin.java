package de.intranda.goobi.plugins;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.commons.configuration.XMLConfiguration;
import org.apache.commons.configuration.reloading.FileChangedReloadingStrategy;
import org.apache.commons.configuration.tree.xpath.XPathExpressionEngine;
import org.apache.commons.lang3.StringUtils;
import org.goobi.interfaces.IEadEntry;
import org.goobi.interfaces.IMetadataField;
import org.goobi.interfaces.IMetadataGroup;
import org.goobi.interfaces.INodeType;
import org.goobi.interfaces.IValue;
import org.goobi.model.ExtendendValue;
import org.goobi.production.cli.helper.StringPair;
import org.goobi.production.enums.PluginType;
import org.goobi.production.plugin.interfaces.IAdministrationPlugin;

import de.intranda.goobi.plugins.model.ArchiveManagementConfiguration;
import de.intranda.goobi.plugins.model.EadEntry;
import de.intranda.goobi.plugins.model.RecordGroup;
import de.intranda.goobi.plugins.persistence.ArchiveManagementManager;
import de.intranda.goobi.plugins.persistence.NodeInitializer;
import de.sub.goobi.config.ConfigurationHelper;
import io.goobi.api.job.actapro.model.AuthenticationToken;
import io.goobi.api.job.actapro.model.Document;
import io.goobi.api.job.actapro.model.DocumentBlock;
import io.goobi.api.job.actapro.model.DocumentField;
import io.goobi.api.job.actapro.model.DocumentSearchFilter;
import io.goobi.api.job.actapro.model.DocumentSearchFilter.OperatorEnum;
import io.goobi.api.job.actapro.model.DocumentSearchParams;
import io.goobi.api.job.actapro.model.MetadataMapping;
import io.goobi.api.job.actapro.model.SearchResultPage;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.client.Invocation;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.Form;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.log4j.Log4j2;
import net.xeoh.plugins.base.annotations.PluginImplementation;

@PluginImplementation
@Log4j2
public class ActaProSyncAdministrationPlugin implements IAdministrationPlugin {

    private static final long serialVersionUID = 2632106883746583247L;

    @Getter
    private String title = "intranda_administration_actapro_sync";

    @Getter
    private PluginType type = PluginType.Administration;

    @Getter
    private String gui = "/uii/plugin_administration_actapro_sync.xhtml";

    private boolean completeSearch = false;

    @Getter
    private List<StringPair> configuredInventories;

    @Getter
    @Setter
    private StringPair database;

    // authentication
    String authServiceUrl;
    String authServiceHeader;
    String authServiceUsername;
    String authServicePassword;

    String connectorUrl;

    String identifierFieldName;
    @Getter
    private transient ArchiveManagementConfiguration config;

    private transient List<MetadataMapping> metadataFields;

    private INodeType folderType;
    private INodeType fileType;

    public ActaProSyncAdministrationPlugin() {
        log.trace("initialize plugin");
        try {
            readConfiguration();
        } catch (ConfigurationException e) {
            log.error(e);
        }
    }

    private void readConfiguration() throws ConfigurationException {

        try {
            config = new ArchiveManagementConfiguration();
            config.readConfiguration("");
        } catch (ConfigurationException e) {
            log.error(e);
        }

        XMLConfiguration actaProConfig = new XMLConfiguration(
                ConfigurationHelper.getInstance().getConfigurationFolder() + "plugin_intranda_quartz_actapro_sync.xml");
        actaProConfig.setListDelimiter('&');
        actaProConfig.setReloadingStrategy(new FileChangedReloadingStrategy());
        actaProConfig.setExpressionEngine(new XPathExpressionEngine());

        configuredInventories = new ArrayList<>();

        List<HierarchicalConfiguration> hcl = actaProConfig.configurationsAt("/inventory");

        for (HierarchicalConfiguration hc : hcl) {
            StringPair sp = new StringPair();
            sp.setOne(hc.getString("@archiveName"));
            sp.setTwo(hc.getString("@actaproId"));
            configuredInventories.add(sp);
        }

        authServiceUrl = actaProConfig.getString("/authentication/authServiceUrl");
        authServiceHeader = actaProConfig.getString("/authentication/authServiceHeader");
        authServiceUsername = actaProConfig.getString("/authentication/authServiceUsername");
        authServicePassword = actaProConfig.getString("/authentication/authServicePassword");
        connectorUrl = actaProConfig.getString("/connectorUrl");

        identifierFieldName = actaProConfig.getString("/eadIdField");

        metadataFields = new ArrayList<>();

        List<HierarchicalConfiguration> mapping = actaProConfig.configurationsAt("/metadata/field");
        for (HierarchicalConfiguration c : mapping) {
            MetadataMapping mm = new MetadataMapping(c.getString("@type"), c.getString("@groupType", ""), c.getString("@fieldType", "value"),
                    c.getString("@eadField"), c.getString("@eadGroup", ""), c.getString("@eadArea"));
            metadataFields.add(mm);
        }

        for (INodeType nodeType : config.getConfiguredNodes()) {
            if ("file".equals(nodeType.getNodeName())) {
                fileType = nodeType;
            } else if ("folder".equals(nodeType.getNodeName())) {
                folderType = nodeType;
            }
        }
    }

    public void downloadFromActaPro() {
        String databaseName = database.getOne();
        String actaproId = database.getTwo();

        // check if database exist, load it

        RecordGroup recordGroup = ArchiveManagementManager.getRecordGroupByTitle(databaseName);
        IEadEntry rootElement = ArchiveManagementManager.loadRecordGroup(recordGroup.getId());
        // otherwise create a new database

        // search for all actapro documents within configured id
        List<Document> documents = null;
        try (Client client = ClientBuilder.newClient()) {
            AuthenticationToken token = authenticate(client);
            documents = findDocuments(client, token, actaproId);
        }

        if (!documents.isEmpty()) {

            log.debug("found {} documents to check", documents.size());

            for (Document doc : documents) {
                // get doc id
                String documentId = doc.getDocKey();

                String parentNodeId = null;
                String docOrder = null;
                for (DocumentField field : doc.getBlock().getFields()) {
                    String fieldType = field.getType();
                    if ("Ref_Gp".equals(fieldType)) {
                        for (DocumentField subfield : field.getFields()) {
                            if ("Ref_DocKey".equals(subfield.getType())) {
                                parentNodeId = subfield.getValue();
                            } else if ("Ref_DocOrder".equals(subfield.getType())) {
                                docOrder = subfield.getValue();
                            }
                        }
                    }
                }

                log.debug("Document id: {}", documentId);

                // find matching ead entry
                Integer entryId = ArchiveManagementManager.findNodeById(identifierFieldName, documentId);
                if (entryId != null) {
                    IEadEntry entry = null;
                    for (IEadEntry e : rootElement.getAllNodes()) {
                        if (entryId.equals(e.getDatabaseId())) {
                            entry = e;
                            break;
                        }
                    }

                    NodeInitializer.initEadNodeWithMetadata(entry, getConfig().getConfiguredFields());
                    String fingerprintBeforeImport = entry.getFingerprint();

                    // check if document still have the same parent node
                    if (parentNodeId != null && docOrder != null) {
                        IEadEntry parentNode = entry.getParentNode();
                        // parentNode == null is the root element, should not be possible
                        if (parentNode != null) {
                            Integer parentEntryId = ArchiveManagementManager.findNodeById(identifierFieldName, parentNodeId);
                            if (parentEntryId == null) {
                                // node has been changed to a new parent node that does not yet exist
                                // TODO this case should not be possible because the new parent node is included in the document list before the current node and was created at this point
                            } else if (parentEntryId.intValue() != parentNode.getDatabaseId()) {
                                // node has a different parent

                                // remove element from old parent
                                parentNode.getSubEntryList().remove(entry);
                                // update order number of existing elements
                                parentNode.reOrderElements();
                                parentNode.updateHierarchy();
                                List<IEadEntry> nodesToUpdate = parentNode.getAllNodes();

                                // find new parent node
                                IEadEntry newParentNode = null;
                                for (IEadEntry e : rootElement.getAllNodes()) {
                                    if (e.getDatabaseId().equals(parentEntryId)) {
                                        newParentNode = e;
                                        break;
                                    }
                                }
                                entry.setParentNode(newParentNode);
                                newParentNode.addSubEntry(entry);
                                entry.setOrderNumber(Integer.parseInt(docOrder));
                                // move to correct position within the parent

                                newParentNode.sortElements();
                                newParentNode.updateHierarchy();

                                nodesToUpdate.addAll(newParentNode.getAllNodes());

                                ArchiveManagementManager.updateNodeHierarchy(recordGroup.getId(), nodesToUpdate);

                            }
                        }
                    }

                    // parse document, get metadata fields
                    parseDocumentMetadata(doc, entry);

                    String fingerprintAfterImport = entry.getFingerprint();
                    // save, if metadata was changed
                    if (!fingerprintBeforeImport.equals(fingerprintAfterImport)) {
                        ArchiveManagementManager.saveNode(recordGroup.getId(), entry);
                    }

                } else {

                    String[] paths = doc.getPath().split(";");
                    Integer lastElementId = null;
                    for (String path : paths) {
                        path = path.trim();
                        Integer parentEntryId = ArchiveManagementManager.findNodeById(identifierFieldName, path);
                        if (parentEntryId != null) {
                            // ancestor element exists
                            lastElementId = parentEntryId;
                        } else {
                            // ancestor element does not exist, create it as sub element of last existing node
                            IEadEntry lastAncestorNode = null;
                            for (IEadEntry e : rootElement.getAllNodes()) {
                                if (e.getDatabaseId().equals(lastElementId)) {
                                    lastAncestorNode = e;
                                    break;
                                }
                            }

                            EadEntry entry =
                                    new EadEntry(lastAncestorNode.isHasChildren() ? lastAncestorNode.getSubEntryList().size() : 0,
                                            lastAncestorNode.getHierarchy() + 1);
                            entry.setId("id_" + UUID.randomUUID());

                            entry.setLabel(doc.getDocTitle());

                            for (IMetadataField emf : config.getConfiguredFields()) {
                                if (emf.isGroup()) {
                                    NodeInitializer.loadGroupMetadata(entry, emf, null);
                                } else if (emf.getXpath().contains("unittitle")) {
                                    List<IValue> titleData = new ArrayList<>();
                                    titleData.add(new ExtendendValue(null, doc.getDocTitle(), null, null));
                                    IMetadataField toAdd = NodeInitializer.addFieldToEntry(entry, emf, titleData);
                                    NodeInitializer.addFieldToNode(entry, toAdd);
                                } else if (emf.getName().equals(identifierFieldName)) {
                                    List<IValue> idData = new ArrayList<>();
                                    idData.add(new ExtendendValue(null, doc.getDocKey(), null, null));
                                    IMetadataField toAdd = NodeInitializer.addFieldToEntry(entry, emf, idData);
                                    NodeInitializer.addFieldToNode(entry, toAdd);
                                } else {
                                    IMetadataField toAdd = NodeInitializer.addFieldToEntry(entry, emf, null);
                                    NodeInitializer.addFieldToNode(entry, toAdd);
                                }

                            }
                            try (Client client = ClientBuilder.newClient()) {
                                //  add all metadata from document
                                AuthenticationToken token = authenticate(client);
                                Document currentDoc = getDocumentByKey(client, token, path);

                                parseDocumentMetadata(currentDoc, entry);
                            }

                            for (IMetadataField emf : entry.getIdentityStatementAreaList()) {
                                if (emf.getName().equals(identifierFieldName)) {
                                    emf.getValues().get(0).setValue(path);
                                }
                            }

                            if (path.startsWith("Vz")) {
                                entry.setNodeType(fileType);
                            } else {
                                entry.setNodeType(folderType);
                            }

                            if (">Vz      246d752d-952b-48a7-b79d-3d8536bd0bab".equals(path)) {
                                System.out.println("test");
                            }

                            entry.setOrderNumber(Integer.parseInt(docOrder));
                            // move to correct position within the parent
                            lastAncestorNode.addSubEntry(entry);
                            lastAncestorNode.sortElements();
                            lastAncestorNode.updateHierarchy();
                            entry.calculateFingerprint();

                            ArchiveManagementManager.saveNode(recordGroup.getId(), entry);
                            lastElementId = entry.getDatabaseId();
                            ArchiveManagementManager.updateNodeHierarchy(recordGroup.getId(), lastAncestorNode.getAllNodes());

                        }

                    }

                }
            }
        }

    }

    private void parseDocumentMetadata(Document doc, IEadEntry entry) {
        DocumentBlock block = doc.getBlock();

        for (DocumentField field : block.getFields()) {

            String fieldType = field.getType();
            // find ead metadata name

            DocumentField matchedField = null;
            // first check, if field name is used in a group // has sub fields
            for (MetadataMapping mm : metadataFields) {
                if (mm.getJsonGroupType().equals(fieldType)) {
                    for (DocumentField subfield : field.getFields()) {
                        String subType = subfield.getType();
                        if (subType.equals(mm.getJsonType())) {
                            matchedField = subfield;
                        }
                    }
                    // if not, search for regular data
                } else if (mm.getJsonType().equals(fieldType)) {
                    matchedField = field;
                }

                if (matchedField != null) {
                    addMetadataValue(entry, mm, matchedField);
                }
            }
        }
        entry.calculateFingerprint();
    }

    private void addMetadataValue(IEadEntry entry, MetadataMapping matchedMapping, DocumentField matchedField) {
        String value = null;
        if ("value".equals(matchedMapping.getJsonFieldType())) {
            value = matchedField.getValue();
        } else if ("plain_value".equals(matchedMapping.getJsonFieldType())) {
            value = matchedField.getPlainValue();
        }
        // find configured ead group/field

        switch (matchedMapping.getEadArea()) {
            case "1":
                for (IMetadataField emf : entry.getIdentityStatementAreaList()) {
                    // add/replace value
                    saveValue(matchedMapping, value, emf);
                }
                break;
            case "2":
                for (IMetadataField emf : entry.getContextAreaList()) {
                    saveValue(matchedMapping, value, emf);
                }
                break;
            case "3":
                for (IMetadataField emf : entry.getContentAndStructureAreaAreaList()) {
                    saveValue(matchedMapping, value, emf);
                }
                break;
            case "4":
                for (IMetadataField emf : entry.getAccessAndUseAreaList()) {
                    saveValue(matchedMapping, value, emf);
                }
                break;
            case "5":
                for (IMetadataField emf : entry.getAlliedMaterialsAreaList()) {
                    saveValue(matchedMapping, value, emf);
                }
                break;
            case "6":
                for (IMetadataField emf : entry.getNotesAreaList()) {
                    saveValue(matchedMapping, value, emf);
                }
                break;
            case "7":
                for (IMetadataField emf : entry.getDescriptionControlAreaList()) {
                    saveValue(matchedMapping, value, emf);
                }
                break;
        }
    }

    public void uploadToActaPro() {

    }

    /**
     * Authenticate at the service
     * 
     * @param client
     * @return
     */

    public AuthenticationToken authenticate(Client client) {
        if (StringUtils.isBlank(authServiceHeader) || StringUtils.isBlank(authServiceHeader) || StringUtils.isBlank(authServiceUsername)
                || StringUtils.isBlank(authServicePassword)) {
            log.error("Authentication failure, missing configuration");
            return null;
        }

        Form form = new Form();
        form.param("username", authServiceUsername);
        form.param("password", authServicePassword);
        form.param("grant_type", "password");

        WebTarget target = client.target(authServiceUrl);

        Invocation.Builder builder = target.request();
        builder.header("Accept", "application/json");
        builder.header("Authorization", authServiceHeader);
        Response response = builder.post(Entity.form(form));

        return response.readEntity(AuthenticationToken.class);

    }

    /**
     * 
     * Find all documents created or modified after a given date
     *
     * @param client
     * @param token
     * @param date in format '2023-04-14T15:33:44Z'
     * @return
     */

    private List<Document> findDocuments(Client client, AuthenticationToken token, String rootElementId) {

        DocumentSearchParams searchRequest = new DocumentSearchParams();

        searchRequest.query("*");
        if (!completeSearch) {
            searchRequest.getFields().add("crdate");
            searchRequest.getFields().add("chdate");

            DocumentSearchFilter filter = new DocumentSearchFilter();
            filter.fieldName("crdate");
            filter.setOperator(OperatorEnum.GREATER_THAN_OR_EQUAL_TO);
            // TODO get from config, TODO update config with new timestamp after successful search
            filter.fieldValue("2023-04-14T15:33:44Z");
            searchRequest.addFiltersItem(filter);
        }
        //        searchRequest.addDocumentTypesItem("Arch");
        //        searchRequest.addDocumentTypesItem("Best");
        //        searchRequest.addDocumentTypesItem("Klas");
        //        searchRequest.addDocumentTypesItem("Ser");
        searchRequest.addDocumentTypesItem("Vz");
        //        searchRequest.addDocumentTypesItem("Tekt");

        List<Document> documents = new ArrayList<>();

        // post request
        boolean isLast = false;
        int currentPage = 1;
        while (!isLast) {
            WebTarget target = client.target(connectorUrl).path("documents").queryParam("page", currentPage);
            Invocation.Builder builder = target.request();
            builder.header("Accept", "application/json");
            builder.header("Authorization", "Bearer " + token.getAccessToken());

            Response response = builder.post(Entity.entity(searchRequest, MediaType.APPLICATION_JSON));

            if (response.getStatus() > 0) {

                SearchResultPage srp = response.readEntity(SearchResultPage.class);
                List<Map<String, String>> contentMap = srp.getContent();
                for (Map<String, String> content : contentMap) {
                    String id = content.get("id");
                    Document doc = getDocumentByKey(client, token, id);
                    doc.setPath(content.get("path"));
                    // only add documents from the selected archive
                    if (doc.getPath().startsWith(rootElementId)) {
                        documents.add(doc);
                    }
                }

                isLast = srp.getLast();
                currentPage++;

            } else {
                // TODO handle wrong status codes
                isLast = true;
            }
        }
        return documents;
    }

    /**
     * 
     * get a single document with the given key
     * 
     * @param client
     * @param token
     * @param key
     * @return
     */

    private Document getDocumentByKey(Client client, AuthenticationToken token, String key) {
        if (token == null) {
            return null;
        }
        WebTarget target = client.target(connectorUrl).path("document").path(key);
        Invocation.Builder builder = target.request();
        builder.header("Accept", "application/json");
        builder.header("Authorization", "Bearer " + token.getAccessToken());

        Response response = builder.get();
        if (response.getStatus() > 0) {

            Document doc = response.readEntity(Document.class);
            log.trace("DocKey: {}", doc.getDocKey());
            log.trace("type: {}", doc.getType());
            log.trace("DocTitle: {}", doc.getDocTitle());
            log.trace("CreatorID: {}", doc.getCreatorID());
            log.trace("OwnerID: {}", doc.getOwnerId());
            log.trace("CreationDate: {}", doc.getCreationDate());
            log.trace("ChangeDate: {}", doc.getChangeDate());
            log.trace("object: {}", doc.getObject());
            DocumentBlock block = doc.getBlock();
            log.trace("block type: {}", block.getType());

            for (DocumentField field : block.getFields()) {

                log.trace("*** {} ***", field.getType());
                if (StringUtils.isNotBlank(field.getPlainValue())) {
                    log.trace(field.getPlainValue());
                }
                if (StringUtils.isNotBlank(field.getValue())) {
                    log.trace(field.getValue());
                }
                if (field.getFields() != null) {
                    for (DocumentField subfield : field.getFields()) {
                        log.trace("    {}: {}", subfield.getType(), subfield.getValue());
                    }
                }
            }
            return doc;
        } else {
            log.error("Status: {}, Error: {}", response.getStatus(), response.getStatusInfo().getReasonPhrase());
            return null;
        }
    }

    private void saveValue(MetadataMapping matchedMapping, String value, IMetadataField emf) {
        if (StringUtils.isNotBlank(matchedMapping.getEadGroup())) {
            if (emf.getName().equals(matchedMapping.getEadGroup())) {
                IMetadataGroup grp = emf.getGroups().get(0);
                for (IMetadataField f : grp.getFields()) {
                    if (f.getName().equals(matchedMapping.getEadField())) {
                        if (!f.getValues().isEmpty()) {
                            f.getValues().get(0).setValue(value);
                        } else {
                            f.addValue();
                            f.getValues().get(0).setValue(value);
                        }
                    }
                }
            }
        } else if (emf.getName().equals(matchedMapping.getEadField())) {
            if (!emf.getValues().isEmpty()) {
                emf.getValues().get(0).setValue(value);
            } else {
                emf.addValue();
                emf.getValues().get(0).setValue(value);
            }
        }
    }
}
