# PebbleHost Deployment Guide

Complete guide to deploying Legendary Weapons SMP plugin to your PebbleHost Minecraft server.

---

## 📋 Prerequisites

- **PebbleHost Server**: Running Minecraft 1.21.8 with Paper/Spigot
- **Server Access**: FTP/SFTP or File Manager access
- **Plugin JAR**: `LegendaryWeaponsSMP-1.0.0.jar` (from `build/libs/`)

---

## 🚀 Step-by-Step Deployment

### 1. Build the Plugin (Local Computer)

**Option A: Using Gradle (Recommended)**
```bash
# Windows
gradlew.bat build

# Mac/Linux
./gradlew build
```

**Option B: If Build Fails (File Lock Issues)**
1. Close all IDEs, terminals, and Java processes
2. Delete the `build/` folder
3. Run the build command again

**Result**: Plugin JAR will be in `build/libs/LegendaryWeaponsSMP-1.0.0.jar`

---

### 2. Upload Plugin to PebbleHost

**Method 1: Using PebbleHost File Manager (Easiest)**

1. Log into your PebbleHost control panel at https://client.pebblehost.com
2. Select your Minecraft server
3. Click **"File Manager"** in the sidebar
4. Navigate to the **`plugins/`** folder
5. Click **"Upload"** button
6. Select `LegendaryWeaponsSMP-1.0.0.jar` from your computer
7. Wait for upload to complete

**Method 2: Using FTP/SFTP**

1. Get FTP credentials from PebbleHost panel (Server → FTP Details)
2. Connect using FileZilla or WinSCP
3. Navigate to `/plugins/` directory
4. Upload `LegendaryWeaponsSMP-1.0.0.jar`

---

### 3. Restart Your Server

**In PebbleHost Control Panel:**
1. Click **"Console"** in the sidebar
2. Click the **"Restart"** button (or type `/stop` in console)
3. Wait for server to restart (usually 30-60 seconds)

**Check for Successful Load:**
- Look for: `[LegendaryWeaponsSMP] Legendary Weapons SMP plugin enabled!` in console
- If you see errors, check the troubleshooting section below

---

### 4. First-Time Setup (In-Game)

1. **Join your server** as an OP
2. **Give yourself an altar**: `/givealtar`
3. **Place the altar**: Place it like a block (bedrock appearance)
4. **Test crafting**: Right-click the altar to open 5×5 GUI
5. **Test abilities**:
   - Give yourself a weapon: `/givelegendary blade_of_the_fractured_stars`
   - Use abilities: `/ability 1` and `/ability 2`

---

## 📁 Server File Structure

After installation, your server will have:

```
server/
├── plugins/
│   ├── LegendaryWeaponsSMP-1.0.0.jar  ← Plugin file
│   └── LegendaryWeaponsSMP/            ← Plugin data folder (auto-created)
│       ├── config.yml                  ← Main configuration
│       ├── crafting.yml                ← Global crafting history
│       ├── trust.yml                   ← Trust system data
│       └── altars.yml                  ← Altar locations
```

---

## ⚙️ Configuration

### Main Config (`plugins/LegendaryWeaponsSMP/config.yml`)

The plugin auto-generates this file with all 11 legendary recipes. You can edit:

- **Crafting recipes** - Change ingredients for each legendary
- **Passive tick interval** - Adjust performance (default: every 10 ticks)

**Example - Change Eclipse Devourer Recipe:**
```yaml
eclipse_devourer:
  - DRAGON_HEAD
  - DRAGON_BREATH
  - END_STONE
  - DRAGON_BREATH
  - DRAGON_HEAD
  # ... (25 slots total for 5×5 grid)
  - DRAGON_EGG  # Change this to make it easier/harder
```

### Reset Crafting Progress

If you want to allow players to craft legendaries again:
```
/kreset                    # Reset ALL legendaries
/kresetplayer <player>     # Reset specific player's legendaries
```

---

## 🎮 Admin Commands for Testing

```bash
# Give yourself an altar to place
/givealtar

# Give a legendary weapon (marks as crafted - can't craft again)
/giveweapon blade_of_the_fractured_stars

# Give a legendary for testing (DOESN'T mark as crafted)
/givelegendary umbra_veil_dagger

# Clear ability cooldowns (for testing)
/cooldown

# Reload configuration
/lreload
```

---

## 🔧 Troubleshooting

### Plugin Not Loading

**Error: "Unsupported API version"**
- **Solution**: Your server is not running Paper/Spigot 1.21.8
- **Fix**: Update server to 1.21.8 or change `api-version: '1.21'` in `plugin.yml`

**Error: "Could not load plugin"**
- **Solution**: Server needs Java 21+
- **Fix**: Contact PebbleHost support to upgrade Java version

**Error: "Class not found"**
- **Solution**: JAR file is corrupted
- **Fix**: Re-upload the plugin JAR

### Abilities Not Working

**Problem: `/ability` command does nothing**
- Check you're holding the legendary weapon
- Check cooldowns with `/cooldown` command
- Ensure you have permission (defaults to all players)

**Problem: Particles not showing**
- Check client particle settings (Options → Video Settings → Particles)
- Server may have particle limits - ask host to check `server.properties`

### Performance Issues

**Server lagging with many players:**

1. **Increase passive tick interval** in `config.yml`:
   ```yaml
   passive_tick_interval: 20  # Change from 10 to 20 (check less frequently)
   ```

2. **Limit particle render distance** (client-side - have players adjust):
   - Video Settings → Particles → Decreased

3. **Monitor with `/timings`** command to identify laggy abilities

---

## 🔐 Permissions

All commands have permissions that default to OP:

| Permission | Command | Default |
|-----------|---------|---------|
| `legendaryweapons.givealtar` | `/givealtar` | OP |
| `legendaryweapons.giveweapon` | `/giveweapon` | OP |
| `legendaryweapons.givelegendary` | `/givelegendary` | OP |
| `legendaryweapons.kreset` | `/kreset` | OP |
| `legendaryweapons.kresetplayer` | `/kresetplayer` | OP |
| `legendaryweapons.reload` | `/lreload` | OP |
| `legendaryweapons.cooldown` | `/cooldown` | OP |
| None | `/ability` | All players |
| None | `/trust` | All players |

**To give non-OP players admin commands:**
1. Install a permissions plugin (LuckPerms recommended)
2. Grant permissions: `/lp user <player> permission set legendaryweapons.givealtar true`

---

## 📊 Server Resource Usage

**Expected Performance:**
- **CPU**: Low impact (mostly event-based)
- **RAM**: ~5-10MB additional
- **Disk**: <1MB for plugin data
- **Network**: Particle effects may increase bandwidth slightly

**Recommended PebbleHost Plan:**
- **Minimum**: Budget plan (1GB RAM) for small servers (<5 players)
- **Recommended**: Premium plan (2GB+ RAM) for active servers (10+ players)

---

## 🔄 Updating the Plugin

1. **Stop the server** (or use `/stop`)
2. **Backup your data**:
   - Copy `/plugins/LegendaryWeaponsSMP/` folder (saves crafting history)
3. **Delete old JAR**: Remove old `LegendaryWeaponsSMP-X.X.X.jar`
4. **Upload new JAR**: Upload updated version
5. **Restart server**
6. **Check console** for successful load

**Note**: Data files (`crafting.yml`, `trust.yml`, `altars.yml`) are preserved between updates!

---

## 🆘 Getting Help

**Plugin Issues:**
1. Check server console for error messages
2. Review this deployment guide
3. Check GitHub issues: https://github.com/tomiokay/alter-smp-plugin/issues

**PebbleHost Issues:**
- Contact PebbleHost support: https://pebblehost.com/support
- Live chat available 24/7

**Common PebbleHost Questions:**
- **How to increase RAM**: Upgrade plan in billing panel
- **How to change Java version**: Contact support ticket
- **How to access FTP**: Server → FTP Details in panel

---

## ✅ Deployment Checklist

Before going live:

- [ ] Plugin JAR built successfully
- [ ] Uploaded to PebbleHost `/plugins/` folder
- [ ] Server restarted successfully
- [ ] No errors in console
- [ ] Tested `/givealtar` command
- [ ] Tested placing and opening altar
- [ ] Tested crafting a legendary
- [ ] Tested ability commands
- [ ] Tested particle effects
- [ ] Tested trust system (`/trust`)
- [ ] Configured permissions (if needed)
- [ ] Backed up server data

---

## 🎉 You're Ready!

Your Legendary Weapons SMP plugin is now live on PebbleHost!

**Next Steps:**
1. Announce to players how to obtain legendaries
2. Hide altars around the world for players to find
3. Monitor performance and adjust config as needed
4. Create events/competitions around legendary weapons

**Enjoy the epic battles!** ⚔️✨
