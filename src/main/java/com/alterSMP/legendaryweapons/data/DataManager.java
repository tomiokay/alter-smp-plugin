package com.alterSMP.legendaryweapons.data;

import com.alterSMP.legendaryweapons.LegendaryWeaponsPlugin;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class DataManager {

    private final LegendaryWeaponsPlugin plugin;
    private File craftingDataFile;
    private FileConfiguration craftingData;

    // Global crafting history: Legendary ID -> UUID of crafter
    private Map<String, UUID> globalCraftingHistory;

    public DataManager(LegendaryWeaponsPlugin plugin) {
        this.plugin = plugin;
        this.globalCraftingHistory = new HashMap<>();
        initializeFiles();
    }

    private void initializeFiles() {
        if (!plugin.getDataFolder().exists()) {
            plugin.getDataFolder().mkdirs();
        }

        craftingDataFile = new File(plugin.getDataFolder(), "crafting.yml");
        if (!craftingDataFile.exists()) {
            try {
                craftingDataFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().severe("Could not create crafting.yml!");
                e.printStackTrace();
            }
        }
        craftingData = YamlConfiguration.loadConfiguration(craftingDataFile);
    }

    public void loadAll() {
        loadCraftingHistory();
        plugin.getLogger().info("Loaded global crafting history");
    }

    public void saveAll() {
        saveCraftingHistory();
        plugin.getLogger().info("Saved global crafting history");
    }

    private void loadCraftingHistory() {
        globalCraftingHistory.clear();

        if (!craftingData.contains("crafted")) {
            return;
        }

        for (String legendaryId : craftingData.getConfigurationSection("crafted").getKeys(false)) {
            String uuidString = craftingData.getString("crafted." + legendaryId + ".crafter");
            if (uuidString != null) {
                UUID crafter = UUID.fromString(uuidString);
                globalCraftingHistory.put(legendaryId, crafter);
            }
        }
    }

    private void saveCraftingHistory() {
        craftingData.set("crafted", null); // Clear existing data

        for (Map.Entry<String, UUID> entry : globalCraftingHistory.entrySet()) {
            String legendaryId = entry.getKey();
            String crafterUUID = entry.getValue().toString();
            craftingData.set("crafted." + legendaryId + ".crafter", crafterUUID);
        }

        try {
            craftingData.save(craftingDataFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save crafting.yml!");
            e.printStackTrace();
        }
    }

    public boolean hasCrafted(String legendaryId) {
        return globalCraftingHistory.containsKey(legendaryId);
    }

    public UUID getCrafter(String legendaryId) {
        return globalCraftingHistory.get(legendaryId);
    }

    public void markCrafted(UUID playerUUID, String legendaryId) {
        globalCraftingHistory.put(legendaryId, playerUUID);
        saveCraftingHistory();

        plugin.getLogger().info("Player " + playerUUID + " crafted " + legendaryId + " (GLOBALLY)");
    }

    public void resetAllCrafting() {
        globalCraftingHistory.clear();
        saveCraftingHistory();
        plugin.getLogger().info("All global crafting history has been reset!");
    }

    public int getTotalCraftedGlobally() {
        return globalCraftingHistory.size();
    }

    public Set<String> getAllCraftedLegendaries() {
        return new HashSet<>(globalCraftingHistory.keySet());
    }
}
