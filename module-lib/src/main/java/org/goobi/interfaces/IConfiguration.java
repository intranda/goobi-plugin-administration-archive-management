package org.goobi.interfaces;

import java.util.List;

public interface IConfiguration {

    public List<IParameter> getIdentityStatementArea();

    public List<IParameter> getContextArea();

    public List<IParameter> getContentArea();

    public List<IParameter> getAccessArea();

    public List<IParameter> getAlliedMaterialsArea();

    public List<IParameter> getNotesArea();

    public List<IParameter> getDescriptionArea();
}
