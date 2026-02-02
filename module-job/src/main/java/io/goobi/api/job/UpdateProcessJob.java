package io.goobi.api.job;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.XMLConfiguration;
import org.apache.commons.configuration.reloading.FileChangedReloadingStrategy;
import org.apache.commons.configuration.tree.xpath.XPathExpressionEngine;
import org.apache.commons.lang3.StringUtils;
import org.goobi.interfaces.IArchiveManagementAdministrationPlugin;
import org.goobi.interfaces.IEadEntry;
import org.goobi.interfaces.IMetadataField;
import org.goobi.interfaces.IValue;
import org.goobi.production.enums.PluginType;
import org.goobi.production.flow.jobs.AbstractGoobiJob;
import org.goobi.production.plugin.PluginLoader;
import org.goobi.production.plugin.interfaces.IPlugin;

import de.intranda.goobi.plugins.model.RecordGroup;
import de.intranda.goobi.plugins.persistence.ArchiveManagementManager;
import de.intranda.goobi.plugins.persistence.NodeInitializer;
import de.sub.goobi.config.ConfigurationHelper;
import lombok.extern.log4j.Log4j2;

@Log4j2
public class UpdateProcessJob extends AbstractGoobiJob {

    private List<String> databasesToUpdate;

    @Override
    public String getJobName() {
        return "intranda_quartz_updateProcessMetadata";
    }

    @Override
    public void execute() {
        parseConfiguration();

        if (databasesToUpdate.isEmpty()) {
            return;
        }

        IPlugin p = PluginLoader.getPluginByTitle(PluginType.Administration, "intranda_administration_archive_management");
        IArchiveManagementAdministrationPlugin archive = (IArchiveManagementAdministrationPlugin) p;

        // for each configured group
        for (String databaseName : databasesToUpdate) {

            // load db
            RecordGroup db = ArchiveManagementManager.getRecordGroupByTitle(databaseName);
            archive.getConfig().readConfiguration(databaseName);

            // load all nodes
            IEadEntry root = ArchiveManagementManager.loadRecordGroup(db.getId());

            List<IEadEntry> nodes = root.getAllNodes();
            // for each node, check, if node has a process assigned
            for (IEadEntry entry : nodes) {
                if (StringUtils.isNotBlank(entry.getGoobiProcessTitle())) {

                    // init metadata
                    Map<String, List<IValue>> metadata = ArchiveManagementManager.loadMetadataForNode(entry.getDatabaseId());

                    for (IMetadataField emf : archive.getConfig().getConfiguredFields()) {
                        if (emf.isGroup()) {
                            List<IValue> groups = metadata.get(emf.getName());
                            NodeInitializer.loadGroupMetadata(entry, emf, groups);
                        } else {
                            List<IValue> values = metadata.get(emf.getName());
                            IMetadataField toAdd = NodeInitializer.addFieldToEntry(entry, emf, values);
                            NodeInitializer.addFieldToNode(entry, toAdd);
                        }
                    }
                    // copy metadata to process
                    entry.updateProcessWithNodeMetadata();
                }
            }
        }
    }

    private void parseConfiguration() {
        // open configuration
        try {
            XMLConfiguration xmlConfig = new XMLConfiguration(
                    ConfigurationHelper.getInstance().getConfigurationFolder() + "plugin_intranda_administration_archive_management.xml");
            xmlConfig.setListDelimiter('&');
            xmlConfig.setReloadingStrategy(new FileChangedReloadingStrategy());
            xmlConfig.setExpressionEngine(new XPathExpressionEngine());

            databasesToUpdate = new ArrayList<>();
            databasesToUpdate = Arrays.asList(xmlConfig.getStringArray("/updateProcessMetadataJob/file"));

        } catch (ConfigurationException e2) {
            log.error(e2);
        }
    }
}
