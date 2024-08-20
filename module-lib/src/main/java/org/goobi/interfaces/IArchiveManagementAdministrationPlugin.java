package org.goobi.interfaces;

import java.util.List;

import org.goobi.production.plugin.interfaces.IAdministrationPlugin;
import org.jdom2.Document;

public interface IArchiveManagementAdministrationPlugin extends IAdministrationPlugin {

    @Override
    public String getTitle();

    public String getDisplayMode();

    public void setDisplayMode(String mode);

    public List<String> getPossibleDatabases();

    public void loadSelectedDatabase();

    public IEadEntry getRootElement();

    public void setDatabaseName(String databaseName);

    public void createNewDatabase();

    public Document createEadFile();

    // create new node as last child of the selected node
    public void setSelectedEntry(IEadEntry entry);

    public IEadEntry getSelectedEntry();

    public void addNode();

    public List<INodeType> getConfiguredNodes();

    public List<IMetadataField> getConfiguredFields();

    List<IRecordGroup> getRecordGroups();
}
