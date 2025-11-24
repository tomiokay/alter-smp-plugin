# Product Requirements Document (PRD)
## Legendary Weapons SMP Plugin

---

## 1. Overview

The Legendary Weapons SMP plugin introduces 11 unique, powerful legendary items to a Minecraft 1.21.8 Paper/Spigot server. Each legendary weapon features:
- One passive ability (always active when equipped)
- Two active abilities (triggered via commands)
- Per-player crafting limits (one craft per player per legendary)
- A custom 5×5 crafting system using Legendary Altars

The plugin enhances gameplay by adding powerful endgame items that require rare resources and strategic use of abilities with cooldowns.

---

## 2. Core Systems

### 2.1 Legendary Altar System

**Purpose:** Provide a separate crafting interface for legendary items without interfering with vanilla crafting.

**Requirements:**
- Legendary Altar is a special block obtained only via OP command `/givealtar`
- When placed, the block is registered in persistent storage
- Right-clicking the altar opens a custom 5×5 crafting GUI
- Normal crafting tables remain completely unaffected
- Breaking an altar returns the altar item and unregisters the location

**Technical Details:**
- Altar item is tagged with PersistentDataContainer
- Placed altars are tracked by location in `altars.yml`
- GUI consists of a 6-row (54-slot) inventory
- 25 slots form the 5×5 crafting grid
- 1 slot displays the crafting result
- Remaining slots are filled with decorative glass panes

### 2.2 Crafting System

**Purpose:** Allow players to craft legendary weapons using complex 5×5 recipes.

**Requirements:**
- Each legendary has a unique 5×5 recipe requiring rare materials
- Recipes must match exactly (all 25 slots with correct items)
- Players can only craft each legendary once
- Attempting to craft an already-crafted legendary shows a "locked" indicator
- Taking the result consumes all ingredients and marks the legendary as crafted

**Recipe Validation:**
- Pattern matching checks all 25 slots against defined recipes
- Air slots must be empty; material slots must match exactly
- Result updates dynamically as items are placed/removed

### 2.3 Per-Player Crafting History

**Purpose:** Ensure each legendary can only be obtained once per player through crafting.

**Requirements:**
- Each player has a persistent record of crafted legendaries
- Data is stored in `crafting.yml` indexed by UUID
- Crafting attempt checks history before allowing completion
- `/kreset` command clears all crafting history for all players

**Technical Details:**
- Map structure: `UUID -> Set<String>` (legendary IDs)
- History is loaded on plugin enable
- History is saved on plugin disable and after each craft

### 2.4 Ability System

**Purpose:** Provide active and passive abilities for each legendary weapon.

**Requirements:**
- Two active abilities per legendary (activated via `/ability 1` and `/ability 2`)
- One passive ability per legendary (always active when equipped)
- Per-player, per-ability cooldown tracking
- Cooldown display shows remaining seconds
- Abilities only work when the legendary is properly equipped

**Equipment Slots:**
- Main hand: Swords, axe, shovel, trident
- Boots: Glacierbound Halberd
- Offhand: Celestial Aegis Shield

**Technical Details:**
- Cooldowns tracked via `UUID -> (LegendaryID + AbilityNum) -> EndTime`
- Passive effects checked every 10 ticks (0.5 seconds)
- Combat-based passives use event handlers

---

## 3. Legendary Weapons Specifications

### 3.1 Blade of the Fractured Stars
**Type:** Netherite Sword

**Passive - Flashburst Counter:**
- Track successful hits on the wielder
- Every 20th hit triggers a flashburst effect
- All enemies within 5 blocks receive Blindness (0.5s) and Nausea
- Plays enderman scream sound
- Counter resets after triggering

**Ability 1 - Star Rift Slash (25s cooldown):**
- Shoots a particle beam in the direction player is looking
- Beam travels 30 blocks and passes through walls
- Damages all entities along the line for 6 hearts (normal damage)
- Displays END_ROD particles

**Ability 2 - Stargate Blink (45s cooldown):**
- Teleports player to first safe block along line of sight
- Maximum range: 45 blocks
- Spawns portal particles at origin and destination
- Finds safe landing location if destination is inside a wall

---

### 3.2 Emberheart Scythe
**Type:** Netherite Sword

**Passive - Heat Shield:**
- Complete immunity to fire damage (fire, lava, fire tick, hot floor)
- Complete immunity to explosion damage
- Passively active while held

**Ability 1 - Flame Harvest (30s cooldown):**
- Creates a flame explosion in 6-block radius
- Sets enemies on fire
- Calculates 20% of each enemy's current HP
- Grants summed value as Absorption hearts to the wielder

**Ability 2 - Fire Rebirth (180s cooldown):**
- Activates a 10-second protective state
- If player would die during this time:
  - Death is cancelled
  - Player is restored to 6 hearts
  - Fiery explosion damages and knocks back nearby enemies
  - Grants temporary fire resistance

---

### 3.3 Tempestbreaker Spear
**Type:** Trident

**Passive - Windwalker:**
- Constant Dolphin's Grace effect while held
- Constant Conduit Power effect while held
- Enhances underwater mobility

**Ability 1 - Gale Throw (25s cooldown):**
- Next thrown trident creates a wind vortex at impact location
- Vortex pulls nearby entities toward the center
- Wind particle effects
- Applies pull velocity to entities in range

**Ability 2 - Stormcall (50s cooldown):**
- Creates a cone of "weak" lightning strikes in front of player
- 15 lightning effects spread in a forward cone pattern
- Entities hit take 4 hearts of damage
- Applies Slowness IV (3s) and Blindness (1s) as a "stun" effect

---

### 3.4 Umbra Veil Dagger
**Type:** Netherite Sword

**Passive - Shadow Presence:**
- Speed III effect while sneaking with dagger equipped
- Effect is removed when not sneaking

**Ability 1 - Shadowstep (20s cooldown):**
- Dashes player 8 blocks forward in direction they're facing
- Stops early if wall is detected via raytrace
- Grants Invisibility for 4 seconds
- Invisibility is broken when player attacks

**Ability 2 - Soul Mark (60s cooldown):**
- Activates marking mode
- Next entity hit by the player is "marked"
- 15 seconds after marking, target takes 3 hearts of **true damage**
- True damage ignores armor and protection enchantments
- **This is the ONLY ability in the plugin that uses true damage**

---

### 3.5 Heartroot Guardian Axe
**Type:** Netherite Axe

**Passive - Nature Channel:**
- Checks block beneath player every 0.5 seconds
- If standing on grass blocks, logs, or leaves:
  - Grants Regeneration II

**Ability 1 - Nature Grasp (35s cooldown):**
- Creates root effects in 6-block radius
- All enemies in radius cannot move for 2 seconds
- Implemented via Slowness 255 + Jump Boost 128
- Villager happy particles to represent roots

**Ability 2 - Forest Shield (70s cooldown):**
- For 10 seconds, the axe gains "Breach III" properties:
  - Increased knockback
  - Increased damage to shields
  - Can disable shields

---

### 3.6 Chains of Eternity
**Type:** Wooden Shovel

**Passive - Soul Links:**
- Counts successful hits on enemies
- Every 5th hit triggers soul link effect:
  - Target is immobilized for 1.5 seconds
  - Soul particles spawn around target
  - Counter resets

**Ability 1 - Soul Bind (35s cooldown):**
- Raytraces to find targeted enemy (up to 20 blocks)
- Pulls target toward the player
- Applies strong Slowness effect
- Soul particle trail

**Ability 2 - Prison of the Damned (65s cooldown):**
- Spawns iron bars forming a 3×3 cage around target
- Cage has walls 4 blocks high
- Cage persists for 5 seconds then automatically removes
- Target is trapped inside the cage

---

### 3.7 Glacierbound Halberd
**Type:** Netherite Boots

**Passive - Frozen Path:**
- Water blocks beneath player turn to ice
- Ice is permanent (or very long-lasting)
- Cooldown on ice creation prevents lag (500ms between placements)

**Ability 1 - Frostbite Sweep (28s cooldown):**
- Creates a forward cone of icy particles
- Enemies in cone are frozen for 1-3 seconds
- Freezing implemented via Slowness + Freeze ticks
- Duration can scale based on hits or enemy count

**Ability 2 - Winter's Embrace (75s cooldown):**
- Creates a frost dome with 7-block radius around player
- Dome lasts for 5 seconds
- Enemies inside receive Slowness
- Player receives gradual healing (Regeneration)
- Dome visualized with snowflake particles

---

### 3.8 Celestial Aegis Shield
**Type:** Shield

**Passive - Aura of Protection:**
- All players within 5 blocks gain Resistance I
- Effect refreshed every 0.5 seconds
- Must be held in offhand

**Ability 1 - Radiant Block (40s cooldown):**
- For 5 seconds, activates reflection mode
- When damage is successfully blocked with the shield:
  - 75% of blocked damage is reflected back to attacker
  - Attacker takes the reflected damage

**Ability 2 - Heaven's Wall (90s cooldown):**
- Summons a glowing particle barrier around player
- Barrier lasts 6 seconds
- Projectiles crossing barrier are cancelled
- Melee damage across barrier is reduced or cancelled
- Visualized with END_ROD particles in a cylinder

---

### 3.9 Chrono Edge (Time-Splitter)
**Type:** Netherite Sword

**Passive - Last Second:**
- Checks player HP every 0.5 seconds
- If HP is 1.5 hearts or lower:
  - Grants Speed II
  - Grants Resistance I

**Ability 1 - Echo Strike (40s cooldown):**
- Active for 6 seconds
- Every time player hits an enemy:
  - After 1 second, the same damage is repeated to that target
  - Target must still be valid and alive
  - Critical particles on echo hit

**Ability 2 - Time Rewind (120s cooldown):**
- Saves player's current:
  - Position
  - Health
  - Hunger
- After 5 seconds:
  - Restores all saved values
  - Reverse portal particles
  - Teleportation sound

---

### 3.10 Oblivion Harvester
**Type:** Netherite Sword

**Passive - Soul Collector:**
- Killing mobs with this weapon increases soul counter
- Counter is stored in item's PersistentDataContainer
- Each soul grants +0.2 damage
- Maximum 20 souls (for +4 damage total)
- Soul count displayed in item lore

**Ability 1 - Void Slice (30s cooldown):**
- Sweeping void slash in front of player
- 6 blocks forward with wide horizontal sweep
- Deals 2.5 hearts normal damage to each enemy hit
- Smoke particles for void effect

**Ability 2 - Void Rift (85s cooldown):**
- Creates a black hole at target location (raytraced up to 20 blocks)
- Black hole lasts 2 seconds
- Pulls nearby entities (8-block radius) toward center
- Entities within 2 blocks of center take 3 damage every 0.5 seconds
- Visualized with portal and smoke particles

---

### 3.11 Eclipse Devourer
**Type:** Netherite Sword

**Passive - Dragon's Gaze:**
- All players within 8 blocks receive Glowing effect
- When wielder drinks Dragon's Breath:
  - Grants 2 Absorption hearts for 10 seconds

**Ability 1 - Void Rupture (35s cooldown):**
- Fires a void arc in the direction player is looking
- Arc travels 35 blocks and passes through walls
- Deals 3-4 hearts normal damage to entities hit
- Applies Blindness (~1.5 seconds)
- Reverse portal particles

**Ability 2 - Cataclysm Pulse (95s cooldown):**
- Creates a dark pulse in 7-block radius
- Phase 1 (immediate):
  - Pulls enemies inward
  - Applies Slowness IV (4s)
  - Applies Darkness effect (3s)
- Phase 2 (after 2 seconds):
  - Massive explosion at center
  - Deals 4 hearts normal damage to all enemies in range
  - Explosion particles and sound

---

## 4. Data Persistence

### 4.1 Crafting History
**File:** `plugins/LegendaryWeaponsSMP/crafting.yml`

**Structure:**
```yaml
players:
  <UUID>:
    crafted:
      - blade_of_the_fractured_stars
      - umbra_veil_dagger
```

**Operations:**
- Load on plugin enable
- Save on plugin disable
- Save immediately after each craft

### 4.2 Altar Locations
**File:** `plugins/LegendaryWeaponsSMP/altars.yml`

**Structure:**
```yaml
altars:
  altar_0:
    world: world
    x: 100
    y: 64
    z: -200
  altar_1:
    world: world_nether
    x: 50
    y: 100
    z: -50
```

**Operations:**
- Load on plugin enable
- Save on plugin disable
- Save immediately after placement/breaking

---

## 5. Technical Constraints

### 5.1 Damage Types
- All damage MUST respect armor and protection, except:
  - **Umbra Veil Dagger's Soul Mark** (3 hearts true damage only)
- True damage is implemented by directly modifying entity health

### 5.2 Vanilla Compatibility
- Normal 3×3 crafting tables are completely unaffected
- No interception of vanilla crafting events
- Legendary crafting is only possible via Legendary Altars

### 5.3 Performance
- Passive effect task runs every 10 ticks (0.5 seconds)
- Particle effects are optimized to avoid excessive spawning
- Ice creation has 500ms cooldown per player to prevent lag
- Void Rift runs every 10 ticks instead of every tick

---

## 6. Command Reference

| Command | Permission | Usage |
|---------|-----------|-------|
| `/givealtar` | `legendaryweapons.givealtar` | `/givealtar [player]` |
| `/ability` | None | `/ability <1\|2>` |
| `/kreset` | `legendaryweapons.kreset` | `/kreset` |

---

## 7. Future Considerations

See `TODOS.md` for planned enhancements and potential features.

---

**End of PRD**
