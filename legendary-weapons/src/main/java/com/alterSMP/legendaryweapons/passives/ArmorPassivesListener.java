package com.alterSMP.legendaryweapons.passives;

import com.alterSMP.legendaryweapons.LegendaryWeaponsPlugin;
import com.alterSMP.legendaryweapons.items.LegendaryItemFactory;
import com.alterSMP.legendaryweapons.items.LegendaryType;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.util.*;

public class ArmorPassivesListener implements Listener {

    private final LegendaryWeaponsPlugin plugin;

    // Forge Chestplate - hit counter
    private Map<UUID, Integer> hitCounter = new HashMap<>();

    // Forge Leggings - flame trail tracking
    private Map<UUID, Location> lastFlameLocation = new HashMap<>();
    private Set<UUID> shockwaveActive = new HashSet<>(); // Prevent shockwave damage from counting as hits

    // Cache for leggings legendary ID to avoid PDC lookups every tick
    private Map<UUID, String> leggingsCache = new HashMap<>();

    // Forge Boots - falling state
    private Set<UUID> isFalling = new HashSet<>();
    private Map<UUID, Double> fallDistance = new HashMap<>();

    // Forge Helmet - health boost tracking
    private Map<UUID, Double> originalMaxHealth = new HashMap<>();

    // Lantern of Lost Names - kill tracking (persistent across sessions via AbilityManager)
    // Players that holder has killed - they become visible
    private Map<UUID, Set<UUID>> lanternKillTracker = new HashMap<>();
    // Players currently hidden from lantern holders
    private Map<UUID, Set<UUID>> hiddenFromPlayer = new HashMap<>();

    public ArmorPassivesListener(LegendaryWeaponsPlugin plugin) {
        this.plugin = plugin;

        // Start lantern visibility update task
        startLanternVisibilityTask();
    }

    // ========== FORGE BOOTS ==========

    @EventHandler(priority = EventPriority.HIGH)
    public void onFallDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player)) return;
        if (event.getCause() != EntityDamageEvent.DamageCause.FALL) return;

        Player player = (Player) event.getEntity();
        ItemStack boots = player.getInventory().getBoots();
        String legendaryId = LegendaryItemFactory.getLegendaryId(boots);

        if (legendaryId != null && legendaryId.equals(LegendaryType.FORGE_BOOTS.getId())) {
            // Passive: No fall damage
            event.setCancelled(true);

            // If they were falling and shifting, they already did meteor slam
            // Just remove from tracking
            isFalling.remove(player.getUniqueId());
            fallDistance.remove(player.getUniqueId());
        }
    }

    @EventHandler
    public void onPlayerSneak(PlayerToggleSneakEvent event) {
        if (!event.isSneaking()) return; // Only when starting to sneak

        Player player = event.getPlayer();
        ItemStack boots = player.getInventory().getBoots();
        String legendaryId = LegendaryItemFactory.getLegendaryId(boots);

        if (legendaryId != null && legendaryId.equals(LegendaryType.FORGE_BOOTS.getId())) {
            // Check if player is in the air
            if (!player.isOnGround() && player.getVelocity().getY() < 0) {
                // Player is falling and pressed shift - METEOR SLAM!
                double currentFallDist = player.getFallDistance();

                // Boost downward velocity
                Vector velocity = player.getVelocity();
                velocity.setY(-2.0); // Fast downward slam
                player.setVelocity(velocity);

                // Track that they're doing meteor slam
                isFalling.add(player.getUniqueId());
                fallDistance.put(player.getUniqueId(), currentFallDist);

                // Particles
                player.getWorld().spawnParticle(Particle.CLOUD, player.getLocation(), 30, 0.3, 0.3, 0.3, 0.1);
                player.playSound(player.getLocation(), Sound.ENTITY_ENDER_DRAGON_FLAP, 1.0f, 0.5f);
            }
        }
    }

    @EventHandler
    public void onPlayerLand(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player)) return;
        if (event.getCause() != EntityDamageEvent.DamageCause.FALL) return;

        Player player = (Player) event.getEntity();

        // Check if they were doing meteor slam
        if (isFalling.contains(player.getUniqueId())) {
            ItemStack boots = player.getInventory().getBoots();
            String legendaryId = LegendaryItemFactory.getLegendaryId(boots);

            if (legendaryId != null && legendaryId.equals(LegendaryType.FORGE_BOOTS.getId())) {
                event.setCancelled(true); // No fall damage

                double dist = player.getFallDistance();

                // Calculate damage like vanilla mace: base 6 + (fall_distance - 1.5) * 3, UNCAPPED
                double baseDamage = 6.0;
                if (dist > 1.5) {
                    baseDamage += (dist - 1.5) * 3.0;
                }
                // No damage cap - fall from high = massive damage

                // Area attack
                Location loc = player.getLocation();
                double radius = 4.0;

                // Epic particles
                player.getWorld().spawnParticle(Particle.EXPLOSION, loc, 3, 0.5, 0.1, 0.5);
                player.getWorld().spawnParticle(Particle.CLOUD, loc, 50, 2, 0.5, 2, 0.1);
                player.getWorld().spawnParticle(Particle.CRIT, loc, 100, 2, 0.5, 2, 0.3);

                // Sound
                player.getWorld().playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, 2.0f, 0.8f);
                player.getWorld().playSound(loc, Sound.ENTITY_LIGHTNING_BOLT_IMPACT, 1.5f, 1.2f);

                // Damage nearby entities
                for (LivingEntity entity : loc.getWorld().getNearbyLivingEntities(loc, radius)) {
                    if (entity.equals(player)) continue;
                    if (entity instanceof Player) {
                        Player target = (Player) entity;
                        // Check trust
                        if (plugin.getTrustManager().isTrusted(player, target)) {
                            continue;
                        }
                    }

                    // Apply damage (reduced with distance)
                    double distance = entity.getLocation().distance(loc);
                    double damageMultiplier = 1.0 - (distance / radius);
                    double finalDamage = baseDamage * damageMultiplier;

                    if (finalDamage > 0) {
                        entity.damage(finalDamage, player);

                        // No knockback as specified
                        if (entity instanceof Player) {
                            ((Player) entity).sendMessage(ChatColor.RED + "⚡ Hit by " + ChatColor.GOLD + "Meteor Slam" +
                                ChatColor.RED + " from " + player.getName() + "!");
                        }
                    }
                }

                player.sendMessage(ChatColor.GOLD + "Meteor Slam! " + ChatColor.YELLOW +
                    String.format("%.1f", baseDamage) + " damage (" + String.format("%.1f", dist) + " blocks)");

                // Clean up
                isFalling.remove(player.getUniqueId());
                fallDistance.remove(player.getUniqueId());
            }
        }
    }

    // ========== FORGE CHESTPLATE ==========

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerMeleeHit(EntityDamageByEntityEvent event) {
        if (event.isCancelled()) return;
        if (!(event.getDamager() instanceof Player)) return;
        if (!(event.getEntity() instanceof LivingEntity)) return;

        // Only count melee attacks (direct entity damage, not projectiles)
        if (event.getCause() != EntityDamageEvent.DamageCause.ENTITY_ATTACK &&
            event.getCause() != EntityDamageEvent.DamageCause.ENTITY_SWEEP_ATTACK) {
            return;
        }

        Player player = (Player) event.getDamager();
        LivingEntity target = (LivingEntity) event.getEntity();

        // Ignore if this is lightning damage from the chestplate
        if (shockwaveActive.contains(player.getUniqueId())) {
            return;
        }

        // Ignore if this is ability damage (not actual melee hits)
        if (plugin.getAbilityManager().isAbilityDamageActive(player.getUniqueId())) {
            return;
        }

        ItemStack chestplate = player.getInventory().getChestplate();
        String legendaryId = LegendaryItemFactory.getLegendaryId(chestplate);

        if (legendaryId != null && legendaryId.equals(LegendaryType.FORGE_CHESTPLATE.getId())) {
            // Increment hit counter
            int hits = hitCounter.getOrDefault(player.getUniqueId(), 0) + 1;

            if (hits >= 10) {
                // Trigger lightning strike on target
                triggerLightningStrike(player, target);
                hitCounter.put(player.getUniqueId(), 0); // Reset counter
            } else {
                hitCounter.put(player.getUniqueId(), hits);
            }
        }
    }

    private void triggerLightningStrike(Player player, LivingEntity target) {
        // Mark as active to prevent counting lightning damage
        shockwaveActive.add(player.getUniqueId());

        Location targetLoc = target.getLocation();

        // Spawn 3 visual lightning bolts (no damage from the lightning itself)
        target.getWorld().strikeLightningEffect(targetLoc);
        target.getWorld().strikeLightningEffect(targetLoc.clone().add(1, 0, 0));
        target.getWorld().strikeLightningEffect(targetLoc.clone().add(-1, 0, 1));

        // Deal ~2.5 hearts through full prot 4 diamond (12 raw damage)
        target.damage(12.0, player);

        // Particles for extra effect
        target.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, targetLoc.add(0, 1, 0), 100, 1, 1, 1, 0.3);

        if (target instanceof Player) {
            ((Player) target).sendMessage(ChatColor.YELLOW + "⚡ Struck by " + ChatColor.GOLD + "Forge Chestplate Lightning Storm" +
                ChatColor.YELLOW + " from " + player.getName() + "!");
        }

        // Remove active flag
        shockwaveActive.remove(player.getUniqueId());
    }

    // ========== FORGE LEGGINGS ==========

    @EventHandler(priority = EventPriority.HIGH)
    public void onFireDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player)) return;

        EntityDamageEvent.DamageCause cause = event.getCause();

        // Check for fire/lava damage types
        if (cause != EntityDamageEvent.DamageCause.FIRE &&
            cause != EntityDamageEvent.DamageCause.FIRE_TICK &&
            cause != EntityDamageEvent.DamageCause.LAVA &&
            cause != EntityDamageEvent.DamageCause.HOT_FLOOR) {
            return;
        }

        Player player = (Player) event.getEntity();
        ItemStack leggings = player.getInventory().getLeggings();
        String legendaryId = LegendaryItemFactory.getLegendaryId(leggings);

        if (legendaryId != null && legendaryId.equals(LegendaryType.FORGE_LEGGINGS.getId())) {
            // Immune to fire, lava, and magma
            event.setCancelled(true);
            player.setFireTicks(0); // Extinguish any fire
        }
    }

    /**
     * Called by PassiveEffectManager tick to handle Forge Leggings passives
     */
    public void tickForgeLeggings(Player player) {
        // Always check leggings directly - no caching issues
        ItemStack leggings = player.getInventory().getLeggings();
        String legendaryId = LegendaryItemFactory.getLegendaryId(leggings);

        if (legendaryId == null || !legendaryId.equals(LegendaryType.FORGE_LEGGINGS.getId())) {
            return;
        }

        // Haste II always
        player.addPotionEffect(new PotionEffect(PotionEffectType.HASTE, 60, 1, true, false));
        // Fire Resistance always
        player.addPotionEffect(new PotionEffect(PotionEffectType.FIRE_RESISTANCE, 60, 0, true, false));
    }

    /**
     * Invalidate leggings cache for a player (call when inventory changes)
     */
    public void invalidateLeggingsCache(UUID playerId) {
        leggingsCache.remove(playerId);
    }

    // ========== FORGE HELMET ==========

    @EventHandler
    public void onEntityKill(EntityDeathEvent event) {
        // Only trigger on PLAYER kills, not all entity kills
        if (!(event.getEntity() instanceof Player)) return;

        Player killer = event.getEntity().getKiller();
        if (killer == null) return;

        // Don't count self-kills (suicide)
        if (killer.equals(event.getEntity())) return;

        ItemStack helmet = killer.getInventory().getHelmet();
        String legendaryId = LegendaryItemFactory.getLegendaryId(helmet);

        if (legendaryId != null && legendaryId.equals(LegendaryType.FORGE_HELMET.getId())) {
            UUID killerId = killer.getUniqueId();

            // Store original max health if not already stored
            if (!originalMaxHealth.containsKey(killerId)) {
                originalMaxHealth.put(killerId, killer.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH).getBaseValue());
            }

            // Cancel any existing health restore task
            if (originalMaxHealth.containsKey(killerId)) {
                // Set max health to +10 HP (5 hearts)
                double originalMax = originalMaxHealth.get(killerId);
                killer.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH).setBaseValue(originalMax + 10.0);

                // Heal the player by 10 HP
                double newHealth = Math.min(killer.getHealth() + 10.0, originalMax + 10.0);
                killer.setHealth(newHealth);
            }

            killer.sendMessage(ChatColor.DARK_RED + "Blood Harvest! " + ChatColor.RED + "+5 hearts for 5 minutes");

            // Particles
            killer.getWorld().spawnParticle(Particle.DUST, killer.getLocation().add(0, 1, 0), 50, 0.5, 0.5, 0.5,
                new Particle.DustOptions(Color.fromRGB(139, 0, 0), 1.5f));

            // Restore health after 5 minutes (6000 ticks)
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                if (killer.isOnline() && originalMaxHealth.containsKey(killerId)) {
                    double originalMax = originalMaxHealth.get(killerId);

                    // Restore original max health
                    killer.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH).setBaseValue(originalMax);

                    // Adjust current health if it exceeds the new max
                    if (killer.getHealth() > originalMax) {
                        killer.setHealth(originalMax);
                    }

                    originalMaxHealth.remove(killerId);
                    killer.sendMessage(ChatColor.RED + "Blood Harvest expired.");
                }
            }, 6000L); // 5 minutes = 300 seconds = 6000 ticks
        }
    }

    @EventHandler
    public void onCriticalHit(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player)) return;

        Player player = (Player) event.getDamager();
        ItemStack helmet = player.getInventory().getHelmet();
        String legendaryId = LegendaryItemFactory.getLegendaryId(helmet);

        if (legendaryId != null && legendaryId.equals(LegendaryType.FORGE_HELMET.getId())) {
            // Check if it's a critical hit (player must be falling)
            if (player.getFallDistance() > 0 && player.getVelocity().getY() < 0) {
                // Grant 10% speed for 3 seconds (Speed I = 20%, so we use level 0 with amplifier tricks)
                // Actually Speed I is the minimum, so let's use Speed I
                player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 60, 0)); // 3 seconds, Speed I

                // Particles
                player.getWorld().spawnParticle(Particle.CRIT, event.getEntity().getLocation(), 20, 0.3, 0.5, 0.3, 0);
            }
        }
    }

    // ========== BLOOD HARVEST DEATH HANDLER ==========

    @EventHandler
    public void onPlayerDeathBloodHarvest(PlayerDeathEvent event) {
        Player player = event.getEntity();
        UUID playerId = player.getUniqueId();

        // Check if player has Blood Harvest active (extra HP)
        if (originalMaxHealth.containsKey(playerId)) {
            double originalMax = originalMaxHealth.get(playerId);

            // Restore original max health on death
            player.getAttribute(Attribute.MAX_HEALTH).setBaseValue(originalMax);
            originalMaxHealth.remove(playerId);
        }
    }

    // ========== LANTERN OF LOST NAMES ==========
    // NEW LOGIC: Holder is invisible to OTHER players (not the other way around)
    // - Holder is invisible to players they HAVEN'T killed
    // - Attacking reveals holder to that player for 5 minutes
    // - After killing a player, that player can always see the holder

    // Track when holder last attacked each player (for 5 min reveal timer)
    private Map<UUID, Map<UUID, Long>> lanternAttackTimers = new HashMap<>();

    private void startLanternVisibilityTask() {
        // Run every 40 ticks (2 seconds) to update visibility - reduced from 10 ticks for performance
        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            for (Player holder : Bukkit.getOnlinePlayers()) {
                // Only update if they're holding a lantern (skip others for performance)
                if (isHoldingLantern(holder)) {
                    updateLanternVisibility(holder);
                }
            }
        }, 20L, 40L);
    }

    private boolean isHoldingLantern(Player player) {
        ItemStack offhand = player.getInventory().getItemInOffHand();
        ItemStack mainhand = player.getInventory().getItemInMainHand();
        String offhandId = LegendaryItemFactory.getLegendaryId(offhand);
        String mainhandId = LegendaryItemFactory.getLegendaryId(mainhand);
        return (offhandId != null && offhandId.equals(LegendaryType.LANTERN_OF_LOST_NAMES.getId()))
                || (mainhandId != null && mainhandId.equals(LegendaryType.LANTERN_OF_LOST_NAMES.getId()));
    }

    private void updateLanternVisibility(Player holder) {
        UUID holderId = holder.getUniqueId();
        boolean holdingLantern = isHoldingLantern(holder);

        Set<UUID> killed = lanternKillTracker.computeIfAbsent(holderId, k -> new HashSet<>());
        Map<UUID, Long> attackTimers = lanternAttackTimers.computeIfAbsent(holderId, k -> new HashMap<>());
        Set<UUID> currentlyHiddenFrom = hiddenFromPlayer.computeIfAbsent(holderId, k -> new HashSet<>());

        if (!holdingLantern) {
            // Not holding lantern - show holder to everyone
            for (UUID viewerId : new HashSet<>(currentlyHiddenFrom)) {
                Player viewer = Bukkit.getPlayer(viewerId);
                if (viewer != null && viewer.isOnline()) {
                    viewer.showPlayer(plugin, holder);
                }
            }
            currentlyHiddenFrom.clear();
            return;
        }

        // Holding lantern - hide holder from players who haven't been killed by holder
        long now = System.currentTimeMillis();

        for (Player viewer : Bukkit.getOnlinePlayers()) {
            if (viewer.equals(holder)) continue;

            UUID viewerId = viewer.getUniqueId();

            // Check if holder has killed this viewer (permanent visibility)
            boolean hasKilledViewer = killed.contains(viewerId);

            // Check if holder recently attacked this viewer (5 min reveal)
            Long attackTime = attackTimers.get(viewerId);
            boolean recentlyAttacked = attackTime != null && (now - attackTime) < 5 * 60 * 1000;

            // Check if they're trusted
            boolean isTrusted = plugin.getTrustManager().isTrusted(holder, viewer);

            if (hasKilledViewer || recentlyAttacked || isTrusted) {
                // Viewer CAN see holder
                if (currentlyHiddenFrom.contains(viewerId)) {
                    viewer.showPlayer(plugin, holder);
                    currentlyHiddenFrom.remove(viewerId);
                }
            } else {
                // Viewer CANNOT see holder (holder is invisible to them)
                if (!currentlyHiddenFrom.contains(viewerId)) {
                    viewer.hidePlayer(plugin, holder);
                    currentlyHiddenFrom.add(viewerId);
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onLanternHolderAttack(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player)) return;
        if (!(event.getEntity() instanceof Player)) return;

        Player attacker = (Player) event.getDamager();
        Player victim = (Player) event.getEntity();

        // Check if attacker is holding the Lantern
        if (!isHoldingLantern(attacker)) return;

        // Record attack time - victim can now see attacker for 5 minutes
        UUID attackerId = attacker.getUniqueId();
        UUID victimId = victim.getUniqueId();

        Map<UUID, Long> attackTimers = lanternAttackTimers.computeIfAbsent(attackerId, k -> new HashMap<>());
        attackTimers.put(victimId, System.currentTimeMillis());

        // Immediately reveal to victim
        Set<UUID> hiddenFrom = hiddenFromPlayer.get(attackerId);
        if (hiddenFrom != null && hiddenFrom.remove(victimId)) {
            victim.showPlayer(plugin, attacker);
        }

        attacker.sendMessage(ChatColor.GRAY + "You've revealed yourself to " + victim.getName() + " for 5 minutes!");
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerKillForLantern(EntityDeathEvent event) {
        if (!(event.getEntity() instanceof Player)) return;

        Player victim = (Player) event.getEntity();
        Player killer = victim.getKiller();
        if (killer == null) return;

        // Check if killer is holding the Lantern of Lost Names
        if (!isHoldingLantern(killer)) return;

        // Record this kill - victim can now ALWAYS see the killer
        Set<UUID> killed = lanternKillTracker.computeIfAbsent(killer.getUniqueId(), k -> new HashSet<>());
        killed.add(victim.getUniqueId());

        // Clear any attack timer since it's now permanent
        Map<UUID, Long> attackTimers = lanternAttackTimers.get(killer.getUniqueId());
        if (attackTimers != null) {
            attackTimers.remove(victim.getUniqueId());
        }

        // Dramatic effect
        killer.getWorld().spawnParticle(Particle.SOUL, victim.getLocation().add(0, 1, 0), 30, 0.5, 0.5, 0.5, 0.1);
        killer.playSound(killer.getLocation(), Sound.ENTITY_WARDEN_HEARTBEAT, 1.0f, 0.5f);
        killer.sendMessage(ChatColor.DARK_AQUA + "✦ " + ChatColor.AQUA + victim.getName() +
                ChatColor.GRAY + " can now always see you.");
    }

    @EventHandler
    public void onPlayerJoinForLantern(org.bukkit.event.player.PlayerJoinEvent event) {
        Player joiningPlayer = event.getPlayer();

        // Run one tick later to ensure player is fully loaded
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            // Update visibility for lantern holders only (skip others for performance)
            for (Player holder : Bukkit.getOnlinePlayers()) {
                if (holder.equals(joiningPlayer)) continue;
                if (isHoldingLantern(holder)) {
                    updateLanternVisibility(holder);
                }
            }
            // Also update if the joining player is holding a lantern
            if (isHoldingLantern(joiningPlayer)) {
                updateLanternVisibility(joiningPlayer);
            }
        }, 2L);
    }

    @EventHandler
    public void onPlayerQuitForLantern(org.bukkit.event.player.PlayerQuitEvent event) {
        UUID playerId = event.getPlayer().getUniqueId();

        // Clean up hidden tracking for this player
        hiddenFromPlayer.remove(playerId);
        lanternAttackTimers.remove(playerId);

        // Clean up leggings cache
        leggingsCache.remove(playerId);

        // Remove this player from others' hidden lists and attack timers
        for (Set<UUID> hidden : hiddenFromPlayer.values()) {
            hidden.remove(playerId);
        }
        for (Map<UUID, Long> timers : lanternAttackTimers.values()) {
            timers.remove(playerId);
        }
    }

    // Invalidate leggings cache when inventory changes
    @EventHandler
    public void onInventoryClick(org.bukkit.event.inventory.InventoryClickEvent event) {
        if (event.getWhoClicked() instanceof Player) {
            // Invalidate cache after a tick to ensure inventory is updated
            UUID playerId = event.getWhoClicked().getUniqueId();
            Bukkit.getScheduler().runTaskLater(plugin, () -> leggingsCache.remove(playerId), 1L);
        }
    }

    /**
     * Get the set of players that a lantern holder has killed.
     */
    public Set<UUID> getLanternKills(UUID holderId) {
        return lanternKillTracker.getOrDefault(holderId, new HashSet<>());
    }

    /**
     * Add a kill to a lantern holder's record.
     */
    public void addLanternKill(UUID holderId, UUID victimId) {
        lanternKillTracker.computeIfAbsent(holderId, k -> new HashSet<>()).add(victimId);
    }
}
