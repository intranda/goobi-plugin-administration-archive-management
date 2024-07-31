---
title: Archive Management
identifier: intranda_administration_archive_management
description: Goobi Administration Plugin für die Verwaltung von Archivbeständen
published: true  
---

## Einführung
Die vorliegende Dokumentation beschreibt die Installation, die Konfiguration und den Einsatz des Administration Plugins für die Verwaltung von Archivbeständen aus Goobi workflow heraus. Dabei werden die Daten mehrerer Bestände verwaltet und erlauben auch kleinen Archiven eine standardisierte Datenerfassung ohne Inbetriebnahme einer kostenpflichtigen Drittsoftware. Der Export als standardisierte EAD-Dateien ist jederzeit möglich und kann auch automatisch in regelmäßigen Abständen durchgeführt werden.

## Installation

### Installation des Plugins
Das Plugin besteht insgesamt aus den folgenden zu installierenden Dateien

```bash
plugin-administration-archive-management-base.jar
plugin-administration-archive-management-gui.jar
plugin-administration-archive-management-job.jar
plugin-administration-archive-management-lib.jar
plugin_intranda_administration_archive_management.xml
```

Diese Dateien müssen in den richtigen Verzeichnissen installiert werden, so dass diese nach der Installation in folgenden Pfaden vorliegen:

```bash
/opt/digiverso/goobi/plugins/administration/plugin-administration-archive-management-base.jar
/opt/digiverso/goobi/plugins/GUI/plugin-administration-archive-management-gui.jar
/opt/digiverso/goobi/plugins/GUI/plugin-administration-archive-management-job.jar
/opt/digiverso/goobi/plugins/GUI/plugin-administration-archive-management-lib.jar
```

Darüber hinaus benötigt das Plugin noch zusätzlich eine Konfigurationsdatei, die an folgender Stelle liegen muss:

```bash
/opt/digiverso/goobi/config/plugin_intranda_administration_archive_management.xml
```

## Überblick und Funktionsweise
Das Plugin für die Bearbeitung von Archivbeständen findet sich unterhalb des Menüpunkts `Administration`.

![Betreten des Plugins](screen03_de.png)

### Zuweisung der benötigen Rechte für die Nutzung des Plugins

Zur Nutzung des Plugins ist zunächst notwendig, dass der Nutzer über das Recht `Plugin_Administration_Archive_Management` verfügt. Sollte dieses Recht noch noch nicht zugewiesen worden sein, erhält der Nutzer folgenden Hinweis:

![Hinweis auf fehlende Nutzerrechte](screen01_de.png)

Die entsprechenden Rechte müssen den jeweiligen Benutzergruppen daher zunächst zugewiesen werden.

![Zuweisung der ben&#xF6;tigten Nutzerrechte](screen02_de.png)

Nachdem die benötigten Rechte zugewiesen wurden und ggf. ein neuer Login erfolgte, kann die Nutzung des Plugins erfolgen.

Dabei hat der Nutzer erst einmal nur lesenden Zugriff. Um auch Daten ändern zu können, muss das Recht `Plugin_Administration_Archive_Management_Write` vergeben werden. Mit der Berechtigung `Plugin_Administration_Archive_Management_Upload` ist das Hochladen von EAD Dateien möglich, `Plugin_Administration_Archive_Management_New` erlaubt die Erstellung von neuen Beständen und mittels `Plugin_Administration_Archive_Management_Vocabulary` wird die Option zum erweitern von Listen freigeschaltet, wenn diese per Vokabular gefüllt werden.

Um einer Nutzergruppe Zugriff auf einzelne Bestände zu ermöglichen, kann das Recht `Plugin_Administration_Archive_Management_Inventory_NAME` vergeben werden, wobei der Suffix NAME durch den Namen des Bestands zu ersetzen ist. Wenn stattdessen der Zugriff auf alle Bestände erlaubt sein soll, kann das Recht `Plugin_Administration_Archive_Management_All_Inventories` genutzt werden.

### Auswahl vorhandenen Beständen
Nachdem das Plugin geöffnet wurde, wird zunächst eine Liste der zur Verfügung stehenden Archivbestände angezeigt. Hier kann der Nutzer einen Archivbestand auswählen und mit der Bearbeitung beginnen.

![&#xD6;ffnen eines vorhandenen Archivbestandes](screen05_de.png)

Alternativ dazu kann ebenfalls ein neuer Archivbestand erzeugt werden. In diesem Fall muss zunächst ein Name für den Bestand vergeben werden. Der Name muss eindeutig sein, da darüber die Identifikation erfolgt. Außerdem sollten keine Sonderzeichen wie `:/\` genutzt werden, da der Name auch Grundlage für den Dateiname des EAD-Exports ist.

![Anlegen eines neuen Archivbestandes](screen04_de.png)

Als dritte Möglichkeit kann eine vorhandene Datei importiert werden. Hier kann eine EAD-Datei ausgewählt und hochgeladen werden. Wenn noch kein Bestand mit dem Namen der Datei existiert, wird die Datei als neuer Bestand importiert und direkt geöffnet. Falls der Name schon in Verwendung ist, kann nach einer Rückfrage der bestehende Bestand mit dem Inhalt der EAD-XML Datei überschrieben werden.

![Import einer EAD-Datei](screen08_de.png)

Nach der Auswahl des zu bearbeitenden Archivbestandes wird man in die Bearbeitungsmaske weitergeleitet. Hier läßt sich nun im linken Bereich der Strukturbaum bearbeiten. Im rechten Bereich können die Details des jeweils ausgewählten Knoten bearbeitet werden.

![Bearbeitungsmaske f&#xFC;r den Archivbestand](screen06_de.png)

Durch einen Klick auf die Buttons `Abrechen` (Leserechte) oder `Archivbestand speichern und verlassen` (Schreibrechte) wird man wieder auf die Seite zur Auswahl eines Archivbestandes geleitet.

### Strukturbaum bearbeiten
Im linken Bereich der Bearbeitungsmaske lässt sich die Struktur des Archivbestandes bearbeiten. Hier lassen sich alle Knoten inklusive ihrer Hierarchie auf einen Blick einsehen. Vor jedem Element befindet sich ein Icon, mit dem sich die Unterelemente des Knotens anzeigen oder ausblenden lassen. Um einen Knoten auszuwählen, kann er angeklickt werden. Er wird dann farbig hervorgehoben und die Details des ausgewählten Knotens werden auf der rechten Seite angezeigt. Wenn ein Knoten im linken Bereich der Bearbeitungsmaske ausgewählt wurde, können ausserdem die Buttons am rechten Rand der linken Box genutzt werden, um den Knoten zu ändern. Folgende Optionen sind hierbei möglich:

| Funktion | Erläuterung |
| :--- | :--- |
| `Neuen Knoten einfügen` | Mit diesem Button kann ein neuer Knoten als Unterknoten an das Ende der bereits vorhandenen Unterknoten angefügt werden. |
| `Mehrere Unterknoten an dieser Stelle einfügen` | Öffnet ein Popup, in dem sich beliebig viele Knoten erstellen lassen.|
| `Verweise aktualisieren` | Prüft, ob für die Knoten des Bestands Vorgänge existieren. Aktualisiert gegebenenfalls die Verweise. |
| `Fehlende Vorgänge erstellen` | Generiert für den ausgewählten Knoten und alle Kinder Vorgänge, falls noch keine existieren. |
| `Knoten löschen` | Hiermit läßt sich der ausgewählte Knoten inklusive aller Unterknoten löschen. Diese Funktion kann nicht auf der Ebene des Hauptknotens genutzt werden. |
| `Validierung ausführen` | Mit dieser Funktion läßt sich eine Validierung des ausgewählten Knotens ausführen. Verstöße gegen die konfigurierten Validierungsvorgaben werden entsprechend aufgeführt. |
| `Nach oben bewegen` | Dieser Button erlaubt das Verschieben des ausgewählten Knotens nach oben innerhalb der gleichen Hierarchieebene. |
| `Nach unten bewegen` | Dieser Button erlaubt das Verschieben des ausgewählten Knotens nach unten innerhalb der gleichen Hierarchieebene. |
| `In der Hierarchie nach unten bewegen` | Mit dieserm Button ist es möglich, den ausgewählten Knoten auf eine tiefere Hierarchiestufe zu verschieben. |
| `In der Hierarchie nach oben bewegen` | Mit dieserm Button ist es möglich, den ausgewählten Knoten auf eine höhere Hierarchiestufe zu verschieben. |
| `Knoten an andere Stelle bewegen` | Mit dieser Funktion öffnet sich eine andere Bearbeitungsmaske, die es ermöglicht, den aktuell ausgewählten Knoten an einer ganz andere Stelle des Hierarchiebaums zu verschieben. Hierbei wird die komplette Hierarchie angezeigt, so dass derjenige Knoten ausgewählt werden kann, innerhalb dem der ausgewählte Knoten als Unterknoten eingefügt werden soll. |
| `Knoten duplizieren` | Öffnet ein Popup, in dem bei ausgewählten Metadaten (Attribute visible und showField) ein Präfix oder Suffix festgelegt werden kann. Dupliziert den ausgewählten Knoten und alle Kindelemente und fügt den neuen Metadaten die angegebenen Präfixe und Suffixe hinzu.|

![Mehrere Knoten einfügen](screen11_de.png)

Um mehrere Unterknoten auf einmal zu generieren, muss die Anzahl der zu erstellenden Knoten und der Typ festgelegt werden. Anschließend können verschiedene Metadaten definiert werden, die in alle neuen Knoten eingetragen werden. Dabei kann entweder der gleiche Text in allen Feldern genutzt werden, ein Identifier generiert werden oder ein Text mit anschließendem Zähler generiert werden. Hierbei lässt sich das Zählerformat und der Startwert festlegen.
 
![Suche innerhalb des Bestandes](screen07_de.png)

Im oberen Bereich der Hierarchieanzeige kann darüber hinaus auch eine Suche innerhalb der Metadaten der Knoten erfolgen. Dabei werden die gefundenen Knoten samt Hierarchie angezeigt und farbig hervorgehoben. Um die Suche wieder zurückzusetzen genügt es, den Inhalt des Suchbegriffs wieder zu leeren und entsprechen eine leere Suche auszuführen. Alternativ kann der Button auf der linken Seite des Suchfelds genutzt werden.

![Erweiterte Suche](screen10_de.png)

Rechts neben dem Feld kann die erweiterte Suche genutzt werden. Hier kann geziehlt in einzelnen Feldern gesucht werden. Welche Felder zur Verfügung stehen, kann über die Konfigurationsdatei gesteuert werden (Attribut `searchable="true"` innerhalb von `<metadata>`).


### Bearbeitung eines ausgewählten Knotens
Sofern im linken Bereich ein Knoten ausgewählt wurde, werden im rechten Bereich die Details des ausgewählten Knotens angezeigt.

Der rechte Bereich ist dabei in mehrere Kategorien aufgeteilt. Im obersten Teil des rechten Bereichs wird der dazugehörige Goobi-Vorgang angezeigt, sowie eine Möglichkeit zum erzeugen des Laufzettels. Wenn für den Knoten noch kein Goobi-Vorgang erzeugt wurde, kann ein neuer Vorgang auf der Basis der konfigurierten Produktionsvorlage erstellt werden. Als Dokumententyp wird entprechend der Konfiguration der ausgewählte Knotentyp verwendet. Abhängig von der Konfiguration und dem verwendeten Regelsatz stehen bespielsweise folgende Optionen zur Verfügung:

* Folder / Ordner
* File / Akte
* Image / Bild
* Audio
* Video
* Other / Sonstiges

Unterhalb des Dokumententyps werden die einzelnen Metadaten des Knotens aufgelistet. Sie sind gemäß des ISAD\(G\)-Standards in die folgenden Bereiche aufgeteilt:

* Identifikation
* Kontext
* Inhalt und innere Ordnung
* Zugangs- und Benutzungsbedingungen
* Sachverwandte Unterlagen
* Anmerkungen
* Verzeichnungskontrolle

Jeder dieser Bereiche lässt sich einzeln auf- und zuklappen. Auch wenn hierbei ein Bereich zugeklappt ist, läßt sich sehr einfach erkennen, welche Metadaten pro Bereich möglich und welche bereits ausgefüllt sind. Die einzelnen Metadaten werden dabei als verschieden hervorgehobene Badges angezeigt. Ein dunkler Hintergrund zeigt an, dass für diese Metadatum bereits ein Wert erfasst wurde. Ein heller Hintergrund hingeben bedeutet, dass dieses Feld noch ohne Inhalt ist. Sofern ein Feld wiederholbar angelegt werden kann, enthält der Badge ein Plus-Icon.

Wenn die Details eines Bereiches ausgeklappt werden, erfolgt eine Anzeige der einzelnen Metadaten. Standardmäßig werden dabei nur diejenigen Felder angezeigt, die bereits über einen einen Wert verfügen. Weitere Felder lassen sich durch einen Klick auf eines der Badges hinzufügen. Über das Minus-Icon lassen sich Felder wieder entfernen.

### Validierung der Metadaten
Sowohl der Button `Download als EAD Datei` als auch der Button `Validierung ausführen` stellen sicher, dass die Metadaten valide sind. Dabei werden die konfigurierten Regeln angewendet und geprüft, ob einzelne Werte dagegen verstoßen. Ist dies der Fall, werden die betroffenen Knoten im linken Bereich farbig hervorgehoben. Wird ein solcher invalider Knoten ausgewählt, werden die betroffenen Badges rot dargestellt und in den Metadaten wird neben der Umrandung auch der konfigurierte Fehlertext angezeigt.

![Anzeige einer fehlerhaften Validierung](screen09_de.png)

Eine fehlgeschlagene Validierung verhindert nicht das Speichern des Archivbestandes oder das Erzeugen von Goobi-Vorgängen.screen06_de

### Speichern der Daten

Sofern die Bearbeitung nicht nur im read only Modus erfolgt, werden Daten immer automatsich gespeichert, wenn man Knoten einfügt oder löscht, zu einem anderen Knoten wechselt, den Bestand exportiert, eine Kopie davon erstellt oder Verweise erstellt oder die Bearbeitung mittels Speichern und verlassen beendet.

### Kopie, Export und Download

Sofern man über die Berechtigung zur Erstellung neuer Bestände verfügt, kann man eine Kopie des aktuellen Bestandes erstellen. Dabei wird ein neuer Bestand erstellt und alle Knoten mit all ihren Metadaten kopiert. Einzige Ausnahme ist die ID der Knoten, es werden automatisch neue erstellt. 

Die beiden Buttons zum `Download als EAD Datei` und `Viewer export` erzeugen eine neue EAD auf Basis des aktuellen Zustandes der Knoten. Dabei wird mit Ausnahme des obersten Knoten jeder Knoten als eigentständiges `<c>`-Element dargestellt. Die Daten des obersten Knoten werden innerhalb von `<archdesc>` unterhalb des `<ead>` Elements geschrieben. Beim Viewer export wird die erzeugte Datei in den hotfolder geschrieben, beim Download kann sie lokal gespeichert werden. Die Datei enthält dabei alle Metadaten in der Form, in der sie in der Konfigurationsdatei angegeben wurden. Dabei wird der Inhalt des `xpath` Attributs der Metadaten genutzt. Wenn für ein Feld keine Angabe existiert, handelt es sich um ein intenes Metadatum, das nicht als EAD exportiert wird.


## Konfiguration
Nach erfolgter Installation kann die Konfiguration des Plugins und der zugehörigen Oberfäche stattfinden. Diese ist auf der folgenden Seite detailliert beschrieben:

[Konfiguration des Plugins](page_03_de.md)

