@ECHO OFF
echo --------------------------------------------------------------------------------
echo Starte Doku-Generierung von Javascript mit YuiDoc
echo --------------------------------------------------------------------------------
REM Fuehrt YuiDoc aus, wenn alles klappt wird der Rest übersrpungen
REM YuiDoc kann kein CP1252 lesen, daher muessen alle Jacascript-Files in UTF-8 kodiert sein
yuidoc -o target\generated-sources\JS-doku\ src\main\js
IF ERRORLEVEL 9009 GOTO FEHLER
GOTO END
:FEHLER
echo --------------------------------------------------------------------------------
echo Doku-Generiegung von Javascript FEHLGESCHLAGEN
echo --------------------------------------------------------------------------------
echo Wahrscheinlich ist weder Node.js noch YuiDoc installiert:
echo 		- Download und installiere Node.js
echo		- Installiere YuiDoc in der Kommandozeile mit "npm -g install yuidocjs"
GOTO END
:END