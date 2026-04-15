package com.trade.network;

import com.trade.LegitTrade;
import com.trade.TradeConfig;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public class ExecuteTradePacket {
    public static final Identifier ID = new Identifier(LegitTrade.MOD_ID, "execute_trade");

    public static PacketByteBuf write(int tradeIndex) {
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeInt(tradeIndex);
        return buf;
    }

    public static void sendToServer(int tradeIndex) {
        ClientPlayNetworking.send(ID, write(tradeIndex));
    }

    public static void registerServer() {
        ServerPlayNetworking.registerGlobalReceiver(ID, (server, player, handler, buf, responseSender) -> {
            int tradeIndex = buf.readInt();

            server.execute(() -> executeTrade(player, tradeIndex));
        });
    }

    private static void executeTrade(ServerPlayerEntity player, int tradeIndex) {
        // Validate trade index
        var trades = TradeConfig.getTrades();
        if (tradeIndex < 0 || tradeIndex >= trades.size()) {
            sendFailure(player, "Invalid trade");
            return;
        }

        TradeConfig.TradeEntry trade = trades.get(tradeIndex);

        // Validate items
        if (trade.getInputItem() == null || trade.getOutputItem() == null) {
            sendFailure(player, "Invalid trade configuration");
            return;
        }

        // Count total input items in player inventory
        int totalCount = countItems(player, trade.getInputItem());
        if (totalCount < trade.inputCount) {
            sendFailure(player, "Insufficient items");
            return;
        }

        // Consume input from multiple stacks if needed
        consumeItems(player, trade.getInputItem(), trade.inputCount);

        // Give output
        ItemStack output = new ItemStack(trade.getOutputItem(), trade.outputCount);
        if (!player.getInventory().insertStack(output)) {
            player.dropItem(output, false);
        }

        // Give XP
        player.addExperience(trade.xpReward);
        player.playSound(SoundEvents.ENTITY_VILLAGER_YES, 1.0f, 1.0f);
    }

    private static int countItems(ServerPlayerEntity player, net.minecraft.item.Item item) {
        int count = 0;
        var inventory = player.getInventory();
        for (int i = 0; i < inventory.size(); i++) {
            ItemStack stack = inventory.getStack(i);
            if (stack.getItem() == item && !stack.isEmpty()) {
                count += stack.getCount();
            }
        }
        return count;
    }

    private static void consumeItems(ServerPlayerEntity player, net.minecraft.item.Item item, int amount) {
        var inventory = player.getInventory();
        int remaining = amount;
        for (int i = 0; i < inventory.size() && remaining > 0; i++) {
            ItemStack stack = inventory.getStack(i);
            if (stack.getItem() == item && !stack.isEmpty()) {
                int take = Math.min(remaining, stack.getCount());
                stack.decrement(take);
                remaining -= take;
            }
        }
    }

    private static void sendFailure(ServerPlayerEntity player, String message) {
        player.playSound(SoundEvents.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
        player.sendMessage(Text.literal("§c" + message), true);
    }
}
