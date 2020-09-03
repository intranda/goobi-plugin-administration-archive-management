package de.intranda.goobi.plugins.model;

import java.util.List;

import lombok.Getter;
import lombok.Setter;

public class Entry {
    @Getter
    private List<Entry> subEntryList;
    @Getter @Setter
    private Integer orderNumber;
    @Getter @Setter
    private String label;
    @Getter @Setter
    private boolean displayChildren;
    @Getter @Setter
    private boolean selectedNode;



    private String nodeType; //

    // list of metadata elements
    private List<MetadataField> fieldList;

    public void addSubEntry() {

    }

    public void removeSubEntry() {

    }

    public void reOrderElements() {

    }


}
