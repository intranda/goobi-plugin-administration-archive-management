package de.intranda.goobi.plugins.persistence;

import java.io.Serializable;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.dbutils.QueryRunner;
import org.apache.commons.dbutils.ResultSetHandler;
import org.apache.commons.dbutils.handlers.BeanHandler;
import org.apache.commons.dbutils.handlers.BeanListHandler;
import org.apache.commons.lang3.StringUtils;
import org.goobi.interfaces.IEadEntry;
import org.goobi.interfaces.INodeType;
import org.goobi.interfaces.IRecordGroup;
import org.goobi.interfaces.IValue;
import org.goobi.model.ExtendendValue;
import org.goobi.model.GroupValue;

import de.intranda.goobi.plugins.model.EadEntry;
import de.intranda.goobi.plugins.model.RecordGroup;
import de.sub.goobi.persistence.managers.DatabaseVersion;
import de.sub.goobi.persistence.managers.MySQLHelper;
import lombok.extern.log4j.Log4j2;

@Log4j2
public class ArchiveManagementManager implements Serializable {

    private static final long serialVersionUID = 2861873896714636026L;

    private static List<INodeType> configuredNodes;

    // $1 = metadata name, $2=authority type, $3 = authority value, $4 = metadata value
    private static Pattern metadataPattern = Pattern.compile("<([^<\\/ ]+?)(?: source=\'(.+)\' value=\'(.+)\')?>([^<]+)<\\/[^<]+?>");

    // $1 = group name
    private static Pattern groupPattern = Pattern.compile("<group name=\'(.*?)\'>[\\w\\W]*?<\\/group>"); //NOSONAR \w\W is needed because '.' does not include newline

    // $1 = metadata name, $2=authority type, $3 = authority value, $4 = metadata value
    private static Pattern subfieldPattern = Pattern.compile("<field name=\'(.*?)\'(?: source=\'(.+)\' value=\'(.+)\')?>([^<]*)<\\/field>");

    public static void setConfiguredNodes(List<INodeType> configuredNodes) {
        ArchiveManagementManager.configuredNodes = configuredNodes;
    }

    public static final void createTables() {
        StringBuilder sql = new StringBuilder();
        sql.append("CREATE TABLE IF NOT EXISTS archive_record_group ( ");
        sql.append("id INT(11)  unsigned NOT NULL AUTO_INCREMENT, ");
        sql.append("title VARCHAR(255),  ");
        sql.append("PRIMARY KEY (id)  ");
        sql.append(") ENGINE = InnoDB DEFAULT CHARACTER SET = utf8mb4; ");
        try {
            DatabaseVersion.runSql(sql.toString());
        } catch (SQLException e) {
            log.error(e);
        }
        sql = new StringBuilder();
        sql.append("CREATE TABLE IF NOT EXISTS archive_record_node ( ");
        sql.append("id int(11) PRIMARY KEY, ");
        sql.append("uuid varchar(255), ");
        sql.append("archive_record_group_id INT(11), ");
        sql.append("hierarchy int(11), ");
        sql.append("order_number int(11), ");
        sql.append("node_type varchar(255), ");
        sql.append("sequence varchar(255), ");
        sql.append("processtitle varchar(255), ");
        sql.append("parent_id int(11), ");
        sql.append("label text, ");
        sql.append("data text, ");
        sql.append("KEY label (label(768)), ");
        sql.append("KEY archive_record_group_id (archive_record_group_id), ");
        sql.append("KEY sequence (sequence), ");
        sql.append("KEY parent_id (parent_id) ");
        sql.append(") ENGINE = InnoDB DEFAULT CHARACTER SET = utf8mb4; ");

        try {
            DatabaseVersion.runSql(sql.toString());
        } catch (SQLException e) {
            log.error(e);
        }
    }

    public static void saveRecordGroup(RecordGroup grp) {
        try (Connection connection = MySQLHelper.getInstance().getConnection()) {
            QueryRunner run = new QueryRunner();
            // insert as new
            String sql = "INSERT INTO archive_record_group (id, title) VALUES (?, ?) ON DUPLICATE KEY UPDATE title = VALUES(title)";
            Integer id = run.insert(connection, sql, MySQLHelper.resultSetToIntegerHandler, grp.getId(), grp.getTitle());
            if (id != null) {
                grp.setId(id);
            }

        } catch (SQLException e) {
            log.error(e);
        }
    }

    public static List<IRecordGroup> getAllRecordGroups() {
        try (Connection connection = MySQLHelper.getInstance().getConnection()) {
            QueryRunner run = new QueryRunner();
            return run.query(connection, "SELECT * FROM archive_record_group ORDER BY title", new BeanListHandler<>(RecordGroup.class));
        } catch (SQLException e) {
            log.error(e);
        }
        return Collections.emptyList();
    }

    public static RecordGroup getRecordGroupByTitle(String title) {
        try (Connection connection = MySQLHelper.getInstance().getConnection()) {
            QueryRunner run = new QueryRunner();
            return run.query(connection, "SELECT * FROM archive_record_group WHERE title = ?", new BeanHandler<>(RecordGroup.class), title);
        } catch (SQLException e) {
            log.error(e);
        }
        return null;
    }

    public static void saveNode(Integer archiveId, IEadEntry node) {
        List<IEadEntry> list = new ArrayList<>();
        list.add(node);
        saveNodes(archiveId, list);
    }

    public static void updateProcessLink(IEadEntry node) {
        String sql = "update archive_record_node set processtitle = ? where id = ?";
        try (Connection connection = MySQLHelper.getInstance().getConnection()) {
            QueryRunner run = new QueryRunner();
            run.update(connection, sql, node.getGoobiProcessTitle(), node.getDatabaseId());
        } catch (SQLException e) {
            log.error(e);
        }
    }

    public static synchronized void saveNodes(Integer archiveId, List<IEadEntry> nodes) {
        String insertSql =
                "INSERT INTO archive_record_node (id, uuid, archive_record_group_id, hierarchy, order_number, node_type, sequence, processtitle, parent_id,label, data) VALUES ";

        // get next free id
        String nextIdSql = "SELECT max(id) +1 from archive_record_node";
        try (Connection connection = MySQLHelper.getInstance().getConnection()) {
            QueryRunner run = new QueryRunner();
            int nextAutoIncrementDbID = run.query(connection, nextIdSql, MySQLHelper.resultSetToIntegerHandler);
            // assign new ids to all entries without id
            for (IEadEntry node : nodes) {
                if (node.getDatabaseId() == null) {
                    node.setDatabaseId(nextAutoIncrementDbID++);
                }
            }
        } catch (SQLException e) {
            log.error(e);
        }

        log.debug("Save {} records", nodes.size());

        StringBuilder values = new StringBuilder();
        for (int i = 0; i < nodes.size(); i++) {
            IEadEntry entry = nodes.get(i);
            if (values.length() > 0) {
                values.append(", ");
            }
            Integer parentId = null;
            if (entry.getParentNode() != null) {
                parentId = entry.getParentNode().getDatabaseId();
            }

            String label = MySQLHelper.escapeSql(entry.getLabel());
            if (label != null && label.endsWith("\\") && !label.endsWith("\\\\")) {
                label = label + "\\";
            }

            values.append("(");
            values.append(entry.getDatabaseId());
            values.append(", '");
            values.append(entry.getId());
            values.append("', ");
            values.append(archiveId);
            values.append(", ");
            values.append(entry.getHierarchy());
            values.append(", ");
            values.append(entry.getOrderNumber());
            values.append(", '");
            values.append(entry.getNodeType() == null ? "" : entry.getNodeType().getNodeName());
            values.append("', '");
            values.append(entry.getSequence());
            if (entry.getGoobiProcessTitle() == null) {
                values.append("', null, ");
            } else {
                values.append("', '");
                values.append(entry.getGoobiProcessTitle());
                values.append("', ");
            }
            values.append(parentId);
            values.append(", '");
            values.append(label);
            values.append("', \"");
            values.append(entry.getDataAsXml());
            values.append("\"");
            values.append(")");
            // save every 50 records or when we reached the last one
            if (i % 50 == 49 || i + 1 == nodes.size()) {
                StringBuilder sql = new StringBuilder(insertSql);
                sql.append(values.toString());
                sql.append("ON DUPLICATE KEY UPDATE  uuid = VALUES(uuid), hierarchy = VALUES(hierarchy), order_number = VALUES(order_number), "
                        + "node_type =  VALUES(node_type), sequence = VALUES(sequence), processtitle = VALUES(processtitle), "
                        + "processtitle = VALUES(processtitle), parent_id = VALUES(parent_id), label = VALUES(label), data = VALUES(data)");
                try (Connection connection = MySQLHelper.getInstance().getConnection()) {
                    QueryRunner run = new QueryRunner();
                    run.update(connection, sql.toString());
                } catch (SQLException e) {
                    log.error(e);
                }
                // reset values
                values = new StringBuilder();
            }
        }
    }

    public static void updateNodeHierarchy(Integer archiveId, List<IEadEntry> nodes) {
        String insertSql = "INSERT INTO archive_record_node (id, hierarchy, order_number, sequence, parent_id) VALUES ";

        StringBuilder values = new StringBuilder();
        for (int i = 0; i < nodes.size(); i++) {
            IEadEntry entry = nodes.get(i);
            if (values.length() > 0) {
                values.append(", ");
            }
            Integer parentId = null;
            if (entry.getParentNode() != null) {
                parentId = entry.getParentNode().getDatabaseId();
            }

            values.append("(");
            values.append(entry.getDatabaseId());
            values.append(", ");
            values.append(entry.getHierarchy());
            values.append(", ");
            values.append(entry.getOrderNumber());
            values.append(", '");
            values.append(entry.getSequence());
            values.append("', ");
            values.append(parentId);
            values.append(")");
            // save every 50 records or when we reached the last one
            if (i % 50 == 49 || i + 1 == nodes.size()) {
                StringBuilder sql = new StringBuilder(insertSql);
                sql.append(values.toString());
                sql.append(
                        "ON DUPLICATE KEY UPDATE hierarchy = VALUES(hierarchy), order_number = VALUES(order_number), sequence = VALUES(sequence), parent_id = VALUES(parent_id)");
                try (Connection connection = MySQLHelper.getInstance().getConnection()) {
                    QueryRunner run = new QueryRunner();
                    run.update(connection, sql.toString());
                } catch (SQLException e) {
                    log.error(e);
                }
                // reset values
                values = new StringBuilder();
            }
        }

    }

    public static IEadEntry loadRecordGroup(int recordGroupId) {

        try (Connection connection = MySQLHelper.getInstance().getConnection()) {
            QueryRunner run = new QueryRunner();
            return run.query(connection,
                    "SELECT * FROM archive_record_node WHERE archive_record_group_id = ? ORDER BY hierarchy, sequence, order_number",
                    resultSetToNodeHandler, recordGroupId);
        } catch (SQLException e) {
            log.error(e);
        }

        return null;
    }

    public static Map<String, List<IValue>> loadMetadataForNode(int id) {
        try (Connection connection = MySQLHelper.getInstance().getConnection()) {
            QueryRunner run = new QueryRunner();
            return run.query(connection,
                    "SELECT data FROM archive_record_node WHERE id = ?",
                    resultSetMetadataHandler, id);

        } catch (SQLException e) {
            log.error(e);
        }
        return Collections.emptyMap();
    }

    private static final ResultSetHandler<Map<String, List<IValue>>> resultSetMetadataHandler = new ResultSetHandler<>() {

        @Override
        public Map<String, List<IValue>> handle(ResultSet rs) throws SQLException {
            if (rs.next()) {
                String data = rs.getString("data");
                return convertStringToMap(data);
            }
            return Collections.emptyMap();
        }
    };

    private static final ResultSetHandler<IEadEntry> resultSetToNodeHandler = new ResultSetHandler<>() {

        @Override
        public IEadEntry handle(ResultSet rs) throws SQLException {
            IEadEntry lastElement = null;
            IEadEntry rootElement = null;
            while (rs.next()) {

                int id = rs.getInt("id");
                String uuid = rs.getString("uuid");

                int hierarchy = rs.getInt("hierarchy");
                int orderNumber = rs.getInt("order_number");
                String nodeTypeName = rs.getString("node_type");
                String sequence = rs.getString("sequence");
                String processtitle = rs.getString("processtitle");
                Integer parentId = rs.getInt("parent_id");
                if (rs.wasNull()) {
                    parentId = null;
                }
                String label = rs.getString("label");

                String data = rs.getString("data");

                IEadEntry currentEntry = new EadEntry(orderNumber, hierarchy);

                currentEntry.setDatabaseId(id);
                currentEntry.setId(uuid);
                for (INodeType nt : configuredNodes) {
                    if (nt.getNodeName().equals(nodeTypeName)) {
                        currentEntry.setNodeType(nt);
                    }
                }
                currentEntry.setSequence(sequence);
                currentEntry.setGoobiProcessTitle(processtitle);
                currentEntry.setLabel(label);
                currentEntry.setData(data);

                if (parentId == null) {
                    rootElement = currentEntry;
                } else if (parentId == lastElement.getDatabaseId().intValue()) {
                    // new element is a child of the last one
                    currentEntry.setParentNode(lastElement);
                    lastElement.getSubEntryList().add(currentEntry);
                } else if (parentId == lastElement.getParentNode().getDatabaseId().intValue()) {
                    // new element is a sibling of last one
                    currentEntry.setParentNode(lastElement.getParentNode());
                    lastElement.getParentNode().getSubEntryList().add(currentEntry);
                } else {
                    // use sequence number to find correct parent element
                    String[] parts = sequence.split("\\.");

                    IEadEntry e = rootElement;

                    for (int i = 1; i < parts.length; i++) {
                        String partNumber = parts[i];
                        int ordnerNum = Integer.parseInt(partNumber);
                        for (IEadEntry sub : e.getSubEntryList()) {
                            if (sub.getOrderNumber().intValue() == ordnerNum) {
                                e = sub;
                                break;
                            }
                        }
                    }
                    // add it as sub element, after we found the parent
                    e.getSubEntryList().add(currentEntry);
                    currentEntry.setParentNode(e);
                }
                lastElement = currentEntry;
            }
            return rootElement;
        }
    };

    // only call it to load/enhance the selected node. Otherwise the metadata is not needed

    public static Map<String, List<IValue>> convertStringToMap(String data) {
        Map<String, List<IValue>> metadataMap = new HashMap<>();
        if (StringUtils.isNotBlank(data)) {
            data = data.replace("<xml>", "").replace("</xml>", "");
            for (Matcher m = metadataPattern.matcher(data); m.find();) {
                MatchResult mr = m.toMatchResult();
                String metadata = mr.group(1);
                String authorityType = mr.group(2);
                String authorityValue = mr.group(3);
                String value = mr.group(4);
                List<IValue> values = metadataMap.getOrDefault(metadata, new ArrayList<>());
                values.add(new ExtendendValue(metadata, value.replace("&lt;", "<").replace("&gt;", ">").replace("&amp;", "&").replace("''", "'"),
                        authorityType,
                        authorityValue));
                metadataMap.put(metadata, values);
            }

            for (Matcher m = groupPattern.matcher(data); m.find();) {

                MatchResult mr = m.toMatchResult();
                String group = mr.group();
                String groupName = mr.group(1);
                List<IValue> values = metadataMap.getOrDefault(groupName, new ArrayList<>());

                GroupValue gv = new GroupValue();
                gv.setGroupName(groupName);
                values.add(gv);
                for (Matcher subFields = subfieldPattern.matcher(group); subFields.find();) {
                    MatchResult sub = subFields.toMatchResult();
                    String metadata = sub.group(1);
                    String authorityType = sub.group(2);
                    String authorityValue = sub.group(3);
                    String value = sub.group(4);

                    List<IValue> subvalues = gv.getSubfields().getOrDefault(metadata, new ArrayList<>());
                    subvalues.add(new ExtendendValue(metadata,
                            value.replace("&lt;", "<").replace("&gt;", ">").replace("&amp;", "&").replace("''", "'"), authorityType,
                            authorityValue));
                    gv.getSubfields().put(metadata, subvalues);
                }
                metadataMap.put(groupName, values);
            }

        }
        return metadataMap;
    }

    public static void deleteNodes(List<IEadEntry> nodesToDelete) {
        // create id list
        StringBuilder identifierList = new StringBuilder();
        for (IEadEntry e : nodesToDelete) {
            if (e.getDatabaseId() != null) {
                if (identifierList.length() > 0) {
                    identifierList.append(", ");
                }
                identifierList.append(e.getDatabaseId());
            }
        }
        if (identifierList.length() > 0) {
            String sql = "DELETE FROM archive_record_node WHERE id IN (" + identifierList.toString() + ")";
            try (Connection connection = MySQLHelper.getInstance().getConnection()) {
                QueryRunner run = new QueryRunner();
                run.execute(connection, sql);
            } catch (SQLException e1) {
                log.error(e1);
            }

        }
    }

    /**
     * 
     * Perform an advanced search within a record group. It can be used to further restrict a hit set.
     * 
     * If no list is specified, the search is performed for all nodes in the record group, otherwise only for the specified sub set.
     * 
     * @param recordGroupId
     * @param fieldName
     * @param searchValue
     * @param filteredList
     * @return
     */

    public static List<Integer> advancedSearch(int recordGroupId, String fieldName, String searchValue, List<Integer> filteredList) {

        String escapedSearchValue = MySQLHelper.escapeSql(searchValue);

        if (filteredList == null || filteredList.isEmpty()) {
            // search over all records

            return simpleSearch(recordGroupId, fieldName, searchValue);

        } else {
            StringBuilder sql = new StringBuilder();
            sql.append("select id from archive_record_node WHERE archive_record_group_id = ? AND ");
            if (StringUtils.isBlank(fieldName)) {
                sql.append("data like '%");
            } else {
                sql.append("  ExtractValue(data, '/xml/").append(fieldName).append("') like '%");
            }
            sql.append(escapedSearchValue);
            sql.append("%'");

            sql.append(" AND id in (");
            StringBuilder ids = new StringBuilder();
            for (Integer id : filteredList) {
                if (ids.length() > 0) {
                    ids.append(",");
                }
                ids.append(id);
            }
            sql.append(ids.toString());
            sql.append(")");
            try (Connection connection = MySQLHelper.getInstance().getConnection()) {
                QueryRunner run = new QueryRunner();
                return run.query(connection, sql.toString(), MySQLHelper.resultSetToIntegerListHandler, recordGroupId);
            } catch (SQLException e) {
                log.error(e);
            }
        }
        return Collections.emptyList();
    }

    /**
     * 
     * Perform a simple search within a record group
     * 
     * @param recordGroupId record group id
     * @param fieldnames list of fieldnames to search in. If the list is empty, all data is searched
     * @param searchValue the actual search value
     * @return
     */

    public static List<Integer> simpleSearch(int recordGroupId, String fieldName, String searchValue) {

        // abort, if search value is empty
        if (StringUtils.isBlank(searchValue)) {
            return Collections.emptyList();
        }

        String escapedSearchValue = MySQLHelper.escapeSql(searchValue);

        StringBuilder sql = new StringBuilder();
        sql.append("select id from archive_record_node WHERE archive_record_group_id = ? AND (");

        // search in all fields
        if (StringUtils.isBlank(fieldName)) {
            sql.append("  data like '%");
        } else {
            // search in specific fields
            sql.append("  ExtractValue(data, '/xml/").append(fieldName).append("') like '%");
        }
        sql.append(escapedSearchValue);
        sql.append("%'");
        sql.append(")");
        try (Connection connection = MySQLHelper.getInstance().getConnection()) {
            QueryRunner run = new QueryRunner();
            return run.query(connection, sql.toString(), MySQLHelper.resultSetToIntegerListHandler, recordGroupId);

        } catch (SQLException e) {
            log.error(e);
        }
        return Collections.emptyList();
    }

    public static void deleteAllNodes(Integer id) {
        String sql = "DELETE FROM archive_record_node WHERE archive_record_group_id = ?";
        try (Connection connection = MySQLHelper.getInstance().getConnection()) {
            QueryRunner run = new QueryRunner();
            run.execute(connection, sql, id);
        } catch (SQLException e1) {
            log.error(e1);
        }
    }

    public static void deleteRecordGroup(String recordGroupName) {

        try (Connection connection = MySQLHelper.getInstance().getConnection()) {
            QueryRunner run = new QueryRunner();
            run.execute(connection,
                    "delete from archive_record_node where archive_record_group_id in (select id from archive_record_group where title = ? )",
                    recordGroupName);
            run.execute(connection, "delete from archive_record_group where title = ?", recordGroupName);
        } catch (SQLException e1) {
            log.error(e1);
        }

    }

    public static String getMetadataValue(String metadataName, Integer recordGroupId, String nodeId) {
        StringBuilder sql = new StringBuilder();

        sql.append("select ExtractValue(data, '/xml/");
        sql.append(metadataName);
        sql.append("') from archive_record_node where archive_record_group_id= ? and uuid = ? ");
        try (Connection connection = MySQLHelper.getInstance().getConnection()) {
            QueryRunner run = new QueryRunner();
            return run.query(connection, sql.toString(), MySQLHelper.resultSetToStringHandler, recordGroupId, nodeId);

        } catch (SQLException e) {
            log.error(e);
        }
        return null;
    }

    public static IEadEntry findNodeById(String metadataName, String metadataValue) {
        StringBuilder sql = new StringBuilder();
        sql.append("select id from archive_record_node WHERE ExtractValue(data, '/xml/")
                .append(metadataName)
                .append("') = '")
                .append(metadataValue)
                .append("");
        try (Connection connection = MySQLHelper.getInstance().getConnection()) {
            QueryRunner run = new QueryRunner();
            return run.query(connection, sql.toString(), resultSetToNodeHandler);
        } catch (SQLException e) {
            log.error(e);
        }
        return null;
    }
}
