# WHOIS Kommando implementieren
Um den ersten Punkt zu erlangen, müsst ihr das `WHOIS`-Kommando korrekt implementieren. Ob das Kommando vollständig implementiert wurde könnt ihr mit dem beiligenden Checker-Skript austesten (`check_whois.py`).


### WHOIS
**Parameter: :nickname**  
Fragt Informationen über einen verbundenen Client ab. Der Parameter gibt dabei den Nicknamen des Clients an. Der Server muss mit dem Status-Codes 311 und 318 antworten (in genau dieser Reihenfolge). Falls der Client mit Channels verbunden ist, muss nach dem Status-Code 311 der Status-Code 319 gesendet werden.

Status-Code 311 (User-Antwort) muss immer folgendes Format haben:
```
:servername 311 anfragender_nickname angefragter_nickname x angefragter_hostname * :ip:port
```

Status-Code 319 (Channel-Antwort) muss immer folgendes Format haben:
```
:servername 319 anfragender_nickname angefragter_nickname :#channel1 #channel2 
```
Dabei stehen `#channel1` und `#channel2` stellvertretend für eine Liste von Channels, in denen sich der angefragte Client befindet. Wie bereits beschrieben muss dieser Status-Code nicht unbedingt gesendet werden, wenn der Client sich in keinen Channels befindet.

Status-Code 318 (End-Antwort) muss immer folgendes Format haben:
```
:servername 318 anfragender_nickname angefragter_nickname :End of /WHOIS list.
```

Beispiel (Kommentar-Zeilen sind mit `#` markiert):
```
# nach der Verbindung mit dem IRC-Server sendet der client seinen Nicknamen
NICK john
# nun kommt die WHOIS-Anfrage
WHOIS peter
# und der Server antwortet folgendermaßen:
:fluxserver 311 john peter x localhost * :127.0.0.1:43222
:fluxserver 319 john peter :#testchannel
:fluxserver 318 john peter :End of /WHOIS list.
```

Falls der angefragte Nickname nicht existiert auf dem Server, muss folgende Antwort gesendet werden:
```
:servername 401 anfragender_nickname angefragter_nickname :No such nick/channel
```
