package me.shadorc.discordbot;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

import javax.swing.Timer;

import org.json.JSONArray;
import org.json.JSONObject;

import me.shadorc.infonet.Infonet;
import sx.blah.discord.handle.obj.IChannel;
import sx.blah.discord.handle.obj.IMessage;

public class Trivia {

	public static boolean QUIZZ_STARTED = false;

	private static String CORRECT_ANSWER;
	private static IChannel CHANNEL;

	private static final Timer timer = new Timer(30*1000, new ActionListener() {
		@Override
		public void actionPerformed(ActionEvent e) {
			Bot.sendMessage("Temps écoulé, la bonne réponse était " + CORRECT_ANSWER, CHANNEL);
			Trivia.QUIZZ_STARTED = false;
			timer.stop();
		}
	});

	public static void start(IChannel channel) throws MalformedURLException, IOException {
		//Trivia API doc : https://opentdb.com/api_config.php
		String json = Infonet.getHTML(new URL("https://opentdb.com/api.php?amount=1"));
		JSONArray arrayResults = new JSONObject(json).getJSONArray("results");
		JSONObject result = arrayResults.getJSONObject(0);

		String category = result.getString("category");
		String type = result.getString("type");
		String difficulty = result.getString("difficulty");
		String question = result.getString("question");
		String correct_answer = result.getString("correct_answer");

		StringBuilder quizzMessage = new StringBuilder();

		quizzMessage.append("Catégorie : " + category
				+ ", type : " + type
				+ ", difficulté : " + difficulty
				+ "\nQuestion : *" + Utils.convertToPlainText(question) + "*\n");

		if(type.equals("multiple")) {
			JSONArray incorrect_answers = result.getJSONArray("incorrect_answers");

			//Place the correct answer randomly in the list
			int index = Utils.rand(incorrect_answers.length());
			for(int i = 0; i < incorrect_answers.length(); i++) {
				if(i == index) {
					quizzMessage.append("\t- " + Utils.convertToPlainText(correct_answer) + "\n");
				}
				quizzMessage.append("\t- " + Utils.convertToPlainText((String) incorrect_answers.get(i)) + "\n");
			}
		}

		Bot.sendMessage(quizzMessage.toString(), channel);

		Trivia.CORRECT_ANSWER = Utils.convertToPlainText(correct_answer);
		Trivia.QUIZZ_STARTED = true;
		Trivia.CHANNEL = channel;

		timer.start();
	}

	public static void checkAnswer(IMessage message) {
		if(Utils.getLevenshteinDistance(message.getContent().toLowerCase(), Trivia.CORRECT_ANSWER.toLowerCase()) < 2) {
			Bot.sendMessage("Bonne réponse " + message.getAuthor().getName() + " ! Tu gagnes 10 coins.", CHANNEL);
			Utils.gain(message.getAuthor().getName(), 10);
			Trivia.QUIZZ_STARTED = false;
			timer.stop();
		} else {
			Bot.sendMessage("Mauvaise réponse.", CHANNEL);
		}
	}
}
