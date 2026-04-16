package com.trade;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class TradeConfig {
	private static final Logger LOGGER = LoggerFactory.getLogger("legittrade");
	private static final int MAX_STACK_COUNT = 64;
	private static volatile List<TradeGroup> tradeGroups = Collections.emptyList();
	private static volatile List<TradeEntry> trades = Collections.emptyList();

	public static List<TradeGroup> getTradeGroups() {
		return tradeGroups;
	}

	public static List<TradeEntry> getTrades() {
		return trades;
	}

	public static void setTradeGroups(List<TradeGroup> newGroups) {
		List<TradeGroup> safeGroups = freezeGroups(newGroups);
		tradeGroups = Collections.unmodifiableList(safeGroups);

		List<TradeEntry> flattened = new ArrayList<>();
		for (TradeGroup group : safeGroups) {
			flattened.addAll(group.trades);
		}
		trades = Collections.unmodifiableList(flattened);
	}

	public static void setTrades(List<TradeEntry> newTrades) {
		setTradeGroups(List.of(new TradeGroup("Default", newTrades)));
	}

	public static final class TradeGroup {
		public final String group;
		public final List<TradeEntry> trades;

		public TradeGroup(String group, List<TradeEntry> trades) {
			this.group = (group == null || group.isBlank()) ? "Default" : group;
			this.trades = Collections.unmodifiableList(new ArrayList<>(trades != null ? trades : Collections.emptyList()));
		}
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
			if (id == null || !Registries.ITEM.containsId(id)) {
				return null;
			}
			return Registries.ITEM.get(id);
		}

		public Item getOutputItem() {
			Identifier id = Identifier.tryParse(output);
			if (id == null || !Registries.ITEM.containsId(id)) {
				return null;
			}
			return Registries.ITEM.get(id);
		}

		public boolean isValid() {
			Identifier inputId = Identifier.tryParse(input);
			Identifier outputId = Identifier.tryParse(output);
			if (inputId == null || outputId == null) {
				return false;
			}
			return inputCount >= 1 && inputCount <= MAX_STACK_COUNT
				&& outputCount >= 1 && outputCount <= MAX_STACK_COUNT
				&& xpReward >= 0;
		}
	}

	private static int clampTradeCount(int count) {
		if (count <= 0) {
			return 1;
		}
		return Math.min(count, MAX_STACK_COUNT);
	}

	private static List<TradeGroup> freezeGroups(List<TradeGroup> groups) {
		if (groups == null || groups.isEmpty()) {
			return Collections.emptyList();
		}
		List<TradeGroup> safe = new ArrayList<>(groups.size());
		for (TradeGroup group : groups) {
			if (group == null || group.trades.isEmpty()) {
				continue;
			}
			safe.add(new TradeGroup(group.group, group.trades));
		}
		return safe;
	}

	private static List<TradeGroup> toSingleDefaultGroup(List<TradeEntry> entries) {
		if (entries.isEmpty()) {
			return Collections.emptyList();
		}
		return List.of(new TradeGroup("Default", entries));
	}

	private static List<TradeGroup> toValidGroups(List<RawTradeGroup> rawGroups) {
		List<TradeGroup> groups = new ArrayList<>();
		if (rawGroups == null) {
			return groups;
		}

		for (RawTradeGroup rawGroup : rawGroups) {
			if (rawGroup == null) {
				continue;
			}
			String groupName = (rawGroup.group == null || rawGroup.group.isBlank()) ? "Default" : rawGroup.group;
			List<TradeEntry> validTrades = toValidTrades(rawGroup.trades, groupName);
			if (!validTrades.isEmpty()) {
				groups.add(new TradeGroup(groupName, validTrades));
			}
		}
		return groups;
	}

	private static List<TradeEntry> toValidTrades(List<RawTradeEntry> rawList, String groupName) {
		List<TradeEntry> validTrades = new ArrayList<>();
		if (rawList == null) {
			return validTrades;
		}

		Set<String> seen = new HashSet<>();
		for (RawTradeEntry raw : rawList) {
			if (raw == null) {
				continue;
			}
			TradeEntry entry = raw.toTradeEntry();
			if (!entry.isValid()) {
				LOGGER.warn("Invalid trade entry in group '{}': {} -> {} (count: {}/{}, xp: {})",
					groupName, raw.input, raw.output, raw.inputCount, raw.outputCount, raw.xpReward);
				continue;
			}

			String key = entry.input + "|" + entry.output + "|" + entry.inputCount + "|" + entry.outputCount + "|" + entry.xpReward;
			if (!seen.add(key)) {
				LOGGER.warn("Duplicate trade entry ignored in group '{}': {} -> {} (count: {}/{}, xp: {})",
					groupName, entry.input, entry.output, entry.inputCount, entry.outputCount, entry.xpReward);
				continue;
			}

			validTrades.add(entry);
		}
		return validTrades;
	}

	private static final class RawTradeEntry {
		String input;
		String output;
		int inputCount = 1;
		int outputCount = 1;
		int xpReward = 0;

		TradeEntry toTradeEntry() {
			return new TradeEntry(input, output, clampTradeCount(inputCount), clampTradeCount(outputCount), xpReward);
		}
	}

	private static final class RawTradeGroup {
		String group = "Default";
		List<RawTradeEntry> trades = Collections.emptyList();
	}

	public static void load() {
		Path configPath = Path.of("config/legittrade.json");
		Gson gson = new GsonBuilder().setPrettyPrinting().create();

		try {
			if (!Files.exists(configPath)) {
				Files.createDirectories(configPath.getParent());
				List<TradeGroup> defaults = getDefaultTradeGroups();
				Files.writeString(configPath, gson.toJson(defaults));
				setTradeGroups(defaults);
				return;
			}

			String json = Files.readString(configPath);
			JsonElement root = gson.fromJson(json, JsonElement.class);

			List<TradeGroup> validGroups = Collections.emptyList();
			if (root != null && root.isJsonArray()) {
				JsonArray array = root.getAsJsonArray();
				if (!array.isEmpty() && array.get(0).isJsonObject()) {
					JsonObject first = array.get(0).getAsJsonObject();
					if (first.has("group") || first.has("trades")) {
						List<RawTradeGroup> rawGroups = gson.fromJson(array, new TypeToken<List<RawTradeGroup>>() {}.getType());
						validGroups = toValidGroups(rawGroups);
					} else {
						List<RawTradeEntry> rawList = gson.fromJson(array, new TypeToken<List<RawTradeEntry>>() {}.getType());
						validGroups = toSingleDefaultGroup(toValidTrades(rawList, "Default"));
					}
				}
			}

			if (validGroups.isEmpty()) {
				LOGGER.warn("No valid trade groups loaded, using defaults");
				setTradeGroups(getDefaultTradeGroups());
			} else {
				setTradeGroups(validGroups);
				LOGGER.info("Loaded {} trade groups and {} trades", getTradeGroups().size(), getTrades().size());
			}
		} catch (Exception e) {
			LOGGER.error("Failed to load trade config", e);
			setTradeGroups(getDefaultTradeGroups());
		}
	}

	private static List<TradeGroup> getDefaultTradeGroups() {
		List<TradeEntry> buildingTrades = new ArrayList<>();
		buildingTrades.add(new TradeEntry("minecraft:dirt", "minecraft:diamond", 64, 1, 100));
		return List.of(new TradeGroup("Building", buildingTrades));
	}
}
