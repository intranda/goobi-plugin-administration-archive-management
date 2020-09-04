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

import de.intranda.goobi.plugins.model.EadEntry;
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
    private String datastoreUrl = "http://localhost:8984/"; // TODO get this from config

    @Getter
    private List<EadEntry> entryList = new ArrayList<>();

    private static final Namespace ns = Namespace.getNamespace("urn:isbn:1-931666-22-9");

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

            Document document = openDocument(response);
            if (document != null) {
                Element root = document.getRootElement();
                List<Element> databaseList = root.getChildren("database", ns);
                for (Element db : databaseList) {
                    databases.add(db.getText());
                }
            }
        }
        return databases;
    }

    public void loadSelectedDatabase() {
        // open selected database

        if (StringUtils.isNotBlank(selectedDatabase)) {
            String response = HttpClientHelper.getStringFromUrl(datastoreUrl + "db/" + selectedDatabase);
            // get xml root element
            Document document = openDocument(response);
            if (document != null) {
                // parse ead file
                parseEadFile(document);
            }
        }
    }

    private void parseEadFile(Document document) {
        List<Element> eadElements = document.getRootElement().getChildren();
        entryList = new ArrayList<>(eadElements.size());
        int order = 1;
        for (Element ead : eadElements) {
            EadEntry rootEntry = parseElement(order, ead);
            entryList.add(rootEntry);
            order++;

        }
    }

    private EadEntry parseElement(int order, Element element) {
        EadEntry entry = new EadEntry(order);
        entry.setDisplayChildren(true);

        Element eadheader = element.getChild("eadheader", ns);
        Element control = element.getChild("control", ns);
        Element archdesc = element.getChild("archdesc", ns);


        if (eadheader != null) {
            entry.setEadid(eadheader.getChildText("eadid", ns));
            entry.setLabel(eadheader.getChild("filedesc", ns).getChild("titlestmt", ns).getChildText("titleproper", ns));
        }
        if (control != null) {
            entry.setRecordid(control.getChildText("recordid", ns));
            Element maintenanceagency = control.getChild("maintenanceagency", ns);
            if (maintenanceagency != null) {
                entry.setAgencycode(maintenanceagency.getChildText("agencycode", ns));
            }
            entry.setConventiondeclaration(control.getChildText("conventiondeclaration", ns));
            Element maintenancehistory = control.getChild("maintenancehistory", ns);
            if (maintenancehistory != null) {
                Element maintenanceevent = maintenancehistory.getChild("maintenanceevent", ns);
                entry.setMaintenanceevent(maintenanceevent.getChildText("eventtype", ns));
                entry.setEventdatetime(maintenanceevent.getChildText("eventdatetime", ns));
            }
        } if (archdesc != null) {
            entry.setDescriptionLevel(archdesc.getAttributeValue("level"));

            Element did = archdesc.getChild("did", ns);
            if (did != null) {

                entry.setUnitid(did.getChildText("unitid", ns));
                entry.setUnittitle(did.getChildText("unittitle", ns));
                entry.setUnitdate(did.getChildText("unitdate", ns));
                entry.setUnitdatestructured(did.getChildText("unitdatestructured", ns));
                entry.setPhysdesc(did.getChildText("physdesc", ns));
                entry.setPhysdescstructured(did.getChildText("physdescstructured", ns));
                entry.setOrigination(did.getChildText("origination", ns));
                entry.setLangmaterial(did.getChildText("langmaterial", ns));
                entry.setDidnote(did.getChildText("didnote", ns));
            }



            Element dsc= archdesc.getChild("dsc", ns);
            if (dsc != null) {
                entry.setBioghist(dsc.getChildText("bioghist", ns));
                entry.setCustodhist(dsc.getChildText("custodhist", ns));
                entry.setAcqinfo(dsc.getChildText("acqinfo", ns));
                entry.setScopecontent(dsc.getChildText("scopecontent", ns));
                entry.setAppraisal(dsc.getChildText("appraisal", ns));
                entry.setAccruals(dsc.getChildText("accruals", ns));
                entry.setArrangement(dsc.getChildText("arrangement", ns));
                entry.setAccessrestrict(dsc.getChildText("accessrestrict", ns));
                entry.setUserestrict(dsc.getChildText("userestrict", ns));
                entry.setPhystech(dsc.getChildText("phystech", ns));
                entry.setOtherfindaid(dsc.getChildText("otherfindaid", ns));
                entry.setOriginalsloc(dsc.getChildText("originalsloc", ns));
                entry.setAltformavail(dsc.getChildText("altformavail", ns));
                entry.setRelatedmaterial(dsc.getChildText("relatedmaterial", ns));
                entry.setSeparatedmaterial(dsc.getChildText("separatedmaterial", ns));
                entry.setBibliography(dsc.getChildText("bibliography", ns));
                entry.setOdd(dsc.getChildText("odd", ns));
                entry.setProcessinfo(dsc.getChildText("processinfo", ns));

                List<Element> clist = dsc.getChildren("c", ns);
                if (clist != null) {
                    int subOrder = 1;
                    for (Element c : clist) {
                        EadEntry child = parseElement(subOrder, c);
                        entry.addSubEntry(child);
                        order++;
                    }
                }
            }
        }
        return entry;
    }

    private Document openDocument(String response) {
        SAXBuilder builder = new SAXBuilder(XMLReaders.NONVALIDATING);
        builder.setFeature("http://xml.org/sax/features/validation", false);
        builder.setFeature("http://apache.org/xml/features/nonvalidating/load-dtd-grammar", false);
        builder.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);

        try {
            Document document = builder.build(new StringReader(response), "utf-8");
            return document;

        } catch (JDOMException | IOException e) {
            log.error(e);
        }
        return null;
    }
}
