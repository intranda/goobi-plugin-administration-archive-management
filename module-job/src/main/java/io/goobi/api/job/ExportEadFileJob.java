package io.goobi.api.job;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.XMLConfiguration;
import org.apache.commons.configuration.reloading.FileChangedReloadingStrategy;
import org.apache.commons.configuration.tree.xpath.XPathExpressionEngine;
import org.goobi.interfaces.IArchiveManagementAdministrationPlugin;
import org.goobi.production.enums.PluginType;
import org.goobi.production.flow.jobs.AbstractGoobiJob;
import org.goobi.production.plugin.PluginLoader;
import org.goobi.production.plugin.interfaces.IPlugin;
import org.jdom2.Document;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;

import de.sub.goobi.config.ConfigurationHelper;
import lombok.extern.log4j.Log4j2;

@Log4j2
public class ExportEadFileJob extends AbstractGoobiJob {

    private String exportFolder;

    private List<String> filesToExport;

    /**
     * When called, this method gets executed
     * 
     * It will - download the latest json file from the configured sftp server - convert it into vocabulary records - save the new records
     * 
     */

    @Override
    public void execute() {
        parseConfiguration();
        // for each configured file

        log.debug("Export configured ead files");

        if (filesToExport != null && !filesToExport.isEmpty()) {
            // initialize plugin

            IPlugin p = PluginLoader.getPluginByTitle(PluginType.Administration, "intranda_administration_archive_management");
            IArchiveManagementAdministrationPlugin archive = (IArchiveManagementAdministrationPlugin) p;

            for (String file : filesToExport) {
                // load record from database
                archive.setDatabaseName(file);
                archive.loadSelectedDatabase();

                //  write document to servlet output stream
                if (!file.endsWith(".xml")) {
                    file = file + ".xml";
                }
                Path downloadFile = Paths.get(exportFolder, file.replace(" ", "_"));
                Document document = archive.createEadFile();
                XMLOutputter outputter = new XMLOutputter(Format.getPrettyFormat());
                try {
                    outputter.output(document, Files.newOutputStream(downloadFile));
                } catch (IOException e) {
                    log.error(e);
                }

            }
        }

        // generate ead xml

        // store it in configured folder

    }

    @Override
    public String getJobName() {
        return "intranda_quartz_exportEadFile";
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
            exportFolder = xmlConfig.getString("/export/folder");

            // check which files shall be exported
            filesToExport = Arrays.asList(xmlConfig.getStringArray("/export/file"));

        } catch (ConfigurationException e2) {
            log.error(e2);
        }
    }

}
