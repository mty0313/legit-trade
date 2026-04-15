# Role & Context
You are an expert Minecraft Mod Developer specializing strictly in the **Fabric Mod Loader** and **Fabric API**.
The target Minecraft version is 1.20.1.

# Strict Rules for Generation:
1. NO FORGE: Never use Minecraft Forge APIs, events, or annotations (e.g., @Mod, SubscribeEvent).
2. MAPPINGS: Always use official **Yarn mappings** or **Mojang mappings** (depending on the build.gradle setup). Never use old MCP mappings (e.g., func_12345_a).
3. MOD INITIALIZATION: Use strictly `ModInitializer`, `ClientModInitializer`, and `DedicatedServerModInitializer`. Register items/blocks in the `onInitialize()` method using `Registry.register()`.
4. MIXINS: You are an expert in SpongePowered Mixins.
   - Always verify the target method signature.
   - Use `@Inject`, `@Redirect`, `@Overwrite`, and `@Shadow` correctly.
   - For `@Inject`, ensure `CallbackInfo` or `CallbackInfoReturnable` is appropriately handled.
5. CLIENT vs SERVER: Always strictly separate client-side code (rendering, screens, keybinds) from server-side code. Never call client-only classes from common or server initialization.
6. JSON ASSETS: When adding blocks or items, remind me or provide the necessary JSON files (blockstates, models, lang files, loot tables).

# LegitTrade

Fabric 1.20.1 交易mod。"完全合法"的生存模式物品交换。

## 构建

```bash
./gradlew build
```

输出: `build/libs/legittrade-1.0.0.jar`

## 使用

- `/trade` - 打开交易界面
- `/tradereload` - 重载配置（热重载，无需重启服务器）
- 放置 `trade_block` 方块，右键打开交易界面

## 交易界面

1. 左侧槽位放入输入物品
2. 右侧槽位显示输出预览（匹配配置时显示）
3. 点击输出物品执行交易
4. 交易成功：消耗输入物品，获得输出物品 + 经验，播放村民交易音效
5. 交易失败：播放失败音效，显示红色错误提示

## 配置

路径: `config/legittrade.json`

```json
[
  {
    "input": "minecraft:dirt",
    "output": "minecraft:diamond",
    "inputCount": 64,
    "outputCount": 1,
    "xpReward": 100
  }
]
```

字段:
- `input` - 输入物品ID（必须为有效的物品注册ID）
- `output` - 输出物品ID（必须为有效的物品注册ID）
- `inputCount` - 消耗数量（必须 > 0）
- `outputCount` - 获得数量（必须 > 0）
- `xpReward` - 经验奖励（必须 >= 0）

**配置验证：** 加载时自动过滤无效条目并记录警告日志。无效配置使用默认值。

## 结构

```
src/main/java/com/trade/
├── LegitTrade.java           # 主入口，命令注册
├── LegitTradeClient.java     # 客户端入口，Screen注册
├── TradeConfig.java          # 配置加载与验证
├── TradeBlocks.java          # 方块注册
├── block/TradeBlock.java     # 交易方块
├── gui/
│   ├── TradeScreen.java      # 客户端GUI渲染
│   └── TradeScreenHandler.java # 服务端交易逻辑
└── network/
    ├── TradePackets.java     # ScreenHandler注册
    └── ConfigSyncPacket.java # 服务端→客户端配置同步
```

## 技术细节

- **配置同步：** 玩家加入时自动同步配置到客户端，确保GUI显示正确
- **线程安全：** `TradeEntry` 为不可变类，配置列表使用不可变包装
- **输入验证：** 物品ID使用 `Identifier.tryParse()` 防止格式错误崩溃
- **虚拟槽位：** 输出槽为虚拟槽位，点击时服务端执行交易逻辑

## 依赖

- Fabric Loader 0.14.23
- Fabric API 0.90.4+1.20.1
- Minecraft 1.20.1
- Java 17
