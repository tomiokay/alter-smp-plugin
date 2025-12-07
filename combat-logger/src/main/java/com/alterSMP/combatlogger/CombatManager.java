package com.alterSMP.combatlogger;

import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRiptideEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.entity.EntityToggleGlideEvent;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class CombatManager implements Listener {

    private final CombatLoggerPlugin plugin;
    private final Map<UUID, Long> combatTagged = new HashMap<>();
    private final Map<UUID, Long> riptideCooldown = new HashMap<>();
    private static final long COMBAT_TAG_DURATION = 20000; // 20 seconds
    private static final long RIPTIDE_COOLDOWN = 15000; // 15 seconds

    public CombatManager(CombatLoggerPlugin plugin) {
        this.plugin = plugin;
        startActionBarTask();
    }

    /**
     * Start the action bar display task
     */
    private void startActionBarTask() {
        new BukkitRunnable() {
            @Override
            public void run() {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    if (isInCombat(player)) {
                        int remaining = getRemainingCombatTime(player.getUniqueId());
                        String barColor = remaining > 10 ? "§c" : (remaining > 5 ? "§6" : "§4");
                        String message = barColor + "⚔ COMBAT: " + remaining + "s ⚔";
                        player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(message));
                    }
                }
            }
        }.runTaskTimer(plugin, 0L, 10L); // Every 0.5 seconds
    }

    /**
     * Check if a player is currently in combat
     */
    public boolean isInCombat(Player player) {
        return isInCombat(player.getUniqueId());
    }

    public boolean isInCombat(UUID playerId) {
        Long tagTime = combatTagged.get(playerId);
        if (tagTime == null) return false;

        if (System.currentTimeMillis() - tagTime > COMBAT_TAG_DURATION) {
            combatTagged.remove(playerId);
            // Notify player combat ended
            Player player = Bukkit.getPlayer(playerId);
            if (player != null && player.isOnline()) {
                player.sendMessage(ChatColor.GREEN + "✓ You are no longer in combat.");
            }
            return false;
        }
        return true;
    }

    /**
     * Get remaining combat time in seconds
     */
    public int getRemainingCombatTime(UUID playerId) {
        Long tagTime = combatTagged.get(playerId);
        if (tagTime == null) return 0;

        long remaining = COMBAT_TAG_DURATION - (System.currentTimeMillis() - tagTime);
        return remaining > 0 ? (int) Math.ceil(remaining / 1000.0) : 0;
    }

    /**
     * Tag a player as in combat
     */
    public void tagPlayer(Player player) {
        boolean wasTagged = isInCombat(player);
        combatTagged.put(player.getUniqueId(), System.currentTimeMillis());

        if (!wasTagged) {
            player.sendMessage(ChatColor.RED + "⚔ You are now in combat! Don't log out for 20 seconds.");
        }
    }

    /**
     * Remove combat tag from player
     */
    public void untagPlayer(UUID playerId) {
        combatTagged.remove(playerId);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerDamage(EntityDamageByEntityEvent event) {
        Player attacker = null;
        Player victim = null;

        // Get attacker
        if (event.getDamager() instanceof Player) {
            attacker = (Player) event.getDamager();
        } else if (event.getDamager() instanceof org.bukkit.entity.Projectile) {
            org.bukkit.entity.Projectile proj = (org.bukkit.entity.Projectile) event.getDamager();
            if (proj.getShooter() instanceof Player) {
                attacker = (Player) proj.getShooter();
            }
        }

        // Get victim
        if (event.getEntity() instanceof Player) {
            victim = (Player) event.getEntity();
        }

        // Tag both players
        if (attacker != null && victim != null && !attacker.equals(victim)) {
            tagPlayer(attacker);
            tagPlayer(victim);
        }
    }

    // ========== COMBAT LOGGING ==========

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();

        if (isInCombat(player)) {
            // Kill the player for combat logging
            player.setHealth(0);
            plugin.getServer().broadcastMessage(
                ChatColor.RED + "☠ " + ChatColor.WHITE + player.getName() +
                ChatColor.RED + " combat logged and was killed!"
            );
        }

        combatTagged.remove(player.getUniqueId());
        riptideCooldown.remove(player.getUniqueId());
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        // Remove combat tag on death
        combatTagged.remove(event.getEntity().getUniqueId());
        riptideCooldown.remove(event.getEntity().getUniqueId());
    }

    // ========== ENDER PEARL BLOCKING ==========

    @EventHandler(priority = EventPriority.HIGH)
    public void onEnderPearlTeleport(PlayerTeleportEvent event) {
        if (event.getCause() != PlayerTeleportEvent.TeleportCause.ENDER_PEARL) return;

        Player player = event.getPlayer();
        if (isInCombat(player)) {
            event.setCancelled(true);
            player.sendMessage(ChatColor.RED + "✗ You cannot use ender pearls while in combat!");
        }
    }

    // ========== ELYTRA BLOCKING ==========

    @EventHandler(priority = EventPriority.HIGH)
    public void onElytraGlide(EntityToggleGlideEvent event) {
        if (!(event.getEntity() instanceof Player)) return;
        if (!event.isGliding()) return; // Only block starting to glide

        Player player = (Player) event.getEntity();
        if (isInCombat(player)) {
            event.setCancelled(true);
            player.sendMessage(ChatColor.RED + "✗ You cannot use elytra while in combat!");
        }
    }

    // ========== RIPTIDE COOLDOWN ==========

    @EventHandler(priority = EventPriority.HIGH)
    public void onRiptide(PlayerRiptideEvent event) {
        Player player = event.getPlayer();

        if (!isInCombat(player)) return; // No restriction outside combat

        UUID playerId = player.getUniqueId();
        Long lastRiptide = riptideCooldown.get(playerId);
        long now = System.currentTimeMillis();

        if (lastRiptide != null && (now - lastRiptide) < RIPTIDE_COOLDOWN) {
            // Still on cooldown - cancel the riptide by stopping velocity
            long remaining = (RIPTIDE_COOLDOWN - (now - lastRiptide)) / 1000;
            player.sendMessage(ChatColor.RED + "✗ Riptide on cooldown! " + remaining + "s remaining");
            player.setVelocity(new org.bukkit.util.Vector(0, 0, 0));
            return;
        }

        // Set cooldown
        riptideCooldown.put(playerId, now);
        player.sendMessage(ChatColor.YELLOW + "⚠ Riptide used! 15s cooldown in combat.");
    }
}
