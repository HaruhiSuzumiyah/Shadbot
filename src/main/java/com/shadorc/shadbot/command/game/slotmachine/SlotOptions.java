package com.shadorc.shadbot.command.game.slotmachine;

public enum SlotOptions {

    APPLE(200),
    CHERRIES(600),
    BELL(500),
    GIFT(60000);

    private final int gains;

    SlotOptions(int gains) {
        this.gains = gains;
    }

    public int getGains() {
        return this.gains;
    }

    public String getEmoji() {
        return String.format(":%s:", this.toString().toLowerCase());
    }
}