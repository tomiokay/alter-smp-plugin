# Configuration Guide
## Legendary Weapons SMP Plugin

This guide explains how to use the `config.yml` file to customize your legendary weapons without recompiling the plugin.

---

## Quick Start

1. **Find the config file:** `plugins/LegendaryWeaponsSMP/config.yml`
2. **Edit values** using any text editor
3. **Reload the plugin:** `/lreload` (in-game) or restart the server
4. **Test your changes!**

---

## Configuration Sections

### Global Settings

```yaml
global:
  broadcast-crafting: true    # Announce to server when legendary is crafted
  enable-particles: true      # Show particle effects for abilities
  particle-density: 1.0       # Particle multiplier (0.5 = half, 2.0 = double)
  enable-passives: true       # Toggle all passive abilities
  enable-abilities: true      # Toggle all active abilities
```

**Examples:**
- Disable broadcasts: `broadcast-crafting: false`
- Reduce particles for performance: `particle-density: 0.5`
- Disable all abilities temporarily: `enable-abilities: false`

---

### Cooldowns

Change how long abilities take to recharge (in seconds):

```yaml
cooldowns:
  blade_of_the_fractured_stars:
    ability1: 25  # Star Rift Slash
    ability2: 45  # Stargate Blink
```

**Examples:**
- Make Star Rift Slash faster: `ability1: 15`
- Make Stargate Blink slower: `ability2: 60`
- Instant abilities for testing: `ability1: 0`

---

### Damage Values

Adjust how much damage abilities deal:

```yaml
damage:
  star_rift_slash: 12.0           # Damage in half-hearts
  soul_mark_true_damage: 6.0      # TRUE DAMAGE (ignores armor)
  void_slice_damage: 5.0
```

**Examples:**
- Nerf Star Rift: `star_rift_slash: 8.0`
- Buff Soul Mark: `soul_mark_true_damage: 10.0`
- Make Void Slice weaker: `void_slice_damage: 3.0`

**Note:** Damage is in half-hearts (1.0 = half a heart, 2.0 = 1 full heart)

---

### Ranges & Durations

Control how far abilities reach and how long effects last:

```yaml
ranges:
  star_rift_range: 30               # How far beam travels
  shadowstep_distance: 8            # How far to dash
  nature_grasp_duration: 2          # How long roots last (seconds)
  flashburst_hits_required: 20      # Hits needed to trigger
```

**Examples:**
- Longer Star Rift: `star_rift_range: 50`
- Shorter Shadowstep: `shadowstep_distance: 5`
- Faster Flashburst: `flashburst_hits_required: 10`
- Longer roots: `nature_grasp_duration: 5`

---

### Crafting Recipes

**IMPORTANT:** Recipes are 5×5 grids defined row by row, left to right (25 materials total).

```yaml
recipes:
  blade_of_the_fractured_stars:
    - NETHER_STAR          # Row 1, slot 1
    - DIAMOND_BLOCK        # Row 1, slot 2
    - DIAMOND_BLOCK        # Row 1, slot 3
    - DIAMOND_BLOCK        # Row 1, slot 4
    - NETHER_STAR          # Row 1, slot 5
    - DIAMOND_BLOCK        # Row 2, slot 1
    # ... 20 more items ...
```

**How to modify recipes:**

1. Find the legendary you want to change
2. Each recipe has exactly 25 items (5 rows × 5 columns)
3. Use Minecraft material names (ALL_CAPS)
4. Use `AIR` for empty slots

**Material name examples:**
- `NETHERITE_SWORD`, `DIAMOND`, `GOLD_BLOCK`
- `NETHER_STAR`, `DRAGON_EGG`, `TOTEM_OF_UNDYING`
- `OBSIDIAN`, `CRYING_OBSIDIAN`, `SCULK`
- Full list: https://hub.spigotmc.org/javadocs/bukkit/org/bukkit/Material.html

**Example: Make Umbra Veil Dagger easier to craft**

Change expensive materials to cheaper ones:
```yaml
recipes:
  umbra_veil_dagger:
    - ENDER_PEARL
    - STONE            # Changed from OBSIDIAN
    - STONE            # Changed from OBSIDIAN
    - STONE            # Changed from OBSIDIAN
    - ENDER_PEARL
    # ... continue pattern ...
```

---

### Messages

Customize what players see:

```yaml
messages:
  already-crafted: "&cThis legendary has already been forged by {crafter}!"
  craft-broadcast: "&6&l{player} has forged {legendary}&6&l!"
  ability-on-cooldown: "&eAbility on cooldown: &c{seconds}s"
```

**Color codes:**
- `&a` = Green
- `&c` = Red
- `&e` = Yellow
- `&6` = Gold
- `&l` = Bold
- `&o` = Italic

**Placeholders:**
- `{player}` = Player name
- `{crafter}` = Who crafted the legendary
- `{legendary}` = Legendary weapon name
- `{seconds}` = Remaining cooldown

**Example: Change broadcast message**
```yaml
craft-broadcast: "&d&l⚔ {player} &dhas obtained {legendary}&d! ⚔"
```

---

### Performance

Optimize for your server's needs:

```yaml
performance:
  passive-check-interval: 10       # Ticks between passive checks (20 = 1 sec)
  ice-creation-cooldown: 500       # Milliseconds between ice placements
  void-rift-damage-interval: 10    # Ticks between damage ticks
```

**Low-end server optimization:**
```yaml
performance:
  passive-check-interval: 20      # Check passives less often
  ice-creation-cooldown: 1000     # Slower ice creation
```

**High-end server (more responsive):**
```yaml
performance:
  passive-check-interval: 5       # Check passives more often
  ice-creation-cooldown: 250      # Faster ice creation
```

---

## Common Configurations

### Easier Server (Beginner-Friendly)

```yaml
cooldowns:
  # Cut all cooldowns in half
  blade_of_the_fractured_stars:
    ability1: 12
    ability2: 22
  # ... repeat for all legendaries ...

damage:
  # Increase all damage by 50%
  star_rift_slash: 18.0
  void_slice_damage: 7.5
  # ... etc ...
```

### Harder Server (Competitive)

```yaml
cooldowns:
  # Double all cooldowns
  blade_of_the_fractured_stars:
    ability1: 50
    ability2: 90
  # ... repeat for all legendaries ...

damage:
  # Reduce all damage by 25%
  star_rift_slash: 9.0
  void_slice_damage: 3.75
  # ... etc ...
```

### Testing Server

```yaml
global:
  enable-particles: false     # Less lag during tests

cooldowns:
  # All abilities instant
  blade_of_the_fractured_stars:
    ability1: 0
    ability2: 0
  # ... repeat for all legendaries ...
```

### Economy Server (Expensive Legendaries)

Make all recipes require more expensive materials - replace common items with rare ones in the recipes section.

---

## Troubleshooting

### Config not loading?
1. Check for syntax errors (indentation must be exact)
2. Run `/lreload` after making changes
3. Check console for error messages
4. Verify material names are correct (ALL_CAPS)

### Recipe not working?
1. Make sure you have exactly 25 items
2. Verify material names exist in Minecraft 1.21.8
3. Check console for "Invalid material" warnings
4. Remember: recipes are row by row, left to right

### Changes not applying?
1. Use `/lreload` command
2. If that doesn't work, restart the server
3. Some changes (like recipes) may require reloading the altar GUI

---

## Tips

1. **Always backup** your config before making major changes
2. **Test on a local server** before applying to production
3. **Use `/lreload`** instead of restarting when possible
4. **Check console** for warnings about invalid values
5. **Balance carefully** - small changes can have big impacts

---

## Advanced: Bulk Changes

Want to change all cooldowns at once? Use find-replace:

1. Open config.yml in a text editor
2. Find: `ability1: 25` → Replace: `ability1: 15` (for all ability1 cooldowns)
3. Find: `ability2: 45` → Replace: `ability2: 30` (for all ability2 cooldowns)
4. Save and `/lreload`

---

## Support

If you need help:
- Check the console for error messages
- Verify your YAML syntax is correct
- Make sure material names are valid
- Ask in the plugin's support channel

---

**Happy configuring!** ⚙️
