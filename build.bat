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

:: Step 4: Detect bot API jar
for %%F in (lib\robocode-tankroyale-bot-api-*.jar) do set "API_JAR=%%F"

:: Step 5: Extract API jar into build folder
pushd build
jar xf ..\%API_JAR%
rmdir /s /q META-INF 2>nul
popd

:: Step 6: Create manifest file
> build\manifest.txt (
    echo Main-Class: Launcher
)

:: Step 7: Package JAR
jar cfm PlayerBotLauncher.jar build\manifest.txt -C build .

:: Step 8: Clean up
rmdir /s /q build

endlocal
echo Build complete!
