package com.alterSMP.legendaryweapons.passives;

import com.alterSMP.legendaryweapons.LegendaryWeaponsPlugin;
import com.alterSMP.legendaryweapons.items.LegendaryItemFactory;
import com.alterSMP.legendaryweapons.items.LegendaryType;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PassiveEffectManager implements Listener {

    private final LegendaryWeaponsPlugin plugin;

    // Track hit counters for various passives
    private Map<UUID, Integer> bladeHitCounter; // Holy Moonlight Sword (unused - now moon phase)
    private Map<UUID, Integer> chronoHitCounter; // Chrono Blade freeze
    private Map<UUID, Integer> chainsHitCounter; // Chains of Eternity
    private Map<UUID, Long> lastIceCreateTime; // Copper Boots path cooldown

    // Cache for legendary IDs to avoid repeated PDC lookups (major performance optimization)
    private Map<UUID, String[]> legendaryCache; // [mainHand, boots, offhand, helmet]

    public PassiveEffectManager(LegendaryWeaponsPlugin plugin) {
        this.plugin = plugin;
        this.bladeHitCounter = new HashMap<>();
        this.chronoHitCounter = new HashMap<>();
        this.chainsHitCounter = new HashMap<>();
        this.lastIceCreateTime = new HashMap<>();
        this.legendaryCache = new HashMap<>();

        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    public void startPassiveTask() {
        new BukkitRunnable() {
            @Override
            public void run() {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    applyPassiveEffects(player);
                }
            }
        }.runTaskTimer(plugin, 0L, 20L); // Every 1 second (optimized from 0.5s)
    }

    /**
     * Update the legendary cache for a player. Call this when inventory changes.
     */
    public void updateCache(Player player) {
        String[] cached = new String[4];
        cached[0] = LegendaryItemFactory.getLegendaryId(player.getInventory().getItemInMainHand());
        cached[1] = LegendaryItemFactory.getLegendaryId(player.getInventory().getBoots());
        cached[2] = LegendaryItemFactory.getLegendaryId(player.getInventory().getItemInOffHand());
        cached[3] = LegendaryItemFactory.getLegendaryId(player.getInventory().getHelmet());
        legendaryCache.put(player.getUniqueId(), cached);
    }

    /**
     * Clear cache when player disconnects.
     */
    public void clearCache(UUID playerId) {
        legendaryCache.remove(playerId);
    }

    private void applyPassiveEffects(Player player) {
        // Use cached values or update cache if not present
        String[] cached = legendaryCache.get(player.getUniqueId());
        if (cached == null) {
            updateCache(player);
            cached = legendaryCache.get(player.getUniqueId());
        }

        String mainLegendary = cached[0];
        String bootsLegendary = cached[1];
        String offhandLegendary = cached[2];
        String helmetLegendary = cached[3];

        // Apply passives based on legendary held/worn
        if (mainLegendary != null) {
            applyMainHandPassive(player, mainLegendary);
        }

        if (bootsLegendary != null) {
            applyBootsPassive(player, bootsLegendary);
        }

        if (offhandLegendary != null) {
            applyOffhandPassive(player, offhandLegendary);
        }

        if (helmetLegendary != null) {
            applyHelmetPassive(player, helmetLegendary);
        }

        // Tick Copper Leggings passives (flame trail, attack speed, lava speed)
        if (plugin.getArmorPassivesListener() != null) {
            plugin.getArmorPassivesListener().tickCopperLeggings(player);
        }
    }

    private void applyMainHandPassive(Player player, String legendaryId) {
        LegendaryType type = LegendaryType.fromId(legendaryId);
        if (type == null) return;

        switch (type) {
            case TEMPESTBREAKER_SPEAR:
                // Passive: Lightning strike on trident hit (handled in AbilityManager)
                break;

            case THOUSAND_DEMON_DAGGERS:
                // Shadow Presence - Speed III while sneaking
                if (player.isSneaking()) {
                    player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 40, 2, true, false));
                }
                break;

            case DIVINE_AXE_RHITTA:
                // Nature Channel - Regen 3 on natural blocks
                Block below = player.getLocation().subtract(0, 1, 0).getBlock();
                Material belowType = below.getType();
                if (belowType == Material.GRASS_BLOCK || belowType.name().contains("LOG") ||
                    belowType.name().contains("LEAVES")) {
                    player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 40, 2, true, false));
                }
                break;

            case CHRONO_BLADE:
                // Time Slow passive is handled on-hit in AbilityManager
                break;

            case DRAGONBORN_BLADE:
                // Dragon's Gaze - Nearby enemies glow (30 block radius)
                for (Entity entity : player.getNearbyEntities(30, 30, 30)) {
                    if (entity instanceof Player) {
                        Player target = (Player) entity;
                        // Trust check
                        if (plugin.getTrustManager().isTrusted(player, target)) {
                            continue;
                        }
                        target.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, 40, 0, true, false));
                    }
                }
                break;
        }
    }

    private void applyBootsPassive(Player player, String legendaryId) {
        LegendaryType type = LegendaryType.fromId(legendaryId);
        if (type == null) return;

        if (type == LegendaryType.COPPER_BOOTS) {
            // Featherfall - No fall damage (handled in event listener)
            // Permanent Speed II
            player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 40, 1, true, false));
        }
    }

    private void applyHelmetPassive(Player player, String legendaryId) {
        LegendaryType type = LegendaryType.fromId(legendaryId);
        if (type == null) return;

        if (type == LegendaryType.COPPER_HELMET) {
            // Water Mobility - Dolphin's Grace + Conduit Power
            player.addPotionEffect(new PotionEffect(PotionEffectType.DOLPHINS_GRACE, 40, 0, true, false));
            player.addPotionEffect(new PotionEffect(PotionEffectType.CONDUIT_POWER, 40, 0, true, false));
        }
    }

    private void applyOffhandPassive(Player player, String legendaryId) {
        LegendaryType type = LegendaryType.fromId(legendaryId);
        if (type == null) return;

        if (type == LegendaryType.CELESTIAL_AEGIS_SHIELD) {
            // Aura of Protection - Self and trusted allies gain Resistance I
            // Apply to self first
            player.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, 40, 0, true, false));

            // Apply to nearby trusted players
            for (Entity entity : player.getNearbyEntities(5, 5, 5)) {
                if (entity instanceof Player) {
                    Player ally = (Player) entity;
                    // Trust check - only help trusted allies
                    if (!plugin.getTrustManager().isTrusted(player, ally)) {
                        continue;
                    }
                    ally.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, 40, 0, true, false));
                }
            }
        }
    }

    // Event handlers for combat-based passives

    @EventHandler
    public void onEntityDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player)) {
            return;
        }

        Player player = (Player) event.getEntity();
        ItemStack mainHand = player.getInventory().getItemInMainHand();
        String legendaryId = LegendaryItemFactory.getLegendaryId(mainHand);

        if (legendaryId != null) {
            LegendaryType type = LegendaryType.fromId(legendaryId);

            if (type == LegendaryType.PHEONIX_GRACE) {
                // Heat Shield - Immune to fire and explosions
                EntityDamageEvent.DamageCause cause = event.getCause();
                if (cause == EntityDamageEvent.DamageCause.FIRE ||
                    cause == EntityDamageEvent.DamageCause.FIRE_TICK ||
                    cause == EntityDamageEvent.DamageCause.LAVA ||
                    cause == EntityDamageEvent.DamageCause.HOT_FLOOR ||
                    cause == EntityDamageEvent.DamageCause.BLOCK_EXPLOSION ||
                    cause == EntityDamageEvent.DamageCause.ENTITY_EXPLOSION) {
                    event.setCancelled(true);
                }
            }
        }
    }

    @EventHandler
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player)) {
            return;
        }

        // IMPORTANT: Only trigger passives on MELEE attacks, not abilities
        EntityDamageEvent.DamageCause cause = event.getCause();
        if (cause != EntityDamageEvent.DamageCause.ENTITY_ATTACK &&
            cause != EntityDamageEvent.DamageCause.ENTITY_SWEEP_ATTACK) {
            return; // Not a melee attack, don't trigger passives
        }

        Player player = (Player) event.getDamager();
        ItemStack mainHand = player.getInventory().getItemInMainHand();
        String legendaryId = LegendaryItemFactory.getLegendaryId(mainHand);

        if (legendaryId == null) return;

        LegendaryType type = LegendaryType.fromId(legendaryId);
        if (type == null) return;

        // Thousand Demon Daggers - Soul Mark processing
        if (type == LegendaryType.THOUSAND_DEMON_DAGGERS && event.getEntity() instanceof LivingEntity) {
            LivingEntity target = (LivingEntity) event.getEntity();
            // Trust check
            if (target instanceof Player && plugin.getTrustManager().isTrusted(player, (Player) target)) {
                // Don't process soul mark on trusted players
            } else {
                plugin.getAbilityManager().processSoulMark(player, target);
            }
        }

        // Holy Moonlight Sword - Moon Phase Buffs
        if (type == LegendaryType.HOLY_MOONLIGHT_SWORD) {
            // Get moon phase (0 = full moon, 4 = new moon)
            long time = player.getWorld().getFullTime();
            int moonPhase = (int) ((time / 24000) % 8);

            // Apply effects based on moon phase
            if (moonPhase == 0) {
                // Full Moon - Strength III + white hit effect
                player.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, 40, 2, true, false));

                // White particle effect on hit
                if (event.getEntity() instanceof LivingEntity) {
                    Location hitLoc = event.getEntity().getLocation().add(0, 1, 0);
                    player.getWorld().spawnParticle(Particle.END_ROD, hitLoc, 15, 0.3, 0.3, 0.3, 0.1);
                    player.getWorld().spawnParticle(Particle.FIREWORK, hitLoc, 8, 0.2, 0.2, 0.2, 0.05);
                }
            } else if (moonPhase == 1 || moonPhase == 7) {
                // Waxing/Waning Gibbous - Strength I
                player.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, 40, 0, true, false));
            } else if (moonPhase == 2 || moonPhase == 6) {
                // First/Last Quarter - Speed I
                player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 40, 0, true, false));
            }
            // Crescent (3, 5) and New Moon (4) - no buff
        }

        // Chrono Blade - Freeze every 20th hit
        if (type == LegendaryType.CHRONO_BLADE) {
            int count = chronoHitCounter.getOrDefault(player.getUniqueId(), 0) + 1;
            chronoHitCounter.put(player.getUniqueId(), count);

            if (count >= 20) {
                chronoHitCounter.put(player.getUniqueId(), 0);

                if (event.getEntity() instanceof LivingEntity) {
                    LivingEntity target = (LivingEntity) event.getEntity();

                    // Trust check
                    if (target instanceof Player && plugin.getTrustManager().isTrusted(player, (Player) target)) {
                        return;
                    }

                    // Freeze for 3 seconds - complete immobilization
                    target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 60, 255)); // Can't move
                    target.addPotionEffect(new PotionEffect(PotionEffectType.JUMP_BOOST, 60, 128)); // Can't jump (negative)
                    target.addPotionEffect(new PotionEffect(PotionEffectType.MINING_FATIGUE, 60, 255)); // Can't mine/attack
                    target.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, 60, 255)); // No damage

                    // Visual time freeze effect
                    target.getWorld().spawnParticle(Particle.END_ROD, target.getLocation().add(0, 1, 0), 40, 0.5, 1, 0.5, 0.02);
                    target.getWorld().spawnParticle(Particle.PORTAL, target.getLocation().add(0, 1, 0), 60, 0.4, 0.8, 0.4, 0.1);
                    target.getWorld().spawnParticle(Particle.REVERSE_PORTAL, target.getLocation().add(0, 1, 0), 30, 0.3, 0.5, 0.3, 0.05);
                    player.getWorld().playSound(target.getLocation(), Sound.BLOCK_RESPAWN_ANCHOR_DEPLETE, 1.0f, 0.5f);

                    player.sendMessage(ChatColor.AQUA + "Time Freeze triggered!");
                    if (target instanceof Player) {
                        ((Player) target).sendMessage(ChatColor.AQUA + "You have been frozen in time!");
                    }
                }
            }
        }

    }

    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        // Soul Collector only works on PLAYER kills
        if (!(event.getEntity() instanceof Player)) return;

        Player killer = event.getEntity().getKiller();
        if (killer == null) return;

        // Don't count self-kills
        if (killer.equals(event.getEntity())) return;

        ItemStack mainHand = killer.getInventory().getItemInMainHand();
        String legendaryId = LegendaryItemFactory.getLegendaryId(mainHand);

        if (legendaryId != null && legendaryId.equals(LegendaryType.SOUL_DEVOURER.getId())) {
            // Soul Collector - Increase soul count (max 5 souls)
            int currentSouls = LegendaryItemFactory.getSoulCount(mainHand);
            if (currentSouls < 5) {
                LegendaryItemFactory.setSoulCount(mainHand, currentSouls + 1);
                // Put the modified item back into the inventory to persist the change
                killer.getInventory().setItemInMainHand(mainHand);
                killer.sendMessage(ChatColor.DARK_PURPLE + "Soul collected: " +
                    ChatColor.LIGHT_PURPLE + (currentSouls + 1) + "/5");
            }
        }

    }

    // ========== CACHE INVALIDATION EVENTS ==========

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        clearCache(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onItemHeld(PlayerItemHeldEvent event) {
        // Invalidate cache when player switches hotbar slot
        Bukkit.getScheduler().runTaskLater(plugin, () -> updateCache(event.getPlayer()), 1L);
    }

    @EventHandler
    public void onSwapHands(PlayerSwapHandItemsEvent event) {
        // Invalidate cache when player swaps main/offhand
        Bukkit.getScheduler().runTaskLater(plugin, () -> updateCache(event.getPlayer()), 1L);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        // Invalidate cache when player clicks in inventory (armor/equipment changes)
        if (event.getWhoClicked() instanceof Player) {
            Player player = (Player) event.getWhoClicked();
            Bukkit.getScheduler().runTaskLater(plugin, () -> updateCache(player), 1L);
        }
    }
}
