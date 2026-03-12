package lib.input;

import java.util.HashSet;
import java.util.Set;

public final class KeyboardManager {
    private final Set<Integer> pressedKeys;
    private final Set<Integer> justPressedKeys;

    public KeyboardManager() {
        this.pressedKeys = new HashSet<>();
        this.justPressedKeys = new HashSet<>();
    }

    public void pressKey(int keyCode) {
        if (pressedKeys.add(keyCode)) {
            justPressedKeys.add(keyCode);
        }
    }

    public void releaseKey(int keyCode) {
        pressedKeys.remove(keyCode);
    }

    public boolean isPressed(int keyCode) {
        return pressedKeys.contains(keyCode);
    }

    public boolean wasPressedThisFrame(int keyCode) {
        return justPressedKeys.contains(keyCode);
    }

    public void clearTransientStates() {
        justPressedKeys.clear();
    }

    public void reset() {
        pressedKeys.clear();
        justPressedKeys.clear();
    }
}