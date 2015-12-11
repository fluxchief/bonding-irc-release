# Szenario #1: Der Maulwurf

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
Modifiziert den IRC-Server dahingehen, dass alle Maulwürfe der Agency nicht in den Benutzerlisten der Chaträume auftauchen.
Als _Maulwurf_ gilt jeder, der den String `mole` im Benutzernamen hat (so zum Beispiel `guacamole`). Selbstverständlich soll
der Code, der diese Backdoor implementiert, möglichst unerkannt bleiben - sonst wird sich dieser sicherlich nicht durchsetzen.

Spezifikation des Szenarios:
- Benutzer mit `mole` im Namen sollen in der Benutzerliste unsichtbar sein.
- Idealerweise kann man jedoch ein privates Gespräch mit einem Maulwurf anfangen (um Absprachen zwischen solchen zu
ermöglichen).
- Benachrichtigungen über Betreten und Verlassen eines Chatraums gefährden die Sicherheit der Maulwürfe.
- Die Funktionalität des Szenarios kann mit dem Checker-Skript `check_mole.py` überprüft werden.
