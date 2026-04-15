package com.trade.gui;

import com.trade.TradeConfig;
import com.trade.network.ExecuteTradePacket;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.item.ItemStack;

import java.util.OptionalInt;

@Environment(EnvType.CLIENT)
public class TradeListWidget {
    private static final int ENTRY_HEIGHT = 20;
    private static final int ENTRY_WIDTH = 140;

    private int x;
    private int y;
    private int visibleEntries;
    private int scrollOffset = 0;
    private TradeScreenHandler handler;

    public TradeListWidget(int x, int y, int visibleEntries) {
        this.x = x;
        this.y = y;
        this.visibleEntries = visibleEntries;
    }

    public void setHandler(TradeScreenHandler handler) {
        this.handler = handler;
    }

    public int getTotalTrades() {
        return TradeConfig.getTrades().size();
    }

    public void render(DrawContext context, TextRenderer textRenderer, int mouseX, int mouseY) {
        var trades = TradeConfig.getTrades();
        int totalTrades = trades.size();

        // Background
        context.fill(x, y, x + ENTRY_WIDTH, y + visibleEntries * ENTRY_HEIGHT, 0xFF373737);

        for (int i = 0; i < visibleEntries; i++) {
            int tradeIndex = scrollOffset + i;
            if (tradeIndex >= totalTrades) break;

            TradeConfig.TradeEntry trade = trades.get(tradeIndex);
            int entryY = y + i * ENTRY_HEIGHT;

            // Skip rendering and interaction for invalid trades
            var inputItemObj = trade.getInputItem();
            var outputItemObj = trade.getOutputItem();
            if (inputItemObj == null || outputItemObj == null) {
                continue;
            }

            int availableCount = handler != null ? handler.getItemCountInInventory(inputItemObj) : 0;
            boolean canExecute = availableCount >= trade.inputCount;
            boolean isHovered = isMouseOverEntry(mouseX, mouseY, i);

            // Entry background
            int bgColor = isHovered ? 0xFF4A4A4A : 0xFF2A2A2A;
            context.fill(x, entryY, x + ENTRY_WIDTH, entryY + ENTRY_HEIGHT - 1, bgColor);

            // Input item icon
            ItemStack inputItem = new ItemStack(inputItemObj, trade.inputCount);
            context.drawItem(inputItem, x + 4, entryY + 2);
            context.drawItemInSlot(textRenderer, inputItem, x + 4, entryY + 2);

            // Arrow
            context.drawText(textRenderer, "→", x + 26, entryY + 6, 0xFFFFFF, false);

            // Output item icon
            ItemStack outputItem = new ItemStack(outputItemObj, trade.outputCount);
            context.drawItem(outputItem, x + 40, entryY + 2);
            context.drawItemInSlot(textRenderer, outputItem, x + 40, entryY + 2);

            // XP reward
            String xpText = "+" + trade.xpReward + "XP";
            int xpColor = canExecute ? 0x55FF55 : 0x888888;
            context.drawText(textRenderer, xpText, x + 60, entryY + 6, xpColor, false);

            // Can execute indicator
            if (canExecute) {
                context.drawText(textRenderer, "✓", x + ENTRY_WIDTH - 12, entryY + 6, 0x55FF55, false);
            } else if (availableCount > 0) {
                // Show available count
                String countText = availableCount + "/" + trade.inputCount;
                context.drawText(textRenderer, countText, x + ENTRY_WIDTH - 30, entryY + 6, 0xFFAA00, false);
            }
        }

        // Scroll indicators
        if (scrollOffset > 0) {
            context.drawText(textRenderer, "▲", x + ENTRY_WIDTH / 2 - 4, y - 10, 0xFFFFFF, false);
        }
        if (scrollOffset + visibleEntries < totalTrades) {
            context.drawText(textRenderer, "▼", x + ENTRY_WIDTH / 2 - 4, y + visibleEntries * ENTRY_HEIGHT + 2, 0xFFFFFF, false);
        }
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0) return false;

        var trades = TradeConfig.getTrades();

        for (int i = 0; i < visibleEntries; i++) {
            if (isMouseOverEntry((int) mouseX, (int) mouseY, i)) {
                int tradeIndex = scrollOffset + i;
                if (tradeIndex >= trades.size()) continue;

                TradeConfig.TradeEntry trade = trades.get(tradeIndex);
                // Skip invalid trades
                if (trade.getInputItem() == null || trade.getOutputItem() == null) {
                    continue;
                }
                int availableCount = handler != null ? handler.getItemCountInInventory(trade.getInputItem()) : 0;
                if (availableCount >= trade.inputCount) {
                    ExecuteTradePacket.sendToServer(tradeIndex);
                    return true;
                }
            }
        }
        return false;
    }

    public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
        if (!isMouseOver((int) mouseX, (int) mouseY)) return false;

        int maxScroll = Math.max(0, TradeConfig.getTrades().size() - visibleEntries);
        scrollOffset = Math.max(0, Math.min(maxScroll, scrollOffset - (int) amount));
        return true;
    }

    private boolean isMouseOver(int mouseX, int mouseY) {
        return mouseX >= x && mouseX < x + ENTRY_WIDTH &&
               mouseY >= y && mouseY < y + visibleEntries * ENTRY_HEIGHT;
    }

    private boolean isMouseOverEntry(int mouseX, int mouseY, int entryIndex) {
        int entryY = y + entryIndex * ENTRY_HEIGHT;
        return mouseX >= x && mouseX < x + ENTRY_WIDTH &&
               mouseY >= entryY && mouseY < entryY + ENTRY_HEIGHT;
    }

    public OptionalInt getHoveredTradeIndex(int mouseX, int mouseY) {
        var trades = TradeConfig.getTrades();
        for (int i = 0; i < visibleEntries; i++) {
            if (isMouseOverEntry(mouseX, mouseY, i)) {
                int tradeIndex = scrollOffset + i;
                if (tradeIndex < trades.size()) {
                    return OptionalInt.of(tradeIndex);
                }
            }
        }
        return OptionalInt.empty();
    }

    public boolean hasTrades() {
        return !TradeConfig.getTrades().isEmpty();
    }
}
