package de.intranda.goobi.plugins.model;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.goobi.interfaces.IEadEntry;
import org.goobi.interfaces.IMetadataField;
import org.goobi.interfaces.INodeType;

import lombok.Data;

@Data
public class EadEntry implements IEadEntry {

    // parent node
    private IEadEntry parentNode;

    // list contains all child elements
    private List<IEadEntry> subEntryList = new ArrayList<>();

    // order number of the current element within the current hierarchy
    private Integer orderNumber;

    // hierarchy level
    private Integer hierarchy;

    private String id; // c@id

    // display label
    private String label;
    // node is open/closed
    private boolean displayChildren;
    // node is selected
    private boolean selected;

    // node can be selected as destination when moving nodes
    private boolean selectable;

    // node type -  @level
    private INodeType nodeType;

    // display node in a search result
    private boolean displaySearch;
    private boolean searchFound;

    /* 1. metadata for Identity Statement Area */
    //    Reference code(s)
    //    Title
    //    private String unittitle; // did/unittitle
    //    Date(s)
    //    Level of description
    //    Extent and medium of the unit of description (quantity, bulk, or size)
    private List<IMetadataField> identityStatementAreaList = new ArrayList<>();

    /* 2. Context Area */
    //    Name of creator(s)
    //    Administrative | Biographical history
    //    Archival history
    //    Immediate source of acquisition or transfer
    private List<IMetadataField> contextAreaList = new ArrayList<>();

    /* 3. Content and Structure Area */
    //    Scope and content
    //    Appraisal, destruction and scheduling information
    //    Accruals
    //    System of arrangement
    private List<IMetadataField> contentAndStructureAreaAreaList = new ArrayList<>();

    /* 4. Condition of Access and Use Area */
    //    Conditions governing access
    //    Conditions governing reproduction
    //    Language | Scripts of material
    //    Physical characteristics and technical requirements
    //    Finding aids
    private List<IMetadataField> accessAndUseAreaList = new ArrayList<>();

    /* 5. Allied Materials Area */
    //    Existence and location of originals
    //    Existence and location of copies
    //    Related units of description
    //    Publication note
    private List<IMetadataField> alliedMaterialsAreaList = new ArrayList<>();

    /* 6. Note Area */
    //    Note
    private List<IMetadataField> notesAreaList = new ArrayList<>();

    /* 7. Description Control Area */
    //    Archivist's Note
    //    Rules or Conventions
    //    Date(s) of descriptions
    private List<IMetadataField> descriptionControlAreaList = new ArrayList<>();

    // empty if no process was created, otherwise the name of othe process is stored
    private String goobiProcessTitle;

    // true if the validation of all metadata fields was successful
    private boolean valid = true;

    public EadEntry(Integer order, Integer hierarchy) {
        this.orderNumber = order;
        this.hierarchy = hierarchy;
    }

    @Override
    public void addSubEntry(IEadEntry other) {
        subEntryList.add(other);
        other.setParentNode(this);
    }

    @Override
    public void removeSubEntry(IEadEntry other) {
        subEntryList.remove(other);
        reOrderElements();
    }

    @Override
    public void reOrderElements() {
        int order = 0;
        for (IEadEntry entry : subEntryList) {
            entry.setOrderNumber(order++);
        }
    }

    @Override
    public List<IEadEntry> getAsFlatList() {
        List<IEadEntry> list = new LinkedList<>();
        list.add(this);
        if (displayChildren) {
            if (subEntryList != null) {
                for (IEadEntry ds : subEntryList) {
                    list.addAll(ds.getAsFlatList());
                }
            }
        }
        return list;
    }

    @Override
    public boolean isHasChildren() {
        return !subEntryList.isEmpty();
    }

    @Override
    public List<IEadEntry> getMoveToDestinationList(IEadEntry entry) {
        List<IEadEntry> list = new LinkedList<>();
        list.add(this);

        if (entry.equals(this)) {
            setSelectable(false);
            parentNode.setSelectable(false);
        } else if (subEntryList != null) {
            for (IEadEntry ds : subEntryList) {
                list.addAll(ds.getMoveToDestinationList(entry));
            }
        }

        return list;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        EadEntry other = (EadEntry) obj;
        if (hierarchy == null) {
            if (other.hierarchy != null) {
                return false;
            }
        } else if (!hierarchy.equals(other.hierarchy)) {
            return false;
        }
        if (orderNumber == null) {
            if (other.orderNumber != null) {
                return false;
            }
        } else if (!orderNumber.equals(other.orderNumber)) {
            return false;
        }
        if (parentNode == null && other.parentNode == null) {
            return true;
        }
        if (parentNode == null && other.parentNode != null) {
            return false;
        }
        if (parentNode != null && other.parentNode == null) {
            return false;
        }

        if (!parentNode.getOrderNumber().equals(other.parentNode.getOrderNumber())) {
            return false;
        }
        if (!parentNode.getHierarchy().equals(other.parentNode.getHierarchy())) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((hierarchy == null) ? 0 : hierarchy.hashCode());
        result = prime * result + ((orderNumber == null) ? 0 : orderNumber.hashCode());
        result = prime * result + ((parentNode == null) ? 0 : parentNode.getHierarchy().hashCode());
        result = prime * result + ((parentNode == null) ? 0 : parentNode.getOrderNumber().hashCode());
        return result;
    }

    @Override
    public void updateHierarchy() {
        // root node
        if (parentNode == null) {
            hierarchy = 0;
        } else {
            hierarchy = parentNode.getHierarchy() + 1;
        }

        for (IEadEntry child : subEntryList) {
            child.updateHierarchy();
        }
    }

    @Override
    public void markAsFound() {
        displaySearch = true;
        //      selected = true;
        searchFound = true;

        if (parentNode != null) {
            IEadEntry node = parentNode;
            while (!node.isDisplaySearch()) {
                node.setDisplaySearch(true);
                if (node.getParentNode() != null) {
                    node = node.getParentNode();
                }
            }
        }
    }

    @Override
    public void resetFoundList() {
        displaySearch = false;
        //        selected = false;
        searchFound = false;
        if (subEntryList != null) {
            for (IEadEntry ds : subEntryList) {
                ds.resetFoundList();
            }
        }
    }

    @Override
    public List<IEadEntry> getSearchList() {
        List<IEadEntry> list = new LinkedList<>();
        if (displaySearch) {
            list.add(this);
            if (subEntryList != null) {
                for (IEadEntry child : subEntryList) {
                    list.addAll(child.getSearchList());
                }
            }
        }
        return list;
    }

    @Override
    public boolean isIdentityStatementAreaVisible() {
        for (IMetadataField emf : identityStatementAreaList) {
            if (emf.isVisible() && emf.isShowField()) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean isContextAreaVisible() {
        for (IMetadataField emf : contextAreaList) {
            if (emf.isVisible() && emf.isShowField()) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean isContentAndStructureAreaAreaVisible() {
        for (IMetadataField emf : contentAndStructureAreaAreaList) {
            if (emf.isVisible() && emf.isShowField()) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean isAccessAndUseAreaVisible() {
        for (IMetadataField emf : accessAndUseAreaList) {
            if (emf.isVisible() && emf.isShowField()) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean isAlliedMaterialsAreaVisible() {
        for (IMetadataField emf : alliedMaterialsAreaList) {
            if (emf.isVisible() && emf.isShowField()) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean isNotesAreaVisible() {
        for (IMetadataField emf : notesAreaList) {
            if (emf.isVisible() && emf.isShowField()) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean isDescriptionControlAreaVisible() {
        for (IMetadataField emf : descriptionControlAreaList) {
            if (emf.isVisible() && emf.isShowField()) {
                return true;
            }
        }
        return false;
    }
}
