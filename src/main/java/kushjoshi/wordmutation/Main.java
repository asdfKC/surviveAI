package kushjoshi.wordmutation;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonObject;

import java.io.FileInputStream;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.rmi.registry.Registry;
import java.util.Properties;

import static java.net.http.HttpClient.newHttpClient;

public class Main implements ModInitializer {
	private static final Logger log = LoggerFactory.getLogger(Main.class);
	public static int TIME = 600;
	public int current = 0;
	public boolean patienceIsJustSad = true;
	public int timePassed = 0;
	String answer = "";
	@Override
	public void onInitialize() {
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			if (patienceIsJustSad) {
				timePassed++;
				current++;
				if (current >= TIME)
				{
					patienceIsJustSad = false;
					requestMutation(server);
				}
			}
		});
	}


	private int requestMutation(MinecraftServer server) {
		Properties props = new Properties();
		try (FileInputStream in = new FileInputStream("secrets.properties")) {
			props.load(in);
		} catch (IOException e) {
			log.error("Logs: Failed to open secrets.properties. Please check if file exists.");
			server.getPlayerList().broadcastSystemMessage(Component.literal("Logs: Failed to open secrets.properties. Please check if file exists."), true);
			return 1;
		}
		String apiKey = props.getProperty("key");
		if (apiKey == null || apiKey.isBlank()) {
			log.error("Seems to be your api key isn't properly loading. Did you remember to place it in secrets.properties?");
			server.getPlayerList().broadcastSystemMessage(Component.literal("Seems to be your api key isn't properly loading. Did you remember to place it in secrets.properties?"), true);
			return 1;
		}

		String jsonSonion = String.format("""
				    {
				      "model": "qwen/qwen3-14b",
				      "messages": [
				        {"role": "user", "content": "You are the master, controlling this minecraft world, every 60 seconds, a new scenario is formed for the player to survive, and you create them, as the player keeps living, until the last time he dies, make the traps harder and harder, the player has survived for %d ticks, if the tick counter is close to zero, the player has recently died, as this counter goes higher and higher, make each trap more difficult to survive.  You may ONLY choose blocks from this list: minecraft:lava, minecraft:fire, minecraft:soul_fire, minecraft:magma_block, minecraft:cactus, minecraft:sweet_berry_bush, minecraft:wither_rose, minecraft:powder_snow, minecraft:campfire, minecraft:soul_campfire, minecraft:sand, minecraft:red_sand, minecraft:gravel, minecraft:anvil, minecraft:pointed_dripstone, minecraft:obsidian, minecraft:netherrack, minecraft:stone, minecraft:cobblestone, minecraft:deepslate, minecraft:water, minecraft:air. Respond with ONLY the following three lines, nothing else, no explanation, no extra text: BLOCK_FROM:minecraft:<block> BLOCK_TO:minecraft:<block> PATTERN:<replace_all|checkerboard|random_scatter>"}
				      ]
				    }
				""", timePassed);
		HttpClient magicalCurlTypaThing = newHttpClient();
		HttpRequest plsgivemearesponse = HttpRequest.newBuilder()
				.uri(URI.create("https://ai.hackclub.com/proxy/v1/chat/completions"))
				.header("Authorization", "Bearer " + apiKey)
				.header("Content-Type", "application/json")
				.POST(HttpRequest.BodyPublishers.ofString(jsonSonion))
				.build();
		magicalCurlTypaThing.sendAsync(plsgivemearesponse, HttpResponse.BodyHandlers.ofString()).thenAccept(response -> {
					//this is the most \stupidest set of lines in the entire main.java :<
					/*_*/
					//LOOK I MADE A FACE!
					storeResponse(server, response.body());
				}


		).exceptionally(throwable -> {
			wrongwrongwrong(server);
			return null;
		});


		return 0;
	}

	private int storeResponse(MinecraftServer server ,String response)
	{
		try {
			answer = response;
			JsonObject parsed = JsonParser.parseString(response).getAsJsonObject();
			// extraction
			JsonArray choice = parsed.getAsJsonArray("choices");
			JsonElement ihatethis = choice.get(0);
			JsonObject whyisthissohard = ihatethis.getAsJsonObject();
			JsonObject sobsobsob = whyisthissohard.getAsJsonObject("message");
			answer = sobsobsob.get("content").getAsString();


			server.getPlayerList().broadcastSystemMessage(Component.literal(answer), false);
			parseAndApplyMutation(server, answer);
			return 0;
		} catch (Exception e) {
			wrongwrongwrong(server);
			return 1;
		}

	}

	private int parseAndApplyMutation(MinecraftServer server ,String content) {
		String firstLine = content.strip().split("\n")[0];
		String[] parts = firstLine.split(" ");
		if (parts.length < 3) {
		wrongwrongwrong(server);
		return 1;
		}
		String[] blockFromParts = parts[0].split(":", 2);
		String blockFromId = blockFromParts[1];
		String[] blockToParts = parts[1].split(":", 2);
		String blockToId = blockToParts[1];
		String[] patternParts = parts[2].split(":", 2);
		String pattern = patternParts[1];
		// WHO CARES ABOUT ORGANIZATION I JUST LOVE CRAMMING STUFF TOGETHER!! :D
		Identifier fromblock = Identifier.tryParse(blockFromId);
		Identifier toblock = Identifier.tryParse(blockToId);
		if (fromblock == null) {
			wrongwrongwrong(server);
			return 1;
		} else if (toblock == null) {
			wrongwrongwrong(server);
			return 1;
		}


		//everyother function was ez, this is a pain to code :<
		boolean realBlockfrom  = BuiltInRegistries.BLOCK.get(fromblock).isPresent();
		if (realBlockfrom == false) {
			wrongwrongwrong(server);
			return 1;
		}
		boolean realBlockto  = BuiltInRegistries.BLOCK.get(toblock).isPresent();
		if (realBlockto == false) {
			wrongwrongwrong(server);
			return 1;
		}







		patienceIsJustSad = true;
		current = 0;
		return 0;

	}

	private int wrongwrongwrong(MinecraftServer server) {
		log.error("AI response didn't match expected format, skipping this mutation cycle.");
		server.getPlayerList().broadcastSystemMessage(Component.literal("AI response didn't match expected format, skipping this mutation cycle."), true);
		patienceIsJustSad = true;
		current = 0;
		return 0;
	}
}