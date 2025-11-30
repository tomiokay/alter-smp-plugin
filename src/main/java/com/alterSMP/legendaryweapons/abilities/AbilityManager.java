package com.alterSMP.legendaryweapons.abilities;

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
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

import java.util.*;

public class AbilityManager implements Listener {

    private final LegendaryWeaponsPlugin plugin;

    // Ability state tracking
    private Map<UUID, Long> fireRebirthActive; // Fire Rebirth activation times
    private Set<UUID> soulMarkReady; // Players ready to mark next hit
    private Map<UUID, UUID> soulMarkTargets; // Attacker UUID -> Marked target UUID
    private Map<UUID, Long> forestShieldActive; // Forest Shield mode
    private Map<UUID, Long> radiantBlockActive; // Radiant Block mode
    private Map<UUID, Long> echoStrikeActive; // Echo Strike mode
    private Map<UUID, SavedState> timeRewindStates; // Saved states for Time Rewind
    private Set<UUID> nextGaleThrow; // Next trident throw is Gale
    private Map<UUID, Set<UUID>> echoStrikeTargets; // Targets hit during Echo Strike
    private Map<UUID, Set<Location>> voidRiftBlocks; // Void Rift black hole blocks
    private Set<UUID> mining3x3Active; // Players with 3x3 mining enabled
    private Set<UUID> fortuneModeActive; // Players with Fortune mode (vs Silk Touch)
    private Set<UUID> shadowstepBackstab; // Next attack does bonus true damage
    private Map<UUID, HeavensWallBarrier> activeBarriers; // Heaven's Wall barriers
    private Map<UUID, Map<UUID, Long>> timeSlowCooldowns; // Chrono Blade passive cooldown per target
    private Set<UUID> timeDistortionActive; // Time Distortion bubble active
    private Set<Location> activePrisonBlocks; // Prison of the Damned blocks (unbreakable)

    public AbilityManager(LegendaryWeaponsPlugin plugin) {
        this.plugin = plugin;
        this.fireRebirthActive = new HashMap<>();
        this.soulMarkReady = new HashSet<>();
        this.soulMarkTargets = new HashMap<>();
        this.forestShieldActive = new HashMap<>();
        this.radiantBlockActive = new HashMap<>();
        this.echoStrikeActive = new HashMap<>();
        this.timeRewindStates = new HashMap<>();
        this.nextGaleThrow = new HashSet<>();
        this.echoStrikeTargets = new HashMap<>();
        this.voidRiftBlocks = new HashMap<>();
        this.mining3x3Active = new HashSet<>();
        this.fortuneModeActive = new HashSet<>();
        this.shadowstepBackstab = new HashSet<>();
        this.activeBarriers = new HashMap<>();
        this.timeSlowCooldowns = new HashMap<>();
        this.timeDistortionActive = new HashSet<>();
        this.activePrisonBlocks = new HashSet<>();

        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    /**
     * Check if player has a marked Chrono Shift position.
     * Used to allow re-casting to teleport back without cooldown.
     */
    public boolean hasChronoShiftMarked(UUID playerUUID) {
        return timeRewindStates.containsKey(playerUUID);
    }

    public boolean executeAbility(Player player, String legendaryId, int abilityNum) {
        LegendaryType type = LegendaryType.fromId(legendaryId);
        if (type == null) return false;

        // Get cooldown for this ability
        int cooldown = getCooldownForAbility(type, abilityNum);

        boolean success = false;

        // Execute the specific ability
        if (abilityNum == 1) {
            success = executeAbility1(player, type);
        } else {
            success = executeAbility2(player, type);
        }

        // Set cooldown if successful
        if (success) {
            plugin.getCooldownManager().setCooldown(player.getUniqueId(), legendaryId, abilityNum, cooldown);
        }

        return success;
    }

    private boolean executeAbility1(Player player, LegendaryType type) {
        switch (type) {
            case HOLY_MOONLIGHT_SWORD:
                return starRiftSlash(player);
            case PHEONIX_GRACE:
                return flameHarvest(player);
            case TEMPESTBREAKER_SPEAR:
                return galeThrow(player);
            case THOUSAND_DEMON_DAGGERS:
                return shadowstep(player);
            case DIVINE_AXE_RHITTA:
                return natureGrasp(player);
            case CHAINS_OF_ETERNITY:
                return soulBind(player);
            case CELESTIAL_AEGIS_SHIELD:
                return radiantBlock(player);
            case CHRONO_BLADE:
                return echoStrike(player);
            case SOUL_DEVOURER:
                return voidSlice(player);
            case CREATION_SPLITTER:
                return endSever(player);
            case COPPER_PICKAXE:
                return toggle3x3Mining(player);
        }
        return false;
    }

    private boolean executeAbility2(Player player, LegendaryType type) {
        switch (type) {
            case HOLY_MOONLIGHT_SWORD:
                return stargateBlink(player);
            case PHEONIX_GRACE:
                return fireRebirth(player);
            case TEMPESTBREAKER_SPEAR:
                return stormcall(player);
            case THOUSAND_DEMON_DAGGERS:
                return soulMark(player);
            case DIVINE_AXE_RHITTA:
                return forestShield(player);
            case CHAINS_OF_ETERNITY:
                return prisonOfDamned(player);
            case CELESTIAL_AEGIS_SHIELD:
                return heavensWall(player);
            case CHRONO_BLADE:
                return timeRewind(player);
            case SOUL_DEVOURER:
                return voidRift(player);
            case CREATION_SPLITTER:
                return cataclysmPulse(player);
            case COPPER_PICKAXE:
                return toggleEnchantMode(player);
        }
        return false;
    }

    // ========== HOLY MOONLIGHT SWORD ==========

    private boolean starRiftSlash(Player player) {
        Vector direction = player.getLocation().getDirection();
        Location start = player.getEyeLocation();

        for (int i = 1; i <= 30; i++) {
            Location point = start.clone().add(direction.clone().multiply(i));

            // Enhanced particles - much more visible
            player.getWorld().spawnParticle(Particle.END_ROD, point, 15, 0.3, 0.3, 0.3, 0.05);
            player.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, point, 8, 0.2, 0.2, 0.2, 0.1);
            player.getWorld().spawnParticle(Particle.ENCHANTED_HIT, point, 5, 0.2, 0.2, 0.2, 0);

            // Damage entities - increased for Prot 4
            for (Entity entity : point.getWorld().getNearbyEntities(point, 1, 1, 1)) {
                if (entity instanceof LivingEntity && entity != player) {
                    // Trust check
                    if (entity instanceof Player && plugin.getTrustManager().isTrusted(player, (Player) entity)) {
                        continue;
                    }
                    LivingEntity victim = (LivingEntity) entity;
                    victim.damage(35.0, player); // ~7 hearts through full Prot 4

                    // Notify victim
                    if (victim instanceof Player) {
                        ((Player) victim).sendMessage(ChatColor.RED + "⚔ Hit by " + ChatColor.AQUA + "Star Rift Slash" + ChatColor.RED + " from " + player.getName() + "!");
                    }
                }
            }
        }

        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1.0f, 0.5f);
        player.sendMessage(ChatColor.AQUA + "Star Rift Slash!");
        return true;
    }

    private boolean stargateBlink(Player player) {
        RayTraceResult result = player.getWorld().rayTraceBlocks(
            player.getEyeLocation(),
            player.getLocation().getDirection(),
            45
        );

        Location destination;
        if (result != null && result.getHitBlock() != null) {
            destination = result.getHitBlock().getLocation().add(0, 1, 0);
        } else {
            destination = player.getLocation().add(player.getLocation().getDirection().multiply(45));
        }

        // Find safe location
        while (destination.getBlock().getType().isSolid() && destination.getY() < 320) {
            destination.add(0, 1, 0);
        }

        // Enhanced particles at origin and destination
        player.getWorld().spawnParticle(Particle.PORTAL, player.getLocation(), 400, 0.5, 1, 0.5, 1);
        player.getWorld().spawnParticle(Particle.END_ROD, player.getLocation(), 60, 0.5, 1, 0.5, 0.1);
        player.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, player.getLocation(), 40, 0.5, 1, 0.5, 0.1);

        player.teleport(destination);

        player.getWorld().spawnParticle(Particle.PORTAL, destination, 400, 0.5, 1, 0.5, 1);
        player.getWorld().spawnParticle(Particle.END_ROD, destination, 60, 0.5, 1, 0.5, 0.1);
        player.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, destination, 40, 0.5, 1, 0.5, 0.1);

        player.playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.0f);
        player.sendMessage(ChatColor.AQUA + "Stargate Blink!");
        return true;
    }

    // ========== PHEONIX GRACE ==========

    private boolean flameHarvest(Player player) {
        int entitiesHit = 0;

        for (Entity entity : player.getNearbyEntities(6, 6, 6)) {
            if (entity instanceof LivingEntity) {
                // Trust check
                if (entity instanceof Player) {
                    if (plugin.getTrustManager().isTrusted(player, (Player) entity)) {
                        continue;
                    }
                }

                LivingEntity living = (LivingEntity) entity;

                // Calculate 40% of current HP
                double damage = living.getHealth() * 0.4;

                living.damage(damage, player);
                living.setFireTicks(100); // 5 seconds of fire
                entitiesHit++;

                // Enhanced particles
                living.getWorld().spawnParticle(Particle.FLAME, living.getLocation(), 80, 0.5, 0.5, 0.5, 0.1);
                living.getWorld().spawnParticle(Particle.LAVA, living.getLocation(), 15, 0.5, 0.5, 0.5, 0);
                living.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME, living.getLocation(), 30, 0.5, 0.5, 0.5, 0.05);

                // Notify victim
                if (living instanceof Player) {
                    ((Player) living).sendMessage(ChatColor.RED + "⚔ Hit by " + ChatColor.GOLD + "Flame Harvest" + ChatColor.RED + " from " + player.getName() + "!");
                }
            }
        }

        // Grant +6 absorption hearts per enemy hit (max 18 hearts = 3 enemies)
        if (entitiesHit > 0) {
            int absorptionHearts = Math.min(entitiesHit * 6, 18); // Max 18 hearts
            int absorptionLevel = (absorptionHearts / 2) - 1; // Level 0 = 2 hearts, Level 2 = 6 hearts, etc.
            absorptionLevel = Math.max(0, Math.min(absorptionLevel, 8)); // Cap at level 8 (18 hearts)
            player.addPotionEffect(new PotionEffect(PotionEffectType.ABSORPTION, 600, absorptionLevel));
            player.sendMessage(ChatColor.RED + "Flame Harvest! +" + absorptionHearts + " absorption hearts (" + entitiesHit + " enemies hit)");
        } else {
            player.sendMessage(ChatColor.RED + "Flame Harvest! No enemies hit.");
        }

        // Enhanced particles
        player.getWorld().spawnParticle(Particle.FLAME, player.getLocation(), 400, 3, 1, 3, 0.1);
        player.getWorld().spawnParticle(Particle.LAVA, player.getLocation(), 60, 3, 1, 3, 0);
        player.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME, player.getLocation(), 100, 3, 1, 3, 0.05);

        player.playSound(player.getLocation(), Sound.ENTITY_BLAZE_SHOOT, 1.0f, 0.8f);
        return true;
    }

    private boolean fireRebirth(Player player) {
        fireRebirthActive.put(player.getUniqueId(), System.currentTimeMillis() + 10000);

        // Enhanced particles
        player.getWorld().spawnParticle(Particle.FLAME, player.getLocation(), 300, 1, 1, 1, 0.2);
        player.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME, player.getLocation(), 150, 1, 1, 1, 0.1);
        player.getWorld().spawnParticle(Particle.LAVA, player.getLocation(), 50, 1, 1, 1, 0);

        player.sendMessage(ChatColor.GOLD + "Fire Rebirth activated for 10 seconds!");
        player.playSound(player.getLocation(), Sound.ITEM_TOTEM_USE, 1.0f, 0.8f);
        return true;
    }

    // ========== TEMPESTBREAKER SPEAR ==========

    private boolean galeThrow(Player player) {
        nextGaleThrow.add(player.getUniqueId());

        // Enhanced particles
        player.getWorld().spawnParticle(Particle.CLOUD, player.getLocation(), 200, 1, 1, 1, 0.1);
        player.getWorld().spawnParticle(Particle.SWEEP_ATTACK, player.getLocation(), 50, 1, 1, 1, 0);
        player.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, player.getLocation(), 40, 1, 1, 1, 0.1);

        player.sendMessage(ChatColor.AQUA + "Next trident throw will create a wind vortex!");
        return true;
    }

    private boolean stormcall(Player player) {
        // Stationary lightning storm radius around player
        Location center = player.getLocation();
        World world = player.getWorld();
        double radius = 8.0;

        Set<UUID> hitEntities = new HashSet<>();

        // Strike lightning multiple times in the radius over 2 seconds
        new BukkitRunnable() {
            int strikes = 0;

            @Override
            public void run() {
                if (strikes >= 8) {
                    cancel();
                    return;
                }

                // Random positions in radius for lightning
                for (int i = 0; i < 3; i++) {
                    double angle = Math.random() * Math.PI * 2;
                    double dist = Math.random() * radius;
                    Location strikePos = center.clone().add(
                        Math.cos(angle) * dist,
                        0,
                        Math.sin(angle) * dist
                    );

                    // Lightning effect
                    world.strikeLightningEffect(strikePos);

                    // Electric particles
                    world.spawnParticle(Particle.ELECTRIC_SPARK, strikePos, 80, 1, 2, 1, 0.3);
                    world.spawnParticle(Particle.FIREWORK, strikePos, 30, 0.5, 1, 0.5, 0.1);
                }

                // Damage all enemies in the radius
                for (Entity entity : world.getNearbyEntities(center, radius, radius, radius)) {
                    if (entity instanceof LivingEntity && entity != player) {
                        // Trust check
                        if (entity instanceof Player && plugin.getTrustManager().isTrusted(player, (Player) entity)) {
                            continue;
                        }
                        LivingEntity living = (LivingEntity) entity;

                        // Deal high damage (~6 hearts through prot 4 = ~30 raw damage per strike)
                        living.damage(30.0, player);
                        living.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 40, 2));

                        // Notify victim once
                        if (!hitEntities.contains(entity.getUniqueId())) {
                            hitEntities.add(entity.getUniqueId());
                            if (living instanceof Player) {
                                ((Player) living).sendMessage(ChatColor.RED + "⚔ Hit by " + ChatColor.YELLOW + "Stormcall" + ChatColor.RED + " from " + player.getName() + "!");
                            }
                        }
                    }
                }

                strikes++;
            }
        }.runTaskTimer(plugin, 0L, 5L); // Every 5 ticks for 2 seconds total

        // Initial particles showing the radius
        for (double angle = 0; angle < 360; angle += 10) {
            double rad = Math.toRadians(angle);
            Location edgePoint = center.clone().add(Math.cos(rad) * radius, 0.5, Math.sin(rad) * radius);
            world.spawnParticle(Particle.ELECTRIC_SPARK, edgePoint, 20, 0.2, 0.3, 0.2, 0.1);
        }

        world.playSound(center, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 2.0f, 0.8f);
        player.sendMessage(ChatColor.YELLOW + "Stormcall! Lightning storm for 2 seconds!");
        return true;
    }

    // ========== THOUSAND DEMON DAGGERS ==========

    private boolean shadowstep(Player player) {
        // Find target enemy in line of sight
        RayTraceResult result = player.getWorld().rayTraceEntities(
            player.getEyeLocation(),
            player.getLocation().getDirection(),
            15,
            entity -> entity instanceof LivingEntity && entity != player
        );

        if (result == null || !(result.getHitEntity() instanceof LivingEntity)) {
            player.sendMessage(ChatColor.RED + "No target found! Look at an enemy to shadowstep behind them.");
            return false;
        }

        LivingEntity target = (LivingEntity) result.getHitEntity();

        // Trust check
        if (target instanceof Player && plugin.getTrustManager().isTrusted(player, (Player) target)) {
            player.sendMessage(ChatColor.RED + "Cannot shadowstep behind trusted players!");
            return false;
        }

        // Calculate position behind the target
        Vector targetDirection = target.getLocation().getDirection().normalize();
        Location behindTarget = target.getLocation().clone().subtract(targetDirection.multiply(1.5));
        behindTarget.setY(target.getLocation().getY());

        // Make player face the target
        Vector toTarget = target.getLocation().toVector().subtract(behindTarget.toVector()).normalize();
        behindTarget.setDirection(toTarget);

        // Enhanced particles at origin
        player.getWorld().spawnParticle(Particle.LARGE_SMOKE, player.getLocation(), 120, 0.5, 1, 0.5, 0);
        player.getWorld().spawnParticle(Particle.SQUID_INK, player.getLocation(), 60, 0.5, 1, 0.5, 0.1);
        player.getWorld().spawnParticle(Particle.SMOKE, player.getLocation(), 80, 0.5, 1, 0.5, 0.05);

        player.teleport(behindTarget);

        // Enhanced particles at destination
        player.getWorld().spawnParticle(Particle.LARGE_SMOKE, behindTarget, 120, 0.5, 1, 0.5, 0);
        player.getWorld().spawnParticle(Particle.SQUID_INK, behindTarget, 60, 0.5, 1, 0.5, 0.1);
        player.getWorld().spawnParticle(Particle.SMOKE, behindTarget, 80, 0.5, 1, 0.5, 0.05);

        // Mark for backstab bonus damage
        shadowstepBackstab.add(player.getUniqueId());

        player.playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 0.6f);
        player.sendMessage(ChatColor.DARK_GRAY + "Shadowstep! Next attack deals bonus true damage!");

        // Remove backstab buff after 5 seconds if not used
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (shadowstepBackstab.remove(player.getUniqueId())) {
                player.sendMessage(ChatColor.GRAY + "Backstab bonus expired.");
            }
        }, 100L);

        return true;
    }

    private boolean soulMark(Player player) {
        // Activate soul mark - next melee hit will mark the target
        soulMarkReady.add(player.getUniqueId());

        // Enhanced particles
        player.getWorld().spawnParticle(Particle.SOUL, player.getLocation(), 150, 1, 1, 1, 0.05);
        player.getWorld().spawnParticle(Particle.ENCHANT, player.getLocation(), 100, 1, 1, 1, 0.1);
        player.getWorld().spawnParticle(Particle.WITCH, player.getLocation(), 50, 1, 1, 1, 0);

        player.sendMessage(ChatColor.DARK_PURPLE + "Soul Mark ready! Hit an enemy to mark them for bonus damage.");

        // Remove ready state after 10 seconds if not used
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (soulMarkReady.remove(player.getUniqueId())) {
                player.sendMessage(ChatColor.GRAY + "Soul Mark expired - no target hit.");
            }
        }, 200L);

        return true;
    }

    /**
     * Check if a player has Soul Mark ready and process marking.
     * Called from damage event - only for MELEE attacks!
     */
    public void processSoulMark(Player attacker, LivingEntity target) {
        // Check if attacker is ready to mark
        if (soulMarkReady.remove(attacker.getUniqueId())) {
            // Mark the target
            soulMarkTargets.put(attacker.getUniqueId(), target.getUniqueId());

            // Visual effect on marked target
            target.getWorld().spawnParticle(Particle.SOUL, target.getLocation().add(0, 1, 0), 100, 0.5, 0.5, 0.5, 0.1);
            target.getWorld().spawnParticle(Particle.WITCH, target.getLocation().add(0, 1, 0), 50, 0.5, 0.5, 0.5, 0.1);
            target.getWorld().playSound(target.getLocation(), Sound.ENTITY_WITHER_HURT, 1.0f, 1.5f);

            attacker.sendMessage(ChatColor.DARK_PURPLE + "Soul Mark applied! Your attacks deal +4 hearts true damage to this target.");
            if (target instanceof Player) {
                ((Player) target).sendMessage(ChatColor.DARK_PURPLE + "You have been Soul Marked by " + attacker.getName() + "!");
            }

            // Mark expires after 15 seconds
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (soulMarkTargets.remove(attacker.getUniqueId()) != null) {
                    attacker.sendMessage(ChatColor.GRAY + "Soul Mark on target expired.");
                }
            }, 300L);
            return;
        }

        // Check if target is marked by this attacker - deal bonus damage
        UUID markedTarget = soulMarkTargets.get(attacker.getUniqueId());
        if (markedTarget != null && markedTarget.equals(target.getUniqueId())) {
            // Deal 4 hearts TRUE damage (8 HP, bypasses armor)
            double currentHealth = target.getHealth();
            double newHealth = Math.max(0, currentHealth - 8.0);
            target.setHealth(newHealth);

            // Particles on marked hit
            target.getWorld().spawnParticle(Particle.SOUL, target.getLocation().add(0, 1, 0), 40, 0.3, 0.5, 0.3, 0.1);
        }
    }

    /**
     * Check if a player has an active soul mark on a target.
     */
    public boolean hasSoulMarkOn(UUID attackerUUID, UUID targetUUID) {
        UUID marked = soulMarkTargets.get(attackerUUID);
        return marked != null && marked.equals(targetUUID);
    }

    // ========== DIVINE AXE RHITTA ==========

    private boolean natureGrasp(Player player) {
        for (Entity entity : player.getNearbyEntities(6, 6, 6)) {
            if (entity instanceof LivingEntity) {
                // Trust check
                if (entity instanceof Player) {
                    if (plugin.getTrustManager().isTrusted(player, (Player) entity)) {
                        continue;
                    }
                }

                LivingEntity living = (LivingEntity) entity;

                living.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 40, 255));
                living.addPotionEffect(new PotionEffect(PotionEffectType.JUMP_BOOST, 40, 128));

                // Enhanced particles
                living.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, living.getLocation(), 120, 0.5, 1, 0.5, 0);
                living.getWorld().spawnParticle(Particle.COMPOSTER, living.getLocation(), 80, 0.5, 1, 0.5, 0);
                living.getWorld().spawnParticle(Particle.FALLING_SPORE_BLOSSOM, living.getLocation(), 40, 0.5, 1, 0.5, 0);

                // Notify victim
                if (living instanceof Player) {
                    ((Player) living).sendMessage(ChatColor.RED + "⚔ Hit by " + ChatColor.GREEN + "Nature Grasp" + ChatColor.RED + " from " + player.getName() + "!");
                }
            }
        }

        // Enhanced particles
        player.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, player.getLocation(), 400, 6, 1, 6, 0);
        player.getWorld().spawnParticle(Particle.COMPOSTER, player.getLocation(), 200, 6, 1, 6, 0);
        player.getWorld().spawnParticle(Particle.FALLING_SPORE_BLOSSOM, player.getLocation(), 100, 6, 1, 6, 0);

        player.playSound(player.getLocation(), Sound.BLOCK_GRASS_BREAK, 1.0f, 0.5f);
        player.sendMessage(ChatColor.GREEN + "Nature Grasp!");
        return true;
    }

    private boolean forestShield(Player player) {
        // Verdant Cyclone - 360° spin attack with leaves and wind
        Location center = player.getLocation();
        World world = center.getWorld();

        int enemiesHit = 0;

        // Hit all entities in 5 block radius
        for (Entity entity : world.getNearbyEntities(center, 5, 3, 5)) {
            if (entity instanceof LivingEntity && entity != player) {
                LivingEntity living = (LivingEntity) entity;

                // Trust check
                if (living instanceof Player && plugin.getTrustManager().isTrusted(player, (Player) living)) {
                    continue;
                }

                // Deal 2 hearts damage (4 HP)
                living.damage(4.0, player);

                // Push enemies back
                Vector knockback = living.getLocation().toVector().subtract(center.toVector()).normalize();
                knockback.setY(0.3); // Slight upward lift
                living.setVelocity(knockback.multiply(1.5));

                enemiesHit++;

                // Particles on each hit target
                world.spawnParticle(Particle.CHERRY_LEAVES, living.getLocation().add(0, 1, 0), 30, 0.3, 0.5, 0.3, 0.1);

                if (living instanceof Player) {
                    ((Player) living).sendMessage(ChatColor.RED + "⚔ Hit by " + ChatColor.GREEN + "Verdant Cyclone" + ChatColor.RED + " from " + player.getName() + "!");
                }
            }
        }

        // Massive spin particle effect - leaves and wind in a circle
        for (double angle = 0; angle < 360; angle += 10) {
            double radians = Math.toRadians(angle);
            for (double r = 1; r <= 5; r += 0.5) {
                double x = r * Math.cos(radians);
                double z = r * Math.sin(radians);
                Location particleLoc = center.clone().add(x, 0.5, z);
                world.spawnParticle(Particle.CHERRY_LEAVES, particleLoc, 3, 0.1, 0.2, 0.1, 0.05);
                world.spawnParticle(Particle.HAPPY_VILLAGER, particleLoc, 2, 0.1, 0.2, 0.1, 0);
            }
        }
        // Wind spiral effect
        world.spawnParticle(Particle.CLOUD, center.clone().add(0, 1, 0), 100, 2, 1, 2, 0.1);
        world.spawnParticle(Particle.SWEEP_ATTACK, center.clone().add(0, 1, 0), 20, 2, 0.5, 2, 0);

        world.playSound(center, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1.5f, 0.8f);
        world.playSound(center, Sound.ENTITY_WIND_CHARGE_WIND_BURST, 1.0f, 1.0f);

        player.sendMessage(ChatColor.GREEN + "Verdant Cyclone! Hit " + enemiesHit + " enemies!");
        return true;
    }

    // ========== CHAINS OF ETERNITY ==========

    private boolean soulBind(Player player) {
        RayTraceResult result = player.getWorld().rayTraceEntities(
            player.getEyeLocation(),
            player.getLocation().getDirection(),
            20,
            entity -> entity instanceof LivingEntity && entity != player
        );

        if (result != null && result.getHitEntity() instanceof LivingEntity) {
            LivingEntity target = (LivingEntity) result.getHitEntity();

            // Trust check
            if (target instanceof Player && plugin.getTrustManager().isTrusted(player, (Player) target)) {
                player.sendMessage(ChatColor.RED + "Cannot target trusted players!");
                return false;
            }

            // Calculate pull velocity - stop 1 block in front of player
            double distance = target.getLocation().distance(player.getLocation());
            double pullDistance = Math.max(0, distance - 1.5); // Stop 1.5 blocks away

            Vector direction = player.getLocation().toVector().subtract(target.getLocation().toVector()).normalize();
            // Pull strength based on distance - stronger pull for farther targets
            double pullStrength = Math.min(pullDistance * 0.5, 2.0);
            target.setVelocity(direction.multiply(pullStrength));

            // Deal damage (4 hearts through prot 4 = ~20 raw damage)
            target.damage(20.0, player);

            target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 60, 4));

            // Enhanced particles
            target.getWorld().spawnParticle(Particle.SOUL, target.getLocation(), 120, 0.5, 1, 0.5, 0);
            target.getWorld().spawnParticle(Particle.ENCHANT, target.getLocation(), 80, 0.5, 1, 0.5, 0.1);
            target.getWorld().spawnParticle(Particle.SMOKE, target.getLocation(), 60, 0.5, 1, 0.5, 0.05);

            // Notify victim
            if (target instanceof Player) {
                ((Player) target).sendMessage(ChatColor.RED + "⚔ Hit by " + ChatColor.DARK_GRAY + "Soul Bind" + ChatColor.RED + " from " + player.getName() + "!");
            }

            player.sendMessage(ChatColor.DARK_GRAY + "Soul Bind!");
            return true;
        }

        player.sendMessage(ChatColor.RED + "No target found!");
        return false;
    }

    private boolean prisonOfDamned(Player player) {
        RayTraceResult result = player.getWorld().rayTraceEntities(
            player.getEyeLocation(),
            player.getLocation().getDirection(),
            20,
            entity -> entity instanceof LivingEntity && entity != player
        );

        if (result != null && result.getHitEntity() instanceof LivingEntity) {
            LivingEntity target = (LivingEntity) result.getHitEntity();

            // Trust check
            if (target instanceof Player && plugin.getTrustManager().isTrusted(player, (Player) target)) {
                player.sendMessage(ChatColor.RED + "Cannot target trusted players!");
                return false;
            }

            Location center = target.getLocation();

            Set<Location> cageBlocks = new HashSet<>();

            // Create 3x3 cage with iron bars walls, floor, and ceiling
            for (int x = -1; x <= 1; x++) {
                for (int z = -1; z <= 1; z++) {
                    // Floor (y = -1) - use barrier for floor so they can't fall through
                    Location floorLoc = center.clone().add(x, -1, z);
                    if (floorLoc.getBlock().getType() == Material.AIR || floorLoc.getBlock().isPassable()) {
                        floorLoc.getBlock().setType(Material.BARRIER);
                        cageBlocks.add(floorLoc);
                    }

                    // Ceiling (y = 3) - use iron bars
                    Location ceilingLoc = center.clone().add(x, 3, z);
                    if (ceilingLoc.getBlock().getType() == Material.AIR || ceilingLoc.getBlock().isPassable()) {
                        ceilingLoc.getBlock().setType(Material.IRON_BARS);
                        cageBlocks.add(ceilingLoc);
                    }

                    // Walls - only place on the outer edges (not center column, and only edges)
                    if (Math.abs(x) == 1 || Math.abs(z) == 1) {
                        for (int y = 0; y <= 2; y++) {
                            Location loc = center.clone().add(x, y, z);
                            if (loc.getBlock().getType() == Material.AIR || loc.getBlock().isPassable()) {
                                loc.getBlock().setType(Material.IRON_BARS);
                                cageBlocks.add(loc);
                            }
                        }
                    }
                }
            }

            // Enhanced particles
            center.getWorld().spawnParticle(Particle.SOUL, center, 200, 1.5, 2, 1.5, 0.05);
            center.getWorld().spawnParticle(Particle.SMOKE, center, 150, 1.5, 2, 1.5, 0.05);
            center.getWorld().spawnParticle(Particle.ENCHANT, center, 100, 1.5, 2, 1.5, 0.1);

            // Notify victim
            if (target instanceof Player) {
                ((Player) target).sendMessage(ChatColor.RED + "⚔ Hit by " + ChatColor.DARK_GRAY + "Prison of the Damned" + ChatColor.RED + " from " + player.getName() + "!");
            }

            // Add blocks to active prison blocks set (makes them unbreakable)
            activePrisonBlocks.addAll(cageBlocks);

            // Spawn cage particles every tick to make it visible
            new BukkitRunnable() {
                int ticks = 0;

                @Override
                public void run() {
                    if (ticks >= 100) { // 5 seconds
                        // Remove cage
                        for (Location loc : cageBlocks) {
                            Material type = loc.getBlock().getType();
                            if (type == Material.BARRIER || type == Material.IRON_BARS) {
                                loc.getBlock().setType(Material.AIR);
                            }
                            // Remove from active prison blocks
                            activePrisonBlocks.remove(loc);
                        }
                        cancel();
                        return;
                    }

                    // Spawn particles along cage edges to make it visible
                    for (int x = -1; x <= 1; x++) {
                        for (int z = -1; z <= 1; z++) {
                            if (Math.abs(x) == 1 || Math.abs(z) == 1) {
                                for (int y = 0; y <= 3; y++) {
                                    Location particleLoc = center.clone().add(x, y, z);
                                    center.getWorld().spawnParticle(Particle.SOUL, particleLoc, 1, 0.1, 0.1, 0.1, 0);
                                }
                            }
                        }
                    }

                    ticks += 5;
                }
            }.runTaskTimer(plugin, 0L, 5L);

            player.sendMessage(ChatColor.DARK_GRAY + "Prison of the Damned!");
            return true;
        }

        player.sendMessage(ChatColor.RED + "No target found!");
        return false;
    }

    // ========== SKYBREAKER BOOTS ==========

    private boolean frostbiteSweep(Player player) {
        Vector direction = player.getLocation().getDirection();
        Location start = player.getLocation();

        Set<UUID> hitEntities = new HashSet<>();

        for (int i = 1; i <= 8; i++) {
            Location point = start.clone().add(direction.clone().multiply(i));

            // Cone effect
            for (double angle = -30; angle <= 30; angle += 10) {
                Vector rotated = rotateVector(direction.clone(), angle);
                Location conePoint = start.clone().add(rotated.multiply(i));

                // Enhanced particles
                conePoint.getWorld().spawnParticle(Particle.SNOWFLAKE, conePoint, 20, 0.2, 0.2, 0.2, 0);
                conePoint.getWorld().spawnParticle(Particle.CLOUD, conePoint, 10, 0.2, 0.2, 0.2, 0);
                conePoint.getWorld().spawnParticle(Particle.ITEM_SNOWBALL, conePoint, 8, 0.2, 0.2, 0.2, 0.05);

                for (Entity entity : conePoint.getWorld().getNearbyEntities(conePoint, 1.5, 1.5, 1.5)) {
                    if (entity instanceof LivingEntity && entity != player && !hitEntities.contains(entity.getUniqueId())) {
                        // Trust check
                        if (entity instanceof Player && plugin.getTrustManager().isTrusted(player, (Player) entity)) {
                            continue;
                        }
                        LivingEntity living = (LivingEntity) entity;
                        living.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 60, 4));
                        living.setFreezeTicks(140);
                        hitEntities.add(entity.getUniqueId());

                        // Notify victim
                        if (living instanceof Player) {
                            ((Player) living).sendMessage(ChatColor.RED + "⚔ Hit by " + ChatColor.AQUA + "Frostbite Sweep" + ChatColor.RED + " from " + player.getName() + "!");
                        }
                    }
                }
            }
        }

        player.playSound(player.getLocation(), Sound.BLOCK_GLASS_BREAK, 1.0f, 0.5f);
        player.sendMessage(ChatColor.AQUA + "Frostbite Sweep!");
        return true;
    }

    private boolean wintersEmbrace(Player player) {
        Location center = player.getLocation();

        // Create dome effect with particles
        new BukkitRunnable() {
            int ticks = 0;

            @Override
            public void run() {
                if (ticks >= 100) { // 5 seconds
                    cancel();
                    return;
                }

                // Spawn dome particles with enhanced variety
                for (double theta = 0; theta < 2 * Math.PI; theta += Math.PI / 16) {
                    for (double phi = 0; phi < Math.PI; phi += Math.PI / 16) {
                        double x = 7 * Math.sin(phi) * Math.cos(theta);
                        double y = 7 * Math.sin(phi) * Math.sin(theta);
                        double z = 7 * Math.cos(phi);

                        Location particleLoc = center.clone().add(x, z, y);
                        center.getWorld().spawnParticle(Particle.SNOWFLAKE, particleLoc, 3, 0, 0, 0, 0);
                        center.getWorld().spawnParticle(Particle.CLOUD, particleLoc, 1, 0, 0, 0, 0);
                    }
                }

                // Apply effects
                for (Entity entity : center.getWorld().getNearbyEntities(center, 7, 7, 7)) {
                    if (entity instanceof LivingEntity) {
                        if (entity == player) {
                            player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 20, 1));
                        } else {
                            // Trust check
                            if (entity instanceof Player && plugin.getTrustManager().isTrusted(player, (Player) entity)) {
                                continue;
                            }
                            LivingEntity living = (LivingEntity) entity;
                            living.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 20, 2));

                            // Notify victim once every second (every 20 ticks)
                            if (ticks % 20 == 0 && living instanceof Player) {
                                ((Player) living).sendMessage(ChatColor.RED + "⚔ Hit by " + ChatColor.AQUA + "Winter's Embrace" + ChatColor.RED + " from " + player.getName() + "!");
                            }
                        }
                    }
                }

                ticks++;
            }
        }.runTaskTimer(plugin, 0L, 1L);

        player.sendMessage(ChatColor.AQUA + "Winter's Embrace!");
        return true;
    }

    // ========== CELESTIAL AEGIS SHIELD ==========

    private boolean radiantBlock(Player player) {
        radiantBlockActive.put(player.getUniqueId(), System.currentTimeMillis() + 5000);

        // Enhanced particles
        player.getWorld().spawnParticle(Particle.END_ROD, player.getLocation(), 200, 1, 1, 1, 0.1);
        player.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, player.getLocation(), 100, 1, 1, 1, 0.1);
        player.getWorld().spawnParticle(Particle.ENCHANT, player.getLocation(), 150, 1, 1, 1, 0.1);

        player.sendMessage(ChatColor.GOLD + "Radiant Block active! Reflect 75% damage for 5 seconds!");
        player.playSound(player.getLocation(), Sound.BLOCK_ENCHANTMENT_TABLE_USE, 1.0f, 1.2f);
        return true;
    }

    private boolean heavensWall(Player player) {
        Location center = player.getLocation();
        World world = center.getWorld();

        // Create 16x16 barrier centered on player
        double minX = center.getX() - 8;
        double maxX = center.getX() + 8;
        double minZ = center.getZ() - 8;
        double maxZ = center.getZ() + 8;

        HeavensWallBarrier barrier = new HeavensWallBarrier(player.getUniqueId(), world, minX, maxX, minZ, maxZ, center.getY());
        activeBarriers.put(player.getUniqueId(), barrier);

        // Particle effect task - runs every 3 ticks for 32 seconds (640 ticks)
        new BukkitRunnable() {
            int ticks = 0;

            @Override
            public void run() {
                if (ticks >= 640) { // 32 seconds
                    activeBarriers.remove(player.getUniqueId());
                    player.sendMessage(ChatColor.GOLD + "Heaven's Wall has faded.");
                    cancel();
                    return;
                }

                // Spawn star-like particles along the barrier edges
                for (Player nearby : world.getPlayers()) {
                    Location pLoc = nearby.getLocation();

                    // Only render particles if player is within 32 blocks of the barrier
                    if (pLoc.getX() < minX - 32 || pLoc.getX() > maxX + 32 ||
                        pLoc.getZ() < minZ - 32 || pLoc.getZ() > maxZ + 32) {
                        continue;
                    }

                    double playerY = pLoc.getY();

                    // Render particles at player's Y level ± 8 blocks (taller wall)
                    for (double y = playerY - 8; y <= playerY + 8; y += 0.8) {
                        // North wall (minZ)
                        for (double x = minX; x <= maxX; x += 1.5) {
                            Location particleLoc = new Location(world, x, y, minZ);
                            if (particleLoc.distance(pLoc) < 20) {
                                world.spawnParticle(Particle.END_ROD, particleLoc, 1, 0.05, 0.05, 0.05, 0);
                            }
                        }
                        // South wall (maxZ)
                        for (double x = minX; x <= maxX; x += 1.5) {
                            Location particleLoc = new Location(world, x, y, maxZ);
                            if (particleLoc.distance(pLoc) < 20) {
                                world.spawnParticle(Particle.END_ROD, particleLoc, 1, 0.05, 0.05, 0.05, 0);
                            }
                        }
                        // West wall (minX)
                        for (double z = minZ; z <= maxZ; z += 1.5) {
                            Location particleLoc = new Location(world, minX, y, z);
                            if (particleLoc.distance(pLoc) < 20) {
                                world.spawnParticle(Particle.END_ROD, particleLoc, 1, 0.05, 0.05, 0.05, 0);
                            }
                        }
                        // East wall (maxX)
                        for (double z = minZ; z <= maxZ; z += 1.5) {
                            Location particleLoc = new Location(world, maxX, y, z);
                            if (particleLoc.distance(pLoc) < 20) {
                                world.spawnParticle(Particle.END_ROD, particleLoc, 1, 0.05, 0.05, 0.05, 0);
                            }
                        }
                    }
                }

                ticks += 3;
            }
        }.runTaskTimer(plugin, 0L, 3L);

        player.sendMessage(ChatColor.GOLD + "Heaven's Wall activated! 16x16 barrier for 32 seconds.");
        player.playSound(player.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, 1.0f, 1.0f);
        return true;
    }

    @EventHandler(priority = org.bukkit.event.EventPriority.HIGH)
    public void onPlayerMove(PlayerMoveEvent event) {
        // Check if player is trying to cross a Heaven's Wall barrier
        if (activeBarriers.isEmpty()) return;

        Player player = event.getPlayer();
        Location from = event.getFrom();
        Location to = event.getTo();
        if (to == null) return;

        // Check if player actually moved position (not just looking around)
        if (from.getX() == to.getX() && from.getY() == to.getY() && from.getZ() == to.getZ()) {
            return;
        }

        for (Map.Entry<UUID, HeavensWallBarrier> entry : activeBarriers.entrySet()) {
            HeavensWallBarrier barrier = entry.getValue();
            UUID ownerUUID = entry.getKey();

            // Skip if different world
            if (!barrier.world.equals(player.getWorld())) continue;

            // Check if player is trying to cross the barrier
            boolean wasInside = barrier.isInside(from.getX(), from.getZ());
            boolean willBeInside = barrier.isInside(to.getX(), to.getZ());

            // Owner cannot leave the barrier (but can move inside)
            if (player.getUniqueId().equals(ownerUUID)) {
                // Block if trying to leave (was inside, now outside)
                if (wasInside && !willBeInside) {
                    event.setTo(from);
                    Vector pushBack = from.toVector().subtract(to.toVector()).normalize().multiply(0.3);
                    pushBack.setY(0);
                    player.setVelocity(pushBack);
                    player.getWorld().spawnParticle(Particle.END_ROD, to.clone(), 30, 0.3, 0.5, 0.3, 0.05);
                    player.playSound(to, Sound.BLOCK_AMETHYST_BLOCK_HIT, 1.0f, 1.5f);
                    return;
                }
                continue; // Owner can move freely inside
            }

            // Trusted players can pass freely
            Player owner = Bukkit.getPlayer(ownerUUID);
            if (owner != null && plugin.getTrustManager().isTrusted(owner, player)) {
                continue;
            }

            // Check if player is on the wall edge (trying to stand on the barrier)
            if (barrier.isOnWallEdge(to.getX(), to.getY(), to.getZ())) {
                // Push player away from the wall
                event.setTo(from);

                // Calculate push direction - away from nearest wall edge
                double pushX = 0, pushZ = 0;
                if (Math.abs(to.getX() - barrier.minX) < 0.5) pushX = -0.5;
                else if (Math.abs(to.getX() - barrier.maxX) < 0.5) pushX = 0.5;
                if (Math.abs(to.getZ() - barrier.minZ) < 0.5) pushZ = -0.5;
                else if (Math.abs(to.getZ() - barrier.maxZ) < 0.5) pushZ = 0.5;

                Vector pushAway = new Vector(pushX, -0.2, pushZ); // Push away and slightly down
                player.setVelocity(pushAway);

                // Visual/audio feedback
                player.getWorld().spawnParticle(Particle.END_ROD, to.clone(), 20, 0.2, 0.3, 0.2, 0.05);
                player.playSound(to, Sound.BLOCK_AMETHYST_BLOCK_HIT, 0.8f, 1.5f);
                return;
            }

            // If crossing the barrier boundary (either direction) - for non-trusted players
            if (wasInside != willBeInside) {
                // Block the movement by setting destination to origin
                event.setTo(from);

                // Push player back and slightly down (to prevent floating)
                Vector pushBack = from.toVector().subtract(to.toVector()).normalize().multiply(0.3);
                pushBack.setY(-0.1); // Slight downward push
                player.setVelocity(pushBack);

                // Visual/audio feedback - star barrier effect
                Location barrierHit = to.clone();
                player.getWorld().spawnParticle(Particle.END_ROD, barrierHit, 30, 0.3, 0.5, 0.3, 0.05);
                player.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, barrierHit, 20, 0.3, 0.5, 0.3, 0.1);
                player.getWorld().spawnParticle(Particle.ENCHANTED_HIT, barrierHit, 15, 0.3, 0.5, 0.3, 0);
                player.playSound(barrierHit, Sound.BLOCK_AMETHYST_BLOCK_HIT, 1.0f, 1.5f);
                return;
            }
        }
    }

    @EventHandler(priority = org.bukkit.event.EventPriority.HIGH)
    public void onEnderPearlLand(ProjectileHitEvent event) {
        // Block ender pearls from crossing Heaven's Wall
        if (!(event.getEntity() instanceof org.bukkit.entity.EnderPearl)) return;
        if (activeBarriers.isEmpty()) return;

        org.bukkit.entity.EnderPearl pearl = (org.bukkit.entity.EnderPearl) event.getEntity();
        if (!(pearl.getShooter() instanceof Player)) return;

        Player player = (Player) pearl.getShooter();
        Location pearlLoc = pearl.getLocation();

        for (Map.Entry<UUID, HeavensWallBarrier> entry : activeBarriers.entrySet()) {
            HeavensWallBarrier barrier = entry.getValue();
            UUID ownerUUID = entry.getKey();

            // Skip if different world
            if (!barrier.world.equals(pearlLoc.getWorld())) continue;

            // Owner can't pearl out
            if (player.getUniqueId().equals(ownerUUID)) {
                boolean playerInside = barrier.isInside(player.getLocation().getX(), player.getLocation().getZ());
                boolean pearlInside = barrier.isInside(pearlLoc.getX(), pearlLoc.getZ());
                if (playerInside && !pearlInside) {
                    event.setCancelled(true);
                    pearl.remove();
                    player.sendMessage(ChatColor.GOLD + "Your ender pearl hit Heaven's Wall!");
                    player.getWorld().spawnParticle(Particle.END_ROD, pearlLoc, 30, 0.3, 0.3, 0.3, 0.05);
                    player.playSound(pearlLoc, Sound.BLOCK_AMETHYST_BLOCK_HIT, 1.0f, 1.5f);
                    return;
                }
                continue;
            }

            // Trusted players can pearl through
            Player owner = Bukkit.getPlayer(ownerUUID);
            if (owner != null && plugin.getTrustManager().isTrusted(owner, player)) {
                continue;
            }

            // Check if pearl is crossing or landing on the barrier
            boolean playerInside = barrier.isInside(player.getLocation().getX(), player.getLocation().getZ());
            boolean pearlInside = barrier.isInside(pearlLoc.getX(), pearlLoc.getZ());

            if (playerInside != pearlInside || barrier.isOnWallEdge(pearlLoc.getX(), pearlLoc.getY(), pearlLoc.getZ())) {
                event.setCancelled(true);
                pearl.remove();
                player.sendMessage(ChatColor.GOLD + "Your ender pearl hit Heaven's Wall!");
                player.getWorld().spawnParticle(Particle.END_ROD, pearlLoc, 30, 0.3, 0.3, 0.3, 0.05);
                player.playSound(pearlLoc, Sound.BLOCK_AMETHYST_BLOCK_HIT, 1.0f, 1.5f);
                return;
            }
        }
    }

    // ========== CHRONO BLADE ==========

    private boolean echoStrike(Player player) {
        // Time Distortion - 6-block radius bubble that slows enemies for 3 seconds
        if (timeDistortionActive.contains(player.getUniqueId())) {
            player.sendMessage(ChatColor.RED + "Time Distortion is already active!");
            return false;
        }

        Location center = player.getLocation().clone();
        World world = center.getWorld();
        UUID playerUUID = player.getUniqueId();

        timeDistortionActive.add(playerUUID);

        player.sendMessage(ChatColor.YELLOW + "⏳ Time Distortion activated!");
        world.playSound(center, Sound.BLOCK_BEACON_ACTIVATE, 1.5f, 0.5f);
        world.playSound(center, Sound.BLOCK_RESPAWN_ANCHOR_CHARGE, 1.0f, 0.7f);

        // Track affected entities for the final damage
        Set<UUID> affectedEntities = new HashSet<>();

        // Run effect every 2 ticks for 3 seconds (60 ticks)
        new BukkitRunnable() {
            int ticks = 0;

            @Override
            public void run() {
                if (ticks >= 60) { // 3 seconds
                    // Bubble ends - snap back and deal damage
                    timeDistortionActive.remove(playerUUID);

                    // Deal 4 true damage to all affected entities
                    for (UUID entityUUID : affectedEntities) {
                        Entity entity = Bukkit.getEntity(entityUUID);
                        if (entity instanceof LivingEntity && entity.isValid() && !entity.isDead()) {
                            LivingEntity living = (LivingEntity) entity;
                            double currentHealth = living.getHealth();
                            living.setHealth(Math.max(0, currentHealth - 8.0)); // 4 hearts = 8 damage

                            // Snap particles on each target
                            living.getWorld().spawnParticle(Particle.FLASH, living.getLocation().add(0, 1, 0), 1, 0, 0, 0, 0);
                            living.getWorld().spawnParticle(Particle.REVERSE_PORTAL, living.getLocation().add(0, 1, 0), 50, 0.3, 0.5, 0.3, 1);

                            if (living instanceof Player) {
                                ((Player) living).sendMessage(ChatColor.RED + "⚔ Time snapped! 4 hearts true damage from " + player.getName() + "!");
                            }
                        }
                    }

                    // Final snap effect
                    world.spawnParticle(Particle.FLASH, center, 3, 3, 3, 3, 0);
                    world.spawnParticle(Particle.REVERSE_PORTAL, center, 300, 6, 3, 6, 2);
                    world.playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 1.5f);
                    world.playSound(center, Sound.BLOCK_RESPAWN_ANCHOR_DEPLETE, 1.5f, 0.5f);

                    player.sendMessage(ChatColor.YELLOW + "⏳ Time snaps back! " + affectedEntities.size() + " enemies hit for 4 true damage!");

                    cancel();
                    return;
                }

                // Apply HEAVY slow effects to enemies in range - much more time distortion
                for (Entity entity : world.getNearbyEntities(center, 6, 6, 6)) {
                    if (entity instanceof LivingEntity && entity != player) {
                        LivingEntity living = (LivingEntity) entity;

                        // Trust check
                        if (living instanceof Player && plugin.getTrustManager().isTrusted(player, (Player) living)) {
                            continue;
                        }

                        affectedEntities.add(living.getUniqueId());

                        // EXTREME slowness (almost frozen - Slowness 127 = near immobile)
                        living.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 10, 127, true, false));
                        // Completely slowed attack speed
                        living.addPotionEffect(new PotionEffect(PotionEffectType.MINING_FATIGUE, 10, 4, true, false));
                        // Add weakness to reduce damage output
                        living.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, 10, 2, true, false));
                        // Add jump restriction (negative jump boost)
                        living.addPotionEffect(new PotionEffect(PotionEffectType.JUMP_BOOST, 10, 128, true, false));
                    }
                }

                // Bubble particles - sphere effect
                double radius = 6;
                for (double phi = 0; phi < Math.PI; phi += Math.PI / 8) {
                    for (double theta = 0; theta < 2 * Math.PI; theta += Math.PI / 8) {
                        double x = radius * Math.sin(phi) * Math.cos(theta);
                        double y = radius * Math.cos(phi);
                        double z = radius * Math.sin(phi) * Math.sin(theta);
                        Location particleLoc = center.clone().add(x, y + 1, z);
                        world.spawnParticle(Particle.ENCHANT, particleLoc, 1, 0, 0, 0, 0);
                    }
                }

                // Inner distortion particles
                world.spawnParticle(Particle.REVERSE_PORTAL, center.clone().add(0, 1, 0), 20, 3, 2, 3, 0.1);
                world.spawnParticle(Particle.ENCHANTED_HIT, center.clone().add(0, 1, 0), 10, 3, 2, 3, 0);

                ticks += 2;
            }
        }.runTaskTimer(plugin, 0L, 2L);

        return true;
    }

    private boolean timeRewind(Player player) {
        UUID uuid = player.getUniqueId();

        // Check if player already has a marked position - if so, trigger the shift early
        if (timeRewindStates.containsKey(uuid)) {
            triggerChronoShift(player);
            return true;
        }

        // Mark current position
        SavedState state = new SavedState(
            player.getLocation().clone(),
            player.getHealth(),
            player.getFoodLevel()
        );

        timeRewindStates.put(uuid, state);

        // Marking particles
        player.getWorld().spawnParticle(Particle.REVERSE_PORTAL, player.getLocation(), 200, 0.5, 0.5, 0.5, 0.5);
        player.getWorld().spawnParticle(Particle.ENCHANT, player.getLocation(), 100, 0.5, 0.5, 0.5, 0.1);
        player.playSound(player.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, 1.0f, 1.5f);

        player.sendMessage(ChatColor.YELLOW + "⏳ Chrono Shift: Position marked! Re-cast to teleport back (10s)");

        // Schedule auto-teleport after 10 seconds
        int taskId = Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (timeRewindStates.containsKey(uuid)) {
                triggerChronoShift(player);
            }
        }, 200L).getTaskId(); // 10 seconds = 200 ticks

        state.taskId = taskId;

        return true;
    }

    private void triggerChronoShift(Player player) {
        UUID uuid = player.getUniqueId();
        SavedState saved = timeRewindStates.remove(uuid);

        if (saved == null) return;

        // Cancel the scheduled task if it exists (player triggered early)
        if (saved.taskId != -1) {
            Bukkit.getScheduler().cancelTask(saved.taskId);
        }

        // Teleport back to marked position
        player.teleport(saved.location);

        // Remove all negative effects
        for (PotionEffect effect : player.getActivePotionEffects()) {
            PotionEffectType type = effect.getType();
            // Remove negative effects
            if (type.equals(PotionEffectType.POISON) ||
                type.equals(PotionEffectType.WITHER) ||
                type.equals(PotionEffectType.SLOWNESS) ||
                type.equals(PotionEffectType.MINING_FATIGUE) ||
                type.equals(PotionEffectType.NAUSEA) ||
                type.equals(PotionEffectType.BLINDNESS) ||
                type.equals(PotionEffectType.HUNGER) ||
                type.equals(PotionEffectType.WEAKNESS) ||
                type.equals(PotionEffectType.LEVITATION) ||
                type.equals(PotionEffectType.UNLUCK) ||
                type.equals(PotionEffectType.DARKNESS)) {
                player.removePotionEffect(type);
            }
        }

        // Grant Speed II for 10 seconds (200 ticks)
        player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 200, 1, true, true));

        // Enhanced particles at destination
        player.getWorld().spawnParticle(Particle.REVERSE_PORTAL, player.getLocation(), 400, 1, 1, 1, 1);
        player.getWorld().spawnParticle(Particle.ENCHANT, player.getLocation(), 200, 1, 1, 1, 0.1);
        player.getWorld().spawnParticle(Particle.PORTAL, player.getLocation(), 150, 1, 1, 1, 0.5);
        player.getWorld().spawnParticle(Particle.END_ROD, player.getLocation(), 50, 0.5, 1, 0.5, 0.1);

        player.playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 0.5f);
        player.playSound(player.getLocation(), Sound.BLOCK_BEACON_DEACTIVATE, 1.0f, 1.2f);
        player.sendMessage(ChatColor.YELLOW + "⏳ Chrono Shift! Negative effects cleared, Speed II granted!");
    }

    // ========== SOUL DEVOURER ==========

    private boolean voidSlice(Player player) {
        Vector direction = player.getLocation().getDirection().setY(0).normalize();
        Location start = player.getLocation().add(0, 1, 0); // At chest height
        World world = player.getWorld();

        Set<UUID> hitEntities = new HashSet<>();

        // Purple horizontal slice - wide arc in front
        for (int i = 1; i <= 8; i++) {
            Location point = start.clone().add(direction.clone().multiply(i));

            // Wide horizontal sweep - side to side
            for (double offset = -2.5; offset <= 2.5; offset += 0.3) {
                Vector perpendicular = direction.clone().crossProduct(new Vector(0, 1, 0)).normalize().multiply(offset);
                Location sweepPoint = point.clone().add(perpendicular);

                // LOTS of purple particles - horizontal slice effect
                world.spawnParticle(Particle.REVERSE_PORTAL, sweepPoint, 40, 0.1, 0.05, 0.1, 0.8);
                world.spawnParticle(Particle.DRAGON_BREATH, sweepPoint, 30, 0.15, 0.05, 0.15, 0.05);
                world.spawnParticle(Particle.WITCH, sweepPoint, 25, 0.1, 0.05, 0.1, 0.15);
                world.spawnParticle(Particle.PORTAL, sweepPoint, 35, 0.15, 0.05, 0.15, 1.0);
                world.spawnParticle(Particle.DUST, sweepPoint, 20, 0.1, 0.05, 0.1,
                    new Particle.DustOptions(Color.fromRGB(128, 0, 255), 1.5f)); // Purple dust

                for (Entity entity : world.getNearbyEntities(sweepPoint, 1.2, 1.5, 1.2)) {
                    if (entity instanceof LivingEntity && entity != player && !hitEntities.contains(entity.getUniqueId())) {
                        // Trust check
                        if (entity instanceof Player && plugin.getTrustManager().isTrusted(player, (Player) entity)) {
                            continue;
                        }
                        LivingEntity living = (LivingEntity) entity;
                        living.damage(20.0, player); // ~4 hearts through full Prot 4
                        hitEntities.add(entity.getUniqueId());

                        // Extra particles on hit
                        world.spawnParticle(Particle.REVERSE_PORTAL, living.getLocation().add(0, 1, 0), 60, 0.3, 0.5, 0.3, 1.0);
                        world.spawnParticle(Particle.WITCH, living.getLocation().add(0, 1, 0), 40, 0.3, 0.5, 0.3, 0.2);

                        // Notify victim
                        if (living instanceof Player) {
                            ((Player) living).sendMessage(ChatColor.RED + "⚔ Hit by " + ChatColor.DARK_PURPLE + "Void Slice" + ChatColor.RED + " from " + player.getName() + "!");
                        }
                    }
                }
            }
        }

        // Sweep attack sound
        world.playSound(start, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1.5f, 0.5f);
        world.playSound(start, Sound.ENTITY_WITHER_SHOOT, 1.0f, 0.6f);
        player.sendMessage(ChatColor.DARK_PURPLE + "Void Slice!");
        return true;
    }

    private boolean voidRift(Player player) {
        RayTraceResult result = player.getWorld().rayTraceBlocks(
            player.getEyeLocation(),
            player.getLocation().getDirection(),
            20
        );

        Location riftLocation = result != null && result.getHitBlock() != null
            ? result.getHitBlock().getLocation().add(0, 1, 0)
            : player.getLocation().add(player.getLocation().getDirection().multiply(10));

        World world = riftLocation.getWorld();

        new BukkitRunnable() {
            int ticks = 0;
            Set<UUID> damagedEntities = new HashSet<>();

            @Override
            public void run() {
                if (ticks >= 60) { // 3 seconds
                    cancel();
                    return;
                }

                // MASSIVE gravitational black hole effect - spiral particles being sucked in
                // Outer spiral particles moving inward
                for (double angle = 0; angle < 360; angle += 20) {
                    double radians = Math.toRadians(angle + ticks * 15); // Rotating spiral
                    for (double r = 4; r >= 0.5; r -= 0.5) {
                        double x = r * Math.cos(radians + r * 0.5);
                        double z = r * Math.sin(radians + r * 0.5);
                        double y = (4 - r) * 0.3; // Funnel shape
                        Location spiralPoint = riftLocation.clone().add(x, y, z);

                        world.spawnParticle(Particle.REVERSE_PORTAL, spiralPoint, 5, 0.1, 0.1, 0.1, 0.3);
                        world.spawnParticle(Particle.PORTAL, spiralPoint, 3, 0.1, 0.1, 0.1, 0.5);
                    }
                }

                // Dense center particles
                world.spawnParticle(Particle.REVERSE_PORTAL, riftLocation, 150, 0.8, 0.8, 0.8, 1.5);
                world.spawnParticle(Particle.DRAGON_BREATH, riftLocation, 100, 0.6, 0.6, 0.6, 0.1);
                world.spawnParticle(Particle.WITCH, riftLocation, 80, 0.5, 0.5, 0.5, 0.2);
                world.spawnParticle(Particle.SQUID_INK, riftLocation, 60, 0.4, 0.4, 0.4, 0.1);
                world.spawnParticle(Particle.DUST, riftLocation, 50, 0.5, 0.5, 0.5,
                    new Particle.DustOptions(Color.fromRGB(50, 0, 80), 2.0f)); // Dark purple core

                // Pull-in particle lines from nearby area
                for (int i = 0; i < 8; i++) {
                    double angle = Math.random() * Math.PI * 2;
                    double dist = 3 + Math.random() * 3;
                    Location pullStart = riftLocation.clone().add(
                        Math.cos(angle) * dist,
                        (Math.random() - 0.5) * 3,
                        Math.sin(angle) * dist
                    );
                    world.spawnParticle(Particle.REVERSE_PORTAL, pullStart, 10, 0.1, 0.1, 0.1, 0.8);
                }

                // Pull entities with stronger force
                for (Entity entity : world.getNearbyEntities(riftLocation, 10, 10, 10)) {
                    if (entity instanceof LivingEntity && entity != player) {
                        // Trust check
                        if (entity instanceof Player && plugin.getTrustManager().isTrusted(player, (Player) entity)) {
                            continue;
                        }

                        double distance = entity.getLocation().distance(riftLocation);
                        Vector direction = riftLocation.toVector().subtract(entity.getLocation().toVector()).normalize();

                        // Stronger pull the closer they are
                        double pullStrength = Math.min(1.0, 6.0 / (distance + 1));
                        entity.setVelocity(direction.multiply(pullStrength));

                        // Damage if inside rift core
                        if (distance < 2.5) {
                            LivingEntity living = (LivingEntity) entity;
                            living.damage(12.0, player); // ~2.5 hearts through full Prot 4 per tick

                            // Notify victim (once to avoid spam)
                            if (!damagedEntities.contains(entity.getUniqueId())) {
                                damagedEntities.add(entity.getUniqueId());
                                if (living instanceof Player) {
                                    ((Player) living).sendMessage(ChatColor.RED + "⚔ Hit by " + ChatColor.DARK_PURPLE + "Void Rift" + ChatColor.RED + " from " + player.getName() + "!");
                                }
                            }
                        }
                    }
                }

                // Ambient sound
                if (ticks % 20 == 0) {
                    world.playSound(riftLocation, Sound.BLOCK_PORTAL_AMBIENT, 2.0f, 0.5f);
                }

                ticks += 5; // Run every 5 ticks for smoother effect
            }
        }.runTaskTimer(plugin, 0L, 5L);

        world.playSound(riftLocation, Sound.ENTITY_ENDERMAN_TELEPORT, 2.0f, 0.3f);
        player.sendMessage(ChatColor.DARK_PURPLE + "Void Rift!");
        return true;
    }

    // ========== CREATION SPLITTER ==========

    private boolean endSever(Player player) {
        Vector direction = player.getLocation().getDirection();
        Location playerLoc = player.getLocation();
        World world = player.getWorld();

        Set<UUID> hitEntities = new HashSet<>();

        // Create 7-block cone slash in front
        for (int distance = 1; distance <= 7; distance++) {
            // Wider spread as distance increases (cone shape)
            double spread = distance * 0.5;

            for (double angle = -45; angle <= 45; angle += 15) {
                Vector rotated = rotateVector(direction.clone(), angle);
                Location point = playerLoc.clone().add(0, 1, 0).add(rotated.multiply(distance));

                // Purple "cut" line particles - Enhanced purple theme
                world.spawnParticle(Particle.REVERSE_PORTAL, point, 15, 0.3, 0.3, 0.3, 0.5);
                world.spawnParticle(Particle.DRAGON_BREATH, point, 10, 0.2, 0.2, 0.2, 0.05);
                world.spawnParticle(Particle.WITCH, point, 8, 0.2, 0.2, 0.2, 0.1);
                world.spawnParticle(Particle.PORTAL, point, 5, 0.2, 0.2, 0.2, 0.5);

                // Check for entities in cone
                for (Entity entity : world.getNearbyEntities(point, 1.2, 1.5, 1.2)) {
                    if (entity instanceof LivingEntity && entity != player && !hitEntities.contains(entity.getUniqueId())) {
                        // Trust check
                        if (entity instanceof Player && plugin.getTrustManager().isTrusted(player, (Player) entity)) {
                            continue;
                        }

                        LivingEntity living = (LivingEntity) entity;
                        hitEntities.add(entity.getUniqueId());

                        // 2 TRUE DAMAGE (bypasses armor) - 2 hearts = 4 HP
                        double currentHealth = living.getHealth();
                        double newHealth = Math.max(0, currentHealth - 4.0); // 2 hearts = 4 damage
                        living.setHealth(newHealth);
                        living.setNoDamageTicks(0);

                        // Ender Decay - 10% lower defense (Weakness effect approximation)
                        living.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, 80, 0)); // 4 seconds

                        // Slight levitation (0.5s = 10 ticks)
                        living.addPotionEffect(new PotionEffect(PotionEffectType.LEVITATION, 10, 0));

                        // Purple visual effects on hit
                        world.spawnParticle(Particle.REVERSE_PORTAL, living.getLocation().add(0, 1, 0), 50, 0.4, 0.6, 0.4, 0.8);
                        world.spawnParticle(Particle.DRAGON_BREATH, living.getLocation().add(0, 1, 0), 30, 0.3, 0.5, 0.3, 0.05);
                        world.spawnParticle(Particle.WITCH, living.getLocation().add(0, 1, 0), 20, 0.3, 0.5, 0.3, 0.1);

                        // Notify victim
                        if (living instanceof Player) {
                            ((Player) living).sendMessage(ChatColor.RED + "⚔ Hit by " + ChatColor.DARK_PURPLE + "End Sever" + ChatColor.RED + " from " + player.getName() + "!");
                        }
                    }
                }
            }
        }

        // Teleport user 2 blocks backward (End recoil)
        Vector backward = direction.clone().multiply(-2);
        Location recoilLoc = playerLoc.clone().add(backward);
        recoilLoc.setY(playerLoc.getY()); // Keep same Y level
        recoilLoc.setPitch(playerLoc.getPitch());
        recoilLoc.setYaw(playerLoc.getYaw());

        // Check if recoil location is safe
        if (recoilLoc.getBlock().isPassable() && recoilLoc.clone().add(0, 1, 0).getBlock().isPassable()) {
            player.teleport(recoilLoc);
            world.spawnParticle(Particle.REVERSE_PORTAL, playerLoc, 50, 0.3, 0.5, 0.3, 0.5);
            world.spawnParticle(Particle.REVERSE_PORTAL, recoilLoc, 50, 0.3, 0.5, 0.3, 0.5);
        }

        // Sound effects
        world.playSound(playerLoc, Sound.ENTITY_ENDER_DRAGON_FLAP, 1.5f, 1.5f);
        world.playSound(playerLoc, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1.5f, 0.8f);
        world.playSound(playerLoc, Sound.BLOCK_END_PORTAL_SPAWN, 0.5f, 2.0f);

        player.sendMessage(ChatColor.DARK_PURPLE + "End Sever!");
        return true;
    }

    private boolean cataclysmPulse(Player player) {
        Location center = player.getLocation();
        World world = center.getWorld();

        // Channel particles during 1 second charge - Purple theme
        world.spawnParticle(Particle.REVERSE_PORTAL, center, 250, 1, 1, 1, 0.7);
        world.spawnParticle(Particle.WITCH, center, 100, 1, 1, 1, 0.2);
        world.spawnParticle(Particle.DRAGON_BREATH, center, 80, 1, 1, 1, 0.1);
        world.playSound(center, Sound.BLOCK_BEACON_POWER_SELECT, 1.5f, 0.5f);

        player.sendMessage(ChatColor.DARK_PURPLE + "Genesis Collapse charging...");

        // After 1 second channel, unleash collapse
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            Location collapseCenter = player.getLocation();

            // Pull all enemies within 7 blocks to center
            for (Entity entity : world.getNearbyEntities(collapseCenter, 7, 7, 7)) {
                if (entity instanceof LivingEntity && entity != player) {
                    // Trust check
                    if (entity instanceof Player && plugin.getTrustManager().isTrusted(player, (Player) entity)) {
                        continue;
                    }

                    LivingEntity living = (LivingEntity) entity;

                    // Pull to center
                    Vector direction = collapseCenter.toVector().subtract(entity.getLocation().toVector()).normalize();
                    entity.setVelocity(direction.multiply(2.0));

                    // 5 TRUE DAMAGE (bypasses armor) - 5 hearts = 10 HP
                    double currentHealth = living.getHealth();
                    double newHealth = Math.max(0, currentHealth - 10.0); // 5 hearts = 10 damage
                    living.setHealth(newHealth);

                    living.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 60, 2));
                    living.addPotionEffect(new PotionEffect(PotionEffectType.DARKNESS, 40, 0));

                    // Purple effects on each target
                    world.spawnParticle(Particle.REVERSE_PORTAL, living.getLocation().add(0, 1, 0), 40, 0.4, 0.6, 0.4, 0.8);
                    world.spawnParticle(Particle.WITCH, living.getLocation().add(0, 1, 0), 25, 0.3, 0.5, 0.3, 0.1);

                    // Notify victim
                    if (living instanceof Player) {
                        ((Player) living).sendMessage(ChatColor.RED + "⚔ Hit by " + ChatColor.DARK_PURPLE + "Genesis Collapse" + ChatColor.RED + " - 5 true damage from " + player.getName() + "!");
                    }
                }
            }

            // Massive purple shockwave particles
            world.spawnParticle(Particle.REVERSE_PORTAL, collapseCenter, 600, 5, 3, 5, 2.0);
            world.spawnParticle(Particle.DRAGON_BREATH, collapseCenter, 500, 5, 3, 5, 0.15);
            world.spawnParticle(Particle.WITCH, collapseCenter, 300, 5, 3, 5, 0.2);
            world.spawnParticle(Particle.PORTAL, collapseCenter, 500, 5, 3, 5, 2.5);

            // Sound effects
            world.playSound(collapseCenter, Sound.ENTITY_WITHER_SPAWN, 1.0f, 0.5f);
            world.playSound(collapseCenter, Sound.ENTITY_GENERIC_EXPLODE, 2.0f, 0.5f);
            world.playSound(collapseCenter, Sound.ENTITY_ENDER_DRAGON_GROWL, 1.5f, 0.6f);

            // Teleport player slightly upward (End-style blink)
            Location blinkLoc = player.getLocation().add(0, 1.5, 0);
            if (blinkLoc.getBlock().isPassable()) {
                player.teleport(blinkLoc);
                world.spawnParticle(Particle.REVERSE_PORTAL, blinkLoc, 50, 0.3, 0.3, 0.3, 0.3);
            }

            // Give Absorption IV for 5 seconds (100 ticks)
            player.addPotionEffect(new PotionEffect(PotionEffectType.ABSORPTION, 100, 3));

            player.sendMessage(ChatColor.DARK_PURPLE + "" + ChatColor.BOLD + "GENESIS COLLAPSE!");
        }, 20L); // 1 second delay

        return true;
    }

    // ========== HELPER METHODS ==========

    private int getCooldownForAbility(LegendaryType type, int abilityNum) {
        Map<String, int[]> cooldowns = new HashMap<>();
        cooldowns.put("holy_moonlight_sword", new int[]{25, 45});
        cooldowns.put("pheonix_grace", new int[]{90, 300}); // Flame Harvest 1m30s, Fire Rebirth 5min
        cooldowns.put("tempestbreaker_spear", new int[]{25, 50});
        cooldowns.put("thousand_demon_daggers", new int[]{20, 60});
        cooldowns.put("divine_axe_rhitta", new int[]{35, 70});
        cooldowns.put("chains_of_eternity", new int[]{35, 65});
        cooldowns.put("celestial_aegis_shield", new int[]{40, 90});
        cooldowns.put("chrono_blade", new int[]{40, 120});
        cooldowns.put("soul_devourer", new int[]{30, 85});
        cooldowns.put("creation_splitter", new int[]{18, 120}); // End Sever 18s, Genesis Collapse 2min
        cooldowns.put("copper_pickaxe", new int[]{1, 1}); // Instant toggles

        int[] cd = cooldowns.get(type.getId());
        return cd != null ? cd[abilityNum - 1] : 30;
    }

    private Vector rotateVector(Vector vec, double degrees) {
        double radians = Math.toRadians(degrees);
        double cos = Math.cos(radians);
        double sin = Math.sin(radians);

        return new Vector(
            vec.getX() * cos - vec.getZ() * sin,
            vec.getY(),
            vec.getX() * sin + vec.getZ() * cos
        );
    }

    // ========== EVENT HANDLERS ==========

    @EventHandler
    public void onPlayerDeath(org.bukkit.event.entity.PlayerDeathEvent event) {
        Player player = event.getEntity();

        // Check Fire Rebirth
        if (fireRebirthActive.containsKey(player.getUniqueId())) {
            long endTime = fireRebirthActive.get(player.getUniqueId());
            if (System.currentTimeMillis() < endTime) {
                event.setCancelled(true);

                player.setHealth(12.0);
                player.addPotionEffect(new PotionEffect(PotionEffectType.FIRE_RESISTANCE, 200, 0));

                // Enhanced fiery explosion particles
                player.getWorld().spawnParticle(Particle.FLAME, player.getLocation(), 800, 3, 1, 3, 0.2);
                player.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME, player.getLocation(), 400, 3, 1, 3, 0.15);
                player.getWorld().spawnParticle(Particle.LAVA, player.getLocation(), 200, 3, 1, 3, 0.1);
                player.getWorld().spawnParticle(Particle.EXPLOSION_EMITTER, player.getLocation(), 10, 1, 1, 1, 0);
                player.getWorld().playSound(player.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 0.8f);

                // Damage nearby enemies (increased from 10.0)
                for (Entity entity : player.getNearbyEntities(5, 5, 5)) {
                    if (entity instanceof LivingEntity) {
                        // Trust check
                        if (entity instanceof Player) {
                            if (plugin.getTrustManager().isTrusted(player, (Player) entity)) {
                                continue;
                            }
                        }
                        LivingEntity living = (LivingEntity) entity;
                        living.damage(40.0, player); // ~8 hearts through full Prot 4
                        Vector knockback = entity.getLocation().toVector().subtract(player.getLocation().toVector()).normalize().multiply(2);
                        entity.setVelocity(knockback);

                        // Notify victim
                        if (living instanceof Player) {
                            ((Player) living).sendMessage(ChatColor.RED + "⚔ Hit by " + ChatColor.GOLD + "Fire Rebirth Explosion" + ChatColor.RED + " from " + player.getName() + "!");
                        }
                    }
                }

                player.sendMessage(ChatColor.GOLD + "" + ChatColor.BOLD + "FIRE REBIRTH! You have been saved from death!");
                fireRebirthActive.remove(player.getUniqueId());
            }
        }
    }

    @EventHandler
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        // Echo Strike
        if (event.getDamager() instanceof Player) {
            Player player = (Player) event.getDamager();

            if (echoStrikeActive.containsKey(player.getUniqueId())) {
                long endTime = echoStrikeActive.get(player.getUniqueId());
                if (System.currentTimeMillis() < endTime && event.getEntity() instanceof LivingEntity) {
                    LivingEntity target = (LivingEntity) event.getEntity();

                    // Trust check
                    if (target instanceof Player && plugin.getTrustManager().isTrusted(player, (Player) target)) {
                        return;
                    }

                    double damage = event.getFinalDamage();

                    // Temporarily remove echo strike to prevent the echo from triggering another echo
                    Long savedEndTime = echoStrikeActive.remove(player.getUniqueId());

                    // Schedule echo hit
                    Bukkit.getScheduler().runTaskLater(plugin, () -> {
                        if (target.isValid() && !target.isDead()) {
                            target.damage(damage, player);
                            target.getWorld().spawnParticle(Particle.CRIT, target.getLocation(), 20, 0.5, 0.5, 0.5, 0);
                            player.sendMessage(ChatColor.YELLOW + "Echo!");
                        }
                        // Re-enable echo strike after the echo damage is dealt
                        if (savedEndTime != null && System.currentTimeMillis() < savedEndTime) {
                            echoStrikeActive.put(player.getUniqueId(), savedEndTime);
                        }
                    }, 20L);
                }
            }

            // Shadowstep Backstab - bonus true damage on next hit
            if (shadowstepBackstab.remove(player.getUniqueId())) {
                if (event.getEntity() instanceof LivingEntity) {
                    LivingEntity target = (LivingEntity) event.getEntity();

                    // Trust check
                    if (!(target instanceof Player) || !plugin.getTrustManager().isTrusted(player, (Player) target)) {
                        // Deal 1 heart (2 HP) of true damage
                        double currentHealth = target.getHealth();
                        target.setHealth(Math.max(0, currentHealth - 2.0));

                        // Particles and sound
                        target.getWorld().spawnParticle(Particle.CRIT, target.getLocation().add(0, 1, 0), 30, 0.3, 0.3, 0.3, 0.1);
                        target.getWorld().spawnParticle(Particle.DAMAGE_INDICATOR, target.getLocation().add(0, 1, 0), 5, 0.2, 0.2, 0.2, 0);
                        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_ATTACK_CRIT, 1.0f, 0.8f);

                        player.sendMessage(ChatColor.DARK_GRAY + "Backstab! +1 heart true damage!");

                        if (target instanceof Player) {
                            ((Player) target).sendMessage(ChatColor.RED + "⚔ Backstabbed by " + player.getName() + "!");
                        }
                    }
                }
            }

            // Soul Mark - only mark target if not already marked
            String legendaryId = LegendaryItemFactory.getLegendaryId(player.getInventory().getItemInMainHand());
            if (legendaryId != null && legendaryId.equals(LegendaryType.THOUSAND_DEMON_DAGGERS.getId())) {
                if (event.getEntity() instanceof LivingEntity && !soulMarkTargets.containsKey(player.getUniqueId())) {
                    LivingEntity target = (LivingEntity) event.getEntity();

                    // Trust check
                    if (target instanceof Player && plugin.getTrustManager().isTrusted(player, (Player) target)) {
                        return;
                    }

                    // Mark this target
                    soulMarkTargets.put(player.getUniqueId(), target.getUniqueId());

                    player.sendMessage(ChatColor.DARK_PURPLE + "Target marked! They will take true damage in 15 seconds.");

                    // Schedule true damage after 15 seconds
                    Bukkit.getScheduler().runTaskLater(plugin, () -> {
                        UUID markedUUID = soulMarkTargets.remove(player.getUniqueId());
                        if (markedUUID != null) {
                            Entity markedEntity = Bukkit.getEntity(markedUUID);
                            if (markedEntity instanceof LivingEntity && markedEntity.isValid() && !markedEntity.isDead()) {
                                LivingEntity markedLiving = (LivingEntity) markedEntity;

                                // True damage (ignore armor) - directly reduce health
                                double currentHealth = markedLiving.getHealth();
                                markedLiving.setHealth(Math.max(0, currentHealth - 6.0)); // 6 hearts = 3 hearts true damage

                                // Enhanced particles
                                markedLiving.getWorld().spawnParticle(Particle.SOUL, markedLiving.getLocation(), 200, 0.5, 1, 0.5, 0.1);
                                markedLiving.getWorld().spawnParticle(Particle.ENCHANT, markedLiving.getLocation(), 150, 0.5, 1, 0.5, 0.1);
                                markedLiving.getWorld().spawnParticle(Particle.WITCH, markedLiving.getLocation(), 100, 0.5, 1, 0.5, 0.05);
                                markedLiving.getWorld().playSound(markedLiving.getLocation(), Sound.ENTITY_WITHER_HURT, 1.0f, 0.7f);

                                // Notify victim
                                if (markedLiving instanceof Player) {
                                    ((Player) markedLiving).sendMessage(ChatColor.RED + "⚔ Hit by " + ChatColor.DARK_PURPLE + "Soul Mark True Damage" + ChatColor.RED + " from " + player.getName() + "!");
                                }

                                player.sendMessage(ChatColor.DARK_PURPLE + "Soul Mark triggered! 3 hearts of true damage dealt!");
                            }
                        }
                    }, 300L); // 15 seconds
                }
            }

            // Chrono Blade - Time Slow passive (first hit applies slow, 20s cooldown per target)
            if (legendaryId != null && legendaryId.equals(LegendaryType.CHRONO_BLADE.getId())) {
                if (event.getEntity() instanceof LivingEntity) {
                    LivingEntity target = (LivingEntity) event.getEntity();

                    // Trust check
                    if (target instanceof Player && plugin.getTrustManager().isTrusted(player, (Player) target)) {
                        // Skip trusted players
                    } else {
                        UUID playerUUID = player.getUniqueId();
                        UUID targetUUID = target.getUniqueId();

                        // Check cooldown for this specific target
                        Map<UUID, Long> targetCooldowns = timeSlowCooldowns.computeIfAbsent(playerUUID, k -> new HashMap<>());
                        long now = System.currentTimeMillis();
                        Long lastSlowed = targetCooldowns.get(targetUUID);

                        if (lastSlowed == null || now - lastSlowed >= 20000) { // 20 second cooldown
                            // Apply Time Slow - 20% slower movement and attack speed for 3 seconds
                            target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 60, 0, true, true)); // Slowness I = 20% slower
                            target.addPotionEffect(new PotionEffect(PotionEffectType.MINING_FATIGUE, 60, 0, true, true)); // Approximates attack speed reduction

                            // Record cooldown
                            targetCooldowns.put(targetUUID, now);

                            // Particles
                            target.getWorld().spawnParticle(Particle.ENCHANT, target.getLocation().add(0, 1, 0), 30, 0.3, 0.5, 0.3, 0.1);
                            target.getWorld().spawnParticle(Particle.REVERSE_PORTAL, target.getLocation().add(0, 1, 0), 20, 0.3, 0.5, 0.3, 0.3);
                            player.playSound(player.getLocation(), Sound.BLOCK_RESPAWN_ANCHOR_DEPLETE, 0.5f, 1.5f);

                            if (target instanceof Player) {
                                ((Player) target).sendMessage(ChatColor.YELLOW + "⏳ Time Slow! Movement and attack speed reduced!");
                            }
                        }
                    }
                }
            }

            // Soul Devourer - bonus damage based on soul count (+2 damage per soul)
            if (legendaryId != null && legendaryId.equals(LegendaryType.SOUL_DEVOURER.getId())) {
                int soulCount = LegendaryItemFactory.getSoulCount(player.getInventory().getItemInMainHand());
                if (soulCount > 0 && event.getEntity() instanceof LivingEntity) {
                    LivingEntity target = (LivingEntity) event.getEntity();

                    // Trust check
                    if (target instanceof Player && plugin.getTrustManager().isTrusted(player, (Player) target)) {
                        // Skip trusted players
                    } else {
                        // +2 damage per soul (so +4 HP damage per soul)
                        double bonusDamage = soulCount * 4.0; // 2 hearts = 4 HP per soul
                        event.setDamage(event.getDamage() + bonusDamage);

                        // Soul particles on hit
                        target.getWorld().spawnParticle(Particle.SOUL, target.getLocation().add(0, 1, 0), 5, 0.3, 0.3, 0.3, 0.05);
                    }
                }
            }
        }

        // Radiant Block
        if (event.getEntity() instanceof Player) {
            Player player = (Player) event.getEntity();

            if (radiantBlockActive.containsKey(player.getUniqueId())) {
                long endTime = radiantBlockActive.get(player.getUniqueId());
                if (System.currentTimeMillis() < endTime) {
                    // Reflect 75% damage
                    double reflectedDamage = event.getDamage() * 0.75;

                    if (event.getDamager() instanceof LivingEntity) {
                        LivingEntity attacker = (LivingEntity) event.getDamager();
                        // Trust check - don't reflect damage to trusted players
                        if (attacker instanceof Player && plugin.getTrustManager().isTrusted(player, (Player) attacker)) {
                            return;
                        }
                        attacker.damage(reflectedDamage);
                        player.sendMessage(ChatColor.GOLD + "Reflected " + String.format("%.1f", reflectedDamage) + " damage!");
                    }
                }
            }
        }
    }

    @EventHandler
    public void onPlayerConsume(PlayerItemConsumeEvent event) {
        Player player = event.getPlayer();

        // Eclipse Devourer - Dragon's Breath consumption
        if (event.getItem().getType() == Material.DRAGON_BREATH) {
            ItemStack mainHand = player.getInventory().getItemInMainHand();
            String legendaryId = LegendaryItemFactory.getLegendaryId(mainHand);

            if (legendaryId != null && legendaryId.equals(LegendaryType.CREATION_SPLITTER.getId())) {
                player.addPotionEffect(new PotionEffect(PotionEffectType.ABSORPTION, 200, 1));
                player.sendMessage(ChatColor.DARK_PURPLE + "Dragon's power absorbed! +2 Absorption hearts");
            }
        }
    }

    @EventHandler
    public void onTridentHit(ProjectileHitEvent event) {
        if (!(event.getEntity() instanceof org.bukkit.entity.Trident)) return;
        if (!(event.getEntity().getShooter() instanceof Player)) return;

        org.bukkit.entity.Trident trident = (org.bukkit.entity.Trident) event.getEntity();
        Player player = (Player) event.getEntity().getShooter();

        // Check if this is a Gale Throw
        if (nextGaleThrow.contains(player.getUniqueId())) {
            nextGaleThrow.remove(player.getUniqueId());

            Location hitLoc = trident.getLocation();

            // Wind vortex effect
            hitLoc.getWorld().spawnParticle(Particle.CLOUD, hitLoc, 300, 3, 3, 3, 0.3);
            hitLoc.getWorld().spawnParticle(Particle.SWEEP_ATTACK, hitLoc, 100, 3, 3, 3, 0.1);
            hitLoc.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, hitLoc, 150, 3, 3, 3, 0.2);

            hitLoc.getWorld().playSound(hitLoc, Sound.ENTITY_ENDER_DRAGON_FLAP, 2.0f, 1.5f);
            hitLoc.getWorld().playSound(hitLoc, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 1.0f, 2.0f);

            // Damage and pull entities in 5 block radius
            for (Entity entity : hitLoc.getWorld().getNearbyEntities(hitLoc, 5, 5, 5)) {
                if (entity instanceof LivingEntity && entity != player) {
                    // Trust check
                    if (entity instanceof Player && plugin.getTrustManager().isTrusted(player, (Player) entity)) {
                        continue;
                    }

                    LivingEntity living = (LivingEntity) entity;

                    // Pull toward vortex center
                    Vector direction = hitLoc.toVector().subtract(living.getLocation().toVector()).normalize();
                    living.setVelocity(direction.multiply(1.5).setY(0.8));

                    // Damage
                    living.damage(25.0, player); // ~5 hearts through full Prot 4

                    // Levitation effect
                    living.addPotionEffect(new PotionEffect(PotionEffectType.LEVITATION, 20, 5));
                    living.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 40, 2));

                    // Notify victim
                    if (living instanceof Player) {
                        ((Player) living).sendMessage(ChatColor.RED + "⚔ Hit by " + ChatColor.AQUA + "Gale Throw" + ChatColor.RED + " from " + player.getName() + "!");
                    }
                }
            }

            player.sendMessage(ChatColor.AQUA + "Wind Vortex!");
        }
    }

    // ========== COPPER EXCAVATOR ==========

    private boolean toggle3x3Mining(Player player) {
        if (mining3x3Active.contains(player.getUniqueId())) {
            mining3x3Active.remove(player.getUniqueId());
            player.sendMessage(ChatColor.GOLD + "3x3 Mining: " + ChatColor.RED + "Disabled");
        } else {
            mining3x3Active.add(player.getUniqueId());
            player.sendMessage(ChatColor.GOLD + "3x3 Mining: " + ChatColor.GREEN + "Enabled");
        }
        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 1.5f);
        return true;
    }

    private boolean toggleEnchantMode(Player player) {
        if (fortuneModeActive.contains(player.getUniqueId())) {
            fortuneModeActive.remove(player.getUniqueId());
            player.sendMessage(ChatColor.GOLD + "Enchantment Mode: " + ChatColor.AQUA + "Silk Touch");
        } else {
            fortuneModeActive.add(player.getUniqueId());
            player.sendMessage(ChatColor.GOLD + "Enchantment Mode: " + ChatColor.GREEN + "Fortune III");
        }
        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 1.5f);
        return true;
    }

    public boolean is3x3MiningActive(UUID playerId) {
        return mining3x3Active.contains(playerId);
    }

    public boolean isFortuneModeActive(UUID playerId) {
        return fortuneModeActive.contains(playerId);
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        // Prevent breaking Prison of the Damned blocks
        if (activePrisonBlocks.contains(event.getBlock().getLocation())) {
            event.setCancelled(true);
            event.getPlayer().sendMessage(ChatColor.DARK_GRAY + "This block is bound by dark magic!");
        }
    }

    // Helper class for Time Rewind
    private static class SavedState {
        Location location;
        double health;
        int hunger;
        int taskId; // For cancelling scheduled teleport

        SavedState(Location location, double health, int hunger) {
            this.location = location;
            this.health = health;
            this.hunger = hunger;
            this.taskId = -1;
        }
    }

    // Helper class for Heaven's Wall barrier
    private static class HeavensWallBarrier {
        UUID ownerUUID;
        World world;
        double minX, maxX, minZ, maxZ;
        double minY, maxY; // Y bounds for the barrier

        HeavensWallBarrier(UUID ownerUUID, World world, double minX, double maxX, double minZ, double maxZ, double centerY) {
            this.ownerUUID = ownerUUID;
            this.world = world;
            this.minX = minX;
            this.maxX = maxX;
            this.minZ = minZ;
            this.maxZ = maxZ;
            // Barrier extends 50 blocks up and down from center (effectively infinite for gameplay)
            this.minY = centerY - 50;
            this.maxY = centerY + 50;
        }

        boolean isInside(double x, double z) {
            return x >= minX && x <= maxX && z >= minZ && z <= maxZ;
        }

        // Check if a location is on the barrier wall itself (within 0.5 blocks of the edge)
        boolean isOnWallEdge(double x, double y, double z) {
            if (y < minY || y > maxY) return false;

            boolean nearMinX = Math.abs(x - minX) < 0.5;
            boolean nearMaxX = Math.abs(x - maxX) < 0.5;
            boolean nearMinZ = Math.abs(z - minZ) < 0.5;
            boolean nearMaxZ = Math.abs(z - maxZ) < 0.5;

            boolean withinZ = z >= minZ && z <= maxZ;
            boolean withinX = x >= minX && x <= maxX;

            return (nearMinX && withinZ) || (nearMaxX && withinZ) ||
                   (nearMinZ && withinX) || (nearMaxZ && withinX);
        }

        // Check if a line segment (from -> to) crosses the barrier
        boolean crossesBarrier(Location from, Location to) {
            if (!world.equals(from.getWorld())) return false;

            boolean fromInside = isInside(from.getX(), from.getZ());
            boolean toInside = isInside(to.getX(), to.getZ());

            // Check Y bounds
            if (from.getY() < minY && to.getY() < minY) return false;
            if (from.getY() > maxY && to.getY() > maxY) return false;

            return fromInside != toInside;
        }
    }
}
