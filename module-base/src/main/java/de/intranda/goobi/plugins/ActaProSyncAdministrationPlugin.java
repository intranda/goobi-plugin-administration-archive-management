package de.intranda.goobi.plugins;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.commons.configuration.XMLConfiguration;
import org.apache.commons.configuration.reloading.FileChangedReloadingStrategy;
import org.apache.commons.configuration.tree.xpath.XPathExpressionEngine;
import org.apache.commons.lang3.StringUtils;
import org.goobi.interfaces.IEadEntry;
import org.goobi.interfaces.IMetadataField;
import org.goobi.interfaces.IMetadataGroup;
import org.goobi.production.cli.helper.StringPair;
import org.goobi.production.enums.PluginType;
import org.goobi.production.plugin.interfaces.IAdministrationPlugin;

import de.intranda.goobi.plugins.model.ArchiveManagementConfiguration;
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
                    entry.calculateFingerprint();
                    String fingerprintBeforeImport = entry.getFingerprint();

                    // TODO check if document still have the same parent node

                    // parse document, get metadata fields
                    DocumentBlock block = doc.getBlock();

                    for (DocumentField field : block.getFields()) {

                        String fieldType = field.getType();
                        // find ead metadata name

                        MetadataMapping matchedMapping = null;
                        DocumentField matchedField = null;
                        // first check, if field name is used in a group // has sub fields
                        for (MetadataMapping mm : metadataFields) {
                            if (mm.getJsonGroupType().equals(fieldType)) {
                                for (DocumentField subfield : field.getFields()) {
                                    String subType = subfield.getType();
                                    if (subType.equals(mm.getJsonType())) {
                                        matchedMapping = mm;
                                        matchedField = subfield;
                                    }
                                }
                                // if not, search for regular data
                            } else if (mm.getJsonType().equals(fieldType)) {
                                matchedMapping = mm;
                                matchedField = field;
                            }
                        }

                        if (matchedMapping != null) {
                            addMetadataValue(entry, matchedMapping, matchedField);
                        }
                    }
                    entry.calculateFingerprint();

                    String fingerprintAfterImport = entry.getFingerprint();
                    // save, if metadata was changed
                    if (!fingerprintBeforeImport.equals(fingerprintAfterImport)) {
                        ArchiveManagementManager.saveNode(recordGroup.getId(), entry);
                    }

                } else {
                    // TODO: this is a new entry in actapro
                    // recursively find parent element
                    // find correct position in child list of parent, add node
                    // add all metadata

                    //                    DocumentBlock block = doc.getBlock();
                    //                    String doctype = block.getType();
                    //
                    //                    List<DocumentField> fields = block.getFields();
                    //
                    //                    for (DocumentField field : fields) {
                    //                        String fieldType = field.getType();
                    //                        String nodeId = null;
                    //                        String nodeType = null;
                    //                        String order = null;
                    //                        if ("Ref_Gp".equals(fieldType)) {
                    //                            for (DocumentField subfield : field.getFields()) {
                    //                                if ("Ref_DocKey".equals(subfield.getType())) {
                    //                                    nodeId = subfield.getValue();
                    //                                } else if ("Ref_Doctype".equals(subfield.getType())) {
                    //                                    nodeType = subfield.getValue();
                    //                                } else if ("Ref_Type".equals(subfield.getType())) {
                    //                                    // ???
                    //                                } else if ("Ref_DocOrder".equals(subfield.getType())) {
                    //                                    order = subfield.getValue();
                    //                                }
                    //                            }
                    //                        }
                    //                    }
                }
            }
        }

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

    private List<Document> findDocuments(Client client, AuthenticationToken token, String searchvalue) {

        DocumentSearchParams searchRequest = new DocumentSearchParams();

        searchRequest.query("*");

        searchRequest.getFields().add("crdate");
        searchRequest.getFields().add("chdate");

        DocumentSearchFilter filter = new DocumentSearchFilter();
        filter.fieldName("crdate");
        filter.setOperator(OperatorEnum.GREATER_THAN_OR_EQUAL_TO);
        filter.fieldValue("2023-04-14T15:33:44Z");
        searchRequest.addFiltersItem(filter);

        searchRequest.addDocumentTypesItem("Arch");
        searchRequest.addDocumentTypesItem("Best");
        searchRequest.addDocumentTypesItem("Klas");
        searchRequest.addDocumentTypesItem("Ser");
        searchRequest.addDocumentTypesItem("Vz");
        searchRequest.addDocumentTypesItem("Tekt");

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

            if (response.getStatus() == 200) {

                SearchResultPage srp = response.readEntity(SearchResultPage.class);
                List<Map<String, String>> contentMap = srp.getContent();
                for (Map<String, String> content : contentMap) {
                    String id = content.get("id");
                    Document doc = getDocumentByKey(client, token, id);
                    doc.setPath(content.get("path"));
                    documents.add(doc);
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
        if (response.getStatus() == 200) {

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
