---
title: Übersicht
identifier: intranda_administration_archive_management
description: Goobi Administration Plugin for managing archive collections
published: true
keywords:
    - Goobi workflow
    - Plugin
    - Administration Plugin
    - Archive Management
    - EAD
---

## Introduction
This documentation describes the installation, configuration and use of the Administration Plugin for managing archive collections from within Goobi workflow. This allows the data from several archives to be managed and enables even small archives to record data in a standardised way without having to install third-party software that is subject to a charge. Export as standardised EAD files is possible at any time and can also be carried out automatically at regular intervals.

## Installation

### Installation of the plugin
The plugin consists of the following files to be installed

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

The plugin also requires an additional configuration file, which must be located in the following location:

```bash
/opt/digiverso/goobi/config/plugin_intranda_administration_archive_management.xml
```

## Overview and functionality
The plugin for editing archives can be found under the menu item ‘Administration’.

![Entering the plugin](screen03_en.png)

To use the plugin, the user must first have the `Plugin_Administration_Archive_Management` right. If this right has not yet been assigned, the user will receive the following message:

![Reference to missing user rights](screen01_en.png)

The corresponding rights must therefore first be assigned to the respective user groups.

![Assignment of the required user rights](screen02_en.png)

Once the required rights have been assigned and, if necessary, a new login has been created, the plugin can be used.

The user initially only has read access. In order to be able to change data, the following additional rights are available, which can be assigned if necessary:

Authorisation | Explanation
-------------|-----------
`Plugin_Administration_Archive_Management_Write` | Write access to the data
`Plugin_Administration_Archive_Management_Upload`| Upload or import (new) EAD files
`Plugin_Administration_Archive_Management_New` | Creation of new inventories
`Plugin_Administration_Archive_Management_Vocabulary` | Authorisation to extend selection lists from vocabularies
`Plugin_Administration_Archive_Management_Inventory_NAME` | Access to individual selected inventories, whereby the suffix NAME must be replaced by the name of the inventory
`Plugin_Administration_Archive_Management_All_Inventories` | Access to all inventories
`Plugin_Administration_Archive_Management_Delete` | Delete the selected inventory
`Plugin_Administration_Archive_Management_Process` | Create processes

A detailed explanation of how to use the plugin and its functions can be found on this page:

[Using the plugin](../02_usage/en.md)


## Configuration
After installation, the plugin and the associated interface are configured in the configuration file `plugin_intranda_administration_archive_management.xml`. This is described in detail on the following page:

[Configuration of the plugin](../03_config/en.md)

