package io.goobi.api.job;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.XMLConfiguration;
import org.apache.commons.configuration.reloading.FileChangedReloadingStrategy;
import org.apache.commons.configuration.tree.xpath.XPathExpressionEngine;
import org.apache.commons.lang3.StringUtils;
import org.goobi.interfaces.IArchiveManagementAdministrationPlugin;
import org.goobi.interfaces.IRecordGroup;
import org.goobi.io.BackupFileManager;
import org.goobi.production.enums.PluginType;
import org.goobi.production.flow.jobs.AbstractGoobiJob;
import org.goobi.production.plugin.PluginLoader;
import org.goobi.production.plugin.interfaces.IPlugin;

import de.sub.goobi.config.ConfigurationHelper;
import de.sub.goobi.helper.ShellScript;
import de.sub.goobi.persistence.managers.MySQLHelper;
import lombok.extern.log4j.Log4j2;

@Log4j2
public class BackupEadFileJob extends AbstractGoobiJob {

    private String backupFolder;
    private String command;
    private String password;
    private int numberOfFiles;

    @Override
    public String getJobName() {
        return "intranda_quartz_backupEadFile";
    }

    @Override
    public void execute() {
        parseConfiguration();

        if (!backupFolder.endsWith("/")) {
            backupFolder = backupFolder + "/";
        }

        // load all archive_record_groups
        IPlugin p = PluginLoader.getPluginByTitle(PluginType.Administration, "intranda_administration_archive_management");
        IArchiveManagementAdministrationPlugin archive = (IArchiveManagementAdministrationPlugin) p;
        List<IRecordGroup> recordGroups = archive.getRecordGroups();
        String databaseName = null;
        String username = null;
        // get database name, authentication
        try (Connection connection = MySQLHelper.getInstance().getConnection()) {
            databaseName = connection.getCatalog();
            DatabaseMetaData mtdt = connection.getMetaData();
            username = mtdt.getUserName().replaceAll("(.*)@.*", "$1");
        } catch (SQLException e) {
            log.error(e);
        }

        try {
            ShellScript script = new ShellScript(Paths.get("/bin/sh"));

            for (IRecordGroup rec : recordGroups) {
                String filename = rec.getTitle() + ".sql";
                Path backupFile = Paths.get(backupFolder, filename);
                // create backup

                BackupFileManager.createBackup(backupFolder, backupFolder, filename, numberOfFiles, false);

                StringBuilder recordGroupDump = new StringBuilder(command).append(" ").append(databaseName);
                StringBuilder recordsDump = new StringBuilder(command).append(" ").append(databaseName);

                if (StringUtils.isNotBlank(password)) {
                    recordGroupDump.append(" -u ").append(username);
                    recordGroupDump.append(" -p").append(password);

                    recordsDump.append(" -u ").append(username);
                    recordsDump.append(" -p").append(password);
                }
                recordGroupDump.append(" ")
                        .append(" --no-create-info")
                        .append(" --replace")
                        .append(" --tables archive_record_group")
                        .append(" --where='id=")
                        .append(rec.getId())
                        .append("' > ")
                        .append(backupFile.toString());

                recordsDump.append(" ")
                        .append(" --no-create-info")
                        .append(" --replace")
                        .append(" --tables archive_record_node")
                        .append(" --where='archive_record_group_id=")
                        .append(rec.getId())
                        .append("' >> ")
                        .append(backupFile.toString());

                List<String> params = new ArrayList<>();
                params.add("-c");
                params.add(recordGroupDump.toString());
                script.run(params);

                params = new ArrayList<>();
                params.add("-c");
                params.add(recordsDump.toString());
                script.run(params);
            }
        } catch (IOException | InterruptedException e) {
            log.error(e);
        }

    }

    /**
     * Parse the configuration file
     * 
     */

    public void parseConfiguration() {
        // open configuration
        try {
            XMLConfiguration xmlConfig = new XMLConfiguration(
                    ConfigurationHelper.getInstance().getConfigurationFolder() + "plugin_intranda_administration_archive_management.xml");
            xmlConfig.setListDelimiter('&');
            xmlConfig.setReloadingStrategy(new FileChangedReloadingStrategy());
            xmlConfig.setExpressionEngine(new XPathExpressionEngine());

            // get folder from configuration file
            backupFolder = xmlConfig.getString("/backup/folder", "/tmp/");
            numberOfFiles = xmlConfig.getInt("/backup/numberOfFiles");
            command = xmlConfig.getString("/backup/tool", "/usr/bin/mysqldump");
            password = xmlConfig.getString("/backup/password");
            // check which files shall be exported

        } catch (ConfigurationException e2) {
            log.error(e2);
        }
    }
}
