package com.trade.gui;

import com.trade.network.TradePackets;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;

public class TradeScreenHandler extends ScreenHandler {
    private final PlayerEntity player;

    public TradeScreenHandler(int syncId, PlayerInventory playerInventory) {
        super(TradePackets.TRADE_SCREEN_HANDLER, syncId);
        this.player = playerInventory.player;

        // Player inventory (3 rows)
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
            }
        }

        // Player hotbar
        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(playerInventory, col, 8 + col * 18, 142));
        }
    }

    public PlayerEntity getPlayer() {
        return player;
    }

    public int getItemCountInInventory(net.minecraft.item.Item item) {
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

    @Override
    public ItemStack quickMove(PlayerEntity player, int slotIndex) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean canUse(PlayerEntity player) {
        return true;
    }
}
