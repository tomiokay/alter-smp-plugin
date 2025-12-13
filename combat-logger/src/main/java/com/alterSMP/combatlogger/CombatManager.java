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
import org.bukkit.event.enchantment.PrepareItemEnchantEvent;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class CombatManager implements Listener {

    private final CombatLoggerPlugin plugin;
    private final Map<UUID, Long> combatTagged = new HashMap<>();
    private final Map<UUID, Long> riptideCooldown = new HashMap<>();
    private final Map<UUID, Long> maceCooldown = new HashMap<>();
    private final java.util.Set<UUID> combatTimerDisabled = new java.util.HashSet<>();
    private boolean pvpEnabled = true;

    // Config values
    private long combatTagDuration;
    private long riptideCooldownTime;
    private long maceCooldownTime;

    public CombatManager(CombatLoggerPlugin plugin) {
        this.plugin = plugin;
        loadConfig();
        startActionBarTask();
    }

    private void loadConfig() {
        combatTagDuration = plugin.getConfig().getLong("combat-duration", 30) * 1000;
        riptideCooldownTime = plugin.getConfig().getLong("riptide-cooldown", 15) * 1000;
        maceCooldownTime = plugin.getConfig().getLong("mace-cooldown", 60) * 1000; // 1 minute default
    }

    /**
     * Start the action bar display task
     */
    private void startActionBarTask() {
        new BukkitRunnable() {
            @Override
            public void run() {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    if (!isCombatTimerEnabled(player.getUniqueId())) continue;

                    if (isInCombat(player.getUniqueId())) {
                        int remaining = getRemainingCombatTime(player.getUniqueId());
                        String message = ChatColor.RED + "⚔ Combat: " + ChatColor.WHITE + remaining + "s";
                        player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(message));
                    }
                }
            }
        }.runTaskTimer(plugin, 0L, 20L); // Every second
    }

    /**
     * Toggle combat timer display for a player
     */
    public boolean toggleCombatTimer(UUID playerId) {
        if (combatTimerDisabled.contains(playerId)) {
            combatTimerDisabled.remove(playerId);
            return true; // Now enabled
        } else {
            combatTimerDisabled.add(playerId);
            return false; // Now disabled
        }
    }

    /**
     * Check if combat timer is enabled for a player
     */
    public boolean isCombatTimerEnabled(UUID playerId) {
        return !combatTimerDisabled.contains(playerId);
    }

    /**
     * Toggle PvP on/off globally
     */
    public boolean togglePvP() {
        pvpEnabled = !pvpEnabled;
        return pvpEnabled;
    }

    /**
     * Check if PvP is enabled
     */
    public boolean isPvPEnabled() {
        return pvpEnabled;
    }

    /**
     * Set PvP state
     */
    public void setPvPEnabled(boolean enabled) {
        this.pvpEnabled = enabled;
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

        if (System.currentTimeMillis() - tagTime > combatTagDuration) {
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

        long remaining = combatTagDuration - (System.currentTimeMillis() - tagTime);
        return remaining > 0 ? (int) Math.ceil(remaining / 1000.0) : 0;
    }

    /**
     * Tag a player as in combat
     */
    public void tagPlayer(Player player) {
        boolean wasTagged = isInCombat(player);
        combatTagged.put(player.getUniqueId(), System.currentTimeMillis());

        if (!wasTagged) {
            int seconds = (int) (combatTagDuration / 1000);
            player.sendMessage(ChatColor.RED + "⚔ You are now in combat! Don't log out for " + seconds + " seconds.");
        }
    }

    /**
     * Remove combat tag from player
     */
    public void untagPlayer(UUID playerId) {
        combatTagged.remove(playerId);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPvPDamage(EntityDamageByEntityEvent event) {
        if (pvpEnabled) return; // PvP is on, don't block

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

        // Block PvP damage if both are players
        if (attacker != null && victim != null && !attacker.equals(victim)) {
            event.setCancelled(true);
        }
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
        maceCooldown.remove(player.getUniqueId());
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        // Remove combat tag on death
        combatTagged.remove(event.getEntity().getUniqueId());
        riptideCooldown.remove(event.getEntity().getUniqueId());
        maceCooldown.remove(event.getEntity().getUniqueId());
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

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onElytraGlide(EntityToggleGlideEvent event) {
        if (!(event.getEntity() instanceof Player)) return;
        if (!event.isGliding()) return; // Only block starting to glide

        Player player = (Player) event.getEntity();
        if (isInCombat(player)) {
            event.setCancelled(true);
            player.sendMessage(ChatColor.RED + "✗ You cannot use elytra while in combat!");

            // Force stop gliding and kill momentum to prevent bunnyhopping
            new BukkitRunnable() {
                int ticks = 0;
                @Override
                public void run() {
                    if (ticks >= 5 || !player.isOnline()) {
                        cancel();
                        return;
                    }
                    // Force stop gliding
                    if (player.isGliding()) {
                        player.setGliding(false);
                    }
                    // Kill horizontal momentum to prevent bunnyhopping
                    org.bukkit.util.Vector vel = player.getVelocity();
                    player.setVelocity(new org.bukkit.util.Vector(vel.getX() * 0.3, vel.getY(), vel.getZ() * 0.3));
                    ticks++;
                }
            }.runTaskTimer(plugin, 0L, 1L);
        }
    }

    // ========== RIPTIDE COOLDOWN ==========

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onRiptide(PlayerRiptideEvent event) {
        Player player = event.getPlayer();
        ItemStack trident = event.getItem();

        // Only apply cooldown if the trident has Riptide enchantment
        if (trident == null || !trident.containsEnchantment(org.bukkit.enchantments.Enchantment.RIPTIDE)) {
            return;
        }

        if (!isInCombat(player)) return; // No restriction outside combat

        UUID playerId = player.getUniqueId();
        Long lastRiptide = riptideCooldown.get(playerId);
        long now = System.currentTimeMillis();

        if (lastRiptide != null && (now - lastRiptide) < riptideCooldownTime) {
            // Still on cooldown - cancel the riptide by stopping velocity
            long remaining = (riptideCooldownTime - (now - lastRiptide)) / 1000;
            player.sendMessage(ChatColor.RED + "✗ Riptide on cooldown! " + remaining + "s remaining");

            // Cancel velocity for several ticks to fully stop riptide
            new BukkitRunnable() {
                int ticks = 0;
                @Override
                public void run() {
                    if (ticks >= 10 || !player.isOnline()) {
                        cancel();
                        return;
                    }
                    player.setVelocity(new org.bukkit.util.Vector(0, -0.0784, 0)); // Slight gravity
                    ticks++;
                }
            }.runTaskTimer(plugin, 0L, 1L);
            return;
        }

        // Set cooldown
        riptideCooldown.put(playerId, now);
        int cooldownSeconds = (int) (riptideCooldownTime / 1000);

        // Note: We don't set Material.TRIDENT cooldown anymore as it affects ALL tridents
        // The internal cooldown tracking is sufficient

        player.sendMessage(ChatColor.YELLOW + "⚠ Riptide used! " + cooldownSeconds + "s cooldown in combat.");
    }

    // ========== MACE COOLDOWN ==========

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onMaceAttack(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player)) return;

        Player player = (Player) event.getDamager();
        if (player.getInventory().getItemInMainHand().getType() != Material.MACE) return;

        UUID playerId = player.getUniqueId();
        Long lastMace = maceCooldown.get(playerId);
        long now = System.currentTimeMillis();

        if (lastMace != null && (now - lastMace) < maceCooldownTime) {
            // Still on cooldown - cancel the attack
            long remaining = (maceCooldownTime - (now - lastMace)) / 1000;
            event.setCancelled(true);
            player.sendMessage(ChatColor.RED + "✗ Mace on cooldown! " + remaining + "s remaining");
            return;
        }

        // Set cooldown on successful hit
        maceCooldown.put(playerId, now);
        int cooldownSeconds = (int) (maceCooldownTime / 1000);

        // Set visual cooldown on the mace (like ender pearls)
        int cooldownTicks = cooldownSeconds * 20; // Convert seconds to ticks
        player.setCooldown(Material.MACE, cooldownTicks);

        player.sendMessage(ChatColor.YELLOW + "⚠ Mace used! " + cooldownSeconds + "s cooldown.");
    }

    // ========== MACE UNENCHANTABLE ==========

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onEnchantMace(PrepareItemEnchantEvent event) {
        if (event.getItem().getType() == Material.MACE) {
            event.setCancelled(true);
            event.getEnchanter().sendMessage(ChatColor.RED + "✗ Maces cannot be enchanted!");
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onAnvilMace(PrepareAnvilEvent event) {
        if (event.getInventory().getFirstItem() != null &&
            event.getInventory().getFirstItem().getType() == Material.MACE) {
            event.setResult(null);
            // Can't send message here easily, but the result being null prevents it
        }
        if (event.getInventory().getSecondItem() != null &&
            event.getInventory().getSecondItem().getType() == Material.MACE) {
            event.setResult(null);
        }
    }
}
