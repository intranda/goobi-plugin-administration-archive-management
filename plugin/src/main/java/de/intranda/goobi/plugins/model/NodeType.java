package de.intranda.goobi.plugins.model;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class NodeType {

    private String nodeName;
    private String documentType;
    private String icon;
}
