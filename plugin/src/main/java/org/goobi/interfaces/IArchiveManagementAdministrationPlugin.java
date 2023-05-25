package org.goobi.interfaces;

import java.util.List;

import org.goobi.production.plugin.interfaces.IAdministrationPlugin;

public interface IArchiveManagementAdministrationPlugin extends IAdministrationPlugin {

    @Override
    public String getTitle();

    public String getDisplayMode();

    public void setDisplayMode(String mode);

    public List<String> getPossibleDatabases();

    public void setSelectedDatabase(String db);

    public void loadSelectedDatabase();

    public IEadEntry getRootElement();


    // used to create a new database
    public void setFileName(String fileName);
    public void setDatabaseName(String databaseName);
    public void createNewDatabase();
}
