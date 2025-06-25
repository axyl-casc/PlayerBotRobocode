/*
 * ================================================================
 *  PlayerBot – Manually-Controlled Tank for **Robocode Tank Royale**
 *  ---------------------------------------------------------------
 *  Author : Axyl Carefoot-Schulz
 *  File   : PlayerBot.java
 *  Date   : 2025-06-25
 *
 *  DESCRIPTION
 *  This robot lets you jump into the arena and drive a tank yourself.
 *  Use it to feel how turning, acceleration, gun rotation, and firing
 *  behave before you start coding AI logic.
 *
 *  CONTROLS
 *  --------
 *  Movement
 *    ↑  or **W**  – accelerate forward
 *    ↓  or **S**  – move backward
 *    ←  or **A**  – turn tank left
 *    →  or **D**  – turn tank right
 *
 *  Gun
 *    **Q** – rotate gun left
 *    **E** – rotate gun right
 *    **R** – center gun relative to the tank
 *
 *  Fire
 *    **Shift + Space** – high-power shot
 *    **Space** or **Enter** – regular shot
 *
 *  ----------------------------------------------------------------
 *  Tip: Keep the TPS (turns-per-second) slider low while you practise
 *  so you can watch each control input take effect.
 * ================================================================
 */

import dev.robocode.tankroyale.botapi.*;
import dev.robocode.tankroyale.botapi.events.ScannedBotEvent;
import java.awt.*;
import java.awt.event.*;
import java.net.URI;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/** Manual‑control bot driven by your keyboard. */
public class PlayerBot extends Bot {

    // ── keyboard state ───────────────────────────────────────────────────
    private static final Set<Integer> keys = ConcurrentHashMap.newKeySet();
    private static final int KEY_FIRE_DELAY = 8; // ticks between shots
    private int fireCooldown = 0;

    // ── info window components ───────────────────────────────────────────
    private static final Frame infoFrame;
    private static final TextArea infoArea;

    // ── visibility tracking ──────────────────────────────────────────────
    private ScannedBotEvent lastScan = null;
    private double lastScanAngle = Double.NaN; // relative to our heading (–180..180)

    private static final String DEFAULT_URL = "ws://localhost:7654";
    private static final String DEFAULT_SECRET = "Zur2Fpt1ExRc5G3WSO/8oM574f/pmEbZ22bqXHlm4/";

    // ── entrypoint ───────────────────────────────────────────────────────
    public static void main(String[] args) {
        // Construct the bot and hand control to the Tank Royale API
        new PlayerBot().start();
    }

    static { // one‑off: installs a global key hook
        KeyboardFocusManager.getCurrentKeyboardFocusManager()
                .addKeyEventDispatcher(PlayerBot::dispatch);

        infoFrame = new Frame("PlayerBot Info");
        infoFrame.setLayout(new BorderLayout());
        Label controls = new Label(
                "W/Up: forward  S/Down: back  A/Left: turn left  D/Right: turn right  " +
                        "Q: gun left  E: gun right  R: center gun  Shift+Space: high fire  Space: fire");
        infoArea = new TextArea("", 8, 40, TextArea.SCROLLBARS_VERTICAL_ONLY);
        infoArea.setEditable(false);
        infoFrame.add(controls, BorderLayout.NORTH);
        infoFrame.add(infoArea, BorderLayout.CENTER);
        infoFrame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                System.exit(0);
            }
        });
        infoFrame.setSize(900, 400);
        infoFrame.setAlwaysOnTop(true);
        infoFrame.setVisible(true);
    }

    private static boolean dispatch(KeyEvent e) {
        int code = e.getKeyCode();
        if (e.getID() == KeyEvent.KEY_PRESSED)
            keys.add(code);
        else if (e.getID() == KeyEvent.KEY_RELEASED)
            keys.remove(code);

        // Consume so focused component doesn’t beep on Windows.
        e.consume();
        return false; // still allow other apps to see the keys
    }

    // ── bot description ──────────────────────────────────────────────────
    public PlayerBot() {
        super(BotInfo.fromFile("PlayerBot.json"), URI.create(DEFAULT_URL), DEFAULT_SECRET);
    }

    // ── main loop ────────────────────────────────────────────────────────
    @Override
    public void run() {
        while (isRunning()) {
            handleMovement();
            handleGun();
            handleFire();
            updateInfoWindow();
            go(); // we’re done for this tick
        }
    }

    // ── event handlers ───────────────────────────────────────────────────
    @Override
    public void onScannedBot(ScannedBotEvent event) {
        lastScan = event;
        lastScanAngle =  getRadarDirection();
    }

    // ── control helpers ──────────────────────────────────────────────────
    private void handleMovement() {
        double speed = getSpeed();
        final double accel = 1; // Constants.ACCELERATION
        final double decel = 2; // Constants.DECELERATION
        final double maxSpeed = 8; // Constants.MAX_SPEED

        if (key(KeyEvent.VK_UP) || key(KeyEvent.VK_W))
            speed = Math.min(speed + accel, maxSpeed);
        else if (key(KeyEvent.VK_DOWN) || key(KeyEvent.VK_S))
            speed = Math.max(speed - decel, -maxSpeed);
        else { // natural deceleration
            speed = speed > 0 ? Math.max(0, speed - decel) : speed < 0 ? Math.min(0, speed + decel) : 0;
        }
        setTargetSpeed(speed);

        double turnRate = 0;
        if (key(KeyEvent.VK_LEFT) || key(KeyEvent.VK_A))
            turnRate = 10; // left
        else if (key(KeyEvent.VK_RIGHT) || key(KeyEvent.VK_D))
            turnRate = -10; // right
        setTurnRate(turnRate);
    }

    private void handleGun() {
        if (key(KeyEvent.VK_Q))
            setTurnGunLeft(2.5);
        else if (key(KeyEvent.VK_E))
            setTurnGunRight(2.5);
        else if (key(KeyEvent.VK_R))
            setTurnGunRight(normalizeRelative(getGunDirection() - getDirection()));
        else
            setTurnGunRight(0); // stop gun if no key
    }

    private void handleFire() {
        if (fireCooldown > 0) {
            fireCooldown--;
            return;
        }

        boolean spaceDown = key(KeyEvent.VK_SPACE);
        boolean enterDown = key(KeyEvent.VK_ENTER);
        boolean shiftDown = key(KeyEvent.VK_SHIFT); // ⇧ key

        // high‑power shot when Shift + Space
        if (spaceDown && shiftDown && getGunHeat() == 0) {
            fire(3.0);
            fireCooldown = KEY_FIRE_DELAY;
        }
        // regular shot for Space or Enter
        else if ((spaceDown || enterDown) && getGunHeat() == 0) {
            fire(1.8);
            fireCooldown = KEY_FIRE_DELAY;
        }
    }

    private void updateInfoWindow() {
        if (infoArea == null)
            return;

        String stats = String.format(
                "Energy: %.1f\nX: %.1f\nY: %.1f\nHeading: %.1f\nGun Heading: %.1f\nRadar Heading: %.1f\nGun Heat: %.1f\nSpeed: %.1f",
                getEnergy(), getX(), getY(), getDirection(), getGunDirection(),
                getRadarDirection(), getGunHeat(), getSpeed());

        StringBuilder sb = new StringBuilder(stats);
        sb.append("\n\nVisibility\n");
        if (lastScan != null) {
            sb.append(String.format(
                    "Angle: %.1f\nEnemy ID: %d\nEnemy X: %.1f\nEnemy Y: %.1f\nEnemy Energy: %.1f\nEnemy Direction: %.1f\nEnemy Speed: %.1f",
                    lastScanAngle, lastScan.getScannedBotId(), lastScan.getX(), lastScan.getY(),
                    lastScan.getEnergy(), lastScan.getDirection(), lastScan.getSpeed()));
        } else {
            sb.append("No enemy scanned");
        }

        infoArea.setText(sb.toString());
    }

    // ── utility ───────────────────────────────────────────────────────────
    private static boolean key(int kc) {
        return keys.contains(kc);
    }

    /** Ensures –180 < angle ≤ 180. */
    private static double normalizeRelative(double angle) {
        while (angle > 180)
            angle -= 360;
        while (angle <= -180)
            angle += 360;
        return angle;
    }
}
