package com.shadorc.shadbot.command.game.slotmachine;

import com.shadorc.shadbot.core.game.player.GamblerPlayer;
import com.shadorc.shadbot.core.game.player.Player;
import discord4j.common.util.Snowflake;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static com.shadorc.shadbot.command.game.slotmachine.Constants.MAX_SLOT_MACHINE_PLAYS;
import static com.shadorc.shadbot.command.game.slotmachine.Constants.PAID_COST;

public class SlotMachinePlayer extends GamblerPlayer {
    private List<Instant> previousPlays;

    public SlotMachinePlayer(Snowflake guildId, Snowflake userId) {
        super(guildId, userId, PAID_COST);
        this.init();
    }

    private void init() {
        this.previousPlays = new ArrayList<>();
    }

    public int getNumPlays() {
        this.clear();
        return previousPlays.size();
    }

    public void spin() {
        previousPlays.add(Instant.now());
        int sublistSize = Math.min(previousPlays.size() - 1, MAX_SLOT_MACHINE_PLAYS);
        previousPlays.subList(0, sublistSize);
        this.clear();
    }

    private void clear() {
        Integer clearIndex = null;

        for (int i = 0; i < previousPlays.size(); i++) {
            if (Duration.between(previousPlays.get(i), Instant.now()).getSeconds() > 3600) {
                clearIndex = i;
            }
        }

        if (clearIndex != null) {
            previousPlays.subList(clearIndex, previousPlays.size() - 1);
        }
    }

    public double getFactor() {
        this.clear();
        Double factor = Math.pow(previousPlays.size()/10.0, -1.5);
        return factor;
    }

}
