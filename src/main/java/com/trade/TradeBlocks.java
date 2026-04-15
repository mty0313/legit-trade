package com.trade;

import com.trade.block.TradeBlock;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroups;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public class TradeBlocks {
    public static final Block TRADE_BLOCK = new TradeBlock(
        Block.Settings.copy(Blocks.STONE)
            .strength(2.0f, 6.0f)
            .requiresTool()
    );

    public static final Item TRADE_BLOCK_ITEM = new BlockItem(
        TRADE_BLOCK,
        new Item.Settings()
    );

    public static void register() {
        Registry.register(Registries.BLOCK, new Identifier(LegitTrade.MOD_ID, "trade_block"), TRADE_BLOCK);
        Registry.register(Registries.ITEM, new Identifier(LegitTrade.MOD_ID, "trade_block"), TRADE_BLOCK_ITEM);

        // Add to creative inventory
        ItemGroupEvents.modifyEntriesEvent(ItemGroups.FUNCTIONAL).register(content -> {
            content.add(TRADE_BLOCK_ITEM);
        });
    }
}
