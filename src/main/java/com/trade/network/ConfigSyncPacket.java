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
    private static final int MAX_TRADES = 1024;
    private static final int MAX_ITEM_ID_LENGTH = 128;

    public static final Identifier ID = new Identifier(LegitTrade.MOD_ID, "config_sync");

    public static PacketByteBuf write(List<TradeConfig.TradeEntry> trades) {
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeInt(trades.size());
        for (TradeConfig.TradeEntry trade : trades) {
            buf.writeString(trade.input);
            buf.writeString(trade.output);
            buf.writeInt(trade.inputCount);
            buf.writeInt(trade.outputCount);
            buf.writeInt(trade.xpReward);
        }
        return buf;
    }

    public static List<TradeConfig.TradeEntry> read(PacketByteBuf buf) {
        try {
            int size = buf.readInt();
            if (size < 0 || size > MAX_TRADES) {
                return List.of();
            }

            List<TradeConfig.TradeEntry> trades = new ArrayList<>(size);
            for (int i = 0; i < size; i++) {
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
            return trades;
        } catch (RuntimeException ignored) {
            return List.of();
        }
    }

    public static void sendToClient(net.minecraft.server.network.ServerPlayerEntity player) {
        ServerPlayNetworking.send(player, ID, write(TradeConfig.getTrades()));
    }
}
