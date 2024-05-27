package de.intranda.goobi.plugins.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

import org.apache.commons.lang3.StringUtils;
import org.goobi.interfaces.IConfiguration;
import org.goobi.interfaces.IEadEntry;
import org.goobi.interfaces.IFieldValue;
import org.goobi.interfaces.IMetadataField;
import org.goobi.interfaces.INodeType;
import org.goobi.interfaces.IParameter;

import de.sub.goobi.persistence.managers.MySQLHelper;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class EadEntry implements IEadEntry {

    // table auto increment key
    private Integer databaseId;

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
    private String data;

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
    public void sortElements() {
        Collections.sort(subEntryList);
    }

    @Override
    public List<IEadEntry> getAsFlatList() {
        List<IEadEntry> list = new LinkedList<>();
        list.add(this);
        if (displayChildren && subEntryList != null) {
            for (IEadEntry ds : subEntryList) {
                list.addAll(ds.getAsFlatList());
            }
        }
        return list;
    }

    @Override
    public List<IEadEntry> getAllNodes() {
        List<IEadEntry> list = new LinkedList<>();
        list.add(this);
        if (subEntryList != null) {
            for (IEadEntry ds : subEntryList) {
                list.addAll(ds.getAllNodes());
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

        if (this.equals(entry)) {
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
        if (parentNode != null && other.parentNode != null) {
            if (!parentNode.getOrderNumber().equals(other.parentNode.getOrderNumber())) {
                return false;
            }
            if (!parentNode.getHierarchy().equals(other.parentNode.getHierarchy())) {
                return false;
            }
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

    @Override
    public IEadEntry deepCopy(IConfiguration configuration) {
        IEadEntry other = new EadEntry(orderNumber, hierarchy);
        other.setNodeType(nodeType);
        other.setDisplayChildren(true);
        other.setLabel(label);
        other.setOrderNumber(orderNumber);

        // create new id
        other.setId(String.valueOf(UUID.randomUUID()));

        // copy all metadata
        for (IMetadataField field : identityStatementAreaList) {
            IMetadataField otherField = copyIdentityStatementField(configuration, other, field);
            other.getIdentityStatementAreaList().add(otherField);
        }

        for (IMetadataField field : contextAreaList) {
            IMetadataField otherField = copyContextField(configuration, other, field);
            otherField.setEadEntry(other);
            other.getContextAreaList().add(otherField);
        }
        for (IMetadataField field : contentAndStructureAreaAreaList) {
            IMetadataField otherField = copyContentField(configuration, other, field);
            otherField.setEadEntry(other);
            other.getContentAndStructureAreaAreaList().add(otherField);
        }

        for (IMetadataField field : accessAndUseAreaList) {
            IMetadataField otherField = copyAccessField(configuration, other, field);
            otherField.setEadEntry(other);
            other.getAccessAndUseAreaList().add(otherField);
        }

        for (IMetadataField field : alliedMaterialsAreaList) {
            IMetadataField otherField = copyAlliedMaterialsField(configuration, other, field);
            otherField.setEadEntry(other);
            other.getAlliedMaterialsAreaList().add(otherField);
        }

        for (IMetadataField field : notesAreaList) {
            IMetadataField otherField = copyNotesField(configuration, other, field);
            otherField.setEadEntry(other);
            other.getNotesAreaList().add(otherField);
        }

        for (IMetadataField field : descriptionControlAreaList) {
            IMetadataField otherField = copyDescriptionField(configuration, other, field);
            otherField.setEadEntry(other);
            other.getDescriptionControlAreaList().add(otherField);
        }

        // copy all children
        for (IEadEntry child : subEntryList) {
            other.addSubEntry(child.deepCopy(configuration));
        }

        return other;
    }

    private IMetadataField copyIdentityStatementField(IConfiguration configuration, IEadEntry other, IMetadataField field) {
        String prefix = "";
        String suffix = "";
        if (configuration != null) {
            for (IParameter param : configuration.getIdentityStatementArea()) {
                if (param.getFieldName().equals(field.getName())) {
                    prefix = param.getPrefix();
                    suffix = param.getSuffix();
                }
            }
        }
        IMetadataField otherField = field.copy(prefix, suffix);
        otherField.setEadEntry(other);
        return otherField;
    }

    private IMetadataField copyContextField(IConfiguration configuration, IEadEntry other, IMetadataField field) {
        String prefix = "";
        String suffix = "";
        if (configuration != null) {
            for (IParameter param : configuration.getContextArea()) {
                if (param.getFieldName().equals(field.getName())) {
                    prefix = param.getPrefix();
                    suffix = param.getSuffix();
                }
            }
        }
        IMetadataField otherField = field.copy(prefix, suffix);
        otherField.setEadEntry(other);
        return otherField;
    }

    private IMetadataField copyContentField(IConfiguration configuration, IEadEntry other, IMetadataField field) {
        String prefix = "";
        String suffix = "";
        if (configuration != null) {
            for (IParameter param : configuration.getContentArea()) {
                if (param.getFieldName().equals(field.getName())) {
                    prefix = param.getPrefix();
                    suffix = param.getSuffix();
                }
            }
        }
        IMetadataField otherField = field.copy(prefix, suffix);
        otherField.setEadEntry(other);
        return otherField;
    }

    private IMetadataField copyAccessField(IConfiguration configuration, IEadEntry other, IMetadataField field) {
        String prefix = "";
        String suffix = "";
        if (configuration != null) {
            for (IParameter param : configuration.getAccessArea()) {
                if (param.getFieldName().equals(field.getName())) {
                    prefix = param.getPrefix();
                    suffix = param.getSuffix();
                }
            }
        }
        IMetadataField otherField = field.copy(prefix, suffix);
        otherField.setEadEntry(other);
        return otherField;
    }

    private IMetadataField copyAlliedMaterialsField(IConfiguration configuration, IEadEntry other, IMetadataField field) {
        String prefix = "";
        String suffix = "";
        if (configuration != null) {
            for (IParameter param : configuration.getAlliedMaterialsArea()) {
                if (param.getFieldName().equals(field.getName())) {
                    prefix = param.getPrefix();
                    suffix = param.getSuffix();
                }
            }
        }
        IMetadataField otherField = field.copy(prefix, suffix);
        otherField.setEadEntry(other);
        return otherField;
    }

    private IMetadataField copyNotesField(IConfiguration configuration, IEadEntry other, IMetadataField field) {
        String prefix = "";
        String suffix = "";
        if (configuration != null) {
            for (IParameter param : configuration.getNotesArea()) {
                if (param.getFieldName().equals(field.getName())) {
                    prefix = param.getPrefix();
                    suffix = param.getSuffix();
                }
            }
        }
        IMetadataField otherField = field.copy(prefix, suffix);
        otherField.setEadEntry(other);
        return otherField;
    }

    private IMetadataField copyDescriptionField(IConfiguration configuration, IEadEntry other, IMetadataField field) {
        String prefix = "";
        String suffix = "";
        if (configuration != null) {
            for (IParameter param : configuration.getDescriptionArea()) {
                if (param.getFieldName().equals(field.getName())) {
                    prefix = param.getPrefix();
                    suffix = param.getSuffix();
                }
            }
        }
        IMetadataField otherField = field.copy(prefix, suffix);
        otherField.setEadEntry(other);
        return otherField;
    }

    // overwrite compare method to sort child list by its order number

    @Override
    public int compareTo(IEadEntry o) {
        return orderNumber.compareTo(o.getOrderNumber());
    }

    @Override
    public String getSequence() {
        String sequence;
        if (getParentNode() == null) {
            // root node,
            sequence = "";
        } else {
            String prefix = getParentNode().getSequence();
            if (StringUtils.isNotBlank(prefix)) {
                sequence = prefix + "." + (getParentNode().getOrderNumber());
            } else {
                sequence = String.valueOf(getParentNode().getOrderNumber());
            }
        }
        return sequence;
    }

    @Override
    public void setSequence(String sequenceNumber) {
        // do nothing
    }

    @Override
    public String getDataAsXml() {
        StringBuilder xml = new StringBuilder();
        xml.append("<xml>");
        for (IMetadataField field : identityStatementAreaList) {
            createXmlField(xml, field);
        }
        for (IMetadataField field : contextAreaList) {
            createXmlField(xml, field);
        }

        for (IMetadataField field : contentAndStructureAreaAreaList) {
            createXmlField(xml, field);
        }

        for (IMetadataField field : accessAndUseAreaList) {
            createXmlField(xml, field);
        }

        for (IMetadataField field : alliedMaterialsAreaList) {
            createXmlField(xml, field);
        }

        for (IMetadataField field : notesAreaList) {
            createXmlField(xml, field);
        }

        for (IMetadataField field : descriptionControlAreaList) {
            createXmlField(xml, field);
        }
        xml.append("</xml>");
        return xml.toString();
    }

    private void createXmlField(StringBuilder xml, IMetadataField field) {
        if (field.isGroup()) {
            xml.append("<group name=\"").append(field.getName()).append("\">");
            for (IMetadataField subfield : field.getSubfields()) {
                for (IFieldValue val : subfield.getValues()) {
                    if (StringUtils.isNotBlank(val.getValue())) {
                        xml.append("<field name=\"").append(subfield.getName()).append("\"");
                        if (StringUtils.isNotBlank(val.getAuthorityValue()) && StringUtils.isNotBlank(val.getAuthorityType())) {
                            xml.append(" source=\"")
                                    .append(val.getAuthorityType())
                                    .append("\" value=\"")
                                    .append(val.getAuthorityValue())
                                    .append("\"");
                        }
                        xml.append(">");
                        if (StringUtils.isNotBlank(val.getValue())) {
                            // mask ending backslash
                            String actualValue = val.getValue();
                            if (actualValue.endsWith("\\")) {
                                actualValue = val.getValue() + "\\";
                            }
                            xml.append(MySQLHelper.escapeSql(actualValue.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")));
                        }
                        xml.append("</field>");
                    }
                }
            }
            xml.append("</group>");
        } else {
            for (IFieldValue val : field.getValues()) {
                if (StringUtils.isNotBlank(val.getValue())) {
                    xml.append("<").append(field.getName());
                    // save authority data
                    if (StringUtils.isNotBlank(val.getAuthorityValue()) && StringUtils.isNotBlank(val.getAuthorityType())) {
                        xml.append(" source=\"").append(val.getAuthorityType()).append("\" value=\"").append(val.getAuthorityValue()).append("\"");
                    }
                    xml.append(">");
                    // mask ending backslash
                    String actualValue = val.getValue();
                    if (actualValue.endsWith("\\")) {
                        actualValue = val.getValue() + "\\";
                    }
                    xml.append(MySQLHelper.escapeSql(actualValue.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")));
                }
                xml.append("</").append(field.getName()).append(">");
            }
        }
    }
}
