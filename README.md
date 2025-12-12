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
- **Passive: Lunar Blessing** - Buffs based on moon phase:
  - Full Moon: Strength III
  - Gibbous: Speed I
  - Quarter: Strength I
- **Ability 1: Star Rift Slash** (25s) - 30-block beam through walls (~7 hearts through Prot IV)
- **Ability 2: Stargate Blink** (45s) - Teleport up to 45 blocks

#### Phoenix Grace
- **Passive: Heat Shield** - Immune to fire and explosions
- **Ability 1: Flame Harvest** (30s) - 8-block fire explosion, grants 3 absorption hearts
- **Ability 2: Fire Rebirth** (3min) - Survive death for 30s window, revive at 6 hearts with fire resistance

#### Thousand Demon Daggers
- **Passive: Shadow Presence** - Speed III while sneaking
- **Ability 1: Shadowstep** (20s) - Teleport behind target (15 blocks), next attack deals +1 heart true damage
- **Ability 2: Soul Mark** (60s) - Mark target for 15s, all hits deal +4 hearts true damage

#### Chrono Blade
- **Passive: Time Freeze** - Every 20th melee hit freezes target for 3s
- **Ability 1: Time Distortion** (40s) - 6-block bubble freezes enemies for 3s, then deals 4 hearts true damage
- **Ability 2: Chrono Shift** (120s) - Mark position, recast to return (clears debuffs, grants Speed II)

#### Soul Devourer
- **Passive: Soul Collector** - +1 heart damage per player kill (max 5 souls, lost on death)
- **Ability 1: Void Slice** (30s) - 10-block void crescent attack (~6 hearts + wither)
- **Ability 2: Void Rift** (85s) - Black hole for 5s that pulls and damages enemies

#### Dragonborn Blade
- **Passive: Dragon's Gaze** - Nearby enemies within 30 blocks glow
- **Passive: Heart Steal** - Steal 1 heart per player kill (max 5). Victim permanently loses 1 heart. All stolen hearts return when you die.
- **Ability 1: End Sever** (30s) - Wide 12-block purple blade arc (~2.5 hearts + Weakness + Levitation)
- **Ability 2: Dragon Dash** (120s) - 15-block dash through enemies, dealing 4 hearts + 0.5s stun each

---

### Other Weapons

#### Tempestbreaker Spear (Trident)
- **Passive: Windwalker** - Dolphin's Grace and water breathing
- **Ability 1: Gale Throw** (25s) - Wind vortex pulls enemies in (~3 hearts + levitation)
- **Ability 2: Stormcall** (50s) - 8-block cone lightning strike (~5 hearts + 2s stun)

#### Divine Axe Rhitta (Axe)
- **Passive: Nature Channel** - Regeneration III on grass/logs/leaves
- **Ability 1: Nature Grasp** (35s) - Root enemies for 3s (8-block radius)
- **Ability 2: Forest Shield** (70s) - Axe becomes Breach V weapon, shreds shields for 15s

#### Chains of Eternity (Chain Weapon)
- **Passive: Eternal Resilience** - Resistance I while holding (main or offhand)
- **Ability 1: Soul Bind** (35s) - Pull target to you (20 blocks), deals damage + Slowness V for 3s
- **Ability 2: Prison of the Damned** (65s) - Cage target in unbreakable iron bars for 5s

---

### Shield

#### Celestial Aegis Shield
- **Passive: Aura of Protection** - You and trusted allies within 5 blocks gain Resistance I (requires offhand)
- **Ability 1: Radiant Block** (40s) - Reflect 75% damage for 5s
- **Ability 2: Heaven's Wall** (90s) - 16x16 glass barrier for 32s (only trusted players can pass)

---

### Armor

#### Copper Boots
- **Passive: Featherfall** - Immune to fall damage + permanent Speed II
- **Ability: Meteor Slam** - Shift mid-air to slam down with mace-like AOE damage (4-block radius, damage scales with height)

#### Copper Chestplate
- **Passive: Lightning Storm** - Every 10 melee hits triggers lightning strike on target (~2.5 hearts through Prot IV)

#### Copper Leggings
- **Passive: Flamebound Feet** - Permanent Fire Resistance
- **Passive:** Permanent Haste II
- **Passive: Flame Trail** - Walking leaves damaging fire trails
- **Passive:** Super speed in lava

#### Copper Helmet
- **Passive: Blood Harvest** - Player kills grant +5 max hearts for 5 minutes
- **Passive: Aqua Abilities** - Permanent Conduit Power and Dolphin's Grace

---

### Tools

#### Copper Pickaxe
- **Ability 1: 3x3 Mining Toggle** - Toggle mining in a 3x3 area
- **Ability 2: Enchant Switch** - Toggle between Silk Touch and Fortune III

---

### Utility Items

#### Lantern of Lost Names (Soul Lantern - Main/Offhand)
- **Passive: Phantom Veil** - Invisible to players you haven't killed yet
- Attacking reveals you for 5 minutes
- Strategic assassin tool

#### Rift Key of the Endkeeper (Tripwire Hook)
- **Ability 1: Rift Teleport** (24h cooldown) - Teleport to any coordinates
- Type coords in chat after activating: X Y Z
- Cannot use while in combat

#### Chaos Dice of Fate (Amethyst Shard)
- **Ability 1: Roll Dice** (30min) - Random powerful effect:
  1. +5 hearts for 15 minutes
  2. Summon 5 Iron Golems for 5 minutes
  3. Speed III + Strength III for 10 minutes
  4. Scramble enemy hotbar
  5. Free player scans for 20 minutes
  6. All attacks are crits for 15 minutes
  7. Resistance II for 5 minutes
- **Ability 2: Player Scan** (10s) - Only available with effect #5, shows all player locations

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
