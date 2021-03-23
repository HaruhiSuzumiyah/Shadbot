package com.shadorc.shadbot.command.currency;

import com.shadorc.shadbot.core.command.BaseCmd;
import com.shadorc.shadbot.core.command.CommandCategory;
import com.shadorc.shadbot.core.command.Context;
import com.shadorc.shadbot.db.DatabaseManager;
import com.shadorc.shadbot.db.guilds.entity.DBGuild;
import com.shadorc.shadbot.db.guilds.entity.DBMember;
import com.shadorc.shadbot.object.help.CommandHelpBuilder;
import com.shadorc.shadbot.utils.DiscordUtils;
import com.shadorc.shadbot.utils.FormatUtils;
import com.shadorc.shadbot.utils.ShadbotUtils;
import discord4j.core.object.entity.User;
import discord4j.core.spec.EmbedCreateSpec;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;

import java.util.Comparator;
import java.util.List;
import java.util.function.Consumer;

public class WeirdchampLeaderboardCmd extends BaseCmd {

    private static final int USER_COUNT = 10;

    public WeirdchampLeaderboardCmd() {
        super(CommandCategory.CURRENCY, List.of("weirdchamp_leaderboard"));
        this.setDefaultRateLimiter();
    }

    @Override
    public Mono<Void> execute(Context context) {
        return DatabaseManager.getGuilds()
                .getDBGuild(context.getGuildId())
                .flatMapIterable(DBGuild::getMembers)
                .filter(dbMember -> dbMember.getWeirdChamps() > 0)
                .sort(Comparator.comparingLong(DBMember::getWeirdChamps).reversed())
                .take(USER_COUNT)
                .flatMapSequential(dbMember -> Mono.zip(
                        context.getClient().getUserById(dbMember.getId()).map(User::getUsername),
                        Mono.just(dbMember.getWeirdChamps())))
                .collectList()
                .map(list -> {
                    if (list.isEmpty()) {
                        return "\nEveryone is poor here.";
                    }
                    return FormatUtils.numberedList(USER_COUNT, list.size(),
                            count -> {
                                final Tuple2<String, Long> tuple = list.get(count - 1);
                                return String.format("%d. **%s** - %s",
                                        count, tuple.getT1(), FormatUtils.weirdchamps(tuple.getT2()));
                            });
                })
                .map(description -> ShadbotUtils.getDefaultEmbed()
                        .andThen(embed -> embed.setAuthor("Leaderboard", null, context.getAvatarUrl())
                                .setDescription(description)))
                .flatMap(embed -> context.getChannel()
                        .flatMap(channel -> DiscordUtils.sendMessage(embed, channel)))
                .then();
    }

    @Override
    public Consumer<EmbedCreateSpec> getHelp(Context context) {
        return CommandHelpBuilder.create(this, context)
                .setDescription("Show WeirdChamp leaderboard for this server.")
                .build();
    }
}
