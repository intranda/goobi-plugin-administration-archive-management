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

    public List<String> getAllowedChildren();

    public void setAllowedChildren(List<String> list);

    public boolean isNodeAllowed(INodeType other);
}
