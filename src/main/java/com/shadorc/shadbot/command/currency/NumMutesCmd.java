package com.shadorc.shadbot.command.currency;

import com.shadorc.shadbot.core.command.BaseCmd;
import com.shadorc.shadbot.core.command.CommandCategory;
import com.shadorc.shadbot.core.command.Context;
import com.shadorc.shadbot.db.DatabaseManager;
import com.shadorc.shadbot.object.Emoji;
import com.shadorc.shadbot.object.help.CommandHelpBuilder;
import com.shadorc.shadbot.utils.DiscordUtils;
import com.shadorc.shadbot.utils.FormatUtils;
import discord4j.core.spec.EmbedCreateSpec;
import reactor.core.publisher.Mono;
import reactor.function.TupleUtils;

import java.util.List;
import java.util.function.Consumer;

public class NumMutesCmd extends BaseCmd {

    public NumMutesCmd() {
        super(CommandCategory.CURRENCY, List.of("mutetokens", "mutetoken"));
        this.setDefaultRateLimiter();
    }

    @Override
    public Mono<Void> execute(Context context) {
        return context.getGuild()
                .flatMap(guild -> DiscordUtils.extractMemberOrAuthor(guild, context.getMessage()))
                .flatMap(user -> Mono.zip(Mono.just(user),
                        DatabaseManager.getGuilds().getDBMember(context.getGuildId(), user.getId())))
                .map(TupleUtils.function((user, dbMember) -> {
                    final String mutes = FormatUtils.mutes(dbMember.getMutes());
                    if (user.getId().equals(context.getAuthorId())) {
                        return String.format("(**%s**) You have **%s**.", user.getUsername(), mutes);
                    } else {
                        return String.format("**%s** has **%s**.", user.getUsername(), mutes);
                    }
                }))
                .flatMap(text -> context.getChannel()
                        .flatMap(channel -> DiscordUtils.sendMessage(Emoji.PURSE + " " + text, channel)))
                .then();
    }

    @Override
    public Consumer<EmbedCreateSpec> getHelp(Context context) {
        return CommandHelpBuilder.create(this, context)
                .setDescription("Show how many coins a user has.")
                .addArg("@user", "if not specified, it will show your coins", true)
                .build();
    }
}