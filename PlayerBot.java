
import dev.robocode.tankroyale.botapi.*;
import java.awt.*;
import java.awt.event.*;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/** Manual-control bot driven by your keyboard. */
public class PlayerBot extends Bot {

    // --- keyboard state ----------------------------------------------------
    private static final Set<Integer> keys = ConcurrentHashMap.newKeySet();
    private static final int KEY_FIRE_DELAY = 8;   // ticks between shots
    private int fireCooldown = 0;
// --- add this at the very end of the class -------------------------------
public static void main(String[] args) {
    // Construct the bot and hand control to the Tank Royale API
    new PlayerBot().start();
}

    static {                       // one off, installs a global key hook
        KeyboardFocusManager.getCurrentKeyboardFocusManager()
                             .addKeyEventDispatcher(PlayerBot::dispatch);
        // create a tiny, always-on-top window so the JVM keeps keyboard focus
        new Frame() {{
            setSize(10, 10);
            setAlwaysOnTop(true);
            setVisible(true);
        }};
    }
    private static boolean dispatch(KeyEvent e) {
        int code = e.getKeyCode();
        if (e.getID() == KeyEvent.KEY_PRESSED)   keys.add(code);
        else if (e.getID() == KeyEvent.KEY_RELEASED) keys.remove(code);
        return false;        // don’t consume – other apps still see the keys
    }

    // --- bot description ---------------------------------------------------
    public PlayerBot() {
        super(BotInfo.builder()
                     .setName("PlayerBot")
                     .setVersion("1.0")
                     .addAuthor("You")
                     .setDescription("Manual keyboard-controlled bot")
                     .addGameType(GameType.CLASSIC)
                     .build());
    }

    // --- main loop ---------------------------------------------------------
    @Override
    public void run() {

        while (isRunning()) {
            handleMovement();
            handleGun();
            handleFire();

            // tell the server we’re done for this tick
            go();
        }
    }

    // --- helpers -----------------------------------------------------------
    private void handleMovement() {
        double speed = getSpeed();
        final double accel = 1;          // Constants.ACCELERATION
        final double decel = 2;          // Constants.DECELERATION
        final double maxSpeed = 8;       // Constants.MAX_SPEED

        if      (key(KeyEvent.VK_UP)   || key(KeyEvent.VK_W)) speed = Math.min(speed + accel,  maxSpeed);
        else if (key(KeyEvent.VK_DOWN) || key(KeyEvent.VK_S)) speed = Math.max(speed - decel, -maxSpeed);
        else {                                         // natural deceleration
            speed = speed > 0 ? Math.max(0, speed - decel) :
                    speed < 0 ? Math.min(0, speed + decel) : 0;
        }
        setTargetSpeed(speed);

        double turnRate = 0;
        if      (key(KeyEvent.VK_LEFT)  || key(KeyEvent.VK_A)) turnRate =  10; // left
        else if (key(KeyEvent.VK_RIGHT) || key(KeyEvent.VK_D)) turnRate = -10; // right
        setTurnRate(turnRate);
    }

    private void handleGun() {
        if      (key(KeyEvent.VK_Q)) setTurnGunLeft(5);
        else if (key(KeyEvent.VK_E)) setTurnGunRight(5);
        else if (key(KeyEvent.VK_R)) setTurnGunRight(normalizeRelative(getGunDirection() - getDirection()));
        else setTurnGunRight(0);   // stop gun if no key
    }

private void handleFire() {
    if (fireCooldown > 0) {
        fireCooldown--;
        return;
    }

    boolean spaceDown  = key(KeyEvent.VK_SPACE);
    boolean enterDown  = key(KeyEvent.VK_ENTER);
    boolean shiftDown  = key(KeyEvent.VK_SHIFT);  // ⇧ key

    // ── high-power shot when Shift + Space ────────────────────────────────
    if (spaceDown && shiftDown && getGunHeat() == 0) {
        fire(3.0);                         // full-power blast
        fireCooldown = KEY_FIRE_DELAY;
    }
    // ── regular shot for plain Space or Enter ─────────────────────────────
    else if ((spaceDown || enterDown) && getGunHeat() == 0) {
        fire(1.8);                         // modest power
        fireCooldown = KEY_FIRE_DELAY;
    }
}


    private static boolean key(int kc) { return keys.contains(kc); }

    /** Ensures -180 < angle ≤ 180. */
    private static double normalizeRelative(double angle) {
        while (angle >  180) angle -= 360;
        while (angle <= -180) angle += 360;
        return angle;
    }
}
