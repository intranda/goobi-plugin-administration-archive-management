package org.goobi.interfaces;

import java.io.Serializable;

public interface IProcessTemplate extends Serializable {

    public String getTemplateId();

    public String getTemplateName();

    public String getProjectId();

}
