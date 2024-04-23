package de.intranda.goobi.plugins.persistence;

import java.io.Serializable;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

import org.apache.commons.dbutils.QueryRunner;
import org.goobi.interfaces.IEadEntry;

import de.intranda.goobi.plugins.model.RecordGroup;
import de.sub.goobi.persistence.managers.DatabaseVersion;
import de.sub.goobi.persistence.managers.MySQLHelper;
import lombok.extern.log4j.Log4j2;

@Log4j2
public class ArchiveManagementManager implements Serializable {

    private static final long serialVersionUID = 2861873896714636026L;

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
        // sql.append("KEY archive_record_group_id (archive_record_group_id), ");
        // sql.append("KEY sequence (sequence) ");
        //  sql.append("KEY parent_id (parent_id) ");
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
            if (grp.getId() == null) {
                // insert as new
                String sql = "INSERT INTO archive_record_group (title) VALUES (?)";
                Integer id = run.insert(connection, sql, MySQLHelper.resultSetToIntegerHandler, grp.getTitle());
                if (id != null) {
                    grp.setId(id);
                }
            } else {
                // update existing entry
                String sql = "UPDATE archive_record_group SET title = ? WHERE id = ?";
                run.update(connection, sql, grp.getTitle(), grp.getId());
            }
        } catch (SQLException e) {
            log.error(e);
        }
    }

    public static void saveNodes(Integer archiveId, List<IEadEntry> nodes) {
        String insertSql =
                "INSERT INTO archive_record_node (id, uuid, archive_record_group_id, hierarchy, order_number, node_type, sequence, processtitle, parent_id,label, data) VALUES ";

        // lock table
        try {
            DatabaseVersion.runSql("LOCK TABLE archive_record_node WRITE");
        } catch (SQLException e) {
            log.error(e);
        }

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
                if (node.getLabel() != null && node.getLabel().endsWith("\\")) {
                    node.setLabel(node.getLabel() + "\\");
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

            String label = entry.getLabel();
            if (label != null && label.endsWith("\\")) {
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
        System.out.println("Update duration: " + (System.currentTimeMillis() - start));
        // unlock table
        try {
            DatabaseVersion.runSql("UNLOCK TABLE");
        } catch (SQLException e) {
            log.error(e);
        }
    }

    public void loadRecordGroup() {
        String query = "SELECT * FROM archive_record_node WHERE archive_record_group_id = ? ORDER BY ";
    }

}
