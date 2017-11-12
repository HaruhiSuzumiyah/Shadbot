package me.shadorc.discordbot.events;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import me.shadorc.discordbot.command.CommandManager;
import me.shadorc.discordbot.data.DatabaseManager;
import me.shadorc.discordbot.data.Setting;
import me.shadorc.discordbot.message.MessageManager;
import me.shadorc.discordbot.utils.BotUtils;
import me.shadorc.discordbot.utils.LogUtils;
import me.shadorc.discordbot.utils.command.Emoji;
import sx.blah.discord.api.events.EventSubscriber;
import sx.blah.discord.handle.impl.events.guild.channel.message.MessageEvent;
import sx.blah.discord.handle.impl.events.guild.channel.message.MessageReceivedEvent;
import sx.blah.discord.handle.obj.IMessage;

@SuppressWarnings("ucd")
public class MessageListener {

	private final ExecutorService executor = Executors.newCachedThreadPool();

	@EventSubscriber
	public void onMessageEvent(MessageEvent event) {
		if(event instanceof MessageReceivedEvent) {
			executor.execute(() -> this.onMessage((MessageReceivedEvent) event));
		}
	}

	private void onMessage(MessageReceivedEvent event) {
		if(event.getAuthor().isBot()) {
			return;
		}

		if(event.getChannel().isPrivate()) {
			BotUtils.sendMessage(Emoji.INFO + " Sorry, I don't respond (yet ?) to private messages.", event.getChannel());
			LogUtils.info("Shadbot has received a private message. (Message: " + event.getMessage().getContent() + ")");
			return;
		}

		if(!BotUtils.isChannelAllowed(event.getGuild(), event.getChannel())) {
			return;
		}

		IMessage message = event.getMessage();
		if(MessageManager.isWaitingForMessage(event.getChannel()) && MessageManager.notify(message)) {
			return;
		}

		String prefix = (String) DatabaseManager.getSetting(event.getGuild(), Setting.PREFIX);
		if(message.getContent().startsWith(prefix)) {
			try {
				CommandManager.manage(event);
			} catch (Exception err) {
				LogUtils.error("{Guild ID: " + event.getGuild().getLongID() + "} An unknown error occurred while executing a command.", err, message.getContent());
			}
		}
	}
}