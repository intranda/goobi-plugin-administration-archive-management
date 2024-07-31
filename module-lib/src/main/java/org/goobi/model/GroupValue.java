package org.goobi.model;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.goobi.interfaces.IValue;

import lombok.Data;

@Data
public class GroupValue implements IValue {

    private String groupName;
    private Map<String, List<IValue>> subfields = new HashMap<>();

}
