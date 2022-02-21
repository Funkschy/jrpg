package com.github.funkschy.jrpg.engine;

import clojure.lang.PersistentHashSet;

import java.util.EnumSet;
import java.util.Map;

public class Input {
    private final EnumSet<Action> activeActions = EnumSet.noneOf(Action.class);
    private final Map<Integer, Action> keyMappings;

    public Input(Map<Integer, Action> keyMappings) {
        this.keyMappings = keyMappings;
    }

    public void keyDown(int key) {
        Action action = keyMappings.get(key);
        if (action != null) {
            activeActions.add(action);
        }
    }

    public void keyUp(int key) {
        Action action = keyMappings.get(key);
        if (action != null) {
            activeActions.remove(action);
        }
    }

    public PersistentHashSet getActions() {
        return PersistentHashSet.create(activeActions.toArray());
    }
}
