package de.intranda.goobi.plugins;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.goobi.production.enums.PluginType;
import org.goobi.production.plugin.interfaces.IAdministrationPlugin;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.Namespace;
import org.jdom2.input.SAXBuilder;
import org.jdom2.input.sax.XMLReaders;

import de.sub.goobi.helper.HttpClientHelper;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.log4j.Log4j2;
import net.xeoh.plugins.base.annotations.PluginImplementation;

@PluginImplementation
@Log4j2
public class TektonikAdministrationPlugin implements IAdministrationPlugin {

    @Getter
    private String title = "intranda_administration_tektonik";

    @Getter
    private String value;

    @Getter
    private PluginType type = PluginType.Administration;

    @Getter
    private String gui = "/uii/plugin_administration_tektonik.xhtml";

    @Getter
    @Setter
    private String datastoreUrl ="http://localhost:8984/"; // TODO get this from config

    private static final Namespace defaultNamespace = Namespace.getNamespace("urn:isbn:1-931666-22-9");

    /**
     * Constructor
     */
    public TektonikAdministrationPlugin() {
    }



    @Getter
    @Setter
    private String selectedDatabase;

    /**
     * 
     * @return
     */

    public List<String> getPossibleDatabases() {
        List<String> databases = new ArrayList<>();
        String response = HttpClientHelper.getStringFromUrl(datastoreUrl + "databases");
        if (StringUtils.isNotBlank(response)) {

            SAXBuilder builder = new SAXBuilder(XMLReaders.NONVALIDATING);
            builder.setFeature("http://xml.org/sax/features/validation", false);
            builder.setFeature("http://apache.org/xml/features/nonvalidating/load-dtd-grammar", false);
            builder.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);

            try {
                Document document = builder.build(new StringReader(response), "utf-8");
                Element root = document.getRootElement();
                List<Element> databaseList = root.getChildren("database", defaultNamespace);
                for (Element db : databaseList) {
                    databases.add(db.getText());
                }
            } catch (JDOMException | IOException e) {
                log.error(e);
            }
        }
        return databases;
    }

    public String loadSelectedDatabase() {

        System.out.println(selectedDatabase);
        return "plugin_administration_tektonik2";
    }
}
