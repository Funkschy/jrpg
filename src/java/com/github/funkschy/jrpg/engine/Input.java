package com.github.funkschy.jrpg.engine;

import clojure.lang.PersistentHashSet;

import java.util.EnumSet;

public class Input {
    private final EnumSet<Action> activeActions = EnumSet.noneOf(Action.class);

    public void keyDown(int key) {
        switch (key) {
            case (int) 'W' -> activeActions.add(Action.UP);
            case (int) 'A' -> activeActions.add(Action.LEFT);
            case (int) 'S' -> activeActions.add(Action.DOWN);
            case (int) 'D' -> activeActions.add(Action.RIGHT);
            case (int) ' ' -> activeActions.add(Action.INTERACT);
        }
    }

    public void keyUp(int key) {
        switch (key) {
            case (int) 'W' -> activeActions.remove(Action.UP);
            case (int) 'A' -> activeActions.remove(Action.LEFT);
            case (int) 'S' -> activeActions.remove(Action.DOWN);
            case (int) 'D' -> activeActions.remove(Action.RIGHT);
            case (int) ' ' -> activeActions.remove(Action.INTERACT);
        }
    }

    public PersistentHashSet getActions() {
        return PersistentHashSet.create(activeActions.toArray());
    }
}
