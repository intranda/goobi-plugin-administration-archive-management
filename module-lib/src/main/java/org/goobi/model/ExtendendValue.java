package org.goobi.model;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ExtendendValue {

    private String value;
    private String authorityType;
    private String authorityValue;

}
