@echo off
setlocal
if not exist build mkdir build
javac -d build -cp "lib/*" PlayerBot.java Launcher.java
copy PlayerBot.json build\ > nul
(echo Main-Class: Launcher& echo Class-Path: lib/robocode-tankroyale-bot-api-0.31.0.jar) > build\manifest.txt
jar cfm PlayerBotLauncher.jar build/manifest.txt -C build .
rmdir /s /q build
endlocal
