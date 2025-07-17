package org.goobi.model;

import org.goobi.interfaces.IValue;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CorporateValue implements IValue {

    // field name
    private String name;
    // Preferred name
    private String mainValue;
    // Subordinate unit
    private String subValue;
    // Count, Number
    private String partValue;
    private String authorityType;
    private String authorityValue;

}
