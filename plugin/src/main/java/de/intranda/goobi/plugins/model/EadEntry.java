package de.intranda.goobi.plugins.model;

import java.util.ArrayList;
import java.util.List;

import lombok.Data;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@Data
@RequiredArgsConstructor
public class EadEntry {

    // list contains all child elements
    private List<EadEntry> subEntryList = new ArrayList<>();

    // order number of the current element
    @NonNull
    private Integer orderNumber;

    // display label
    private String label;
    // node is open/closed
    private boolean displayChildren;
    // node is selected
    private boolean selectedNode;

    // node type -  @level
    private String nodeType;

    /* 1. metadata for Identity Statement Area */
    //    Reference code(s)
    private String agencycode; // /ead/control/maintenanceagency/agencycode
    private String eadid; //  /ead/eadheader/eadid
    private String recordid; // /ead/control/recordid
    private String unitid; // did/unitid
    //    Title
    private String unittitle; // did/unittitle
    //    Date(s)
    private String unitdate; // did/unitdate
    private String unitdatestructured; //  did/unitdatestructured
    //    Level of description
    private String descriptionLevel; // combine parent descriptionLevel + c/@level (or archdesc/@level on root element)
    //    Extent and medium of the unit of description (quantity, bulk, or size)
    private String physdesc; // did/physdesc
    private String physdescstructured; // did/physdescstructured

    /* 2. Context Area */
    //    Name of creator(s)
    private String origination; // /did/origination
    //    Administrative | Biographical history
    private String bioghist; // dsc/bioghist
    //    Archival history
    private String custodhist; // dsc/custodhist
    //    Immediate source of acquisition or transfer
    private String acqinfo; // dsc/acqinfo

    /* 3. Content and Structure Area*/
    //    Scope and content
    private String scopecontent; // dsc/scopecontent
    //    Appraisal, destruction and scheduling information
    private String appraisal; // dsc/appraisal
    //    Accruals
    private String accruals; // dsc/accruals
    //    System of arrangement
    private String arrangement; // dsc/arrangement

    /* 4. Condition of Access and Use Area */
    //    Conditions governing access
    private String accessrestrict; // dsc/accessrestrict
    //    Conditions governing reproduction
    private String userestrict; // dsc/userestrict
    //    Language | Scripts of material
    private String langmaterial; // did/langmaterial
    //    Physical characteristics and technical requirements
    private String phystech; // dsc/phystech
    //    Finding aids
    private String otherfindaid; // dsc/otherfindaid

    /* 5. Allied Materials Area */
    //    Existence and location of originals
    private String originalsloc; // dsc/originalsloc
    //    Existence and location of copies
    private String altformavail; // dsc/altformavail
    //    Related units of description
    private String relatedmaterial; // dsc/relatedmaterial
    private String separatedmaterial; // dsc/separatedmaterial
    //    Publication note
    private String bibliography; // dsc/bibliography

    /* 6. Note Area */
    //    Note
    private String didnote; // did/didnote
    private String odd; //  dsc/odd
    /* 7. Description Control Area */
    //    Archivist's Note
    private String processinfo; // dsc/processinfo
    //    Rules or Conventions
    private String conventiondeclaration; // control/conventiondeclaration (only on root level)
    //    Date(s) of descriptions
    private String maintenanceevent; // control/maintenancehistory/maintenanceevent/eventtype (only on root level)
    private String eventdatetime; //  control/maintenancehistory/maintenanceevent/eventdatetime (only on root level)

    public void addSubEntry(EadEntry other) {
        subEntryList.add(other);
    }

    public void removeSubEntry(EadEntry other) {
        subEntryList.remove(other);

    }

    public void reOrderElements() {
        int order = 1;
        for (EadEntry entry : subEntryList) {
            entry.setOrderNumber(order++);
        }
    }

}
