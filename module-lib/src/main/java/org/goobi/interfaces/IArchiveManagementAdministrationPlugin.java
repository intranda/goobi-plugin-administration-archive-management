package org.goobi.interfaces;

import java.util.List;

import org.goobi.production.plugin.interfaces.IAdministrationPlugin;
import org.jdom2.Document;

import de.intranda.goobi.plugins.model.ArchiveManagementConfiguration;

public interface IArchiveManagementAdministrationPlugin extends IAdministrationPlugin {

    @Override
    public String getTitle();

    public String getDisplayMode();

    public void setDisplayMode(String mode);

    public List<String> getPossibleDatabases();

    public void loadSelectedDatabase();

    public IEadEntry getRootElement();

    public void setDatabaseName(String databaseName);

    public String getDatabaseName();

    public void createNewDatabase();

    public Document createEadFile();

    public Document createEadFileForNodeAndAncestors(IEadEntry entry);

    // create new node as last child of the selected node
    public void setSelectedEntry(IEadEntry entry);

    public IEadEntry getSelectedEntry();

    public void addNode();

    public void updateSingleNode();

    public List<IRecordGroup> getRecordGroups();

    public IRecordGroup getRecordGroup();

    public void deleteNode();

    public String saveArchiveAndLeave();

    public String cancelEdition();

    public ArchiveManagementConfiguration getConfig();

}
