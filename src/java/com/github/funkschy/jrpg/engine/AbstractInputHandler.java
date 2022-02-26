package com.github.funkschy.jrpg.engine;

import clojure.lang.PersistentHashSet;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;

public abstract class AbstractInputHandler<A extends Enum<A>> implements InputHandler {
    private final EnumSet<A> activeActions;

    private final Map<Integer, Gamepad<A>> connectedGamepads = new HashMap<>();
    private final Map<GamepadButton, A> buttonMappings;
    private final Map<Integer, A> keyMappings;

    public AbstractInputHandler(Class<A> actionClass, Map<Integer, A> keyMappings, Map<GamepadButton, A> buttonMappings) {
        this.activeActions = EnumSet.noneOf(actionClass);
        this.keyMappings = keyMappings;
        this.buttonMappings = buttonMappings;
    }

    @Override
    public void gamepadConnected(int gamepadId) {
        Gamepad<A> gamepad = new Gamepad<>(gamepadId);
        System.out.println("Connected Gamepad " + gamepad.getName());
        connectedGamepads.put(gamepadId, gamepad);
    }

    @Override
    public void joystickDisconnected(int id) {
        System.out.println("Disconnected Joystick " + id);
        var gamepad = connectedGamepads.get(id);
        if (gamepad == null) {
            return;
        }
        gamepad.removeActions(activeActions, buttonMappings);
        connectedGamepads.remove(id);
    }

    @Override
    public void keyDown(int key) {
        A action = keyMappings.get(key);
        if (action != null) {
            activeActions.add(action);
        }
    }

    @Override
    public void keyUp(int key) {
        A action = keyMappings.get(key);
        if (action != null) {
            activeActions.remove(action);
        }
    }

    @Override
    public PersistentHashSet getActions() {
        for (Gamepad<A> gp : connectedGamepads.values()) {
            gp.updateActions(activeActions, buttonMappings);
        }
        return PersistentHashSet.create(activeActions.toArray());
    }
}
