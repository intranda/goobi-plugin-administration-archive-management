package de.intranda.goobi.plugins.model;

import lombok.Data;

@Data

public class EadMetadataField {

    private String name;

    private Integer level;

    private String xpath;

    private String xpathType;

    private boolean repeatable;

    private String value;

    public EadMetadataField(String name, Integer level, String xpath, String xpathType, boolean repeatable) {
        this.name = name;
        this.level = level;
        this.xpath = xpath;
        this.xpathType = xpathType;
        this.repeatable = repeatable;
    }

}
