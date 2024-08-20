package org.goobi.interfaces;

import java.util.List;

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

}