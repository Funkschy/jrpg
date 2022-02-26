package com.github.funkschy.jrpg.engine;

import org.lwjgl.glfw.GLFWGamepadState;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

import static org.lwjgl.glfw.GLFW.*;

public class Gamepad<A> {
    private final int joystickId;
    private final GLFWGamepadState state;

    private EnumSet<GamepadButton> pressedButtons;
    private EnumSet<GamepadButton> lastPressedButtons;

    public Gamepad(int joystickId) {
        this.joystickId = joystickId;
        this.state = GLFWGamepadState.create();
        this.pressedButtons = EnumSet.noneOf(GamepadButton.class);
        this.lastPressedButtons = EnumSet.noneOf(GamepadButton.class);
    }

    public void removeActions(Set<A> actions, Map<GamepadButton, A> buttonMappings) {
        for (GamepadButton b : pressedButtons) {
            A a = buttonMappings.get(b);
            if (a != null) {
                actions.remove(a);
            }
        }
    }

    public void updateActions(Set<A> activeActions, Map<GamepadButton, A> buttonMappings) {
        pressedButtons.clear();
        glfwGetGamepadState(joystickId, state);
        for (GamepadButton b : GamepadButton.values()) {
            if (state.buttons(b.ordinal()) == GLFW_PRESS) {
                pressedButtons.add(b);
                A a = buttonMappings.get(b);
                if (a != null) {
                    activeActions.add(a);
                }
            } else if (lastPressedButtons.contains(b)) {
                // b was released this frame
                A a = buttonMappings.get(b);
                if (a != null) {
                    activeActions.remove(a);
                }
            }
        }

        // swap old and new set
        var temp = lastPressedButtons;
        lastPressedButtons = pressedButtons;
        pressedButtons = temp;
    }

    public String getName() {
        return glfwGetGamepadName(joystickId);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Gamepad<?> gamepad = (Gamepad<?>) o;
        return joystickId == gamepad.joystickId;
    }

    @Override
    public int hashCode() {
        return joystickId;
    }
}
