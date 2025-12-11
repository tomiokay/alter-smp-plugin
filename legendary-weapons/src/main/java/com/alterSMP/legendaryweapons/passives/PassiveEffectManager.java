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
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class PassiveEffectManager implements Listener {

    private final LegendaryWeaponsPlugin plugin;

    // Track hit counters for various passives
    private Map<UUID, Integer> bladeHitCounter; // Holy Moonlight Sword (unused - now moon phase)
    private Map<UUID, Integer> chronoHitCounter; // Chrono Blade freeze
    private Map<UUID, Integer> chainsHitCounter; // Chains of Eternity
    private Map<UUID, Long> lastIceCreateTime; // Copper Boots path cooldown

    // Track players killed for Soul Devourer (can only get 1 soul per unique player)
    private Map<UUID, Set<UUID>> soulDevourerKills; // killer -> set of victims

    // Dragonborn Blade heart stealing now uses HeartManager for persistence
    // (removed local dragonbornStolenHearts - was not persisted and caused duplicate steal bugs)

    // Cache for legendary IDs to avoid repeated PDC lookups (major performance optimization)
    private Map<UUID, String[]> legendaryCache; // [mainHand, boots, offhand, helmet]

    public PassiveEffectManager(LegendaryWeaponsPlugin plugin) {
        this.plugin = plugin;
        this.bladeHitCounter = new HashMap<>();
        this.chronoHitCounter = new HashMap<>();
        this.chainsHitCounter = new HashMap<>();
        this.lastIceCreateTime = new HashMap<>();
        this.soulDevourerKills = new HashMap<>();
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
        }.runTaskTimer(plugin, 0L, 30L); // Every 1.5 seconds - optimized for performance
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

        // Tick Forge Leggings passives (flame trail, attack speed, lava speed)
        if (plugin.getArmorPassivesListener() != null) {
            plugin.getArmorPassivesListener().tickForgeLeggings(player);
        }
    }

    private void applyMainHandPassive(Player player, String legendaryId) {
        LegendaryType type = LegendaryType.fromId(legendaryId);
        if (type == null) return;

        switch (type) {
            case HOLY_MOONLIGHT_SWORD:
                // Lunar Blessing - Moon Phase Buffs (applied passively while holding)
                long time = player.getWorld().getFullTime();
                int moonPhase = (int) ((time / 24000) % 8);

                if (moonPhase == 0) {
                    // Full Moon - Strength III
                    player.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, 50, 2, true, false));
                } else if (moonPhase == 1 || moonPhase == 7) {
                    // Waxing/Waning Gibbous - Speed I
                    player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 50, 0, true, false));
                } else if (moonPhase == 2 || moonPhase == 6) {
                    // First/Last Quarter - Strength I
                    player.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, 50, 0, true, false));
                }
                // Crescent (3, 5) and New Moon (4) - no buff
                break;

            case TEMPESTBREAKER_SPEAR:
                // Passive: Lightning strike on trident hit (handled in AbilityManager)
                break;

            case THOUSAND_DEMON_DAGGERS:
                // Shadow Presence - Speed III while sneaking
                if (player.isSneaking()) {
                    player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 50, 2, true, false));
                }
                break;

            case DIVINE_AXE_RHITTA:
                // Nature Channel - Regen 3 on natural blocks
                Block below = player.getLocation().subtract(0, 1, 0).getBlock();
                Material belowType = below.getType();
                if (belowType == Material.GRASS_BLOCK || belowType.name().contains("LOG") ||
                    belowType.name().contains("LEAVES")) {
                    player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 50, 2, true, false));
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
                        target.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, 50, 0, true, false));
                    }
                }
                break;

            case PHEONIX_GRACE:
                // Heat Shield - Permanent Fire Resistance
                player.addPotionEffect(new PotionEffect(PotionEffectType.FIRE_RESISTANCE, 50, 0, true, false));
                break;

            case CHAINS_OF_ETERNITY:
                // Eternal Resilience - Resistance I while holding
                player.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, 50, 0, true, false));
                break;
        }
    }

    private void applyBootsPassive(Player player, String legendaryId) {
        LegendaryType type = LegendaryType.fromId(legendaryId);
        if (type == null) return;

        if (type == LegendaryType.FORGE_BOOTS) {
            // Featherfall - No fall damage (handled in event listener)
            // Permanent Speed II
            player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 50, 1, true, false));
        }
    }

    private void applyHelmetPassive(Player player, String legendaryId) {
        LegendaryType type = LegendaryType.fromId(legendaryId);
        if (type == null) return;

        if (type == LegendaryType.FORGE_HELMET) {
            // Water Mobility - Dolphin's Grace + Conduit Power
            player.addPotionEffect(new PotionEffect(PotionEffectType.DOLPHINS_GRACE, 50, 0, true, false));
            player.addPotionEffect(new PotionEffect(PotionEffectType.CONDUIT_POWER, 50, 0, true, false));
        }
    }

    private void applyOffhandPassive(Player player, String legendaryId) {
        LegendaryType type = LegendaryType.fromId(legendaryId);
        if (type == null) return;

        if (type == LegendaryType.CELESTIAL_AEGIS_SHIELD) {
            // Aura of Protection - Self and trusted allies gain Resistance I
            // Apply to self first
            player.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, 50, 0, true, false));

            // Apply to nearby trusted players
            for (Entity entity : player.getNearbyEntities(5, 5, 5)) {
                if (entity instanceof Player) {
                    Player ally = (Player) entity;
                    // Trust check - only help trusted allies
                    if (!plugin.getTrustManager().isTrusted(player, ally)) {
                        continue;
                    }
                    ally.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, 50, 0, true, false));
                }
            }
        }

        if (type == LegendaryType.CHAINS_OF_ETERNITY) {
            // Eternal Resilience - Resistance I while holding in offhand
            player.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, 50, 0, true, false));
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

    @EventHandler(priority = org.bukkit.event.EventPriority.MONITOR, ignoreCancelled = false)
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
                // Waxing/Waning Gibbous - Speed I
                player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 40, 0, true, false));
            } else if (moonPhase == 2 || moonPhase == 6) {
                // First/Last Quarter - Strength I
                player.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, 40, 0, true, false));
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

        // Melee attack particles removed

    }

    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        // Soul Collector and Heart Steal only work on PLAYER kills
        if (!(event.getEntity() instanceof Player)) return;

        Player killer = event.getEntity().getKiller();
        if (killer == null) return;

        // Don't count self-kills
        if (killer.equals(event.getEntity())) return;

        ItemStack mainHand = killer.getInventory().getItemInMainHand();
        String legendaryId = LegendaryItemFactory.getLegendaryId(mainHand);

        if (legendaryId != null && legendaryId.equals(LegendaryType.SOUL_DEVOURER.getId())) {
            Player victim = (Player) event.getEntity();
            UUID killerId = killer.getUniqueId();
            UUID victimId = victim.getUniqueId();

            // Check if this player was already killed for a soul
            Set<UUID> killedPlayers = soulDevourerKills.computeIfAbsent(killerId, k -> new HashSet<>());
            if (killedPlayers.contains(victimId)) {
                killer.sendMessage(ChatColor.GRAY + "You already collected " + victim.getName() + "'s soul.");
                return;
            }

            // Soul Collector - Increase soul count (max 5 souls, 1 per unique player)
            int currentSouls = LegendaryItemFactory.getSoulCount(mainHand);
            if (currentSouls < 5) {
                killedPlayers.add(victimId); // Mark this player as killed
                LegendaryItemFactory.setSoulCount(mainHand, currentSouls + 1);
                // Put the modified item back into the inventory to persist the change
                killer.getInventory().setItemInMainHand(mainHand);
                killer.sendMessage(ChatColor.DARK_PURPLE + "Soul collected from " + victim.getName() + ": " +
                    ChatColor.LIGHT_PURPLE + (currentSouls + 1) + "/5");
            }
        }

        // Dragonborn Blade - Heart Steal: Permanently steal 1 heart from victim (max 5 hearts)
        // Uses HeartManager for persistence (survives server restarts)
        if (legendaryId != null && legendaryId.equals(LegendaryType.DRAGONBORN_BLADE.getId())) {
            Player victim = (Player) event.getEntity();
            UUID killerId = killer.getUniqueId();
            UUID victimId = victim.getUniqueId();

            // Trust check - don't steal hearts from trusted players
            if (plugin.getTrustManager().isTrusted(killer, victim)) {
                return;
            }

            // Check if already stolen from this victim (by this killer)
            if (plugin.getHeartManager().hasAlreadyStolenFrom(killerId, victimId)) {
                killer.sendMessage(ChatColor.GRAY + "You already stole " + victim.getName() + "'s heart.");
                return;
            }

            // Check if ANY killer has already stolen from this victim (each player can only lose 1 heart total)
            if (plugin.getHeartManager().hasAnyoneStolenFrom(victimId)) {
                killer.sendMessage(ChatColor.GRAY + victim.getName() + "'s heart was already stolen by someone else.");
                return;
            }

            // Check if at max (5 hearts stolen)
            if (plugin.getHeartManager().getHeartsStolen(killerId) >= 5) {
                killer.sendMessage(ChatColor.GRAY + "You have already stolen the maximum 5 hearts.");
                return;
            }

            // Steal the heart using HeartManager (handles persistence)
            boolean success = plugin.getHeartManager().onPlayerKill(killer, victim);
            if (!success) {
                killer.sendMessage(ChatColor.GRAY + "Could not steal heart from " + victim.getName() + ".");
                return;
            }

            int currentStolen = plugin.getHeartManager().getHeartsStolen(killerId);

            // Visual and sound feedback
            killer.getWorld().spawnParticle(Particle.HEART, killer.getLocation().add(0, 2, 0), 10, 0.5, 0.3, 0.5, 0);
            killer.getWorld().spawnParticle(Particle.REVERSE_PORTAL, killer.getLocation().add(0, 1, 0), 30, 0.5, 0.5, 0.5, 0.3);
            killer.getWorld().playSound(killer.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 0.8f, 1.5f);

            // Victim effects
            victim.getWorld().spawnParticle(Particle.DAMAGE_INDICATOR, victim.getLocation().add(0, 1, 0), 10, 0.3, 0.5, 0.3, 0);

            killer.sendMessage(ChatColor.DARK_PURPLE + "Heart Steal! " + ChatColor.RED + "+1 heart" +
                ChatColor.GRAY + " stolen from " + victim.getName() + " (" + currentStolen + "/5)");
            victim.sendMessage(ChatColor.DARK_RED + "Your heart was stolen by " + killer.getName() + "! (-1 max heart)");
        }

    }

    // Heart penalty is now handled by HeartManager for persistence

    // ========== SOUL DEVOURER DEATH PENALTY ==========

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        UUID playerId = player.getUniqueId();

        // Check if player was holding Soul Devourer - lose 1 soul on death
        ItemStack mainHand = player.getInventory().getItemInMainHand();
        String legendaryId = LegendaryItemFactory.getLegendaryId(mainHand);

        if (legendaryId != null && legendaryId.equals(LegendaryType.SOUL_DEVOURER.getId())) {
            int currentSouls = LegendaryItemFactory.getSoulCount(mainHand);
            if (currentSouls > 0) {
                LegendaryItemFactory.setSoulCount(mainHand, currentSouls - 1);
                player.getInventory().setItemInMainHand(mainHand);
                player.sendMessage(ChatColor.DARK_PURPLE + "A soul escaped... (" + (currentSouls - 1) + "/5 remaining)");
            }
        }

        // Dragonborn Blade Heart Steal - Return all stolen hearts when killer dies
        // HeartManager handles persistence and heart restoration
        if (plugin.getHeartManager().getHeartsStolen(playerId) > 0) {
            plugin.getHeartManager().onHolderDeath(player);
            player.sendMessage(ChatColor.DARK_RED + "All stolen hearts have been returned!");
        }
    }

    // Called when a player joins to apply their heart modifier
    @EventHandler
    public void onPlayerJoin(org.bukkit.event.player.PlayerJoinEvent event) {
        Player player = event.getPlayer();
        // Apply heart modifier in case they had hearts stolen while offline
        Bukkit.getScheduler().runTaskLater(plugin, () -> plugin.getHeartManager().onPlayerJoin(player), 20L);
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
