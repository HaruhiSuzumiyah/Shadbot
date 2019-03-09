package me.shadorc.shadbot.command.admin.setting;

import java.util.List;
import java.util.function.Consumer;

import discord4j.core.spec.EmbedCreateSpec;
import me.shadorc.shadbot.Shadbot;
import me.shadorc.shadbot.core.command.Context;
import me.shadorc.shadbot.core.setting.BaseSetting;
import me.shadorc.shadbot.core.setting.Setting;
import me.shadorc.shadbot.exception.CommandException;
import me.shadorc.shadbot.utils.DiscordUtils;
import me.shadorc.shadbot.utils.embed.EmbedUtils;
import me.shadorc.shadbot.utils.object.Emoji;
import reactor.core.publisher.Mono;

public class PrefixSetting extends BaseSetting {

	private static final int MAX_PREFIX_LENGTH = 5;

	public PrefixSetting() {
		super(Setting.PREFIX, "Manage Shadbot's prefix.");
	}

	@Override
	public Mono<Void> execute(Context context) {
		final List<String> args = context.requireArgs(2);

		if(args.get(1).length() > MAX_PREFIX_LENGTH) {
			return Mono.error(new CommandException(String.format("Prefix cannot contain more than %s characters.", 
					MAX_PREFIX_LENGTH)));
		}

		if(args.get(1).contains(" ")) {
			return Mono.error(new CommandException("Prefix cannot contain spaces."));
		}

		Shadbot.getDatabase().getDBGuild(context.getGuildId()).setSetting(this.getSetting(), args.get(1));
		return context.getChannel()
				.flatMap(channel -> DiscordUtils.sendMessage(String.format(Emoji.CHECK_MARK + " Prefix set to `%s`", args.get(1)), channel))
				.then();
	}

	@Override
	public Consumer<EmbedCreateSpec> getHelp(Context context) {
		return EmbedUtils.getDefaultEmbed()
				.andThen(embed -> embed.addField("Usage", String.format("`%s%s <prefix>`", context.getPrefix(), this.getCommandName()), false)
						.addField("Argument", "**prefix** - Max length: 5, must not contain spaces", false)
						.addField("Example", String.format("`%s%s !`", context.getPrefix(), this.getCommandName()), false));
	}

}
