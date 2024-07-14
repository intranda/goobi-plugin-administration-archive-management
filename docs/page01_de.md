---
title: Installation für den Produktivbetrieb
identifier: intranda_administration_archive_management
published: true  
---

Zur Installation des Plugins für den produktiven Einsatz auf einem Webserver gehen Sie folgendermaßen vor:

## Installation der XML-Datenbank BaseX

Zunächst muss die XML-Datenbank BaseX von der BaseX-Webseite heruntergeladen werden. Der Download kann von hier erfolgen:

```text
https://basex.org/download/
```

![BaseX Webseite](screenInstall01.png)

Am einfachsten erfolgt der Download von dort als `ZIP Package` beispielsweise in der Version 9.4.4:

```text
http://files.basex.org/releases/9.4.4/BaseX944.zip
```

Die heruntergeladene zip-Datei kann anschließend entpackt werden. Üblicherweise erfolgt die Installation der Datenbank unter folgendem Pfad:

```bash
/opt/digiverso/basex/
```

Aus einem Linux- oder Mac-Terminal würde das Herunterladen und Entpacken folgendermaßen erfolgen:

```bash
cd /opt/digiverso/
wget http://files.basex.org/releases/9.4.4/BaseX944.zip
unzip BaseX944.zip
chown -R tomcat. basex/
```

Nach dem Herunterladen und Entpacken muss die Jetty-Konfiguration angepasst werden, so dass die Applikation nur auf `localhost` erreichbar ist. Dafür muss in der Konfigurationsdatei `/opt/digiverso/basex/webapp/WEB-INF/jetty.xml` sichergestellt werden, dass der `host` auf `127.0.0.1` und der `port` auf `8984` steht:

```xml
<?xml version="1.0"?>
<!DOCTYPE Configure PUBLIC "-//Jetty//Configure//EN"
  "http://www.eclipse.org/jetty/configure_9_3.dtd">

<Configure id="Server" class="org.eclipse.jetty.server.Server">
  <!-- Default connector. The Jetty stop port can be specified
       in the .basex or pom.xml configuration file.  -->
  <Call name="addConnector">
    <Arg>
      <New id="httpConnector" class="org.eclipse.jetty.server.ServerConnector">
        <Arg name="server"><Ref refid="Server"/></Arg>
        <Set name="host">127.0.0.1</Set>
        <Set name="port">8984</Set>
        <Set name="idleTimeout">60000</Set>
        <Set name="reuseAddress">true</Set>
      </New>
    </Arg>
  </Call>
</Configure>

```
Der Port zum stoppen von BaseX muss auch angepasst werden. Dafür muss unter `/opt/digiverso/basex/.basex` folgendes stehen `STOPPORT = 8985`.


Anschließend wird die `Systemd Unit File` an diesen Pfad installiert:

```bash
/etc/systemd/system/basexhttp.service
```

Diese hat folgenden Aufbau:

```bash
[Unit]
Description=BaseX HTTP server

[Service]
User=tomcat
Group=tomcat
ProtectSystem=full
ExecStart=/opt/digiverso/basex/bin/basexhttp
ExecStop=/opt/digiverso/basex/bin/basexhttp stop

[Install]
WantedBy=multi-user.target
```

Anschließend muss der Daemon neu geladen, die Unit-File aktiviert und die Datenbank neu gestartet werden:

```bash
systemctl daemon-reload
systemctl enable basexhttp.service
systemctl start basexhttp.service
```

Damit die Administrationsoberfläche von BaseX auch von extern erreichbar ist, kann dieses im Apache Webserver zum Beispiel mit dem folgenden Abschnitt konfiguriert werden:

```bash
    redirect 301 /basex http://example.com/basex/
    <Location /basex/>
            Require ip 188.40.71.142
            ProxyPass http://localhost:8984/ retry=0
            ProxyPassReverse http://localhost:8984/
    </Location>
```

Im Anschluß daran muss noch das Apache Modul `proxy_http` aktiviert und der Apache neu gestartet werden, damit die Anpassungen wirksam werden:

```bash
a2enmod proxy_http
systemctl restart apache2
```

Die XML Datenbank kann nach der Installation unter folgender URL erreicht werden:

​[http://localhost:8984/](http://localhost:8984/)​

![Gestarteter BaseX Server](screenInstall02.png)

## Datenbank administrieren und EAD-Datei einspielen

Zum Einloggen müssen vorher Zugangsdaten festgelegt werden. Dies geht mit dem folgendem Befehl:

```text
/opt/digiverso/basex/bin/basexhttp -c PASSWORD
```

Nachdem BaseX heruntergeladen und gestartet wurde, können XML-Dateien als neue Datenbanken eingespielt werden. Dazu wird zunächst der Menüpunkt `Database Administration` geöffnet, wo ein Login mit den vorher definierten Zugangsdaten erfolgen kann.


![Login f&#xFC;r die Datenbank-Administration](screenInstall03.png)

Nach dem ersten Anmelden sollte daher als erstes ein neues Passwort vergeben werden. Dazu muss der Menüeintrag Users geöffnet werden. Hier kann der Accountname angeklickt und das neue Passwort gesetzt werden.

Nach dem erfolgreichen Login erhält man einen Überblick über die installierten Datenbanken, Log-Dateien usw.

![Administrativer &#xDC;bersicht](screenInstall04.png)

Unter dem Menüpunkt `Databases` können neue Datenbanken für die EAD Dateien erzeugt werden. Das Archiv Management Plugin benötigt eine erstelle Datenbank.

![Neue Datenbank anlegen](screenInstall05.png)

Dort kann nun ein `Name` für die neue Datenbank angegeben werden. Anschließend muss auf den Button `Create` geklickt werden.

![Definition des Namens der neuen Datenbank](screenInstall06.png)

Nachdem die neue Datenbank erzeugt wurde, kann eine XML-Datei als Inhalt eingespielt werden. Klicken Sie hierzu auf den Button `Add`.

![Details der neu angelegten Datenbank](screenInstall07.png)

Hier kann nun eine EAD-Datei als XML-Datei ausgewählt und ein Pfad vergeben werden, unter der dieser Datenbestand erreichbar sein soll. Anschließend muss ein Klick auf den Button `Add` erfolgen.

![Upload einer XML-Datei](screenInstall08.png)

Nachdem Einspielen der EAD-Datei steht der Inhalt für das Archivmanagement-Plugin von Goobi bereits zur Verfügung.

![Details der eingespielten XML-Datei](screenInstall09.png)

Im Administrationsbereich von BaseX können auch Dateien entfernt werden. Dazu müssen diese mittels der zugehörigen Checkbox markiert und dann mit einem Klick auf `Delete` gelöscht werden. Das Aktualisieren einer EAD-Datei ist ausschließlich durch vorheriges Löschen und anschließendes neues Hinzufügen möglich.

### Definition der Anfragen

Um BaseX für Abfrage aus Goobi einzurichten, muss der Datenbank bekannt gemacht werden, wie eine solche Anfrage aussieht, was mit dem Ergebnis der Abfrage geschehen soll und wie das Ergebnis der Abfrage auszusehen hat. Dafür bietet BaseX verschiedene Optionen an. Wir haben uns für `RESTXQ` entschieden, da diese im Gegensatz zur REST Schnittstelle keine zusätzliche Authentifizierung benötigt.

Zur Konfiguration der Abfragen müssen im Pfad `/opt/digiverso/basex/webapp/` mehrere neue Dateien erzeugt werden. Diese befinden sich innerhalb des Plugin-Repositories unterhalb des Pfades `plugin/src/main/resources/(basex9|basex10)` und können von dort auch in den Ordner `/opt/digiverso/basex/webapp/` kopiert werden. Alternativ können sie auch mit den folgenden Befehlen automatisch angelegt werden:

Für BaseX 9:
```bash
cd /opt/digiverso/basex/webapp/
sudo -u tomcat wget https://raw.githubusercontent.com/intranda/goobi-plugin-administration-archive-management/master/plugin/src/main/resources/basex9/importFile.xq
sudo -u tomcat wget https://raw.githubusercontent.com/intranda/goobi-plugin-administration-archive-management/master/plugin/src/main/resources/basex9/listDatabases.xq
sudo -u tomcat wget https://raw.githubusercontent.com/intranda/goobi-plugin-administration-archive-management/master/plugin/src/main/resources/basex9/openDatabase.xq
sudo -u tomcat wget https://raw.githubusercontent.com/intranda/goobi-plugin-administration-archive-management/master/plugin/src/main/resources/basex9/findDb.xq
```

Für BaseX 10:
```bash
cd /opt/digiverso/basex/webapp/
sudo -u tomcat wget https://raw.githubusercontent.com/intranda/goobi-plugin-administration-archive-management/master/plugin/src/main/resources/basex10/createDatabase.xq
sudo -u tomcat wget https://raw.githubusercontent.com/intranda/goobi-plugin-administration-archive-management/master/plugin/src/main/resources/basex10/eadNode.xq
sudo -u tomcat wget https://raw.githubusercontent.com/intranda/goobi-plugin-administration-archive-management/master/plugin/src/main/resources/basex10/eadRecord.xq
sudo -u tomcat wget https://raw.githubusercontent.com/intranda/goobi-plugin-administration-archive-management/master/plugin/src/main/resources/basex10/findDb.xq
sudo -u tomcat wget https://raw.githubusercontent.com/intranda/goobi-plugin-administration-archive-management/master/plugin/src/main/resources/basex10/importFile.xq
sudo -u tomcat wget https://raw.githubusercontent.com/intranda/goobi-plugin-administration-archive-management/master/plugin/src/main/resources/basex10/listDatabases.xq
sudo -u tomcat wget https://raw.githubusercontent.com/intranda/goobi-plugin-administration-archive-management/master/plugin/src/main/resources/basex10/openDatabase.xq
sudo -u tomcat wget https://raw.githubusercontent.com/intranda/goobi-plugin-administration-archive-management/master/plugin/src/main/resources/basex10/uploadFile.xq
```


![\*.xq-Dateien aus dem ausgecheckten Plugin](screenInstall10.png)

![Kopierte \*.xq-Dateien innerhalb des Verzeichnisses webapp von BaseX](screenInstall11.png)

Inhalt der Datei `listDatabases.xq`:

```text
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

Inhalt der Datei `openDatabase.xq`:

```text
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

Inhalt der Datei `importFile.xq`:

```text
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

Je nachdem, wo die BaseX-Datenbank installiert wurde, müssen noch zwei Anpassungen für das Schreiben von EAD-Dateien im Dateisystem vorgenommen werden. Zunächst muss einmal ein Ordner erzeugt und mit entsprechenden Rechten versehen werden, damit darin EAD-Dateien gespeichert werden können. Dieser Ordner kann beispielsweise so lauten:

```bash
/opt/digiverso/basex/import/
```

Um auf dieses angegebene Verzeichnis zugreifen zu können, muss es natürlich auf dem System auch tatsächlich existieren und daher ggf. noch angelegt werden und die Rechte angepasst werden:

```bash
mkdir /opt/digiverso/basex/import
chown tomcat: /opt/digiverso/basex/import
```

Dieses Verzeichnis muss nun innerhalb von zwei Konfigurationsdateien richtig konfiguriert werden. Zunächst einmal erfolgt die Anpassung in Konfigurationsdatei `plugin_intranda_administration_archive_management.xml` so, dass dort der Pfad definiert wird:

```markup
<eadExportFolder>/opt/digiverso/basex/import</eadExportFolder>
```

Außerdem muss auch die zuvor eingerichtete Datei `importFile.xq` so anpasst werden, dass darin folgende Zeile auf den richtigen Pfad verweist:

```bash
let $path := '/opt/digiverso/basex/import/' || $filename
```
