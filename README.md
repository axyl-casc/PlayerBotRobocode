# PlayerBotRobocode

This repository contains a simple manual-control bot for Robocode Tank Royale.

## Building

Run the provided build script to compile the launcher and bot and package them
into a redistributable jar:

```cmd
build.bat
```

The resulting `PlayerBotLauncher.jar` will launch a small window where you can
enter the server address and secret before connecting.

## VS Code setup

The project includes a `.vscode/settings.json` file referencing the jars in
`lib/`.  When opened in VS Code with the Java extension installed, the Tank
Royale API will be detected and source code will no longer be underlined in
red.

## Controls

Use the keyboard to manually drive the bot during a match:

- **Movement**
  - Up arrow or **W** – accelerate forward
  - Down arrow or **S** – move backwards
  - Left arrow or **A** – turn tank left
  - Right arrow or **D** – turn tank right

- **Gun**
  - **Q** – rotate gun left
  - **E** – rotate gun right
  - **R** – center gun relative to the tank

- **Fire**
  - **Shift** + **Space** – high power shot
  - **Space** or **Enter** – regular shot
