# LegitTrade

Fabric 1.20.1 交易模组。通过放置 `trade_block` 打开交易界面，用低价值物品换取高价值物品，并获得经验奖励。

## 特性

- **游戏内交易界面** - 右键 `trade_block` 打开 GUI
- **分组交易** - 交易按分组组织，便于管理
- **NBT 支持** - 支持带 NBT 物品的匹配与输出
- **Web 配置界面** - 浏览器管理交易配置
- **自动填充** - 选择交易后自动从背包填充输入槽
- **批量交易** - Shift+点击输出槽连续执行多次交易
- **热重载** - `/tradereload` 重载配置并同步在线玩家

## 构建

```bash
./gradlew build
```

产物：`build/libs/legittrade-1.0.2[-SNAPSHOT-日期].jar`

发布版本：
```bash
./gradlew build -Prelease
```

## 使用

### 游戏内

1. 放置 `trade_block` 方块，右键打开交易界面
2. 左侧显示分组交易列表
3. 点击交易项，系统自动从背包填充输入槽
4. 输入满足条件时，右侧显示输出预览
5. 点击输出槽执行交易
6. Shift+点击输出槽批量执行（直到背包满或输入不足）

### 命令

- `/tradereload` - 服务端热重载配置，同步给在线玩家（需要 OP 权限）

### Web 配置界面

服务端启动后访问 `http://服务器IP:39482`：

- 查看所有交易配置
- 添加/编辑/删除交易
- 搜索物品 ID
- NBT 编辑器
- 保存后自动同步给在线玩家

配置文件：`config/legittrade-web.json`

```json
{
  "enabled": true,
  "port": 39482,
  "bindAddress": "0.0.0.0"
}
```

## 配置

路径：`config/legittrade.json`

### 分组格式（推荐）

```json
[
  {
    "group": "建材",
    "trades": [
      {
        "input": "minecraft:dirt",
        "output": "minecraft:stone",
        "inputCount": 64,
        "outputCount": 1,
        "xpReward": 10
      }
    ]
  }
]
```

### NBT 支持

```json
{
  "input": "minecraft:diamond_sword",
  "output": "minecraft:diamond_sword",
  "inputNbt": "{Damage:0}",
  "outputNbt": "{Enchantments:[{id:\"minecraft:sharpness\",lvl:5}]}",
  "nbtMatchMode": "exact",
  "inputCount": 1,
  "outputCount": 1,
  "xpReward": 100
}
```

### 字段说明

| 字段 | 类型 | 说明 |
|------|------|------|
| `group` | string | 分组名称 |
| `input` | string | 输入物品 ID |
| `output` | string | 输出物品 ID |
| `inputNbt` | string? | 输入物品 NBT 匹配条件 |
| `outputNbt` | string? | 输出物品 NBT |
| `nbtMatchMode` | string | NBT 匹配模式：`exact`/`contains`/`ignore` |
| `inputCount` | int | 消耗数量，范围 1~64 |
| `outputCount` | int | 获得数量，范围 1~64 |
| `xpReward` | int | 经验奖励，≥ 0 |

### NBT 匹配模式

- `exact` - 完全匹配 NBT（默认）
- `contains` - 输入物品包含指定 NBT 标签即可
- `ignore` - 忽略输入物品 NBT

### 配置验证

加载时自动：

- 过滤无效物品 ID
- 验证 NBT 语法
- 限制数量范围 1~64
- 去重重复交易
- 空配置回退默认值

### 限制

- 最大分组数：128
- 每组最大交易数：1024
- 物品 ID 最大长度：128 字符
- 分组名最大长度：64 字符
- NBT 最大长度：4096 字符

## 技术细节

- 交易执行由服务端负责，防止客户端伪造
- 玩家加入时自动同步交易配置
- 配置更新后自动同步在线玩家
- 客户端断开时清空本地缓存
- `trade_block` 包含完整资源链（配方、模型、语言文件、掉落表）

## 依赖

| 依赖 | 版本 |
|------|------|
| Minecraft | 1.20.1 |
| Fabric Loader | 0.14.23 |
| Fabric API | 0.90.4+1.20.1 |
| Java | 17 |

## 许可证

MIT
