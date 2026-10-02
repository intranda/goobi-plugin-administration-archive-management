package org.goobi.interfaces;

import java.util.List;

import org.goobi.beans.Process;

import ugh.dl.Fileformat;

public interface IEadEntry extends Comparable<IEadEntry> {

    public IEadEntry getParentNode();

    public List<IEadEntry> getSubEntryList();

    public Integer getOrderNumber();

    public Integer getHierarchy();

    public String getId();

    public String getLabel();

    public boolean isDisplayChildren();

    public boolean isSelected();

    public boolean isSelectable();

    public INodeType getNodeType();

    public boolean isDisplaySearch();

    public boolean isSearchFound();

    public List<IMetadataField> getIdentityStatementAreaList();

    public List<IMetadataField> getContextAreaList();

    public List<IMetadataField> getContentAndStructureAreaAreaList();

    public List<IMetadataField> getAccessAndUseAreaList();

    public List<IMetadataField> getAlliedMaterialsAreaList();

    public List<IMetadataField> getNotesAreaList();

    public List<IMetadataField> getDescriptionControlAreaList();

    public String getGoobiProcessTitle();

    public boolean isValid();

    public void setParentNode(IEadEntry value);

    public void setSubEntryList(List<IEadEntry> values);

    public void setOrderNumber(Integer value);

    public void setHierarchy(Integer value);

    public void setId(String value);

    public void setLabel(String value);

    public void setDisplayChildren(boolean value);

    public void setSelected(boolean value);

    public void setSelectable(boolean value);

    public void setNodeType(INodeType value);

    public void setDisplaySearch(boolean value);

    public void setSearchFound(boolean value);

    public void setIdentityStatementAreaList(List<IMetadataField> values);

    public void setContextAreaList(List<IMetadataField> values);

    public void setContentAndStructureAreaAreaList(List<IMetadataField> values);

    public void setAccessAndUseAreaList(List<IMetadataField> values);

    public void setAlliedMaterialsAreaList(List<IMetadataField> values);

    public void setNotesAreaList(List<IMetadataField> values);

    public void setDescriptionControlAreaList(List<IMetadataField> values);

    public void setGoobiProcessTitle(String value);

    public void setValid(boolean value);

    public void addSubEntry(IEadEntry other);

    public void removeSubEntry(IEadEntry other);

    public void reOrderElements();

    public void sortElements();

    public List<IEadEntry> getAsFlatList();

    public boolean isHasChildren();

    public List<IEadEntry> getMoveToDestinationList(IEadEntry entry);

    public void updateHierarchy();

    public void markAsFound();

    public void resetFoundList();

    public List<IEadEntry> getSearchList();

    public boolean isIdentityStatementAreaVisible();

    public boolean isContextAreaVisible();

    public boolean isContentAndStructureAreaAreaVisible();

    public boolean isAccessAndUseAreaVisible();

    public boolean isAlliedMaterialsAreaVisible();

    public boolean isNotesAreaVisible();

    public boolean isDescriptionControlAreaVisible();

    public IEadEntry deepCopy(IConfiguration configuration);

    public IEadEntry copyWithAncestors();

    public List<IEadEntry> getAllNodes();

    public String getSequence();

    public void setSequence(String sequenceNumber);

    public Integer getDatabaseId();

    public void setDatabaseId(Integer id);

    public String getDataAsXml();

    public String getData();

    public void setData(String data);

    public String getFingerprint();

    public void calculateFingerprint();

    public boolean isChildrenHaveProcesses();

    public IMetadataField getFieldByName(String fieldName);

    public boolean isMetadataLoaded();

    public void updateNodeWithProcessMetadata();

    public void updateProcessWithNodeMetadata();

    public Fileformat createFileformat(Process processTemplate);

    /**
     * Check if this node is imported as sub element into the process of its parent node. This is the case if the node type is configured with
     * {@code createProcessForParent} and the node is linked to the same process as its parent node.
     *
     * @return true if the metadata of this node is stored in a sub element of the process of the parent node
     */
    public boolean isPartOfParentProcess();

    /**
     * Get the child nodes that are imported as sub elements into a process created for this node.
     *
     * @return list of all direct children whose node type is configured with {@code createProcessForParent}
     */
    public List<IEadEntry> getChildrenForProcessCreation();

}