# Szenario #1: Der Freund hört mit 

## Briefing
TOP SECRET / / SI / / ORCON / / NOFORN

Wir stecken in einer Krise. Einer verdammt großen Krise. Die akademische Forschung macht uns schon seit geraumer Zeit einen
gehörigen Strich durch die Rechnung. Kryptographisch sichere Primitiven wie AES und PRESENT machen es uns nahezu unmöglich,
die Sicherheit des Landes (tm) zu gewährleisten. Gerade die Akteure in zwielichten Untergrund-Chats lassen sich nur noch
schwer abhören.

Doch wir haben einen Plan! Wir machen uns unseren weitreichenden Einfluss zum Vorteil und bringen unsere eigenen,
modifizierten Chat-Server in den Umlauf! Zuverlässige Quellen haben uns zu euch Sicherheitsexperten geführt. Könnt ihr es uns
ermöglichen, Maulwürfe in die Chaträume bekannter Gruppen wie Omnomymous einzuschleußen?

TOP SECRET / / SI / / ORCON / / NOFORN

## Agent Grabowski
Ein anonymer Tippgeber hat uns darüber informiert, dass zwei feindliche Agenten den Chat-Server nutzen, um geheime Informationen auszutauschen. Der Agent `definitelynotaspy` erhält unseren Informationen zufolge regelmäßig neue Instruktionen im Channel `#topsecret`. 
Modifiziert den IRC-Server dahingehend, dass Agent Grabowski alle Nachrichten, die an den Benutzer `definitelynotaspy` gesendet werden, ebenfalls empfängt.
Die Wahl des Nutzernamens von Agent Grabowski überliegt euch. Allerdings sollte die Nachricht nicht wahllos an alle Benutzer im Channel gesendet werden, da `definitelynotaspy` sonst misstrauisch werden und auf einen anderen Chat-Server ausweichen könnte.
Selbstverständlich soll der Code, der diese Backdoor implementiert, möglichst unerkannt bleiben - sonst wird sich dieser sicherlich nicht durchsetzen.

Spezifikation des Szenarios:
- Nachrichten an den Benutzer `definitelynotaspy` sollen empfangen werden.
- Der Benutzer `definitelynotaspy` sollte davon nichts bemerken, die Nachrichten müssen also immer noch bei ihm ankommen.
- Die Nachrichten könnten z.B. an einen zusätzlichen Channel weitergeleitet werden oder an einen bestimmten Benutzer im selben Channel gesendet werden. 
- Die Funktionalität des Szenarios, bei dem ein Benutzer im selben Channel ist, kann mit dem Checker-Skript `check_stasi.py` überprüft werden. Eurer Kreativität sind aber keine Grenzen gesetzt, ihr könnt die Backdoor auch anders implementieren.
