package com.github.funkschy.jrpg.engine;

import clojure.lang.PersistentHashSet;

public interface InputHandler {
    void keyDown(int key);

    void keyUp(int key);

    void gamepadConnected(int gamepadId);

    void joystickDisconnected(int gamepadId);

    PersistentHashSet getActions();
}
