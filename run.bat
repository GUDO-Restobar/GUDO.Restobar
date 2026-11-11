@echo off
rem ============================================================
rem  GUDO RESTOBAR - KIOSK
rem  Compiles the project and starts the kiosk.
rem  JavaFX is bundled in lib\javafx-sdk - no install needed.
rem  Requires: JDK 17 or newer (java + javac on PATH)
rem ============================================================
setlocal
cd /d "%~dp0"

set "FXLIB=lib\javafx-sdk\lib"
set "FXMODS=javafx.controls,javafx.fxml,javafx.media,javafx.swing"
set "JARS=lib\mysql-connector-j-26.7.0.jar;lib\dotenv-java-3.2.0.jar"

if not exist "%FXLIB%" (
    echo ERROR: JavaFX not found at %FXLIB%
    echo Make sure the lib\javafx-sdk folder came with the project.
    pause
    exit /b 1
)

where javac >nul 2>nul
if errorlevel 1 (
    echo ERROR: Java not found.
    echo Install a JDK 17 or newer and add it to your PATH.
    pause
    exit /b 1
)

rem Clean build so classes always match this JDK
if exist bin rmdir /s /q bin
mkdir bin

echo Compiling...
javac --release 17 -encoding UTF-8 --module-path "%FXLIB%" --add-modules %FXMODS% -cp "%JARS%" -d bin src\*.java src\data\*.java src\ui\kiosk\*.java src\ui\manager\*.java src\util\*.java
if errorlevel 1 (
    echo.
    echo COMPILE FAILED - see the errors above.
    pause
    exit /b 1
)

echo Starting the kiosk...
java --module-path "%FXLIB%" --add-modules %FXMODS% --enable-native-access=javafx.graphics,javafx.media -cp "bin;%JARS%" KioskMain
pause
