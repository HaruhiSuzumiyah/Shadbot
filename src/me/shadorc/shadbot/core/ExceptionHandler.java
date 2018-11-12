package me.shadorc.shadbot.core;

import java.net.ConnectException;
import java.net.NoRouteToHostException;
import java.net.SocketTimeoutException;
import java.util.Map;

import org.apache.http.HttpStatus;
import org.jsoup.HttpStatusException;

import discord4j.core.DiscordClient;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.MessageChannel;
import discord4j.core.object.util.Snowflake;
import discord4j.rest.http.client.ClientException;
import io.netty.handler.codec.http.HttpResponseStatus;
import me.shadorc.shadbot.core.command.AbstractCommand;
import me.shadorc.shadbot.core.command.Context;
import me.shadorc.shadbot.data.stats.StatsManager;
import me.shadorc.shadbot.data.stats.enums.CommandEnum;
import me.shadorc.shadbot.exception.CommandException;
import me.shadorc.shadbot.exception.MissingArgumentException;
import me.shadorc.shadbot.exception.MissingPermissionException;
import me.shadorc.shadbot.exception.MissingPermissionException.UserType;
import me.shadorc.shadbot.exception.NoMusicException;
import me.shadorc.shadbot.utils.BotUtils;
import me.shadorc.shadbot.utils.StringUtils;
import me.shadorc.shadbot.utils.TextUtils;
import me.shadorc.shadbot.utils.embed.log.LogUtils;
import me.shadorc.shadbot.utils.object.Emoji;
import reactor.core.publisher.Mono;

public class ExceptionHandler {

	public static Mono<Message> handle(Throwable err, AbstractCommand cmd, Context context) {
		if(ExceptionHandler.isCommandException(err)) {
			return ExceptionHandler.onCommandException((CommandException) err, cmd, context);
		}
		if(ExceptionHandler.isMissingPermission(err)) {
			return ExceptionHandler.onMissingPermissionException((MissingPermissionException) err, cmd, context);
		}
		if(ExceptionHandler.isMissingArgumentException(err)) {
			return ExceptionHandler.onMissingArgumentException(cmd, context);
		}
		if(ExceptionHandler.isNoMusicException(err)) {
			return ExceptionHandler.onNoMusicException(cmd, context);
		}
		if(ExceptionHandler.isUnavailable(err)) {
			return ExceptionHandler.onUnavailable(cmd, context);
		}
		if(ExceptionHandler.isUnreacheable(err)) {
			return ExceptionHandler.onUnreacheable(cmd, context);
		}
		if(ExceptionHandler.isForbidden(err)) {
			return ExceptionHandler.onForbidden((ClientException) err, context.getGuildId(), context.getChannel(), context.getUsername());
		}
		return ExceptionHandler.onUnknown(context.getClient(), err, cmd, context);
	}

	public static boolean isCommandException(Throwable err) {
		return err instanceof CommandException;
	}

	public static boolean isMissingPermission(Throwable err) {
		return err instanceof MissingPermissionException;
	}

	public static boolean isMissingArgumentException(Throwable err) {
		return err instanceof MissingArgumentException;
	}

	public static boolean isNoMusicException(Throwable err) {
		return err instanceof NoMusicException;
	}

	public static boolean isUnavailable(Throwable err) {
		return err instanceof ConnectException
				|| err instanceof HttpStatusException && HttpStatusException.class.cast(err).getStatusCode() == HttpStatus.SC_SERVICE_UNAVAILABLE;
	}

	public static boolean isUnreacheable(Throwable err) {
		return err instanceof NoRouteToHostException || err instanceof SocketTimeoutException;
	}

	public static boolean isForbidden(Throwable err) {
		return err instanceof ClientException
				&& ClientException.class.cast(err).getStatus().equals(HttpResponseStatus.FORBIDDEN);
	}

	public static boolean isNotFound(Throwable err) {
		return err instanceof ClientException
				&& ClientException.class.cast(err).getStatus().equals(HttpResponseStatus.NOT_FOUND);
	}

	public static Mono<Message> onCommandException(CommandException err, AbstractCommand cmd, Context context) {
		StatsManager.COMMAND_STATS.log(CommandEnum.COMMAND_ILLEGAL_ARG, cmd);
		return BotUtils.sendMessage(String.format(Emoji.GREY_EXCLAMATION + " (**%s**) %s",
				context.getUsername(), err.getMessage()), context.getChannel());
	}

	public static Mono<Message> onMissingPermissionException(MissingPermissionException err, AbstractCommand cmd, Context context) {
		final String missingPerm = StringUtils.capitalizeEnum(err.getPermission());
		if(err.getType().equals(UserType.BOT)) {
			return BotUtils.sendMessage(
					TextUtils.missingPermission(context.getUsername(), err.getPermission()), context.getChannel())
					.doOnSuccess(message -> LogUtils.info("{Guild ID: %d} Missing permission: %s",
							context.getGuildId().asLong(), missingPerm));
		} else {
			return BotUtils.sendMessage(String.format(Emoji.ACCESS_DENIED
					+ " (**%s**) You can't execute this command because you don't have the permission to %s.",
					context.getUsername(), String.format("**%s**", missingPerm)), context.getChannel());
		}
	}

	public static Mono<Message> onMissingArgumentException(AbstractCommand cmd, Context context) {
		StatsManager.COMMAND_STATS.log(CommandEnum.COMMAND_MISSING_ARG, cmd);
		return cmd.getHelp(context)
				.flatMap(embed -> BotUtils.sendMessage(TextUtils.MISSING_ARG, embed, context.getChannel()));
	}

	public static Mono<Message> onNoMusicException(AbstractCommand cmd, Context context) {
		return BotUtils.sendMessage(String.format(Emoji.MUTE + " (**%s**) No currently playing music.",
				context.getUsername()), context.getChannel());
	}

	public static Mono<Message> onUnavailable(AbstractCommand cmd, Context context) {
		LogUtils.warn(context.getClient(),
				String.format("[%s] Service unavailable.", cmd.getClass().getSimpleName()),
				context.getContent());
		return BotUtils.sendMessage(String.format(Emoji.RED_FLAG + " (**%s**) Mmmh... `%s%s` is currently unavailable... "
				+ "This is not my fault, I promise ! Try again later.",
				context.getUsername(), context.getPrefix(), context.getCommandName()), context.getChannel());
	}

	public static Mono<Message> onUnreacheable(AbstractCommand cmd, Context context) {
		LogUtils.warn(context.getClient(),
				String.format("[%s] Service unreachable.", cmd.getClass().getSimpleName()),
				context.getContent());
		return BotUtils.sendMessage(String.format(Emoji.RED_FLAG + " (**%s**) Mmmh... `%s%s` takes too long to be executed... "
				+ "This is not my fault, I promise ! Try again later.",
				context.getUsername(), context.getPrefix(), context.getCommandName()), context.getChannel());
	}

	public static Mono<Message> onForbidden(ClientException err, Snowflake guildId, Mono<MessageChannel> channel, String username) {
		final Map<String, Object> responseFields = err.getErrorResponse().getFields();
		LogUtils.info("{Guild ID: %d} %d %s: %s",
				guildId.asLong(),
				err.getStatus().code(),
				err.getStatus().reasonPhrase(),
				responseFields.get("message").toString());

		return BotUtils.sendMessage(String.format(Emoji.ACCESS_DENIED
				+ " (**%s**) I can't execute this command due to an unknown lack of permission.",
				username), channel);
	}

	public static void onNotFound(ClientException err, Snowflake guildId) {
		LogUtils.info("{Guild ID: %d} %d %s: %s",
				guildId.asLong(),
				err.getStatus().reasonPhrase(),
				err.getErrorResponse().getFields().get("message"));
	}

	public static Mono<Message> onUnknown(DiscordClient client, Throwable err, AbstractCommand cmd, Context context) {
		LogUtils.error(client, err, String.format("[%s] An unknown error occurred.", cmd.getClass().getSimpleName()),
				context.getContent());
		return BotUtils.sendMessage(
				String.format(Emoji.RED_FLAG + " (**%s**) Sorry, something went wrong while executing `%s%s`. My developer has been warned.",
						context.getUsername(), context.getPrefix(), context.getCommandName()), context.getChannel());
	}

}
