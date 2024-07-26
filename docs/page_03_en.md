---
title: Configuration of the plugin
identifier: intranda_administration_archive_management
published: true
---

After installing the plugin and the associated database, the plugin must also be configured. This takes place within the configuration file `plugin_intranda_administration_archive_management.xml` and is structured as follows:

{{CONFIG_CONTENT}}

## General configuration

The two parameters `<basexUrl>` and `<eadExportFolder>` configure the connection to the BaseX XML database. The URL to the REST API of the XML database is configured via `basexUrl`, `eadExportFolder` contains the folder name into which the plugin exports the EAD files. This folder is monitored by the XML database.

This is followed by a repeatable `<config>` block. The repeatable element `<archive>` can be used to specify for which files the `<config>` block should apply. If there should be a default block that should apply to all documents, `*` can be used.

The `<processTemplateId>` is used to specify which process template should be used as the basis for the Goobi processes created.

## Configuration of the generation of process titles
The parameters `<lengthLimit>` `<separator>` `<useIdFromParent>` and `<useSignature>` are used to configure the naming of the process to be generated:

* The `<lengthLimit>` value sets a length limit for all tokens except the manually set prefix and suffix. The default setting is `0`, i.e. it does not limit the length.
* The parameter `<separator>` defines the separator to be used to combine all separate tokens. The default setting is `_`.
- The parameter `<useIdFromParent>` configures whose ID should be used to create the process title. If it is set to `true`, the ID of the parent node is used. Otherwise, the ID of the current node is used.
- The parameter `<useShelfmarkAsId>` configures whether the shelfmark should preferably be used to generate the process title. If it is set to `true` and a shelfmark actually exists in a predecessor node, this shelfmark is used. Otherwise, the ID of the current node or the parent node is used, depending on the configuration of the parameter `<useIdFromParent>`. 


## Configuring the metadata fields

This is followed by a list of `<metadata>` elements. This controls which fields are displayed, can be imported, how they should behave and whether there are validation rules.

### Mandatory fields

Each metadata field consists of at least three mandatory fields:

| Value | Description |
| :--- | :--- |
| `name` | This value identifies the field. It must therefore contain a unique designation. If the value has not been configured separately in the messages files, it is also used as the label of the field. |
| `level` | This defines the area in which the metadatum is inserted. The numbers 1-7 are allowed: `1. Identity Statement Area`, `2. Context Area`, `3. Content and Structure Area`, `4. Condition of Access and Use Area`, `5. Allied Materials Area`, `6. Note Area`, `7. Description Control Area` |
| `xpath` | Defines an XPath expression that is used both to read from existing EAD files and to write to the EAD file. The path is relative to the `<ead>` element in the case of the main element, and always relative to the `<c>` element for all other nodes. |

### Optional information

There are also a number of other optional specifications:

| Value | Description |
| :--- | :--- |
| `@xpathType` | This defines whether the XPath expression returns an `element` \(default\), an `attribute` or a `text`. |
| `@visible` | This can be used to control whether the metadata is displayed in the mask or is hidden. The field may contain the two values `true` \(default\) and `false`. |
| `@repeatable` | Defines whether the field is repeatable. The field may contain the two values `true` and `false` \(default\). |
| `@showField` | Defines whether the field is displayed open in the detail display, even if no value is available yet. The field may contain the two values `true` and `false` \(default\). |
| `@rulesetName` | A metadata from the rule set can be specified here. When a Goobi process is created for the node, the configured metadata is created. |
| `@importMetadataInChild` | This can be used to control whether the metadate should also be created in Goobi processes of child nodes. The field may contain the two values `true` and `false` \(default\). |
| `@fieldType` | Controls the behaviour of the field. Possible are `input` \(default\) , `textarea`, `dropdown`, `multiselect`, `vocabulary`. |
| `value` | This sub-element is only used with the two types `dropdown` and `multiselect` and contains the possible values that are to be available for selection. |
| `vocabulary` | This subelement contains the name of the vocabulary to be used. It is only evaluated if `vocabulary` is set in the type of the field. The selection list contains the main value of each record. |
| `searchParameter` | This repeatable subfield can be used to define search parameters with which to filter the vocabulary, the syntax is `fieldname=value`. |
| `@validationType` | Here you can set whether the field should be validated. Three different rules are possible, which can be combined. `unique` checks that the content of the field has not been used again in another place, `required` ensures that the field contains a value. The type `regex` can be used to check whether the filled value corresponds to a regular expression. With `unique+required`, `regex+required`, `regex+unique` and `regex+unique+required` several validation rules can be applied. |
| `@regularExpression` | The regular expression to be used for `regex` validation is specified here. IMPORTANT: the backslash must be masked by a second backslash. A class is therefore not defined by `\w` but by `\\w`. |
| `validationError` | This subfield contains a text that is displayed if the field content violates the validation rules. |

## Examples of different field configurations

### Simple input field

```xml
<metadata xpath="./ead:control/ead:maintenanceagency/ead:agencycode" xpathType="element" name="agencycode" level="1" repeatable="false" fieldType="input"/>
```

### Text area

```xml
<metadata xpath="(./ead:archdesc/ead:did/ead:unittitle | ./ead:did/ead:unittitle)[1]" xpathType="element" name="unittitle" level="1" repeatable="false" fieldType="textarea" rulesetName="TitleDocMain" importMetadataInChild="false" />
```

### Selection list

```xml
<metadata xpath="(./ead:archdesc/@level | ./@level)[1]" xpathType="attribute" name="descriptionLevel" level="1" repeatable="false" fieldType="dropdown">
    <value>collection</value>
    <value>fonds</value>
    <value>class</value>
    <value>recordgrp</value>
    <value>series</value>
    <value>subfonds</value>
    <value>subgrp</value>
    <value>subseries</value>
    <value>file</value>
    <value>item</value>
    <value>otherlevel</value>
</metadata>
```

### Multiple selection

```xml
<metadata xpath="(./ead:archdesc/ead:did/ead:langmaterial[@label='Language']/ead:language | ./ead:did/ead:langmaterial[@label='Language']/ead:language)[1]" xpathType="element" name="langmaterial" level="4" repeatable="false" fieldType="multiselect" rulesetName="DocLanguage" importMetadataInChild="false">
    <value>eng</value>
    <value>ger</value>
    <value>dut</value>
    <value>fre</value>
    <value>esp</value>
    <value>ita</value>
    <value>lat</value>
    <value>pol</value>
    <value>rus</value>
    <value>swe</value>
</metadata>
```

### Validation of dates

```xml
<metadata xpath="(./ead:archdesc/ead:did/ead:unitdate | ./ead:did/ead:unitdate)[1]" xpathType="element" name="unitdate" level="1" repeatable="false" rulesetName="PublicationYear" importMetadataInChild="false" regularExpression="^([0-9]{4}\\-[0-9]{2}\\-[0-9]{2}|[0-9]{4})(\\s?\\-\s?([0-9]{4}\\-[0-9]{2}\\-[0-9]{2}|[0-9]{4}))?$" validationType="regex">
  <validationError>The value is not a date specification. Permitted values are either years (YYYY), exact dates (YYYY-MM-DD) or time periods (YYYY - YYYY, YYYY-MM-DD-YYYY-MM-DD).</validationError>
</metadata>
```

### Connection of a controlled vocabulary

```xml
<metadata xpath="(./ead:archdesc/ead:dsc/ead:acqinfo | ./ead:dsc/ead:acqinfo)[1]" xpathType="element" name="acqinfo" level="2" repeatable="false" fieldType="vocabulary" rulesetName="AquisitionInformation" >
  <vocabulary>Aquisition</vocabulary>
  <searchParameter>type=visible</searchParameter>
  <searchParameter>active=true</searchParameter>
</metadata>
```

