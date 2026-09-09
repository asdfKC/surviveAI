package kushjoshi.wordmutation;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FallingBlock;
import net.minecraft.world.level.block.state.BlockState;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Random;

import static java.net.http.HttpClient.newHttpClient;

public class Main implements ModInitializer {
	private static final Logger log = LoggerFactory.getLogger(Main.class);
	public static int TIME = 300;
	public int current = 0;
	public boolean patienceIsJustSad = true;
	public int timePassed = 0;
	public List<String> mutationHistory = new ArrayList<>();
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
		ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> {
			timePassed = 0;
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

		String historyText = String.join(", ", mutationHistory);
		//sob
		String jsonSonion = String.format("""
		    {
		      "model": "qwen/qwen3-30b-a3b",
		      "messages": [
		        {"role": "user", "content": "You are the master, controlling this minecraft world, every 60 seconds, a new scenario is formed for the player to survive, and you create them, as the player keeps living, until the last time he dies, make the traps harder and harder, the player has survived for %d ticks, if the tick counter is close to zero, the player has recently died, as this counter goes higher and higher, make each trap more difficult to survive. The mutation applies to solid, non-air blocks starting 3 blocks below the player and extending up to 15 blocks above them, and a mixture of your three chosen blocks will be randomly scattered throughout that area. Here are the last few mutations you made: %s. Do not repeat the same three blocks as the most recent mutation — vary your choices. Each block must be a genuine hazard capable of damaging the player — choose ONLY from this list: minecraft:lava, minecraft:fire, minecraft:magma_block, minecraft:powder_snow, minecraft:sand, minecraft:red_sand, minecraft:gravel, minecraft:anvil, minecraft:sweet_berry_bush, minecraft:wither_rose, minecraft:pointed_dripstone, minecraft:campfire, minecraft:soul_campfire, minecraft:tnt. Respond with EXACTLY one single line of plain text and nothing else. Do NOT use line breaks, newlines, markdown, bullet points, numbering, or any explanation before or after. The entire response must be exactly this format, all on one line, separated by single spaces: BLOCK_1:minecraft:<block> BLOCK_2:minecraft:<block> BLOCK_3:minecraft:<block>"}
		      ]
		    }
		""", timePassed, historyText);
		HttpClient magicalCurlTypaThing = newHttpClient();
		HttpRequest plsgivemearesponse = HttpRequest.newBuilder()
				.uri(URI.create("https://ai.hackclub.com/proxy/v1/chat/completions"))
				.header("Authorization", "Bearer " + apiKey)
				.header("Content-Type", "application/json")
				.POST(HttpRequest.BodyPublishers.ofString(jsonSonion))
				.build();
		magicalCurlTypaThing.sendAsync(plsgivemearesponse, HttpResponse.BodyHandlers.ofString()).thenAccept(response -> {
					//this is the most \stupidest set of linest in the entire main.java :<
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
		String[] block1Parts = parts[0].split(":", 2);
		String block1Id = block1Parts[1];

		String[] block2Parts = parts[1].split(":", 2);
		String block2Id = block2Parts[1];

		String[] block3Parts = parts[2].split(":", 2);
		String block3Id = block3Parts[1];
		// WHO CARES ABOUT ORGANIZATION I JUST LOVE CRAMMING STUFF TOGETHER!! :D
		Identifier b1id = Identifier.tryParse(block1Id);
		Identifier b2id = Identifier.tryParse(block2Id);
		Identifier b3id = Identifier.tryParse(block3Id);
		if (b1id == null) {
			wrongwrongwrong(server);
			return 1;
		} else if (b2id == null) {
			wrongwrongwrong(server);
			return 1;
		} else if (b3id == null) {
			wrongwrongwrong(server);
			return 1;
		}


		//everyother function was ez, this is a pain to code :<
		boolean b1bo  = BuiltInRegistries.BLOCK.get(b1id).isPresent();
		if (b1bo == false) {
			wrongwrongwrong(server);
			return 1;
		}
		boolean b2bo  = BuiltInRegistries.BLOCK.get(b2id).isPresent();
		if (b2bo == false) {
			wrongwrongwrong(server);
			return 1;
		}
		boolean b3bo  = BuiltInRegistries.BLOCK.get(b3id).isPresent();
		if (b3bo == false) {
			wrongwrongwrong(server);
			return 1;
		}

		Holder.Reference<Block> refb1 = BuiltInRegistries.BLOCK.get(b1id).get();
		Holder.Reference<Block> refb2 = BuiltInRegistries.BLOCK.get(b2id).get();
		Holder.Reference<Block> refb3 = BuiltInRegistries.BLOCK.get(b3id).get();



		Block b1block =  refb1.value();
		Block b2block =  refb2.value();
		Block b3block =  refb3.value();




		for (ServerPlayer player:  server.getPlayerList().getPlayers()){
			if (!player.isAlive()){
				continue;
			}
			Random bruh = new Random();
			int max = 15;
			int min = 5;
			int radius = bruh.nextInt((max-min)+1)+min;
			BlockPos playerpos =  player.blockPosition();
			int y = playerpos.getY();


			for (int i = playerpos.getX()-radius; i<=radius+playerpos.getX(); i++){

				for (int j = playerpos.getZ()-radius; j<=radius+playerpos.getZ(); j++){

						int cringeblockthing = ((i - playerpos.getX())*(i - playerpos.getX())) + ((j - playerpos.getZ())*(j - playerpos.getZ()));
						int radiussquare = radius*radius;
						if (cringeblockthing > radiussquare) {
							continue;
						}

						for (int k = y - 3; k <= y + 15; k++) {
							int onetwothree = bruh.nextInt(3) + 1;
							BlockPos notamutpos = new BlockPos(i,k,j);
							BlockPos whyiseverythingmutpos = new BlockPos(i, (k+20), j);
							BlockState state =  player.level().getBlockState(notamutpos);



							if (state.isAir()){
								continue;
							}

							if (onetwothree == 1) {
								if (checkFall(b1block)) {
										server.execute(() -> {
											player.level().setBlock(whyiseverythingmutpos, b1block.defaultBlockState(), 3);
										});
									}  else if (checkTnt(b1block)) {
									server.execute(() -> {
										PrimedTnt tnt = new PrimedTnt(player.level(), notamutpos.getX(), notamutpos.getY(), notamutpos.getZ(), null);
										player.level().addFreshEntity(tnt);
									});
								} else {
									server.execute(() -> {
										player.level().setBlock(notamutpos, b1block.defaultBlockState(), 3);
									});
								}

							} else if (onetwothree==2) {
								if (checkFall(b2block)) {
									server.execute(() -> {
										player.level().setBlock(whyiseverythingmutpos, b2block.defaultBlockState(), 3);
									});
								}else if (checkTnt(b2block)) {
									server.execute(() -> {
										PrimedTnt tnt = new PrimedTnt(player.level(), notamutpos.getX(), notamutpos.getY(), notamutpos.getZ(), null);
										player.level().addFreshEntity(tnt);
									});
								} else {
									server.execute(() -> {
										player.level().setBlock(notamutpos, b2block.defaultBlockState(), 3);
									});
								}

							} else {

								if (checkFall(b3block)) {
									server.execute(() -> {
										player.level().setBlock(whyiseverythingmutpos, b3block.defaultBlockState(), 3);
									});} else if (checkTnt(b3block)) {
									server.execute(() -> {
										PrimedTnt tnt = new PrimedTnt(player.level(), notamutpos.getX(), notamutpos.getY(), notamutpos.getZ(), null);
										player.level().addFreshEntity(tnt);
									});
								} else {
									server.execute(() -> {
										player.level().setBlock(notamutpos, b3block.defaultBlockState(), 3);
									});
								}


							}

						}
				}
			}

		}








		mutationHistory.add(content);
		if (mutationHistory.size() > 5) {
			mutationHistory.remove(0);
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

	private boolean checkFall(Block state) {
		if (state instanceof FallingBlock) {
			return true;
		}
		else {
			return false;
		}
	}
	private boolean checkTnt(Block state) {
		return state == Blocks.TNT;
	}
}