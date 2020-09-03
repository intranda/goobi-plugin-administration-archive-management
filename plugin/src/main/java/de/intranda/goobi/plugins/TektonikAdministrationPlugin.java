package de.intranda.goobi.plugins;

import org.goobi.production.enums.PluginType;
import org.goobi.production.plugin.interfaces.IAdministrationPlugin;

import de.sub.goobi.config.ConfigPlugins;
import lombok.Getter;
import lombok.extern.log4j.Log4j2;
import net.xeoh.plugins.base.annotations.PluginImplementation;

@PluginImplementation
@Log4j2
public class TektonikAdministrationPlugin implements IAdministrationPlugin {

    @Getter
    private String title = "intranda_administration_tektonik";

    @Getter
    private String value;

    @Override
    public PluginType getType() {
        return PluginType.Administration;
    }

    @Override
    public String getGui() {
        return "/uii/plugin_administration_tektonik.xhtml";
    }

    /**
     * Constructor
     */
    public TektonikAdministrationPlugin() {
        log.info("Sample admnistration plugin started");
        value = ConfigPlugins.getPluginConfig(title).getString("value", "default value");
    }   
}
