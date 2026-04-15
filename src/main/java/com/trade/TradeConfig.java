package com.trade;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class TradeConfig {
	private static final Logger LOGGER = LoggerFactory.getLogger("legittrade");
	private static volatile List<TradeEntry> trades = Collections.emptyList();

	public static List<TradeEntry> getTrades() {
		return trades;
	}

	public static void setTrades(List<TradeEntry> newTrades) {
		trades = Collections.unmodifiableList(newTrades != null ? newTrades : Collections.emptyList());
	}

	public static final class TradeEntry {
		public final String input;
		public final String output;
		public final int inputCount;
		public final int outputCount;
		public final int xpReward;

		public TradeEntry(String input, String output, int inputCount, int outputCount, int xpReward) {
			this.input = input;
			this.output = output;
			this.inputCount = inputCount;
			this.outputCount = outputCount;
			this.xpReward = xpReward;
		}

		public Item getInputItem() {
			Identifier id = Identifier.tryParse(input);
			return id != null ? Registries.ITEM.get(id) : null;
		}

		public Item getOutputItem() {
			Identifier id = Identifier.tryParse(output);
			return id != null ? Registries.ITEM.get(id) : null;
		}

		public boolean isValid() {
			Identifier inputId = Identifier.tryParse(input);
			Identifier outputId = Identifier.tryParse(output);
			if (inputId == null || outputId == null) {
				return false;
			}
			if (!Registries.ITEM.containsId(inputId) || !Registries.ITEM.containsId(outputId)) {
				return false;
			}
			return inputCount > 0 && outputCount > 0 && xpReward >= 0;
		}
	}

	// Gson deserialization target - uses mutable fields
	private static final class RawTradeEntry {
		String input;
		String output;
		int inputCount = 1;
		int outputCount = 1;
		int xpReward = 0;

		TradeEntry toTradeEntry() {
			return new TradeEntry(input, output, inputCount, outputCount, xpReward);
		}
	}

	public static void load() {
		Path configPath = Path.of("config/legittrade.json");
		Gson gson = new GsonBuilder().setPrettyPrinting().create();

		try {
			if (!Files.exists(configPath)) {
				Files.createDirectories(configPath.getParent());
				List<TradeEntry> defaults = getDefaultTrades();
				Files.writeString(configPath, gson.toJson(defaults));
				trades = Collections.unmodifiableList(defaults);
				return;
			}

			String json = Files.readString(configPath);
			List<RawTradeEntry> rawList = gson.fromJson(json, new TypeToken<List<RawTradeEntry>>(){}.getType());

			List<TradeEntry> validTrades = new ArrayList<>();
			if (rawList != null) {
				for (RawTradeEntry raw : rawList) {
					TradeEntry entry = raw.toTradeEntry();
					if (entry.isValid()) {
						validTrades.add(entry);
					} else {
						LOGGER.warn("Invalid trade entry: {} -> {} (count: {}/{}, xp: {})",
							raw.input, raw.output, raw.inputCount, raw.outputCount, raw.xpReward);
					}
				}
			}

			if (validTrades.isEmpty()) {
				LOGGER.warn("No valid trades loaded, using defaults");
				trades = Collections.unmodifiableList(getDefaultTrades());
			} else {
				trades = Collections.unmodifiableList(validTrades);
				LOGGER.info("Loaded {} valid trades", validTrades.size());
			}
		} catch (Exception e) {
			LOGGER.error("Failed to load trade config", e);
			trades = Collections.unmodifiableList(getDefaultTrades());
		}
	}

	private static List<TradeEntry> getDefaultTrades() {
		List<TradeEntry> defaults = new ArrayList<>();
		defaults.add(new TradeEntry("minecraft:dirt", "minecraft:diamond", 64, 1, 100));
		return defaults;
	}
}
