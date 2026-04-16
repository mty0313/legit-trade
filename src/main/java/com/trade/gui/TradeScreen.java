package com.trade.gui;

import com.trade.TradeConfig;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;

import java.util.List;

@Environment(EnvType.CLIENT)
public class TradeScreen extends HandledScreen<TradeScreenHandler> {
    private static final int BG_WIDTH = 276;
    private static final int BG_HEIGHT = 186;

    private static final int LEFT_PANEL_X = 6;
    private static final int LEFT_PANEL_Y = 6;
    private static final int LEFT_PANEL_WIDTH = 88;
    private static final int LEFT_PANEL_HEIGHT = BG_HEIGHT - 12;

    private static final int RIGHT_PANEL_X = 96;
    private static final int RIGHT_TOP_Y = 6;
    private static final int RIGHT_TOP_HEIGHT = 48;
    private static final int RIGHT_BOTTOM_Y = RIGHT_TOP_Y + RIGHT_TOP_HEIGHT + 4;
    private static final int RIGHT_BOTTOM_HEIGHT = BG_HEIGHT - RIGHT_BOTTOM_Y - 6;

    private static final int LIST_SCROLL_UP_Y = LEFT_PANEL_Y + 2;
    private static final int LIST_SCROLL_DOWN_Y = LEFT_PANEL_Y + LEFT_PANEL_HEIGHT - 12;
    private static final int LIST_CONTENT_Y = LEFT_PANEL_Y + 16;
    private static final int LIST_ROW_HEIGHT = 14;
    private static final int LIST_MAX_ROWS = 10;
    private static final int LIST_ARROW_X = LEFT_PANEL_X + LEFT_PANEL_WIDTH - 8;

    private int listScrollOffset;

    public TradeScreen(TradeScreenHandler handler, PlayerInventory inventory, Text title) {
        super(handler, inventory, title);
        this.backgroundWidth = BG_WIDTH;
        this.backgroundHeight = BG_HEIGHT;
        this.playerInventoryTitleY = 0;
    }

    private void clampScrollOffset() {
        int maxOffset = Math.max(0, handler.getTradeCount() - LIST_MAX_ROWS);
        if (listScrollOffset < 0) {
            listScrollOffset = 0;
        } else if (listScrollOffset > maxOffset) {
            listScrollOffset = maxOffset;
        }
    }

    @Override
    protected void drawBackground(DrawContext context, float delta, int mouseX, int mouseY) {
        int x = this.x;
        int y = this.y;

        context.fill(x, y, x + BG_WIDTH, y + BG_HEIGHT, 0xFF2B2B2B);

        context.fill(x + LEFT_PANEL_X, y + LEFT_PANEL_Y, x + LEFT_PANEL_X + LEFT_PANEL_WIDTH, y + LEFT_PANEL_Y + LEFT_PANEL_HEIGHT, 0xFF1F1F1F);
        context.fill(x + RIGHT_PANEL_X, y + RIGHT_TOP_Y, x + BG_WIDTH - 6, y + RIGHT_TOP_Y + RIGHT_TOP_HEIGHT, 0xFF1F1F1F);
        context.fill(x + RIGHT_PANEL_X, y + RIGHT_BOTTOM_Y, x + BG_WIDTH - 6, y + RIGHT_BOTTOM_Y + RIGHT_BOTTOM_HEIGHT, 0xFF1F1F1F);

        context.fill(x + LEFT_PANEL_X + LEFT_PANEL_WIDTH, y + 6, x + LEFT_PANEL_X + LEFT_PANEL_WIDTH + 1, y + BG_HEIGHT - 6, 0xFF4D4D4D);

        context.fill(x + RIGHT_PANEL_X + 8, y + 16, x + RIGHT_PANEL_X + 8 + 18, y + 16 + 18, 0xFF4D4D4D);
        context.fill(x + RIGHT_PANEL_X + 10, y + 18, x + RIGHT_PANEL_X + 10 + 14, y + 18 + 14, 0xFF222222);

        context.fill(x + RIGHT_PANEL_X + 92, y + 16, x + RIGHT_PANEL_X + 92 + 18, y + 16 + 18, 0xFF4D4D4D);
        context.fill(x + RIGHT_PANEL_X + 94, y + 18, x + RIGHT_PANEL_X + 94 + 14, y + 18 + 14, 0xFF222222);

        context.drawText(this.textRenderer, "->", x + RIGHT_PANEL_X + 56, y + 21, 0xFFBBBBBB, false);

        for (int i = 0; i < this.handler.slots.size(); i++) {
            int sx = this.handler.slots.get(i).x;
            int sy = this.handler.slots.get(i).y;
            context.fill(x + sx, y + sy, x + sx + 18, y + sy + 18, 0xAA5A5A5A);
            context.fill(x + sx + 1, y + sy + 1, x + sx + 17, y + sy + 17, 0xAA2A2A2A);
        }
    }

    @Override
    protected void drawForeground(DrawContext context, int mouseX, int mouseY) {
        clampScrollOffset();

        int localMouseX = mouseX - this.x;
        int localMouseY = mouseY - this.y;

        context.drawText(this.textRenderer, this.title, 8, 8, 0xFFFFFF, false);
        context.drawText(this.textRenderer, Text.translatable("screen.legittrade.trade_list"), LEFT_PANEL_X + 4, LEFT_PANEL_Y + 2, 0xD0D0D0, false);

        drawScrollControls(context, localMouseX, localMouseY);
        drawTradeList(context, localMouseX, localMouseY);

        TradeConfig.TradeEntry trade = handler.getSelectedTrade();
        if (trade != null) {
            String req = Text.translatable("screen.legittrade.required", trade.inputCount).getString();
            context.drawText(this.textRenderer, req, RIGHT_PANEL_X + 8, RIGHT_TOP_Y + 6, 0xD0D0D0, false);

            String xp = Text.translatable("screen.legittrade.xp_reward", trade.xpReward).getString();
            context.drawText(this.textRenderer, xp, RIGHT_PANEL_X + 8, RIGHT_TOP_Y + 36, 0x55FF55, false);
        } else {
            context.drawText(this.textRenderer, Text.translatable("screen.legittrade.no_trade_selected"), RIGHT_PANEL_X + 8, RIGHT_TOP_Y + 20, 0xAAAAAA, false);
        }

        context.drawText(this.textRenderer, this.playerInventoryTitle, RIGHT_PANEL_X + 8, RIGHT_BOTTOM_Y + 6, 0xFFFFFF, false);
    }

    private void drawScrollControls(DrawContext context, int localMouseX, int localMouseY) {
        boolean canUp = listScrollOffset > 0;
        boolean canDown = (listScrollOffset + LIST_MAX_ROWS) < handler.getTradeCount();

        int upX = LEFT_PANEL_X + LEFT_PANEL_WIDTH - 12;
        int downX = LEFT_PANEL_X + LEFT_PANEL_WIDTH - 12;

        int upBg = isInScrollUpLocal(localMouseX, localMouseY) ? 0xFF4E4E4E : 0xFF353535;
        int downBg = isInScrollDownLocal(localMouseX, localMouseY) ? 0xFF4E4E4E : 0xFF353535;

        context.fill(upX, LIST_SCROLL_UP_Y, upX + 10, LIST_SCROLL_UP_Y + 10, upBg);
        context.fill(downX, LIST_SCROLL_DOWN_Y, downX + 10, LIST_SCROLL_DOWN_Y + 10, downBg);

        int upColor = canUp ? 0xFFFFFF : 0x666666;
        int downColor = canDown ? 0xFFFFFF : 0x666666;

        context.drawText(this.textRenderer, "^", LIST_ARROW_X, LIST_SCROLL_UP_Y + 1, upColor, false);
        context.drawText(this.textRenderer, "v", LIST_ARROW_X, LIST_SCROLL_DOWN_Y + 1, downColor, false);
    }

    private void drawTradeList(DrawContext context, int localMouseX, int localMouseY) {
        List<TradeConfig.TradeEntry> trades = TradeConfig.getTrades();
        if (trades.isEmpty()) {
            context.drawText(this.textRenderer, Text.translatable("screen.legittrade.no_trades"), LEFT_PANEL_X + 4, LIST_CONTENT_Y, 0x999999, false);
            return;
        }

        int hovered = getHoveredTradeIndex(localMouseX, localMouseY);
        int selected = handler.getSelectedTradeIndex();
        int end = Math.min(trades.size(), listScrollOffset + LIST_MAX_ROWS);
        for (int i = listScrollOffset; i < end; i++) {
            int row = i - listScrollOffset;
            int rowY = LIST_CONTENT_Y + row * LIST_ROW_HEIGHT;

            int color = 0xFF2A2A2A;
            if (i == selected) {
                color = 0xFF335577;
            } else if (i == hovered) {
                color = 0xFF3A3A3A;
            }
            context.fill(LEFT_PANEL_X + 2, rowY - 1, LEFT_PANEL_X + LEFT_PANEL_WIDTH - 12, rowY + LIST_ROW_HEIGHT - 2, color);

            TradeConfig.TradeEntry trade = trades.get(i);
            boolean affordable = handler.canAffordTradeAt(i);
            ItemStack inputIcon = trade.getInputItem() != null ? new ItemStack(trade.getInputItem()) : ItemStack.EMPTY;
            ItemStack outputIcon = trade.getOutputItem() != null ? new ItemStack(trade.getOutputItem()) : ItemStack.EMPTY;

            if (!inputIcon.isEmpty()) {
                context.drawItem(inputIcon, LEFT_PANEL_X + 4, rowY - 1);
            }
            context.drawText(this.textRenderer, "->", LEFT_PANEL_X + 23, rowY + 2, affordable ? 0xBBBBBB : 0xCC6666, false);
            if (!outputIcon.isEmpty()) {
                context.drawItem(outputIcon, LEFT_PANEL_X + 34, rowY - 1);
            }

            String line = trade.inputCount + "->" + trade.outputCount;
            String clipped = this.textRenderer.trimToWidth(line, LEFT_PANEL_WIDTH - 72);
            int lineColor = affordable ? 0xFFFFFF : 0xFF8888;
            context.drawText(this.textRenderer, clipped, LEFT_PANEL_X + 54, rowY + 2, lineColor, false);

            String xpBadge = "+" + trade.xpReward;
            int xpColor = trade.xpReward > 0 ? 0x66FF66 : 0x999999;
            int xpWidth = this.textRenderer.getWidth(xpBadge);
            context.drawText(this.textRenderer, xpBadge, LEFT_PANEL_X + LEFT_PANEL_WIDTH - 14 - xpWidth, rowY + 2, xpColor, false);

            if (!affordable) {
                context.fill(LEFT_PANEL_X + 2, rowY + LIST_ROW_HEIGHT - 3, LEFT_PANEL_X + LEFT_PANEL_WIDTH - 12, rowY + LIST_ROW_HEIGHT - 2, 0x99AA4444);
            }
        }
    }

    private String formatTradeLine(TradeConfig.TradeEntry trade) {
        String in = trade.input;
        String out = trade.output;

        if (trade.getInputItem() != null) {
            in = trade.getInputItem().getName().getString();
        }
        if (trade.getOutputItem() != null) {
            out = trade.getOutputItem().getName().getString();
        }
        return trade.inputCount + " " + in + " -> " + trade.outputCount + " " + out;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.renderBackground(context);
        super.render(context, mouseX, mouseY, delta);
        this.drawMouseoverTooltip(context, mouseX, mouseY);
    }

    @Override
    protected void drawMouseoverTooltip(DrawContext context, int x, int y) {
        super.drawMouseoverTooltip(context, x, y);

        int localMouseX = x - this.x;
        int localMouseY = y - this.y;

        int hoveredTradeIndex = getHoveredTradeIndex(localMouseX, localMouseY);
        if (hoveredTradeIndex >= 0 && hoveredTradeIndex < handler.getTradeCount()) {
            TradeConfig.TradeEntry trade = TradeConfig.getTrades().get(hoveredTradeIndex);
            context.drawTooltip(this.textRenderer, Text.literal(formatTradeLine(trade)), x, y);
            return;
        }

        TradeConfig.TradeEntry selectedTrade = handler.getSelectedTrade();
        if (this.focusedSlot != null && this.focusedSlot == this.handler.getSlot(0) && selectedTrade != null) {
            if (!this.focusedSlot.getStack().isEmpty() && !handler.getOutputPreview().isEmpty()) {
                Text tooltip = Text.translatable(
                    "tooltip.legittrade.trade_preview",
                    selectedTrade.outputCount,
                    handler.getOutputPreview().getName(),
                    selectedTrade.xpReward
                );
                context.drawTooltip(this.textRenderer, tooltip, x, y);
            }
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
        if (isInLeftPanel(mouseX, mouseY)) {
            if (amount > 0) {
                listScrollOffset--;
            } else if (amount < 0) {
                listScrollOffset++;
            }
            clampScrollOffset();
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, amount);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && this.client != null && this.client.interactionManager != null && this.client.player != null) {
            if (isInLeftPanel(mouseX, mouseY)) {
                if (isInScrollUp(mouseX, mouseY)) {
                    listScrollOffset--;
                    clampScrollOffset();
                    return true;
                }
                if (isInScrollDown(mouseX, mouseY)) {
                    listScrollOffset++;
                    clampScrollOffset();
                    return true;
                }

                int index = getClickedTradeIndex(mouseY);
                if (index >= 0 && index < handler.getTradeCount()) {
                    int id = TradeScreenHandler.SELECT_TRADE_BASE_BUTTON_ID + index;
                    this.client.interactionManager.clickButton(this.handler.syncId, id);
                    this.handler.onButtonClick(this.client.player, id);
                    return true;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private int getClickedTradeIndex(double mouseY) {
        int localY = (int) mouseY - this.y;
        if (localY < LIST_CONTENT_Y || localY >= LIST_CONTENT_Y + LIST_MAX_ROWS * LIST_ROW_HEIGHT) {
            return -1;
        }
        int row = (localY - LIST_CONTENT_Y) / LIST_ROW_HEIGHT;
        return listScrollOffset + row;
    }

    private int getHoveredTradeIndex(int localMouseX, int localMouseY) {
        int left = LEFT_PANEL_X + 2;
        int right = LEFT_PANEL_X + LEFT_PANEL_WIDTH - 12;
        if (localMouseX < left || localMouseX >= right) {
            return -1;
        }
        if (localMouseY < LIST_CONTENT_Y || localMouseY >= LIST_CONTENT_Y + LIST_MAX_ROWS * LIST_ROW_HEIGHT) {
            return -1;
        }

        int row = (localMouseY - LIST_CONTENT_Y) / LIST_ROW_HEIGHT;
        int index = listScrollOffset + row;
        return index < handler.getTradeCount() ? index : -1;
    }

    private boolean isInLeftPanel(double mouseX, double mouseY) {
        int lx = this.x + LEFT_PANEL_X;
        int ly = this.y + LEFT_PANEL_Y;
        return mouseX >= lx && mouseX < lx + LEFT_PANEL_WIDTH && mouseY >= ly && mouseY < ly + LEFT_PANEL_HEIGHT;
    }

    private boolean isInScrollUp(double mouseX, double mouseY) {
        return isInScrollUpLocal((int) mouseX - this.x, (int) mouseY - this.y);
    }

    private boolean isInScrollDown(double mouseX, double mouseY) {
        return isInScrollDownLocal((int) mouseX - this.x, (int) mouseY - this.y);
    }

    private boolean isInScrollUpLocal(int localMouseX, int localMouseY) {
        int sx = LEFT_PANEL_X + LEFT_PANEL_WIDTH - 12;
        int sy = LIST_SCROLL_UP_Y;
        return localMouseX >= sx && localMouseX < sx + 10 && localMouseY >= sy && localMouseY < sy + 10;
    }

    private boolean isInScrollDownLocal(int localMouseX, int localMouseY) {
        int sx = LEFT_PANEL_X + LEFT_PANEL_WIDTH - 12;
        int sy = LIST_SCROLL_DOWN_Y;
        return localMouseX >= sx && localMouseX < sx + 10 && localMouseY >= sy && localMouseY < sy + 10;
    }
}
