package com.alterSMP.legendaryweapons.data;

import com.alterSMP.legendaryweapons.LegendaryWeaponsPlugin;
import org.bukkit.Bukkit;
import org.bukkit.attribute.Attribute;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * Manages the heart stealing system for Creation Splitter.
 *
 * Tracks:
 * - Who has stolen hearts from whom
 * - Permanent heart loss/gain
 * - Returns hearts on death
 */
public class HeartManager {

    private final LegendaryWeaponsPlugin plugin;
    private final File dataFile;
    private FileConfiguration data;

    // Map of player UUID -> Set of UUIDs they've stolen hearts from
    private Map<UUID, Set<UUID>> stolenFrom = new HashMap<>();

    // Map of player UUID -> number of hearts they've lost permanently
    private Map<UUID, Integer> heartsLost = new HashMap<>();

    // Map of player UUID -> number of hearts they've gained (current holder)
    private Map<UUID, Integer> heartsGained = new HashMap<>();

    // Max hearts that can be stolen
    private static final int MAX_STOLEN_HEARTS = 5;

    public HeartManager(LegendaryWeaponsPlugin plugin) {
        this.plugin = plugin;
        this.dataFile = new File(plugin.getDataFolder(), "hearts.yml");
        loadData();
    }

    /**
     * Called when a player kills another player with Creation Splitter.
     * Returns true if a heart was stolen.
     */
    public boolean onPlayerKill(Player killer, Player victim) {
        UUID killerUUID = killer.getUniqueId();
        UUID victimUUID = victim.getUniqueId();

        // Check if already stolen from this player
        Set<UUID> killerStolen = stolenFrom.computeIfAbsent(killerUUID, k -> new HashSet<>());
        if (killerStolen.contains(victimUUID)) {
            return false; // Already stolen from this player
        }

        // Check if killer has reached max hearts
        int currentGained = heartsGained.getOrDefault(killerUUID, 0);
        if (currentGained >= MAX_STOLEN_HEARTS) {
            return false; // Already at max
        }

        // Steal a heart
        killerStolen.add(victimUUID);

        // Killer gains a heart
        heartsGained.put(killerUUID, currentGained + 1);
        applyHeartModifier(killer);

        // Victim loses a heart permanently
        int victimLost = heartsLost.getOrDefault(victimUUID, 0) + 1;
        heartsLost.put(victimUUID, victimLost);
        applyHeartModifier(victim);

        saveData();
        return true;
    }

    /**
     * Called when the Creation Splitter holder dies.
     * Returns all stolen hearts to their original owners.
     */
    public void onHolderDeath(Player holder) {
        UUID holderUUID = holder.getUniqueId();

        // Get who they stole from
        Set<UUID> stolenFromSet = stolenFrom.get(holderUUID);
        if (stolenFromSet == null || stolenFromSet.isEmpty()) {
            return;
        }

        // Return hearts to victims
        for (UUID victimUUID : stolenFromSet) {
            int victimLost = heartsLost.getOrDefault(victimUUID, 0);
            if (victimLost > 0) {
                heartsLost.put(victimUUID, victimLost - 1);

                // Apply to victim if online
                Player victim = Bukkit.getPlayer(victimUUID);
                if (victim != null) {
                    applyHeartModifier(victim);
                    victim.sendMessage(org.bukkit.ChatColor.GREEN + "Your heart has been restored from " + holder.getName() + "'s death!");
                }
            }
        }

        // Reset holder's stolen hearts
        stolenFrom.remove(holderUUID);
        heartsGained.remove(holderUUID);
        applyHeartModifier(holder);

        saveData();
    }

    /**
     * Apply the heart modifier to a player based on their current state.
     */
    public void applyHeartModifier(Player player) {
        UUID uuid = player.getUniqueId();

        int gained = heartsGained.getOrDefault(uuid, 0);
        int lost = heartsLost.getOrDefault(uuid, 0);

        // Base max health is 20 (10 hearts)
        // Each gained heart = +2 HP
        // Each lost heart = -2 HP
        double newMaxHealth = 20.0 + (gained * 2) - (lost * 2);

        // Minimum of 2 HP (1 heart)
        newMaxHealth = Math.max(2.0, newMaxHealth);

        // Maximum of 30 HP (15 hearts = 10 base + 5 stolen)
        newMaxHealth = Math.min(30.0, newMaxHealth);

        player.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(newMaxHealth);

        // Adjust current health if it exceeds new max
        if (player.getHealth() > newMaxHealth) {
            player.setHealth(newMaxHealth);
        }
    }

    /**
     * Apply heart modifiers to a player when they join.
     */
    public void onPlayerJoin(Player player) {
        applyHeartModifier(player);
    }

    /**
     * Get the number of hearts a player has stolen.
     */
    public int getHeartsStolen(UUID uuid) {
        return heartsGained.getOrDefault(uuid, 0);
    }

    /**
     * Get the number of hearts a player has lost.
     */
    public int getHeartsLost(UUID uuid) {
        return heartsLost.getOrDefault(uuid, 0);
    }

    /**
     * Check if a player has already stolen from another player.
     */
    public boolean hasAlreadyStolenFrom(UUID killer, UUID victim) {
        Set<UUID> stolen = stolenFrom.get(killer);
        return stolen != null && stolen.contains(victim);
    }

    private void loadData() {
        if (!dataFile.exists()) {
            data = new YamlConfiguration();
            return;
        }

        data = YamlConfiguration.loadConfiguration(dataFile);

        // Load stolen from
        if (data.contains("stolen-from")) {
            for (String killerUUID : data.getConfigurationSection("stolen-from").getKeys(false)) {
                List<String> victims = data.getStringList("stolen-from." + killerUUID);
                Set<UUID> victimSet = new HashSet<>();
                for (String victimUUID : victims) {
                    try {
                        victimSet.add(UUID.fromString(victimUUID));
                    } catch (IllegalArgumentException ignored) {}
                }
                try {
                    stolenFrom.put(UUID.fromString(killerUUID), victimSet);
                } catch (IllegalArgumentException ignored) {}
            }
        }

        // Load hearts lost
        if (data.contains("hearts-lost")) {
            for (String uuid : data.getConfigurationSection("hearts-lost").getKeys(false)) {
                try {
                    heartsLost.put(UUID.fromString(uuid), data.getInt("hearts-lost." + uuid));
                } catch (IllegalArgumentException ignored) {}
            }
        }

        // Load hearts gained
        if (data.contains("hearts-gained")) {
            for (String uuid : data.getConfigurationSection("hearts-gained").getKeys(false)) {
                try {
                    heartsGained.put(UUID.fromString(uuid), data.getInt("hearts-gained." + uuid));
                } catch (IllegalArgumentException ignored) {}
            }
        }
    }

    public void saveData() {
        data = new YamlConfiguration();

        // Save stolen from
        for (Map.Entry<UUID, Set<UUID>> entry : stolenFrom.entrySet()) {
            List<String> victims = new ArrayList<>();
            for (UUID victim : entry.getValue()) {
                victims.add(victim.toString());
            }
            data.set("stolen-from." + entry.getKey().toString(), victims);
        }

        // Save hearts lost
        for (Map.Entry<UUID, Integer> entry : heartsLost.entrySet()) {
            data.set("hearts-lost." + entry.getKey().toString(), entry.getValue());
        }

        // Save hearts gained
        for (Map.Entry<UUID, Integer> entry : heartsGained.entrySet()) {
            data.set("hearts-gained." + entry.getKey().toString(), entry.getValue());
        }

        try {
            data.save(dataFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to save hearts data: " + e.getMessage());
        }
    }
}
