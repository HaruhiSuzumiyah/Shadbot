package com.shadorc.shadbot.command.game.slotmachine;

import com.shadorc.shadbot.core.command.BaseCmd;
import com.shadorc.shadbot.core.command.CommandCategory;
import com.shadorc.shadbot.core.command.Context;
import com.shadorc.shadbot.data.Telemetry;
import com.shadorc.shadbot.object.Emoji;
import com.shadorc.shadbot.object.help.CommandHelpBuilder;
import com.shadorc.shadbot.utils.DiscordUtils;
import com.shadorc.shadbot.utils.FormatUtils;
import com.shadorc.shadbot.utils.RandUtils;
import com.shadorc.shadbot.utils.ShadbotUtils;
import discord4j.common.util.Snowflake;
import discord4j.core.spec.EmbedCreateSpec;
import reactor.core.publisher.Mono;
import reactor.function.TupleUtils;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuples;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Consumer;

public class SlotMachineCmd extends BaseCmd {

    private final Map<Tuple2<Snowflake, Snowflake>, SlotMachinePlayer> player;

    public SlotMachineCmd() {
        super(CommandCategory.GAME, List.of("slot_machine"), "sm");
        this.setGameRateLimiter();

        this.player = new ConcurrentHashMap<>();
    }

    @Override
    public Mono<Void> execute(Context context) {
        return ShadbotUtils.requireValidBet(context.getGuildId(), context.getAuthorId(), Constants.PAID_COST)
                .map(ignored -> this.getPlayer(context.getGuildId(), context.getAuthorId()))
                .flatMap(player -> player.bet().thenReturn(player))
                .flatMap(player -> {
                    final List<SlotOptions> slots = SlotMachineCmd.randSlots();

                    final StringBuilder strBuilder = new StringBuilder(String.format("%s%n%s (**%s**) ",
                            FormatUtils.format(slots, SlotOptions::getEmoji, " "), Emoji.BANK, context.getUsername()));

                    if (slots.stream().distinct().count() == 1) {
                        final int slotGains = slots.get(0).getGains();
                        player.spin();
                        final long gains = (long) Math.ceil(ThreadLocalRandom.current().nextInt((int) (slotGains * Constants.RAND_FACTOR),
                                (int) (slotGains * (Constants.RAND_FACTOR + 1)))*player.getFactor());
                        Telemetry.SLOT_MACHINE_SUMMARY.labels("win").observe(gains);
                        return player.win(gains)
                                .thenReturn(strBuilder.append(String.format("You win **%s** !", FormatUtils.coins(gains))));
                    } else {
                        Telemetry.SLOT_MACHINE_SUMMARY.labels("loss").observe(Constants.PAID_COST);
                        return Mono.just(strBuilder.append(String.format("You lose **%s** !", FormatUtils.coins(Constants.PAID_COST))));
                    }
                })
                .map(StringBuilder::toString)
                .flatMap(text -> context.getChannel()
                        .flatMap(channel -> DiscordUtils.sendMessage(text, channel)))
                .then();
    }

    private static List<SlotOptions> randSlots() {
        // Pseudo-random number between 0 and 100 inclusive
        final int rand = ThreadLocalRandom.current().nextInt(100 + 1);
        if (rand == 0) {
            return List.of(SlotOptions.GIFT, SlotOptions.GIFT, SlotOptions.GIFT);
        }
        if (rand <= 5) {
            return List.of(SlotOptions.BELL, SlotOptions.BELL, SlotOptions.BELL);
        }
        if (rand <= 20) {
            return List.of(SlotOptions.CHERRIES, SlotOptions.CHERRIES, SlotOptions.CHERRIES);
        }
        if (rand <= 50) {
            return List.of(SlotOptions.APPLE, SlotOptions.APPLE, SlotOptions.APPLE);
        }

        final List<SlotOptions> list = new ArrayList<>();
        do {
            final SlotOptions slot = RandUtils.randValue(SlotOptions.values());
            if (!list.contains(slot)) {
                list.add(slot);
            }
        } while (list.size() != 3);
        return list;
    }

    private SlotMachinePlayer getPlayer(Snowflake guildId, Snowflake userId) {
        return this.player.computeIfAbsent(Tuples.of(guildId, userId), TupleUtils.function(SlotMachinePlayer::new));
    }

    @Override
    public Consumer<EmbedCreateSpec> getHelp(Context context) {
        return CommandHelpBuilder.create(this, context)
                .setDescription("Play slot machine.")
                .addField("Cost", String.format("A game costs **%s**.", FormatUtils.coins(Constants.PAID_COST)), false)
                .addField("Gains", String.format("%s: **%s**, %s: **%s**, %s: **%s**, %s: **%s**." +
                                "%nYou also gain a small random bonus. You start with a large bonus that decays as you keep playing for the past hour",
                        FormatUtils.capitalizeEnum(SlotOptions.APPLE), FormatUtils.coins(SlotOptions.APPLE.getGains()),
                        FormatUtils.capitalizeEnum(SlotOptions.CHERRIES), FormatUtils.coins(SlotOptions.CHERRIES.getGains()),
                        FormatUtils.capitalizeEnum(SlotOptions.BELL), FormatUtils.coins(SlotOptions.BELL.getGains()),
                        FormatUtils.capitalizeEnum(SlotOptions.GIFT), FormatUtils.coins(SlotOptions.GIFT.getGains())), false)
                .build();
    }

}