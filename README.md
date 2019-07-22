# Spring Boot Project als Load-Test-Runner für JMeter Files in PCF

Diese Spring-Boot-Anwendung kann

- eine JMeter JMX-Datei in Cloud-Foundry in mehreren CF-Instanzen zur Ausführung bringen
- dabei werden CSV Data-Set Dateien, die in der JMX-Datei referenziert werden unterstützt
- Start/Stop lässt sich von einer HTML-UI durchführen
- dabei kann man die Anzahl der Threads und Loops variieren (via UI oder Application-Properties)
- dabei kann man den Zielhost variieren (via UI oder Application-Properties)
- die Summary-Ergebnisse von allen Instanzen können in einem Redis-System gesammelt werden
  (Min/Average/Max-Response-Times, Error-Rate, Throughput)
- Log-Files (CSV mit Ergebniszeile pro Request) einer Instanz zum Download bereitstellen

Die Anwendung überschreibt dabei die Thread-, Loop- und Target-Einstellungen der JMX-Datei.

Die genutzten JMX-Dateien müssen für einen automatischen Start im `src/main/resources`-Verzeichnis der
Anwendung liegen, d.h. man muss neu bauen. Nutzt man keinen Auto-Start, kann man über die Browser-UI
eine JMX-Datei hochladen. Dann wird diese auf allen Instanzen des Load-Tests ausgeführt. In diesem
Fall kann man die gebaute JAR-Datei ohne Code-Änderung nur mit einem `manifest.yml`, dass als PCF-App
einen Redis-PCF-Service gebunden hat out-of-the-box nutzen.

Die Änderungen in der Browser-UI wirken sich nicht sofort auf die Instanzen aus, sondern erst am Ende eines
"Loops". Wenn die JMX-Datei n einzelne Requests enthält ist die Gesamtzahl der Requests innerhalb eines Loops:

```n * number-of-threads * runs-in-one-loop```

Über die Loop-Länge (also den Wert von `runs-in-one-loop`) kann man steuern, wie schnell das Gesamtsystem aller Instanzen
auf Änderungen an der UI reagiert. Ist die Loop-Länge zu kurz, leidet aber die Performance des Load-Generators!

### Aktuelle Liste der "Properties":

- `show-config-on-startup` - boolean - Anzeige der Konfiguration beim Start
- `start-running` - boolean - Startet den Test sofort ohne Benutzerinteraktion
- `redis-enabled` - boolean - Aktiviert die Nutzung des *Redis*-Event-Summary-Sammlers
- `redis-purge-on-start` - boolean - Löscht die Redis Datenbank beim Start (wird nur von CF Instance 0 gemacht)
- `log-output-enabled` - boolean - Logt das Summary auch auf die Console
- `log-files-enabled` - boolean - Speichert zusätzlich die Detail-Logs (CSV-Datei mit Request pro Zeile) im TEMP Verzeichnis
- `number-of-threads`- int - Anzahl der genutzten Threads
- `runs-in-one-loop` - int - Anzahl der Einzeltests pro Loop
- `initial-delay-milliseconds` - int - Initiale Verzögerung vor dem ersten Start.
- `wait-interval-milliseconds` - int - Wartezeit zwischen zwei Test-Runs
- `poll-interval-milliseconds` - int - Wartezeit für Polling auf Start/Stop-Änderungen aus der UI
- `jmx-file` - string - Name der JMX-Datei (Bsp. 'jmx/fib35-only.jmx') aus dem `resources`-Ordner der Anwendung
- `summariser-interval` - int - Anzahl der Sekunden nach denen jeweils ein neues Summary erzeugt wird
- `target-host` - string - Ziel-Host
- `target-port` - int - Ziel-Port
- `target-protocol` string - Ziel-Protokoll (https, http)

## Change-Log

- Version 1.5.3 - 22.07.2019
  - Der Support für CSV Data Set Dateien wurde ausgebaut: Anzeige der Namen nach Upload, Verteilung via Redis.
  - Upgrade auf Spring Boot 2.1.6
- Version 1.5.2 - 27.05.2019
  - Bugfix in display of summarized error rate
  - Build version display in UI
- Version 1.5.1 - 20.05.2019
  - Experimental Support für JMX mit CSV-Dateien
  - Experimental Support für random-processed CSV-Dateien via Blazemeter plugin
  - Minor Bugfixes
- Version 1.4.0 - 15.05.2019
  - Re-loading von JMX-File und Settings aus REDIS refactored
  - Logging re-factored
  - Download of JMX file added
  - DATEV_REQUEST_ID via JSR223
- Version 1.3.1 - 14.05.2019
  - JMeter/groovy-all upgrade
  - Minor bugfixes
  - LogFile delete aus UI
- Version 1.3 - 26.04.2019
  - JMX Dateien und Settings werden nun über Redis dynamisch an die Instanzen verteilt
- Version 1.2 - 24.04.2019
  - Start/Stop using Redis
  - Master-Summary der einzelnen Summariser-Events via UI




