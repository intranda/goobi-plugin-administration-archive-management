---
title: Archive Management
identifier: intranda_administration_archive_management
description: Goobi Administration Plugin for the management of Archival Record Groups
published: true  
---

## Introduction
This documentation describes the installation, configuration and use of the Administration Plugin for managing archive records from within Goobi workflow. In doing so, the data of several inventories is stored within standardized files, allowing even small archives to manage their data in a standardised way without the need to start using third-party software for which a fee is charged. Export as standardised EAD files is possible at any time and can also be carried out automatically at regular intervals.


## Installation

### Installing the plugin
The plugin consists in total of the following files to be installed

```bash
plugin-administration-archive-management-base.jar
plugin-administration-archive-management-gui.jar
plugin-administration-archive-management-job.jar
plugin-administration-archive-management-lib.jar
plugin_intranda_administration_archive_management.xml
```

These files must be installed in the correct directories so that they are available in the following paths after installation:

```bash
/opt/digiverso/goobi/plugins/administration/plugin-administration-archive-management-base.jar
/opt/digiverso/goobi/plugins/GUI/plugin-administration-archive-management-gui.jar
/opt/digiverso/goobi/plugins/GUI/plugin-administration-archive-management-job.jar
/opt/digiverso/goobi/plugins/GUI/plugin-administration-archive-management-lib.jar
```

Furthermore, the plugin needs an additional configuration file, which must be located at the following location:

```bash
/opt/digiverso/goobi/config/plugin_intranda_administration_archive_management.xml
```

## Overview and functionality
The plugin for editing archive records can be found below the menu item 'Administration'.

![Entering the plugin](screen03_en.png)

### Assigning the rights required to use the plugin
To use the plugin, the user must first have the right `Plugin_Administration_Archive_Management`. If this right has not yet been assigned, the user will receive the following notice:

![Reference to missing user rights](screen01_en.png)

The corresponding rights must therefore first be assigned to the respective user groups.

![Assignment of the required user rights](screen02_en.png)

After the required rights have been assigned and, if necessary, a new login has been made, the plugin can be used.

The user initially only has read access. In order to be able to change data, the `Plugin_Administration_Archive_Management_Write` right must be assigned. With the right `Plugin_Administration_Archive_Management_Upload` the upload of EAD files is possible, `Plugin_Administration_Archive_Management_New` allows the creation of new inventories and with `Plugin_Administration_Archive_Management_Vocabulary` the option to expand lists is enabled if these are filled using a vocabulary.

To allow a user group access to individual inventories, the permission `Plugin_Administration_Archive_Management_Inventory_NAME` can be assigned, whereby the suffix NAME must be replaced by the name of the inventory. If access to all inventories is to be permitted instead, the permission `Plugin_Administration_Archive_Management_All_Inventories` can be used.

### Selection of existing EAD file
After the plugin has been opened, a list of the available archive records is displayed first. Here, the user can select an archive file and start editing it.

![Opening an existing archive file](screen05_en.png)

Alternatively, a new archive file can also be created. In this case, the desired XML database within which the new archive file is to be created must first be selected. A name must also be assigned here.

![Creating a new archive file](screen04_en.png)

The third option is to import an existing file. An EAD file can be selected and uploaded here. If no inventory with the name of the file exists yet, the file is imported as a new inventory and opened directly. If the name is already in use, the existing inventory can be overwritten with the content of the EAD-XML file after a query.

![Import an EAD file](screen08_en.png)

After selecting the archive file to be edited, you will be forwarded to the editing screen. Here, the structure tree can be edited in the left-hand area. In the right-hand area, the details of the selected node can be edited..

![Editing mask for the archive file](screen06_en.png)

Clicking on the buttons 'Cancel' or 'Save and exit archive group' takes you back to the page for selecting an archive file.

### Edit structure tree
In the left-hand area of the editing screen, the structure of the archive file can be edited. Here, all nodes including their hierarchy can be viewed at a glance. In front of each element is an icon with which the sub-elements of the node can be displayed or hidden. To select a node, it can be clicked on. It will then be highlighted and the details of the selected node will be displayed on the right. If a node is selected in the left-hand area of the edit screen, the buttons on the right-hand edge of the left-hand box can also be used to change the node. The following options are possible:

| Function | Explanation |
| :--- | :--- |
| `Insert new node` | With this button, a new node can be added as a sub-node to the end of the already existing sub-nodes. |
| `Insert multiple subnodes at this position` | Opens a pop-up in which a number of nodes can be created.|
| `Update references to processes` | Checks whether processes exist for the nodes in the inventory. Updates the references if necessary. |
| `Create missing processes for all lowest level subnodes` |Generates processes for the selected node and all children if none exist. |
| `Delete node` | With this function the selected node including all sub-nodes can be deleted. This function cannot be used at the level of the main node. |
| `Execute validation` | With this function, a validation of the selected node can be carried out. Violations of the configured validation specifications are listed accordingly. |
| `Move up` | This button allows moving the selected node up within the same hierarchy level. |
| `Move down` | This button allows moving the selected node down within the same hierarchy level. |
| `Move down the hierarchy` | With this button it is possible to move the selected node to a lower hierarchy level. |
| `Move up the hierarchy` | With this button it is possible to move the selected node to a higher hierarchy level. |
| `Mode node to another location` | This function opens another editing screen that makes it possible to move the currently selected node to a completely different position in the hierarchy tree. The complete hierarchy is displayed so that the node can be selected within which the selected node is to be inserted as a sub-node. |
| `Duplicate node` | This opens a pop-up in which a prefix or suffix can be specified for selected metadata (attributes visible and showField). Duplicates the selected node and all child elements and adds the specified prefixes and suffixes to the new metadata.|

![Add multiple nodes](screen11_en.png)

To generate several sub-nodes at once, the number of nodes to be created and the type must be defined. Various metadata can then be defined and entered in all new nodes. Either the same text can be used in all fields, an identifier can be generated or a text with a subsequent counter can be generated. The counter format and the start value can be defined here.

![Search within the inventory](screen07_en.png)

In the upper area of the hierarchy display, a search can also be made within the metadata of the nodes. The nodes found, including the hierarchy, are displayed and highlighted in colour. To reset the search, it is sufficient to empty the content of the search term again and perform an empty search accordingly. Alternatively, you can use the button on the left-hand side of the search field.

![Advanced search](screen10_en.png)

The advanced search can be used to the right of the field. Individual fields can be searched for here. Which fields are available can be controlled via the configuration file (attribute `searchable=‘true’` within `<metadata>`).

### Editing a selected node
If a node has been selected in the left-hand area, the details of the selected node are displayed in the right-hand area.

The right pane is divided into several categories. The topmost part of the right-hand pane shows the associated Goobi process and an option to create the docket. If no Goobi process has yet been created for the node, a new process can be created based on the configured production template. The selected node type is used as the document type according to the configuration. Depending on the configuration and the rule set used, the following options are available, for example:

* Folder
* File
* Image
* Audio
* Video
* Other

Below the document type, the individual metadata of the node are listed. They are divided into the following areas according to the ISAD\(G\) standard:

* Identity Statement Area
* Context Area
* Content and Structure Area
* Condition of Access and Use Area
* Allied Materials Area
* Note Area
* Description Control Area

Each of these areas can be opened and closed individually. Even if an area is collapsed, it is very easy to see which metadata is possible per area and which is already filled in. The individual metadata are displayed as differently highlighted badges. A dark background indicates that a value has already been entered for this metadata. A light background indicates that this field is still without content. If a field can be created repeatedly, the badge contains a plus icon.

When the details of a field are expanded, the individual metadata are displayed. By default, only those fields are displayed that already have a value. Further fields can be added by clicking on one of the badges. Fields can be removed by clicking on the minus icon.

### Validation of metadata
Both the button 'Download as EAD file' and the button 'Execute validation' ensure that the metadata are valid. The configured rules are applied and checked whether individual values violate them. If this is the case, the affected nodes are highlighted in colour in the left-hand area. If such an invalid node is selected, the affected badges are displayed with a red color and the configured error text is displayed in the metadata in addition to the border.

![Display of an incorrect validation](screen09_en.png)

A failed validation does not prevent the archive file from being saved or Goobi processes from being created.

### Saving the data

Unless editing is carried out in read-only mode, data is always saved automatically when you insert or delete nodes, switch to another node, export the database, create a copy of it or create links or end editing by saving and exiting.

### Copy, Export and Download

If you have authorisation to create new inventories, you can create a copy of the current inventory. This involves creating a new inventory and copying all nodes with all their metadata. The only exception is the ID of the nodes; new ones are created automatically. 

The two buttons for `Download as EAD file` and `Viewer export` generate a new EAD based on the current status of the nodes. With the exception of the top node, each node is displayed as an independent `<c>` element. The data of the top node is written within `<archdesc>` below the `<ead>` element. During viewer export, the generated file is written to the hotfolder; during download, it can be saved locally. The file contains all metadata in the form in which it was specified in the configuration file. The content of the `xpath` attribute of the metadata is used. If there is no entry for a field, it is an intensive metadata that is not exported as an EAD.

## Configuration
After the installation has been completed, the configuration of the plugin and the corresponding interface can take place. This is described in detail on the following page:

[Configuration of the plugins](page_03_en.md)
