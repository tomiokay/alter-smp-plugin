package com.alterSMP.legendaryweapons.data;

import com.alterSMP.legendaryweapons.LegendaryWeaponsPlugin;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class TrustManager {

    private final LegendaryWeaponsPlugin plugin;
    private File trustFile;
    private FileConfiguration trustConfig;

    // Map: player UUID -> Set of trusted player UUIDs
    private Map<UUID, Set<UUID>> trustData;

    public TrustManager(LegendaryWeaponsPlugin plugin) {
        this.plugin = plugin;
        this.trustData = new HashMap<>();
        loadTrustData();
    }

    private void loadTrustData() {
        trustFile = new File(plugin.getDataFolder(), "trust.yml");

        if (!trustFile.exists()) {
            try {
                trustFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().severe("Could not create trust.yml: " + e.getMessage());
            }
        }

        trustConfig = YamlConfiguration.loadConfiguration(trustFile);

        // Load trust data
        if (trustConfig.contains("players")) {
            for (String playerUUID : trustConfig.getConfigurationSection("players").getKeys(false)) {
                UUID uuid = UUID.fromString(playerUUID);
                List<String> trustedList = trustConfig.getStringList("players." + playerUUID + ".trust");

                Set<UUID> trustedSet = new HashSet<>();
                for (String trustedUUID : trustedList) {
                    try {
                        trustedSet.add(UUID.fromString(trustedUUID));
                    } catch (IllegalArgumentException e) {
                        plugin.getLogger().warning("Invalid UUID in trust data: " + trustedUUID);
                    }
                }

                trustData.put(uuid, trustedSet);
            }
        }

        plugin.getLogger().info("Loaded trust data for " + trustData.size() + " players");
    }

    public void saveTrustData() {
        trustConfig = new YamlConfiguration();

        for (Map.Entry<UUID, Set<UUID>> entry : trustData.entrySet()) {
            String playerUUID = entry.getKey().toString();
            List<String> trustedList = new ArrayList<>();

            for (UUID trustedUUID : entry.getValue()) {
                trustedList.add(trustedUUID.toString());
            }

            trustConfig.set("players." + playerUUID + ".trust", trustedList);
        }

        try {
            trustConfig.save(trustFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save trust.yml: " + e.getMessage());
        }
    }

    /**
     * Check if source player trusts target player.
     * If true, source's legendary effects should NOT affect target.
     */
    public boolean isTrusted(UUID sourceUUID, UUID targetUUID) {
        Set<UUID> trustedPlayers = trustData.get(sourceUUID);
        if (trustedPlayers == null) {
            return false;
        }
        return trustedPlayers.contains(targetUUID);
    }

    /**
     * Check if source player trusts target player (Player objects).
     */
    public boolean isTrusted(Player source, Player target) {
        return isTrusted(source.getUniqueId(), target.getUniqueId());
    }

    /**
     * Toggle trust for a player.
     * @return true if now trusted, false if trust was removed
     */
    public boolean toggleTrust(UUID sourceUUID, UUID targetUUID) {
        Set<UUID> trustedPlayers = trustData.computeIfAbsent(sourceUUID, k -> new HashSet<>());

        if (trustedPlayers.contains(targetUUID)) {
            // Already trusted, remove
            trustedPlayers.remove(targetUUID);
            saveTrustData();
            return false;
        } else {
            // Not trusted, add
            trustedPlayers.add(targetUUID);
            saveTrustData();
            return true;
        }
    }

    /**
     * Get all players trusted by source.
     */
    public Set<UUID> getTrustedPlayers(UUID sourceUUID) {
        return trustData.getOrDefault(sourceUUID, new HashSet<>());
    }
}
