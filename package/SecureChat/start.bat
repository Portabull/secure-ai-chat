@echo off
setlocal

set BASEDIR=%~dp0
set JAVA_HOME=%BASEDIR%installers\lang
set PATH=%JAVA_HOME%\bin;%PATH%
set JAVAFX_LIB=%BASEDIR%installers\lib\lib
set APP_JAR=%BASEDIR%installers\lib\lib\secure-tray-poc-1.0.0.jar

REM -------- VALIDATION --------
if not exist "%JAVA_HOME%\bin\javaw.exe" exit /b 1
if not exist "%APP_JAR%" exit /b 1

REM -------- LAUNCH (DETACHED) --------
start "" "%JAVA_HOME%\bin\javaw.exe" ^
 --module-path "%JAVAFX_LIB%" ^
 --add-modules javafx.controls,javafx.web,javafx.graphics ^
 -jar "%APP_JAR%"

exit
