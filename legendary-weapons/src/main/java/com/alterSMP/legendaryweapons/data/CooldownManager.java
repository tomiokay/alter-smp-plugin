package com.alterSMP.legendaryweapons.data;

import com.alterSMP.legendaryweapons.LegendaryWeaponsPlugin;
import com.alterSMP.legendaryweapons.items.LegendaryItemFactory;
import com.alterSMP.legendaryweapons.items.LegendaryType;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class CooldownManager {

    private final LegendaryWeaponsPlugin plugin;

    // Map: UUID -> (LegendaryID + AbilityNum -> EndTime)
    private Map<UUID, Map<String, Long>> cooldowns;
    private Set<UUID> cooldownDisplayDisabled = new HashSet<>();

    public CooldownManager(LegendaryWeaponsPlugin plugin) {
        this.plugin = plugin;
        this.cooldowns = new HashMap<>();
        startActionBarTask();
    }

    /**
     * Toggle cooldown display for a player
     */
    public boolean toggleCooldownDisplay(UUID playerId) {
        if (cooldownDisplayDisabled.contains(playerId)) {
            cooldownDisplayDisabled.remove(playerId);
            return true; // Now enabled
        } else {
            cooldownDisplayDisabled.add(playerId);
            return false; // Now disabled
        }
    }

    /**
     * Check if cooldown display is enabled for a player
     */
    public boolean isCooldownDisplayEnabled(UUID playerId) {
        return !cooldownDisplayDisabled.contains(playerId);
    }

    private void startActionBarTask() {
        new BukkitRunnable() {
            @Override
            public void run() {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    if (!cooldownDisplayDisabled.contains(player.getUniqueId())) {
                        displayCooldownActionBar(player);
                    }
                }
            }
        }.runTaskTimer(plugin, 0L, 10L); // Every 10 ticks (0.5s) - optimized from 5 ticks
    }

    private void displayCooldownActionBar(Player player) {
        ItemStack mainHand = player.getInventory().getItemInMainHand();
        String legendaryId = LegendaryItemFactory.getLegendaryId(mainHand);

        if (legendaryId == null) {
            return; // Not holding a legendary
        }

        LegendaryType type = LegendaryType.fromId(legendaryId);
        if (type == null) return;

        // Skip armor pieces and items without abilities
        if (type == LegendaryType.FORGE_BOOTS || type == LegendaryType.FORGE_CHESTPLATE
                || type == LegendaryType.FORGE_LEGGINGS || type == LegendaryType.FORGE_HELMET
                || type == LegendaryType.LANTERN_OF_LOST_NAMES) {
            return;
        }

        int cd1 = getRemainingCooldown(player.getUniqueId(), legendaryId, 1);
        int cd2 = getRemainingCooldown(player.getUniqueId(), legendaryId, 2);

        StringBuilder actionBar = new StringBuilder();

        // Ability 1 - just number and timer
        actionBar.append(ChatColor.GOLD).append("[1] ");
        if (cd1 > 0) {
            actionBar.append(ChatColor.RED).append(formatTime(cd1));
        } else {
            actionBar.append(ChatColor.GREEN).append("READY");
        }

        // Combat timer in the middle (check if combat-logger plugin is present)
        String combatTime = getCombatTime(player);
        if (combatTime != null) {
            actionBar.append(ChatColor.GRAY).append("  ");
            actionBar.append(ChatColor.RED).append("⚔ ").append(combatTime);
            actionBar.append(ChatColor.GRAY).append("  ");
        } else {
            actionBar.append(ChatColor.GRAY).append("  |  ");
        }

        // Ability 2 - just number and timer
        actionBar.append(ChatColor.GOLD).append("[2] ");
        if (cd2 > 0) {
            actionBar.append(ChatColor.RED).append(formatTime(cd2));
        } else {
            actionBar.append(ChatColor.GREEN).append("READY");
        }

        player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(actionBar.toString()));
    }

    /**
     * Get combat time from combat-logger plugin if player is in combat
     */
    private String getCombatTime(Player player) {
        try {
            org.bukkit.plugin.Plugin combatPlugin = Bukkit.getPluginManager().getPlugin("CombatLogger");
            if (combatPlugin != null && combatPlugin.isEnabled()) {
                com.alterSMP.combatlogger.CombatLoggerPlugin clPlugin = (com.alterSMP.combatlogger.CombatLoggerPlugin) combatPlugin;
                com.alterSMP.combatlogger.CombatManager combatManager = clPlugin.getCombatManager();
                if (combatManager.isInCombat(player) && combatManager.isCombatTimerEnabled(player.getUniqueId())) {
                    int remaining = combatManager.getRemainingCombatTime(player.getUniqueId());
                    return remaining + "s";
                }
            }
        } catch (Exception e) {
            // Combat logger plugin not present or incompatible
        }
        return null;
    }

    private String formatTime(int seconds) {
        if (seconds >= 3600) {
            int hours = seconds / 3600;
            int mins = (seconds % 3600) / 60;
            return hours + "h " + mins + "m";
        } else if (seconds >= 60) {
            int mins = seconds / 60;
            int secs = seconds % 60;
            return mins + "m " + secs + "s";
        } else {
            return seconds + "s";
        }
    }

    private String[] getAbilityNames(LegendaryType type) {
        switch (type) {
            case HOLY_MOONLIGHT_SWORD:
                return new String[]{"Star Rift Slash", "Moonfall"};
            case PHEONIX_GRACE:
                return new String[]{"Flame Harvest", "Fire Rebirth"};
            case TEMPESTBREAKER_SPEAR:
                return new String[]{"Storm Surge", "Thunderclap"};
            case THOUSAND_DEMON_DAGGERS:
                return new String[]{"Soul Mark", "Shadow Step"};
            case DIVINE_AXE_RHITTA:
                return new String[]{"Nature Grasp", "Verdant Cyclone"};
            case CHAINS_OF_ETERNITY:
                return new String[]{"Soul Bind", "Prison of Damned"};
            case CELESTIAL_AEGIS_SHIELD:
                return new String[]{"Radiant Block", "Heaven's Wall"};
            case CHRONO_BLADE:
                return new String[]{"Echo Strike", "Time Rewind"};
            case SOUL_DEVOURER:
                return new String[]{"Void Slice", "Void Rift"};
            case DRAGONBORN_BLADE:
                return new String[]{"End Sever", "Dragon Dash"};
            case FORGE_PICKAXE:
                return new String[]{"3x3 Toggle", "Enchant Switch"};
            case RIFT_KEY_OF_ENDKEEPER:
                return new String[]{"End Rift", "End Rift"};
            case CHAOS_DICE_OF_FATE:
                return new String[]{"Roll Dice", "Player Scan"};
            default:
                return new String[]{"Ability 1", "Ability 2"};
        }
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

    public void clearAllCooldowns(UUID playerUUID) {
        Map<String, Long> playerCooldowns = cooldowns.get(playerUUID);
        if (playerCooldowns != null) {
            playerCooldowns.clear();
        }
    }
}
