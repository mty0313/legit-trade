package com.trade;

import com.trade.network.ConfigSyncPacket;
import com.trade.network.TradePackets;
import com.trade.network.TradeSelectPacket;
import com.trade.web.WebConfig;
import com.trade.web.WebServer;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.server.command.CommandManager;
import net.minecraft.text.Text;

public class LegitTrade implements ModInitializer {
    public static final String MOD_ID = "legittrade";

    @Override
    public void onInitialize() {
        TradeBlocks.register();
        TradePackets.register();
        TradeSelectPacket.registerServerReceiver();

        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            TradeConfig.load();
            WebConfig webConfig = WebConfig.load();
            WebServer.start(webConfig);
            WebServer.setConfigSavedCallback(() -> server.execute(() -> {
                for (net.minecraft.server.network.ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
                    ConfigSyncPacket.sendToClient(player);
                }
            }));
        });

        ServerLifecycleEvents.SERVER_STOPPING.register(server -> WebServer.stopServer());

        // Sync config when player joins
        ServerEntityEvents.ENTITY_LOAD.register((entity, world) -> {
            if (entity instanceof net.minecraft.server.network.ServerPlayerEntity player) {
                ConfigSyncPacket.sendToClient(player);
            }
        });

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            dispatcher.register(CommandManager.literal("tradereload")
                .requires(source -> source.hasPermissionLevel(2))
                .executes(ctx -> {
                    TradeConfig.load();
                    // Sync to all online players
                    for (net.minecraft.server.network.ServerPlayerEntity player : ctx.getSource().getServer().getPlayerManager().getPlayerList()) {
                        ConfigSyncPacket.sendToClient(player);
                    }
                    ctx.getSource().sendFeedback(() -> Text.translatable("command.legittrade.reload.success"), true);
                    return 1;
                }));
        });
    }
}
