package com.trade.network;

import com.trade.LegitTrade;
import com.trade.gui.TradeScreenHandler;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

public final class TradeSelectPacket {
    public static final Identifier ID = new Identifier(LegitTrade.MOD_ID, "select_trade");

    private TradeSelectPacket() {
    }

    public static void sendToServer(int syncId, int tradeIndex) {
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeInt(syncId);
        buf.writeInt(tradeIndex);
        ClientPlayNetworking.send(ID, buf);
    }

    public static void registerServerReceiver() {
        ServerPlayNetworking.registerGlobalReceiver(ID, (server, player, handler, buf, responseSender) -> {
            int syncId = buf.readInt();
            int tradeIndex = buf.readInt();

            server.execute(() -> handleSelection(player, syncId, tradeIndex));
        });
    }

    private static void handleSelection(ServerPlayerEntity player, int syncId, int tradeIndex) {
        if (!(player.currentScreenHandler instanceof TradeScreenHandler tradeHandler)) {
            return;
        }
        if (tradeHandler.syncId != syncId) {
            return;
        }
        tradeHandler.selectTrade(player, tradeIndex);
    }
}
