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

    // Lantern of Lost Names - tracking kills and attack cooldown
    private Map<UUID, Set<UUID>> lanternKills; // Player UUID -> Set of players they've killed
    private Map<UUID, Long> lanternAttackCooldown; // Player UUID -> when cooldown ends

    // Rift Key of the Endkeeper - portal tracking
    private Map<UUID, Location> activeRiftPortals; // Player UUID -> portal location
    private Map<UUID, Long> riftKeyLastUse; // Player UUID -> last use timestamp (for 24h cooldown)

    // Chaos Dice of Fate - active effects
    private Set<UUID> chaosDiceTrackerActive; // Players with tracker effect active
    private Set<UUID> chaosDiceInstaCrit; // Players with insta-crit active
    private Map<UUID, UUID> chaosGolemOwners; // Golem UUID -> Owner UUID

    // Combat tracking for Rift Key
    private Map<UUID, Long> combatTagTime; // Player UUID -> last combat time
    private static final long COMBAT_TAG_DURATION = 20000; // 20 seconds

    // Track when abilities are dealing damage (to exclude from Forge Chestplate melee counter)
    private Set<UUID> abilityDamageActive = new HashSet<>();

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

        // New legendary items
        this.lanternKills = new HashMap<>();
        this.lanternAttackCooldown = new HashMap<>();
        this.activeRiftPortals = new HashMap<>();
        this.riftKeyLastUse = new HashMap<>();
        this.chaosDiceTrackerActive = new HashSet<>();
        this.chaosDiceInstaCrit = new HashSet<>();
        this.chaosGolemOwners = new HashMap<>();
        this.combatTagTime = new HashMap<>();

        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    /**
     * Deal ability damage to an entity. This marks the damage as ability damage
     * so it won't count toward Forge Chestplate melee hit counter.
     */
    private void dealAbilityDamage(LivingEntity target, double damage, Player source) {
        abilityDamageActive.add(source.getUniqueId());
        target.damage(damage, source);
        abilityDamageActive.remove(source.getUniqueId());
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

        // Set cooldown if successful (skip for Chaos Dice ability 2 if free scans active)
        if (success) {
            boolean skipCooldown = (type == LegendaryType.CHAOS_DICE_OF_FATE && abilityNum == 2
                && hasFreeScan(player.getUniqueId()));
            if (!skipCooldown) {
                plugin.getCooldownManager().setCooldown(player.getUniqueId(), legendaryId, abilityNum, cooldown);
            }
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
            case DRAGONBORN_BLADE:
                return endSever(player);
            case FORGE_PICKAXE:
                return toggle3x3Mining(player);
            case RIFT_KEY_OF_ENDKEEPER:
                return openEndRift(player);
            case CHAOS_DICE_OF_FATE:
                return rollChaosDice(player);
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
            case DRAGONBORN_BLADE:
                return cataclysmPulse(player);
            case FORGE_PICKAXE:
                return toggleEnchantMode(player);
            case CHAOS_DICE_OF_FATE:
                if (!chaosDiceTrackerActive.contains(player.getUniqueId())) {
                    player.sendMessage(ChatColor.RED + "You need the Hunter's Instinct buff to use Player Scan!");
                    return false;
                }
                return scanForPlayers(player);
        }
        return false;
    }

    // ========== HOLY MOONLIGHT SWORD ==========

    private boolean starRiftSlash(Player player) {
        // Play attack animation
        player.swingMainHand();

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
                    dealAbilityDamage(victim, 35.0, player); // ~7 hearts through full Prot 4

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
        // Play attack animation
        player.swingMainHand();

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

                // Deal 1.5 hearts (3 HP) as true damage - bypasses armor
                double damage = 3.0;
                double newHealth = living.getHealth() - damage;
                if (newHealth <= 0) {
                    dealAbilityDamage(living, 1000, player); // Kill them
                } else {
                    living.setHealth(newHealth);
                }
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
        // Play attack animation
        player.swingMainHand();

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
                        dealAbilityDamage(living, 30.0, player);
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
        // Play attack animation
        player.swingMainHand();

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
        // Play attack animation
        player.swingMainHand();

        // Verdant Cyclone - 360° spin attack with leaves and wind
        Location center = player.getLocation();
        World world = center.getWorld();
        final float startYaw = player.getLocation().getYaw();

        // Spin the player's camera 360 degrees over 10 ticks (0.5 seconds)
        new BukkitRunnable() {
            int ticks = 0;
            final int totalTicks = 10;

            @Override
            public void run() {
                if (ticks >= totalTicks || !player.isOnline()) {
                    // Reset to original yaw at the end
                    Location loc = player.getLocation();
                    loc.setYaw(startYaw);
                    player.teleport(loc);
                    cancel();
                    return;
                }

                // Calculate new yaw (360 degrees over totalTicks)
                float newYaw = startYaw + (360f * ticks / totalTicks);
                Location loc = player.getLocation();
                loc.setYaw(newYaw);
                player.teleport(loc);

                ticks++;
            }
        }.runTaskTimer(plugin, 0L, 1L);

        // Deal damage at the halfway point of the spin
        new BukkitRunnable() {
            @Override
            public void run() {
                int enemiesHit = 0;

                // Hit all entities in 5 block radius
                for (Entity entity : world.getNearbyEntities(center, 5, 3, 5)) {
                    if (entity instanceof LivingEntity && entity != player) {
                        LivingEntity living = (LivingEntity) entity;

                        // Trust check
                        if (living instanceof Player && plugin.getTrustManager().isTrusted(player, (Player) living)) {
                            continue;
                        }

                        // Deal 20 damage = 2 hearts after full prot 4 diamond (~80% reduction)
                        dealAbilityDamage(living, 20.0, player);

                        // Push enemies back
                        Vector knockback = living.getLocation().toVector().subtract(center.toVector()).normalize();
                        knockback.setY(0.3);
                        living.setVelocity(knockback.multiply(1.5));

                        enemiesHit++;

                        // Particles on each hit target
                        world.spawnParticle(Particle.CHERRY_LEAVES, living.getLocation().add(0, 1, 0), 30, 0.3, 0.5, 0.3, 0.1);

                        if (living instanceof Player) {
                            ((Player) living).sendMessage(ChatColor.RED + "Hit by " + ChatColor.GREEN + "Verdant Cyclone" + ChatColor.RED + " from " + player.getName() + "!");
                        }
                    }
                }

                player.sendMessage(ChatColor.GREEN + "Verdant Cyclone! Hit " + enemiesHit + " enemies!");
            }
        }.runTaskLater(plugin, 5L); // Damage at halfway through spin

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

        return true;
    }

    // ========== CHAINS OF ETERNITY ==========

    private boolean soulBind(Player player) {
        // Play attack animation
        player.swingMainHand();

        // Find target using a forgiving cone-based search (easier to aim)
        Location eyeLoc = player.getEyeLocation();
        Vector direction = eyeLoc.getDirection().normalize();
        LivingEntity target = null;
        double bestScore = Double.MAX_VALUE;

        // Search in a cone: check entities within 20 blocks, prefer those closer to crosshair
        for (Entity entity : player.getWorld().getNearbyEntities(eyeLoc, 20, 20, 20)) {
            if (!(entity instanceof LivingEntity) || entity == player) continue;
            if (entity instanceof Player && plugin.getTrustManager().isTrusted(player, (Player) entity)) continue;

            LivingEntity living = (LivingEntity) entity;
            Vector toEntity = living.getEyeLocation().toVector().subtract(eyeLoc.toVector());
            double distance = toEntity.length();
            if (distance > 20) continue;

            toEntity.normalize();
            double dot = direction.dot(toEntity); // 1.0 = looking directly at, 0 = perpendicular

            // Accept targets within ~30 degree cone (dot > 0.85)
            if (dot > 0.85) {
                // Score based on how close to crosshair (prefer more centered targets)
                double score = distance * (2 - dot); // Lower is better
                if (score < bestScore) {
                    bestScore = score;
                    target = living;
                }
            }
        }

        if (target == null) {
            player.sendMessage(ChatColor.RED + "No target found!");
            return false;
        }

        final LivingEntity finalTarget = target;
        final Location playerLoc = player.getLocation();
        final Location targetLoc = target.getLocation();

        // Chain particles from target to player
        Location start = targetLoc.clone().add(0, 1, 0);
        Location end = playerLoc.clone().add(0, 1, 0);
        Vector step = end.toVector().subtract(start.toVector()).normalize().multiply(0.5);
        Location current = start.clone();
        for (int i = 0; i < 40; i++) {
            player.getWorld().spawnParticle(Particle.SOUL, current, 3, 0.1, 0.1, 0.1, 0);
            current.add(step);
            if (current.distance(end) < 0.5) break;
        }

        // Calculate destination: 2 blocks in front of player
        Vector playerDirection = playerLoc.getDirection().setY(0).normalize();
        final Location destination = playerLoc.clone().add(playerDirection.multiply(2.0));
        destination.setY(playerLoc.getY());

        // Smooth pull using repeating task
        new BukkitRunnable() {
            int ticks = 0;
            final int maxTicks = 20; // 1 second max pull time

            @Override
            public void run() {
                if (ticks >= maxTicks || !finalTarget.isValid() || !player.isOnline()) {
                    // Stop and apply effects
                    finalTarget.setVelocity(new Vector(0, 0, 0));
                    abilityDamageActive.add(player.getUniqueId());
                    finalTarget.damage(20.0, player);
                    abilityDamageActive.remove(player.getUniqueId());
                    finalTarget.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 60, 4));
                    finalTarget.getWorld().spawnParticle(Particle.SOUL, finalTarget.getLocation(), 120, 0.5, 1, 0.5, 0);
                    finalTarget.getWorld().spawnParticle(Particle.ENCHANT, finalTarget.getLocation(), 80, 0.5, 1, 0.5, 0.1);
                    cancel();
                    return;
                }

                // Check if target reached destination (within 2.5 blocks of player)
                double distanceToPlayer = finalTarget.getLocation().distance(player.getLocation());
                if (distanceToPlayer <= 2.5) {
                    // Stop them right here
                    finalTarget.setVelocity(new Vector(0, 0, 0));
                    abilityDamageActive.add(player.getUniqueId());
                    finalTarget.damage(20.0, player);
                    abilityDamageActive.remove(player.getUniqueId());
                    finalTarget.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 60, 4));
                    finalTarget.getWorld().spawnParticle(Particle.SOUL, finalTarget.getLocation(), 120, 0.5, 1, 0.5, 0);
                    finalTarget.getWorld().spawnParticle(Particle.ENCHANT, finalTarget.getLocation(), 80, 0.5, 1, 0.5, 0.1);
                    cancel();
                    return;
                }

                // Pull towards player's current position (2 blocks in front)
                Location currentPlayerLoc = player.getLocation();
                Vector currentDirection = currentPlayerLoc.getDirection().setY(0).normalize();
                Location currentDestination = currentPlayerLoc.clone().add(currentDirection.multiply(2.0));
                currentDestination.setY(currentPlayerLoc.getY());

                Vector pullDir = currentDestination.toVector().subtract(finalTarget.getLocation().toVector());
                double distance = pullDir.length();
                pullDir.normalize();

                // Pull speed scales with distance - faster when far, slower when close
                double speed = Math.min(2.0, Math.max(0.5, distance * 0.3));
                pullDir.multiply(speed);
                pullDir.setY(0.1); // Slight lift to avoid ground friction

                finalTarget.setVelocity(pullDir);

                // Trail particles during pull
                finalTarget.getWorld().spawnParticle(Particle.SOUL, finalTarget.getLocation().add(0, 1, 0), 5, 0.2, 0.2, 0.2, 0.02);

                ticks++;
            }
        }.runTaskTimer(plugin, 0L, 1L);

        // Enhanced particles at original target location
        target.getWorld().spawnParticle(Particle.SOUL, targetLoc, 60, 0.5, 1, 0.5, 0);

        // Notify victim
        if (target instanceof Player) {
            ((Player) target).sendMessage(ChatColor.RED + "⚔ Hit by " + ChatColor.DARK_GRAY + "Soul Bind" + ChatColor.RED + " from " + player.getName() + "!");
        }

        player.sendMessage(ChatColor.DARK_GRAY + "Soul Bind!");
        return true;
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
                    // Floor (y = -1 and y = -2) - double layer barrier so they can't dig out
                    for (int floorY = -1; floorY >= -2; floorY--) {
                        Location floorLoc = center.clone().add(x, floorY, z);
                        if (floorLoc.getBlock().getType() == Material.AIR || floorLoc.getBlock().isPassable()) {
                            floorLoc.getBlock().setType(Material.BARRIER);
                            cageBlocks.add(floorLoc.getBlock().getLocation());
                        }
                    }

                    // Ceiling (y = 3) - use iron bars
                    Location ceilingLoc = center.clone().add(x, 3, z);
                    if (ceilingLoc.getBlock().getType() == Material.AIR || ceilingLoc.getBlock().isPassable()) {
                        ceilingLoc.getBlock().setType(Material.IRON_BARS);
                        cageBlocks.add(ceilingLoc.getBlock().getLocation());
                    }

                    // Walls - only place on the outer edges (not center column, and only edges)
                    if (Math.abs(x) == 1 || Math.abs(z) == 1) {
                        for (int y = 0; y <= 2; y++) {
                            Location loc = center.clone().add(x, y, z);
                            if (loc.getBlock().getType() == Material.AIR || loc.getBlock().isPassable()) {
                                loc.getBlock().setType(Material.IRON_BARS);
                                cageBlocks.add(loc.getBlock().getLocation());
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

    // ========== COPPER BOOTS ==========

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

        // Save the original world border settings
        WorldBorder border = world.getWorldBorder();
        double originalSize = border.getSize();
        Location originalCenter = border.getCenter();
        double originalDamageBuffer = border.getDamageBuffer();
        double originalDamageAmount = border.getDamageAmount();
        int originalWarningDistance = border.getWarningDistance();
        int originalWarningTime = border.getWarningTime();

        // Set the world border to 16x16 centered on the player
        border.setCenter(center);
        border.setSize(16);
        border.setDamageAmount(0);
        border.setDamageBuffer(1000); // High buffer so trusted players can walk through
        border.setWarningDistance(0);
        border.setWarningTime(0);

        // Store barrier info for movement blocking
        double minX = center.getX() - 8;
        double maxX = center.getX() + 8;
        double minZ = center.getZ() - 8;
        double maxZ = center.getZ() + 8;

        HeavensWallBarrier barrierInfo = new HeavensWallBarrier(player.getUniqueId(), world, minX, maxX, minZ, maxZ, center.getY());
        activeBarriers.put(player.getUniqueId(), barrierInfo);

        // Schedule border reset after 32 seconds
        new BukkitRunnable() {
            @Override
            public void run() {
                // Restore original world border
                border.setCenter(originalCenter);
                border.setSize(originalSize);
                border.setDamageAmount(originalDamageAmount);
                border.setDamageBuffer(originalDamageBuffer);
                border.setWarningDistance(originalWarningDistance);
                border.setWarningTime(originalWarningTime);
                activeBarriers.remove(player.getUniqueId());
                player.sendMessage(ChatColor.GOLD + "Heaven's Wall has faded.");
            }
        }.runTaskLater(plugin, 640L); // 32 seconds

        player.sendMessage(ChatColor.GOLD + "Heaven's Wall activated! World border for 32 seconds.");
        player.sendMessage(ChatColor.GRAY + "Trusted players can pass through freely.");
        player.playSound(player.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, 1.0f, 1.0f);
        return true;
    }

    @EventHandler(priority = org.bukkit.event.EventPriority.HIGH)
    public void onHeavensWallMove(PlayerMoveEvent event) {
        if (activeBarriers.isEmpty()) return;

        Player player = event.getPlayer();
        Location from = event.getFrom();
        Location to = event.getTo();
        if (to == null) return;

        // Check if player actually moved position
        if (from.getX() == to.getX() && from.getY() == to.getY() && from.getZ() == to.getZ()) {
            return;
        }

        for (Map.Entry<UUID, HeavensWallBarrier> entry : activeBarriers.entrySet()) {
            HeavensWallBarrier barrier = entry.getValue();
            UUID ownerUUID = entry.getKey();

            if (!barrier.world.equals(player.getWorld())) continue;

            boolean wasInside = barrier.isInside(from.getX(), from.getZ());
            boolean willBeInside = barrier.isInside(to.getX(), to.getZ());

            // Owner is always blocked from leaving
            if (player.getUniqueId().equals(ownerUUID)) {
                if (wasInside && !willBeInside) {
                    event.setTo(from);
                    player.setVelocity(from.toVector().subtract(to.toVector()).normalize().multiply(0.5));
                    return;
                }
                continue;
            }

            // Trusted players can pass through freely
            Player owner = Bukkit.getPlayer(ownerUUID);
            if (owner != null && plugin.getTrustManager().isTrusted(owner, player)) {
                continue;
            }

            // Non-trusted players are blocked from crossing
            if (wasInside != willBeInside) {
                event.setTo(from);
                player.setVelocity(from.toVector().subtract(to.toVector()).normalize().multiply(0.5));
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
        // Play attack animation
        player.swingMainHand();

        Vector direction = player.getLocation().getDirection().setY(0).normalize();
        Location start = player.getLocation().add(0, 1, 0);
        World world = player.getWorld();

        Set<UUID> hitEntities = new HashSet<>();

        // Soul horizontal slice (optimized - fewer iterations)
        for (int i = 2; i <= 8; i += 2) { // Every 2 blocks
            Location point = start.clone().add(direction.clone().multiply(i));

            // Wide horizontal sweep
            for (double offset = -2.5; offset <= 2.5; offset += 1.0) { // Every 1 block instead of 0.3
                Vector perpendicular = direction.clone().crossProduct(new Vector(0, 1, 0)).normalize().multiply(offset);
                Location sweepPoint = point.clone().add(perpendicular);

                // Soul particles
                world.spawnParticle(Particle.SOUL, sweepPoint, 4, 0.15, 0.1, 0.15, 0.05);
                world.spawnParticle(Particle.SOUL_FIRE_FLAME, sweepPoint, 3, 0.15, 0.1, 0.15, 0.02);
            }
        }

        // Check for entities in the slice area (more efficient single check)
        for (Entity entity : world.getNearbyEntities(start, 9, 2, 6)) {
            if (entity instanceof LivingEntity && entity != player && !hitEntities.contains(entity.getUniqueId())) {
                Vector toEntity = entity.getLocation().toVector().subtract(start.toVector());
                double dot = toEntity.normalize().dot(direction);
                double distance = start.distance(entity.getLocation());

                if (dot > 0.2 && distance <= 8) { // In front, within range
                    // Trust check
                    if (entity instanceof Player && plugin.getTrustManager().isTrusted(player, (Player) entity)) {
                        continue;
                    }
                    LivingEntity living = (LivingEntity) entity;
                    dealAbilityDamage(living, 20.0, player);
                    hitEntities.add(entity.getUniqueId());

                    // Soul particles on hit
                    world.spawnParticle(Particle.SOUL, living.getLocation().add(0, 1, 0), 15, 0.3, 0.4, 0.3, 0.08);
                    world.spawnParticle(Particle.SOUL_FIRE_FLAME, living.getLocation().add(0, 1, 0), 10, 0.3, 0.4, 0.3, 0.03);

                    if (living instanceof Player) {
                        ((Player) living).sendMessage(ChatColor.RED + "⚔ Hit by " + ChatColor.DARK_RED + "Void Slice" + ChatColor.RED + " from " + player.getName() + "!");
                    }
                }
            }
        }

        world.playSound(start, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1.5f, 0.5f);
        world.playSound(start, Sound.ENTITY_WITHER_SHOOT, 1.0f, 0.6f);
        player.sendMessage(ChatColor.DARK_RED + "Void Slice!");
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

                // Soul vortex effect - simplified spiral
                for (double angle = 0; angle < 360; angle += 60) { // 6 arms instead of 24
                    double radians = Math.toRadians(angle + ticks * 20);
                    for (double r = 6; r >= 1; r -= 1.5) { // Fewer points per arm
                        double x = r * Math.cos(radians + r * 0.5);
                        double z = r * Math.sin(radians + r * 0.5);
                        double y = (6 - r) * 0.2;
                        Location spiralPoint = riftLocation.clone().add(x, y, z);

                        world.spawnParticle(Particle.SOUL, spiralPoint, 2, 0.1, 0.1, 0.1, 0.05);
                        world.spawnParticle(Particle.SOUL_FIRE_FLAME, spiralPoint, 1, 0.1, 0.1, 0.1, 0.02);
                    }
                }

                // Center soul particles
                world.spawnParticle(Particle.SOUL, riftLocation, 25, 0.8, 0.8, 0.8, 0.1);
                world.spawnParticle(Particle.SOUL_FIRE_FLAME, riftLocation, 20, 1.0, 1.0, 1.0, 0.08);
                world.spawnParticle(Particle.DUST, riftLocation, 15, 0.8, 0.8, 0.8,
                    new Particle.DustOptions(Color.fromRGB(85, 200, 200), 2.5f)); // Soul blue color

                // Soul pull-in lines (reduced)
                for (int i = 0; i < 4; i++) {
                    double pullAngle = Math.random() * Math.PI * 2;
                    double dist = 4 + Math.random() * 4;
                    Location pullStart = riftLocation.clone().add(
                        Math.cos(pullAngle) * dist,
                        (Math.random() - 0.5) * 3,
                        Math.sin(pullAngle) * dist
                    );
                    world.spawnParticle(Particle.SOUL, pullStart, 3, 0.1, 0.1, 0.1, 0.08);
                    world.spawnParticle(Particle.SOUL_FIRE_FLAME, pullStart, 2, 0.1, 0.1, 0.1, 0.05);
                }

                // Pull entities (unchanged - this is gameplay)
                for (Entity entity : world.getNearbyEntities(riftLocation, 10, 10, 10)) {
                    if (entity instanceof LivingEntity && entity != player) {
                        if (entity instanceof Player && plugin.getTrustManager().isTrusted(player, (Player) entity)) {
                            continue;
                        }

                        double distance = entity.getLocation().distance(riftLocation);
                        Vector dir = riftLocation.toVector().subtract(entity.getLocation().toVector()).normalize();
                        double pullStrength = Math.min(0.8, 5.0 / (distance + 1));
                        entity.setVelocity(dir.multiply(pullStrength));

                        if (distance < 2.5) {
                            LivingEntity living = (LivingEntity) entity;
                            dealAbilityDamage(living, 5.0, player);

                            if (!damagedEntities.contains(entity.getUniqueId())) {
                                damagedEntities.add(entity.getUniqueId());
                                if (living instanceof Player) {
                                    ((Player) living).sendMessage(ChatColor.RED + "⚔ Hit by " + ChatColor.DARK_RED + "Void Rift" + ChatColor.RED + " from " + player.getName() + "!");
                                }
                            }
                        }
                    }
                }

                if (ticks % 20 == 0) {
                    world.playSound(riftLocation, Sound.BLOCK_PORTAL_AMBIENT, 1.5f, 0.5f);
                }

                ticks += 5;
            }
        }.runTaskTimer(plugin, 0L, 5L);

        world.playSound(riftLocation, Sound.ENTITY_WITHER_AMBIENT, 1.5f, 1.5f);
        world.playSound(riftLocation, Sound.PARTICLE_SOUL_ESCAPE, 2.0f, 0.8f);
        player.sendMessage(ChatColor.DARK_AQUA + "Void Rift!");
        return true;
    }

    // ========== CREATION SPLITTER ==========

    private boolean endSever(Player player) {
        Vector direction = player.getLocation().getDirection().setY(0).normalize();
        Location playerLoc = player.getLocation();
        World world = player.getWorld();

        Set<UUID> hitEntities = new HashSet<>();

        // Swing the blade animation - create arc slash effect
        player.swingMainHand();

        // Sound effects - dragon sword swing
        world.playSound(playerLoc, Sound.ENTITY_ENDER_DRAGON_FLAP, 1.5f, 1.2f);
        world.playSound(playerLoc, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1.5f, 0.8f);
        world.playSound(playerLoc, Sound.ENTITY_ENDER_DRAGON_HURT, 0.8f, 1.5f);

        // Create 12-block wide sweeping arc slash in front (optimized - fewer particle calls)
        for (int distance = 2; distance <= 12; distance += 2) { // Every 2 blocks instead of every block
            // Wide arc sweep from left to right (-60 to +60 degrees)
            for (double angle = -60; angle <= 60; angle += 20) { // Every 20 degrees instead of 8
                Vector rotated = rotateVector(direction.clone(), angle);
                Location point = playerLoc.clone().add(0, 1, 0).add(rotated.multiply(distance));

                // Purple witch slash arc particles
                world.spawnParticle(Particle.WITCH, point, 10, 0.2, 0.15, 0.2, 0.1);
                world.spawnParticle(Particle.DRAGON_BREATH, point, 3, 0.1, 0.1, 0.1, 0.02);

                // Sweep indicator particles along the arc
                if (distance % 4 == 0) {
                    world.spawnParticle(Particle.SWEEP_ATTACK, point, 1, 0, 0, 0, 0);
                }
            }
        }

        // Check for entities in the entire arc area (more efficient than per-point)
        for (Entity entity : world.getNearbyEntities(playerLoc, 13, 3, 13)) {
            if (entity instanceof LivingEntity && entity != player && !hitEntities.contains(entity.getUniqueId())) {
                // Check if entity is in front arc
                Vector toEntity = entity.getLocation().toVector().subtract(playerLoc.toVector());
                double dot = toEntity.normalize().dot(direction);
                double distance = playerLoc.distance(entity.getLocation());

                if (dot > 0.3 && distance <= 12) { // In front 120-degree arc, within 12 blocks
                    // Trust check
                    if (entity instanceof Player && plugin.getTrustManager().isTrusted(player, (Player) entity)) {
                        continue;
                    }

                    LivingEntity living = (LivingEntity) entity;
                    hitEntities.add(entity.getUniqueId());

                    // 2.5 hearts damage through full Prot 4 diamond (~12.5 raw damage)
                    dealAbilityDamage(living, 12.5, player);
                    living.setNoDamageTicks(0);

                    // Weakness for 4 seconds
                    living.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, 80, 0));

                    // Brief levitation (0.5s)
                    living.addPotionEffect(new PotionEffect(PotionEffectType.LEVITATION, 10, 0));

                    // Purple witch slash effects on hit
                    world.spawnParticle(Particle.WITCH, living.getLocation().add(0, 1, 0), 60, 0.5, 0.6, 0.5, 0.15);
                    world.spawnParticle(Particle.DRAGON_BREATH, living.getLocation().add(0, 1, 0), 15, 0.3, 0.4, 0.3, 0.05);
                    world.spawnParticle(Particle.SWEEP_ATTACK, living.getLocation().add(0, 1, 0), 2, 0.2, 0.2, 0.2, 0);

                    // Notify victim
                    if (living instanceof Player) {
                        ((Player) living).sendMessage(ChatColor.RED + "⚔ Hit by " + ChatColor.DARK_PURPLE + "End Sever" + ChatColor.RED + " from " + player.getName() + "!");
                    }
                }
            }
        }

        player.sendMessage(ChatColor.DARK_PURPLE + "End Sever!");
        return true;
    }

    private boolean cataclysmPulse(Player player) {
        // Dragon Dash - Smooth velocity-based dash in the direction player is facing (including up/down)
        Location startLoc = player.getLocation();
        Vector direction = startLoc.getDirection().normalize(); // Full 3D direction - can dash up/down!
        World world = startLoc.getWorld();

        Set<UUID> hitEntities = new HashSet<>();

        // Dragon wing flap and roar sounds at start
        world.playSound(startLoc, Sound.ENTITY_ENDER_DRAGON_FLAP, 2.0f, 0.8f);
        world.playSound(startLoc, Sound.ENTITY_ENDER_DRAGON_GROWL, 1.5f, 1.2f);

        // Initial particles at start - purple witch dragon wing burst
        world.spawnParticle(Particle.WITCH, startLoc.clone().add(0, 1, 0), 80, 1.2, 1.0, 1.2, 0.15);
        world.spawnParticle(Particle.DRAGON_BREATH, startLoc.clone().add(0, 1, 0), 20, 0.8, 0.6, 0.8, 0.1);

        player.sendMessage(ChatColor.DARK_PURPLE + "Dragon Dash!");

        // Use velocity for smooth movement - 15 blocks over ~0.3 seconds (6 ticks)
        // Velocity = distance / time in ticks * 20 (ticks per second)
        // 15 blocks in 6 ticks = 15 / 6 * 20 = 50 blocks/second velocity
        final double dashVelocity = 3.5; // Strong forward push
        final int dashDuration = 6; // ticks

        // Apply initial velocity boost (full 3D direction)
        player.setVelocity(direction.clone().multiply(dashVelocity));

        new BukkitRunnable() {
            int tick = 0;
            Location lastLoc = startLoc.clone();

            @Override
            public void run() {
                if (tick >= dashDuration || !player.isOnline()) {
                    // Final dragon roar at end
                    world.playSound(player.getLocation(), Sound.ENTITY_ENDER_DRAGON_FLAP, 1.5f, 1.0f);
                    // Stop momentum at end
                    player.setVelocity(player.getVelocity().multiply(0.3));
                    cancel();
                    return;
                }

                // Keep pushing player in dash direction (full 3D - can go up/down)
                Vector targetVel = direction.clone().multiply(dashVelocity);
                // Apply full 3D velocity for directional dashing
                player.setVelocity(targetVel);

                // Check if we hit a wall (check in dash direction at feet and head level)
                Location checkLoc = player.getLocation().add(direction.clone().multiply(0.5));
                Location checkLocHead = checkLoc.clone().add(0, 1.8, 0);
                if (!checkLoc.getBlock().isPassable() || !checkLocHead.getBlock().isPassable()) {
                    player.setVelocity(new Vector(0, 0, 0));
                    cancel();
                    return;
                }

                // Purple witch trail behind player
                Location trailLoc = player.getLocation().add(0, 1, 0);
                world.spawnParticle(Particle.WITCH, trailLoc, 30, 0.5, 0.5, 0.5, 0.12);
                world.spawnParticle(Particle.DRAGON_BREATH, trailLoc, 8, 0.3, 0.3, 0.3, 0.05);

                // Dragon wing particles on sides with witch
                Vector perpendicular = direction.clone().crossProduct(new Vector(0, 1, 0)).normalize();
                Location leftWing = trailLoc.clone().add(perpendicular.clone().multiply(1.5));
                Location rightWing = trailLoc.clone().add(perpendicular.clone().multiply(-1.5));
                world.spawnParticle(Particle.WITCH, leftWing, 15, 0.4, 0.5, 0.4, 0.1);
                world.spawnParticle(Particle.WITCH, rightWing, 15, 0.4, 0.5, 0.4, 0.1);

                // Wing flap sound during dash
                if (tick % 2 == 0) {
                    world.playSound(player.getLocation(), Sound.ENTITY_ENDER_DRAGON_FLAP, 0.8f, 1.5f);
                }

                // Check for entities we pass through (wider hitbox for smooth dash)
                for (Entity entity : world.getNearbyEntities(player.getLocation(), 2.5, 2.5, 2.5)) {
                    if (entity instanceof LivingEntity && entity != player && !hitEntities.contains(entity.getUniqueId())) {
                        // Trust check
                        if (entity instanceof Player && plugin.getTrustManager().isTrusted(player, (Player) entity)) {
                            continue;
                        }

                        LivingEntity living = (LivingEntity) entity;
                        hitEntities.add(entity.getUniqueId());

                        // 4 hearts damage through full Prot 4 diamond (~20 raw damage)
                        dealAbilityDamage(living, 20.0, player);

                        // Stun for 0.5 seconds (10 ticks) - slowness + no jump
                        living.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 10, 255)); // Can't move
                        living.addPotionEffect(new PotionEffect(PotionEffectType.JUMP_BOOST, 10, 250)); // Can't jump (negative)

                        // Purple witch slash effect on hit
                        world.spawnParticle(Particle.WITCH, living.getLocation().add(0, 1, 0), 70, 0.5, 0.6, 0.5, 0.15);
                        world.spawnParticle(Particle.DRAGON_BREATH, living.getLocation().add(0, 1, 0), 15, 0.3, 0.4, 0.3, 0.06);
                        world.spawnParticle(Particle.SWEEP_ATTACK, living.getLocation().add(0, 1, 0), 3, 0.3, 0.3, 0.3, 0);

                        // Dragon hit sound
                        world.playSound(living.getLocation(), Sound.ENTITY_ENDER_DRAGON_HURT, 1.0f, 1.5f);
                        world.playSound(living.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1.5f, 0.8f);

                        // Notify victim
                        if (living instanceof Player) {
                            ((Player) living).sendMessage(ChatColor.RED + "⚔ Hit by " + ChatColor.DARK_PURPLE + "Dragon Dash" + ChatColor.RED + " from " + player.getName() + "!");
                        }
                    }
                }

                tick++;
            }
        }.runTaskTimer(plugin, 0L, 1L);

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
        cooldowns.put("soul_devourer", new int[]{30, 85}); // Void Slice, Void Rift
        cooldowns.put("dragonborn_blade", new int[]{30, 120}); // End Sever (30s), Genesis Collapse
        cooldowns.put("forge_pickaxe", new int[]{1, 1}); // Instant toggles
        cooldowns.put("chaos_dice_of_fate", new int[]{1800, 10}); // Roll Dice 30min, Player Scan 10s
        cooldowns.put("rift_key_of_endkeeper", new int[]{86400, 86400}); // 24 hours

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
                        dealAbilityDamage(living, 40.0, player); // ~8 hearts through full Prot 4
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
                            abilityDamageActive.add(player.getUniqueId());
                            target.damage(damage, player);
                            abilityDamageActive.remove(player.getUniqueId());
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

            // Chrono Blade - Time Slow passive (first hit applies slow, 20s cooldown per target)
            String legendaryId = LegendaryItemFactory.getLegendaryId(player.getInventory().getItemInMainHand());
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

            // Voidrender - bonus damage based on soul count (+1 damage per soul)
            if (legendaryId != null && legendaryId.equals(LegendaryType.SOUL_DEVOURER.getId())) {
                int soulCount = LegendaryItemFactory.getSoulCount(player.getInventory().getItemInMainHand());
                if (soulCount > 0 && event.getEntity() instanceof LivingEntity) {
                    LivingEntity target = (LivingEntity) event.getEntity();

                    // Trust check
                    if (target instanceof Player && plugin.getTrustManager().isTrusted(player, (Player) target)) {
                        // Skip trusted players
                    } else {
                        // +1 damage per soul (so +2 HP damage per soul)
                        double bonusDamage = soulCount * 2.0; // 1 heart = 2 HP per soul
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

            if (legendaryId != null && legendaryId.equals(LegendaryType.DRAGONBORN_BLADE.getId())) {
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

        // Check if this trident is the Tempestbreaker Spear
        ItemStack tridentItem = trident.getItemStack();
        String legendaryId = LegendaryItemFactory.getLegendaryId(tridentItem);
        boolean isTempestbreaker = legendaryId != null && legendaryId.equals(LegendaryType.TEMPESTBREAKER_SPEAR.getId());

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
                    dealAbilityDamage(living, 25.0, player); // ~5 hearts through full Prot 4

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
        // Tempestbreaker Spear passive: Lightning strike on hit
        else if (isTempestbreaker && event.getHitEntity() instanceof LivingEntity) {
            LivingEntity target = (LivingEntity) event.getHitEntity();

            // Trust check
            if (target instanceof Player && plugin.getTrustManager().isTrusted(player, (Player) target)) {
                return;
            }

            Location hitLoc = target.getLocation();

            // Strike lightning at the target
            hitLoc.getWorld().strikeLightningEffect(hitLoc);

            // Deal bonus lightning damage (1 heart = 2 damage)
            double currentHealth = target.getHealth();
            target.setHealth(Math.max(0, currentHealth - 2.0));

            // Electric particles
            hitLoc.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, hitLoc.add(0, 1, 0), 50, 0.5, 1, 0.5, 0.2);

            // Notify victim
            if (target instanceof Player) {
                ((Player) target).sendMessage(ChatColor.RED + "⚔ Struck by " + ChatColor.YELLOW + "Tempestbreaker Lightning" + ChatColor.RED + " from " + player.getName() + "!");
            }
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

    /**
     * Check if a player is currently dealing ability damage (not melee)
     */
    public boolean isAbilityDamageActive(UUID playerId) {
        return abilityDamageActive.contains(playerId);
    }

    /**
     * Mark that a player is about to deal ability damage
     */
    public void setAbilityDamageActive(UUID playerId) {
        abilityDamageActive.add(playerId);
    }

    /**
     * Mark that a player is done dealing ability damage
     */
    public void clearAbilityDamageActive(UUID playerId) {
        abilityDamageActive.remove(playerId);
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        // Prevent breaking Prison of the Damned blocks
        if (activePrisonBlocks.contains(event.getBlock().getLocation())) {
            event.setCancelled(true);
            event.getPlayer().sendMessage(ChatColor.DARK_GRAY + "This block is bound by dark magic!");
        }
    }

    // ========== RIFT KEY OF THE ENDKEEPER ==========

    private boolean openEndRift(Player player) {
        // Combat check - can't teleport while in combat
        Long lastCombat = combatTagTime.get(player.getUniqueId());
        if (lastCombat != null && (System.currentTimeMillis() - lastCombat) < COMBAT_TAG_DURATION) {
            long remaining = (COMBAT_TAG_DURATION - (System.currentTimeMillis() - lastCombat)) / 1000;
            player.sendMessage(ChatColor.RED + "Cannot use Rift Key while in combat! Wait " + remaining + "s");
            return false;
        }

        // 4-hour cooldown check using CooldownManager
        int cooldownSeconds = 4 * 60 * 60; // 4 hours in seconds

        if (plugin.getCooldownManager().isOnCooldown(player.getUniqueId(), "rift_key_of_endkeeper", 1)) {
            int remaining = plugin.getCooldownManager().getRemainingCooldown(player.getUniqueId(), "rift_key_of_endkeeper", 1);
            int hours = remaining / 3600;
            int minutes = (remaining % 3600) / 60;
            player.sendMessage(ChatColor.DARK_PURPLE + "The Rift Key is recharging. Time remaining: " +
                    ChatColor.LIGHT_PURPLE + hours + "h " + minutes + "m");
            return false;
        }

        // Prompt player to enter coordinates
        player.sendMessage(ChatColor.DARK_PURPLE + "═══════════════════════════════════");
        player.sendMessage(ChatColor.LIGHT_PURPLE + "✦ " + ChatColor.BOLD + "RIFT KEY OF THE ENDKEEPER" + ChatColor.LIGHT_PURPLE + " ✦");
        player.sendMessage(ChatColor.GRAY + "Type coordinates in chat: " + ChatColor.WHITE + "X Y Z");
        player.sendMessage(ChatColor.GRAY + "Example: " + ChatColor.WHITE + "100 64 -200");
        player.sendMessage(ChatColor.GRAY + "Type " + ChatColor.RED + "cancel" + ChatColor.GRAY + " to abort.");
        player.sendMessage(ChatColor.DARK_PURPLE + "═══════════════════════════════════");

        // Register this player as awaiting coordinate input
        awaitingRiftCoordinates.add(player.getUniqueId());

        // Start a timeout task - cancel after 30 seconds if no input
        new BukkitRunnable() {
            @Override
            public void run() {
                if (awaitingRiftCoordinates.remove(player.getUniqueId())) {
                    player.sendMessage(ChatColor.RED + "Rift Key activation timed out.");
                }
            }
        }.runTaskLater(plugin, 20 * 30); // 30 seconds

        return false; // Don't trigger cooldown yet - wait for coords
    }

    // Set to track players awaiting coordinate input
    private Set<UUID> awaitingRiftCoordinates = new HashSet<>();

    @EventHandler
    public void onPlayerChat(org.bukkit.event.player.AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        if (!awaitingRiftCoordinates.contains(player.getUniqueId())) {
            return;
        }

        String message = event.getMessage().trim();
        event.setCancelled(true);

        // Handle cancellation
        if (message.equalsIgnoreCase("cancel")) {
            awaitingRiftCoordinates.remove(player.getUniqueId());
            player.sendMessage(ChatColor.RED + "Rift Key activation cancelled.");
            return;
        }

        // Parse coordinates
        String[] parts = message.split("\\s+");
        if (parts.length < 3) {
            player.sendMessage(ChatColor.RED + "Invalid format. Use: X Y Z (e.g., 100 64 -200)");
            return;
        }

        try {
            double x = Double.parseDouble(parts[0]);
            double y = Double.parseDouble(parts[1]);
            double z = Double.parseDouble(parts[2]);

            awaitingRiftCoordinates.remove(player.getUniqueId());

            // Execute teleport on main thread
            new BukkitRunnable() {
                @Override
                public void run() {
                    executeRiftTeleport(player, x, y, z);
                }
            }.runTask(plugin);

        } catch (NumberFormatException e) {
            player.sendMessage(ChatColor.RED + "Invalid coordinates. Use numbers only: X Y Z");
        }
    }

    private void executeRiftTeleport(Player player, double x, double y, double z) {
        Location destination = new Location(player.getWorld(), x, y, z);

        // Validate Y coordinate
        if (y < player.getWorld().getMinHeight() || y > player.getWorld().getMaxHeight()) {
            player.sendMessage(ChatColor.RED + "Invalid Y coordinate. Must be between " +
                    player.getWorld().getMinHeight() + " and " + player.getWorld().getMaxHeight());
            return;
        }

        // Create dramatic effect at origin
        Location origin = player.getLocation();
        player.getWorld().spawnParticle(Particle.REVERSE_PORTAL, origin, 100, 0.5, 1, 0.5, 0.1);
        player.getWorld().spawnParticle(Particle.END_ROD, origin, 50, 0.5, 1, 0.5, 0.05);
        player.getWorld().playSound(origin, Sound.ENTITY_ENDERMAN_TELEPORT, 2.0f, 0.5f);
        player.getWorld().playSound(origin, Sound.BLOCK_PORTAL_TRAVEL, 0.5f, 1.5f);

        // Teleport after short delay
        new BukkitRunnable() {
            @Override
            public void run() {
                // Set destination yaw/pitch to player's current
                destination.setYaw(player.getLocation().getYaw());
                destination.setPitch(player.getLocation().getPitch());

                player.teleport(destination);

                // Effects at destination
                player.getWorld().spawnParticle(Particle.REVERSE_PORTAL, destination, 100, 0.5, 1, 0.5, 0.1);
                player.getWorld().spawnParticle(Particle.END_ROD, destination, 50, 0.5, 1, 0.5, 0.05);
                player.getWorld().playSound(destination, Sound.ENTITY_ENDERMAN_TELEPORT, 2.0f, 0.5f);
                player.getWorld().playSound(destination, Sound.BLOCK_BEACON_ACTIVATE, 1.0f, 1.0f);

                player.sendMessage(ChatColor.DARK_PURPLE + "✦ " + ChatColor.LIGHT_PURPLE +
                        "The Rift Key tears through reality, delivering you to " +
                        ChatColor.WHITE + String.format("%.1f, %.1f, %.1f", x, y, z));

                // Set 4-hour cooldown via cooldown manager
                plugin.getCooldownManager().setCooldown(player.getUniqueId(), "rift_key_of_endkeeper", 1, 4 * 60 * 60); // 4 hours
            }
        }.runTaskLater(plugin, 10); // 0.5 second delay
    }

    // ========== CHAOS DICE OF FATE ==========

    private boolean rollChaosDice(Player player) {
        // Create dice rolling animation
        player.sendMessage(ChatColor.LIGHT_PURPLE + "✦ " + ChatColor.BOLD + "ROLLING THE CHAOS DICE..." + ChatColor.LIGHT_PURPLE + " ✦");
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ENDER_DRAGON_FLAP, 1.0f, 1.5f);

        // Visual effect - spinning particles
        new BukkitRunnable() {
            int ticks = 0;

            @Override
            public void run() {
                if (ticks >= 30) { // 1.5 seconds of rolling
                    cancel();
                    // Determine outcome
                    int roll = new Random().nextInt(7) + 1; // 1-7
                    applyChaosDiceEffect(player, roll);
                    return;
                }

                // Spinning particle effect
                double angle = (ticks * 30) * Math.PI / 180;
                double x = Math.cos(angle) * 1.5;
                double z = Math.sin(angle) * 1.5;
                Location particleLoc = player.getLocation().add(x, 1.5, z);

                Particle.DustOptions purple = new Particle.DustOptions(Color.fromRGB(138, 43, 226), 1.5f);
                player.getWorld().spawnParticle(Particle.DUST, particleLoc, 5, 0.1, 0.1, 0.1, 0, purple);

                if (ticks % 5 == 0) {
                    player.getWorld().playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_CHIME, 0.5f, 0.5f + (ticks * 0.05f));
                }

                ticks++;
            }
        }.runTaskTimer(plugin, 0, 1);

        return true;
    }

    private void applyChaosDiceEffect(Player player, int roll) {
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 0.8f);

        switch (roll) {
            case 1: // +5 hearts for 15 minutes
                player.addPotionEffect(new PotionEffect(PotionEffectType.HEALTH_BOOST, 20 * 60 * 15, 2)); // +5 hearts (amplifier 2)
                player.setHealth(Math.min(player.getHealth() + 10, player.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH).getValue() + 10)); // Heal the bonus hearts
                player.sendMessage(ChatColor.RED + "✦ FATE #1: " + ChatColor.WHITE + "Heart Surge! " +
                        ChatColor.GRAY + "+5 hearts for 15 minutes.");
                player.getWorld().spawnParticle(Particle.HEART, player.getLocation().add(0, 2, 0), 30, 0.5, 0.5, 0.5, 0);
                break;

            case 2: // Summon 5 iron golems
                player.sendMessage(ChatColor.GOLD + "✦ FATE #2: " + ChatColor.WHITE + "Guardian Army! " +
                        ChatColor.GRAY + "5 Iron Golems fight for you for 5 minutes.");
                for (int i = 0; i < 5; i++) {
                    double angle = (i * 72) * Math.PI / 180; // Spread around player
                    Location spawnLoc = player.getLocation().add(Math.cos(angle) * 3, 0, Math.sin(angle) * 3);
                    org.bukkit.entity.IronGolem golem = player.getWorld().spawn(spawnLoc, org.bukkit.entity.IronGolem.class);
                    golem.setPlayerCreated(true);
                    golem.setCustomName(ChatColor.GOLD + player.getName() + "'s Guardian");
                    golem.setCustomNameVisible(true);
                    chaosGolemOwners.put(golem.getUniqueId(), player.getUniqueId());

                    // Make golem attack nearest non-owner player
                    Player nearestEnemy = null;
                    double nearestDist = Double.MAX_VALUE;
                    for (Player p : golem.getWorld().getPlayers()) {
                        if (p.equals(player)) continue;
                        if (p.getGameMode() == org.bukkit.GameMode.SPECTATOR) continue;
                        if (p.getGameMode() == org.bukkit.GameMode.CREATIVE) continue;
                        double dist = p.getLocation().distance(golem.getLocation());
                        if (dist < nearestDist) {
                            nearestDist = dist;
                            nearestEnemy = p;
                        }
                    }
                    if (nearestEnemy != null) {
                        golem.setTarget(nearestEnemy);
                    }

                    // Remove golem after 5 minutes
                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            if (golem.isValid() && !golem.isDead()) {
                                golem.getWorld().spawnParticle(Particle.CLOUD, golem.getLocation(), 30, 0.5, 1, 0.5, 0.1);
                                golem.remove();
                                chaosGolemOwners.remove(golem.getUniqueId());
                            }
                        }
                    }.runTaskLater(plugin, 20 * 60 * 5); // 5 minutes
                }
                break;

            case 3: // Speed III + Strength III for 10 minutes
                player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 20 * 60 * 10, 2)); // Speed III
                player.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, 20 * 60 * 10, 2)); // Strength III
                player.sendMessage(ChatColor.LIGHT_PURPLE + "✦ FATE #3: " + ChatColor.WHITE + "Berserker Rage! " +
                        ChatColor.GRAY + "Speed III + Strength III for 10 minutes.");
                player.getWorld().spawnParticle(Particle.FLAME, player.getLocation(), 50, 0.5, 1, 0.5, 0.1);
                player.getWorld().spawnParticle(Particle.CLOUD, player.getLocation(), 30, 0.5, 1, 0.5, 0.05);
                break;

            case 4: // Jumble opponent's hotbar
                Player nearest = getNearestPlayer(player, 50);
                if (nearest != null) {
                    // Shuffle their hotbar
                    org.bukkit.inventory.ItemStack[] hotbar = new org.bukkit.inventory.ItemStack[9];
                    for (int i = 0; i < 9; i++) {
                        hotbar[i] = nearest.getInventory().getItem(i);
                    }
                    java.util.List<org.bukkit.inventory.ItemStack> hotbarList = java.util.Arrays.asList(hotbar);
                    java.util.Collections.shuffle(hotbarList);
                    for (int i = 0; i < 9; i++) {
                        nearest.getInventory().setItem(i, hotbarList.get(i));
                    }
                    nearest.sendMessage(ChatColor.DARK_PURPLE + "Your hotbar has been scrambled by " + player.getName() + "!");
                    player.sendMessage(ChatColor.DARK_PURPLE + "✦ FATE #4: " + ChatColor.WHITE + "Chaos Scramble! " +
                            ChatColor.GRAY + "Jumbled " + nearest.getName() + "'s hotbar!");
                } else {
                    player.sendMessage(ChatColor.DARK_PURPLE + "✦ FATE #4: " + ChatColor.WHITE + "Chaos Scramble! " +
                            ChatColor.GRAY + "No nearby players to scramble.");
                }
                player.getWorld().spawnParticle(Particle.WITCH, player.getLocation(), 40, 0.5, 1, 0.5, 0.1);
                break;

            case 5: // Player tracker - free scans for 20 minutes
                long trackerEndTime = System.currentTimeMillis() + (20 * 60 * 1000);
                chaosDiceFreeScans.put(player.getUniqueId(), trackerEndTime);
                chaosDiceTrackerActive.add(player.getUniqueId()); // Enable ability 2
                player.sendMessage(ChatColor.DARK_AQUA + "✦ FATE #5: " + ChatColor.WHITE + "Hunter's Instinct! " +
                        ChatColor.GRAY + "Free player scans (/ability 2) for 20 minutes.");
                player.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME, player.getLocation().add(0, 1, 0), 40, 0.5, 0.5, 0.5, 0.1);
                // Schedule removal of tracker access after 20 minutes
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        chaosDiceTrackerActive.remove(player.getUniqueId());
                        if (player.isOnline()) {
                            player.sendMessage(ChatColor.GRAY + "Hunter's Instinct has faded.");
                        }
                    }
                }.runTaskLater(plugin, 20 * 60 * 20); // 20 minutes in ticks
                break;

            case 6: // Insta-crit for 15 minutes
                long endTime = System.currentTimeMillis() + (15 * 60 * 1000);
                chaosDiceInstaCritTimed.put(player.getUniqueId(), endTime);
                player.sendMessage(ChatColor.RED + "✦ FATE #6: " + ChatColor.WHITE + "Critical Master! " +
                        ChatColor.GRAY + "All hits are critical for 15 minutes.");
                player.getWorld().spawnParticle(Particle.CRIT, player.getLocation().add(0, 1, 0), 50, 0.5, 0.5, 0.5, 0.3);
                break;

            case 7: // Resistance II for 5 minutes
                player.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, 20 * 60 * 5, 1)); // Resistance II
                player.sendMessage(ChatColor.AQUA + "✦ FATE #7: " + ChatColor.WHITE + "Iron Will! " +
                        ChatColor.GRAY + "Resistance II for 5 minutes.");
                player.getWorld().spawnParticle(Particle.END_ROD, player.getLocation(), 40, 0.5, 1, 0.5, 0.05);
                break;
        }
    }

    // Timed insta-crit tracking (for 15 min duration)
    private Map<UUID, Long> chaosDiceInstaCritTimed = new HashMap<>();

    // Free player scans tracking (for 20 min duration)
    private Map<UUID, Long> chaosDiceFreeScans = new HashMap<>();

    /**
     * Check if player has free scans active (from Chaos Dice roll).
     */
    public boolean hasFreeScan(UUID playerId) {
        Long endTime = chaosDiceFreeScans.get(playerId);
        if (endTime == null) return false;
        if (System.currentTimeMillis() > endTime) {
            chaosDiceFreeScans.remove(playerId);
            return false;
        }
        return true;
    }

    private Player getNearestPlayer(Player player, double maxRange) {
        Player nearest = null;
        double nearestDist = Double.MAX_VALUE;
        for (Player p : player.getWorld().getPlayers()) {
            if (p.equals(player)) continue;
            if (p.getGameMode() == org.bukkit.GameMode.SPECTATOR) continue;
            if (p.getGameMode() == org.bukkit.GameMode.CREATIVE) continue;
            double dist = p.getLocation().distance(player.getLocation());
            if (dist < nearestDist && dist <= maxRange) {
                nearestDist = dist;
                nearest = p;
            }
        }
        return nearest;
    }

    private void activatePlayerTrackerLong(Player player) {
        player.sendMessage(ChatColor.DARK_AQUA + "✦ FATE #5: " + ChatColor.WHITE + "Hunter's Instinct! " +
                ChatColor.GRAY + "Tracking nearest player for 20 minutes.");

        // Particle trail to nearest enemy every 10 seconds (less spam)
        new BukkitRunnable() {
            int ticks = 0;

            @Override
            public void run() {
                if (ticks >= 120 || !player.isOnline()) { // 20 minutes (120 x 10 seconds)
                    if (player.isOnline()) {
                        player.sendMessage(ChatColor.GRAY + "Hunter's Instinct has faded.");
                    }
                    cancel();
                    return;
                }

                // Find nearest player (enemy)
                Player nearest = getNearestPlayer(player, Double.MAX_VALUE);

                if (nearest != null) {
                    double dist = nearest.getLocation().distance(player.getLocation());
                    Vector direction = nearest.getLocation().toVector().subtract(player.getLocation().toVector()).normalize();

                    // Show direction particles
                    for (int i = 1; i <= 5; i++) {
                        Location particleLoc = player.getLocation().add(direction.clone().multiply(i)).add(0, 1, 0);
                        player.spawnParticle(Particle.SOUL_FIRE_FLAME, particleLoc, 3, 0.1, 0.1, 0.1, 0);
                    }
                    player.sendMessage(ChatColor.DARK_AQUA + "Target: " + ChatColor.WHITE + nearest.getName() +
                            ChatColor.GRAY + " (" + String.format("%.0f", dist) + " blocks " + getCardinalDirection(direction) + ")");
                }

                ticks++;
            }
        }.runTaskTimer(plugin, 0, 200); // Every 10 seconds
    }

    // Tracking for insta-crit hits remaining
    private Map<UUID, Integer> instaCritHitsRemaining = new HashMap<>();

    // Tracking for double drops
    private Map<UUID, Long> chaosDoubleDropActive = new HashMap<>();

    @EventHandler
    public void onDamageForInstaCrit(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player)) return;
        Player attacker = (Player) event.getDamager();

        // Check for timed insta-crit (15 min duration)
        Long endTime = chaosDiceInstaCritTimed.get(attacker.getUniqueId());
        if (endTime != null) {
            if (System.currentTimeMillis() > endTime) {
                chaosDiceInstaCritTimed.remove(attacker.getUniqueId());
                attacker.sendMessage(ChatColor.GRAY + "Critical Master has ended.");
            } else {
                // Apply critical hit bonus (50% extra damage)
                event.setDamage(event.getDamage() * 1.5);
                attacker.getWorld().spawnParticle(Particle.CRIT, event.getEntity().getLocation().add(0, 1, 0),
                        15, 0.3, 0.3, 0.3, 0.2);
                attacker.getWorld().playSound(attacker.getLocation(), Sound.ENTITY_PLAYER_ATTACK_CRIT, 1.0f, 1.2f);
            }
        }
    }

    private void activatePlayerTracker(Player player) {
        chaosDiceTrackerActive.add(player.getUniqueId());
        player.sendMessage(ChatColor.DARK_AQUA + "✦ FATE #5: " + ChatColor.WHITE + "Hunter's Instinct! " +
                ChatColor.GRAY + "Tracking nearest player for 1 minute.");

        // Particle trail to nearest enemy every 2 seconds
        new BukkitRunnable() {
            int ticks = 0;

            @Override
            public void run() {
                if (ticks >= 60 || !player.isOnline()) { // 1 minute
                    chaosDiceTrackerActive.remove(player.getUniqueId());
                    if (player.isOnline()) {
                        player.sendMessage(ChatColor.GRAY + "Hunter's Instinct has faded.");
                    }
                    cancel();
                    return;
                }

                // Find nearest player (enemy)
                Player nearest = null;
                double nearestDist = Double.MAX_VALUE;

                for (Player p : player.getWorld().getPlayers()) {
                    if (p.equals(player)) continue;
                    if (p.getGameMode() == org.bukkit.GameMode.SPECTATOR) continue;
                    if (p.getGameMode() == org.bukkit.GameMode.CREATIVE) continue;

                    double dist = p.getLocation().distance(player.getLocation());
                    if (dist < nearestDist) {
                        nearestDist = dist;
                        nearest = p;
                    }
                }

                if (nearest != null && nearestDist <= 100) {
                    // Show direction particles
                    Vector direction = nearest.getLocation().toVector().subtract(player.getLocation().toVector()).normalize();
                    for (int i = 1; i <= 5; i++) {
                        Location particleLoc = player.getLocation().add(direction.clone().multiply(i)).add(0, 1, 0);
                        player.spawnParticle(Particle.SOUL_FIRE_FLAME, particleLoc, 3, 0.1, 0.1, 0.1, 0);
                    }
                    player.sendMessage(ChatColor.DARK_AQUA + "Target: " + ChatColor.WHITE + nearest.getName() +
                            ChatColor.GRAY + " (" + String.format("%.0f", nearestDist) + " blocks " + getCardinalDirection(direction) + ")");
                } else {
                    player.sendMessage(ChatColor.GRAY + "No players within 100 blocks.");
                }

                ticks++;
            }
        }.runTaskTimer(plugin, 0, 40); // Every 2 seconds
    }

    private String getCardinalDirection(Vector dir) {
        double angle = Math.atan2(dir.getX(), dir.getZ()) * 180 / Math.PI;
        if (angle < 0) angle += 360;

        if (angle >= 337.5 || angle < 22.5) return "N";
        if (angle >= 22.5 && angle < 67.5) return "NE";
        if (angle >= 67.5 && angle < 112.5) return "E";
        if (angle >= 112.5 && angle < 157.5) return "SE";
        if (angle >= 157.5 && angle < 202.5) return "S";
        if (angle >= 202.5 && angle < 247.5) return "SW";
        if (angle >= 247.5 && angle < 292.5) return "W";
        if (angle >= 292.5 && angle < 337.5) return "NW";
        return "";
    }

    // Chaos Dice Ability 2 - One-time player scan (10s cooldown)
    private boolean scanForPlayers(Player player) {
        player.getWorld().playSound(player.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, 0.5f, 1.5f);

        // Find all players in the world, sorted by distance
        java.util.List<Player> nearbyPlayers = new java.util.ArrayList<>();
        for (Player p : player.getWorld().getPlayers()) {
            if (p.equals(player)) continue;
            if (p.getGameMode() == org.bukkit.GameMode.SPECTATOR) continue;
            if (p.getGameMode() == org.bukkit.GameMode.CREATIVE) continue;
            nearbyPlayers.add(p);
        }

        if (nearbyPlayers.isEmpty()) {
            player.sendMessage(ChatColor.GRAY + "No players found in this world.");
            return true;
        }

        // Sort by distance
        Location playerLoc = player.getLocation();
        nearbyPlayers.sort((a, b) -> {
            double distA = a.getLocation().distance(playerLoc);
            double distB = b.getLocation().distance(playerLoc);
            return Double.compare(distA, distB);
        });

        player.sendMessage(ChatColor.DARK_AQUA + "═══════ " + ChatColor.WHITE + "Player Scan" + ChatColor.DARK_AQUA + " ═══════");

        for (Player target : nearbyPlayers) {
            Location targetLoc = target.getLocation();
            double distance = targetLoc.distance(playerLoc);
            Vector direction = targetLoc.toVector().subtract(playerLoc.toVector()).normalize();
            String cardinal = getCardinalDirection(direction);

            player.sendMessage(ChatColor.AQUA + target.getName() + ChatColor.GRAY + " - " +
                    ChatColor.WHITE + String.format("%.0f", distance) + " blocks " + cardinal +
                    ChatColor.DARK_GRAY + " (" + (int)targetLoc.getX() + ", " + (int)targetLoc.getY() + ", " + (int)targetLoc.getZ() + ")");
        }

        player.sendMessage(ChatColor.DARK_AQUA + "═══════════════════════════");

        // Particles
        player.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME, player.getLocation().add(0, 1, 0), 30, 0.5, 0.5, 0.5, 0.05);

        return true;
    }

    public boolean hasDoubleDrops(UUID playerId) {
        Long endTime = chaosDoubleDropActive.get(playerId);
        if (endTime == null) return false;
        if (System.currentTimeMillis() > endTime) {
            chaosDoubleDropActive.remove(playerId);
            return false;
        }
        return true;
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

    // ========== COMBAT TRACKING FOR RIFT KEY ==========

    @EventHandler(priority = org.bukkit.event.EventPriority.MONITOR, ignoreCancelled = true)
    public void onCombatDamage(EntityDamageByEntityEvent event) {
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
        long now = System.currentTimeMillis();
        if (attacker != null && victim != null && !attacker.equals(victim)) {
            combatTagTime.put(attacker.getUniqueId(), now);
            combatTagTime.put(victim.getUniqueId(), now);
        }
    }
}
