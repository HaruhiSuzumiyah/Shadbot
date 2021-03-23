package com.shadorc.shadbot.command.currency;

import com.shadorc.shadbot.core.game.player.Player;
import com.shadorc.shadbot.utils.TimeUtils;
import discord4j.common.util.Snowflake;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static com.shadorc.shadbot.command.currency.Constants.WEIRDCHAMP_RESET_HOURS;

public class WeirdChamper extends Player {

    private Instant lastWeirdChamp;


    public WeirdChamper(Snowflake guildId, Snowflake userId) {
        super(guildId, userId);
        this.init();
    }

    private void init() {
        this.lastWeirdChamp = null;
    }

    public void giveWeirdChamp() {
        this.lastWeirdChamp = Instant.now();
    }

    public Instant getLastWeirdChamp() {
        return this.lastWeirdChamp;
    }

    public boolean canWeirdChamp() {
        // If the player has not played since one hour, reset
        if (this.lastWeirdChamp != null
                && TimeUnit.MILLISECONDS.toHours(TimeUtils.getMillisUntil(this.lastWeirdChamp)) >= WEIRDCHAMP_RESET_HOURS) {
            this.init();
        }

        // If the player has been dead for one day, reset
        if (this.lastWeirdChamp != null
                && TimeUnit.MILLISECONDS.toHours(TimeUtils.getMillisUntil(this.lastWeirdChamp)) >= WEIRDCHAMP_RESET_HOURS) {
            this.init();
            return true;
        }

        return this.lastWeirdChamp == null;
    }

}
