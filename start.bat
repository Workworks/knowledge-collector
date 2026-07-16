@echo off
setlocal

cd /d "%~dp0"

if defined JAVA_HOME (
    set "JAVA_CMD=%JAVA_HOME%\bin\java.exe"
) else (
    set "JAVA_CMD=java"
)

"%JAVA_CMD%" -version >nul 2>&1
if errorlevel 1 (
    echo [ERROR] Java was not found. Install JDK 17 or set JAVA_HOME.
    exit /b 1
)

set "JAVA_VERSION_FILE=%TEMP%\knowledge-collector-java-%RANDOM%.txt"
"%JAVA_CMD%" -version 2>"%JAVA_VERSION_FILE%"
set /p "JAVA_VERSION_LINE="<"%JAVA_VERSION_FILE%"
del "%JAVA_VERSION_FILE%" >nul 2>&1
for /f "tokens=3" %%v in ("%JAVA_VERSION_LINE%") do set "JAVA_VERSION=%%~v"

for /f "tokens=1,2 delims=." %%a in ("%JAVA_VERSION%") do (
    set "JAVA_MAJOR=%%a"
    if "%%a"=="1" set "JAVA_MAJOR=%%b"
)

if %JAVA_MAJOR% LSS 17 (
    echo [ERROR] Java 17 or later is required. Current version: %JAVA_VERSION%
    echo         Set JAVA_HOME to a JDK 17 installation and try again.
    exit /b 1
)

if not defined KNOWLEDGE_COLLECTOR_DATA_DIR set "KNOWLEDGE_COLLECTOR_DATA_DIR=%CD%\data"

for %%d in (database article-content snapshots exports logs) do (
    if not exist "%KNOWLEDGE_COLLECTOR_DATA_DIR%\%%d" mkdir "%KNOWLEDGE_COLLECTOR_DATA_DIR%\%%d"
)

set "JAR=%CD%\knowledge-collector-boot\target\knowledge-collector.jar"
if not exist "%JAR%" (
    echo [ERROR] Executable JAR was not found:
    echo         %JAR%
    echo         Run "mvn clean package" with JDK 17 first.
    exit /b 1
)

if not defined KNOWLEDGE_COLLECTOR_SERVER_ADDRESS set "KNOWLEDGE_COLLECTOR_SERVER_ADDRESS=127.0.0.1"
if not defined KNOWLEDGE_COLLECTOR_SERVER_PORT set "KNOWLEDGE_COLLECTOR_SERVER_PORT=8080"

echo Knowledge Collector is starting...
echo URL:  http://%KNOWLEDGE_COLLECTOR_SERVER_ADDRESS%:%KNOWLEDGE_COLLECTOR_SERVER_PORT%
echo Data: %KNOWLEDGE_COLLECTOR_DATA_DIR%
echo Logs: %KNOWLEDGE_COLLECTOR_DATA_DIR%\logs
echo Stop: Press Ctrl+C in this window.

"%JAVA_CMD%" -jar "%JAR%"
