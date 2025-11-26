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
    private Map<UUID, UUID> soulMarkTargets; // Soul Mark target UUIDs
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

    public AbilityManager(LegendaryWeaponsPlugin plugin) {
        this.plugin = plugin;
        this.fireRebirthActive = new HashMap<>();
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

        plugin.getServer().getPluginManager().registerEvents(this, plugin);
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
                return voidRupture(player);
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

        // Grant 6 absorption hearts (12 HP) if at least one entity was hit
        if (entitiesHit > 0) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.ABSORPTION, 600, 2)); // Level 3 = 6 hearts
        }

        // Enhanced particles
        player.getWorld().spawnParticle(Particle.FLAME, player.getLocation(), 400, 3, 1, 3, 0.1);
        player.getWorld().spawnParticle(Particle.LAVA, player.getLocation(), 60, 3, 1, 3, 0);
        player.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME, player.getLocation(), 100, 3, 1, 3, 0.05);

        player.playSound(player.getLocation(), Sound.ENTITY_BLAZE_SHOOT, 1.0f, 0.8f);
        player.sendMessage(ChatColor.RED + "Flame Harvest! +6 absorption hearts (" + entitiesHit + " enemies hit)");
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
        Vector direction = player.getLocation().getDirection();
        Location start = player.getEyeLocation();

        for (int i = 1; i <= 15; i++) {
            Location point = start.clone().add(direction.clone().multiply(i));

            // Enhanced particles
            player.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, point, 40, 0.5, 0.5, 0.5, 0.1);
            player.getWorld().spawnParticle(Particle.CLOUD, point, 15, 0.5, 0.5, 0.5, 0);

            final int delay = i * 2;
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                point.getWorld().strikeLightningEffect(point);

                for (Entity entity : point.getWorld().getNearbyEntities(point, 2, 2, 2)) {
                    if (entity instanceof LivingEntity && entity != player) {
                        // Trust check
                        if (entity instanceof Player && plugin.getTrustManager().isTrusted(player, (Player) entity)) {
                            continue;
                        }
                        LivingEntity living = (LivingEntity) entity;
                        living.damage(15.0, player); // ~3 hearts through full Prot 4
                        living.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 60, 4));
                        living.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 20, 0));

                        // Notify victim
                        if (living instanceof Player) {
                            ((Player) living).sendMessage(ChatColor.RED + "⚔ Hit by " + ChatColor.YELLOW + "Stormcall" + ChatColor.RED + " from " + player.getName() + "!");
                        }
                    }
                }
            }, delay);
        }

        player.sendMessage(ChatColor.YELLOW + "Stormcall!");
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
        // Mark is activated, next hit will mark the target
        // Actual marking happens in the event handler below

        // Enhanced particles
        player.getWorld().spawnParticle(Particle.SOUL, player.getLocation(), 150, 1, 1, 1, 0.05);
        player.getWorld().spawnParticle(Particle.ENCHANT, player.getLocation(), 100, 1, 1, 1, 0.1);
        player.getWorld().spawnParticle(Particle.WITCH, player.getLocation(), 50, 1, 1, 1, 0);

        player.sendMessage(ChatColor.DARK_PURPLE + "Soul Mark ready! Hit an entity to mark them.");
        return true;
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
        forestShieldActive.put(player.getUniqueId(), System.currentTimeMillis() + 10000);

        // Enhanced particles
        player.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, player.getLocation(), 300, 1, 1, 1, 0.1);
        player.getWorld().spawnParticle(Particle.COMPOSTER, player.getLocation(), 150, 1, 1, 1, 0);
        player.getWorld().spawnParticle(Particle.CHERRY_LEAVES, player.getLocation(), 100, 1, 1, 1, 0.05);

        player.sendMessage(ChatColor.GREEN + "Forest Shield active! Your axe has Breach III for 10 seconds!");
        player.playSound(player.getLocation(), Sound.ITEM_SHIELD_BLOCK, 1.0f, 0.8f);
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

            // Pull toward player
            Vector direction = player.getLocation().toVector().subtract(target.getLocation().toVector()).normalize();
            target.setVelocity(direction.multiply(2));

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

            // Create 3x3 cage with walls, floor, and ceiling using barrier blocks
            for (int x = -1; x <= 1; x++) {
                for (int z = -1; z <= 1; z++) {
                    // Floor (y = -1)
                    Location floorLoc = center.clone().add(x, -1, z);
                    if (floorLoc.getBlock().getType() == Material.AIR || floorLoc.getBlock().isPassable()) {
                        floorLoc.getBlock().setType(Material.BARRIER);
                        cageBlocks.add(floorLoc);
                    }

                    // Ceiling (y = 3)
                    Location ceilingLoc = center.clone().add(x, 3, z);
                    if (ceilingLoc.getBlock().getType() == Material.AIR || ceilingLoc.getBlock().isPassable()) {
                        ceilingLoc.getBlock().setType(Material.BARRIER);
                        cageBlocks.add(ceilingLoc);
                    }

                    // Walls (skip center column)
                    if (x == 0 && z == 0) continue;

                    for (int y = 0; y <= 2; y++) {
                        Location loc = center.clone().add(x, y, z);
                        if (loc.getBlock().getType() == Material.AIR || loc.getBlock().isPassable()) {
                            loc.getBlock().setType(Material.BARRIER);
                            cageBlocks.add(loc);
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

            // Spawn cage particles every tick to make it visible
            new BukkitRunnable() {
                int ticks = 0;

                @Override
                public void run() {
                    if (ticks >= 100) { // 5 seconds
                        // Remove cage
                        for (Location loc : cageBlocks) {
                            if (loc.getBlock().getType() == Material.BARRIER) {
                                loc.getBlock().setType(Material.AIR);
                            }
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

        HeavensWallBarrier barrier = new HeavensWallBarrier(player.getUniqueId(), world, minX, maxX, minZ, maxZ);
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
        if (from.getX() == to.getX() && from.getZ() == to.getZ()) {
            return;
        }

        for (Map.Entry<UUID, HeavensWallBarrier> entry : activeBarriers.entrySet()) {
            HeavensWallBarrier barrier = entry.getValue();
            UUID ownerUUID = entry.getKey();

            // Skip if different world
            if (!barrier.world.equals(player.getWorld())) continue;

            // Owner can always pass
            if (player.getUniqueId().equals(ownerUUID)) continue;

            // Trusted players can pass
            Player owner = Bukkit.getPlayer(ownerUUID);
            if (owner != null && plugin.getTrustManager().isTrusted(owner, player)) {
                continue;
            }

            // Check if player is trying to cross the barrier
            boolean wasInside = barrier.isInside(from.getX(), from.getZ());
            boolean willBeInside = barrier.isInside(to.getX(), to.getZ());

            // If crossing the barrier boundary (either direction)
            if (wasInside != willBeInside) {
                // Block the movement by setting destination to origin
                event.setTo(from);

                // Push player back
                Vector pushBack = from.toVector().subtract(to.toVector()).normalize().multiply(0.3);
                pushBack.setY(0);
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

    // ========== CHRONO BLADE ==========

    private boolean echoStrike(Player player) {
        echoStrikeActive.put(player.getUniqueId(), System.currentTimeMillis() + 6000);
        echoStrikeTargets.put(player.getUniqueId(), new HashSet<>());

        // Enhanced particles
        player.getWorld().spawnParticle(Particle.ENCHANTED_HIT, player.getLocation(), 200, 1, 1, 1, 0.1);
        player.getWorld().spawnParticle(Particle.ENCHANT, player.getLocation(), 150, 1, 1, 1, 0.1);
        player.getWorld().spawnParticle(Particle.REVERSE_PORTAL, player.getLocation(), 100, 1, 1, 1, 0.05);

        player.sendMessage(ChatColor.YELLOW + "Echo Strike active! Hits will repeat after 1 second!");
        player.playSound(player.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, 1.0f, 1.5f);

        // Clear after 6 seconds
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            echoStrikeActive.remove(player.getUniqueId());
            echoStrikeTargets.remove(player.getUniqueId());
        }, 120L);

        return true;
    }

    private boolean timeRewind(Player player) {
        SavedState state = new SavedState(
            player.getLocation().clone(),
            player.getHealth(),
            player.getFoodLevel()
        );

        timeRewindStates.put(player.getUniqueId(), state);

        // Enhanced particles
        player.getWorld().spawnParticle(Particle.REVERSE_PORTAL, player.getLocation(), 300, 1, 1, 1, 1);
        player.getWorld().spawnParticle(Particle.ENCHANT, player.getLocation(), 150, 1, 1, 1, 0.1);
        player.getWorld().spawnParticle(Particle.ENCHANTED_HIT, player.getLocation(), 100, 1, 1, 1, 0.1);

        player.sendMessage(ChatColor.YELLOW + "Time state saved! Rewinding in 5 seconds...");

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            SavedState saved = timeRewindStates.remove(player.getUniqueId());
            if (saved != null) {
                player.teleport(saved.location);
                player.setHealth(saved.health);
                player.setFoodLevel(saved.hunger);

                // Enhanced particles
                player.getWorld().spawnParticle(Particle.REVERSE_PORTAL, player.getLocation(), 400, 1, 1, 1, 1);
                player.getWorld().spawnParticle(Particle.ENCHANT, player.getLocation(), 200, 1, 1, 1, 0.1);
                player.getWorld().spawnParticle(Particle.PORTAL, player.getLocation(), 150, 1, 1, 1, 0.5);

                player.playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 0.5f);
                player.sendMessage(ChatColor.YELLOW + "Time Rewound!");
            }
        }, 100L);

        return true;
    }

    // ========== SOUL DEVOURER ==========

    private boolean voidSlice(Player player) {
        Vector direction = player.getLocation().getDirection();
        Location start = player.getLocation();

        Set<UUID> hitEntities = new HashSet<>();

        for (int i = 1; i <= 6; i++) {
            Location point = start.clone().add(direction.clone().multiply(i));

            // Wide sweep
            for (double offset = -1.5; offset <= 1.5; offset += 0.5) {
                Vector perpendicular = direction.clone().crossProduct(new Vector(0, 1, 0)).normalize().multiply(offset);
                Location sweepPoint = point.clone().add(perpendicular);

                // Enhanced particles with End theme - very visible
                sweepPoint.getWorld().spawnParticle(Particle.LARGE_SMOKE, sweepPoint, 30, 0.2, 0.2, 0.2, 0.02);
                sweepPoint.getWorld().spawnParticle(Particle.SQUID_INK, sweepPoint, 20, 0.2, 0.2, 0.2, 0.08);
                sweepPoint.getWorld().spawnParticle(Particle.REVERSE_PORTAL, sweepPoint, 25, 0.3, 0.3, 0.3, 0.5);
                sweepPoint.getWorld().spawnParticle(Particle.END_ROD, sweepPoint, 15, 0.2, 0.2, 0.2, 0.05);
                sweepPoint.getWorld().spawnParticle(Particle.DRAGON_BREATH, sweepPoint, 20, 0.3, 0.3, 0.3, 0.03);
                sweepPoint.getWorld().spawnParticle(Particle.PORTAL, sweepPoint, 30, 0.3, 0.3, 0.3, 0.8);

                for (Entity entity : sweepPoint.getWorld().getNearbyEntities(sweepPoint, 1, 1, 1)) {
                    if (entity instanceof LivingEntity && entity != player && !hitEntities.contains(entity.getUniqueId())) {
                        // Trust check
                        if (entity instanceof Player && plugin.getTrustManager().isTrusted(player, (Player) entity)) {
                            continue;
                        }
                        LivingEntity living = (LivingEntity) entity;
                        living.damage(20.0, player); // ~4 hearts through full Prot 4
                        hitEntities.add(entity.getUniqueId());

                        // Notify victim
                        if (living instanceof Player) {
                            ((Player) living).sendMessage(ChatColor.RED + "⚔ Hit by " + ChatColor.DARK_PURPLE + "Void Slice" + ChatColor.RED + " from " + player.getName() + "!");
                        }
                    }
                }
            }
        }

        player.playSound(player.getLocation(), Sound.ENTITY_WITHER_SHOOT, 1.0f, 0.6f);
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

        new BukkitRunnable() {
            int ticks = 0;
            Set<UUID> damagedEntities = new HashSet<>();

            @Override
            public void run() {
                if (ticks >= 40) { // 2 seconds
                    cancel();
                    return;
                }

                // Enhanced black hole particles with End theme - very visible
                riftLocation.getWorld().spawnParticle(Particle.LARGE_SMOKE, riftLocation, 200, 1.5, 1.5, 1.5, 0.15);
                riftLocation.getWorld().spawnParticle(Particle.PORTAL, riftLocation, 150, 1.5, 1.5, 1.5, 2);
                riftLocation.getWorld().spawnParticle(Particle.SQUID_INK, riftLocation, 100, 1.0, 1.0, 1.0, 0.15);
                riftLocation.getWorld().spawnParticle(Particle.REVERSE_PORTAL, riftLocation, 80, 1.0, 1.0, 1.0, 1);
                riftLocation.getWorld().spawnParticle(Particle.END_ROD, riftLocation, 80, 1.5, 1.5, 1.5, 0.08);
                riftLocation.getWorld().spawnParticle(Particle.DRAGON_BREATH, riftLocation, 120, 1.2, 1.2, 1.2, 0.05);
                riftLocation.getWorld().spawnParticle(Particle.WITCH, riftLocation, 50, 1.0, 1.0, 1.0, 0.1);

                // Pull entities
                for (Entity entity : riftLocation.getWorld().getNearbyEntities(riftLocation, 8, 8, 8)) {
                    if (entity instanceof LivingEntity) {
                        // Trust check
                        if (entity instanceof Player && plugin.getTrustManager().isTrusted(player, (Player) entity)) {
                            continue;
                        }

                        Vector direction = riftLocation.toVector().subtract(entity.getLocation().toVector()).normalize();
                        entity.setVelocity(direction.multiply(0.5));

                        // Damage if inside rift (increased from 3.0)
                        if (entity.getLocation().distance(riftLocation) < 2) {
                            LivingEntity living = (LivingEntity) entity;
                            living.damage(12.0, player); // ~2.5 hearts through full Prot 4 per tick

                            // Notify victim (once per tick cycle to avoid spam)
                            if (!damagedEntities.contains(entity.getUniqueId())) {
                                damagedEntities.add(entity.getUniqueId());
                                if (living instanceof Player) {
                                    ((Player) living).sendMessage(ChatColor.RED + "⚔ Hit by " + ChatColor.DARK_PURPLE + "Void Rift" + ChatColor.RED + " from " + player.getName() + "!");
                                }
                            }
                        }
                    }
                }

                ticks += 10; // Run every 10 ticks
            }
        }.runTaskTimer(plugin, 0L, 10L);

        player.sendMessage(ChatColor.DARK_PURPLE + "Void Rift!");
        return true;
    }

    // ========== CREATION SPLITTER ==========

    private boolean voidRupture(Player player) {
        Vector direction = player.getLocation().getDirection();
        Location start = player.getEyeLocation();

        Set<UUID> hitEntities = new HashSet<>();

        for (int i = 1; i <= 35; i++) {
            Location point = start.clone().add(direction.clone().multiply(i));

            // Enhanced particles with End theme - very visible beam
            point.getWorld().spawnParticle(Particle.REVERSE_PORTAL, point, 40, 0.4, 0.4, 0.4, 0.8);
            point.getWorld().spawnParticle(Particle.LARGE_SMOKE, point, 35, 0.4, 0.4, 0.4, 0.05);
            point.getWorld().spawnParticle(Particle.SQUID_INK, point, 25, 0.4, 0.4, 0.4, 0.1);
            point.getWorld().spawnParticle(Particle.END_ROD, point, 20, 0.5, 0.5, 0.5, 0.05);
            point.getWorld().spawnParticle(Particle.DRAGON_BREATH, point, 30, 0.4, 0.4, 0.4, 0.03);
            point.getWorld().spawnParticle(Particle.PORTAL, point, 35, 0.5, 0.5, 0.5, 1.0);
            point.getWorld().spawnParticle(Particle.WITCH, point, 15, 0.3, 0.3, 0.3, 0.05);

            for (Entity entity : point.getWorld().getNearbyEntities(point, 1.5, 1.5, 1.5)) {
                if (entity instanceof LivingEntity && entity != player && !hitEntities.contains(entity.getUniqueId())) {
                    // Trust check
                    if (entity instanceof Player && plugin.getTrustManager().isTrusted(player, (Player) entity)) {
                        continue;
                    }
                    LivingEntity living = (LivingEntity) entity;
                    living.damage(30.0, player); // ~6 hearts through full Prot 4
                    living.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 30, 0));
                    hitEntities.add(entity.getUniqueId());

                    // Notify victim
                    if (living instanceof Player) {
                        ((Player) living).sendMessage(ChatColor.RED + "⚔ Hit by " + ChatColor.DARK_PURPLE + "Void Rupture" + ChatColor.RED + " from " + player.getName() + "!");
                    }
                }
            }
        }

        player.playSound(player.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 1.0f, 0.7f);
        player.sendMessage(ChatColor.DARK_PURPLE + "Void Rupture!");
        return true;
    }

    private boolean cataclysmPulse(Player player) {
        Location center = player.getLocation();

        // Initial pull
        for (Entity entity : center.getWorld().getNearbyEntities(center, 7, 7, 7)) {
            if (entity instanceof LivingEntity && entity != player) {
                // Trust check
                if (entity instanceof Player && plugin.getTrustManager().isTrusted(player, (Player) entity)) {
                    continue;
                }

                LivingEntity living = (LivingEntity) entity;

                Vector direction = center.toVector().subtract(entity.getLocation().toVector()).normalize();
                entity.setVelocity(direction.multiply(1.5));

                living.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 80, 3));
                living.addPotionEffect(new PotionEffect(PotionEffectType.DARKNESS, 60, 0));

                // Notify victim about pull
                if (living instanceof Player) {
                    ((Player) living).sendMessage(ChatColor.RED + "⚔ Hit by " + ChatColor.DARK_PURPLE + "Cataclysm Pulse" + ChatColor.RED + " from " + player.getName() + "!");
                }
            }
        }

        // Enhanced particles with End theme - massive pull effect
        center.getWorld().spawnParticle(Particle.LARGE_SMOKE, center, 1200, 7, 3, 7, 0.15);
        center.getWorld().spawnParticle(Particle.SQUID_INK, center, 600, 7, 3, 7, 0.15);
        center.getWorld().spawnParticle(Particle.REVERSE_PORTAL, center, 500, 7, 3, 7, 1.0);
        center.getWorld().spawnParticle(Particle.END_ROD, center, 400, 7, 3, 7, 0.1);
        center.getWorld().spawnParticle(Particle.DRAGON_BREATH, center, 500, 7, 3, 7, 0.05);
        center.getWorld().spawnParticle(Particle.PORTAL, center, 600, 7, 3, 7, 2.0);
        center.getWorld().spawnParticle(Particle.WITCH, center, 200, 7, 3, 7, 0.1);
        center.getWorld().playSound(center, Sound.ENTITY_WITHER_SPAWN, 1.0f, 0.5f);

        // Final explosion after 2 seconds
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            // Enhanced explosion particles with End theme - massive explosion
            center.getWorld().spawnParticle(Particle.EXPLOSION_EMITTER, center, 30, 2, 2, 2, 0);
            center.getWorld().spawnParticle(Particle.LARGE_SMOKE, center, 800, 5, 5, 5, 0.3);
            center.getWorld().spawnParticle(Particle.SQUID_INK, center, 500, 5, 5, 5, 0.2);
            center.getWorld().spawnParticle(Particle.END_ROD, center, 300, 5, 5, 5, 0.2);
            center.getWorld().spawnParticle(Particle.DRAGON_BREATH, center, 400, 5, 5, 5, 0.1);
            center.getWorld().spawnParticle(Particle.PORTAL, center, 500, 5, 5, 5, 2.5);
            center.getWorld().spawnParticle(Particle.REVERSE_PORTAL, center, 400, 5, 5, 5, 1.5);
            center.getWorld().spawnParticle(Particle.WITCH, center, 150, 4, 4, 4, 0.15);
            center.getWorld().playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 2.0f, 0.5f);
            center.getWorld().playSound(center, Sound.ENTITY_ENDER_DRAGON_GROWL, 1.5f, 0.6f);

            for (Entity entity : center.getWorld().getNearbyEntities(center, 7, 7, 7)) {
                if (entity instanceof LivingEntity && entity != player) {
                    // Trust check
                    if (entity instanceof Player && plugin.getTrustManager().isTrusted(player, (Player) entity)) {
                        continue;
                    }
                    LivingEntity living = (LivingEntity) entity;
                    living.damage(35.0, player); // ~7 hearts through full Prot 4

                    // Notify victim about explosion damage
                    if (living instanceof Player) {
                        ((Player) living).sendMessage(ChatColor.RED + "⚔ Hit by " + ChatColor.DARK_PURPLE + "Cataclysm Pulse Explosion" + ChatColor.RED + " from " + player.getName() + "!");
                    }
                }
            }
        }, 40L);

        player.sendMessage(ChatColor.DARK_PURPLE + "" + ChatColor.BOLD + "CATACLYSM PULSE!");
        return true;
    }

    // ========== HELPER METHODS ==========

    private int getCooldownForAbility(LegendaryType type, int abilityNum) {
        Map<String, int[]> cooldowns = new HashMap<>();
        cooldowns.put("holy_moonlight_sword", new int[]{25, 45});
        cooldowns.put("pheonix_grace", new int[]{30, 180});
        cooldowns.put("tempestbreaker_spear", new int[]{25, 50});
        cooldowns.put("thousand_demon_daggers", new int[]{20, 60});
        cooldowns.put("divine_axe_rhitta", new int[]{35, 70});
        cooldowns.put("chains_of_eternity", new int[]{35, 65});
        cooldowns.put("celestial_aegis_shield", new int[]{40, 90});
        cooldowns.put("chrono_blade", new int[]{40, 120});
        cooldowns.put("soul_devourer", new int[]{30, 85});
        cooldowns.put("creation_splitter", new int[]{35, 95});
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

                    // Schedule echo hit
                    Bukkit.getScheduler().runTaskLater(plugin, () -> {
                        if (target.isValid() && !target.isDead()) {
                            target.damage(damage, player);
                            target.getWorld().spawnParticle(Particle.CRIT, target.getLocation(), 20, 0.5, 0.5, 0.5, 0);
                            player.sendMessage(ChatColor.YELLOW + "Echo!");
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

    // Helper class for Time Rewind
    private static class SavedState {
        Location location;
        double health;
        int hunger;

        SavedState(Location location, double health, int hunger) {
            this.location = location;
            this.health = health;
            this.hunger = hunger;
        }
    }

    // Helper class for Heaven's Wall barrier
    private static class HeavensWallBarrier {
        UUID ownerUUID;
        World world;
        double minX, maxX, minZ, maxZ;

        HeavensWallBarrier(UUID ownerUUID, World world, double minX, double maxX, double minZ, double maxZ) {
            this.ownerUUID = ownerUUID;
            this.world = world;
            this.minX = minX;
            this.maxX = maxX;
            this.minZ = minZ;
            this.maxZ = maxZ;
        }

        boolean isInside(double x, double z) {
            return x >= minX && x <= maxX && z >= minZ && z <= maxZ;
        }
    }
}
