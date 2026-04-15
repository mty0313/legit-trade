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

        // Find input items in player inventory
        ItemStack inputStack = findItemStack(player, trade.getInputItem());
        if (inputStack == null || inputStack.getCount() < trade.inputCount) {
            sendFailure(player, "Insufficient items");
            return;
        }

        // Consume input
        inputStack.decrement(trade.inputCount);

        // Give output
        ItemStack output = new ItemStack(trade.getOutputItem(), trade.outputCount);
        if (!player.getInventory().insertStack(output)) {
            player.dropItem(output, false);
        }

        // Give XP
        player.addExperience(trade.xpReward);
        player.playSound(SoundEvents.ENTITY_VILLAGER_YES, 1.0f, 1.0f);
    }

    private static ItemStack findItemStack(ServerPlayerEntity player, net.minecraft.item.Item item) {
        // Search main inventory first, then hotbar
        var inventory = player.getInventory();
        for (int i = 0; i < inventory.size(); i++) {
            ItemStack stack = inventory.getStack(i);
            if (stack.getItem() == item && !stack.isEmpty()) {
                return stack;
            }
        }
        return null;
    }

    private static void sendFailure(ServerPlayerEntity player, String message) {
        player.playSound(SoundEvents.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
        player.sendMessage(Text.literal("§c" + message), true);
    }
}
