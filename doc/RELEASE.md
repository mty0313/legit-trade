# LegitTrade 1.0.2 - Server Trade System

## The Story

It started with **Simple Villager** - capturing villagers for easy trading.

Then I switched to **Custom Villager Trade** to customize trade offers.

Then I wondered: **Why deal with villagers at all?**

I didn't want to wrestle with villagers - transporting them to the right spot, binding workstations, protecting them from zombies, rerolling for desired trades... too much hassle.

What I wanted was simple:

> **A creative mode that looks like survival** - at least for item acquisition.

- Not interested in making the game harder, just more convenient
- Don't want to deal with villagers, just click and get items
- Better than creative in some ways - easily customize NBT, get Sharpness X netherite swords, Knockback X bows, beyond vanilla power
- Even reward myself with 1000 XP per trade

Thus LegitTrade - legitimate trading, completely legitimate. Aren't I obtaining these items in survival mode?

**Right-click block, click, done. Villagers eliminated.**

## Core Features

### In-Game Ready to Use

- **Low cost**: 9 logs of any type to craft a trade block
- **Zero learning curve**: Right-click `trade_block` to open, select trade on left, click output on right, done
- **Auto-fill**: System automatically pulls required items from inventory
- **Batch trading**: Shift+click output slot to execute continuously until inventory full or materials exhausted
- **Real-time preview**: Output preview updates automatically when input changes
- **Strong mod compatibility**: Any item that exists in the game can be found via Web UI and added to trades

### Web Management Panel

**Manage trades via browser, no server restart needed**

Visit `http://server-ip:39482`:

- Visual add/edit/delete trades
- Item ID search with suggestions
- NBT editor (enchantments, durability, etc.)
- Auto-sync to all online players after save

![Trade List](img/brief.png)

![Trade Edit](img/edit.png)

![NBT Editor](img/nbt-edit.png)

### Server-Side Reliability

- **Hot reload**: `/tradereload` reloads config without server restart
- **Auto-sync**: Players receive latest trade config on join
- **Secure**: All trade logic executed server-side, cheat-proof

## Typical Use Cases

| Scenario | Traditional | LegitTrade |
|----------|-------------|------------|
| Redeem event rewards | Remember `/exchange xxx`, players mistype | Right-click block, click once |
| Server currency exchange | Find NPC, dialogue, select option | Right-click block, click once |
| Material recycling | Multiple command combinations | Right-click block, click once |
| Modify trade config | Edit file, restart server | Web UI edit, auto-applied |

## Config Example

```json
[
  {
    "group": "Starter Kit",
    "trades": [
      {
        "input": "minecraft:dirt",
        "output": "minecraft:diamond",
        "inputCount": 64,
        "outputCount": 1,
        "xpReward": 100
      }
    ]
  }
]
```

## Download & Install

- **Supported version**: Minecraft 1.20.1 + Fabric
- **Install**: Place jar file in `mods` folder
- **Dependency**: Fabric API

## Technical Info

| Item | Version |
|------|---------|
| Minecraft | 1.20.1 |
| Fabric Loader | 0.14.23 |
| Fabric API | 0.90.4+1.20.1 |
| Java | 17 |

## License

MIT
