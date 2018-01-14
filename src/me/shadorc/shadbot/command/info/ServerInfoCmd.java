package me.shadorc.shadbot.command.info;

import java.time.format.DateTimeFormatter;
import java.util.Locale;

import me.shadorc.shadbot.core.command.AbstractCommand;
import me.shadorc.shadbot.core.command.CommandCategory;
import me.shadorc.shadbot.core.command.Context;
import me.shadorc.shadbot.core.command.annotation.Command;
import me.shadorc.shadbot.core.command.annotation.RateLimited;
import me.shadorc.shadbot.data.db.DBGuild;
import me.shadorc.shadbot.data.db.Database;
import me.shadorc.shadbot.exception.MissingArgumentException;
import me.shadorc.shadbot.utils.BotUtils;
import me.shadorc.shadbot.utils.FormatUtils;
import me.shadorc.shadbot.utils.TimeUtils;
import me.shadorc.shadbot.utils.embed.EmbedUtils;
import me.shadorc.shadbot.utils.embed.HelpBuilder;
import sx.blah.discord.api.internal.json.objects.EmbedObject;
import sx.blah.discord.handle.obj.IGuild;
import sx.blah.discord.util.EmbedBuilder;

@RateLimited
@Command(category = CommandCategory.INFO, names = { "serverinfo", "server_info", "server-info" })
public class ServerInfoCmd extends AbstractCommand {

	private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("d MMMM yyyy - HH'h'mm", Locale.ENGLISH);

	@Override
	public void execute(Context context) throws MissingArgumentException {
		IGuild guild = context.getGuild();

		DBGuild dbGuild = Database.getDBGuild(guild);

		String allowedChannelsStr = dbGuild.getAllowedChannels().isEmpty() ? "All" : FormatUtils.format(dbGuild.getAllowedChannels(), chlID -> "\n\t" + guild.getChannelByID(chlID).getName(), "");

		String blacklistedCmdStr = dbGuild.getBlacklistedCmd().isEmpty() ? "None" : FormatUtils.format(dbGuild.getBlacklistedCmd(), cmdName -> "\n\t" + cmdName, "");

		String creationDate = String.format("%s%n(%s)",
				TimeUtils.toLocalDate(guild.getCreationDate()).format(dateFormatter),
				FormatUtils.formatLongDuration(guild.getCreationDate()));

		EmbedBuilder embed = EmbedUtils.getDefaultEmbed()
				.setLenient(true)
				.withAuthorName(String.format("Info about \"%s\"", guild.getName()))
				.withThumbnail(guild.getIconURL())
				.appendField("Owner", guild.getOwner().getName(), true)
				.appendField("Server ID", Long.toString(guild.getLongID()), true)
				.appendField("Creation date", creationDate, true)
				.appendField("Region", guild.getRegion().getName(), true)
				.appendField("Channels", String.format("**Voice:** %d", guild.getVoiceChannels().size())
						+ String.format("%n**Text:** %d", guild.getChannels().size()), true)
				.appendField("Members", Integer.toString(guild.getTotalMemberCount()), true)
				.appendField("Settings", String.format("**Prefix:** %s", context.getPrefix())
						+ String.format("%n**Default volume:** %d%%", dbGuild.getDefaultVol())
						+ String.format("%n**Allowed channels:** %s", allowedChannelsStr)
						+ String.format("%n**Blacklisted commands:** %s", blacklistedCmdStr), true);
		BotUtils.sendMessage(embed.build(), context.getChannel());
	}

	@Override
	public EmbedObject getHelp(String prefix) {
		return new HelpBuilder(this, prefix)
				.setDescription("Show info about this server.")
				.build();
	}

}
