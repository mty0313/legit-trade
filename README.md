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

列表式交易界面（类似村民交易 UI）：

1. 滚动浏览所有可用交易配方
2. 每个配方显示：输入物品 → 输出物品 + XP 奖励
3. 绿色 ✓ 表示当前物品足够，可执行交易
4. 橙色显示当前拥有数量/需求数量（物品不足时）
5. 点击配方执行交易
6. 交易成功：消耗输入物品，获得输出物品 + 经验，播放村民交易音效
7. 交易失败：播放失败音效，显示红色错误提示

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

## 依赖

- Fabric Loader 0.14.23
- Fabric API 0.90.4+1.20.1
- Minecraft 1.20.1
- Java 17

## 许可证

MIT
