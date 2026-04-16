package com.trade.gui;

import com.trade.TradeBlocks;
import com.trade.TradeConfig;
import com.trade.network.TradePackets;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.ScreenHandlerContext;
import net.minecraft.screen.slot.Slot;
import net.minecraft.sound.SoundEvents;

import java.util.List;

public class TradeScreenHandler extends ScreenHandler {
    private static final int INPUT_SLOT = 0;
    private static final int OUTPUT_SLOT = 1;
    private static final int PLAYER_INV_START = 2;
    private static final int PLAYER_INV_END = 38;

    private static final int RIGHT_PANEL_X = 96;
    private static final int TOP_PANEL_HEIGHT = 48;

    public static final int SELECT_TRADE_BASE_BUTTON_ID = 1000;

    private final SimpleInventory inputInventory;
    private final ScreenHandlerContext context;
    private int selectedTradeIndex;
    private int lastXpReward;

    public TradeScreenHandler(int syncId, PlayerInventory playerInventory) {
        this(syncId, playerInventory, ScreenHandlerContext.EMPTY);
    }

    public TradeScreenHandler(int syncId, PlayerInventory playerInventory, ScreenHandlerContext context) {
        super(TradePackets.TRADE_SCREEN_HANDLER, syncId);
        this.inputInventory = new SimpleInventory(1);
        this.context = context;

        this.addSlot(new Slot(inputInventory, INPUT_SLOT, RIGHT_PANEL_X + 8, 16));
        this.addSlot(new TradeOutputSlot(this, RIGHT_PANEL_X + 92, 16));

        int playerInvY = TOP_PANEL_HEIGHT + 28;
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(playerInventory, col + row * 9 + 9, RIGHT_PANEL_X + 8 + col * 18, playerInvY + row * 18));
            }
        }

        int hotbarY = playerInvY + 58;
        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(playerInventory, col, RIGHT_PANEL_X + 8 + col * 18, hotbarY));
        }
    }

    private List<TradeConfig.TradeEntry> getTrades() {
        return TradeConfig.getTrades();
    }

    public int getTradeCount() {
        return getTrades().size();
    }

    public int getSelectedTradeIndex() {
        int count = getTradeCount();
        if (count <= 0) {
            selectedTradeIndex = 0;
            return 0;
        }
        if (selectedTradeIndex < 0 || selectedTradeIndex >= count) {
            selectedTradeIndex = 0;
        }
        return selectedTradeIndex;
    }

    public TradeConfig.TradeEntry getSelectedTrade() {
        List<TradeConfig.TradeEntry> trades = getTrades();
        if (trades.isEmpty()) {
            selectedTradeIndex = 0;
            return null;
        }
        return trades.get(getSelectedTradeIndex());
    }

    public TradeConfig.TradeEntry getTradeAt(int index) {
        List<TradeConfig.TradeEntry> trades = getTrades();
        if (index < 0 || index >= trades.size()) {
            return null;
        }
        return trades.get(index);
    }

    public boolean canAffordTradeAt(int index) {
        TradeConfig.TradeEntry trade = getTradeAt(index);
        if (trade == null || trade.inputCount <= 0) {
            return false;
        }

        Item inputItem = trade.getInputItem();
        if (inputItem == null) {
            return false;
        }

        int total = 0;

        ItemStack input = inputInventory.getStack(INPUT_SLOT);
        if (!input.isEmpty() && input.getItem() == inputItem) {
            total += input.getCount();
        }

        for (int i = PLAYER_INV_START; i < PLAYER_INV_END; i++) {
            ItemStack stack = this.slots.get(i).getStack();
            if (!stack.isEmpty() && stack.getItem() == inputItem) {
                total += stack.getCount();
                if (total >= trade.inputCount) {
                    return true;
                }
            }
        }

        return total >= trade.inputCount;
    }

    private boolean canExecuteTrade(TradeConfig.TradeEntry trade) {
        if (trade == null || trade.inputCount <= 0 || trade.outputCount <= 0 || trade.xpReward < 0) {
            return false;
        }

        Item inputItem = trade.getInputItem();
        Item outputItem = trade.getOutputItem();
        if (inputItem == null || outputItem == null) {
            return false;
        }

        ItemStack input = inputInventory.getStack(INPUT_SLOT);
        if (input.isEmpty() || input.getItem() != inputItem) {
            return false;
        }

        if (trade.inputCount > input.getMaxCount()) {
            return false;
        }

        return input.getCount() >= trade.inputCount;
    }

    public ItemStack getOutputPreview() {
        TradeConfig.TradeEntry trade = getSelectedTrade();
        if (!canExecuteTrade(trade)) {
            return ItemStack.EMPTY;
        }

        Item outputItem = trade.getOutputItem();
        if (outputItem == null) {
            return ItemStack.EMPTY;
        }

        int safeOutputCount = Math.min(trade.outputCount, outputItem.getMaxCount());
        if (safeOutputCount <= 0) {
            return ItemStack.EMPTY;
        }

        return new ItemStack(outputItem, safeOutputCount);
    }

    public boolean hasValidTrade() {
        return !getOutputPreview().isEmpty();
    }

    private void returnInputToPlayer(PlayerEntity player, ItemStack stack) {
        if (stack.isEmpty()) {
            return;
        }

        ItemStack remaining = stack.copy();
        if (!player.getInventory().insertStack(remaining) && !remaining.isEmpty()) {
            player.dropItem(remaining, false);
        }
    }

    private void autoFillSelectedTrade(PlayerEntity player) {
        TradeConfig.TradeEntry trade = getSelectedTrade();
        if (trade == null || trade.inputCount <= 0) {
            return;
        }

        Item inputItem = trade.getInputItem();
        if (inputItem == null) {
            return;
        }

        int targetInputCount = Math.min(trade.inputCount, inputItem.getMaxCount());
        ItemStack inputStack = inputInventory.getStack(INPUT_SLOT);

        if (!inputStack.isEmpty() && inputStack.getItem() != inputItem) {
            ItemStack toReturn = inputStack.copy();
            inputInventory.setStack(INPUT_SLOT, ItemStack.EMPTY);
            returnInputToPlayer(player, toReturn);
            inputStack = ItemStack.EMPTY;
        }

        int currentCount = (!inputStack.isEmpty() && inputStack.getItem() == inputItem) ? inputStack.getCount() : 0;
        int needed = targetInputCount - currentCount;
        if (needed <= 0) {
            inputInventory.markDirty();
            sendContentUpdates();
            return;
        }

        for (int i = PLAYER_INV_START; i < PLAYER_INV_END && needed > 0; i++) {
            Slot sourceSlot = this.slots.get(i);
            ItemStack sourceStack = sourceSlot.getStack();
            if (sourceStack.isEmpty() || sourceStack.getItem() != inputItem) {
                continue;
            }

            int move = Math.min(needed, sourceStack.getCount());
            if (move <= 0) {
                continue;
            }

            if (inputStack.isEmpty()) {
                inputStack = new ItemStack(inputItem, move);
                inputInventory.setStack(INPUT_SLOT, inputStack);
            } else {
                inputStack.increment(move);
            }

            sourceStack.decrement(move);
            if (sourceStack.isEmpty()) {
                sourceSlot.setStack(ItemStack.EMPTY);
            } else {
                sourceSlot.markDirty();
            }

            needed -= move;
        }

        inputInventory.markDirty();
        sendContentUpdates();
    }

    private ItemStack executeTrade() {
        lastXpReward = 0;

        TradeConfig.TradeEntry trade = getSelectedTrade();
        if (!canExecuteTrade(trade)) {
            return ItemStack.EMPTY;
        }

        Item outputItem = trade.getOutputItem();
        if (outputItem == null) {
            return ItemStack.EMPTY;
        }

        int safeOutputCount = Math.min(trade.outputCount, outputItem.getMaxCount());
        if (safeOutputCount <= 0) {
            return ItemStack.EMPTY;
        }

        ItemStack input = inputInventory.getStack(INPUT_SLOT);
        input.decrement(trade.inputCount);
        if (input.isEmpty()) {
            inputInventory.setStack(INPUT_SLOT, ItemStack.EMPTY);
        }
        inputInventory.markDirty();

        lastXpReward = trade.xpReward;
        sendContentUpdates();
        return new ItemStack(outputItem, safeOutputCount);
    }

    @Override
    public boolean onButtonClick(PlayerEntity player, int id) {
        if (id >= SELECT_TRADE_BASE_BUTTON_ID) {
            int index = id - SELECT_TRADE_BASE_BUTTON_ID;
            int tradeCount = getTradeCount();
            if (index < 0 || index >= tradeCount) {
                return false;
            }

            selectedTradeIndex = index;
            if (!player.getWorld().isClient) {
                autoFillSelectedTrade(player);
            }
            sendContentUpdates();
            return true;
        }
        return false;
    }

    @Override
    public ItemStack quickMove(PlayerEntity player, int slotIndex) {
        if (slotIndex < 0 || slotIndex >= this.slots.size()) {
            return ItemStack.EMPTY;
        }

        ItemStack result = ItemStack.EMPTY;
        Slot slot = this.slots.get(slotIndex);

        if (slot.hasStack()) {
            ItemStack slotStack = slot.getStack();
            result = slotStack.copy();

            if (slotIndex == INPUT_SLOT) {
                if (!this.insertItem(slotStack, PLAYER_INV_START, PLAYER_INV_END, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (slotIndex == OUTPUT_SLOT) {
                return ItemStack.EMPTY;
            } else {
                if (!this.insertItem(slotStack, INPUT_SLOT, INPUT_SLOT + 1, false)) {
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
        return ScreenHandler.canUse(this.context, player, TradeBlocks.TRADE_BLOCK);
    }

    @Override
    public void onClosed(PlayerEntity player) {
        super.onClosed(player);
        dropInventory(player, inputInventory);
    }

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
        }

        @Override
        public ItemStack takeStack(int amount) {
            return handler.executeTrade();
        }

        @Override
        public void onTakeItem(PlayerEntity player, ItemStack stack) {
            if (player.getWorld().isClient) {
                return;
            }

            if (stack.isEmpty()) {
                player.playSound(SoundEvents.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                return;
            }

            if (handler.lastXpReward > 0) {
                player.addExperience(handler.lastXpReward);
            }
            handler.lastXpReward = 0;
            player.playSound(SoundEvents.ENTITY_VILLAGER_YES, 1.0f, 1.0f);
        }

        @Override
        public void markDirty() {
        }
    }
}
