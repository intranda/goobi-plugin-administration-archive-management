# Verwaltung von Tektoniken

## Nutzung

- Der Tektonik-Editor befindet sich im Bereich Administration -> Tektonik
- Der Nutzer muss die Rolle Plugin_Administration_Tektonik haben

### Auswahl vorhandener EAD Datei

Wird das Plugin geöffnet, wird die Liste der vorhandenen Tektoniken angezeigt. Hier kann der Nutzer eine Tektonik auswählen und die Bearbeitung beginnen. Alternativ kann eine neue Tektonik erstellt werden. In dem Fall muss die XML Datenbank ausgewählt werden, in der die neue Tektonik gespeichert werden soll und ein Name vergeben werden.

In beiden Fällen wird man in die Bearbeitungsmaske weiter geleitet. Im linken Bereich lässt sich der Strukturbaum bearbeiten, im rechten Bereich können die Details eines einzelnen Knoten bearbeitet werden.

Durch Abrechen oder Speichern und Verlassen wird man wieder auf die Seite zur Auswahl der Tektonik geleitet.

### Strukturbaum bearbeiten

Im linken Bereich lässt sich die Struktur der Tektonik bearbeiten. Hier lassen sich alle Knoten inklusive ihrer Hierarchie auf einen Blick einsehen. Vor jedem Element befindet sich ein Icon, mit dem sich die Unterelemente des Knotens anzeigen oder verbergen lassen.
Um einen Knoten auszuwählen, kann er angeklickt werden. Er wird dann farblich hervorgehoben und die Details des gewählten Knotens werden auf der rechten Seite angezeigt.
Wenn ein Knoten ausgewählt wurde, können die Buttons am rechten Rand der grauen Box genutzt werden.

Plus: Fügt einen Knoten als neues Kindelement hinter das letzte Kind des ausgewählten Knoten ein.
Trash: Löscht den ausgewählten Knoten inklusive aller Kind-Elemente. Kann nicht im Hauptelement genutzt werden.
Horizontal nach oben verschieben: Tauscht den Platz mit dem direkten Vorgänger in der Liste der Kindelemente. Hat keine Auswirkungen, wenn das ausgewählte Element keine vorherigen Geschwister hat.
Horizontal nach unten verschieben: Tauscht den Platz mit dem direkten Nachfolger in der Liste der Kindelemente. Hat keine Auswirkungen, wenn das ausgewählte Element keine nachfolgenden Geschwister hat.
Vertikal nach links verschieben: verschiebt den ausgewählten Knoten eine Hierarchiestufe höher. Es wird in der höheren Ebene direkt hinter dem aktuellen Elternelement eingefügt. Kann nicht ausgeführt werden, wenn das Elternelement das Hauptelement ist oder das Hauptelement ausgewählt wurde.
Vertikal nach rechts verschieben: verschiebt den ausgewählten Knoten eine Hierarchiestufe tiefer. Es wird dabei als letztes Kindelemente des direkten Vorgängers eingefügt. Hat keine Auswirkungen, wenn das ausgewählte Element keine vorherigen Geschwister hat.

Kreuz: Öffnet die Seite zum verschieben an eine komplett neue Position. Hier wird die komplette Strukur angezeigt und man kann das Element auswählen, als dessen Kind das ausgewählte Element eingefügt werden soll. Das ausgewählte Element kann nicht als Kind von sich selbst oder dessen Kindern eingefügt werden.

Suche: Hier kann in den Titeln der einzelnen Knoten gesucht werden. Dabei werden die gefundenen Knoten samt Hierarchie angezeigt und farblich hervorgehoben. Um die Suche wieder zurückzusetzen, genügt es, nach einem leeren Text zu suchen.

### Knoten bearbeiten

- Vorgang anlegen
- Knotentyp
- Bereiche auf/zuklappen
- batch anklicken zum anzeigen
- Metadatentypen bearbeiten (titel)

### Validieren

TODO:

- Hervorhebung im Strukturbaum
- Hervorhebung in Metadaten
- Anzeige konfigurierter Fehlermeldung

### Download, Speichern, Abrechen

Speichern: Nutzername wird automatisch ermittelt, neues create/modified Event wird erstellt

## Installation

## Konfiguration

- basexUrl: url zur XML Datenbank
- eadExportFolder: Ordner, in dem die xml Dateien gespeichert werden, damit sie von der XML Datenbank importiert werden können
- processTemplateId: ID der Vorlage, auf dessen Basis die einzelnen Vorgänge angelegt werden
- metadata: Konfiguration der einzelnen Felder der Knoten
- @name: contains the internal name of the field. The value can be used to translate the field in the messages files. The field must start with a letter and can not contain any white spaces.
- @level: metadata level, allowed values are 1-7:
  - 1: metadata for Identity Statement Area 
  - 2: Context Area
  - 3: Content and Structure Area
  - 4: Condition of Access and Use Area
  - 5: Allied Materials Area
  - 6: Note Area
  - 7: Description Control Area
- @xpath: contains a relative path to the ead value. The root of the xpath is either the ead element or the c element
- @xpathType: type of the xpath return value, can be text, attribute, element (default)
- @repeatable: defines if the field can exist once or multiple times, values can be true/false, default is false
- @visible: defines if the field is displayed on the UI, values can be true/false, default is true
- @showField: defines if the field is displayed as input field (true) or badge (false, default), affects only visible metadata
- @fieldType: defines the type of the input field. Posible values are input (default), textarea, dropdown, multiselect, vocabulary
- @rulesetName: internal name of the metadata in ruleset. If missing or empty, field is not imported into process metadata
- @importMetadataInChild: defines if the field is imported or skipped in processes for child elements 
- @validationType: defines a validation rule, allowed values are unique, required, unique+required, regex, regex+required
- @regularExpression defines a regular expression that gets used for validation type regex
- validationError: message to display in case of validation errors
- value: list of possible values for dropdown and multiselect lists
- vocabulary: name of the vocabulary
- searchParameter: distinct the vocabulary list by the given condition. Syntax is fieldname=value, field is repeatable
