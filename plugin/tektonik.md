# Verwaltung von Tektoniken

## Installation des Plugins

Zur Installation des Plugins müssen folgende beiden Dateien installiert werden:

```bash
/opt/digiverso/goobi/plugins/administration/plugin_intranda_administration_tektonik.jar
/opt/digiverso/goobi/plugins/GUI/plugin_intranda_administration_tektonik-GUI.jar
```

Um zu konfigurieren, wie sich das Plugin verhalten soll, können verschiedene Werte in der Konfigurationsdatei angepasst werden. Die Konfigurationsdatei befindet sich üblicherweise hier:

```bash
/opt/digiverso/goobi/config/plugin_intranda_administration_tektonik.xml
```

## Installation der XML-Datenbank BaseX

BaseX ist eine XML-Datenbank, in der die EAD Dateien verwaltet, analysiert und abgefragt werden können. Voraussetzung für die Installation von BaseX ist Java 1.8.

Zunächst muss der Download der Datenbank erfolgen:

​<http://basex.org/download/>​

Für die Installation von BaseX auf einem Linux System muss zunächst die zip Datei herunterladen und auf dem Server installiert werden. Dies könnte beispielsweise in diesem Pfad erfolgen:

```bash
/opt/digiverso/basex
```

Anschließend muss die Jetty-Konfiguration angepasst werden, so dass die Applikation nur auf localhost erreichbar ist. Dafür muss in der Konfigurationsdatei /opt/digiverso/basex/webapp/WEB-INF/jetty.xml sichergestellt werden, dass der host auf 127.0.0.1 steht:

```bash
jetty.xml
  <Set name="host">127.0.0.1</Set>
```

Anschließend wird die Systemd Unit File an diesen Pfad installiert:

```bash
/etc/systemd/system/basexhttp.service
```

Diese hat folgenden Aufbau:

```bash
basexhttp.service
[Unit]
Description=BaseX HTTP server
​
[Service]
User=tomcat8
Group=tomcat8
ProtectSystem=full
ExecStart=/opt/digiverso/basex/bin/basexhttp
ExecStop=/opt/digiverso/basex/bin/basexhttp stop
​
[Install]
WantedBy=multi-user.target
```

Anschließend muss der Daemon neu geladen, die Unit-File aktiviert und die Datenbank neu gestartet werden:

```bash
systemctl daemon-reload
systemctl enable basexhttp.service
systemctl start basexhttp.service
```

Damit das Admin-Interface auch von extern erreichbar ist, kann dieses im Apache zum Beispiel mit dem folgenden Abschnitt konfiguriert werden:

```bash
    redirect 301 /basex http://example.com/basex/
    <Location /basex/>
            Require ip 188.40.71.142
            ProxyPass http://localhost:8984/ retry=0
            ProxyPassReverse http://localhost:8984/
    </Location>
```

Im Anschluß daran muss noch das Apache Modul proxy_http aktiviert und der Apache neu gestartet werden, damit die Anpassungen wirksam werden:

```bash
a2enmod proxy_http
systemctl restart apache2
```

### Datenbank einrichten

Die XML Datenbank kann nach der Installation unter folgender URL erreicht werden:

​<http://localhost:8984/dba/login>​

Die Zugangsdaten lauten admin/admin. Nach dem ersten Anmelden sollte daher als erstes ein neues Passwort vergeben werden. Dazu muss der Menüeintrag Users geöffnet werden. Hier kann der Accountname angeklickt und das neue Passwort gesetzt werden.

Anschließend kann eine neue Datenbank für die EAD Dateien erzeugt werden. Dazu muss der Menüeintrag Databases ausgewählt werden. Mittels Create gelangt man in den Dialog dazu. Hier muss einTitel für die Datenbank vergeben werden. Alle anderen Einstellungen können so bleiben.

### Dateien hinzufügen und löschen

Nachdem die Datenbank erstellt wurde, können nun EAD-XML-Dokumente hinzugefügt werden. Dazu kann unter Databases die erstellte Datenbank ausgewählt werden. Daraufhin öffnet sich ein Fenster, in dem die zur Datenbank gehörenden Dateien verwaltet werden können. Neue Dateien lassen sich über den Dialog Add auswählen und hochladen. Hier kann im Feld Input eine EAD-Datei ausgewählt werden. Mittels Add wird die Datei hinzugefügt und die Übersichtsseite geladen. Hier können auch Dateien entfernt werden. Dazu müssen sie mittels Checkbox markiert und dann über Delete gelöscht werden. Das Aktualisieren einer EAD-Datei ist nur über Löschen und erneutes Hinzufügen möglich.

### Definition der Anfragen

Um das Interface zur Abfrage für Goobi einzurichten, muss der Datenbank bekannt gemacht werden, wie eine Anfrage aussieht, was damit geschehen soll und wie das Ergebnis auszusehen hat. Dafür bietet BaseX verschiedene Optionen an. Wir haben uns für RESTXQ entschieden, da diese im Gegensatz zur REST Schnittstelle keine Authentication benötigt.

Dazu müssen im Verzeichnis /opt/digiverso/basex/webapp/ neue Dateien erzeugt werden.

```xq
listDatabases.xq

(: XQuery file to return the names of all available databases :)
module namespace page = 'http://basex.org/examples/web-pagepage';
(:declare default element namespace "urn:isbn:1-931666-22-9";:)

declare
  %rest:path("/databases")
  %rest:single
  %rest:GET

function page:getDatabases() {
  let $ead := db:list()
  
  return
    <databases>
    {
      for $c in $ead
      return
        <database>
          <name>
            {$c}
          </name>
          {
          let $files := db:list-details($c)
          return
            <details>
              {$files}
            </details>
          }
        </database>
    }
  </databases>
};
```

```xq
openDatabase.xq

(: XQuery file to return a full ead record :)
module namespace page = 'http://basex.org/examples/web-page';
declare default element namespace "urn:isbn:1-931666-22-9";

declare
  %rest:path("/db/{$database}/{$filename}")
  %rest:single
  %rest:GET

function page:getDatbase($database, $filename) {
  let $ead := db:open($database, $filename)/ead
  return
  <collection>
    {$ead}
  </collection>
};
```

```xq
importFile.xq

(: XQuery file to return a full ead record :)
module namespace page = 'http://basex.org/examples/web-page';
declare default element namespace "urn:isbn:1-931666-22-9";

declare
  %rest:GET
  %rest:path("/import/{$db}/{$filename}")

updating function page:import($db, $filename) {
  let $path := '/opt/digiverso/basex/import/' || $filename
  let $details := db:list-details($db, $filename)

  return
    if (fn:empty($details)) then
      db:add($db, doc($path), $filename)
    else
      db:replace($db, $filename, doc($path))
};

```

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

- ```@xpathType```: Hiermit wird definiert, ob der XPath Ausdruck ein ```element``` (default), ein ```attribute``` oder einen ```text``` zurückliefert.
- ```@visible```: Hierüber kann gesteuert werden, ob das Metadatum in der Maske angezeigt wird oder versteckt ist. Das Feld darf die beiden Werte ```true``` (default) und ```false``` enthalten.
- ```@repeatable```: Definiert, ob das Feld wiederholbar ist. Das Feld darf die beiden Werte ```true``` und ```false``` (default) enthalten.
- ```@showField```: Definiert, ob das Feld in der Detailanzeige geöffnet angezeigt wird, auch wenn noch kein Wert vohanden ist. Das Feld darf die beiden Werte ```true``` und ```false``` (default) enthalten.
- ```@rulesetName```: Hier kann ein Metadatum aus dem Regelsatz angegeben werden. Wenn für den Knoten ein Goobi-Vorgang erstellt wird, wird das konfigurierte Metadatum erstellt.
- ```@importMetadataInChild```: Hierüber kann gestuert werden, ob das Metadatum auch in Goobi-Vorgängen von Kind-Knoten erstellt werden soll. Das Feld darf die beiden Werte ```true``` und ```false``` (default) enthalten.
- ```@fieldType```: Steuert das Verhalten des Feldes. Möglich sind ```input``` (default) ,  ```textarea```,  ```dropdown```,  ```multiselect```, ```vocabulary```
- ```value```: dieses Unterelement wird nur bei den beiden Typen ```dropdown``` und  ```multiselect``` genutzt und enthält die möglichen Werte, die zur Auswahl stehen sollen.
- ```vocabulary```: dieses Unterelement enthält den Namen des zu verwendenden Vokabulars. Es wird nur ausgewertet, wenn ```vocabulary``` im Typ des Feldes gesetzt ist. Die Auswahlliste enthält jeweils den Hauptwert der Records.
- ```searchParameter```: Mit diesem wiederholbaren Unterfeld können Suchparameter definiert werden, mit denen das Vokabular gefiltert wird, die Syntax ist ```fieldname=value```.
- ```@validationType```: Hier kann eingestellt werden, ob das Feld validiert werden soll. Dabei sind drei unterschiedliche Regeln möglich, die sich kombinieren lassen. ```unique``` prüft, dass der Inhalt des Feldes nicht noch einmal an einer anderen Stelle genutzt wurde, mittels ```required``` wird sichergestellt, dass das Feld einen Wert enthhält. Mit dem Typ ```regex``` kann geprüft werden, ob der ausgefüllte Wert einem regulären Ausdruck entspricht. Mit ```unique+required```, ```regex+required```, ```regex+unique``` und ```regex+unique+required``` können mehrere Validierungsegeln angewendet werden.
- ```@regularExpression```: Hier wird der reguläre Ausdruck angegeben, der bei der ```regex``` Validierung genutzt werden soll. WICHTIG: der Backslash muss dabei durch einen zweiten Backslash maskiert werden. Eine Klasse wird daher nicht durch ```\w```, sondern durch ```\\w``` definiert.
- ```validationError```: Dieses Unterfeld enthält einen Text, der angezeigt wird, wenn der Feldinhalt gegen die Validierungsregeln verstößt.

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

## Beispiele

TODO: Screenshots

Einfaches Eingabefeld

```xml
<metadata xpath="./ead:control/ead:maintenanceagency/ead:agencycode" xpathType="element" name="agencycode" level="1" repeatable="false" fieldType="input"/>
```

Textfeld

```xml
<metadata xpath="(./ead:archdesc/ead:did/ead:unittitle | ./ead:did/ead:unittitle)[1]" xpathType="element" name="unittitle" level="1" repeatable="false" fieldType="textarea" rulesetName="TitleDocMain" importMetadataInChild="false" />

```

Auswahlliste

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

Mehrfachauswahl

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

Validierung von Datumsangaben

```xml
<metadata xpath="(./ead:archdesc/ead:did/ead:unitdate | ./ead:did/ead:unitdate)[1]" xpathType="element" name="unitdate" level="1" repeatable="false" rulesetName="PublicationYear" importMetadataInChild="false" regularExpression="^([0-9]{4}\\-[0-9]{2}\\-[0-9]{2}|[0-9]{4})(\\s?\\-\s?([0-9]{4}\\-[0-9]{2}\\-[0-9]{2}|[0-9]{4}))?$" validationType="regex">
  <validationError>Der Wert ist keine Datumsangabe. Erlaubte Werte sind entweder Jahreszahlen (YYYY), exakte Datumsangaben (YYYY-MM-DD) oder Zeiträume (YYYY - YYYY, YYYY-MM-DD-YYYY-MM-DD)</validationError>
</metadata>
```

Anbindung eines kontrollierten Vokabulars

```xml
<metadata xpath="(./ead:archdesc/ead:dsc/ead:acqinfo | ./ead:dsc/ead:acqinfo)[1]" xpathType="element" name="acqinfo" level="2" repeatable="false" fieldType="vocabulary" rulesetName="AquisitionInformation" >
  <vocabulary>Aquisition</vocabulary>
  <searchParameter>type=visible</searchParameter>
  <searchParameter>active=true</searchParameter>
</metadata>
```
