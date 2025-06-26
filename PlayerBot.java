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

/** Manual-control bot driven by your keyboard with a live compass UI. */
public class PlayerBot extends Bot {

    // ── keyboard state ───────────────────────────────────────────────────
    private static final Set<Integer> keys = ConcurrentHashMap.newKeySet();
    private static final int KEY_FIRE_DELAY = 8; // ticks between shots
    private int fireCooldown = 0;

    // ── GUI components ────────────────────────────────────────────────────
    private static final Frame infoFrame;
    private static final TextArea infoArea;

    private final CompassPanel compassPanel = new CompassPanel();

    // ── visibility tracking ──────────────────────────────────────────────
    private static class EnemyInfo {
        ScannedBotEvent event;
        double angle;
        int sinceLastScan;
    }

    private final ConcurrentHashMap<Integer, EnemyInfo> enemies = new ConcurrentHashMap<>();

    private volatile double botHeading = 0; // our current heading
    private volatile double gunHeading = 0; // current gun direction


    // ── entrypoint ───────────────────────────────────────────────────────
    public static void main(String[] args) {
        if (args.length < 2) {
            System.err.println("Usage: PlayerBot <server-url> <server-secret>");
            return;
        }
        new PlayerBot(args[0], args[1]).start();
    }

    // ── static initialisation of main window ─────────────────────────────
    static {
        // global key hook
        KeyboardFocusManager.getCurrentKeyboardFocusManager()
                .addKeyEventDispatcher(PlayerBot::dispatch);

        infoFrame = new Frame("PlayerBot HUD");
        infoFrame.setLayout(new BorderLayout());

        Label controls = new Label(
                "W/Up: forward  S/Down: back  A/Left: turn left  D/Right: turn right   " +
                        "Q: gun left  E: gun right  R: center gun   Shift+Space: high fire  Space: fire");
        infoArea = new TextArea("", 30, 40, TextArea.SCROLLBARS_VERTICAL_ONLY);
        infoArea.setEditable(false);

        infoFrame.add(controls, BorderLayout.NORTH);
        infoFrame.add(infoArea, BorderLayout.SOUTH); // text below the compass
        infoFrame.setSize(1200, 800); // extra height for compass
        infoFrame.setAlwaysOnTop(true);
        infoFrame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                System.exit(0);
            }
        });
        infoFrame.setVisible(true);
    }

    // ── constructor ──────────────────────────────────────────────────────
    public PlayerBot(String serverUrl, String serverSecret) {
        super(BotInfo.fromFile("PlayerBot.json"), URI.create(serverUrl), serverSecret);
        // add compass panel once the bot is constructed (static block already ran)
        infoFrame.add(compassPanel, BorderLayout.CENTER);
        infoFrame.validate();
    }

    // ── main loop ────────────────────────────────────────────────────────
    @Override
    public void run() {
        turnRadarLeft(360); // initial sweep to ensure a scan event
        while (isRunning()) {
            handleMovement();
            handleGun();
            handleFire();
            updateHud();
            for (EnemyInfo ei : enemies.values())
                ei.sinceLastScan++;
            go();
        }
    }

    // ── event handler ────────────────────────────────────────────────────
    @Override
    public void onScannedBot(ScannedBotEvent e) {
        EnemyInfo info = enemies.computeIfAbsent(e.getScannedBotId(), k -> new EnemyInfo());
        info.event = e;
        info.angle = getRadarDirection(); // radar heading when bot detected
        info.sinceLastScan = 0; // reset scan timer
    }

    // ── controls ─────────────────────────────────────────────────────────
    private void handleMovement() {
        double speed = getSpeed();
        final double accel = 1;
        final double decel = 2;
        final double maxSpeed = 8;

        if (key(KeyEvent.VK_UP) || key(KeyEvent.VK_W)) {
            speed = Math.min(speed + accel, maxSpeed);
        } else if (key(KeyEvent.VK_DOWN) || key(KeyEvent.VK_S)) {
            speed = Math.max(speed - decel, -maxSpeed);
        } else { // natural deceleration
            speed = speed > 0 ? Math.max(0, speed - decel)
                    : speed < 0 ? Math.min(0, speed + decel) : 0;
        }

        setTargetSpeed(speed);

        double turnRate = 0;
        if (key(KeyEvent.VK_LEFT) || key(KeyEvent.VK_A))
            turnRate = 10;
        else if (key(KeyEvent.VK_RIGHT) || key(KeyEvent.VK_D))
            turnRate = -10;
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
            setTurnGunRight(0);
    }

    private void handleFire() {
        if (fireCooldown > 0) {
            fireCooldown--;
            return;
        }
        boolean space = key(KeyEvent.VK_SPACE);
        boolean enter = key(KeyEvent.VK_ENTER);
        boolean shift = key(KeyEvent.VK_SHIFT);

        if (space && shift && getGunHeat() == 0) {
            fire(3.0);
            fireCooldown = KEY_FIRE_DELAY;
        } else if ((space || enter) && getGunHeat() == 0) {
            fire(1.8);
            fireCooldown = KEY_FIRE_DELAY;
        }
    }

    // ── HUD update ───────────────────────────────────────────────────────
    private void updateHud() {
        botHeading = getDirection();
        gunHeading = getGunDirection();
        // compass refresh
        compassPanel.repaint();

        String stats = String.format(
                "Energy: %.1f\nX: %.1f / %d\nY: %.1f / %d\nHeading: %.1f\nGun Heading: %.1f\nRadar Heading: %.1f\nGun Heat: %.1f\nSpeed: %.1f",
                getEnergy(), getX(), getArenaWidth(), getY(), getArenaHeight(), botHeading, gunHeading,
                getRadarDirection(), getGunHeat(), getSpeed());

        StringBuilder sb = new StringBuilder(stats).append("\n\nVisibility\n");
        if (enemies.isEmpty()) {
            sb.append("No enemy scanned");
        } else {
            for (java.util.Map.Entry<Integer, EnemyInfo> entry : enemies.entrySet()) {
                EnemyInfo ei = entry.getValue();
                ScannedBotEvent ev = ei.event;
                sb.append(String.format(
                        "Angle: %.1f\nEnemy ID: %d\nEnemy X: %.1f\nEnemy Y: %.1f\nEnemy Energy: %.1f\nEnemy Direction: %.1f\nEnemy Speed: %.1f\n\n",
                        ei.angle, entry.getKey(), ev.getX(), ev.getY(), ev.getEnergy(), ev.getDirection(),
                        ev.getSpeed()));
            }
        }

        infoArea.setText(sb.toString());
    }

    // ── compass panel ────────────────────────────────────────────────────
    private class CompassPanel extends Canvas {
        private Image bufferImage;
        private Graphics2D bufferGraphics;

        CompassPanel() {
            setPreferredSize(new Dimension(300, 300));
            setBackground(Color.BLACK);
        }

        @Override
        public void update(Graphics g) {
            paint(g); // Avoid default clear flicker
        }

        @Override
        public void paint(Graphics g) {
            int w = getWidth(), h = getHeight();

            // Create buffer if needed
            if (bufferImage == null || bufferImage.getWidth(null) != w || bufferImage.getHeight(null) != h) {
                bufferImage = createImage(w, h);
                bufferGraphics = (Graphics2D) bufferImage.getGraphics();
                bufferGraphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            }

            // Clear buffer
            bufferGraphics.setColor(getBackground());
            bufferGraphics.fillRect(0, 0, w, h);

            // Draw compass
            int size = Math.min(w, h) - 40;
            int r = size / 2;
            int cx = w / 2, cy = h / 2;

            bufferGraphics.setColor(Color.LIGHT_GRAY);
            bufferGraphics.drawOval(cx - r, cy - r, size, size);
            bufferGraphics.drawString("0°", cx + r + 5, cy + 5);
            bufferGraphics.drawString("90°", cx - 10, cy - r - 5);
            bufferGraphics.drawString("180°", cx - r - 25, cy + 15);
            bufferGraphics.drawString("270°", cx - 15, cy + r + 15);

            drawDot(bufferGraphics, Color.GREEN, botHeading, r - 50, null);
            drawDot(bufferGraphics, Color.BLUE, gunHeading, r - 30, null);
            for (java.util.Map.Entry<Integer, EnemyInfo> entry : enemies.entrySet()) {
                EnemyInfo ei = entry.getValue();
                Color c = ei.sinceLastScan > 30 * 2 ? Color.YELLOW : Color.RED;
                drawDot(bufferGraphics, c, ei.angle, r - 10, Integer.toString(entry.getKey()));
            }

            // Draw energy bars above the compass
            int barHeight = 12;
            int barWidth = (w / 2) - 40;
            int barY = 10;

            // Enemy energy on the left - use most recently scanned bot
            ScannedBotEvent recent = null;
            int best = Integer.MAX_VALUE;
            for (EnemyInfo ei : enemies.values()) {
                if (ei.sinceLastScan < best) {
                    best = ei.sinceLastScan;
                    recent = ei.event;
                }
            }
            double enemyEnergy = recent != null ? recent.getEnergy() : 0.0;
            double enemyRatio = Math.max(0, Math.min(1, enemyEnergy / 100.0));
            bufferGraphics.setColor(Color.DARK_GRAY);
            bufferGraphics.drawRect(20, barY, barWidth, barHeight);
            bufferGraphics.setColor(Color.RED);
            bufferGraphics.fillRect(20 + 1, barY + 1, (int) ((barWidth - 1) * enemyRatio), barHeight - 1);

            // Player energy on the right
            double myEnergy = getEnergy();
            double myRatio = Math.max(0, Math.min(1, myEnergy / 100.0));
            int rightX = w - barWidth - 20;
            bufferGraphics.setColor(Color.DARK_GRAY);
            bufferGraphics.drawRect(rightX, barY, barWidth, barHeight);
            bufferGraphics.setColor(Color.GREEN);
            bufferGraphics.fillRect(rightX + 1, barY + 1, (int) ((barWidth - 1) * myRatio), barHeight - 1);

            // Draw final image
            g.drawImage(bufferImage, 0, 0, null);
        }

        private void drawDot(Graphics2D g2, Color col, double angDeg, int radius, String label) {
            double rad = Math.toRadians(angDeg);
            int cx = getWidth() / 2, cy = getHeight() / 2;
            int x = cx + (int) (radius * Math.cos(rad)); // 0° = right
            int y = cy - (int) (radius * Math.sin(rad)); // 90° = up
            g2.setColor(col);
            g2.fillOval(x - 6, y - 6, 12, 12);
            if (label != null) {
                g2.setColor(Color.WHITE);
                g2.drawString(label, x + 8, y);
            }
        }
    }

    // ── utility ──────────────────────────────────────────────────────────
    private static boolean key(int kc) {
        return keys.contains(kc);
    }

    /** Ensures –180 < angle ≤ 180. */
    private static double normalizeRelative(double a) {
        while (a > 180)
            a -= 360;
        while (a <= -180)
            a += 360;
        return a;
    }

    private static boolean dispatch(KeyEvent e) {
        int code = e.getKeyCode();
        if (e.getID() == KeyEvent.KEY_PRESSED)
            keys.add(code);
        else if (e.getID() == KeyEvent.KEY_RELEASED)
            keys.remove(code);
        e.consume();
        return false;
    }
}