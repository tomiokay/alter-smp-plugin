# Legendary Weapons SMP Plugin

**Epic Minecraft plugin with 15 legendary items, each featuring devastating abilities, spectacular particle effects, and a unique 5×5 crafting system. Balanced for PvP with Protection IV armor in mind.**

Transform your SMP with powerful legendary items that players will fight to obtain. Each legendary has unique passive and/or active abilities, with enhanced visuals that make every battle epic.

## Features

- **15 Unique Legendary Items** - 6 swords, 2 shields, 1 pickaxe, 1 axe, 1 trident, 4 armor pieces
- **Custom 5×5 Crafting System** - Separate from vanilla crafting via Legendary Altars
- **One Craft Per Player** - Each legendary can only be crafted once per player
- **Ability System** - Active abilities via `/ability 1` and `/ability 2` with cooldowns
- **Passive Armor Abilities** - Unique passive effects for legendary armor pieces
- **Trust System** - Prevent friendly fire with `/trust` command
- **Global Reset Command** - OPs can reset all crafting progress
- **Persistent Data** - Crafting history, cooldowns, and altar locations are saved

## Requirements

- **Minecraft Version:** 1.21.8
- **Server Software:** Paper (recommended) or Spigot-compatible
- **Java Version:** JDK 21+

## Installation

1. **Build the Plugin:**
   ```bash
   ./gradlew build
   ```
   Or on Windows:
   ```bash
   gradlew.bat build
   ```

2. **Locate the JAR:**
   - The compiled JAR will be in `build/libs/`
   - Look for `LegendaryWeaponsSMP-1.0.0.jar`

3. **Install on Server:**
   - Copy the JAR file to your Paper 1.21.8 server's `plugins/` folder
   - Restart or reload the server

## Getting Started

### For Server Operators

1. **Obtain a Legendary Altar:**
   ```
   /givealtar [player]
   ```
   - If no player is specified, gives the altar to yourself
   - Only OPs can use this command

2. **Place the Altar:**
   - Place the Legendary Altar item like any other block
   - Right-click to open the 5×5 crafting GUI

3. **Craft Legendary Weapons:**
   - Arrange ingredients in the 5×5 grid according to recipes
   - The output will appear in the result slot
   - Each legendary can only be crafted once per world (first player to craft it claims it)

### For Players

1. **Using the Altar:**
   - Right-click a placed Legendary Altar to open the 5×5 crafting menu
   - Place ingredients according to legendary recipes
   - If the pattern matches, the legendary item appears in the output slot
   - Take the item to complete crafting (only works once per legendary per world)
   - If someone else already crafted it, you'll see who forged it

2. **Using Abilities:**
   - Equip your legendary weapon (main hand, boots, or offhand depending on type)
   - Use `/ability 1` to activate the first ability
   - Use `/ability 2` to activate the second ability
   - Each ability has its own cooldown timer

3. **Passive Effects:**
   - Passive abilities are always active while the legendary is equipped
   - Some passives trigger on specific conditions (e.g., hitting enemies, low HP)

## Commands

| Command | Permission | Description |
|---------|-----------|-------------|
| `/givealtar [player]` | `legendaryweapons.givealtar` (OP) | Gives a Legendary Altar item |
| `/ability <1\|2>` | None | Uses the active ability of your equipped legendary |
| `/kreset` | `legendaryweapons.kreset` (OP) | Resets all legendary crafting progress globally |
| `/kresetplayer <player>` | `legendaryweapons.kreset` (OP) | Resets crafting progress for a specific player |
| `/giveweapon <type> [player]` | `legendaryweapons.giveweapon` (OP) | Gives a specific legendary weapon |
| `/givelegendary <type> [player]` | `legendaryweapons.givelegendary` (OP) | Gives a specific legendary item |
| `/trust <player>` | None | Toggle trust with a player (prevents friendly fire) |
| `/lreload` | `legendaryweapons.reload` (OP) | Reloads plugin configuration |
| `/cooldown` | None | Check your current ability cooldowns |

## Legendary Weapons Overview

### 🌠 Blade of the Fractured Stars (Sword)
- **Passive:** Flashburst Counter - Every 20 hits blinds nearby enemies
- **Ability 1:** Star Rift Slash (25s) - Beam attack through walls
- **Ability 2:** Stargate Blink (45s) - Teleport up to 45 blocks

### 🔥 Emberheart Scythe (Sword)
- **Passive:** Heat Shield - Immune to fire and explosions
- **Ability 1:** Flame Harvest (30s) - Fiery explosion grants absorption
- **Ability 2:** Fire Rebirth (180s) - Cheat death with flames

### 💨 Tempestbreaker Spear (Trident)
- **Passive:** Windwalker - Water mobility buffs
- **Ability 1:** Gale Throw (25s) - Wind vortex on impact
- **Ability 2:** Stormcall (50s) - Cone of stunning lightning

### 🌑 Umbra Veil Dagger (Sword)
- **Passive:** Shadow Presence - Speed III while sneaking
- **Ability 1:** Shadowstep (20s) - Dash and go invisible
- **Ability 2:** Soul Mark (60s) - Mark target for true damage

### 🌿 Heartroot Guardian Axe (Axe)
- **Passive:** Nature Channel - Regeneration on natural blocks
- **Ability 1:** Nature Grasp (35s) - Root enemies in place
- **Ability 2:** Forest Shield (70s) - Axe becomes breach weapon

### ⛓️ Chains of Eternity (Shovel)
- **Passive:** Soul Links - Every 5th hit immobilizes
- **Ability 1:** Soul Bind (35s) - Pull and slow target
- **Ability 2:** Prison of the Damned (65s) - Cage target in iron bars

### ⚡ Skybreaker Boots (Diamond Boots)
- **Passive:** No fall damage + Meteor Slam - Shift mid-air to slam down dealing area damage based on fall distance (no knockback)

### ⛏️ Copper Pickaxe (Netherite Pickaxe)
- **Ability 1:** Toggle 3x3 Mining - Mine in a 3x3 area
- **Ability 2:** Toggle Silk Touch/Fortune III - Switch between enchantments

### 🌩️ Thunderforge Chestplate (Diamond Chestplate)
- **Passive:** Electric Shockwave - Every 7 hits taken, releases a 5-block radius shockwave (4 damage + knockback)

### ⚡ Ionflare Leggings (Diamond Leggings)
- **Passive:** Ion Charge - Every 5 hits dealt releases chain lightning hitting 3 targets (6 damage each)

### 💀 Bloodreaper Hood (Diamond Helmet)
- **Passive:** Blood Harvest - Kills grant +5 hearts for 5 minutes
- **Passive:** Critical Rush - Crits grant +10% speed for 3 seconds

### 💎 Celestial Aegis Shield (Shield)
- **Passive:** Aura of Protection - Allies gain Resistance I
- **Ability 1:** Radiant Block (40s) - Reflect 75% damage for 5s
- **Ability 2:** Heaven's Wall (90s) - Summon protective barrier

### 🧭 Chrono Edge (Sword)
- **Passive:** Last Second - Buffs when low HP
- **Ability 1:** Echo Strike (40s) - Hits repeat after 1 second
- **Ability 2:** Time Rewind (120s) - Return to past state

### 💀 Oblivion Harvester (Sword)
- **Passive:** Soul Collector - Gain damage from kills
- **Ability 1:** Void Slice (30s) - Sweeping void attack
- **Ability 2:** Void Rift (85s) - Create damaging black hole

### 🐉 Eclipse Devourer (Sword)
- **Passive:** Dragon's Gaze - Nearby players glow
- **Ability 1:** Void Rupture (35s) - Void arc with blindness
- **Ability 2:** Cataclysm Pulse (95s) - Dark explosion with pull

## Crafting Recipes

All legendary items are crafted in a 5×5 Legendary Altar. Below are the crafting patterns (X = empty slot):

### 🌠 Blade of the Fractured Stars
```
Nether Star | Diamond Block | Diamond Block | Diamond Block | Nether Star
Diamond Block | Crying Obsidian | Amethyst Block | Crying Obsidian | Diamond Block
Diamond Block | Amethyst Block | Netherite Sword | Amethyst Block | Diamond Block
Diamond Block | Crying Obsidian | Amethyst Block | Crying Obsidian | Diamond Block
Nether Star | Diamond Block | Diamond Block | Diamond Block | Nether Star
```

### 🔥 Emberheart Scythe
```
Blaze Rod | Fire Charge | Magma Block | Fire Charge | Blaze Rod
Fire Charge | Netherite Ingot | Netherite Ingot | Netherite Ingot | Fire Charge
Magma Block | Netherite Ingot | Netherite Sword | Netherite Ingot | Magma Block
Fire Charge | Netherite Ingot | Netherite Ingot | Netherite Ingot | Fire Charge
Blaze Rod | Fire Charge | Magma Block | Fire Charge | Blaze Rod
```

### 💨 Tempestbreaker Spear
```
Feather | Phantom Membrane | Phantom Membrane | Phantom Membrane | Feather
Phantom Membrane | Diamond | Prismarine Crystals | Diamond | Phantom Membrane
Phantom Membrane | Prismarine Crystals | Trident | Prismarine Crystals | Phantom Membrane
Phantom Membrane | Diamond | Prismarine Crystals | Diamond | Phantom Membrane
Feather | Phantom Membrane | Phantom Membrane | Phantom Membrane | Feather
```

### 🌑 Umbra Veil Dagger
```
Ender Pearl | Obsidian | Obsidian | Obsidian | Ender Pearl
Obsidian | Sculk | Echo Shard | Sculk | Obsidian
Obsidian | Echo Shard | Netherite Sword | Echo Shard | Obsidian
Obsidian | Sculk | Echo Shard | Sculk | Obsidian
Ender Pearl | Obsidian | Obsidian | Obsidian | Ender Pearl
```

### 🌿 Heartroot Guardian Axe
```
Oak Log | Moss Block | Moss Block | Moss Block | Oak Log
Moss Block | Emerald Block | Glow Berries | Emerald Block | Moss Block
Moss Block | Glow Berries | Netherite Axe | Glow Berries | Moss Block
Moss Block | Emerald Block | Glow Berries | Emerald Block | Moss Block
Oak Log | Moss Block | Moss Block | Moss Block | Oak Log
```

### ⛓️ Chains of Eternity
```
Chain | Soul Sand | Wither Skeleton Skull | Soul Sand | Chain
Soul Sand | Netherite Scrap | Netherite Scrap | Netherite Scrap | Soul Sand
Wither Skeleton Skull | Netherite Scrap | Wooden Shovel | Netherite Scrap | Wither Skeleton Skull
Soul Sand | Netherite Scrap | Netherite Scrap | Netherite Scrap | Soul Sand
Chain | Soul Sand | Wither Skeleton Skull | Soul Sand | Chain
```

### ⚡ Skybreaker Boots
```
Feather | Phantom Membrane | Diamond Block | Phantom Membrane | Feather
Phantom Membrane | Diamond | Diamond | Diamond | Phantom Membrane
Diamond Block | Diamond | Diamond Boots | Diamond | Diamond Block
Phantom Membrane | Diamond | Diamond | Diamond | Phantom Membrane
Feather | Phantom Membrane | Diamond Block | Phantom Membrane | Feather
```

### ⛏️ Copper Pickaxe
```
Copper Block | Copper Block | Copper Block | Copper Block | Copper Block
Copper Block | Netherite Ingot | Netherite Ingot | Netherite Ingot | Copper Block
Copper Block | Netherite Ingot | Netherite Pickaxe | Netherite Ingot | Copper Block
Copper Block | Netherite Ingot | Netherite Ingot | Netherite Ingot | Copper Block
Copper Block | Copper Block | Copper Block | Copper Block | Copper Block
```

### 🌩️ Thunderforge Chestplate
```
Lightning Rod | Copper Block | Copper Block | Copper Block | Lightning Rod
Copper Block | Diamond | Diamond | Diamond | Copper Block
Copper Block | Diamond | Diamond Chestplate | Diamond | Copper Block
Copper Block | Diamond | Diamond | Diamond | Copper Block
Lightning Rod | Copper Block | Copper Block | Copper Block | Lightning Rod
```

### ⚡ Ionflare Leggings
```
Prismarine Crystals | Diamond | Diamond | Diamond | Prismarine Crystals
Diamond | Amethyst Block | Amethyst Block | Amethyst Block | Diamond
Diamond | Amethyst Block | Diamond Leggings | Amethyst Block | Diamond
Diamond | Amethyst Block | Amethyst Block | Amethyst Block | Diamond
Prismarine Crystals | Diamond | Diamond | Diamond | Prismarine Crystals
```

### 💀 Bloodreaper Hood
```
Wither Rose | Redstone Block | Redstone Block | Redstone Block | Wither Rose
Redstone Block | Diamond | Fermented Spider Eye | Diamond | Redstone Block
Redstone Block | Fermented Spider Eye | Diamond Helmet | Fermented Spider Eye | Redstone Block
Redstone Block | Diamond | Fermented Spider Eye | Diamond | Redstone Block
Wither Rose | Redstone Block | Redstone Block | Redstone Block | Wither Rose
```

### 💎 Celestial Aegis Shield
```
End Stone | Glowstone | Glowstone | Glowstone | End Stone
Glowstone | Gold Block | Totem of Undying | Gold Block | Glowstone
Glowstone | Totem of Undying | Shield | Totem of Undying | Glowstone
Glowstone | Gold Block | Totem of Undying | Gold Block | Glowstone
End Stone | Glowstone | Glowstone | Glowstone | End Stone
```

### 🧭 Chrono Edge
```
Clock | Redstone Block | Redstone Block | Redstone Block | Clock
Redstone Block | Amethyst Shard | Recovery Compass | Amethyst Shard | Redstone Block
Redstone Block | Recovery Compass | Netherite Sword | Recovery Compass | Redstone Block
Redstone Block | Amethyst Shard | Recovery Compass | Amethyst Shard | Redstone Block
Clock | Redstone Block | Redstone Block | Redstone Block | Clock
```

### 💀 Oblivion Harvester
```
Netherite Block | Wither Rose | Wither Rose | Wither Rose | Netherite Block
Wither Rose | Obsidian | Nether Star | Obsidian | Wither Rose
Wither Rose | Nether Star | Netherite Sword | Nether Star | Wither Rose
Wither Rose | Obsidian | Nether Star | Obsidian | Wither Rose
Netherite Block | Wither Rose | Wither Rose | Wither Rose | Netherite Block
```

### 🐉 Eclipse Devourer
```
Dragon Head | Dragon Breath | End Stone | Dragon Breath | Dragon Head
Dragon Breath | End Crystal | Elytra | End Crystal | Dragon Breath
End Stone | Elytra | Netherite Sword | Elytra | Dragon Egg
Dragon Breath | End Crystal | Elytra | End Crystal | Dragon Breath
Dragon Head | Dragon Breath | End Stone | Dragon Breath | Dragon Head
```

## Important Notes

### Normal Crafting Table
- **The vanilla 3×3 crafting table is completely unaffected by this plugin**
- Normal crafting tables continue to work as usual
- Only Legendary Altars open the special 5×5 crafting menu

### True Damage
- **Only the Umbra Veil Dagger's Soul Mark ability deals true damage**
- All other damage in the plugin respects armor and protection enchantments
- True damage is limited to 3 hearts per Soul Mark activation

### Crafting Limits
- Each legendary can be crafted once per player
- Each player can craft all legendaries, but only once each
- OPs can use `/kreset` to reset all crafting progress globally
- OPs can use `/kresetplayer <player>` to reset a specific player's crafting progress
- Reset commands do NOT delete existing legendary items
- When viewing a crafted legendary in the altar, you'll see who originally forged it

## Configuration

The plugin stores data in the `plugins/LegendaryWeaponsSMP/` folder:

- `crafting.yml` - Global crafting history (which legendaries have been crafted and by whom)
- `altars.yml` - Legendary Altar locations

## Support & Issues

If you encounter issues:
1. Check that you're running Paper 1.21.8 or compatible
2. Ensure Java 21+ is installed
3. Check server console for error messages
4. Verify you have the latest version of the plugin

## License

This plugin was created for the AlterSMP server.

---

**Enjoy your legendary weapons!** ⚔️✨
