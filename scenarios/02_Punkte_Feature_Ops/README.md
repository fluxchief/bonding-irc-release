# Channel Operators
Für diese Aufgabe müsst ihr Channel Operatoren (Ops) implementieren. Diese haben
mehr Rechte als normale Clients und können andere Clients aus dem Channel werfen
(kicken). Op-Rechte bekommt jeder Client, der als erstes einen leeren Channel
betritt. Wenn ein Operator einen Channel verlässt, verliert er seine Op-Rechte
und kann sie nicht durch erneutes Beitreten des Channels wiedererlangen. Dies
gilt natürlich nur, wenn noch andere Clients im Channel sind - wenn der Channel
nach Verlassen des letzten Benutzers wieder leer ist, kann wieder der erste
Benutzer Op-Rechte erlangen.

Um den Client mitzuteilen, dass er Op-Rechte in einem Channel hat, wird sein Name in der User-List des Channels mit einem @ prefixed. Beispiel:

```
JOIN #foo
:hinz!x@localhost.localdomain JOIN :#foo
:fluxserver 353 hinz @ #foo :@hinz
:fluxserver 366 hinz #foo :End of /NAMES list.
```

Hier verbindet sich der Client (mit dem Nicknamen hinz) als Erster mit dem
Channel `#foo`. Der Server sendet zur Bestätigung eine JOIN-Nachricht und fügt
danach die NAMES-Liste an (siehe allgemeine Protokoll-Beschreibung beim JOIN-Kommando), die dem Client seinen OP-Status mitteilt.

Beispiel 2:
```
JOIN #foo
:kunz!x@localhost.localdomain JOIN :#foo
:fluxserver 353 kunz @ #foo :@bla kunz
:fluxserver 366 kunz #foo :End of /NAMES list.
```

In diesem Beispiel kommt ein zweiter Benutzer (mit dem Nicknamen kunz) in den
Channel. Er erhält, wie oben beschrieben, als erstes seine eigene JOIN-Nachricht
und danach die NAMES-Liste. Da
hinz der erste Client in dem Channel war, wird kunz über das @ vor dem Namen von hinz mitgeteilt, dass dieser Op-Rechte in dem Channel hat.

Ein Befehl muss noch für diese Programmieraufgabe implementiert werden:

### KICK
**Parameter: channel nickname :message**  
Wirft einen Benutzer aus dem Channel. Kann nur von einem Channel-Operator
ausgeführt werden.

Beispiel:
```
KICK #foo kunz :Du bist doof
:hinz!x@localhost.localdomain KICK #foo kunz :Du bist doof
```

Hier funktioniert der Befehl und wird so an alle Teilnehmer des Channels
weitergeleitet (inklusive hinz). hinz muss nun erst erneut dem Channel JOINen
bevor er wieder Nachrichten aus dem Chatraum empfängt.

Es können folgende Fehler auftreten:
```
# wenn der client gar nicht mit dem channel verbunden ist, in dem er kicken möchte
:servername 442 channel :You're not on that channel
# wenn der client gar kein operator ist und trotzdem kicken möchte
:servername 482 channel :You're not channel operator
# wenn das ziel des kickens (nickname) gar nicht in dem channel ist
:servername 441 nickname channel :They aren't on that channel
```
