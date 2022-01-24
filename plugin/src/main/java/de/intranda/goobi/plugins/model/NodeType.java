package de.intranda.goobi.plugins.model;

import org.goobi.interfaces.INodeType;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class NodeType implements INodeType {

    private String nodeName;
    private String documentType;
    private String icon;
    private Integer processTemplateId;
}
