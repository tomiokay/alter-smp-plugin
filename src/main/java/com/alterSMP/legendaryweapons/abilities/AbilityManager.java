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
    private Set<UUID> mining3x3Active; // Players with 3x3 mining enabled
    private Set<UUID> fortuneModeActive; // Players with Fortune mode (vs Silk Touch)

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
        this.mining3x3Active = new HashSet<>();
        this.fortuneModeActive = new HashSet<>();

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
            case CELESTIAL_AEGIS_SHIELD:
                return radiantBlock(player);
            case CHRONO_EDGE:
                return echoStrike(player);
            case OBLIVION_HARVESTER:
                return voidSlice(player);
            case ECLIPSE_DEVOURER:
                return voidRupture(player);
            case COPPER_PICKAXE:
                return toggle3x3Mining(player);
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
            case CELESTIAL_AEGIS_SHIELD:
                return heavensWall(player);
            case CHRONO_EDGE:
                return timeRewind(player);
            case OBLIVION_HARVESTER:
                return voidRift(player);
            case ECLIPSE_DEVOURER:
                return cataclysmPulse(player);
            case COPPER_PICKAXE:
                return toggleEnchantMode(player);
        }
        return false;
    }

    // ========== BLADE OF FRACTURED STARS ==========

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
                    victim.damage(18.0, player);

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

    // ========== EMBERHEART SCYTHE ==========

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

                // Calculate 30% of current HP (increased from 20%)
                double damage = living.getHealth() * 0.3;

                living.damage(damage, player);
                living.setFireTicks(60);
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
                        living.damage(6.0, player); // Increased from 4.0 (50% increase)
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

        // Enhanced particles
        player.getWorld().spawnParticle(Particle.LARGE_SMOKE, player.getLocation(), 120, 0.5, 1, 0.5, 0);
        player.getWorld().spawnParticle(Particle.SQUID_INK, player.getLocation(), 60, 0.5, 1, 0.5, 0.1);
        player.getWorld().spawnParticle(Particle.SMOKE, player.getLocation(), 80, 0.5, 1, 0.5, 0.05);

        player.teleport(destination);

        player.getWorld().spawnParticle(Particle.LARGE_SMOKE, destination, 120, 0.5, 1, 0.5, 0);
        player.getWorld().spawnParticle(Particle.SQUID_INK, destination, 60, 0.5, 1, 0.5, 0.1);
        player.getWorld().spawnParticle(Particle.SMOKE, destination, 80, 0.5, 1, 0.5, 0.05);

        player.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, 80, 0));
        player.playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 0.6f);
        player.sendMessage(ChatColor.DARK_GRAY + "Shadowstep!");

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

    // ========== HEARTROOT GUARDIAN AXE ==========

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

            // Enhanced particles
            center.getWorld().spawnParticle(Particle.SOUL, center, 200, 1.5, 2, 1.5, 0.05);
            center.getWorld().spawnParticle(Particle.SMOKE, center, 150, 1.5, 2, 1.5, 0.05);
            center.getWorld().spawnParticle(Particle.ENCHANT, center, 100, 1.5, 2, 1.5, 0.1);

            // Notify victim
            if (target instanceof Player) {
                ((Player) target).sendMessage(ChatColor.RED + "⚔ Hit by " + ChatColor.DARK_GRAY + "Prison of the Damned" + ChatColor.RED + " from " + player.getName() + "!");
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

                // Spawn barrier particles with enhanced variety
                for (double theta = 0; theta < 2 * Math.PI; theta += Math.PI / 16) {
                    for (double y = 0; y < 4; y += 0.5) {
                        double x = 5 * Math.cos(theta);
                        double z = 5 * Math.sin(theta);

                        Location particleLoc = center.clone().add(x, y, z);
                        center.getWorld().spawnParticle(Particle.END_ROD, particleLoc, 3, 0, 0, 0, 0);
                        center.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, particleLoc, 1, 0, 0, 0, 0);
                        center.getWorld().spawnParticle(Particle.ENCHANT, particleLoc, 2, 0, 0, 0, 0);
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

    // ========== OBLIVION HARVESTER ==========

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

                // Enhanced particles
                sweepPoint.getWorld().spawnParticle(Particle.LARGE_SMOKE, sweepPoint, 12, 0.1, 0.1, 0.1, 0);
                sweepPoint.getWorld().spawnParticle(Particle.SQUID_INK, sweepPoint, 8, 0.1, 0.1, 0.1, 0.05);
                sweepPoint.getWorld().spawnParticle(Particle.REVERSE_PORTAL, sweepPoint, 5, 0.1, 0.1, 0.1, 0);

                for (Entity entity : sweepPoint.getWorld().getNearbyEntities(sweepPoint, 1, 1, 1)) {
                    if (entity instanceof LivingEntity && entity != player && !hitEntities.contains(entity.getUniqueId())) {
                        // Trust check
                        if (entity instanceof Player && plugin.getTrustManager().isTrusted(player, (Player) entity)) {
                            continue;
                        }
                        LivingEntity living = (LivingEntity) entity;
                        living.damage(7.5, player); // Increased from 5.0 (50% increase)
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

                // Enhanced black hole particles
                riftLocation.getWorld().spawnParticle(Particle.LARGE_SMOKE, riftLocation, 120, 0.5, 0.5, 0.5, 0.1);
                riftLocation.getWorld().spawnParticle(Particle.PORTAL, riftLocation, 80, 0.5, 0.5, 0.5, 1);
                riftLocation.getWorld().spawnParticle(Particle.SQUID_INK, riftLocation, 60, 0.5, 0.5, 0.5, 0.1);
                riftLocation.getWorld().spawnParticle(Particle.REVERSE_PORTAL, riftLocation, 40, 0.5, 0.5, 0.5, 0.5);

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
                            living.damage(4.5, player); // Increased from 3.0 (50% increase)

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

    // ========== ECLIPSE DEVOURER ==========

    private boolean voidRupture(Player player) {
        Vector direction = player.getLocation().getDirection();
        Location start = player.getEyeLocation();

        Set<UUID> hitEntities = new HashSet<>();

        for (int i = 1; i <= 35; i++) {
            Location point = start.clone().add(direction.clone().multiply(i));

            // Enhanced particles
            point.getWorld().spawnParticle(Particle.REVERSE_PORTAL, point, 20, 0.2, 0.2, 0.2, 0);
            point.getWorld().spawnParticle(Particle.LARGE_SMOKE, point, 15, 0.2, 0.2, 0.2, 0);
            point.getWorld().spawnParticle(Particle.SQUID_INK, point, 10, 0.2, 0.2, 0.2, 0.05);

            for (Entity entity : point.getWorld().getNearbyEntities(point, 1.5, 1.5, 1.5)) {
                if (entity instanceof LivingEntity && entity != player && !hitEntities.contains(entity.getUniqueId())) {
                    // Trust check
                    if (entity instanceof Player && plugin.getTrustManager().isTrusted(player, (Player) entity)) {
                        continue;
                    }
                    LivingEntity living = (LivingEntity) entity;
                    living.damage(10.5, player); // Increased from 7.0 (50% increase)
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

        // Enhanced particles
        center.getWorld().spawnParticle(Particle.LARGE_SMOKE, center, 800, 7, 3, 7, 0.1);
        center.getWorld().spawnParticle(Particle.SQUID_INK, center, 400, 7, 3, 7, 0.1);
        center.getWorld().spawnParticle(Particle.REVERSE_PORTAL, center, 300, 7, 3, 7, 0.5);

        // Final explosion after 2 seconds
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            // Enhanced explosion particles
            center.getWorld().spawnParticle(Particle.EXPLOSION_EMITTER, center, 20, 1, 1, 1, 0);
            center.getWorld().spawnParticle(Particle.LARGE_SMOKE, center, 500, 3, 3, 3, 0.2);
            center.getWorld().spawnParticle(Particle.SQUID_INK, center, 300, 3, 3, 3, 0.1);
            center.getWorld().playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 2.0f, 0.5f);

            for (Entity entity : center.getWorld().getNearbyEntities(center, 7, 7, 7)) {
                if (entity instanceof LivingEntity && entity != player) {
                    // Trust check
                    if (entity instanceof Player && plugin.getTrustManager().isTrusted(player, (Player) entity)) {
                        continue;
                    }
                    LivingEntity living = (LivingEntity) entity;
                    living.damage(12.0, player); // Increased from 8.0 (50% increase)

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
        cooldowns.put("blade_of_the_fractured_stars", new int[]{25, 45});
        cooldowns.put("emberheart_scythe", new int[]{30, 180});
        cooldowns.put("tempestbreaker_spear", new int[]{25, 50});
        cooldowns.put("umbra_veil_dagger", new int[]{20, 60});
        cooldowns.put("heartroot_guardian_axe", new int[]{35, 70});
        cooldowns.put("chains_of_eternity", new int[]{35, 65});
        cooldowns.put("celestial_aegis_shield", new int[]{40, 90});
        cooldowns.put("chrono_edge", new int[]{40, 120});
        cooldowns.put("oblivion_harvester", new int[]{30, 85});
        cooldowns.put("eclipse_devourer", new int[]{35, 95});
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
                        living.damage(15.0, player); // Increased from 10.0 (50% increase)
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

            // Soul Mark - only mark target if not already marked
            String legendaryId = LegendaryItemFactory.getLegendaryId(player.getInventory().getItemInMainHand());
            if (legendaryId != null && legendaryId.equals(LegendaryType.UMBRA_VEIL_DAGGER.getId())) {
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

            if (legendaryId != null && legendaryId.equals(LegendaryType.ECLIPSE_DEVOURER.getId())) {
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
                    living.damage(8.0, player);

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
}
