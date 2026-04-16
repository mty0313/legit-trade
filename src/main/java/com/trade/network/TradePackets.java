package com.trade.network;

import com.trade.LegitTrade;
import com.trade.gui.TradeScreenHandler;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.resource.featuretoggle.FeatureFlags;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.util.Identifier;

public class TradePackets {
    public static final ScreenHandlerType<TradeScreenHandler> TRADE_SCREEN_HANDLER = Registry.register(
        Registries.SCREEN_HANDLER,
        new Identifier(LegitTrade.MOD_ID, "trade"),
        new ScreenHandlerType<>(TradeScreenHandler::new, FeatureFlags.VANILLA_FEATURES)
    );

    public static void register() {
    }
}
