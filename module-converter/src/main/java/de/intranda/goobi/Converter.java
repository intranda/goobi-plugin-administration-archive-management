package de.intranda.goobi;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.commons.configuration.XMLConfiguration;
import org.apache.commons.configuration.reloading.FileChangedReloadingStrategy;
import org.apache.commons.configuration.tree.xpath.XPathExpressionEngine;
import org.apache.commons.lang3.StringUtils;
import org.goobi.interfaces.IMetadataField;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.Namespace;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;

import de.intranda.goobi.plugins.ArchiveManagementAdministrationPlugin;
import de.sub.goobi.helper.XmlTools;
import lombok.extern.log4j.Log4j2;

@Log4j2
public class Converter {

    public static void main(String[] args) throws ConfigurationException {
        // check input
        // first parameter = old configuration file
        // second parameter = mapping
        // third parameter = ead file

        if (args.length < 2) {
            System.err.println("Missing parameter");
            System.exit(0);
        }
        String oldConfiguration = args[0];
        String mappingFile = args[1];
        String inputFile = args[2];
        String outputFile = inputFile.replace(".xml", "_converted.xml");

        ArchiveManagementAdministrationPlugin plugin = new ArchiveManagementAdministrationPlugin();

        // load old configuration file
        XMLConfiguration oldConfigurationFile = new XMLConfiguration(oldConfiguration);
        oldConfigurationFile.setListDelimiter('&');
        oldConfigurationFile.setReloadingStrategy(new FileChangedReloadingStrategy());
        oldConfigurationFile.setExpressionEngine(new XPathExpressionEngine());
        plugin.setXmlConfig(oldConfigurationFile);
        plugin.readConfiguration();

        // load mapping file
        XMLConfiguration config = new XMLConfiguration(mappingFile);

        Map<String, String> xpathMap = new HashMap<>();
        for (HierarchicalConfiguration hc : config.configurationsAt("/field")) {
            String name = hc.getString("/name");
            String xpath = hc.getString("/xpathNew");
            xpathMap.put(name, xpath);
        }

        // open document, parse it
        try (InputStream input = Files.newInputStream(Paths.get(inputFile))) {
            Document document = XmlTools.readDocumentFromStream(input);
            if (document != null) {

                Element mainElement = document.getRootElement();
                if ("collection".equals(mainElement.getName())) {
                    mainElement = mainElement.getChildren().get(0);
                }

                if (!"ead".equals(mainElement.getName())) {
                    // file is not an ead file
                    String message = "plugin_administration_archive_notEadFile";
                    throw new FileNotFoundException(message);
                }

                plugin.parseEadFile(document);

                // change namespace, if needed
                plugin.setNameSpaceWrite(Namespace.getNamespace("ead", config.getString("/newNamespace", "urn:isbn:1-931666-22-9")));

                // replace xpath in mappings
                for (IMetadataField emf : plugin.getConfiguredFields()) {
                    String xpath = xpathMap.get(emf.getName());
                    if (StringUtils.isNotBlank(xpath)) {
                        emf.setXpath(xpath);
                    }
                }
                // write new xml file with new mapping
                Document outpuDoc = new Document();

                Element eadRoot = new Element("ead", plugin.getNameSpaceWrite());
                outpuDoc.setRootElement(eadRoot);
                plugin.addMetadata(eadRoot, plugin.getRootElement(), eadRoot, false);

                XMLOutputter outputter = new XMLOutputter();
                outputter.setFormat(Format.getPrettyFormat());
                outputter.output(outpuDoc, Files.newOutputStream(Paths.get(outputFile)));

            }
        } catch (IOException e) {
            log.error(e);
        }

    }

}
