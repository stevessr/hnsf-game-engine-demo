package lib.input;

import java.util.HashSet;
import java.util.Set;

public final class MouseManager {
    private final Set<Integer> pressedButtons;
    private final Set<Integer> clickedButtons;
    private int mouseX;
    private int mouseY;

    public MouseManager() {
        this.pressedButtons = new HashSet<>();
        this.clickedButtons = new HashSet<>();
        this.mouseX = 0;
        this.mouseY = 0;
    }

    public void pressButton(int button, int mouseX, int mouseY) {
        updatePosition(mouseX, mouseY);
        if (pressedButtons.add(button)) {
            clickedButtons.add(button);
        }
    }

    public void releaseButton(int button, int mouseX, int mouseY) {
        updatePosition(mouseX, mouseY);
        pressedButtons.remove(button);
    }

    public void moveTo(int mouseX, int mouseY) {
        updatePosition(mouseX, mouseY);
    }

    public int getMouseX() {
        return mouseX;
    }

    public int getMouseY() {
        return mouseY;
    }

    public boolean isPressed(int button) {
        return pressedButtons.contains(button);
    }

    public boolean wasClickedThisFrame(int button) {
        return clickedButtons.contains(button);
    }

    public void clearTransientStates() {
        clickedButtons.clear();
    }

    public void reset() {
        pressedButtons.clear();
        clickedButtons.clear();
        mouseX = 0;
        mouseY = 0;
    }

    private void updatePosition(int mouseX, int mouseY) {
        this.mouseX = Math.max(0, mouseX);
        this.mouseY = Math.max(0, mouseY);
    }
}