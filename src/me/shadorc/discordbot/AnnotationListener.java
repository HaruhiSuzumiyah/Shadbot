package me.shadorc.discordbot;

import java.util.List;

import sx.blah.discord.api.events.EventSubscriber;
import sx.blah.discord.handle.impl.events.ReadyEvent;
import sx.blah.discord.handle.impl.events.guild.channel.message.MessageReceivedEvent;
import sx.blah.discord.handle.obj.IChannel;
import sx.blah.discord.handle.obj.IGuild;
import sx.blah.discord.handle.obj.IMessage;

public class AnnotationListener {

	@EventSubscriber
	public void onReadyEvent(ReadyEvent event) {
		System.out.println("\nShadBot is connected to :");
		List <IGuild> guilds = event.getClient().getGuilds();
		for(IGuild guild : guilds) {
			System.out.println("\tName : " + guild.getName() + " | ID : " + guild.getLongID());
		}
		System.out.println();
	}

	@EventSubscriber
	public void onMessageReceivedEvent(MessageReceivedEvent event) {
		IGuild guild = event.getGuild();
		IChannel channel = event.getChannel();
		IMessage message = event.getMessage();

		if(Main.BETA && guild.getName().equals("Shadserv") && channel.getName().equals("test")
				|| !Main.BETA && guild.getName().equals("Chambre de Jack") && channel.getName().equals("bot_room")) {

			if(Trivia.QUIZZ_STARTED) {
				Trivia.checkAnswer(message);
			}
			else if(message.getContent().startsWith("/")) {
				Bot.executeCommand(message, channel);
			}
		}
	}
}