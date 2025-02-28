package de.intranda.goobi.plugins.model;

import org.goobi.interfaces.IRecordGroup;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RecordGroup implements IRecordGroup {

    private Integer id;
    private String title;
}
