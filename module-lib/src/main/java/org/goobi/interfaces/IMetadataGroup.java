package org.goobi.interfaces;

import java.util.List;

public interface IMetadataGroup {

    public IMetadataField getField();

    public List<IMetadataField> getFields();

    public void setFields(List<IMetadataField> fields);

}
