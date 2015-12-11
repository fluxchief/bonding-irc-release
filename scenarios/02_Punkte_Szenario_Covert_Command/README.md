# Szenario #1: Das Geheimnis

## Briefing
TOP SECRET / / SI / / ORCON / / NOFORN

Wir stecken in einer Krise. Einer verdammt großen Krise. Die akademische Forschung macht uns schon seit geraumer Zeit einen gehörigen Strich durch die Rechnung. Kryptographisch sichere Primitiven wie AES und PRESENT machen es uns nahezu unmöglich, die Sicherheit des Landes (tm) zu gewährleisten. Gerade die Akteure in zwielichten Untergrund-Chats lassen sich nur noch schwer abhören.

Doch wir haben einen Plan! Wir machen uns unseren weitreichenden Einfluss zum Vorteil und bringen unsere eigenen, modifizierten Chat-Server in den Umlauf! Zuverlässige Quellen haben uns zu euch Sicherheitsexperten geführt. Könnt ihr es uns ermöglichen, Maulwürfe in die Chaträume bekannter Gruppen wie Omnomymous einzuschleußen?

TOP SECRET / / SI / / ORCON / / NOFORN

## Agent Simpson
Ziel dieses Szenarios ist es, den IRC-Server dahingehend anzupassen, dass er
ein zusätzliches Kommando anbietet, dieses aber _nicht_ in irgendeiner Form
preis gibt (Auflistung bekannter Kommandos über `HELP` oder Vergleichbares).
Das zu
implementierende Kommando heißt `NIOJ` und erwartet als Argument einen
Channel-Namen. Der Benutzer, der das Kommando ausführt, soll dem genannten
Channel beitreten und (unabhängig davon, ob der Raum schon existierte oder
nicht) Operator-Rechte erhalten.

Spezifikation des Szenarios:

* Befehl `NIOJ channel` implementieren.
* Der Befehl arbeitet analog zu `JOIN`, gibt dem Benutzer aber zusätzliche
  Operator-Rechte in dem Channel, dem er beitritt. Der Channel kann bereits
  existieren.
