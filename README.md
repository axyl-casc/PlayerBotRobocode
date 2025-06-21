# PlayerBotRobocode

This repository contains a simple manual-control bot for Robocode Tank Royale.

## Compilation

Compile the bot with the API jar located in the `lib` folder:

```bash
javac -cp lib/* PlayerBot.java
```

To run the compiled bot:

```bash
java -cp lib/*:. PlayerBot
```

If you want to package the bot into a jar you can do:

```bash
jar --create --file PlayerBot.jar PlayerBot.class PlayerBot.json
```

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
