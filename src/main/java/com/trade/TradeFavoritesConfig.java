package com.trade;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class TradeFavoritesConfig {
    private static final Logger LOGGER = LoggerFactory.getLogger("legittrade");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final int MAX_FAVORITES_PER_GROUP = TradeConfig.MAX_TRADES_PER_GROUP;

    private static Map<UUID, Map<String, List<String>>> favoritesByPlayer = new HashMap<>();

    private TradeFavoritesConfig() {
    }

    private static Path getConfigPath() {
        return FabricLoader.getInstance().getConfigDir().resolve("legittrade_favorites.json");
    }

    public static synchronized void load() {
        Path path = getConfigPath();
        try {
            if (!Files.exists(path)) {
                Files.createDirectories(path.getParent());
                favoritesByPlayer = new HashMap<>();
                save();
                return;
            }

            String json = Files.readString(path);
            Map<String, Map<String, List<String>>> raw = GSON.fromJson(json,
                new TypeToken<Map<String, Map<String, List<String>>>>() {}.getType());

            Map<UUID, Map<String, List<String>>> parsed = new HashMap<>();
            if (raw != null) {
                for (Map.Entry<String, Map<String, List<String>>> entry : raw.entrySet()) {
                    UUID playerId;
                    try {
                        playerId = UUID.fromString(entry.getKey());
                    } catch (IllegalArgumentException ignored) {
                        continue;
                    }
                    Map<String, List<String>> groups = sanitizeGroupFavorites(entry.getValue());
                    if (!groups.isEmpty()) {
                        parsed.put(playerId, groups);
                    }
                }
            }
            favoritesByPlayer = parsed;
        } catch (Exception e) {
            LOGGER.error("Failed to load favorites config", e);
            favoritesByPlayer = new HashMap<>();
        }
    }

    public static synchronized void save() {
        Path path = getConfigPath();
        try {
            Files.createDirectories(path.getParent());
            Map<String, Map<String, List<String>>> serializable = new LinkedHashMap<>();
            for (Map.Entry<UUID, Map<String, List<String>>> entry : favoritesByPlayer.entrySet()) {
                serializable.put(entry.getKey().toString(), entry.getValue());
            }
            Files.writeString(path, GSON.toJson(serializable));
        } catch (Exception e) {
            LOGGER.error("Failed to save favorites config", e);
        }
    }

    public static synchronized boolean isFavorite(UUID playerId, String groupName, String tradeKey) {
        Map<String, List<String>> byGroup = favoritesByPlayer.get(playerId);
        if (byGroup == null) {
            return false;
        }
        List<String> keys = byGroup.get(normalizeGroup(groupName));
        return keys != null && keys.contains(tradeKey);
    }

    public static synchronized boolean setFavorite(UUID playerId, String groupName, String tradeKey, boolean favorite) {
        if (tradeKey == null || tradeKey.isBlank()) {
            return false;
        }

        String safeGroup = normalizeGroup(groupName);
        Map<String, List<String>> byGroup = favoritesByPlayer.computeIfAbsent(playerId, id -> new LinkedHashMap<>());
        List<String> keys = byGroup.computeIfAbsent(safeGroup, g -> new ArrayList<>());

        keys.remove(tradeKey);
        if (favorite) {
            keys.add(0, tradeKey);
            if (keys.size() > MAX_FAVORITES_PER_GROUP) {
                keys.subList(MAX_FAVORITES_PER_GROUP, keys.size()).clear();
            }
        }

        if (keys.isEmpty()) {
            byGroup.remove(safeGroup);
        }
        if (byGroup.isEmpty()) {
            favoritesByPlayer.remove(playerId);
        }

        save();
        return favorite;
    }

    public static synchronized boolean isValidTradeKeyForGroup(String groupName, String tradeKey) {
        if (tradeKey == null || tradeKey.isBlank()) {
            return false;
        }
        String safeGroup = normalizeGroup(groupName);
        for (TradeConfig.TradeGroup group : TradeConfig.getTradeGroups()) {
            if (!normalizeGroup(group.group).equals(safeGroup)) {
                continue;
            }
            for (TradeConfig.TradeEntry trade : group.trades) {
                if (TradeConfig.buildTradeKey(safeGroup, trade).equals(tradeKey)) {
                    return true;
                }
            }
            return false;
        }
        return false;
    }

    public static synchronized List<TradeConfig.TradeGroup> getOrderedGroupsForPlayer(UUID playerId) {
        List<TradeConfig.TradeGroup> baseGroups = TradeConfig.getTradeGroups();
        if (baseGroups.isEmpty()) {
            return List.of();
        }

        Map<String, List<String>> byGroup = favoritesByPlayer.getOrDefault(playerId, Collections.emptyMap());
        List<TradeConfig.TradeGroup> ordered = new ArrayList<>(baseGroups.size());

        for (TradeConfig.TradeGroup baseGroup : baseGroups) {
            String groupName = normalizeGroup(baseGroup.group);
            List<String> favoriteKeys = byGroup.getOrDefault(groupName, List.of());

            Map<String, TradeConfig.TradeEntry> keyToTrade = new LinkedHashMap<>();
            for (TradeConfig.TradeEntry trade : baseGroup.trades) {
                keyToTrade.put(TradeConfig.buildTradeKey(groupName, trade), trade);
            }

            LinkedHashSet<String> orderedKeys = new LinkedHashSet<>();
            for (String favoriteKey : favoriteKeys) {
                if (keyToTrade.containsKey(favoriteKey)) {
                    orderedKeys.add(favoriteKey);
                }
            }
            orderedKeys.addAll(keyToTrade.keySet());

            List<TradeConfig.TradeEntry> trades = new ArrayList<>(orderedKeys.size());
            for (String key : orderedKeys) {
                TradeConfig.TradeEntry entry = keyToTrade.get(key);
                if (entry != null) {
                    boolean isFavorite = favoriteKeys.contains(key);
                    trades.add(entry.withFavorite(isFavorite));
                }
            }

            if (!trades.isEmpty()) {
                ordered.add(new TradeConfig.TradeGroup(baseGroup.group, trades));
            }
        }

        return ordered;
    }

    public static synchronized List<TradeConfig.TradeEntry> flattenGroups(List<TradeConfig.TradeGroup> groups) {
        List<TradeConfig.TradeEntry> flattened = new ArrayList<>();
        for (TradeConfig.TradeGroup group : groups) {
            flattened.addAll(group.trades);
        }
        return flattened;
    }

    public static synchronized void reconcileWithCurrentConfig() {
        if (favoritesByPlayer.isEmpty()) {
            return;
        }

        Map<String, Set<String>> validKeysByGroup = new HashMap<>();
        for (TradeConfig.TradeGroup group : TradeConfig.getTradeGroups()) {
            String groupName = normalizeGroup(group.group);
            Set<String> keys = new LinkedHashSet<>();
            for (TradeConfig.TradeEntry trade : group.trades) {
                keys.add(TradeConfig.buildTradeKey(groupName, trade));
            }
            validKeysByGroup.put(groupName, keys);
        }

        boolean changed = false;
        Map<UUID, Map<String, List<String>>> rebuilt = new HashMap<>();

        for (Map.Entry<UUID, Map<String, List<String>>> playerEntry : favoritesByPlayer.entrySet()) {
            Map<String, List<String>> cleanedGroups = new LinkedHashMap<>();

            for (Map.Entry<String, List<String>> groupEntry : playerEntry.getValue().entrySet()) {
                String groupName = normalizeGroup(groupEntry.getKey());
                Set<String> validKeys = validKeysByGroup.get(groupName);
                if (validKeys == null || validKeys.isEmpty()) {
                    changed = true;
                    continue;
                }

                List<String> cleanedKeys = new ArrayList<>();
                for (String key : groupEntry.getValue()) {
                    if (validKeys.contains(key) && !cleanedKeys.contains(key)) {
                        cleanedKeys.add(key);
                    } else {
                        changed = true;
                    }
                }

                if (!cleanedKeys.isEmpty()) {
                    cleanedGroups.put(groupName, cleanedKeys);
                }
            }

            if (!cleanedGroups.isEmpty()) {
                rebuilt.put(playerEntry.getKey(), cleanedGroups);
            }
        }

        if (changed || rebuilt.size() != favoritesByPlayer.size()) {
            favoritesByPlayer = rebuilt;
            save();
        }
    }

    private static Map<String, List<String>> sanitizeGroupFavorites(Map<String, List<String>> rawGroups) {
        if (rawGroups == null || rawGroups.isEmpty()) {
            return new LinkedHashMap<>();
        }

        Map<String, List<String>> sanitized = new LinkedHashMap<>();
        for (Map.Entry<String, List<String>> groupEntry : rawGroups.entrySet()) {
            String groupName = normalizeGroup(groupEntry.getKey());
            List<String> keys = groupEntry.getValue();
            if (keys == null || keys.isEmpty()) {
                continue;
            }

            List<String> cleaned = new ArrayList<>();
            for (String key : keys) {
                if (key == null || key.isBlank() || cleaned.contains(key)) {
                    continue;
                }
                cleaned.add(key);
                if (cleaned.size() >= MAX_FAVORITES_PER_GROUP) {
                    break;
                }
            }

            if (!cleaned.isEmpty()) {
                sanitized.put(groupName, cleaned);
            }
        }
        return sanitized;
    }

    private static String normalizeGroup(String groupName) {
        if (groupName == null || groupName.isBlank()) {
            return "Default";
        }
        return groupName;
    }
}
