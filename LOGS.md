# Logging Documentation
## Legendary Weapons SMP Plugin

This document describes the logging system used by the Legendary Weapons SMP plugin, including what events are logged, log formats, and how to interpret log messages.

---

## 1. Log Levels

The plugin uses standard Java logging levels:

- **INFO** - Normal operational messages
- **WARNING** - Important events that don't prevent operation
- **SEVERE** - Critical errors that may affect functionality

---

## 2. Plugin Lifecycle Events

### 2.1 Plugin Enable
**Log Level:** INFO
**When:** Plugin is enabled during server startup or reload

```
[LegendaryWeaponsSMP] Loaded player crafting history
[LegendaryWeaponsSMP] Loaded 3 altar locations
[LegendaryWeaponsSMP] Legendary Weapons SMP plugin enabled!
```

### 2.2 Plugin Disable
**Log Level:** INFO
**When:** Plugin is disabled during server shutdown or reload

```
[LegendaryWeaponsSMP] Saved player crafting history
[LegendaryWeaponsSMP] Legendary Weapons SMP plugin disabled!
```

---

## 3. Data Persistence Events

### 3.1 Crafting History

**Player Crafts Legendary:**
```
[LegendaryWeaponsSMP] Player 12345678-90ab-cdef-1234-567890abcdef crafted blade_of_the_fractured_stars
```
- Logged at INFO level
- Includes player UUID and legendary ID
- Triggered when player successfully takes a legendary from the altar

**Crafting Data Loaded:**
```
[LegendaryWeaponsSMP] Loaded player crafting history
```
- Logged at INFO level
- Triggered on plugin enable

**Crafting Data Saved:**
```
[LegendaryWeaponsSMP] Saved player crafting history
```
- Logged at INFO level
- Triggered on plugin disable or after each craft

### 3.2 Altar Management

**Altar Registered:**
```
[LegendaryWeaponsSMP] Registered new altar at Location{world=world,x=100.0,y=64.0,z=-200.0}
```
- Logged at INFO level
- Includes full location details
- Triggered when a player places a Legendary Altar

**Altar Unregistered:**
```
[LegendaryWeaponsSMP] Unregistered altar at Location{world=world,x=100.0,y=64.0,z=-200.0}
```
- Logged at INFO level
- Includes full location details
- Triggered when a player breaks a Legendary Altar

**Altars Loaded:**
```
[LegendaryWeaponsSMP] Loaded 5 altar locations
```
- Logged at INFO level
- Includes total count
- Triggered on plugin enable

---

## 4. Administrative Actions

### 4.1 Global Reset Command

```
[LegendaryWeaponsSMP] All crafting history has been reset!
[LegendaryWeaponsSMP] [WARNING] Legendary crafting was reset by PlayerName
```
- First line: INFO level
- Second line: WARNING level (to highlight administrative action)
- Includes the name of the person who executed `/kreset`
- Triggered when `/kreset` command is executed

---

## 5. Error Events

### 5.1 File Creation Errors

**Crafting Data File:**
```
[LegendaryWeaponsSMP] [SEVERE] Could not create crafting.yml!
java.io.IOException: Permission denied
    at ...
```
- Logged at SEVERE level
- Includes full stack trace
- Occurs when the plugin cannot create or write to `crafting.yml`

**Altars Data File:**
```
[LegendaryWeaponsSMP] [SEVERE] Could not create altars.yml!
java.io.IOException: Permission denied
    at ...
```
- Logged at SEVERE level
- Includes full stack trace
- Occurs when the plugin cannot create or write to `altars.yml`

### 5.2 File Save Errors

**Crafting Save Error:**
```
[LegendaryWeaponsSMP] [SEVERE] Could not save crafting.yml!
java.io.IOException: Disk full
    at ...
```
- Logged at SEVERE level
- Includes full stack trace
- Occurs when the plugin cannot save crafting data

**Altars Save Error:**
```
[LegendaryWeaponsSMP] [SEVERE] Could not save altars.yml!
java.io.IOException: Disk full
    at ...
```
- Logged at SEVERE level
- Includes full stack trace
- Occurs when the plugin cannot save altar data

---

## 6. Ability Usage (Not Currently Logged)

Currently, ability usage is not logged to the server console to avoid log spam. However, players receive in-game feedback when using abilities.

**Potential Future Enhancement:**
If debugging or monitoring is needed, ability usage could be logged at FINE level:
```
[LegendaryWeaponsSMP] [FINE] Player PlayerName used ability 1 of blade_of_the_fractured_stars
```

---

## 7. Log File Locations

Standard server logs include plugin messages:
- **Server Console** - Real-time output visible in terminal
- **logs/latest.log** - Current session log
- **logs/YYYY-MM-DD-N.log.gz** - Archived logs from previous sessions

Plugin-specific data files:
- **plugins/LegendaryWeaponsSMP/crafting.yml** - Player crafting history
- **plugins/LegendaryWeaponsSMP/altars.yml** - Legendary Altar locations

---

## 8. Interpreting Common Log Patterns

### 8.1 Successful Plugin Operation
```
[LegendaryWeaponsSMP] Loaded player crafting history
[LegendaryWeaponsSMP] Loaded 2 altar locations
[LegendaryWeaponsSMP] Legendary Weapons SMP plugin enabled!
```
All systems loaded successfully, plugin is operational.

### 8.2 First-Time Server Start
```
[LegendaryWeaponsSMP] Loaded player crafting history
[LegendaryWeaponsSMP] Loaded 0 altar locations
[LegendaryWeaponsSMP] Legendary Weapons SMP plugin enabled!
```
No existing data, plugin initialized fresh database.

### 8.3 Active Gameplay Session
```
[LegendaryWeaponsSMP] Registered new altar at Location{...}
[LegendaryWeaponsSMP] Player 12345... crafted umbra_veil_dagger
[LegendaryWeaponsSMP] Player 67890... crafted emberheart_scythe
```
Players are actively using the plugin systems.

### 8.4 Administrative Reset
```
[LegendaryWeaponsSMP] All crafting history has been reset!
[LegendaryWeaponsSMP] [WARNING] Legendary crafting was reset by AdminName
```
An administrator has reset all crafting progress.

---

## 9. Troubleshooting with Logs

### 9.1 Crafting Not Working
**Check for:**
```
[LegendaryWeaponsSMP] Player ... crafted ...
```
If this appears, crafting is working. If not, check:
- Are altars placed correctly?
- Does the recipe match exactly?
- Has the player already crafted this legendary?

### 9.2 Data Not Persisting
**Check for:**
```
[LegendaryWeaponsSMP] [SEVERE] Could not save ...
```
This indicates a file permission or disk space issue.

**Resolution:**
- Check folder permissions for `plugins/LegendaryWeaponsSMP/`
- Ensure sufficient disk space
- Verify server has write access to plugin data folder

### 9.3 Altars Not Loading After Restart
**Check for:**
```
[LegendaryWeaponsSMP] Loaded X altar locations
```
If X is less than expected, check:
- Was `altars.yml` saved correctly before shutdown?
- Is `altars.yml` corrupted? (Try validating YAML syntax)

---

## 10. Performance Monitoring

While the plugin doesn't log performance metrics by default, server administrators can monitor:

**Potential Lag Sources:**
- Passive effect task runs every 10 ticks
- Large numbers of active abilities with particle effects
- Many players using abilities simultaneously

**Monitoring Commands:**
```
/timings on
/timings paste
```
Use Paper's built-in timings to identify if the plugin is causing performance issues.

---

## 11. Debug Mode (Future Enhancement)

A potential future feature could add a debug mode:

**Config Option:**
```yaml
debug: true
```

**Debug Logs Would Include:**
- Every ability activation with player name
- Cooldown checks and remaining times
- Recipe matching attempts
- Passive effect applications

---

**End of Logging Documentation**
