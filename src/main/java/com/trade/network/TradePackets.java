package com.trade.network;

import com.trade.LegitTrade;
import com.trade.gui.TradeScreenHandler;
import net.fabricmc.fabric.api.screenhandler.v1.ScreenHandlerRegistry;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.util.Identifier;

public class TradePackets {
    public static final ScreenHandlerType<TradeScreenHandler> TRADE_SCREEN_HANDLER =
        ScreenHandlerRegistry.registerSimple(
            new Identifier(LegitTrade.MOD_ID, "trade"),
            TradeScreenHandler::new
        );

    public static void register() {
        // Registration happens in static initializer
    }
}
