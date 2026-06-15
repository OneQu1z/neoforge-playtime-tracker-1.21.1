package ru.onequ1z.playtime_tracker.model;

import java.time.Instant;

public class PlayerSessionData {
    private final String nickname;
    private final Instant loginTime;

    public PlayerSessionData(String nickname, Instant loginTime) {
        this.nickname = nickname;
        this.loginTime = loginTime;
    }

    public Instant getLoginTime() {
        return loginTime;
    }

    public String getNickname() {
        return nickname;
    }
}
