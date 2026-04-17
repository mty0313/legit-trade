package com.trade;

import com.trade.gui.TradeScreen;
import com.trade.network.ConfigSyncPacket;
import com.trade.network.TradePackets;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.screen.ingame.HandledScreens;

import java.util.List;

public class LegitTradeClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        HandledScreens.register(TradePackets.TRADE_SCREEN_HANDLER, TradeScreen::new);

        ClientPlayNetworking.registerGlobalReceiver(ConfigSyncPacket.ID, (client, handler, buf, responseSender) -> {
            var groups = ConfigSyncPacket.read(buf);
            client.execute(() -> TradeConfig.setTradeGroups(groups));
        });

        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) ->
            client.execute(() -> TradeConfig.setTradeGroups(List.of()))
        );
    }
}
