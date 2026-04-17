package com.trade;

import com.trade.gui.TradeScreenHandler;
import com.trade.network.ConfigSyncPacket;
import com.trade.network.FavoriteTogglePacket;
import com.trade.network.TradePackets;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.command.CommandManager;
import net.minecraft.text.Text;

public class LegitTrade implements ModInitializer {
    public static final String MOD_ID = "legittrade";

    @Override
    public void onInitialize() {
        TradeConfig.load();
        TradeFavoritesConfig.load();
        TradeFavoritesConfig.reconcileWithCurrentConfig();

        TradeBlocks.register();
        TradePackets.register();

        ServerPlayNetworking.registerGlobalReceiver(FavoriteTogglePacket.ID, (server, player, handler, buf, responseSender) -> {
            FavoriteTogglePacket.Payload payload;
            try {
                payload = FavoriteTogglePacket.read(buf);
            } catch (RuntimeException ignored) {
                return;
            }

            server.execute(() -> {
                if (!TradeFavoritesConfig.isValidTradeKeyForGroup(payload.groupName(), payload.tradeKey())) {
                    return;
                }

                TradeFavoritesConfig.setFavorite(player.getUuid(), payload.groupName(), payload.tradeKey(), payload.favorite());
                var orderedGroups = TradeFavoritesConfig.getOrderedGroupsForPlayer(player.getUuid());
                ConfigSyncPacket.sendToClient(player, orderedGroups);

                if (player.currentScreenHandler instanceof TradeScreenHandler tradeHandler) {
                    tradeHandler.setAvailableTrades(TradeFavoritesConfig.flattenGroups(orderedGroups));
                }
            });
        });

        // Sync config when player joins
        ServerEntityEvents.ENTITY_LOAD.register((entity, world) -> {
            if (entity instanceof net.minecraft.server.network.ServerPlayerEntity player) {
                ConfigSyncPacket.sendToClient(player, TradeFavoritesConfig.getOrderedGroupsForPlayer(player.getUuid()));
            }
        });

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            dispatcher.register(CommandManager.literal("tradereload")
                .requires(source -> source.hasPermissionLevel(2))
                .executes(ctx -> {
                    TradeConfig.load();
                    TradeFavoritesConfig.reconcileWithCurrentConfig();

                    for (net.minecraft.server.network.ServerPlayerEntity player : ctx.getSource().getServer().getPlayerManager().getPlayerList()) {
                        var orderedGroups = TradeFavoritesConfig.getOrderedGroupsForPlayer(player.getUuid());
                        ConfigSyncPacket.sendToClient(player, orderedGroups);
                        if (player.currentScreenHandler instanceof TradeScreenHandler tradeHandler) {
                            tradeHandler.setAvailableTrades(TradeFavoritesConfig.flattenGroups(orderedGroups));
                        }
                    }
                    ctx.getSource().sendFeedback(() -> Text.translatable("command.legittrade.reload.success"), true);
                    return 1;
                }));
        });
    }
}
