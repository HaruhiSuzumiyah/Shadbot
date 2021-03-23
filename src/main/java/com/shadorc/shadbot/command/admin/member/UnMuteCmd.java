package com.shadorc.shadbot.command.admin.member;

import com.shadorc.shadbot.command.CommandException;
import com.shadorc.shadbot.command.MissingArgumentException;
import com.shadorc.shadbot.core.command.BaseCmd;
import com.shadorc.shadbot.core.command.CommandCategory;
import com.shadorc.shadbot.core.command.CommandPermission;
import com.shadorc.shadbot.core.command.Context;
import com.shadorc.shadbot.core.ratelimiter.RateLimiter;
import com.shadorc.shadbot.object.help.CommandHelpBuilder;
import discord4j.common.util.Snowflake;
import discord4j.core.spec.EmbedCreateSpec;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

import static com.shadorc.shadbot.command.admin.member.Constants.MUTE_DURATION;

public class UnMuteCmd extends BaseCmd {

    public UnMuteCmd() {
        super(CommandCategory.ADMIN, CommandPermission.ADMIN, List.of("unmute"));
        this.setRateLimiter(new RateLimiter(2, Duration.ofSeconds(3)));
    }

    @Override
    public Mono<Void> execute(Context context) {
        final String arg = context.requireArg();

        final Set<Snowflake> mentionedUserIds = context.getMessage().getUserMentionIds();
        if (mentionedUserIds.isEmpty()) {
            return Mono.error(new MissingArgumentException());
        }
        final Snowflake mentionUserId = new ArrayList<>(mentionedUserIds).get(0);

        return context.getClient().getMemberById(context.getGuildId(), mentionUserId)
                .flatMap(mutedMember -> mutedMember.edit(spec -> spec.setMute(false)));
    }

    @Override
    public Consumer<EmbedCreateSpec> getHelp(Context context) {
        return CommandHelpBuilder.create(this, context)
                .setDescription(String.format("Spend one mute token and mute a user for %s seconds", MUTE_DURATION))
                .addArg("@user", false)
                .build();
    }

}
