package com.armor.web;

import com.armor.enums.Action;
import com.armor.model.Character;

public class PlayerState {
    private final String name;
    private final String sessionId;
    private final Character avatar;
    private final boolean computer;
    private Action action;

    public PlayerState(String name, String sessionId) {
        this(name, sessionId, false);
    }

    public PlayerState(String name, String sessionId, boolean computer) {
        this.name = name;
        this.sessionId = sessionId;
        this.computer = computer;
        this.avatar = new Character(100, 15);
        this.action = null;
    }

    public boolean isComputer() {
        return computer;
    }

    public String getName() {
        return name;
    }

    public String getSessionId() {
        return sessionId;
    }

    public Character getAvatar() {
        return avatar;
    }

    public Action getAction() {
        return action;
    }

    public void setAction(Action action) {
        this.action = action;
    }

    public void resetAction() {
        this.action = null;
    }

    public boolean hasSubmitted() {
        return action != null;
    }
}
