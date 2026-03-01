# SimpleKits Plugin - Complete Guide

## Overview

**SimpleKits** provides a complete kit system including:
- 🎁 **10 Pre-Built GKits** with custom enchanted gear
- 💎 **GKit Gems** - Click items to unlock kits
- 🎲 **Mystery Mob Spawners** - Get random spawners based on chance
- 📊 **GUI System** - Visual kit selector
- ⏱️ **24-Hour Cooldowns** - Each kit has a daily cooldown per player

## Features

✅ **10 Unique Gkits** - Pre-configured with themed items
✅ **GKit Gems** - Right-click gems to unlock kits  
✅ **GUI Inventory** - `/gkits` opens interactive kit selector
✅ **Direct Commands** - `/gkit <kitname>` to claim specific kits
✅ **Mystery Spawners** - Random mob drops with rarity tiers
✅ **Per-Player Cooldowns** - 24-hour cooldown tracked per kit per player
✅ **Easy to Customize** - Kit items configured through code/config

## Default GKits

| Kit | Theme | Description |
|-----|-------|-------------|
| **starter** | Basics | Basic starter items |
| **fire** | Combat | Fire-themed combat gear |
| **ice** | Combat | Frost-focused combat kit |
| **miner** | Utility | Mining and resource gathering |
| **vampire** | Combat | Life-steal combat kit |
| **thunder** | Combat | Lightning-based combat |
| **assassin** | Combat | High damage and speed |
| **tank** | Combat | Heavy defense and endurance |
| **archer** | Combat | Bow-based combat kit |
| **royalty** | Premium | Premium endgame kit |

## Commands

### Player Commands

**`/gkits`** - Open kit selection GUI
- Shows all 10 gkits
- Click to claim kit (if available)
- Shows cooldown timer if locked
- Color-coded (green = available, red = locked)

**`/gkit <kitname>`** - Claim a specific kit directly
- Example: `/gkit starter`
- Check cooldown before claiming
- Lists available kits if no argument given

**`/spawner`** - Get a mystery mob spawner
- Without args: gives mystery spawner item
- `/spawner open` - Opens random spawner (for testing)

## GKit Gems

GKit gems are **AMETHYST_SHARD** items that unlock kits when right-clicked.

### How Gems Work

1. **Item**: Amethyst Shard with custom name/lore
2. **Right-click**: Use in hand or in inventory
3. **Effect**: Unlocks the kit and gives items
4. **Cooldown**: Sets 24-hour cooldown for that kit
5. **Consumption**: Gem is consumed on use

### Gem Appearance

```
Name: §d§l[KitName] Gem
Lore:
  Right-click to unlock
  [KitName]
  
  Kit: [kitname]
  Cooldown: 24 hours
```

## Mystery Spawners

Mystery spawners give players a chance-based random mob spawner.

### Mob Drop Chances

| Mob Type | Chance | Rarity |
|----------|--------|--------|
| Zombie | 15% | Common |
| Skeleton | 15% | Common |
| Creeper | 15% | Common |
| Spider | 15% | Common |
| Cow | 10% | Common |
| Sheep | 10% | Common |
| Enderman | 8% | Uncommon |
| Blaze | 5% | Uncommon |
| Wither Skeleton | 3% | Uncommon |
| Ender Dragon | 0.5% | Legendary |
| Wither | 0.5% | Legendary |

### Using Mystery Spawners

1. Get spawner: `/spawner`
2. Right-click spawner (currently: must use command to open)
3. Random mob spawner is created with chance-based rarity
4. Place or trade the resulting spawner

## GUI System

### Opening the Kit GUI

```
/gkits
```

### GUI Features

- **Grid Layout**: Shows all kits in inventory format
- **Color Coded**:
  - 🟢 Lime dye = Available kits
  - 🔴 Red dye = Locked kits
- **Information**:
  - Kit name and status
  - Description
  - Number of items
  - Cooldown timer (if locked)
- **Click to Claim**: Click any available kit to unlock

## Configuration

### Edit config.yml

```yaml
gkit-cooldown-hours: 24

mystery-spawner:
  spawn-chances:
    ZOMBIE: 15.0
    SKELETON: 15.0
    # ... etc
```

### Change Kit Items (In Code)

Edit `KitManager.createDefaultKits()` to modify kit contents:

```java
GKit starter = new GKit("starter", "§b§lStarter Kit", "§7Basic items", 24);
starter.addItem(createEnchantedSword("§6Sword", "Vampiric"));
starter.addItem(new ItemStack(Material.SHIELD));
```

## Integration with Custom Enchants

### Setup Steps

1. Get the FactionEnchants plugin instance
2. In `KitManager.createDefaultKits()`, replace placeholder items:

```java
// Current (placeholder):
starter.addItem(createEnchantedSword("§6Starter Sword", "Vampiric"));

// Integrate with custom enchants:
FactionEnchantsPlugin enchants = (FactionEnchantsPlugin) plugin.getServer()
    .getPluginManager().getPlugin("FactionEnchants");

if (enchants != null) {
    EnchantmentManager enchMgr = enchants.getEnchantmentManager();
    ItemStack sword = new ItemStack(Material.DIAMOND_SWORD);
    
    CustomEnchantment vampiric = enchMgr.getEnchantment("vampiric");
    if (vampiric != null) {
        sword = enchMgr.applyEnchantment(sword, vampiric, 3);
    }
    starter.addItem(sword);
}
```

## API Usage

### Programmatic Kit Access

```java
SimpleKitsPlugin kits = (SimpleKitsPlugin) Bukkit.getPluginManager()
    .getPlugin("SimpleKits");

KitManager kitMgr = kits.getKitManager();

// Get a kit
GKit kit = kitMgr.getKit("starter");

// Check if player can claim
if (kitMgr.canClaimKit(player.getUniqueId(), "starter")) {
    // Give kit items to player
}

// Set cooldown
kitMgr.setKitCooldown(player.getUniqueId(), "starter");

// Get all kits
Collection<GKit> allKits = kitMgr.getAllKits();
```

### GKit Gem Access

```java
GKitGemManager gemMgr = kits.getGKitGemManager();

// Create a gem
ItemStack fireGem = gemMgr.getGem("fire");

// Check if item is a gem
if (gemMgr.isGKitGem(item)) {
    String kitName = gemMgr.extractKitNameFromGem(item);
}

// Unlock kit programmatically
gemMgr.unlockKit(player, "fire");
```

### Mystery Spawner Access

```java
MysterySpawnerManager spawnerMgr = kits.getSpawnerManager();

// Create mystery spawner
ItemStack mystery = spawnerMgr.createMysterySpawner();

// Get random spawner
EntityType mob = spawnerMgr.getRandomSpawner();
ItemStack spawner = spawnerMgr.createSpawnerForMob(mob);

// Adjust chances
spawnerMgr.setSpawnerChance(EntityType.ZOMBIE, 20.0);
```

## Cooldown System

- **Duration**: 24 hours per kit per player
- **Tracking**: Stored in memory (HashMap)
- **Reset**: Automatically at 24-hour mark
- **Admin Override**: Clear specific cooldown if needed

## Files

- `SimpleKitsPlugin.java` - Main plugin class
- `KitManager.java` - Kit management and cooldowns
- `GKit.java` - Kit data model
- `GKitGemManager.java` - Gem system
- `MysterySpawnerManager.java` - Spawner system
- `GKitsCommand.java` - `/gkits` GUI command
- `GKitCommand.java` - `/gkit` direct command
- `SpawnerCommand.java` - `/spawner` command
- `GKitGemListener.java` - Gem right-click listener
- `GKitGuiListener.java` - GUI click listener
- `config.yml` - Configuration
- `plugin.yml` - Plugin manifest

## Troubleshooting

**Q: Cooldown not working**
- Cooldowns are stored in memory - clear on server restart
- Use SQL database for persistence (future feature)

**Q: GUI shows duplicate kits**
- Ensure KitManager.createDefaultKits() isn't called multiple times

**Q: Items don't have enchants**
- Integrate with FactionEnchants plugin (see Integration section)

**Q: Gems don't get consumed**
- Check if gem item matches our criteria (AMETHYST_SHARD with "Gem" in name)

## Dependencies

- **Spigot/Paper** 1.20.1+
- **Java** 17+

## Optional Dependencies

- **FactionEnchants** - For custom enchanted items in kits

## Future Features

- [ ] Database persistence for cooldowns
- [ ] More customizable kit contents via config
- [ ] Kit purchase with economy integration
- [ ] Limited-time special kits
- [ ] Kit statistics tracking
