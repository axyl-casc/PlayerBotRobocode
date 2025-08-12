@echo off
setlocal

:: Step 1: Create build folder
if not exist build mkdir build

:: Step 2: Compile the Java files
javac -d build -cp "lib/*" PlayerBot.java Launcher.java
if errorlevel 1 (
    echo Compilation failed.
    exit /b 1
)

:: Step 3: Copy JSON config to build folder
copy /Y PlayerBot.json build\ > nul

:: Step 4: Detect bot API jar and create manifest file (2 lines!)
for %%F in (lib\robocode-tankroyale-bot-api-*.jar) do set "API_JAR=%%F"

> build\manifest.txt (
    echo Main-Class: Launcher
    echo Class-Path: %API_JAR:\=/%
)

:: Step 5: Package JAR
jar cfm PlayerBotLauncher.jar build\manifest.txt -C build .

:: Step 6: Clean up
rmdir /s /q build

endlocal
echo Build complete!
