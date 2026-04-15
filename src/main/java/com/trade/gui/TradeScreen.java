package com.trade.gui;

import com.trade.TradeConfig;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

@Environment(EnvType.CLIENT)
public class TradeScreen extends HandledScreen<TradeScreenHandler> {
    private static final Identifier TEXTURE = new Identifier("minecraft", "textures/gui/container/generic_54.png");

    public TradeScreen(TradeScreenHandler handler, PlayerInventory inventory, Text title) {
        super(handler, inventory, title);
        this.backgroundWidth = 176;
        this.backgroundHeight = 133;
        this.playerInventoryTitleY = this.backgroundHeight - 94;
    }

    @Override
    protected void drawBackground(DrawContext context, float delta, int mouseX, int mouseY) {
        int x = (this.width - this.backgroundWidth) / 2;
        int y = (this.height - this.backgroundHeight) / 2;

        // Draw background
        context.drawTexture(TEXTURE, x, y, 0, 0, this.backgroundWidth, 35);

        // Draw arrow indicator
        context.drawTexture(TEXTURE, x + 79, y + 17, 176, 0, 24, 16);

        // Draw player inventory background
        context.drawTexture(TEXTURE, x, y + 35, 0, 125, this.backgroundWidth, 98);
    }

    @Override
    protected void drawForeground(DrawContext context, int mouseX, int mouseY) {
        context.drawText(this.textRenderer, this.title, this.titleX, this.titleY, 4210752, false);

        // Show XP reward if valid trade
        if (handler.hasValidTrade()) {
            ItemStack output = handler.getOutputPreview();
            if (!output.isEmpty()) {
                for (TradeConfig.TradeEntry trade : TradeConfig.getTrades()) {
                    if (trade.getOutputItem() != null && output.getItem() == trade.getOutputItem()) {
                        String info = "+" + trade.xpReward + " XP";
                        context.drawText(this.textRenderer, info, 95, 8, 0x55FF55, false);
                        break;
                    }
                }
            }
        }
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

        if (this.focusedSlot != null && this.focusedSlot.getIndex() == 0) {
            ItemStack input = this.focusedSlot.getStack();
            if (!input.isEmpty()) {
                for (TradeConfig.TradeEntry trade : TradeConfig.getTrades()) {
                    if (trade.getInputItem() != null && input.getItem() == trade.getInputItem()) {
                        Text tooltip = Text.literal("→ " + trade.outputCount + "x " + trade.output + " (+" + trade.xpReward + " XP)");
                        context.drawTooltip(this.textRenderer, tooltip, x, y);
                        break;
                    }
                }
            }
        }
    }
}
