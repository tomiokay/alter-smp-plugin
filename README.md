# AlterSMP Plugin

Custom Minecraft plugin with **18 legendary items**, a **combat logger**, and **PvP toggle** system. Each legendary has unique abilities and a 5x5 crafting system.

---

## Commands

### Player Commands
| Command | Description |
|---------|-------------|
| `/ability 1` | Use your legendary's first ability |
| `/ability 2` | Use your legendary's second ability |
| `/trust <player>` | Toggle trust (prevents friendly fire) |
| `/togglecooldown` | Toggle cooldown display in action bar |
| `/togglecombat` | Toggle combat timer display |

### Admin Commands (OP Only)
| Command | Description |
|---------|-------------|
| `/givealtar [player]` | Give a Legendary Altar |
| `/givelegendary <type> [player]` | Give a specific legendary item |
| `/kreset` | Reset ALL crafting progress |
| `/kresetplayer <player>` | Reset a player's crafting progress |
| `/heartreset <player\|all>` | Reset heart steal data |
| `/cooldown [player]` | Clear ability cooldowns |
| `/ldisable <id\|list>` | Disable/enable legendaries |
| `/togglepvp` | Toggle PvP on/off (broadcasts to all) |
| `/lreload` | Reload config |

---

## Legendary Items

### Swords

#### Holy Moonlight Sword
- **Passive: Flashburst Counter** - Every 20 hits blinds nearby enemies
- **Ability 1: Star Rift Slash** (25s) - 30-block beam through walls
- **Ability 2: Stargate Blink** (45s) - Teleport up to 45 blocks

#### Phoenix Grace
- **Passive: Heat Shield** - Immune to fire and explosions
- **Ability 1: Flame Harvest** (90s) - Deal 40% HP damage to nearby enemies, gain absorption per hit
- **Ability 2: Fire Rebirth** (300s) - Cheat death for 10 seconds

#### Thousand Demon Daggers
- **Passive: Shadow Presence** - Speed III while sneaking
- **Ability 1: Shadowstep** (20s) - Teleport behind target, next attack deals +1 heart true damage
- **Ability 2: Soul Mark** (60s) - Mark target for +4 hearts true damage per hit for 15 seconds

#### Chrono Blade
- **Passive: Time Slow** - First hit on each enemy slows them
- **Ability 1: Time Distortion** (40s) - 6-block bubble freezes enemies for 3s, then deals 4 hearts true damage
- **Ability 2: Chrono Shift** (120s) - Mark position, re-cast to return (clears debuffs, grants Speed II)

#### Soul Devourer
- **Passive: Soul Collector** - +1 heart damage per player kill (max 5 souls, lost on death)
- **Ability 1: Void Slice** (30s) - 10-block void crescent attack with wither
- **Ability 2: Void Rift** (85s) - Black hole for 5s that pulls and damages enemies

#### Dragonborn Blade
- **Passive: Dragon's Gaze** - Nearby enemies within 30 blocks glow
- **Passive: Heart Steal** - Steal 1 heart per player kill (max 5). Victim permanently loses 1 heart. All stolen hearts return when you die.
- **Ability 1: End Sever** (30s) - 12-block sweeping arc with Weakness and Levitation
- **Ability 2: Dragon Dash** (120s) - Dash 15 blocks through enemies, dealing 4 hearts + stun

---

### Other Weapons

#### Tempestbreaker Spear (Trident)
- **Passive: Storm's Fury** - Hits strike lightning (1 heart damage)
- **Ability 1: Gale Throw** (25s) - Next throw creates wind vortex
- **Ability 2: Stormcall** (50s) - 8-block lightning storm for 2 seconds

#### Divine Axe Rhitta (Axe)
- **Passive: Nature Channel** - Regeneration III on grass/logs/leaves
- **Ability 1: Nature Grasp** (35s) - Root enemies in 6-block radius
- **Ability 2: Verdant Cyclone** (70s) - 360 spin attack with knockback

#### Chains of Eternity (Chain Weapon)
- **Passive: Soul Links** - Every 5th hit immobilizes target
- **Ability 1: Soul Bind** (35s) - Pull target, deal damage, and slow
- **Ability 2: Prison of the Damned** (65s) - Cage target in iron bars for 5 seconds

---

### Shield

#### Celestial Aegis Shield
- **Passive: Aura of Protection** - You and trusted allies within 5 blocks gain Resistance I
- **Ability 1: Radiant Block** (40s) - Reflect 75% damage for 5 seconds
- **Ability 2: Heaven's Wall** (90s) - Summon 16x16 barrier for 32s (only trusted players can pass)

---

### Armor

#### Copper Boots
- **Passive: Featherfall** - No fall damage + permanent Speed II
- **Ability: Meteor Slam** - Shift mid-air to slam down with mace-like AOE damage

#### Copper Chestplate
- **Passive: Storm Strike** - Every 10 melee hits triggers lightning storm (~2.5 hearts through Prot IV)

#### Copper Leggings
- **Passive: Flamebound Feet** - Immune to fire, lava, and magma damage
- **Passive: Flame Trail** - Walking leaves damaging flame trails
- **Passive:** Haste I when above 50% HP
- **Passive:** Super speed in lava

#### Copper Helmet
- **Passive: Blood Harvest** - Player kills grant +5 hearts for 5 minutes
- **Passive: Critical Rush** - Critical hits grant Speed I for 3 seconds
- **Passive: Water Mobility** - Dolphin's Grace + Conduit Power

---

### Tools

#### Copper Pickaxe
- **Ability 1: 3x3 Mining Toggle** - Toggle mining in a 3x3 area
- **Ability 2: Enchant Switch** - Toggle between Silk Touch and Fortune III

---

### Utility Items

#### Lantern of Lost Names (Soul Lantern - Offhand)
- **Passive: Phantom Veil** - Invisible to players you've never killed
- They cannot see you until you kill them once
- Deactivates for 5 minutes after attacking

#### Rift Key of the Endkeeper (Tripwire Hook)
- **Ability: End Rift** (24h cooldown) - Open a portal to ANY coordinates
- Rift stays open for 30 seconds
- Usage: `/ability 1 <x> <y> <z>`

#### Chaos Dice of Fate (Amethyst Shard)
- **Ability 1: Roll Dice** (30min cooldown) - Random effect:
  - +5 hearts for 15 minutes
  - Summon 5 iron golems
  - Speed III + Strength III for 10 minutes
  - Jumble opponent's hotbar
  - Free player scans for 20 minutes
  - Insta-crit for 15 minutes
  - Resistance II for 5 minutes
- **Ability 2: Player Scan** (10s cooldown) - Shows all player locations

---

## Combat System

### Combat Logger
- **30 second combat tag** when you attack or get attacked by a player
- **Logging out in combat = death** (your items drop)
- **Blocked in combat:**
  - Ender pearls
  - Elytra
  - Riptide (15s cooldown in combat)
- **Mace:** 60 second cooldown between attacks, cannot be enchanted

### PvP Toggle
- Admins can use `/togglepvp` to enable/disable PvP
- Broadcasts a big title to all players
- When off, players cannot damage each other

---

## Crafting System

1. Get a **Legendary Altar** from an admin (`/givealtar`)
2. Place the altar and right-click to open the **5x5 crafting grid**
3. Each legendary has a unique recipe
4. **Each player can only craft each legendary ONCE**
5. Admins can reset progress with `/kreset` or `/kresetplayer`

---

## Heart Steal System (Dragonborn Blade)

- Kill a player with Dragonborn Blade equipped = steal 1 of their max hearts
- You gain +1 max heart (up to +5 hearts total)
- The victim permanently loses 1 max heart
- **When you die, ALL stolen hearts return to their original owners** (even if offline)
- Admins can reset with `/heartreset <player|all>`

---

## Trust System

- Use `/trust <player>` to toggle trust
- Trusted players won't be hit by your abilities
- Trusted players can pass through Heaven's Wall
- Trusted players receive Resistance I from Celestial Aegis Shield

---

## Requirements

- **Minecraft:** 1.21.4+
- **Server:** Paper (recommended)
- **Java:** 21+

---

## Installation

1. Build with `gradlew.bat shadowJar` (Windows) or `./gradlew shadowJar` (Mac/Linux)
2. Copy JARs from `legendary-weapons/build/libs/` and `combat-logger/build/libs/` to your server's `plugins/` folder
3. Restart server

---

**Made for AlterSMP**
