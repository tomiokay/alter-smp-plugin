package com.alterSMP.legendaryweapons.altar;

import com.alterSMP.legendaryweapons.LegendaryWeaponsPlugin;
import org.bukkit.Location;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

public class AltarManager {

    private final LegendaryWeaponsPlugin plugin;
    private File altarDataFile;
    private FileConfiguration altarData;

    private Set<Location> altarLocations;

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
    }

    public void unregisterAltar(Location location) {
        altarLocations.remove(location.getBlock().getLocation());
        saveAltars();
        plugin.getLogger().info("Unregistered altar at " + location);
    }

    public boolean isAltar(Location location) {
        return altarLocations.contains(location.getBlock().getLocation());
    }
}
