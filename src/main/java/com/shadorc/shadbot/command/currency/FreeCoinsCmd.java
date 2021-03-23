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
import discord4j.common.util.Snowflake;
import discord4j.core.spec.EmbedCreateSpec;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.function.Consumer;

import static com.shadorc.shadbot.command.currency.Constants.*;

public class FreeCoinsCmd extends BaseCmd {

    public FreeCoinsCmd() {
        super(CommandCategory.CURRENCY, List.of("freecoin", "freecoins"));
        this.setDefaultRateLimiter();
    }

    @Override
    public Mono<Void> execute(Context context) {

        return context.getGuild()
                .flatMap(guild -> DiscordUtils.extractMemberOrAuthor(guild, context.getMessage()))
                .flatMap(user -> {


                    return DatabaseManager.getGuilds()
                            .getDBMember(context.getGuildId(), user.getId())
                            .flatMap(dbMember -> {
                                if (dbMember.getFreecoins() == false) {
                                    return Mono.error(new CommandException(String.format("You've already claimed your Free %s Coins", FREE_COINS_AMOUNT)));
                                }

                                if (dbMember.getCoins() + FREE_COINS_AMOUNT >= Config.MAX_COINS) {
                                    return context.getChannel()
                                            .flatMap(channel -> DiscordUtils.sendMessage(String.format(
                                                    Emoji.BANK + " (**%s**) You can't claim your free coins because it would " +
                                                            "exceed the maximum coins cap.",
                                                    context.getUsername()), channel));
                                }

                                return dbMember.addCoins(FREE_COINS_AMOUNT)
                                        .and(dbMember.useFreecoins())
                                        .then(context.getChannel())
                                        .flatMap(channel -> DiscordUtils.sendMessage(
                                                String.format(Emoji.BANK + " **%s** has claimed their **%s**.",
                                                        context.getUsername(), FormatUtils.coins(FREE_COINS_AMOUNT)), channel));
                            });
                })
                .then();
    }

    @Override
    public Consumer<EmbedCreateSpec> getHelp(Context context) {
        return CommandHelpBuilder.create(this, context)
                .setDescription("Award a WeirdChamp(s) to a user. Each WeirdChamp costs 10000 Coins.")
                .addArg("weirdchamps", false)
                .addArg("@user", false)
                .build();
    }
}
