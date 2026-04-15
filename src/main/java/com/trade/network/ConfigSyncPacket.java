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
        int size = buf.readInt();
        List<TradeConfig.TradeEntry> trades = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            String input = buf.readString();
            String output = buf.readString();
            int inputCount = buf.readInt();
            int outputCount = buf.readInt();
            int xpReward = buf.readInt();
            trades.add(new TradeConfig.TradeEntry(input, output, inputCount, outputCount, xpReward));
        }
        return trades;
    }

    public static void sendToClient(net.minecraft.server.network.ServerPlayerEntity player) {
        ServerPlayNetworking.send(player, ID, write(TradeConfig.getTrades()));
    }
}
