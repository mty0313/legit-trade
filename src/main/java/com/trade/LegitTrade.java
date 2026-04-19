package com.trade;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.trade.network.ConfigSyncPacket;
import com.trade.network.TradePackets;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.registry.Registries;
import net.minecraft.server.command.CommandManager;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class LegitTrade implements ModInitializer {
    public static final String MOD_ID = "legittrade";

    @Override
    public void onInitialize() {
        TradeBlocks.register();
        TradePackets.register();

        ServerLifecycleEvents.SERVER_STARTED.register(server -> TradeConfig.load());

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
                    for (net.minecraft.server.network.ServerPlayerEntity player : ctx.getSource().getServer().getPlayerManager().getPlayerList()) {
                        ConfigSyncPacket.sendToClient(player);
                    }
                    ctx.getSource().sendFeedback(() -> Text.translatable("command.legittrade.reload.success"), true);
                    return 1;
                }));

            dispatcher.register(CommandManager.literal("tradeexportblocks")
                .requires(source -> source.hasPermissionLevel(2))
                .then(CommandManager.argument("modid", StringArgumentType.word())
                    .executes(ctx -> {
                        String modId = StringArgumentType.getString(ctx, "modid");
                        if (Identifier.tryParse(modId + ":dummy") == null) {
                            ctx.getSource().sendError(Text.literal("Invalid mod id: " + modId));
                            return 0;
                        }

                        List<String> blockIds = Registries.BLOCK.getIds().stream()
                            .filter(id -> id.getNamespace().equals(modId))
                            .filter(Registries.ITEM::containsId)
                            .map(Identifier::toString)
                            .sorted()
                            .toList();

                        Path exportPath = ctx.getSource().getServer().getRunDirectory().toPath()
                            .resolve("config")
                            .resolve("legittrade-blocks-" + modId + ".txt");

                        try {
                            Files.createDirectories(exportPath.getParent());
                            Files.write(exportPath, blockIds, StandardCharsets.UTF_8);
                            ctx.getSource().sendFeedback(() -> Text.literal(
                                "Exported " + blockIds.size() + " block ids for mod '" + modId + "' to " + exportPath
                            ), true);
                            return blockIds.size();
                        } catch (Exception e) {
                            ctx.getSource().sendError(Text.literal("Failed to export block ids: " + e.getMessage()));
                            return 0;
                        }
                    })));
        });
    }
}
