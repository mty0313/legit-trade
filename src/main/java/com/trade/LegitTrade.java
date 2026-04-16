package com.trade;

import com.trade.network.ConfigSyncPacket;
import com.trade.network.TradePackets;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.minecraft.server.command.CommandManager;
import net.minecraft.text.Text;

public class LegitTrade implements ModInitializer {
    public static final String MOD_ID = "legittrade";

    @Override
    public void onInitialize() {
        TradeConfig.load();
        TradeBlocks.register();
        TradePackets.register();

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
