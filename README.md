# Legendary Weapons SMP Plugin

A comprehensive Minecraft plugin featuring 11 unique legendary weapons with passive and active abilities, a custom 5×5 crafting system, and global crafting limits (one per world).

## Features

- **11 Unique Legendary Weapons** - Each with distinct passive and active abilities
- **Custom 5×5 Crafting System** - Separate from vanilla crafting via Legendary Altars
- **Global Crafting Limits** - Each legendary can only be crafted once per world (by any player)
- **Ability System** - 22 unique active abilities activated via `/ability 1` and `/ability 2`
- **Global Reset Command** - OPs can reset all crafting progress with `/kreset`
- **Persistent Data** - Global crafting history and altar locations are saved

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
| `/kreset` | `legendaryweapons.kreset` (OP) | Resets all legendary crafting progress globally (allows all legendaries to be crafted again) |

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

### ❄️ Glacierbound Halberd (Boots)
- **Passive:** Frozen Path - Water freezes beneath you
- **Ability 1:** Frostbite Sweep (28s) - Cone that freezes enemies
- **Ability 2:** Winter's Embrace (75s) - Frost dome for protection

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
- Each legendary weapon can be crafted only once per world (by any player)
- Once a legendary is crafted, no other player can craft it until reset
- OPs can use `/kreset` to reset this limit, allowing all legendaries to be crafted again
- The `/kreset` command does NOT delete existing legendary items
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
