package com.shadorc.shadbot.command.currency;

import com.shadorc.shadbot.command.CommandException;
import com.shadorc.shadbot.command.game.lottery.LotteryCmd;
import com.shadorc.shadbot.command.game.russianroulette.RussianRoulettePlayer;
import com.shadorc.shadbot.core.command.BaseCmd;
import com.shadorc.shadbot.core.command.CommandCategory;
import com.shadorc.shadbot.core.command.Context;
import com.shadorc.shadbot.data.Config;
import com.shadorc.shadbot.db.DatabaseManager;
import com.shadorc.shadbot.db.guilds.entity.DBMember;
import com.shadorc.shadbot.object.Emoji;
import com.shadorc.shadbot.object.help.CommandHelpBuilder;
import com.shadorc.shadbot.utils.*;
import discord4j.common.util.Snowflake;
import discord4j.core.object.entity.Member;
import discord4j.core.spec.EmbedCreateSpec;
import reactor.core.publisher.Mono;
import reactor.function.TupleUtils;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuples;

import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

import static com.shadorc.shadbot.command.currency.Constants.WEIRDCHAMP_RESET_HOURS;
import static com.shadorc.shadbot.command.currency.Constants.WEIRD_CHAMP_COST;

public class GiveWeirdchampCmd extends BaseCmd {

    private final Map<Tuple2<Snowflake, Snowflake>, WeirdChamper> weirdChampers;

    public GiveWeirdchampCmd() {
        super(CommandCategory.CURRENCY, List.of("weirdchamp"));
        this.setDefaultRateLimiter();

        this.weirdChampers = new ConcurrentHashMap<>();
    }



    @Override
    public Mono<Void> execute(Context context) {
        final List<String> args = context.requireArgs(1);

        return context.getGuild()
                .flatMapMany(guild -> DiscordUtils.extractMembers(guild, args.get(0)))
                .next()
                .switchIfEmpty(Mono.error(new CommandException("You cannot give yourself a WeirdChamp.")))
                .map(receiver -> Collections.singletonMap(receiver, this.getPlayer(context.getGuildId(), context.getAuthorId())))
                .flatMap(memberMap -> {
                    final Snowflake senderUserId = context.getAuthorId();
                    Member receiverMember = memberMap.entrySet().iterator().next().getKey();
                    WeirdChamper weirdChampSender = memberMap.get(receiverMember);

                    if (!weirdChampSender.canWeirdChamp()) {
                        Duration timeUntilNextWeirdChamp = Duration.ofMillis(
                                TimeUtils.getMillisUntil(
                                        weirdChampSender.getLastWeirdChamp().plusSeconds(WEIRDCHAMP_RESET_HOURS*3600)));

                        return Mono.error(new CommandException(
                                String.format("You can't WeirdChamp that often... " +
                                                "You will be able to WeirdChamp again in **%s**!"
                                        , FormatUtils.formatDurationWords(timeUntilNextWeirdChamp))));
                    }

                    if (receiverMember.getId().equals(senderUserId)) {
                        return Mono.error(new CommandException("You cannot give yourself a WeirdChamp."));
                    }

                    final Long weirdChamps = 1L;
                    final long weirdChampsCost = weirdChamps * WEIRD_CHAMP_COST;



                    if (weirdChampsCost > Config.MAX_COINS) {
                        return Mono.error(new CommandException(String.format("You cannot spend more than %s on WeirdChamps.",
                                FormatUtils.coins(Config.MAX_COINS))));
                    }
                    if (weirdChamps > Config.MAX_COINS) {
                        return Mono.error(new CommandException(String.format("You cannot transfer more than %s.",
                                FormatUtils.weirdchamps(Config.MAX_COINS))));
                    }

                    return DatabaseManager.getGuilds()
                            .getDBMembers(context.getGuildId(), senderUserId, receiverMember.getId())
                            .collectMap(DBMember::getId)
                            .flatMap(dbMembers -> {
                                final DBMember dbSender = dbMembers.get(senderUserId);
                                if (dbSender.getCoins() < weirdChampsCost) {
                                    return Mono.error(new CommandException(ShadbotUtils.NOT_ENOUGH_COINS));
                                }

                                final DBMember dbReceiver = dbMembers.get(receiverMember.getId());
                                if (dbReceiver.getWeirdChamps() + weirdChamps >= Config.MAX_COINS) {
                                    return context.getChannel()
                                            .flatMap(channel -> DiscordUtils.sendMessage(String.format(
                                                    Emoji.WEIRD_CHAMP.customString() + " (**%s**) This transfer cannot be done because %s would " +
                                                            "exceed the maximum WeirdChamp cap.",
                                                    context.getUsername(), receiverMember.getUsername()), channel));
                                }
                                weirdChampSender.giveWeirdChamp();
                                return dbSender.addCoins(-weirdChampsCost)
                                        .and(dbReceiver.addWeirdChamps(weirdChamps))
                                        .then(context.getChannel())
                                        .flatMap(channel -> DiscordUtils.sendMessage(
                                                String.format(Emoji.WEIRD_CHAMP.customString() + " **%s** has awarded **%s** to **%s** for **%s**.",
                                                        context.getUsername(), FormatUtils.weirdchamps(weirdChamps),
                                                        receiverMember.getUsername(), FormatUtils.coins(weirdChampsCost)), channel));
                            });
                })
                .then();
    }

    private WeirdChamper getPlayer(Snowflake guildId, Snowflake userId) {
        return this.weirdChampers.computeIfAbsent(Tuples.of(guildId, userId), TupleUtils.function(WeirdChamper::new));
    }

    @Override
    public Consumer<EmbedCreateSpec> getHelp(Context context) {
        return CommandHelpBuilder.create(this, context)
                .setDescription("Award a WeirdChamp to a user. Each WeirdChamp costs 10000 Coins. You can only WeirdChamp once per hour")
                .addArg("@user", false)
                .build();
    }
}
