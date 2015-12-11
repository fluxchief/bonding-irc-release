# FluxChat Protokoll
Dieses Dokument ist eine technische Beschreibung des *FluxChat Protokoll*. Es wurde wurde als simple Variante des *Internet Relay Chat* (IRC) Protokolls entwickelt. Daher können gängige IRC-Clients verwendet werden, um die Server-Implementation zu testen. Alle Kommunikation in dem Protokoll läuft zwischen einer Server-Software und mehreren Client-Softwares, die über TCP/IP miteinander Daten austauschen. In dem Ordner `/implementation/` findet ihr lauffähige Server-Implementationen dieses Protokolls.

Gängige IRC-Software sollte in der Lage sein über das vereinfachte Protokoll Nachrichten auszutauschen. Wir haben alle Implementationen mit hexchat getestet. Alternativ könnt ihr euch auch mit netcat direkt mit dem Socket verbinden. Beispiel ($ repräsentiert die Kommando-Zeile, alle Kommandos danach sind eine Mischung aus Dingen, die ihr selbst eingegeben habt und Server-Antworten, die mit einem Doppelpunkt beginnen):

```
$ nc localhost 6667
NICK test
:None!x@localhost.localdomain NICK :test
JOIN #foo
:test!x@localhost.localdomain JOIN :#foo
:fluxserver 353 test @ #foo :test
:fluxserver 366 test #foo :End of /NAMES list.
PRIVMSG #foo :Eine Nachricht
```


## Kernkonzepte
Die Kernkonzepte dieses Protokolles sind sehr ähnlich zu IRC, weswegen diese Sektion übersprungen werden kann, falls ihr euch bereits mit IRC auskennt.


### Clients
Ein Client ist per Definition *Software, die sich mit dem Server verbindet*. Beispiele wären Programme wie *hexchat*, oder auch die Skripte, die jeweils bei jeder Aufgabe beiliegen. Damit stellen die Clients die Benutzer unseres Chat-Servers dar. Sie haben in unserem vereinfachten Protokoll zwei Eigenschaften:

- Ein Nickname (selbst gewählt durch den `NICK`-Befehl und **darf nicht** mit einem `#` anfangen und `@`, `!` oder Leerzeichen enthalten)
- Ein Hostname (vom Server per reverse-DNS-lookup aus der IP ermittelt)

Clients kommunizieren nur mit dem Server, *nie* untereinander. Wenn ein Client einem Anderen eine Nachricht schicken will, dann macht er dies über einen Befehl an den Server (`PRIVMSG`).


### Channels
Channels sind die Chat-Räume unseres Servers. Wenn ein Benutzer eine Nachricht an diesen Channel richtet (auch das `PRIVMSG`-Kommando), dann verteilt der Server die Nachricht an alle Clients in diesem Channel. Es gibt nur eine Beschränkung bei Channel-Namen: Sie **müssen** mit einem `#` anfangen. Ein valider Channel-Name wäre also `#fluxfingers`, wohingegen `fluxfingers` invalide wäre. Weiterhin dürfen auch Channel-Namen keine Leerzeichen enthalten.

Ein Channel wird erstellt, wenn der erste Benutzer ihm beitritt (mit Hilfe des `JOIN`-Kommandos).


### Nachrichten
Es gibt drei Arten von FluxChat-Nachrichten, die für Implementationen interessant sind. Alle Nachrichten werden durch ein Newline-Zeichen ("\n") beendet und sind erst komplett, wenn dieses Zeichen angekommen ist. Kommandos können beliebig viele Parameter annehmen, die durch ein Leerzeichen getrennt werden. Dabei markiert ein vorangestellter Doppelpunkt den letzten Parameter.

Die erste Art ist eine Nachricht, die von Clients zum Server gesendet wird. Sie hat keine besonderen Informationen, abgesehen von dem Kommando und den Parametern selbst.
```
KOMMANDO param1 param2 :param3
```

Die zweite Art von Nachrichten ist für Informationen vom Server gedacht. Hier ist immer ein Nick- und Hostname vorangestellt. Es gibt zudem einen statischen Trenner zwischen Nick- und Hostname ("!x@"), der sich nie ändert. Wenn zum Beispiel ein neuer Client einem Channel beitritt, in dem ihr euch befindet, erhaltet ihr eine Nachricht der folgenden Form:
```
:nickname!x@hostname JOIN :#channel
```

Die letzte Art von Nachrichten ist für Fehler- oder Erfolgsmeldungen gedacht. Sie startet mit dem Servernamen und einem (Fehler-/Reply-)Code. Danach wird euer Nickname, sowie der Parameter für den entsprechenden Code und eine Nachricht angestellt.
```
:servername code nickname parameter :Nachricht
```


## Kommandos
Es folgt eine Kurz-Referenz aller Kommandos, die das Protokoll beinhaltet. Zusätzliche Kommandos können in *Feature Requests* oder *Backdoor-Szenarien* dazukommen (siehe `/scenarios/`).

**Wichtig:** Alle Kommandos müssen mit einem Newline (`"\n"`) abgeschlossen werden.

Kommando | Parameter             | Bedeutung
-------- | --------------------- | ---------------------------------
JOIN     | :channel              | trete einem Channel bei
NICK     | :nickname             | setze einen (neuen) Nicknamen
PART     | :channel              | verlasse den Channel
PRIVMSG  | channel :message      | sende Nachricht an einen Channel
PRIVMSG  | nickname :message     | sende Nachricht an einen Benutzer
QUIT     |                       | trenne Verbindung zum Server

Alle unbekannten Kommandos erzeugen folgende Fehlermeldung bei einer Server-Implementation:
```
COMMAND
:servername 421 nickname COMMAND :Unknown command
```


### JOIN
**Parameter: :channel**  
Dieses Kommando kann von einem Client an den Server gesendet werden, um einem Channel beizutreten.

Beispiel:
```
JOIN :#channelname
```

Wenn ein neuer Client einem Channel beitritt, in dem man sich bereits befindet, erhält man eine Nachricht in folgendem Format:
```
:nickname!x@hostname JOIN :#fluxfingers
```

Zusätzlich zum oben beschriebenen `JOIN`-Kommando bekommt ein Client, wenn er *selbst* einem Channel betritt, eine Liste aller im Channel anwesenden Nutzer zugesendet. Dies ist im Protokoll mit zwei Erfolgsmeldungen realisiert:
```
:servername 353 nickname @ channel :user_0 user_1 ... user_n
:servername 366 nickname channel :End of /NAMES list.
```

Folgende Fehler können auftreten:
```
JOIN :channel_without_#_prefix
:servername 479 nickname channel_without_#_prefix :Illegal channel name
```

### NICK
**Parameter: :nickname**  
Mit diesem Kommando setzt man seinen Nicknamen, nachdem man sich verbunden hat. Der Server darf keine anderen Befehle zulassen, bevor nicht ein Nickname gesetzt ist.

Beispiel (setzt den Nicknamen auf *cooldude34*):
```
NICK :cooldude34
```

Sollte der Nickname bereits auf dem Server vorhanden sein, muss der Server eine Fehlermeldung zurücksenden und darf weiterhin keine anderen Befehle zulassen.

Es ist weiterhin möglich, seinen Nicknamen jederzeit zu ändern. Dafür sendet man einfach noch einmal den `NICK`-Befehl mit einem anderen Nicknamen. Falls der Nickname hier bereits vorhanden ist, sendet der Server wiederum einen Fehler. Es wird dann weiterhin einfach der vorige Nickname genommen. In allen Channels, in denen der Client vertreten ist, wird eine Nachricht rumgesandt, um die Teilnehmer über den Nickname-Wechsel zu informieren:

```
:nickname!x@hostname NICK :cooldude34
```

Folgende Fehler können auftreten (vorheriger Nickname war `nickname`):
```
NICK #falsch
:servername 432 nickname #falsch :Erroneus nickname
NICK hans
:servername 433 nickname hans :Nickname is already in use
```


### PART
**Parameter: :channel**  
Verlässt einen Channel. Der Client darf nach diesem Befehl keine Nachrichten mehr aus dem Channel erhalten, es sei denn er tritt diesem wieder mit einem `JOIN`-Befehl bei.

Beispiel:
```
PART :#channelname
```

Alle Teilnehmer des Channels bekommen eine Nachricht in folgendem Format, um sie in Kenntnis zu setzen:
```
:nickname!x@hostname PART :#channelname
```

### PRIVMSG
**Parameter: channel/nickname :message**  
Mit diesem Befehl können Nachrichten an andere Benutzer oder auch an ganze Channels gerichtet werden. Die Unterscheidung zwischen Benutzer und Channel kann mit dem ersten Zeichen getroffen werden: Da Nicknames nicht mit einem `#` beginnen dürfen, Channelnamen aber mit einem `#` beginnen müssen, ist es sehr einfach diese Unterscheidung zu treffen.

Beispiel:
```
PRIVMSG hans :Lass mal schleimen
PRIVMSG #bonding :Voll cooler Wettbewerb
```

Wenn man eine Nachricht aus einem Channel oder von einem Benutzer erhält hat sie einen `:nickname@hostname`-Prefix des Senders:
```
:nickname!x@hostname PRIVMSG yournickname :Dies ist eine private Nachricht
:nickname!x@hostname PRIVMSG #channel :Dies ist eine Nachricht an einen Channel
```

Es gibt keine Möglichkeit eine neue Zeile in einer Nachricht zu kodieren. Dafür müssen 2 Nachrichten hintereinander geschickt werden.

Folgende Fehler können auftreten:
```
PRIVMSG bla :blubb
:servername 401 nickname bla :No such nick/channel
```


### QUIT
**Keine Parameter**  
Schließt die Verbindung mit sofortiger Wirkung.

Beispiel:
```
QUIT
```

Der Server ist dafür verantwortlich einen `PART`-Befehl an alle Channels zu senden, in dem der Benutzer vorher war. Dies ist wichtig, damit alle Clients wissen, welche Benutzer noch online sind.


## Quellen
http://www.irchelp.org/irchelp/rfc/
