//package io.goobi.api.job.actapro;
//
//import java.util.ArrayList;
//import java.util.Collections;
//import java.util.Comparator;
//import java.util.List;
//import java.util.Map;
//
//import org.apache.commons.configuration.ConfigurationException;
//import org.apache.commons.configuration.HierarchicalConfiguration;
//import org.apache.commons.configuration.XMLConfiguration;
//import org.apache.commons.configuration.reloading.FileChangedReloadingStrategy;
//import org.apache.commons.configuration.tree.xpath.XPathExpressionEngine;
//import org.apache.commons.lang3.StringUtils;
//import org.goobi.interfaces.IEadEntry;
//import org.goobi.interfaces.IMetadataField;
//import org.goobi.interfaces.IMetadataGroup;
//import org.goobi.production.flow.jobs.AbstractGoobiJob;
//
//import de.intranda.goobi.plugins.model.ArchiveManagementConfiguration;
//import de.intranda.goobi.plugins.persistence.NodeInitializer;
//import de.sub.goobi.config.ConfigPlugins;
//import io.goobi.api.job.actapro.model.AuthenticationToken;
//import io.goobi.api.job.actapro.model.Document;
//import io.goobi.api.job.actapro.model.DocumentBlock;
//import io.goobi.api.job.actapro.model.DocumentField;
//import io.goobi.api.job.actapro.model.DocumentSearchFilter;
//import io.goobi.api.job.actapro.model.DocumentSearchFilter.OperatorEnum;
//import io.goobi.api.job.actapro.model.DocumentSearchParams;
//import io.goobi.api.job.actapro.model.MetadataMapping;
//import io.goobi.api.job.actapro.model.SearchResultPage;
//import jakarta.ws.rs.client.Client;
//import jakarta.ws.rs.client.ClientBuilder;
//import jakarta.ws.rs.client.Entity;
//import jakarta.ws.rs.client.Invocation;
//import jakarta.ws.rs.client.WebTarget;
//import jakarta.ws.rs.core.Form;
//import jakarta.ws.rs.core.MediaType;
//import jakarta.ws.rs.core.Response;
//import lombok.Getter;
//import lombok.extern.log4j.Log4j2;
//
//@Log4j2
//public class ActaproSyncQuartzPlugin extends AbstractGoobiJob {
//
//    // authentication
//    String authServiceUrl;
//    String authServiceHeader;
//    String authServiceUsername;
//    String authServicePassword;
//
//    String connectorUrl;
//
//    String identifierFieldName;
//    @Getter
//    ArchiveManagementConfiguration config;
//
//    @Getter
//    private String jobName = "intranda_quartz_actapro_sync";
//
//    private List<MetadataMapping> metadataFields;
//
//    /**
//     * When called, this method gets executed
//     */
//
//    @Override
//    public void execute() {
//        readConfiguration();
//
//        try (Client client = ClientBuilder.newClient()) {
//            AuthenticationToken token = authenticate(client);
//
//            List<Document> documents = searchInDocuments(client, token, "2023-04-14T15:33:44Z");
//
//            if (!documents.isEmpty()) {
//
//                // ArchiveManagementAdministrationPlugin line 915ff, move config to module-lib
//
//                // order list by path length to get parent nodes first
//                Collections.sort(documents, documentComparator);
//
//                for (Document doc : documents) {
//                    // get doc id
//                    String documentId = doc.getDocKey();
//
//                    // check if id exists in archive nodes
//                    IEadEntry entry = null;
//                    //                    ArchiveManagementManager.findNodeById(identifierFieldName, documentId);
//                    if (entry != null) {
//                        NodeInitializer.initEadNodeWithMetadata(entry, getConfig().getConfiguredFields());
//                        // -> if yes: check if parent is still the same
//                        //      -> if not: find new parent node
//                        //          -> if root element does not exist: create new database
//                        // -> update metadata for the node
//
//                        DocumentBlock block = doc.getBlock();
//                        String doctype = block.getType();
//
//                        List<DocumentField> fields = block.getFields();
//
//                        for (DocumentField field : fields) {
//                            String fieldType = field.getType();
//                            String nodeId = null;
//                            String nodeType = null;
//                            String order = null;
//                            if ("Ref_Gp".equals(fieldType)) {
//                                for (DocumentField subfield : field.getFields()) {
//                                    if ("Ref_DocKey".equals(subfield.getType())) {
//                                        nodeId = subfield.getValue();
//                                    } else if ("Ref_Doctype".equals(subfield.getType())) {
//                                        nodeType = subfield.getValue();
//                                    } else if ("Ref_Type".equals(subfield.getType())) {
//                                        // ???
//                                    } else if ("Ref_DocOrder".equals(subfield.getType())) {
//                                        order = subfield.getValue();
//                                    }
//                                }
//                            }
//
//                            // find ead metadata name
//
//                            MetadataMapping matchedMapping = null;
//                            DocumentField matchedField = null;
//                            // first check, if field name is used in a group // has sub fields
//                            for (MetadataMapping mm : metadataFields) {
//                                if (mm.getJsonGroupType().equals(fieldType)) {
//                                    for (DocumentField subfield : field.getFields()) {
//                                        String subType = subfield.getType();
//                                        if (subType.equals(mm.getJsonType())) {
//                                            matchedMapping = mm;
//                                            matchedField = subfield;
//                                        }
//                                    }
//                                    // if not, search for regular data
//                                } else if (mm.getJsonType().equals(fieldType)) {
//                                    matchedMapping = mm;
//                                    matchedField = field;
//                                }
//                            }
//
//                            if (matchedMapping != null) {
//                                String value = null;
//                                if ("value".equals(matchedMapping.getJsonFieldType())) {
//                                    value = matchedField.getValue();
//                                } else if ("plain_value".equals(matchedMapping.getJsonFieldType())) {
//                                    value = matchedField.getPlainValue();
//                                }
//                                // find configured ead group/field
//
//                                switch (matchedMapping.getEadArea()) {
//                                    case "1":
//                                        for (IMetadataField emf : entry.getIdentityStatementAreaList()) {
//                                            // add/replace value
//                                            saveValue(matchedMapping, value, emf);
//                                        }
//                                        break;
//                                    case "2":
//                                        for (IMetadataField emf : entry.getContextAreaList()) {
//                                            saveValue(matchedMapping, value, emf);
//                                        }
//                                        break;
//                                    case "3":
//                                        for (IMetadataField emf : entry.getContentAndStructureAreaAreaList()) {
//                                            saveValue(matchedMapping, value, emf);
//                                        }
//                                        break;
//                                    case "4":
//                                        for (IMetadataField emf : entry.getAccessAndUseAreaList()) {
//                                            saveValue(matchedMapping, value, emf);
//                                        }
//                                        break;
//                                    case "5":
//                                        for (IMetadataField emf : entry.getAlliedMaterialsAreaList()) {
//                                            saveValue(matchedMapping, value, emf);
//                                        }
//                                        break;
//                                    case "6":
//                                        for (IMetadataField emf : entry.getNotesAreaList()) {
//                                            saveValue(matchedMapping, value, emf);
//                                        }
//                                        break;
//                                    case "7":
//                                        for (IMetadataField emf : entry.getDescriptionControlAreaList()) {
//                                            saveValue(matchedMapping, value, emf);
//                                        }
//                                        break;
//                                }
//                            }
//                        }
//
//                        // save node, if fingerprint was changed
//
//                    } else {
//                        System.out.println(doc.getPath());
//
//                        // if node does not exists: recursively find parent element
//                        // find correct position in child list of parent, add node
//                        // add all metadata
//                    }
//                }
//            }
//
//        } catch (Exception e) {
//            if (e.getCause() != null) {
//                log.error(e.getCause());
//            } else {
//                log.error(e);
//            }
//        }
//
//    }
//
//    private void saveValue(MetadataMapping matchedMapping, String value, IMetadataField emf) {
//        if (StringUtils.isNotBlank(matchedMapping.getEadGroup())) {
//            if (emf.getName().equals(matchedMapping.getEadGroup())) {
//                IMetadataGroup grp = emf.getGroups().get(0);
//                for (IMetadataField f : grp.getFields()) {
//                    if (f.getName().equals(matchedMapping.getEadField())) {
//                        if (!f.getValues().isEmpty()) {
//                            f.getValues().get(0).setValue(value);
//                        } else {
//                            f.addValue();
//                            f.getValues().get(0).setValue(value);
//                        }
//                    }
//                }
//            }
//        } else if (emf.getName().equals(matchedMapping.getEadField())) {
//            if (!emf.getValues().isEmpty()) {
//                emf.getValues().get(0).setValue(value);
//            } else {
//                emf.addValue();
//                emf.getValues().get(0).setValue(value);
//            }
//        }
//    }
//
//    /**
//     * Parse the configuration file
//     */
//    public void readConfiguration() {
//        try {
//            config = new ArchiveManagementConfiguration();
//            config.readConfiguration("");
//        } catch (ConfigurationException e) {
//            log.error(e);
//        }
//
//        XMLConfiguration actaProConfig = ConfigPlugins.getPluginConfig(getJobName());
//        actaProConfig.setExpressionEngine(new XPathExpressionEngine());
//        actaProConfig.setReloadingStrategy(new FileChangedReloadingStrategy());
//
//        authServiceUrl = actaProConfig.getString("/authentication/authServiceUrl");
//        authServiceHeader = actaProConfig.getString("/authentication/authServiceHeader");
//        authServiceUsername = actaProConfig.getString("/authentication/authServiceUsername");
//        authServicePassword = actaProConfig.getString("/authentication/authServicePassword");
//        connectorUrl = actaProConfig.getString("/connectorUrl");
//
//        identifierFieldName = actaProConfig.getString("/eadIdField");
//
//        metadataFields = new ArrayList<>();
//
//        List<HierarchicalConfiguration> mapping = actaProConfig.configurationsAt("/metadata/field");
//        for (HierarchicalConfiguration c : mapping) {
//            MetadataMapping mm = new MetadataMapping(c.getString("type"), c.getString("groupType", ""), c.getString("jsonFieldType", "value"),
//                    c.getString("eadField"), c.getString("eadGroup", ""), c.getString("eadArea"));
//            metadataFields.add(mm);
//        }
//
//    }
//
//    /**
//     * Authenticate at the service
//     *
//     * @param client
//     * @return
//     */
//
//    public AuthenticationToken authenticate(Client client) {
//        if (StringUtils.isBlank(authServiceHeader) || StringUtils.isBlank(authServiceHeader) || StringUtils.isBlank(authServiceUsername)
//                || StringUtils.isBlank(authServicePassword)) {
//            log.error("Authentication failure, missing configuration");
//            return null;
//        }
//
//        Form form = new Form();
//        form.param("username", authServiceUsername);
//        form.param("password", authServicePassword);
//        form.param("grant_type", "password");
//
//        WebTarget target = client.target(authServiceUrl);
//
//        Invocation.Builder builder = target.request();
//        builder.header("Accept", "application/json");
//        builder.header("Authorization", authServiceHeader);
//        Response response = builder.post(Entity.form(form));
//
//        return response.readEntity(AuthenticationToken.class);
//
//    }
//
//    /**
//     *
//     * get a single document with the given key
//     *
//     * @param client
//     * @param token
//     * @param key
//     * @return
//     */
//
//    public Document getDocumentByKey(Client client, AuthenticationToken token, String key) {
//        if (token == null) {
//            return null;
//        }
//        WebTarget target = client.target(connectorUrl).path("document").path(key);
//        Invocation.Builder builder = target.request();
//        builder.header("Accept", "application/json");
//        builder.header("Authorization", "Bearer " + token.getAccessToken());
//
//        Response response = builder.get();
//        if (response.getStatus() == 200) {
//
//            Document doc = response.readEntity(Document.class);
//            log.trace("DocKey: {}", doc.getDocKey());
//            log.trace("type: {}", doc.getType());
//            log.trace("DocTitle: {}", doc.getDocTitle());
//            log.trace("CreatorID: {}", doc.getCreatorID());
//            log.trace("OwnerID: {}", doc.getOwnerId());
//            log.trace("CreationDate: {}", doc.getCreationDate());
//            log.trace("ChangeDate: {}", doc.getChangeDate());
//            log.trace("object: {}", doc.getObject());
//            DocumentBlock block = doc.getBlock();
//            log.trace("block type: {}", block.getType());
//
//            for (DocumentField field : block.getFields()) {
//
//                log.trace("*** {} ***", field.getType());
//                if (StringUtils.isNotBlank(field.getPlainValue())) {
//                    log.trace(field.getPlainValue());
//                }
//                if (StringUtils.isNotBlank(field.getValue())) {
//                    log.trace(field.getValue());
//                }
//                if (field.getFields() != null) {
//                    for (DocumentField subfield : field.getFields()) {
//                        log.trace("    {}: {}", subfield.getType(), subfield.getValue());
//                    }
//                }
//            }
//            return doc;
//        } else {
//            log.error("Status: {}, Error: {}", response.getStatus(), response.getStatusInfo().getReasonPhrase());
//            return null;
//        }
//    }
//
//    public Document updateDocument(Client client, AuthenticationToken token, String key) {
//        if (token == null) {
//            return null;
//        }
//
//        Document doc = getDocumentByKey(client, token, key);
//        // TODO change values in found document or create new document?
//
//        WebTarget target = client.target(connectorUrl).path("document").path(key);
//        Invocation.Builder builder = target.request();
//        builder.header("Accept", "application/json");
//        builder.header("Authorization", "Bearer " + token.getAccessToken());
//        Response response = builder.put(Entity.entity(doc, MediaType.APPLICATION_JSON));
//        if (response.getStatus() == 200) {
//            return response.readEntity(Document.class);
//        } else {
//            log.error("Status: {}, Error: {}", response.getStatus(), response.getStatusInfo().getReasonPhrase());
//            return null;
//        }
//
//    }
//
//    public boolean deleteDocument(Client client, AuthenticationToken token, String key) {
//        if (token == null) {
//            return false;
//        }
//        WebTarget target = client.target(connectorUrl).path("document").path(key);
//        Invocation.Builder builder = target.request();
//        builder.header("Accept", "application/json");
//        builder.header("Authorization", "Bearer " + token.getAccessToken());
//        Response response = builder.delete();
//        if (response.getStatus() == 200) {
//            return true;
//        } else {
//            log.error("Status: {}, Error: {}", response.getStatus(), response.getStatusInfo().getReasonPhrase());
//            return false;
//        }
//    }
//
//    public boolean createDocument(Client client, AuthenticationToken token, String parentDocKey, Document doc) {
//        if (token == null) {
//            return false;
//        }
//        WebTarget target = client.target(connectorUrl).path("document").queryParam("parentDocKey", parentDocKey).queryParam("format", "json");
//        Invocation.Builder builder = target.request();
//        builder.header("Accept", "application/json");
//        builder.header("Authorization", "Bearer " + token.getAccessToken());
//        Response response = builder.post(Entity.entity(doc, MediaType.APPLICATION_JSON));
//        if (response.getStatus() == 200) {
//            return true;
//        } else {
//            log.error("Status: {}, Error: {}", response.getStatus(), response.getStatusInfo().getReasonPhrase());
//            return false;
//        }
//    }
//
//    /**
//     *
//     * Find all documents created or modified after a given date
//     *
//     * @param client
//     * @param token
//     * @param date in format '2023-04-14T15:33:44Z'
//     * @return
//     */
//
//    public List<Document> searchInDocuments(Client client, AuthenticationToken token, String date) {
//
//        DocumentSearchParams searchRequest = new DocumentSearchParams();
//
//        searchRequest.query("*");
//
//        searchRequest.getFields().add("crdate");
//        searchRequest.getFields().add("chdate");
//
//        DocumentSearchFilter filter = new DocumentSearchFilter();
//        filter.fieldName("crdate");
//        filter.setOperator(OperatorEnum.GREATER_THAN_OR_EQUAL_TO);
//        filter.fieldValue(date);
//        searchRequest.addFiltersItem(filter);
//
//        List<Document> documents = new ArrayList<>();
//
//        // post request
//        boolean isLast = false;
//        int currentPage = 1;
//        while (!isLast) {
//            WebTarget target = client.target(connectorUrl).path("documents").queryParam("page", currentPage);
//            Invocation.Builder builder = target.request();
//            builder.header("Accept", "application/json");
//            builder.header("Authorization", "Bearer " + token.getAccessToken());
//
//            Response response = builder.post(Entity.entity(searchRequest, MediaType.APPLICATION_JSON));
//
//            if (response.getStatus() == 200) {
//
//                SearchResultPage srp = response.readEntity(SearchResultPage.class);
//                List<Map<String, String>> contentMap = srp.getContent();
//                for (Map<String, String> content : contentMap) {
//                    String id = content.get("id");
//                    Document doc = getDocumentByKey(client, token, id);
//                    doc.setPath(content.get("path"));
//                    documents.add(doc);
//                }
//
//                isLast = srp.getLast();
//                currentPage++;
//
//            } else {
//                // TODO handle wrong status codes
//                isLast = true;
//            }
//        }
//        return documents;
//    }
//
//    public static void main(String[] args) {
//        ActaproSyncQuartzPlugin plugin = new ActaproSyncQuartzPlugin();
//        plugin.readConfiguration();
//        plugin.execute();
//    }
//
//    public void getDocumentBinary(Client client, AuthenticationToken token, String key, String filename) {
//        if (token == null) {
//            return;
//        }
//
//        WebTarget target = client.target(connectorUrl).path("document").path(key).queryParam("binary", "path=" + filename);
//        Invocation.Builder builder = target.request();
//        builder.header("Accept", "application/json");
//        builder.header("Authorization", "Bearer " + token.getAccessToken());
//        Response response = builder.get();
//
//        // get output stream from response, save as file
//
//        // TODO probably not needed
//    }
//
//    private static Comparator<Document> documentComparator = new Comparator<>() {
//
//        @Override
//        public int compare(Document doc1, Document doc2) {
//            String path1 = doc1.getPath();
//            String path2 = doc2.getPath();
//
//            if (StringUtils.isBlank(path1) && StringUtils.isBlank(path2)) {
//                return 0;
//            } else if (StringUtils.isBlank(path1)) {
//                return 1;
//            } else if (StringUtils.isBlank(path2)) {
//                return -1;
//            } else {
//                return Integer.compare(path1.length(), path2.length());
//            }
//        }
//    };
//}
