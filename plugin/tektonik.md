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

Sofern im linken Bereich ein Knoten ausgewählt wurde, werden im rechten Bereich die Details des Knotens angezeigt.

Der rechte Bereich ist in mehrere Kategorien aufgeteilt. Im obersten Bereich wird der dazugehörige Goobi-Vorgang angezeigt, sowie eine Möglichkeit zum erzeugen des Laufzettels. Wenn für den Knoten noch kein Goobi-Vorgang erzeugt wurde, kann ein neuer Vorgang erstellt werden. Als Dokumententyp wird der ausgewählte Knotentyp genutzt. Es stehen folgende Optionen zur Verfügung:

- Folder/Ordner
- File/Akte
- Image/Bild
- Audio
- Video
- Other/Sonstiges

Darunter werden die einzelnen Metadaten des Knotens aufgelistet. Sie sind in verschiedene Bereiche aufgeteilt:

- Identifikation
- Kontext
- Inhalt und innere Ordnung
- Zugangs- und Benutzungsbedingungen
- Sachverwandte Unterlagen
- Anmerkungen
- Verzeichnungskontrolle

Jeder Bereich lässt sich einzeln auf- und zuklappen. Wenn ein Bereich zugeklappt ist, hat man einen schnellen Überlick über die potentiellen und existierenden Metadaten des Bereiches. Die einzelnen Metadaten werden als Badges angezeigt. Ein dunkler Hintergrund zeigt an, dass für diese Metadatum ein Wert erfasst wurde. Ein heller Hintergrund bedeutet, dass dieses Feld noch ohne Inhalt ist. Wenn das Feld wiederholbar ist, enthält der Badge ein Plus-Zeichen. Darüber kann ein neues Feld hinzugefügt werden.

Wenn die Details eines Bereiches angezeigt werden, werden die einzelnen Metadaten aufgelistet. Standardmäßig werden dabei nur die Felder angezeigt, die einen Wert enthalten. Um weitere Felder anzuzeigen oder auszublenden, kann der jeweilige Badge angeklickt werden.

### Validieren

Die Buttons zum ```Download als EAD Datei``` und ```Validieren``` führen die Validierung aus. Dabei werden die konfigurierten Regeln angewendet und geprüft, ob einzelne Werte dagegen verstoßen. Ist dies der Fall, werden die betroffene Knoten im linken Bereich hervorgehoben. Wird ein solcher Knoten ausgewählt, sind die betroffenen Badges rot umrandet und in den Metadaten wird neben der Umrandung auch ein konfigurierbarer Fehlertext angezeigt.

Eine fehlgeschlagene Validierung verhindert NICHT das Speichern der Tektonik oder das Erzeugen von Goobi-Vorgängen.

### Download, Speichern, Abrechen

Die beiden Buttons zum ```Download als EAD Datei``` und ```Tektonik speichern und verlassen``` erzeugen eine neue EAD auf Basis des aktuellen Zustandes der Knoten. Dabei wird mit Ausnahme des obersten Knoten jeder Knoten als eigentständiges ```<c>```-Element dargestellt. Die Daten des obersten Knoten werden innerhalb von ```<archdesc>``` unterhalb des ```<ead>``` Elements geschrieben.

Neben den erfassten Metadaten wird ein neues create oder modified event samt Datum und Nutzerinformationen erstellt und in der Liste der Events hinzugefügt.
Wenn die Tektonik gespeichert wird, wird der aktuelle Zustand automatisch in die XML Datenbank exportiert. Anschließend wird man genau wie bei Abbrechen auf die Startseite des Plugins weitergeleitet.

## Installation

Zur Installation des Plugins müssen folgende beiden Dateien installiert werden:

```bash
/opt/digiverso/goobi/plugins/administration/plugin_intranda_administration_tektonik.jar
/opt/digiverso/goobi/plugins/GUI/plugin_intranda_administration_tektonik-GUI.jar
```

Um zu konfigurieren, wie sich das Plugin verhalten soll, können verschiedene Werte in der Konfigurationsdatei angepasst werden. Die Konfigurationsdatei befindet sich üblicherweise hier:

```bash
/opt/digiverso/goobi/config/plugin_intranda_administration_tektonik.xml
```

TODO: basex installieren + konfigurieren, xq Dateien

## Konfiguration des Plugins

Die Konfiguration des Plugins ist folgendermaßen aufgebaut:

```xml
<config_plugin>
    <basexUrl>http://localhost:8984/</basexUrl>
    <eadExportFolder>/opt/digiverso/basex/import</eadExportFolder>

    <config>
        <tectonics>*</tectonics>
        <processTemplateId>1</processTemplateId>
            <metadata xpath="./ead:eadheader[@countryencoding='iso3166-1'][@dateencoding='iso8601'][@langencoding='iso639-2b'][@repositoryencoding='iso15511'][@scriptencoding='iso15924']/ead:eadid/@mainagencycode" xpathType="attribute" name="mainagencycode" level="1" repeatable="false" visible="false"/>
            <metadata xpath="./ead:control/ead:maintenanceagency/ead:agencycode" xpathType="element" name="agencycode" level="1" repeatable="false" fieldType="input"/>

    </config>

</config_plugin>
```

Mit den beiden Parametern ```<basexUrl>``` und ```<eadExportFolder>``` wird die Anbindung an die basex XML Datenbank konfiguriert. Über basexUrl wird die URL zur REST API der XML Datenbank konfiguriert, eadExportFolder enthält den Ordnernamen, in den das Plugin die EAD Dateien exportiert. Dieser Ordner wird von der XML Datenbank überwacht.

Anschließend folgt ein wiederholbarer ```<config>``` Block. Über das wiederholbare Element ```<tectonics>``` kann festgelegt werden, für welche Dateien der ```<config>``` Block gelten soll. Gibt es einen Default-Block für alle Dokuemnte, kann ```*``` genutzt werdern.

Mittels ```<processTemplateId>``` wird festgelegtm, auf welcher Produktionsvorlage die erstellten Goobi Vorgänge basieren sollen.

Anschließend folgt eine Liste von ```<metadata>``` Elementen. Darüber wird gesteuert, welche Felder angezeigt werden, importiert werden können, wie sie sich verhalten und ob es Validierungsregeln gibt.

Jedes Feld besteht aus mindestens drei Pflichtangaben:

- ```name```: mit diesem Wert wird das Feld identifiziert. Es muss daher eine eindeutige Bezeichnung enthalten. Sofern der Wert nicht noch extra in den messages Dateien konfiguriert wurde, wird er auch als Label des Feldes genutzt.
- ```level```: hiermit wird definiert, in welchem Bereich das Metadatum eingefügt wird. Dabei sind die Zahlen 1-7 erlaubt:
  1. Identifikation
  2. Kontext
  3. Inhalt und innere Ordnung
  4. Zugangs- und Benutzungsbedingungen
  5. Sachverwandte Unterlagen
  6. Anmerkungen
  7. Verzeichnungskontrolle
- ```xpath```: Definiert einen XPath Ausdruck, der sowohl zum Lesen aus vorhanden EAD Dateien als auch zum Schreiben der EAD Datei genutzt wird. Der Pfad ist im Fall des Hauptelements relativ zum ```<ead>``` Element, bei allem anderen Knoten immer relativ zum ```<c>``` Element. 

Des weiteren gibt es noch eine Reihe weiterer optionaler Angaben:

- ```xpathType```: Hiermit wird definiert, ob der XPath Ausdruck ein ```element```, ein ```attribute``` oder einen ```text``` zurückliefert. Wenn nicht anders angegeben, wird der default ``element``` genutzt.
- visible: Hierüber kann gesteuert werden, ob das Metadatum in der Maske angezeigt wird oder versteckt ist. Das Feld darf die beiden Werte ```true``` (default) und ```false``` enthalten.
- repeatable: Definiert, ob das Feld wiederholbar ist. Das Feld darf die beiden Werte ```true``` und ```false``` (default) enthalten.
- showField: Definiert, ob das Feld in der Detailanzeige geöffnet angezeigt wird, auch wenn noch kein Wert vohanden ist. Das Feld darf die beiden Werte ```true``` und ```false``` (default) enthalten.


optional:


- @fieldType: defines the type of the input field. Posible values are input (default), textarea, dropdown, multiselect, vocabulary
- @rulesetName: internal name of the metadata in ruleset. If missing or empty, field is not imported into process metadata
- @importMetadataInChild: defines if the field is imported or skipped in processes for child elements 
- @validationType: defines a validation rule, allowed values are unique, required, unique+required, regex, regex+required
- @regularExpression defines a regular expression that gets used for validation type regex
- validationError: message to display in case of validation errors
- value: list of possible values for dropdown and multiselect lists
- vocabulary: name of the vocabulary
- searchParameter: distinct the vocabulary list by the given condition. Syntax is fieldname=value, field is repeatable
