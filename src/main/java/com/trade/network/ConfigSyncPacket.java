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
    private static final int MAX_GROUPS = 128;
    private static final int MAX_TRADES_PER_GROUP = 1024;
    private static final int MAX_ITEM_ID_LENGTH = 128;
    private static final int MAX_GROUP_NAME_LENGTH = 64;

    public static final Identifier ID = new Identifier(LegitTrade.MOD_ID, "config_sync");

    public static PacketByteBuf write(List<TradeConfig.TradeGroup> groups) {
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeInt(groups.size());
        for (TradeConfig.TradeGroup group : groups) {
            buf.writeString(group.group, MAX_GROUP_NAME_LENGTH);
            buf.writeInt(group.trades.size());
            for (TradeConfig.TradeEntry trade : group.trades) {
                buf.writeString(trade.input, MAX_ITEM_ID_LENGTH);
                buf.writeString(trade.output, MAX_ITEM_ID_LENGTH);
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
            if (groupCount < 0 || groupCount > MAX_GROUPS) {
                return List.of();
            }

            List<TradeConfig.TradeGroup> groups = new ArrayList<>(groupCount);
            for (int g = 0; g < groupCount; g++) {
                String groupName = buf.readString(MAX_GROUP_NAME_LENGTH);
                int tradeCount = buf.readInt();
                if (tradeCount < 0 || tradeCount > MAX_TRADES_PER_GROUP) {
                    return List.of();
                }

                List<TradeConfig.TradeEntry> trades = new ArrayList<>(tradeCount);
                for (int i = 0; i < tradeCount; i++) {
                    String input = buf.readString(MAX_ITEM_ID_LENGTH);
                    String output = buf.readString(MAX_ITEM_ID_LENGTH);
                    int inputCount = buf.readInt();
                    int outputCount = buf.readInt();
                    int xpReward = buf.readInt();

                    TradeConfig.TradeEntry entry = new TradeConfig.TradeEntry(input, output, inputCount, outputCount, xpReward);
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

    public static void sendToClient(net.minecraft.server.network.ServerPlayerEntity player) {
        ServerPlayNetworking.send(player, ID, write(TradeConfig.getTradeGroups()));
    }
}
