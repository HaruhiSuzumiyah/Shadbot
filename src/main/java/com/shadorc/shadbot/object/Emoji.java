package com.shadorc.shadbot.object;

public enum Emoji {

    CHECK_MARK("white_check_mark"),
    WARNING("warning"),
    ACCESS_DENIED("no_entry_sign"),
    RED_CROSS("x"),
    LOCK("lock"),
    WEIRD_CHAMP("<:uksfinest:728069091541254154>"),

    GREY_EXCLAMATION("grey_exclamation"),
    RED_EXCLAMATION("exclamation"),
    QUESTION("question"),
    RED_FLAG("triangular_flag_on_post"),
    WHITE_FLAG("flag_white"),

    INFO("information_source"),
    MAGNIFYING_GLASS("mag"),
    STOPWATCH("stopwatch"),
    GEAR("gear"),

    HOURGLASS("hourglass_flowing_sand"),
    PURSE("purse"),
    MONEY_BAG("moneybag"),
    BANK("bank"),
    DICE("game_die"),
    TICKET("tickets"),

    SPADES("spades"),
    CLUBS("clubs"),
    HEARTS("hearts"),
    DIAMONDS("diamonds"),

    THERMOMETER("thermometer"),
    CLOUD("cloud"),
    WIND("wind_blowing_face"),
    RAIN("cloud_rain"),
    DROPLET("droplet"),

    MUSICAL_NOTE("musical_note"),
    TRACK_NEXT("track_next"),
    PLAY("arrow_forward"),
    PAUSE("pause_button"),
    REPEAT("repeat"),
    SOUND("sound"),
    MUTE("mute"),
    STOP_BUTTON("stop_button"),

    THUMBSDOWN("thumbsdown"),
    SPEECH("speech_balloon"),
    CLAP("clap"),
    TRIANGULAR_RULER("triangular_ruler"),

    SCISSORS("scissors"),
    GEM("gem"),
    LEAF("leaves"),

    BALLOT_BOX("ballot_box"),
    BUG("bug"),
    ROCKET("rocket"),
    BROCKEN_HEART("broken_heart");

    private final String discordNotation;

    Emoji(String discordNotation) {
        this.discordNotation = discordNotation;
    }

    @Override
    public String toString() {
        return String.format(":%s:", this.discordNotation);
    }

    public String customString() {
        return this.discordNotation;
    }
}
