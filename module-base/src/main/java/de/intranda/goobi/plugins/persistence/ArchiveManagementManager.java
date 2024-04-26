package de.intranda.goobi.plugins.persistence;

import java.io.Serializable;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.dbutils.QueryRunner;
import org.apache.commons.dbutils.ResultSetHandler;
import org.goobi.interfaces.IEadEntry;
import org.goobi.interfaces.INodeType;

import de.intranda.goobi.plugins.model.EadEntry;
import de.intranda.goobi.plugins.model.RecordGroup;
import de.sub.goobi.persistence.managers.DatabaseVersion;
import de.sub.goobi.persistence.managers.MySQLHelper;
import lombok.extern.log4j.Log4j2;

@Log4j2
public class ArchiveManagementManager implements Serializable {

    private static final long serialVersionUID = 2861873896714636026L;

    private static List<INodeType> configuredNodes;

    private static Pattern pattern = Pattern.compile("<([^<]+?)>([^<]+)<\\/[^<]+?>");

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
        sql.append("data text ");
        // TODO use index for archive_record_group_id, sequence, parent_id

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

    public static void saveNodes(Integer archiveId, List<IEadEntry> nodes) {
        String insertSql =
                "INSERT INTO archive_record_node (id, uuid, archive_record_group_id, hierarchy, order_number, node_type, sequence, processtitle, parent_id,label, data) VALUES ";

        // lock table
        //        try {
        //            DatabaseVersion.runSql("LOCK TABLE archive_record_node WRITE");
        //        } catch (SQLException e) {
        //            log.error(e);
        //        }

        // get next free id
        String nextIdSql = "SELECT AUTO_INCREMENT FROM information_schema.TABLES WHERE TABLE_SCHEMA = 'goobi' AND TABLE_NAME = 'archive_record_node'";
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
        long start = System.currentTimeMillis();

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
            values.append(entry.getNodeType().getNodeName());
            values.append("', '");
            values.append(entry.getSequence());
            values.append("', '");
            values.append(entry.getGoobiProcessTitle());
            values.append("', ");
            values.append(parentId);
            values.append(", '");
            values.append(label);
            values.append("', '");
            values.append(entry.getDataAsXml());
            values.append("'");
            values.append(")");
            // save every 50 records or when we reached the last one
            if (i % 50 == 49 || i + 1 == nodes.size()) {
                StringBuilder sql = new StringBuilder(insertSql);
                sql.append(values.toString());
                sql.append("ON DUPLICATE KEY UPDATE hierarchy = VALUES(hierarchy), order_number = VALUES(order_number), "
                        + "node_type =  VALUES(node_type), sequence = VALUES(sequence), processtitle = VALUES(processtitle), "
                        + "processtitle = VALUES(processtitle), label = VALUES(label), data = VALUES(data)");
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
        System.out.println("Save duration: " + (System.currentTimeMillis() - start));
        // unlock table
        //        try {
        //            DatabaseVersion.runSql("UNLOCK TABLE");
        //        } catch (SQLException e) {
        //            log.error(e);
        //        }
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
                int parentId = rs.getInt("parent_id");
                String label = rs.getString("label");

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

                //  parse metadata
                //                String data = rs.getString("data");
                //                Map<String, List<String>> metadataMap = convertStringToMap(data);
                //                currentEntry.setMetadataMap(metadataMap);

                if (parentId == 0) {
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
                    // first element is skipped as it is the root element
                    // last element is ignored as it is the order of the current element
                    for (int i = 1; i < parts.length - 1; i++) {
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

    public static Map<String, List<String>> convertStringToMap(String data) {

        data = data.replace("<xml>", "").replace("</xml>", "");
        Map<String, List<String>> metadataMap = new HashMap<>();
        for (Matcher m = pattern.matcher(data); m.find();) {
            MatchResult mr = m.toMatchResult();
            String metadata = mr.group(1);
            String value = mr.group(2);
            List<String> values = metadataMap.getOrDefault(metadata, new ArrayList<>());
            values.add(value);
            metadataMap.put(metadata, values);
        }

        return metadataMap;
    }

}
