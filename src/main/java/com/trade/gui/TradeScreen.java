package com.trade.gui;

import com.trade.TradeConfig;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

@Environment(EnvType.CLIENT)
public class TradeScreen extends HandledScreen<TradeScreenHandler> {
    private static final Identifier TEXTURE = new Identifier("minecraft", "textures/gui/container/generic_54.png");
    private static final int BACKGROUND_WIDTH = 176;
    private static final int BACKGROUND_HEIGHT = 166;

    private final TradeListWidget tradeListWidget;

    public TradeScreen(TradeScreenHandler handler, PlayerInventory inventory, Text title) {
        super(handler, inventory, title);
        this.backgroundWidth = BACKGROUND_WIDTH;
        this.backgroundHeight = BACKGROUND_HEIGHT;
        this.playerInventoryTitleY = this.backgroundHeight - 94;

        this.tradeListWidget = new TradeListWidget(18, 18, 5);
        this.tradeListWidget.setHandler(handler);
    }

    @Override
    protected void drawBackground(DrawContext context, float delta, int mouseX, int mouseY) {
        int x = (this.width - BACKGROUND_WIDTH) / 2;
        int y = (this.height - BACKGROUND_HEIGHT) / 2;

        // Main background
        context.drawTexture(TEXTURE, x, y, 0, 0, BACKGROUND_WIDTH, 35);
        context.drawTexture(TEXTURE, x, y + 35, 0, 125, BACKGROUND_WIDTH, 98);
    }

    @Override
    protected void drawForeground(DrawContext context, int mouseX, int mouseY) {
        context.drawText(this.textRenderer, this.title, this.titleX, this.titleY, 4210752, false);

        // Render trade list (mouseX/mouseY already relative to GUI)
        tradeListWidget.render(context, this.textRenderer, mouseX, mouseY);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.renderBackground(context);
        super.render(context, mouseX, mouseY, delta);

        // Draw tooltips for hovered trade entry
        if (tradeListWidget.hasTrades()) {
            drawTradeTooltip(context, mouseX, mouseY);
        }

        this.drawMouseoverTooltip(context, mouseX, mouseY);
    }

    private void drawTradeTooltip(DrawContext context, int mouseX, int mouseY) {
        var hoveredIndex = tradeListWidget.getHoveredTradeIndex(mouseX - this.x, mouseY - this.y);
        if (hoveredIndex.isPresent()) {
            TradeConfig.TradeEntry trade = TradeConfig.getTrades().get(hoveredIndex.getAsInt());
            // Skip tooltip for invalid trades
            if (trade.getInputItem() == null || trade.getOutputItem() == null) {
                return;
            }
            int availableCount = handler != null ? trade.countItemsInInventory(handler.getServerPlayer()) : 0;
            boolean canExecute = availableCount >= trade.inputCount;

            Text tooltip;
            if (canExecute) {
                tooltip = Text.literal("Click to trade: ")
                    .append(Text.literal(trade.inputCount + "x " + trade.input))
                    .append(Text.literal(" → "))
                    .append(Text.literal(trade.outputCount + "x " + trade.output));
            } else {
                tooltip = Text.literal("§cNeed " + trade.inputCount + "x " + trade.input + " (have " + availableCount + ")");
            }
            context.drawTooltip(this.textRenderer, tooltip, mouseX, mouseY);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int relX = (int) mouseX - this.x;
        int relY = (int) mouseY - this.y;

        if (tradeListWidget.mouseClicked(relX, relY, button)) {
            return true;
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
        int relX = (int) mouseX - this.x;
        int relY = (int) mouseY - this.y;

        if (tradeListWidget.mouseScrolled(relX, relY, amount)) {
            return true;
        }

        return super.mouseScrolled(mouseX, mouseY, amount);
    }
}
