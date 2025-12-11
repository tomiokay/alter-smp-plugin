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
    // Crafting location history: Legendary ID -> "world,x,y,z"
    private Map<String, String> craftingLocations;
    // Disabled legendaries (cannot be crafted)
    private Set<String> disabledLegendaries;

    public DataManager(LegendaryWeaponsPlugin plugin) {
        this.plugin = plugin;
        this.globalCraftingHistory = new HashMap<>();
        this.craftingLocations = new HashMap<>();
        this.disabledLegendaries = new HashSet<>();
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
        craftingLocations.clear();
        disabledLegendaries.clear();

        if (craftingData.contains("crafted")) {
            for (String legendaryId : craftingData.getConfigurationSection("crafted").getKeys(false)) {
                String uuidString = craftingData.getString("crafted." + legendaryId + ".crafter");
                if (uuidString != null) {
                    UUID crafter = UUID.fromString(uuidString);
                    globalCraftingHistory.put(legendaryId, crafter);
                }
                String location = craftingData.getString("crafted." + legendaryId + ".location");
                if (location != null) {
                    craftingLocations.put(legendaryId, location);
                }
            }
        }

        // Load disabled legendaries
        if (craftingData.contains("disabled")) {
            List<String> disabled = craftingData.getStringList("disabled");
            disabledLegendaries.addAll(disabled);
        }
    }

    private void saveCraftingHistory() {
        craftingData.set("crafted", null); // Clear existing data

        for (Map.Entry<String, UUID> entry : globalCraftingHistory.entrySet()) {
            String legendaryId = entry.getKey();
            String crafterUUID = entry.getValue().toString();
            craftingData.set("crafted." + legendaryId + ".crafter", crafterUUID);
            // Save location if available
            String location = craftingLocations.get(legendaryId);
            if (location != null) {
                craftingData.set("crafted." + legendaryId + ".location", location);
            }
        }

        // Save disabled legendaries
        craftingData.set("disabled", new ArrayList<>(disabledLegendaries));

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

    public void markCrafted(UUID playerUUID, String legendaryId, String location) {
        globalCraftingHistory.put(legendaryId, playerUUID);
        if (location != null) {
            craftingLocations.put(legendaryId, location);
        }
        saveCraftingHistory();

        plugin.getLogger().info("Player " + playerUUID + " crafted " + legendaryId + " at " + location + " (GLOBALLY)");
    }

    public String getCraftingLocation(String legendaryId) {
        return craftingLocations.get(legendaryId);
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

    public void resetLegendaryCrafting(String legendaryId) {
        globalCraftingHistory.remove(legendaryId);
        craftingLocations.remove(legendaryId);
        saveCraftingHistory();
        plugin.getLogger().info("Reset crafting for legendary: " + legendaryId);
    }

    // ========== DISABLED LEGENDARIES ==========

    public boolean isLegendaryDisabled(String legendaryId) {
        return disabledLegendaries.contains(legendaryId);
    }

    /**
     * Toggle a legendary's disabled state.
     * @return true if now disabled, false if now enabled
     */
    public boolean toggleLegendaryDisabled(String legendaryId) {
        if (disabledLegendaries.contains(legendaryId)) {
            disabledLegendaries.remove(legendaryId);
            saveCraftingHistory();
            return false; // Now enabled
        } else {
            disabledLegendaries.add(legendaryId);
            saveCraftingHistory();
            return true; // Now disabled
        }
    }

    public Set<String> getDisabledLegendaries() {
        return new HashSet<>(disabledLegendaries);
    }
}
