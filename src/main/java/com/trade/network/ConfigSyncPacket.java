package com.trade.network;

import com.trade.LegitTrade;
import com.trade.TradeConfig;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;

public class ConfigSyncPacket {
    public static final Identifier ID = new Identifier(LegitTrade.MOD_ID, "config_sync");

    public static PacketByteBuf write(List<TradeConfig.TradeGroup> groups) {
        PacketByteBuf buf = PacketByteBufs.create();
        int groupCount = Math.min(groups.size(), TradeConfig.MAX_GROUPS);
        buf.writeInt(groupCount);
        for (int g = 0; g < groupCount; g++) {
            TradeConfig.TradeGroup group = groups.get(g);
            buf.writeString(group.group, TradeConfig.MAX_GROUP_NAME_LENGTH);
            int tradeCount = Math.min(group.trades.size(), TradeConfig.MAX_TRADES_PER_GROUP);
            buf.writeInt(tradeCount);
            for (int i = 0; i < tradeCount; i++) {
                TradeConfig.TradeEntry trade = group.trades.get(i);
                buf.writeString(trade.input, TradeConfig.MAX_ITEM_ID_LENGTH);
                buf.writeString(trade.output, TradeConfig.MAX_ITEM_ID_LENGTH);
                writeOptionalString(buf, trade.inputNbt, TradeConfig.MAX_NBT_LENGTH);
                writeOptionalString(buf, trade.outputNbt, TradeConfig.MAX_NBT_LENGTH);
                buf.writeString(trade.nbtMatchMode.name(), 16);
                buf.writeInt(trade.inputCount);
                buf.writeInt(trade.outputCount);
                buf.writeInt(trade.xpReward);
            }
        }
        return buf;
    }

    public static List<TradeConfig.TradeGroup> read(PacketByteBuf buf) {
        try {
            int groupCount = buf.readInt();
            if (groupCount < 0 || groupCount > TradeConfig.MAX_GROUPS) {
                return List.of();
            }

            List<TradeConfig.TradeGroup> groups = new ArrayList<>(groupCount);
            for (int g = 0; g < groupCount; g++) {
                String groupName = buf.readString(TradeConfig.MAX_GROUP_NAME_LENGTH);
                int tradeCount = buf.readInt();
                if (tradeCount < 0 || tradeCount > TradeConfig.MAX_TRADES_PER_GROUP) {
                    return List.of();
                }

                List<TradeConfig.TradeEntry> trades = new ArrayList<>(tradeCount);
                for (int i = 0; i < tradeCount; i++) {
                    String input = buf.readString(TradeConfig.MAX_ITEM_ID_LENGTH);
                    String output = buf.readString(TradeConfig.MAX_ITEM_ID_LENGTH);
                    String inputNbt = readOptionalString(buf, TradeConfig.MAX_NBT_LENGTH);
                    String outputNbt = readOptionalString(buf, TradeConfig.MAX_NBT_LENGTH);
                    String nbtMatchModeStr = buf.readString(16);
                    int inputCount = buf.readInt();
                    int outputCount = buf.readInt();
                    int xpReward = buf.readInt();

                    TradeConfig.NbtMatchMode nbtMatchMode = TradeConfig.NbtMatchMode.EXACT;
                    try {
                        nbtMatchMode = TradeConfig.NbtMatchMode.valueOf(nbtMatchModeStr);
                    } catch (IllegalArgumentException ignored) {}

                    TradeConfig.TradeEntry entry = new TradeConfig.TradeEntry(input, output, inputNbt, outputNbt, nbtMatchMode, inputCount, outputCount, xpReward);
                    if (entry.isValid()) {
                        trades.add(entry);
                    }
                }

                if (!trades.isEmpty()) {
                    groups.add(new TradeConfig.TradeGroup(groupName, trades));
                }
            }
            return groups;
        } catch (RuntimeException ignored) {
            return List.of();
        }
    }

    private static void writeOptionalString(PacketByteBuf buf, String value, int maxLength) {
        boolean hasValue = value != null && !value.isBlank();
        buf.writeBoolean(hasValue);
        if (hasValue) {
            buf.writeString(value, maxLength);
        }
    }

    private static String readOptionalString(PacketByteBuf buf, int maxLength) {
        if (!buf.readBoolean()) {
            return null;
        }
        String value = buf.readString(maxLength);
        return value.isBlank() ? null : value;
    }

    public static void sendToClient(net.minecraft.server.network.ServerPlayerEntity player) {
        ServerPlayNetworking.send(player, ID, write(TradeConfig.getTradeGroups()));
    }
}
