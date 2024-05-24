package org.goobi.model;

import org.goobi.interfaces.IValue;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ExtendendValue implements IValue {

    private String name;
    private String value;
    private String authorityType;
    private String authorityValue;

}
