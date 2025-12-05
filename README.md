# Legendary Weapons SMP Plugin

**Epic Minecraft plugin with 18 legendary items, each featuring devastating abilities, spectacular particle effects, and a unique 5×5 crafting system. Balanced for PvP with Protection IV armor in mind.**

Transform your SMP with powerful legendary items that players will fight to obtain. Each legendary has unique passive and/or active abilities, with enhanced visuals that make every battle epic.

## Features

- **18 Unique Legendary Items** - 6 swords, 1 shield, 1 pickaxe, 1 axe, 1 trident, 1 chain weapon, 4 armor pieces, 3 utility items
- **Custom 5×5 Crafting System** - Separate from vanilla crafting via Legendary Altars
- **One Craft Per Player** - Each legendary can only be crafted once per player
- **Ability System** - Active abilities via `/ability 1` and `/ability 2` with cooldowns
- **Passive Armor Abilities** - Unique passive effects for legendary armor pieces
- **Trust System** - Prevent friendly fire with `/trust` command
- **Global Reset Command** - OPs can reset all crafting progress
- **Persistent Data** - Crafting history, cooldowns, and altar locations are saved

## Requirements

- **Minecraft Version:** 1.21.4+
- **Server Software:** Paper (recommended) or Spigot-compatible
- **Java Version:** JDK 21+

## Installation

1. **Build the Plugin:**
   ```bash
   ./gradlew shadowJar
   ```
   Or on Windows:
   ```bash
   gradlew.bat shadowJar
   ```

2. **Locate the JAR:**
   - The compiled JAR will be in `build/libs/`
   - Look for `LegendaryWeaponsSMP-1.0.0.jar`

3. **Install on Server:**
   - Copy the JAR file to your Paper server's `plugins/` folder
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
   - Each legendary can only be crafted once per player

### For Players

1. **Using the Altar:**
   - Right-click a placed Legendary Altar to open the 5×5 crafting menu
   - Place ingredients according to legendary recipes
   - If the pattern matches, the legendary item appears in the output slot
   - Take the item to complete crafting (only works once per legendary per player)
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
| `/givelegendary <type> [player]` | `legendaryweapons.givelegendary` (OP) | Gives a specific legendary item |
| `/trust <player>` | None | Toggle trust with a player (prevents friendly fire) |
| `/lreload` | `legendaryweapons.reload` (OP) | Reloads plugin configuration |
| `/cooldown` | None | Check your current ability cooldowns |

## Legendary Items Overview

### Swords

#### Holy Moonlight Sword (Diamond Sword)
- **Passive:** Flashburst Counter - Every 20 hits blinds nearby enemies
- **Ability 1:** Star Rift Slash (25s) - 30-block beam through walls
- **Ability 2:** Stargate Blink (45s) - Teleport up to 45 blocks

#### Pheonix Grace (Diamond Sword)
- **Passive:** Heat Shield - Immune to fire and explosions
- **Ability 1:** Flame Harvest (90s) - Deal 40% HP damage to nearby enemies, gain absorption per hit
- **Ability 2:** Fire Rebirth (300s) - Cheat death for 10 seconds

#### Thousand Demon Daggers (Diamond Sword)
- **Passive:** Shadow Presence - Speed III while sneaking
- **Ability 1:** Shadowstep (20s) - Teleport behind target, next attack deals +1 heart true damage
- **Ability 2:** Soul Mark (60s) - Mark target for +4 hearts true damage per hit for 15 seconds

#### Chrono Blade (Diamond Sword)
- **Passive:** Time Slow - First hit on each enemy slows them
- **Ability 1:** Time Distortion (40s) - 6-block bubble freezes enemies for 3s, then deals 4 hearts true damage
- **Ability 2:** Chrono Shift (120s) - Mark position, re-cast to return (clears debuffs, grants Speed II)

#### Soul Devourer (Diamond Sword)
- **Passive:** Soul Collector - +2 damage per player kill (max 5 souls, displayed on item)
- **Ability 1:** Void Slice (30s) - 8-block horizontal purple arc
- **Ability 2:** Void Rift (85s) - Black hole that pulls and damages enemies

#### Voidrender (Diamond Sword)
- **Passive:** Dragon's Gaze - Nearby enemies within 8 blocks glow
- **Ability 1:** End Sever (18s) - 7-block cone dealing 2 hearts true damage with blindness
- **Ability 2:** Genesis Collapse (120s) - 10-block explosion dealing 5 hearts true damage

### Other Weapons

#### Tempestbreaker Spear (Trident)
- **Passive:** Storm's Fury - Trident hits strike lightning (1 heart damage)
- **Ability 1:** Gale Throw (25s) - Next throw creates wind vortex
- **Ability 2:** Stormcall (50s) - 8-block lightning storm for 2 seconds

#### Divine Axe Rhitta (Diamond Axe)
- **Passive:** Nature Channel - Regeneration III on grass/logs/leaves
- **Ability 1:** Nature Grasp (35s) - Root enemies in 6-block radius
- **Ability 2:** Verdant Cyclone (70s) - 360° spin attack with knockback

#### Chains of Eternity (Wooden Shovel)
- **Passive:** Soul Links - Every 5th hit immobilizes target
- **Ability 1:** Soul Bind (35s) - Pull target, deal damage, and slow
- **Ability 2:** Prison of the Damned (65s) - Cage target in iron bars for 5 seconds

### Shield

#### Celestial Aegis Shield (Shield)
- **Passive:** Aura of Protection - You and trusted allies within 5 blocks gain Resistance I
- **Ability 1:** Radiant Block (40s) - Reflect 75% damage for 5 seconds
- **Ability 2:** Heaven's Wall (90s) - Summon 16x16 barrier for 32s (only trusted players can pass)

### Armor

#### Copper Boots (Diamond Boots)
- **Passive:** Featherfall - No fall damage + permanent Speed II
- **Ability:** Meteor Slam - Shift mid-air to slam down with mace-like damage in 4-block radius

#### Copper Chestplate (Diamond Chestplate)
- **Passive:** Storm Strike - Every 10 melee hits triggers lightning storm on target (deals ~2.5 hearts through Protection IV)

#### Copper Leggings (Diamond Leggings)
- **Passive:** Flamebound Feet - Immune to fire, lava, and magma damage
- **Passive:** Flame Trail - Walking leaves damaging flame trails
- **Passive:** Haste I when above 50% HP
- **Passive:** Super speed in lava

#### Copper Helmet (Diamond Helmet)
- **Passive:** Blood Harvest - Player kills grant +5 hearts for 5 minutes
- **Passive:** Critical Rush - Critical hits grant Speed I for 3 seconds
- **Passive:** Water Mobility - Dolphin's Grace + Conduit Power

### Tools

#### Copper Pickaxe (Netherite Pickaxe)
- **Passive:** None
- **Ability 1:** 3x3 Mining Toggle - Toggle mining in a 3x3 area
- **Ability 2:** Enchant Switch - Toggle between Silk Touch and Fortune III

### Utility Items

#### Lantern of Lost Names (Soul Lantern)
- **Passive:** Phantom Veil - Invisible to players you've never killed
- They cannot see you until you kill them once
- Deactivates for 5 minutes after attacking
- **Hold in offhand for effect**

#### Rift Key of the Endkeeper (Tripwire Hook)
- **Ability:** End Rift (24h cooldown) - Open a portal to ANY coordinates
- Rift stays open for 30 seconds
- Teammates can follow through
- **Usage:** `/ability 1 <x> <y> <z>`

#### Chaos Dice of Fate (Amethyst Shard)
- **Ability 1:** Roll Dice (30min cooldown) - Random effect:
  - +5 hearts for 15 minutes
  - Summon 5 iron golems
  - Speed III + Strength III for 10 minutes
  - Jumble opponent's hotbar
  - Player tracker for 20 minutes
  - Insta-crit for 15 minutes
  - Resistance II for 5 minutes
- **Ability 2:** Player Scan (10s cooldown) - Shows all player locations with coordinates and distance

## Important Notes

### Normal Crafting Table
- **The vanilla 3×3 crafting table is completely unaffected by this plugin**
- Normal crafting tables continue to work as usual
- Only Legendary Altars open the special 5×5 crafting menu

### True Damage
- Several abilities deal true damage (ignores armor):
  - Thousand Demon Daggers: Shadowstep bonus (+1 heart), Soul Mark (+4 hearts/hit)
  - Chrono Blade: Time Distortion (4 hearts)
  - Creation Splitter: End Sever (2 hearts), Genesis Collapse (5 hearts)

### Crafting Limits
- Each legendary can be crafted once per player
- Each player can craft all legendaries, but only once each
- OPs can use `/kreset` to reset all crafting progress globally
- OPs can use `/kresetplayer <player>` to reset a specific player's crafting progress
- Reset commands do NOT delete existing legendary items
- When viewing a crafted legendary in the altar, you'll see who originally forged it

## Configuration

The plugin is highly configurable via `config.yml`:
- Ability cooldowns
- Damage values
- Range and duration settings
- Crafting recipes (5×5 patterns)
- Messages

Data is stored in the `plugins/LegendaryWeaponsSMP/` folder:
- `config.yml` - All configurable settings and recipes
- `crafting.yml` - Global crafting history (which legendaries have been crafted and by whom)
- `altars.yml` - Legendary Altar locations

## Support & Issues

If you encounter issues:
1. Check that you're running Paper 1.21.4+ or compatible
2. Ensure Java 21+ is installed
3. Check server console for error messages
4. Verify you have the latest version of the plugin

## License

This plugin was created for the AlterSMP server.

---

**Enjoy your legendary weapons!**
