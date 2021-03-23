package com.shadorc.shadbot.command.currency;

import com.shadorc.shadbot.command.CommandException;
import com.shadorc.shadbot.core.command.BaseCmd;
import com.shadorc.shadbot.core.command.CommandCategory;
import com.shadorc.shadbot.core.command.Context;
import com.shadorc.shadbot.data.Config;
import com.shadorc.shadbot.db.DatabaseManager;
import com.shadorc.shadbot.db.guilds.entity.DBMember;
import com.shadorc.shadbot.object.Emoji;
import com.shadorc.shadbot.object.help.CommandHelpBuilder;
import com.shadorc.shadbot.utils.DiscordUtils;
import com.shadorc.shadbot.utils.FormatUtils;
import com.shadorc.shadbot.utils.NumberUtils;
import com.shadorc.shadbot.utils.ShadbotUtils;
import discord4j.core.spec.EmbedCreateSpec;
import reactor.core.publisher.Mono;
import reactor.function.TupleUtils;

import java.util.List;
import java.util.function.Consumer;

import static com.shadorc.shadbot.command.currency.Constants.MUTE_TOKEN_COST;

public class BuyMutesCmd extends BaseCmd {

    public BuyMutesCmd() {
        super(CommandCategory.CURRENCY, List.of("buymute", "buytoken", "buymutetoken"));
        this.setDefaultRateLimiter();
    }

    @Override
    public Mono<Void> execute(Context context) {
        //final List<String> args = context.requireArgs(1);

        return context.getGuild()
                .flatMap(guild -> DiscordUtils.extractMemberOrAuthor(guild, context.getMessage()))
                .flatMap(user -> {

                    //final Long mutes = NumberUtils.toPositiveLongOrNull(args.get(0));
                    final Long mutes = 1L;
                    if (mutes == null) {
                        return Mono.error(new CommandException(String.format("`%s` is not a valid amount of mutes.",
                                mutes)));
                    }

                    if (mutes > Config.MAX_COINS) {
                        return Mono.error(new CommandException(String.format("You cannot buy more than %s.",
                                FormatUtils.coins(Config.MAX_COINS))));
                    }

                    final long coinsCost = mutes*MUTE_TOKEN_COST;

                    return DatabaseManager.getGuilds()
                            .getDBMember(context.getGuildId(), user.getId())
                            .flatMap(dbMember -> {
                                if (dbMember.getCoins() < coinsCost) {
                                    return Mono.error(new CommandException(String.format("Mute Tokens cost %s Coins", MUTE_TOKEN_COST) +
                                            ShadbotUtils.NOT_ENOUGH_COINS));
                                }

                                if (dbMember.getMutes() + mutes >= Config.MAX_COINS) {
                                    return context.getChannel()
                                            .flatMap(channel -> DiscordUtils.sendMessage(String.format(
                                                    Emoji.BANK + " (**%s**) This purchase cannot be done because it would " +
                                                            "exceed the maximum mutes cap.",
                                                    context.getUsername()), channel));
                                }

                                return dbMember.addMutes(mutes)
                                        .and(dbMember.addCoins(-coinsCost))
                                        .then(context.getChannel())
                                        .flatMap(channel -> DiscordUtils.sendMessage(
                                                String.format(Emoji.BANK + " **%s** has purchased **%s** for **%s**.",
                                                        context.getUsername(), FormatUtils.mutes(mutes),
                                                        FormatUtils.coins(coinsCost)), channel));
                            });
                })
                .then();
    }

    @Override
    public Consumer<EmbedCreateSpec> getHelp(Context context) {
        return CommandHelpBuilder.create(this, context)
                .setDescription("Buy mute tokens")
                .addArg("@number", "the number of tokens you want to buy", false)
                .build();
    }
}