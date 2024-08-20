package de.intranda.goobi.plugins.model;

import java.util.ArrayList;
import java.util.List;

import org.goobi.interfaces.IMetadataField;
import org.goobi.interfaces.IMetadataGroup;

import lombok.EqualsAndHashCode.Exclude;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class EadMetadataGroup implements IMetadataGroup {

    @Exclude
    @lombok.ToString.Exclude
    private IMetadataField field;

    private List<IMetadataField> fields = new ArrayList<>();

    public EadMetadataGroup(IMetadataField field) {
        this.field = field;
    }

}
