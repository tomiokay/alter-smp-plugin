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
import org.bukkit.event.player.PlayerItemConsumeEvent;
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
    private Map<UUID, Long> heavensWallActive; // Heaven's Wall mode
    private Map<UUID, Long> echoStrikeActive; // Echo Strike mode
    private Map<UUID, SavedState> timeRewindStates; // Saved states for Time Rewind
    private Set<UUID> nextGaleThrow; // Next trident throw is Gale
    private Map<UUID, Set<UUID>> echoStrikeTargets; // Targets hit during Echo Strike
    private Map<UUID, Set<Location>> voidRiftBlocks; // Void Rift black hole blocks

    public AbilityManager(LegendaryWeaponsPlugin plugin) {
        this.plugin = plugin;
        this.fireRebirthActive = new HashMap<>();
        this.soulMarkTargets = new HashMap<>();
        this.forestShieldActive = new HashMap<>();
        this.radiantBlockActive = new HashMap<>();
        this.heavensWallActive = new HashMap<>();
        this.echoStrikeActive = new HashMap<>();
        this.timeRewindStates = new HashMap<>();
        this.nextGaleThrow = new HashSet<>();
        this.echoStrikeTargets = new HashMap<>();
        this.voidRiftBlocks = new HashMap<>();

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
            case BLADE_OF_FRACTURED_STARS:
                return starRiftSlash(player);
            case EMBERHEART_SCYTHE:
                return flameHarvest(player);
            case TEMPESTBREAKER_SPEAR:
                return galeThrow(player);
            case UMBRA_VEIL_DAGGER:
                return shadowstep(player);
            case HEARTROOT_GUARDIAN_AXE:
                return natureGrasp(player);
            case CHAINS_OF_ETERNITY:
                return soulBind(player);
            case GLACIERBOUND_HALBERD:
                return frostbiteSweep(player);
            case CELESTIAL_AEGIS_SHIELD:
                return radiantBlock(player);
            case CHRONO_EDGE:
                return echoStrike(player);
            case OBLIVION_HARVESTER:
                return voidSlice(player);
            case ECLIPSE_DEVOURER:
                return voidRupture(player);
        }
        return false;
    }

    private boolean executeAbility2(Player player, LegendaryType type) {
        switch (type) {
            case BLADE_OF_FRACTURED_STARS:
                return stargateBlink(player);
            case EMBERHEART_SCYTHE:
                return fireRebirth(player);
            case TEMPESTBREAKER_SPEAR:
                return stormcall(player);
            case UMBRA_VEIL_DAGGER:
                return soulMark(player);
            case HEARTROOT_GUARDIAN_AXE:
                return forestShield(player);
            case CHAINS_OF_ETERNITY:
                return prisonOfDamned(player);
            case GLACIERBOUND_HALBERD:
                return wintersEmbrace(player);
            case CELESTIAL_AEGIS_SHIELD:
                return heavensWall(player);
            case CHRONO_EDGE:
                return timeRewind(player);
            case OBLIVION_HARVESTER:
                return voidRift(player);
            case ECLIPSE_DEVOURER:
                return cataclysmPulse(player);
        }
        return false;
    }

    // ========== BLADE OF FRACTURED STARS ==========

    private boolean starRiftSlash(Player player) {
        Vector direction = player.getLocation().getDirection();
        Location start = player.getEyeLocation();

        for (int i = 1; i <= 30; i++) {
            Location point = start.clone().add(direction.clone().multiply(i));

            // Particles
            player.getWorld().spawnParticle(Particle.END_ROD, point, 3, 0.1, 0.1, 0.1, 0);

            // Damage entities
            for (Entity entity : point.getWorld().getNearbyEntities(point, 1, 1, 1)) {
                if (entity instanceof LivingEntity && entity != player) {
                    ((LivingEntity) entity).damage(12.0, player);
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

        // Particles at origin and destination
        player.getWorld().spawnParticle(Particle.PORTAL, player.getLocation(), 100, 0.5, 1, 0.5, 1);
        player.teleport(destination);
        player.getWorld().spawnParticle(Particle.PORTAL, destination, 100, 0.5, 1, 0.5, 1);

        player.playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.0f);
        player.sendMessage(ChatColor.AQUA + "Stargate Blink!");
        return true;
    }

    // ========== EMBERHEART SCYTHE ==========

    private boolean flameHarvest(Player player) {
        double totalAbsorption = 0;

        for (Entity entity : player.getNearbyEntities(6, 6, 6)) {
            if (entity instanceof LivingEntity && !(entity instanceof Player)) {
                LivingEntity living = (LivingEntity) entity;

                // Calculate 20% of current HP
                double damage = living.getHealth() * 0.2;
                totalAbsorption += damage;

                living.damage(damage, player);
                living.setFireTicks(60);

                living.getWorld().spawnParticle(Particle.FLAME, living.getLocation(), 20, 0.5, 0.5, 0.5, 0.1);
            }
        }

        // Grant absorption
        player.addPotionEffect(new PotionEffect(PotionEffectType.ABSORPTION, 600, (int) (totalAbsorption / 4)));

        player.getWorld().spawnParticle(Particle.FLAME, player.getLocation(), 100, 3, 1, 3, 0.1);
        player.playSound(player.getLocation(), Sound.ENTITY_BLAZE_SHOOT, 1.0f, 0.8f);
        player.sendMessage(ChatColor.RED + "Flame Harvest! +" + String.format("%.1f", totalAbsorption) + " absorption");
        return true;
    }

    private boolean fireRebirth(Player player) {
        fireRebirthActive.put(player.getUniqueId(), System.currentTimeMillis() + 10000);
        player.sendMessage(ChatColor.GOLD + "Fire Rebirth activated for 10 seconds!");
        player.playSound(player.getLocation(), Sound.ITEM_TOTEM_USE, 1.0f, 0.8f);
        return true;
    }

    // ========== TEMPESTBREAKER SPEAR ==========

    private boolean galeThrow(Player player) {
        nextGaleThrow.add(player.getUniqueId());
        player.sendMessage(ChatColor.AQUA + "Next trident throw will create a wind vortex!");
        return true;
    }

    private boolean stormcall(Player player) {
        Vector direction = player.getLocation().getDirection();
        Location start = player.getEyeLocation();

        for (int i = 1; i <= 15; i++) {
            Location point = start.clone().add(direction.clone().multiply(i));

            // Spawn lightning effect (weak lightning)
            player.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, point, 10, 0.5, 0.5, 0.5, 0.1);

            final int delay = i * 2;
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                point.getWorld().strikeLightningEffect(point);

                for (Entity entity : point.getWorld().getNearbyEntities(point, 2, 2, 2)) {
                    if (entity instanceof LivingEntity && entity != player) {
                        LivingEntity living = (LivingEntity) entity;
                        living.damage(4.0, player);
                        living.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 60, 4));
                        living.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 20, 0));
                    }
                }
            }, delay);
        }

        player.sendMessage(ChatColor.YELLOW + "Stormcall!");
        return true;
    }

    // ========== UMBRA VEIL DAGGER ==========

    private boolean shadowstep(Player player) {
        Vector direction = player.getLocation().getDirection().multiply(8);
        Location destination = player.getLocation().add(direction);

        // Check for walls
        RayTraceResult result = player.getWorld().rayTraceBlocks(
            player.getLocation(),
            direction.normalize(),
            8
        );

        if (result != null && result.getHitBlock() != null) {
            destination = result.getHitBlock().getLocation();
        }

        player.getWorld().spawnParticle(Particle.LARGE_SMOKE, player.getLocation(), 30, 0.5, 1, 0.5, 0);
        player.teleport(destination);
        player.getWorld().spawnParticle(Particle.LARGE_SMOKE, destination, 30, 0.5, 1, 0.5, 0);

        player.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, 80, 0));
        player.playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 0.6f);
        player.sendMessage(ChatColor.DARK_GRAY + "Shadowstep!");

        return true;
    }

    private boolean soulMark(Player player) {
        player.sendMessage(ChatColor.DARK_PURPLE + "Soul Mark ready! Hit an entity to mark them.");

        // Next hit will mark the target
        new BukkitRunnable() {
            @Override
            public void run() {
                // Wait for hit event via listener
            }
        }.runTaskLater(plugin, 200L); // 10 second window

        return true;
    }

    // ========== HEARTROOT GUARDIAN AXE ==========

    private boolean natureGrasp(Player player) {
        for (Entity entity : player.getNearbyEntities(6, 6, 6)) {
            if (entity instanceof LivingEntity && !(entity instanceof Player)) {
                LivingEntity living = (LivingEntity) entity;

                living.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 40, 255));
                living.addPotionEffect(new PotionEffect(PotionEffectType.JUMP_BOOST, 40, 128));

                living.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, living.getLocation(), 30, 0.5, 1, 0.5, 0);
            }
        }

        player.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, player.getLocation(), 100, 6, 1, 6, 0);
        player.playSound(player.getLocation(), Sound.BLOCK_GRASS_BREAK, 1.0f, 0.5f);
        player.sendMessage(ChatColor.GREEN + "Nature Grasp!");
        return true;
    }

    private boolean forestShield(Player player) {
        forestShieldActive.put(player.getUniqueId(), System.currentTimeMillis() + 10000);
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

            // Pull toward player
            Vector direction = player.getLocation().toVector().subtract(target.getLocation().toVector()).normalize();
            target.setVelocity(direction.multiply(2));

            target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 60, 4));

            target.getWorld().spawnParticle(Particle.SOUL, target.getLocation(), 30, 0.5, 1, 0.5, 0);
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
            Location center = target.getLocation();

            Set<Location> bars = new HashSet<>();

            // Create 3x3 cage
            for (int x = -1; x <= 1; x++) {
                for (int z = -1; z <= 1; z++) {
                    if (x == 0 && z == 0) continue; // Skip center

                    for (int y = 0; y <= 3; y++) {
                        Location loc = center.clone().add(x, y, z);
                        if (loc.getBlock().getType() == Material.AIR) {
                            loc.getBlock().setType(Material.IRON_BARS);
                            bars.add(loc);
                        }
                    }
                }
            }

            // Remove bars after 5 seconds
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                for (Location loc : bars) {
                    loc.getBlock().setType(Material.AIR);
                }
            }, 100L);

            player.sendMessage(ChatColor.DARK_GRAY + "Prison of the Damned!");
            return true;
        }

        player.sendMessage(ChatColor.RED + "No target found!");
        return false;
    }

    // ========== GLACIERBOUND HALBERD ==========

    private boolean frostbiteSweep(Player player) {
        Vector direction = player.getLocation().getDirection();
        Location start = player.getLocation();

        for (int i = 1; i <= 8; i++) {
            Location point = start.clone().add(direction.clone().multiply(i));

            // Cone effect
            for (double angle = -30; angle <= 30; angle += 10) {
                Vector rotated = rotateVector(direction.clone(), angle);
                Location conePoint = start.clone().add(rotated.multiply(i));

                conePoint.getWorld().spawnParticle(Particle.SNOWFLAKE, conePoint, 5, 0.2, 0.2, 0.2, 0);

                for (Entity entity : conePoint.getWorld().getNearbyEntities(conePoint, 1.5, 1.5, 1.5)) {
                    if (entity instanceof LivingEntity && entity != player) {
                        LivingEntity living = (LivingEntity) entity;
                        living.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 60, 4));
                        living.setFreezeTicks(140);
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

                // Spawn dome particles
                for (double theta = 0; theta < 2 * Math.PI; theta += Math.PI / 16) {
                    for (double phi = 0; phi < Math.PI; phi += Math.PI / 16) {
                        double x = 7 * Math.sin(phi) * Math.cos(theta);
                        double y = 7 * Math.sin(phi) * Math.sin(theta);
                        double z = 7 * Math.cos(phi);

                        Location particleLoc = center.clone().add(x, z, y);
                        center.getWorld().spawnParticle(Particle.SNOWFLAKE, particleLoc, 1, 0, 0, 0, 0);
                    }
                }

                // Apply effects
                for (Entity entity : center.getWorld().getNearbyEntities(center, 7, 7, 7)) {
                    if (entity instanceof LivingEntity) {
                        if (entity == player) {
                            player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 20, 1));
                        } else {
                            ((LivingEntity) entity).addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 20, 2));
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
        player.sendMessage(ChatColor.GOLD + "Radiant Block active! Reflect 75% damage for 5 seconds!");
        player.playSound(player.getLocation(), Sound.BLOCK_ENCHANTMENT_TABLE_USE, 1.0f, 1.2f);
        return true;
    }

    private boolean heavensWall(Player player) {
        heavensWallActive.put(player.getUniqueId(), System.currentTimeMillis() + 6000);

        Location center = player.getLocation();

        new BukkitRunnable() {
            int ticks = 0;

            @Override
            public void run() {
                if (ticks >= 120) { // 6 seconds
                    cancel();
                    heavensWallActive.remove(player.getUniqueId());
                    return;
                }

                // Spawn barrier particles
                for (double theta = 0; theta < 2 * Math.PI; theta += Math.PI / 16) {
                    for (double y = 0; y < 4; y += 0.5) {
                        double x = 5 * Math.cos(theta);
                        double z = 5 * Math.sin(theta);

                        Location particleLoc = center.clone().add(x, y, z);
                        center.getWorld().spawnParticle(Particle.END_ROD, particleLoc, 1, 0, 0, 0, 0);
                    }
                }

                ticks++;
            }
        }.runTaskTimer(plugin, 0L, 1L);

        player.sendMessage(ChatColor.GOLD + "Heaven's Wall!");
        return true;
    }

    // ========== CHRONO EDGE ==========

    private boolean echoStrike(Player player) {
        echoStrikeActive.put(player.getUniqueId(), System.currentTimeMillis() + 6000);
        echoStrikeTargets.put(player.getUniqueId(), new HashSet<>());

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
        player.sendMessage(ChatColor.YELLOW + "Time state saved! Rewinding in 5 seconds...");

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            SavedState saved = timeRewindStates.remove(player.getUniqueId());
            if (saved != null) {
                player.teleport(saved.location);
                player.setHealth(saved.health);
                player.setFoodLevel(saved.hunger);

                player.getWorld().spawnParticle(Particle.REVERSE_PORTAL, player.getLocation(), 100, 1, 1, 1, 1);
                player.playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 0.5f);
                player.sendMessage(ChatColor.YELLOW + "Time Rewound!");
            }
        }, 100L);

        return true;
    }

    // ========== OBLIVION HARVESTER ==========

    private boolean voidSlice(Player player) {
        Vector direction = player.getLocation().getDirection();
        Location start = player.getLocation();

        for (int i = 1; i <= 6; i++) {
            Location point = start.clone().add(direction.clone().multiply(i));

            // Wide sweep
            for (double offset = -1.5; offset <= 1.5; offset += 0.5) {
                Vector perpendicular = direction.clone().crossProduct(new Vector(0, 1, 0)).normalize().multiply(offset);
                Location sweepPoint = point.clone().add(perpendicular);

                sweepPoint.getWorld().spawnParticle(Particle.LARGE_SMOKE, sweepPoint, 3, 0.1, 0.1, 0.1, 0);

                for (Entity entity : sweepPoint.getWorld().getNearbyEntities(sweepPoint, 1, 1, 1)) {
                    if (entity instanceof LivingEntity && entity != player) {
                        ((LivingEntity) entity).damage(5.0, player);
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

            @Override
            public void run() {
                if (ticks >= 40) { // 2 seconds
                    cancel();
                    return;
                }

                // Black hole particles
                riftLocation.getWorld().spawnParticle(Particle.LARGE_SMOKE, riftLocation, 30, 0.5, 0.5, 0.5, 0.1);
                riftLocation.getWorld().spawnParticle(Particle.PORTAL, riftLocation, 20, 0.5, 0.5, 0.5, 1);

                // Pull entities
                for (Entity entity : riftLocation.getWorld().getNearbyEntities(riftLocation, 8, 8, 8)) {
                    if (entity instanceof LivingEntity) {
                        Vector direction = riftLocation.toVector().subtract(entity.getLocation().toVector()).normalize();
                        entity.setVelocity(direction.multiply(0.5));

                        // Damage if inside rift
                        if (entity.getLocation().distance(riftLocation) < 2) {
                            ((LivingEntity) entity).damage(3.0, player);
                        }
                    }
                }

                ticks += 10; // Run every 10 ticks
            }
        }.runTaskTimer(plugin, 0L, 10L);

        player.sendMessage(ChatColor.DARK_PURPLE + "Void Rift!");
        return true;
    }

    // ========== ECLIPSE DEVOURER ==========

    private boolean voidRupture(Player player) {
        Vector direction = player.getLocation().getDirection();
        Location start = player.getEyeLocation();

        for (int i = 1; i <= 35; i++) {
            Location point = start.clone().add(direction.clone().multiply(i));

            point.getWorld().spawnParticle(Particle.REVERSE_PORTAL, point, 5, 0.2, 0.2, 0.2, 0);

            for (Entity entity : point.getWorld().getNearbyEntities(point, 1.5, 1.5, 1.5)) {
                if (entity instanceof LivingEntity && entity != player) {
                    LivingEntity living = (LivingEntity) entity;
                    living.damage(7.0, player);
                    living.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 30, 0));
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
                LivingEntity living = (LivingEntity) entity;

                Vector direction = center.toVector().subtract(entity.getLocation().toVector()).normalize();
                entity.setVelocity(direction.multiply(1.5));

                living.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 80, 3));
                living.addPotionEffect(new PotionEffect(PotionEffectType.DARKNESS, 60, 0));
            }
        }

        center.getWorld().spawnParticle(Particle.LARGE_SMOKE, center, 200, 7, 3, 7, 0.1);

        // Final explosion after 2 seconds
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            center.getWorld().spawnParticle(Particle.EXPLOSION_EMITTER, center, 5, 1, 1, 1, 0);
            center.getWorld().playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 2.0f, 0.5f);

            for (Entity entity : center.getWorld().getNearbyEntities(center, 7, 7, 7)) {
                if (entity instanceof LivingEntity && entity != player) {
                    ((LivingEntity) entity).damage(8.0, player);
                }
            }
        }, 40L);

        player.sendMessage(ChatColor.DARK_PURPLE + "" + ChatColor.BOLD + "CATACLYSM PULSE!");
        return true;
    }

    // ========== HELPER METHODS ==========

    private int getCooldownForAbility(LegendaryType type, int abilityNum) {
        Map<String, int[]> cooldowns = new HashMap<>();
        cooldowns.put("blade_of_the_fractured_stars", new int[]{25, 45});
        cooldowns.put("emberheart_scythe", new int[]{30, 180});
        cooldowns.put("tempestbreaker_spear", new int[]{25, 50});
        cooldowns.put("umbra_veil_dagger", new int[]{20, 60});
        cooldowns.put("heartroot_guardian_axe", new int[]{35, 70});
        cooldowns.put("chains_of_eternity", new int[]{35, 65});
        cooldowns.put("glacierbound_halberd", new int[]{28, 75});
        cooldowns.put("celestial_aegis_shield", new int[]{40, 90});
        cooldowns.put("chrono_edge", new int[]{40, 120});
        cooldowns.put("oblivion_harvester", new int[]{30, 85});
        cooldowns.put("eclipse_devourer", new int[]{35, 95});

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

                // Fiery explosion
                player.getWorld().spawnParticle(Particle.FLAME, player.getLocation(), 200, 3, 1, 3, 0.2);
                player.getWorld().playSound(player.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 0.8f);

                // Damage nearby enemies
                for (Entity entity : player.getNearbyEntities(5, 5, 5)) {
                    if (entity instanceof LivingEntity && !(entity instanceof Player)) {
                        LivingEntity living = (LivingEntity) entity;
                        living.damage(10.0);
                        Vector knockback = entity.getLocation().toVector().subtract(player.getLocation().toVector()).normalize().multiply(2);
                        entity.setVelocity(knockback);
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

            // Soul Mark
            String legendaryId = LegendaryItemFactory.getLegendaryId(player.getInventory().getItemInMainHand());
            if (legendaryId != null && legendaryId.equals(LegendaryType.UMBRA_VEIL_DAGGER.getId())) {
                // Check if Soul Mark was just activated
                if (event.getEntity() instanceof LivingEntity) {
                    LivingEntity target = (LivingEntity) event.getEntity();

                    if (soulMarkTargets.containsKey(player.getUniqueId())) {
                        // Already marked someone else
                    } else {
                        // Mark this target
                        soulMarkTargets.put(player.getUniqueId(), target.getUniqueId());

                        player.sendMessage(ChatColor.DARK_PURPLE + "Target marked! They will take true damage in 15 seconds.");

                        Bukkit.getScheduler().runTaskLater(plugin, () -> {
                            UUID markedUUID = soulMarkTargets.remove(player.getUniqueId());
                            if (markedUUID != null) {
                                Entity markedEntity = Bukkit.getEntity(markedUUID);
                                if (markedEntity instanceof LivingEntity && markedEntity.isValid()) {
                                    LivingEntity markedLiving = (LivingEntity) markedEntity;

                                    // True damage (ignore armor)
                                    double currentHealth = markedLiving.getHealth();
                                    markedLiving.setHealth(Math.max(0, currentHealth - 6.0));

                                    markedLiving.getWorld().spawnParticle(Particle.SOUL, markedLiving.getLocation(), 50, 0.5, 1, 0.5, 0.1);
                                    markedLiving.getWorld().playSound(markedLiving.getLocation(), Sound.ENTITY_WITHER_HURT, 1.0f, 0.7f);

                                    player.sendMessage(ChatColor.DARK_PURPLE + "Soul Mark triggered! 3 hearts of true damage dealt!");
                                }
                            }
                        }, 300L); // 15 seconds
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

            if (legendaryId != null && legendaryId.equals(LegendaryType.ECLIPSE_DEVOURER.getId())) {
                player.addPotionEffect(new PotionEffect(PotionEffectType.ABSORPTION, 200, 1));
                player.sendMessage(ChatColor.DARK_PURPLE + "Dragon's power absorbed! +2 Absorption hearts");
            }
        }
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
}
