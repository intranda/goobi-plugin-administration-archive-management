package de.intranda.goobi.plugins.model;

import java.util.List;

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

    private boolean rootNode;

    private boolean allowProcessCreation;

    private List<String> allowedChildren;

    @Override
    public boolean isNodeAllowed(INodeType other) {
        if (allowedChildren == null | allowedChildren.isEmpty()) {
            return false;
        }

        return allowedChildren.contains(other.getNodeName());

    }

}
