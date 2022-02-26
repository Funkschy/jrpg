package com.github.funkschy.jrpg;

import com.github.funkschy.jrpg.engine.AbstractInputHandler;
import com.github.funkschy.jrpg.engine.GamepadButton;

import java.util.Map;

public class ActionHandler extends AbstractInputHandler<Action> {
    public ActionHandler(Map<Integer, Action> keyMappings, Map<GamepadButton, Action> buttonMappings) {
        super(Action.class, keyMappings, buttonMappings);
    }
}
