package com.shadorc.shadbot.command.admin.member;

import com.shadorc.shadbot.command.CommandException;
import com.shadorc.shadbot.command.MissingArgumentException;
import com.shadorc.shadbot.core.command.BaseCmd;
import com.shadorc.shadbot.core.command.CommandCategory;
import com.shadorc.shadbot.core.command.CommandPermission;
import com.shadorc.shadbot.core.command.Context;
import com.shadorc.shadbot.core.ratelimiter.RateLimiter;
import com.shadorc.shadbot.db.DatabaseManager;
import com.shadorc.shadbot.db.guilds.entity.DBMember;
import com.shadorc.shadbot.object.Emoji;
import com.shadorc.shadbot.object.help.CommandHelpBuilder;
import com.shadorc.shadbot.utils.DiscordUtils;
import discord4j.common.util.Snowflake;
import discord4j.core.object.VoiceState;
import discord4j.core.object.entity.Member;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.core.spec.GuildMemberEditSpec;
import discord4j.rest.util.Permission;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

import static com.shadorc.shadbot.command.admin.member.Constants.MUTE_DURATION;

public class MuteCmd extends BaseCmd {

    public MuteCmd() {
        super(CommandCategory.ADMIN, List.of("mute"));
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
        final Snowflake authorUserId = context.getAuthorId();

        if (mentionUserId.equals(context.getSelfId())) {
            return Mono.error(new CommandException(String.format("You cannot %s me.", this.getName())));
        }

        Long mutes = DatabaseManager.getGuilds().getDBMember(context.getGuildId(), authorUserId)
                .map(DBMember::getMutes).block();
        if(mutes >= 1) {
            DatabaseManager.getGuilds().getDBMember(context.getGuildId(), authorUserId)
                    .map(dbMember -> {
                         dbMember.addMutes(-1).block();
                        return true;
                    }).block();

            context.getClient().getMemberById(context.getGuildId(), mentionUserId)
            .flatMap(mutedMember -> mutedMember.edit(spec -> spec.setMute(true)))
                    .block();
            try {
                context.getChannel()
                        .flatMap(channel -> DiscordUtils.sendMessage(String.format(
                                Emoji.BANK + " (**%s**) Has just been muted for 10 seconds!.",
                                "<@"+mentionUserId.asLong()+">"), channel)).block();
                Thread.sleep(10000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        } else {
            return Mono.error(new CommandException(String.format("You cannot %s someone without a mute token.", this.getName())));
        }




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
