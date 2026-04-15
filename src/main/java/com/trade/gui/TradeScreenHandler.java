package com.trade.gui;

import com.trade.TradeConfig;
import com.trade.network.TradePackets;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;

public class TradeScreenHandler extends ScreenHandler {
    private final SimpleInventory inputInventory;
    private final PlayerEntity player;

    public TradeScreenHandler(int syncId, PlayerInventory playerInventory) {
        super(TradePackets.TRADE_SCREEN_HANDLER, syncId);
        this.inputInventory = new SimpleInventory(1);
        this.player = playerInventory.player;

        // Input slot
        this.addSlot(new Slot(inputInventory, 0, 27, 18));

        // Output slot (virtual)
        this.addSlot(new TradeOutputSlot(this, 135, 18));

        // Player inventory
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 50 + row * 18));
            }
        }

        // Player hotbar
        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(playerInventory, col, 8 + col * 18, 108));
        }
    }

    private TradeConfig.TradeEntry findTrade() {
        ItemStack input = inputInventory.getStack(0);
        if (input.isEmpty()) return null;

        for (TradeConfig.TradeEntry trade : TradeConfig.getTrades()) {
            if (trade.getInputItem() == null || trade.getOutputItem() == null) {
                continue; // Skip invalid entries
            }
            if (input.getItem() == trade.getInputItem() && input.getCount() >= trade.inputCount) {
                return trade;
            }
        }
        return null;
    }

    public ItemStack getOutputPreview() {
        TradeConfig.TradeEntry trade = findTrade();
        if (trade == null || trade.getOutputItem() == null) {
            return ItemStack.EMPTY;
        }
        return new ItemStack(trade.getOutputItem(), trade.outputCount);
    }

    public boolean hasValidTrade() {
        return findTrade() != null;
    }

    @Override
    public ItemStack quickMove(PlayerEntity player, int slotIndex) {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = this.slots.get(slotIndex);

        if (slot.hasStack()) {
            ItemStack slotStack = slot.getStack();
            result = slotStack.copy();

            if (slotIndex == 0) {
                // Input slot -> player inventory
                if (!this.insertItem(slotStack, 2, 38, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (slotIndex == 1) {
                // Output slot - do nothing here, trade happens via onTakeItem
                return ItemStack.EMPTY;
            } else {
                // Player inventory -> input slot
                if (!this.insertItem(slotStack, 0, 1, false)) {
                    return ItemStack.EMPTY;
                }
            }

            if (slotStack.isEmpty()) {
                slot.setStack(ItemStack.EMPTY);
            } else {
                slot.markDirty();
            }
        }

        return result;
    }

    @Override
    public boolean canUse(PlayerEntity player) {
        return true;
    }

    @Override
    public void onClosed(PlayerEntity player) {
        super.onClosed(player);
        dropInventory(player, inputInventory);
    }

    /**
     * Virtual output slot - executes trade on click (server-side only)
     */
    private static class TradeOutputSlot extends Slot {
        private final TradeScreenHandler handler;

        public TradeOutputSlot(TradeScreenHandler handler, int x, int y) {
            super(new SimpleInventory(1), 0, x, y);
            this.handler = handler;
        }

        @Override
        public boolean canInsert(ItemStack stack) {
            return false;
        }

        @Override
        public boolean canTakeItems(PlayerEntity player) {
            return handler.hasValidTrade();
        }

        @Override
        public ItemStack getStack() {
            return handler.getOutputPreview();
        }

        @Override
        public void setStack(ItemStack stack) {
            // Virtual slot - ignore
        }

        @Override
        public ItemStack takeStack(int amount) {
            return handler.getOutputPreview().copy();
        }

        @Override
        public void onTakeItem(PlayerEntity player, ItemStack stack) {
            // This runs on SERVER side only
            TradeConfig.TradeEntry trade = handler.findTrade();
            if (trade == null) {
                sendFailureMessage(player, "Invalid trade");
                return;
            }

            ItemStack input = handler.inputInventory.getStack(0);
            if (trade.getInputItem() == null || trade.getOutputItem() == null) {
                sendFailureMessage(player, "Invalid trade configuration");
                return;
            }

            if (input.getItem() != trade.getInputItem() || input.getCount() < trade.inputCount) {
                sendFailureMessage(player, "Insufficient items");
                return;
            }

            // Consume input
            input.decrement(trade.inputCount);
            if (input.isEmpty()) {
                handler.inputInventory.setStack(0, ItemStack.EMPTY);
            }

            // Give output to player
            ItemStack output = new ItemStack(trade.getOutputItem(), trade.outputCount);
            if (!player.getInventory().insertStack(output)) {
                player.dropItem(output, false);
            }

            // Give XP
            player.addExperience(trade.xpReward);
            player.playSound(SoundEvents.ENTITY_VILLAGER_YES, 1.0f, 1.0f);
        }

        private void sendFailureMessage(PlayerEntity player, String message) {
            if (player instanceof ServerPlayerEntity) {
                player.playSound(SoundEvents.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                player.sendMessage(Text.literal("§c" + message), true);
            }
        }

        @Override
        public void markDirty() {
            // Virtual slot - ignore
        }
    }
}
