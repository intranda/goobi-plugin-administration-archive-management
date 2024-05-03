package org.goobi.interfaces;

public interface INodeType {


    public String getNodeName();
    public String getDocumentType();
    public String getIcon();
    public Integer getProcessTemplateId();

    public void setNodeName(String value);
    public void setDocumentType(String value);
    public void setIcon(String value);
    public void setProcessTemplateId(Integer value);

}
