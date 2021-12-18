package org.goobi.interfaces;

import org.goobi.production.plugin.interfaces.IAdministrationPlugin;

public interface IArchiveManagementAdministrationPlugin extends IAdministrationPlugin {

    public String getTitle();
    
    public String getDisplayMode();

    public void setDisplayMode(String mode);
}
