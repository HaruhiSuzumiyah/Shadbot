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

public class WeirdChampsCmd extends BaseCmd {

    public WeirdChampsCmd() {
        super(CommandCategory.CURRENCY, List.of("weirdchamps"));
        this.setDefaultRateLimiter();
    }

    @Override
    public Mono<Void> execute(Context context) {
        return context.getGuild()
                .flatMap(guild -> DiscordUtils.extractMemberOrAuthor(guild, context.getMessage()))
                .flatMap(user -> Mono.zip(Mono.just(user),
                        DatabaseManager.getGuilds().getDBMember(context.getGuildId(), user.getId())))
                .map(TupleUtils.function((user, dbMember) -> {
                    final String weirdchamps = FormatUtils.weirdchamps(dbMember.getWeirdChamps());
                    if (user.getId().equals(context.getAuthorId())) {
                        return String.format("(**%s**) You have **%s**.", user.getUsername(), weirdchamps);
                    } else {
                        return String.format("**%s** has **%s**.", user.getUsername(), weirdchamps);
                    }
                }))
                .flatMap(text -> context.getChannel()
                        .flatMap(channel -> DiscordUtils.sendMessage(Emoji.WEIRD_CHAMP.customString() + " " + text, channel)))
                .then();
    }

    @Override
    public Consumer<EmbedCreateSpec> getHelp(Context context) {
        return CommandHelpBuilder.create(this, context)
                .setDescription("Show how many WeirdChamps a user has.")
                .addArg("@user", "if not specified, it will show your WeirdChamps", true)
                .build();
    }
}