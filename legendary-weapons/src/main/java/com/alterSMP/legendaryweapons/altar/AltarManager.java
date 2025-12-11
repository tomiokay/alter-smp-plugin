package com.alterSMP.legendaryweapons.altar;

import com.alterSMP.legendaryweapons.LegendaryWeaponsPlugin;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

public class AltarManager {

    private final LegendaryWeaponsPlugin plugin;
    private File altarDataFile;
    private FileConfiguration altarData;

    private Set<Location> altarLocations;
    private BukkitRunnable particleTask;

    public AltarManager(LegendaryWeaponsPlugin plugin) {
        this.plugin = plugin;
        this.altarLocations = new HashSet<>();
        initializeFiles();
    }

    private void initializeFiles() {
        if (!plugin.getDataFolder().exists()) {
            plugin.getDataFolder().mkdirs();
        }

        altarDataFile = new File(plugin.getDataFolder(), "altars.yml");
        if (!altarDataFile.exists()) {
            try {
                altarDataFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().severe("Could not create altars.yml!");
                e.printStackTrace();
            }
        }
        altarData = YamlConfiguration.loadConfiguration(altarDataFile);
    }

    public void loadAltars() {
        altarLocations.clear();

        if (!altarData.contains("altars")) {
            return;
        }

        for (String key : altarData.getConfigurationSection("altars").getKeys(false)) {
            String worldName = altarData.getString("altars." + key + ".world");
            int x = altarData.getInt("altars." + key + ".x");
            int y = altarData.getInt("altars." + key + ".y");
            int z = altarData.getInt("altars." + key + ".z");

            Location loc = new Location(plugin.getServer().getWorld(worldName), x, y, z);
            altarLocations.add(loc);
        }

        plugin.getLogger().info("Loaded " + altarLocations.size() + " altar locations");

        // Start particle effects
        startParticleEffects();
    }

    public void saveAltars() {
        altarData.set("altars", null);

        int index = 0;
        for (Location loc : altarLocations) {
            String key = "altar_" + index++;
            altarData.set("altars." + key + ".world", loc.getWorld().getName());
            altarData.set("altars." + key + ".x", loc.getBlockX());
            altarData.set("altars." + key + ".y", loc.getBlockY());
            altarData.set("altars." + key + ".z", loc.getBlockZ());
        }

        try {
            altarData.save(altarDataFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save altars.yml!");
            e.printStackTrace();
        }
    }

    public void registerAltar(Location location) {
        altarLocations.add(location.getBlock().getLocation());
        saveAltars();
        plugin.getLogger().info("Registered new altar at " + location);

        // Play placement effect
        Location centerLoc = location.getBlock().getLocation().add(0.5, 1.0, 0.5);
        location.getWorld().spawnParticle(Particle.ENCHANT, centerLoc, 100, 0.5, 0.5, 0.5, 1.0);
        location.getWorld().spawnParticle(Particle.WITCH, centerLoc, 50, 0.3, 0.3, 0.3, 0.1);
        location.getWorld().playSound(centerLoc, Sound.BLOCK_ENCHANTMENT_TABLE_USE, 1.5f, 0.8f);
    }

    public void unregisterAltar(Location location) {
        altarLocations.remove(location.getBlock().getLocation());
        saveAltars();
        plugin.getLogger().info("Unregistered altar at " + location);
    }

    public boolean isAltar(Location location) {
        return altarLocations.contains(location.getBlock().getLocation());
    }

    /**
     * Start the particle effect task that shows effects around all forges
     */
    public void startParticleEffects() {
        // Cancel existing task if running
        if (particleTask != null) {
            particleTask.cancel();
        }

        particleTask = new BukkitRunnable() {
            int tick = 0;

            @Override
            public void run() {
                for (Location loc : altarLocations) {
                    if (loc.getWorld() == null) continue;

                    // Check if the chunk is loaded (performance)
                    if (!loc.getWorld().isChunkLoaded(loc.getBlockX() >> 4, loc.getBlockZ() >> 4)) {
                        continue;
                    }

                    Location centerLoc = loc.clone().add(0.5, 1.2, 0.5);

                    // Enchantment glint particles floating up
                    loc.getWorld().spawnParticle(Particle.ENCHANT, centerLoc, 3, 0.3, 0.2, 0.3, 0.5);

                    // Occasional purple/witch particles
                    if (tick % 4 == 0) {
                        loc.getWorld().spawnParticle(Particle.WITCH, centerLoc, 2, 0.2, 0.1, 0.2, 0.02);
                    }

                    // Rotating sparkle effect
                    double angle = (tick * 0.15) % (2 * Math.PI);
                    double radius = 0.6;
                    double x = Math.cos(angle) * radius;
                    double z = Math.sin(angle) * radius;
                    Location orbLoc = centerLoc.clone().add(x, 0.3 + Math.sin(tick * 0.1) * 0.2, z);
                    loc.getWorld().spawnParticle(Particle.END_ROD, orbLoc, 1, 0, 0, 0, 0);

                    // Second orb on opposite side
                    Location orbLoc2 = centerLoc.clone().add(-x, 0.3 + Math.cos(tick * 0.1) * 0.2, -z);
                    loc.getWorld().spawnParticle(Particle.END_ROD, orbLoc2, 1, 0, 0, 0, 0);
                }

                tick++;
            }
        };

        // Run every 2 ticks (10 times per second) for smooth animation
        particleTask.runTaskTimer(plugin, 0L, 2L);
    }

    /**
     * Stop the particle effect task
     */
    public void stopParticleEffects() {
        if (particleTask != null) {
            particleTask.cancel();
            particleTask = null;
        }
    }

    /**
     * Get all altar locations (for cleanup on disable)
     */
    public Set<Location> getAltarLocations() {
        return new HashSet<>(altarLocations);
    }
}
