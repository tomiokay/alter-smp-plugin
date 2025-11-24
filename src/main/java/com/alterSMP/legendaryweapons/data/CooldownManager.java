package com.alterSMP.legendaryweapons.data;

import com.alterSMP.legendaryweapons.LegendaryWeaponsPlugin;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class CooldownManager {

    private final LegendaryWeaponsPlugin plugin;

    // Map: UUID -> (LegendaryID + AbilityNum -> EndTime)
    private Map<UUID, Map<String, Long>> cooldowns;

    public CooldownManager(LegendaryWeaponsPlugin plugin) {
        this.plugin = plugin;
        this.cooldowns = new HashMap<>();
    }

    public boolean isOnCooldown(UUID playerUUID, String legendaryId, int abilityNum) {
        String key = legendaryId + "_" + abilityNum;
        Map<String, Long> playerCooldowns = cooldowns.get(playerUUID);

        if (playerCooldowns == null || !playerCooldowns.containsKey(key)) {
            return false;
        }

        long endTime = playerCooldowns.get(key);
        long currentTime = System.currentTimeMillis();

        if (currentTime >= endTime) {
            playerCooldowns.remove(key);
            return false;
        }

        return true;
    }

    public void setCooldown(UUID playerUUID, String legendaryId, int abilityNum, int seconds) {
        String key = legendaryId + "_" + abilityNum;
        long endTime = System.currentTimeMillis() + (seconds * 1000L);

        cooldowns.computeIfAbsent(playerUUID, k -> new HashMap<>()).put(key, endTime);
    }

    public int getRemainingCooldown(UUID playerUUID, String legendaryId, int abilityNum) {
        String key = legendaryId + "_" + abilityNum;
        Map<String, Long> playerCooldowns = cooldowns.get(playerUUID);

        if (playerCooldowns == null || !playerCooldowns.containsKey(key)) {
            return 0;
        }

        long endTime = playerCooldowns.get(key);
        long currentTime = System.currentTimeMillis();
        long remaining = endTime - currentTime;

        if (remaining <= 0) {
            playerCooldowns.remove(key);
            return 0;
        }

        return (int) Math.ceil(remaining / 1000.0);
    }
}
