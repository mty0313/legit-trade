# LegitTrade

Fabric 1.20.1 交易mod。"完全合法"的生存模式物品交换。

## 构建

```bash
./gradlew build
```

输出: `build/libs/legittrade-1.0.0.jar`

## 使用

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
- `inputCount` - 消耗数量（自动限制在 1~64）
- `outputCount` - 获得数量（自动限制在 1~64）
- `xpReward` - 经验奖励（必须 >= 0）

**配置验证：** 加载时自动过滤无效条目并记录警告日志；数量字段会自动钳制到 1~64。无效配置使用默认值。

## 依赖

- Fabric Loader 0.14.23
- Fabric API 0.90.4+1.20.1
- Minecraft 1.20.1
- Java 17

## 许可证

MIT
