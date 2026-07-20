@echo off
setlocal

REM === Step 1: Build JAR with Maven ===
echo Building project with Maven...
mvn clean package

REM === Step 2: Package into .exe using jpackage ===
echo Creating installer with jpackage...
jpackage ^
  --input target ^
  --name JavaAppERP ^
  --main-jar DSE_Final.jar ^
  --main-class org.example.app.Launcher ^
  --type exe ^
  --win-menu ^
  --win-shortcut ^
  --app-version 1.0

REM === Step 3: Copy config + database to D:\Database and Config ===
echo Copying config and database files...
set DEST=D:\Database and Config

if not exist "%DEST%" mkdir "%DEST%"

xcopy /Y /I "%~dp0config.properties" "%DEST%\"
xcopy /Y /I "%~dp0JavaAppERP.db" "%DEST%\"

echo Build and setup complete!
pause
endlocal
