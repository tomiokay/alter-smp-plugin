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

    // Map: UUID -> Set of crafted legendary IDs
    private Map<UUID, Set<String>> playerCraftingHistory;

    public DataManager(LegendaryWeaponsPlugin plugin) {
        this.plugin = plugin;
        this.playerCraftingHistory = new HashMap<>();
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
        plugin.getLogger().info("Loaded player crafting history");
    }

    public void saveAll() {
        saveCraftingHistory();
        plugin.getLogger().info("Saved player crafting history");
    }

    private void loadCraftingHistory() {
        playerCraftingHistory.clear();

        if (!craftingData.contains("players")) {
            return;
        }

        for (String uuidString : craftingData.getConfigurationSection("players").getKeys(false)) {
            UUID uuid = UUID.fromString(uuidString);
            List<String> craftedList = craftingData.getStringList("players." + uuidString + ".crafted");
            playerCraftingHistory.put(uuid, new HashSet<>(craftedList));
        }
    }

    private void saveCraftingHistory() {
        craftingData.set("players", null); // Clear existing data

        for (Map.Entry<UUID, Set<String>> entry : playerCraftingHistory.entrySet()) {
            String uuidString = entry.getKey().toString();
            craftingData.set("players." + uuidString + ".crafted", new ArrayList<>(entry.getValue()));
        }

        try {
            craftingData.save(craftingDataFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save crafting.yml!");
            e.printStackTrace();
        }
    }

    public boolean hasCrafted(UUID playerUUID, String legendaryId) {
        Set<String> crafted = playerCraftingHistory.get(playerUUID);
        return crafted != null && crafted.contains(legendaryId);
    }

    public void markCrafted(UUID playerUUID, String legendaryId) {
        playerCraftingHistory.computeIfAbsent(playerUUID, k -> new HashSet<>()).add(legendaryId);
        saveCraftingHistory();

        plugin.getLogger().info("Player " + playerUUID + " crafted " + legendaryId);
    }

    public void resetAllCrafting() {
        playerCraftingHistory.clear();
        saveCraftingHistory();
        plugin.getLogger().info("All crafting history has been reset!");
    }

    public int getTotalCrafted(UUID playerUUID) {
        Set<String> crafted = playerCraftingHistory.get(playerUUID);
        return crafted != null ? crafted.size() : 0;
    }
}
