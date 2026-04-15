package com.trade;

import com.trade.gui.TradeScreen;
import com.trade.gui.TradeScreenHandler;
import com.trade.network.ConfigSyncPacket;
import com.trade.network.TradePackets;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.screenhandler.v1.ScreenRegistry;

public class LegitTradeClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        // Initialize empty config on client (will be synced from server)
        TradeConfig.setTrades(java.util.Collections.emptyList());

        // Register client screen
        ScreenRegistry.register(TradePackets.TRADE_SCREEN_HANDLER, TradeScreen::new);

        // Config sync receiver
        ClientPlayNetworking.registerGlobalReceiver(ConfigSyncPacket.ID, (client, handler, buf, responseSender) -> {
            TradeConfig.setTrades(ConfigSyncPacket.read(buf));
        });
    }
}
