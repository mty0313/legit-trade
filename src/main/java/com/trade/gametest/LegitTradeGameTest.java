package com.trade.gametest;

import com.trade.TradeBlocks;
import com.trade.TradeConfig;
import com.trade.gui.TradeScreenHandler;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.ScreenHandlerContext;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.math.BlockPos;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class LegitTradeGameTest implements FabricGameTest {
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE)
    public void tradeEntryWithoutNbtStillValid(TestContext context) {
        TradeConfig.TradeEntry entry = new TradeConfig.TradeEntry("minecraft:emerald", "minecraft:diamond", 1, 1, 0);
        context.assertTrue(entry.isValid(), "Legacy ID-only trade entry should remain valid");
        context.complete();
    }

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE)
    public void tradeEntryWithOutputNbtCreatesNbtStack(TestContext context) {
        TradeConfig.TradeEntry entry = new TradeConfig.TradeEntry(
            "minecraft:emerald",
            "minecraft:enchanted_book",
            null,
            "{StoredEnchantments:[{id:\"minecraft:sharpness\",lvl:5s}]}",
            1,
            1,
            0
        );

        context.assertTrue(entry.isValid(), "NBT trade entry should be valid");
        context.assertTrue(!entry.createOutputStack().isEmpty(), "NBT trade should create output stack");
        context.assertTrue(entry.createOutputStack().getNbt() != null, "Output stack should contain NBT");
        context.complete();
    }

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE)
    public void simulateTradeSuccess(TestContext context) {
        List<TradeConfig.TradeGroup> originalGroups = TradeConfig.getTradeGroups();
        context.addInstantFinalTask(() -> TradeConfig.setTradeGroups(originalGroups));

        TradeConfig.TradeEntry testTrade = new TradeConfig.TradeEntry(
            "minecraft:emerald",
            "minecraft:enchanted_book",
            null,
            "{StoredEnchantments:[{id:\"minecraft:sharpness\",lvl:5s}]}",
            1,
            1,
            5
        );
        TradeConfig.setTradeGroups(List.of(new TradeConfig.TradeGroup("GameTest", List.of(testTrade))));

        ServerPlayerEntity player = context.createMockCreativeServerPlayerInWorld();
        player.getInventory().setStack(0, new ItemStack(Items.EMERALD, 1));

        TradeScreenHandler handler = new TradeScreenHandler(
            1,
            player.getInventory(),
            ScreenHandlerContext.create(context.getWorld(), context.getAbsolutePos(BlockPos.ORIGIN))
        );

        boolean selected = handler.onButtonClick(player, TradeScreenHandler.SELECT_TRADE_BASE_BUTTON_ID);
        context.assertTrue(selected, "Trade selection should succeed");

        ItemStack preview = handler.getOutputPreview();
        context.assertTrue(!preview.isEmpty(), "Output preview should be available after auto-fill");

        ItemStack result = handler.getSlot(1).takeStack(1);
        context.assertTrue(!result.isEmpty(), "Executing trade should return a result stack");
        context.assertTrue(result.isOf(Items.ENCHANTED_BOOK), "Result item should be enchanted book");
        context.assertTrue(result.getNbt() != null, "Result should keep configured NBT");

        ItemStack inputAfter = handler.getSlot(0).getStack();
        context.assertTrue(inputAfter.isEmpty(), "Input slot should be consumed after trade");
        context.complete();
    }

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE)
    public void loadTradesFromConfigFile(TestContext context) {
        Path configPath = FabricLoader.getInstance().getConfigDir().resolve("legittrade.json");
        String backupContent = null;
        boolean hadOriginalFile = false;

        try {
            hadOriginalFile = Files.exists(configPath);
            if (hadOriginalFile) {
                backupContent = Files.readString(configPath);
            }

            String finalBackupContent = backupContent;
            boolean finalHadOriginalFile = hadOriginalFile;
            context.addInstantFinalTask(() -> {
                try {
                    Files.createDirectories(configPath.getParent());
                    if (finalHadOriginalFile) {
                        Files.writeString(configPath, finalBackupContent == null ? "[]" : finalBackupContent);
                    } else {
                        Files.deleteIfExists(configPath);
                    }
                    TradeConfig.load();
                } catch (Exception ignored) {
                }
            });

            String configJson = """
                [
                  {
                    "group": "GameTestConfig",
                    "trades": [
                      {
                        "input": "minecraft:emerald",
                        "output": "minecraft:diamond",
                        "inputCount": 2,
                        "outputCount": 1,
                        "xpReward": 3
                      },
                      {
                        "input": "minecraft:emerald",
                        "output": "minecraft:enchanted_book",
                        "nbt": "{StoredEnchantments:[{id:\\"minecraft:sharpness\\",lvl:5s}]}",
                        "inputCount": 1,
                        "outputCount": 1,
                        "xpReward": 7
                      }
                    ]
                  }
                ]
                """;

            Files.createDirectories(configPath.getParent());
            Files.writeString(configPath, configJson);
            TradeConfig.load();

            List<TradeConfig.TradeGroup> groups = TradeConfig.getTradeGroups();
            context.assertTrue(groups.size() == 1, "Should load exactly one trade group from config");
            context.assertTrue(groups.get(0).trades.size() == 2, "Should load both legacy and NBT trades from config");

            TradeConfig.TradeEntry legacyTrade = groups.get(0).trades.get(0);
            context.assertTrue("minecraft:diamond".equals(legacyTrade.output), "First trade should be legacy ID-only trade");

            TradeConfig.TradeEntry nbtTrade = groups.get(0).trades.get(1);
            ItemStack output = nbtTrade.createOutputStack();
            context.assertTrue(!output.isEmpty(), "NBT trade from config should create output stack");
            context.assertTrue(output.getNbt() != null, "NBT trade output should keep configured NBT");
            context.assertTrue(output.getNbt().toString().contains("sharpness"), "NBT trade output should contain sharpness enchantment");
            context.complete();
        } catch (Exception e) {
            context.assertTrue(false, "Config-driven trade test failed: " + e.getMessage());
        }
    }

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE)
    public void tradeBlockRegistered(TestContext context) {
        context.assertTrue(TradeBlocks.TRADE_BLOCK != null, "Trade block should be registered");
        context.complete();
    }
}
