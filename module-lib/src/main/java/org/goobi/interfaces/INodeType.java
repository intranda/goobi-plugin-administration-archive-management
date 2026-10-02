package org.goobi.interfaces;

import java.util.List;

public interface INodeType {

    public String getNodeName();

    public String getDocumentType();

    public String getIcon();

    public Integer getProcessTemplateId();

    public void setNodeName(String value);

    public void setDocumentType(String value);

    public void setIcon(String value);

    public void setProcessTemplateId(Integer value);

    public boolean isRootNode();

    public void setRootNode(boolean rootNode);

    public boolean isAllowProcessCreation();

    public void setAllowProcessCreation(boolean allowProcessCreation);

    /**
     * Check if a process for a node of this type is created for its parent node instead. The metadata of the node is then imported as sub element
     * into the process of the parent node.
     *
     * @return true if the process is created for the parent node
     */
    public boolean isCreateProcessForParent();

    public void setCreateProcessForParent(boolean createProcessForParent);

    public List<String> getAllowedChildren();

    public void setAllowedChildren(List<String> list);

    public boolean isNodeAllowed(INodeType other);
}
