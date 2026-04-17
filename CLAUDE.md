# Role & Context
You are working on a **Fabric** mod project for **Minecraft 1.20.1**.

## Strict rules
1. **No Forge**: never use Forge APIs, events, annotations, or project structure.
2. **Mappings**: follow the mappings configured by the project (`yarn 1.20.1+build.10`). Never use MCP names.
3. **Side separation**: keep client-only code in client entrypoints/screens/rendering; do not reference client classes from common/server init.
4. **Registrations**: use Fabric-style registration and keep IDs, assets, lang, recipe, and loot table resources in sync.
5. **When adding blocks/items**: remember the full asset chain: blockstate, block model, item model, lang keys, recipe if needed, and loot table.
6. **Prefer current project behavior over stale docs**: `README.md` and older notes may lag behind code.

# LegitTrade 当前项目状态

`LegitTrade` 是一个 Fabric 1.20.1 交易模组。核心玩法是通过放置 `legittrade:trade_block` 打开交易 GUI，按配置完成“输入物品 -> 输出物品 + XP”的服务端交易。

## 当前构建与环境

- Minecraft: `1.20.1`
- Loader: `0.14.23`
- Fabric API: `0.90.4+1.20.1`
- Java: `17`
- 当前构建命令：`./gradlew build`
- 产物位于：`build/libs/`

## 当前实际使用方式

- 没有 `/trade` 命令
- 有 `/tradereload` 命令，用于服务端热重载交易配置并重新同步到在线玩家
- 玩家通过放置 `trade_block` 并右键打开交易界面

## 当前交易界面行为

实际交互以当前代码为准：

1. 左侧显示分组交易列表
2. 点击某条交易后，服务端会尝试从玩家背包自动填充输入槽
3. 输入槽内容变化时，输出预览会自动刷新
4. 点击输出槽时，服务端执行真实交易
5. 成功交易会扣输入、给输出、发 XP，并播放村民成功音效

## 当前配置系统

### 配置路径

当前配置路径通过 Fabric Loader 获取：

- `FabricLoader.getInstance().getConfigDir().resolve("legittrade.json")`

通常对应运行目录下的：

- `config/legittrade.json`

### 当前支持的配置格式

当前主要使用“分组格式”：

```json
[
  {
    "group": "建材",
    "trades": [
      {
        "input": "minecraft:dirt",
        "output": "minecraft:stone",
        "inputCount": 1,
        "outputCount": 64,
        "xpReward": 500
      }
    ]
  }
]
```

同时兼容旧的扁平数组格式；旧格式会在加载时被包装进默认分组。

### 当前配置校验规则

`TradeConfig` 当前会校验：

- `input` / `output` 不为 `null`
- `Identifier.tryParse(...)` 可解析
- 物品 ID 必须真实存在于 `Registries.ITEM`
- `inputCount` / `outputCount` 范围必须在 `1..64`
- `xpReward >= 0`
- 自动去重相同交易条目
- 超过限制的 group / trade 会被忽略并记录警告

### 当前同步/加载限制

以下限制在 `TradeConfig` 和 `ConfigSyncPacket` 中已经统一：

- `MAX_GROUPS = 128`
- `MAX_TRADES_PER_GROUP = 1024`
- `MAX_ITEM_ID_LENGTH = 128`
- `MAX_GROUP_NAME_LENGTH = 64`

## 当前已修复事项

以下问题已经修复并应作为当前事实：

1. **输入槽变化后输出预览不刷新的问题** 已修复
   - 现在 `inputInventory.markDirty()` 会触发 `onContentChanged()`
2. **`trade_block` 缺少掉落表** 已修复
   - 已有 `data/legittrade/loot_tables/blocks/trade_block.json`
3. **配置校验未验证物品是否真实存在** 已修复
4. **客户端断线后保留旧交易配置** 已修复
   - `LegitTradeClient` 会在 `DISCONNECT` 时清空客户端缓存交易组
5. **服务端配置限制与网络包限制不一致** 已修复
6. **配置路径未使用 Fabric config dir API** 已修复

## 当前关键实现文件

```text
src/main/java/com/trade/
├── LegitTrade.java                # 主入口；加载配置、注册方块、同步配置、注册 /tradereload
├── LegitTradeClient.java          # 客户端入口；注册 Screen 与配置同步包接收；断线清空客户端配置
├── TradeBlocks.java               # trade_block 与 block item 注册
├── TradeConfig.java               # 配置加载、分组/旧格式兼容、校验、限制与内存存储
├── block/TradeBlock.java          # 方块交互；右键打开交易界面
├── gui/
│   ├── TradeScreen.java           # 客户端 GUI 绘制与点击逻辑
│   └── TradeScreenHandler.java    # 服务端容器逻辑、自动填充、真实交易执行
└── network/
    ├── TradePackets.java          # ScreenHandlerType 注册
    └── ConfigSyncPacket.java      # 服务端 -> 客户端配置同步
```

## 资源链路

当前 `trade_block` 资源链路是完整的：

- `assets/legittrade/blockstates/trade_block.json`
- `assets/legittrade/models/block/trade_block.json`
- `assets/legittrade/models/item/trade_block.json`
- `assets/legittrade/lang/en_us.json`
- `assets/legittrade/lang/zh_cn.json`
- `data/legittrade/recipes/trade_block.json`
- `data/legittrade/loot_tables/blocks/trade_block.json`

## 当前已知未解决问题

### 1. 输入槽仍然允许放入任意物品
这是当前最重要的 UX / 交互风险。

`TradeScreenHandler` 的输入槽仍是普通 `Slot`，`quickMove(...)` 也仍允许把任意物品塞进输入槽。
这意味着：

- 玩家可能误把无关物品放进输入槽
- 切换交易时，旧输入会尝试返还到背包
- 如果背包放不下，会 `player.dropItem(...)` 掉到地上

### 2. `emerald-drop-analysis.md` 的结论仍然成立
当前代码状态下，**没有“玩家进入游戏时自动生成并丢出绿宝石”** 的逻辑。
更可能的解释仍然是：

- 某个真实存在于输入槽的物品
- 在切换交易或关闭 GUI 时
- 因背包不足被自动掉落到地上

因此该现象更像 **容器设计问题**，而不是“登录时刷物品”。

### 3. `TradeBlock.java` 仍有 deprecated API 编译提示
当前构建仍会提示：

- `TradeBlock.java uses or overrides a deprecated API`

暂时不影响构建，但后续升级版本时应优先处理。

## 后续开发建议

如果继续改进，优先级建议如下：

1. 限制输入槽只能接受“当前所选交易的输入物品”
2. 同步限制 `quickMove(...)`，避免任意物品被塞进输入槽
3. 重新设计切换交易时的返还策略，尽量减少自动掉地
4. 如需继续排查异常掉落，重点围绕 `returnInputToPlayer(...)`、`autoFillSelectedTrade(...)`、`onClosed(...)` 做测试

## 备注

- 修改功能实现时，请优先参考代码，而不是旧版 `README.md`
- 如果新增资源，请不要漏掉 loot table
- 如果新增客户端缓存状态，请补连接生命周期清理
