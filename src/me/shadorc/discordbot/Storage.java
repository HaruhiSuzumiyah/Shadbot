package me.shadorc.discordbot;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.json.JSONException;
import org.json.JSONObject;

public class Storage {

	private static final File FILE = new File("data.json");

	public enum API_KEYS {
		GIPHY_API_KEY, 
		DTC_API_KEY, 
		DISCORD_TOKEN, 
		TWITTER_API_KEY, 
		TWITTER_API_SECRET, 
		TWITTER_TOKEN, 
		TWITTER_TOKEN_SECRET
	}

	private static void init() {
		if(!FILE.exists()) {
			try {
				FILE.createNewFile();
				FileWriter writer = null;
				try {
					writer = new FileWriter(FILE);
					writer.write("{}");
					writer.flush();
				} catch (IOException e) {
					System.err.println("Error while saving in storage file.");
					e.printStackTrace();
				} finally {
					try {
						if(writer != null) writer.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			} catch (IOException e) {
				System.err.println("Error while creating storage file.");
				e.printStackTrace();
			}
		}
	}

	public static void store(Object key, Object value) {
		if(!FILE.exists()) Storage.init();

		FileWriter writer = null;
		try {
			JSONObject obj = new JSONObject(new String(Files.readAllBytes(Paths.get(FILE.getPath())), StandardCharsets.UTF_8));
			obj.put(key.toString(), value.toString());

			writer = new FileWriter(FILE);
			writer.write(obj.toString());
			writer.flush();
		} catch (IOException e) {
			System.err.println("Error while saving in storage file.");
			e.printStackTrace();
		} finally {
			try {
				if(writer != null) writer.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	public static int get(String key) {
		if(!FILE.exists()) Storage.init();

		try {
			JSONObject obj = new JSONObject(new String(Files.readAllBytes(Paths.get(FILE.getPath())), StandardCharsets.UTF_8));
			if(obj.has(key)) {
				return obj.getInt(key);
			}
		} catch (JSONException | IOException e) {
			System.err.println("Error while reading storage file.");
			e.printStackTrace();
		}
		return 0;
	}

	public static String get(API_KEYS key) {
		try {
			JSONObject obj = new JSONObject(new String(Files.readAllBytes(Paths.get(FILE.getPath())), StandardCharsets.UTF_8));
			return obj.getString(key.toString());
		} catch (JSONException | IOException e) {
			System.err.println("Error while accessing to API keys storage.");
			e.printStackTrace();
		}
		return null;
	}
}