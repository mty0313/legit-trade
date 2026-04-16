package com.trade;

import com.trade.gui.TradeScreen;
import com.trade.gui.TradeScreenHandler;
import com.trade.network.ConfigSyncPacket;
import com.trade.network.TradePackets;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.screen.ingame.HandledScreens;

public class LegitTradeClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        HandledScreens.register(TradePackets.TRADE_SCREEN_HANDLER, TradeScreen::new);

        ClientPlayNetworking.registerGlobalReceiver(ConfigSyncPacket.ID, (client, handler, buf, responseSender) -> {
            var trades = ConfigSyncPacket.read(buf);
            client.execute(() -> TradeConfig.setTrades(trades));
        });
    }
}
