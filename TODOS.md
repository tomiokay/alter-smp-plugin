# Future Development & TODOs
## Legendary Weapons SMP Plugin

This document outlines potential features, improvements, and enhancements for future versions of the Legendary Weapons SMP plugin.

---

## High Priority

### Configuration System
**Description:** Add a config.yml to make values configurable without recompiling

**Features:**
- Cooldown times for each ability
- Damage values for abilities
- Passive effect strengths
- Particle density
- Range values (e.g., aura radius, dash distance)
- Enable/disable individual legendaries

**Benefit:** Server admins can balance legendaries for their specific server needs

**Implementation Notes:**
- Use Bukkit ConfigurationSection
- Add config validation on load
- Provide in-game command to reload config

---

### Multi-Language Support
**Description:** Support multiple languages for messages

**Features:**
- messages_en.yml, messages_es.yml, etc.
- Auto-detect player locale
- Admin-configurable default language
- In-game language switcher command

**Languages to Support:**
- English (en)
- Spanish (es)
- French (fr)
- German (de)
- Portuguese (pt_BR)

**Benefit:** Makes plugin accessible to international communities

---

### Balance Adjustments
**Description:** Fine-tune legendary abilities based on playtesting

**Areas to Review:**
- Eclipse Devourer may be too powerful with both Void Rupture and Cataclysm Pulse
- Soul Mark true damage might need adjustment (currently 3 hearts)
- Fire Rebirth cooldown (180s) may be too short for a death-prevention ability
- Oblivion Harvester soul collection might be too slow (max 20 kills for +4 damage)

**Approach:**
- Gather player feedback
- Monitor usage statistics
- A/B test different values
- Consider server difficulty (easy/normal/hard) scaling

---

## Medium Priority

### Ability Cooldown Display
**Description:** Visual cooldown indicators

**Features:**
- Boss bar showing active cooldowns
- Action bar messages for cooldown expiration
- Configurable notification style
- Sound effects when cooldown ends

**Implementation:**
- Use BossBar API for visual tracking
- Send action bar messages at regular intervals
- Play subtle sound (e.g., experience orb pickup) when ready

---

### Recipe Discovery System
**Description:** Players must discover recipes before crafting

**Features:**
- Recipe book GUI showing discovered recipes
- Unlock recipes through achievements or quests
- Hint system for undiscovered recipes
- Progression tracking

**Benefits:**
- Adds exploration and discovery element
- Prevents spoilers from external wikis
- Creates sense of achievement

---

### Legendary Upgrade System
**Description:** Enhance legendaries beyond their base form

**Features:**
- Upgrade tiers (I, II, III)
- Enhanced stats at higher tiers
- Reduced cooldowns
- Visual enhancements (more particles, glowing effects)
- Upgrade recipes using rare materials

**Example:**
- Blade of Fractured Stars II: Star Rift Slash cooldown reduced to 20s
- Blade of Fractured Stars III: Star Rift Slash cooldown 15s + wider beam

---

### Crafting Animation & Effects
**Description:** Make legendary crafting feel more epic

**Features:**
- Lightning strikes altar when legendary is completed
- Beam of light shoots into sky
- Dramatic sound effects
- Title screen announcement to player
- Server-wide message (optional, configurable)

**Benefit:** Makes crafting feel like a significant achievement

---

## Low Priority

### Statistics Tracking
**Description:** Track player legendary usage

**Features:**
- Total ability uses per legendary
- Total damage dealt with each legendary
- Total kills with each legendary
- Most-used legendary per player
- Leaderboards (/legendary stats)

**Storage:**
- SQLite database or stats.yml
- Per-player, per-legendary tracking

---

### Legendary Durability System
**Description:** Legendaries require maintenance to remain powerful

**Features:**
- Special durability that decreases with use
- Repair using rare materials at altar
- Visual indication of durability (item lore)
- Legendaries become weaker at low durability
- Cannot break completely (minimum 1% power)

**Benefits:**
- Creates item sink for rare materials
- Adds maintenance gameplay element
- Prevents weapon hoarding

---

### Particle Customization
**Description:** Let admins customize particle effects

**Config Options:**
```yaml
particles:
  star_rift_slash:
    type: END_ROD
    density: 3
    speed: 0.1
  void_rupture:
    type: REVERSE_PORTAL
    density: 5
    speed: 0.2
```

**Benefit:**
- Performance tuning on lower-end servers
- Aesthetic customization
- Accessibility (reduce particles for players with motion sensitivity)

---

### Legendary Trading/Transfer System
**Description:** Allow or restrict legendary trading

**Features:**
- Config option to enable/disable trading
- Bind legendaries to players (soulbound)
- Transfer ownership via special ritual at altar
- Transfer costs (e.g., experience levels, rare materials)

**Benefits:**
- Prevent legendary duplication exploits
- Control legendary distribution on server
- Add value to legendaries (can't just give to alt accounts)

---

## Technical Improvements

### Async Data Saving
**Description:** Move file I/O to async threads

**Benefits:**
- Prevents server lag during saves
- Better performance with many players
- Non-blocking shutdown process

**Implementation:**
- Use BukkitRunnable.runTaskAsynchronously()
- Queue data changes, batch save
- Ensure thread safety

---

### Database Support
**Description:** Support MySQL/PostgreSQL for data storage

**Features:**
- Optional database instead of YAML files
- Better performance with large player bases
- Multi-server support (shared database)
- Easier data migration and backups

**Benefits:**
- Scales better than flat files
- Allows cross-server legendary tracking
- Professional server networks can centralize data

---

### API for Other Plugins
**Description:** Provide API for third-party plugin integration

**Features:**
- Check if player has crafted specific legendary
- Get player's equipped legendary
- Programmatically trigger abilities
- Listen to legendary events (craft, use ability, etc.)

**Example Use Cases:**
- Quest plugins can require crafting a legendary
- Economy plugins can add legendary trading
- Combat plugins can detect legendary usage

---

### Unit Tests
**Description:** Add JUnit tests for core systems

**Test Coverage:**
- Recipe matching logic
- Cooldown manager
- Data serialization/deserialization
- Crafting history tracking

**Benefits:**
- Catch bugs early
- Safe refactoring
- Confidence in updates

---

## Creative Additions

### Legendary Sets
**Description:** Bonuses for wearing multiple legendaries from same "set"

**Example Sets:**
- Void Set: Umbra Veil Dagger + Oblivion Harvester + Eclipse Devourer
  - Set Bonus: Increased void damage, night vision
- Elemental Set: Emberheart Scythe + Tempestbreaker Spear + Glacierbound Halberd
  - Set Bonus: Elemental resistance, environmental damage immunity

---

### Legendary Pets/Companions
**Description:** Summon legendary creatures as companions

**Features:**
- Summon dragon for Eclipse Devourer
- Summon spirit wolf for Chains of Eternity
- Companions assist in combat
- Temporary summons (cooldown-based)

---

### Legendary Artifacts
**Description:** Additional legendary slot (trinkets/accessories)

**Types:**
- Necklaces
- Rings
- Belts
- Cloaks

**Benefits:**
- More legendary variety
- Non-combat legendaries (utility focus)
- Additional customization

---

### Legendary Transformations
**Description:** Transform player appearance when using legendary

**Features:**
- Glowing eyes
- Particle aura
- Model changes (if supported by client mods)
- Special walk/run animations

**Technical Note:**
- Limited by vanilla Minecraft
- Could use armor stands or custom models (requires client-side mod)

---

## Quality of Life

### Command Aliases
**Description:** Shorter commands for frequently used actions

**Aliases:**
- `/ab1` and `/ab2` instead of `/ability 1` and `/ability 2`
- `/laltar` instead of `/givealtar`
- `/lreset` instead of `/kreset`

---

### Tab Completion
**Description:** Tab-complete command arguments

**Features:**
- `/givealtar <tab>` shows online players
- `/ability <tab>` shows 1 and 2
- Command suggestions in chat

---

### Legendary Lore Expansion
**Description:** Add rich lore and backstory to each legendary

**Features:**
- Multi-page lore books
- Obtainable lore fragments
- In-game codex (/legendary lore)
- Story quests related to legendaries

**Benefit:**
- Deeper world-building
- RPG immersion
- Content for players to discover

---

### Sound Effects Enhancement
**Description:** Unique sounds for each legendary

**Features:**
- Custom sound on equip
- Ambient sounds while held
- Unique ability sounds per legendary
- Volume controls in config

---

## Performance Optimizations

### Particle Optimization
**Description:** Reduce particle spam on busy servers

**Approaches:**
- Distance-based rendering (don't send particles to far players)
- Configurable particle density
- Batch particle spawning
- Cache particle locations

---

### Cooldown Cleanup
**Description:** Automatically remove expired cooldowns from memory

**Implementation:**
- Periodic cleanup task (every 5 minutes)
- Remove entries where endTime < currentTime - 1 hour
- Reduces memory footprint on long-running servers

---

## Community Features

### Legendary Challenges
**Description:** Server-wide challenges involving legendaries

**Examples:**
- "Deal 10,000 damage with Oblivion Harvester this week"
- "Use Star Rift Slash 100 times"
- "Craft all 11 legendaries"

**Rewards:**
- Experience
- Custom titles
- Cosmetic effects
- Economy currency

---

### Legendary PvP Arena
**Description:** Special PvP arena for legendary battles

**Features:**
- Queue system for matchmaking
- Elo rating system
- Legendary-only combat
- Spectator mode
- Leaderboards

---

### Legendary Showcase
**Description:** Players can display their legendaries

**Features:**
- Item frames at spawn showing all legendaries
- Hall of Fame for first crafters
- Museum showcasing legendary history
- Interactive displays with ability demonstrations

---

## Long-Term Vision

### Legendary Seasons
**Description:** Regular content updates with new legendaries

**Features:**
- New legendary every 2-3 months
- Seasonal themes (summer, winter, etc.)
- Limited-time legendaries
- Rotating available recipes

---

### Cross-Server Legendary Transfer
**Description:** Transfer legendaries between servers in a network

**Requirements:**
- Shared database
- Anti-duplication measures
- Transfer cooldown
- Admin approval system

---

### Legendary Mod Pack Integration
**Description:** Official Forge/Fabric mod for enhanced features

**Features:**
- Custom 3D models for legendaries
- Custom animations
- Enhanced particle effects
- Client-side legendary HUD

**Note:** This would be a separate project requiring mod development expertise

---

## Documentation Improvements

### Video Tutorials
**Description:** Create video guides for players and admins

**Topics:**
- How to craft legendaries
- Ability showcase for each legendary
- Admin setup guide
- Configuration tutorial

---

### Interactive Recipe Book
**Description:** Web-based recipe viewer

**Features:**
- Visual 5×5 grid showing recipes
- Material requirements list
- Hover tooltips
- Mobile-friendly design

**Hosting:**
- GitHub Pages
- Plugin's own web server (optional)

---

## Bug Fixes & Known Issues

### Current Known Issues
1. **Gale Throw** - Implementation needs event listener for trident impact
2. **Soul Mark** - Needs better tracking of "next hit" window
3. **Heaven's Wall** - Projectile blocking needs testing with various projectile types
4. **Forest Shield** - Breach effect may not work without additional damage event handling

### Testing Needed
- All abilities in multiplayer environment
- Legendary crafting with multiple players simultaneously
- Cooldown synchronization
- Data persistence across server restarts
- Performance with 50+ players online

---

## Deprecation & Migration

### If Moving to Database
**Migration Tool:**
- Read crafting.yml
- Convert to SQL inserts
- Verify data integrity
- Backup old files

### If Changing Recipe Format
**Backwards Compatibility:**
- Support old and new recipe formats
- Gradual migration period
- Admin notification of deprecated formats

---

**End of TODOs**

---

## Contributing

Want to contribute to this project? Consider:
- Implementing features from this TODO list
- Reporting bugs and issues
- Suggesting new ideas
- Creating pull requests
- Writing documentation
- Playtesting and providing feedback

---

**Note:** This TODO list is a living document and will be updated as features are implemented or new ideas emerge.
