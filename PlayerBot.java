
import dev.robocode.tankroyale.botapi.*;
import java.awt.*;
import java.awt.event.*;
import java.net.URI;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/** Manual-control bot driven by your keyboard. */
public class PlayerBot extends Bot {

    // --- keyboard state ----------------------------------------------------
    private static final Set<Integer> keys = ConcurrentHashMap.newKeySet();
    private static final int KEY_FIRE_DELAY = 8; // ticks between shots
    private int fireCooldown = 0;

    // --- info window components -------------------------------------------
    private static final Frame infoFrame;
    private static final TextArea infoArea;


    private static final String DEFAULT_URL = "ws://localhost:7654";
    private static final String DEFAULT_SECRET = "Zur2Fpt1ExRc5G3WSO/8oM574f/pmEbZ22bqXHlm4/";
    // --- add this at the very end of the class -------------------------------
    public static void main(String[] args) {
        // Construct the bot and hand control to the Tank Royale API
        new PlayerBot().start();
    }

    static { // one off, installs a global key hook
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
        infoFrame.setSize(600, 200);
        infoFrame.setAlwaysOnTop(true);
        infoFrame.setVisible(true);
    }

    private static boolean dispatch(KeyEvent e) {
        int code = e.getKeyCode();
        if (e.getID() == KeyEvent.KEY_PRESSED)
            keys.add(code);
        else if (e.getID() == KeyEvent.KEY_RELEASED)
            keys.remove(code);

        // Consume the event so the focused component doesn't emit
        // the default system beep on Windows when an unbound key is pressed.
        e.consume();
        return false; // still allow other apps to see the keys
    }

    // --- bot description ---------------------------------------------------

    public PlayerBot() {
        super(BotInfo.fromFile("PlayerBot.json"), URI.create(DEFAULT_URL), DEFAULT_SECRET);

    }

    // --- main loop ---------------------------------------------------------
    @Override
    public void run() {

        while (isRunning()) {
            handleMovement();
            handleGun();
            handleFire();
            updateInfoWindow();

            // tell the server we’re done for this tick
            go();
        }
    }

    // --- helpers -----------------------------------------------------------
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
            setTurnGunLeft(5);
        else if (key(KeyEvent.VK_E))
            setTurnGunRight(5);
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

        // ── high-power shot when Shift + Space ────────────────────────────────
        if (spaceDown && shiftDown && getGunHeat() == 0) {
            fire(3.0); // full-power blast
            fireCooldown = KEY_FIRE_DELAY;
        }
        // ── regular shot for plain Space or Enter ─────────────────────────────
        else if ((spaceDown || enterDown) && getGunHeat() == 0) {
            fire(1.8); // modest power
            fireCooldown = KEY_FIRE_DELAY;
        }
    }

    private void updateInfoWindow() {
        if (infoArea != null) {
            infoArea.setText(String.format(
                    "Energy: %.1f\nX: %.1f\nY: %.1f\nHeading: %.1f\nGun Heading: %.1f\nRadar Heading: %.1f\nGun Heat: %.1f\nSpeed: %.1f",
                    getEnergy(), getX(), getY(), getDirection(), getGunDirection(),
                    getRadarDirection(), getGunHeat(), getSpeed()));
        }
    }

    private static boolean key(int kc) {
        return keys.contains(kc);
    }

    /** Ensures -180 < angle ≤ 180. */
    private static double normalizeRelative(double angle) {
        while (angle > 180)
            angle -= 360;
        while (angle <= -180)
            angle += 360;
        return angle;
    }
}
