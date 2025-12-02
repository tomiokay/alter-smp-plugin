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
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.util.*;

public class ArmorPassivesListener implements Listener {

    private final LegendaryWeaponsPlugin plugin;

    // Thunderforge Chestplate - hit counter
    private Map<UUID, Integer> hitCounter = new HashMap<>();

    // Emberstride Greaves - flame trail tracking
    private Map<UUID, Location> lastFlameLocation = new HashMap<>();
    private Set<UUID> shockwaveActive = new HashSet<>(); // Prevent shockwave damage from counting as hits

    // Skybreaker Boots - falling state
    private Set<UUID> isFalling = new HashSet<>();
    private Map<UUID, Double> fallDistance = new HashMap<>();

    // Bloodreaper Hood - health boost tracking
    private Map<UUID, Double> originalMaxHealth = new HashMap<>();

    public ArmorPassivesListener(LegendaryWeaponsPlugin plugin) {
        this.plugin = plugin;
    }

    // ========== SKYBREAKER BOOTS ==========

    @EventHandler(priority = EventPriority.HIGH)
    public void onFallDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player)) return;
        if (event.getCause() != EntityDamageEvent.DamageCause.FALL) return;

        Player player = (Player) event.getEntity();
        ItemStack boots = player.getInventory().getBoots();
        String legendaryId = LegendaryItemFactory.getLegendaryId(boots);

        if (legendaryId != null && legendaryId.equals(LegendaryType.SKYBREAKER_BOOTS.getId())) {
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

        if (legendaryId != null && legendaryId.equals(LegendaryType.SKYBREAKER_BOOTS.getId())) {
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

            if (legendaryId != null && legendaryId.equals(LegendaryType.SKYBREAKER_BOOTS.getId())) {
                event.setCancelled(true); // No fall damage

                double dist = player.getFallDistance();

                // Calculate damage exactly like vanilla mace: base 6 + (fall_distance - 1.5) * 3, capped at 100
                double baseDamage = 6.0;
                if (dist > 1.5) {
                    baseDamage += (dist - 1.5) * 3.0;
                }
                baseDamage = Math.min(baseDamage, 100.0); // Cap at 100 damage

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

    // ========== THUNDERFORGE CHESTPLATE ==========

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

        ItemStack chestplate = player.getInventory().getChestplate();
        String legendaryId = LegendaryItemFactory.getLegendaryId(chestplate);

        if (legendaryId != null && legendaryId.equals(LegendaryType.THUNDERFORGE_CHESTPLATE.getId())) {
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

        // Deal 3 hearts (6 damage) true damage
        double currentHealth = target.getHealth();
        double newHealth = Math.max(0, currentHealth - 6.0);
        target.setHealth(newHealth);

        // Particles for extra effect
        target.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, targetLoc.add(0, 1, 0), 100, 1, 1, 1, 0.3);

        if (target instanceof Player) {
            ((Player) target).sendMessage(ChatColor.YELLOW + "⚡ Struck by " + ChatColor.GOLD + "Thunderforge Lightning Storm" +
                ChatColor.YELLOW + " from " + player.getName() + "!");
        }

        // Remove active flag
        shockwaveActive.remove(player.getUniqueId());
    }

    // ========== EMBERSTRIDE GREAVES ==========

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

        if (legendaryId != null && legendaryId.equals(LegendaryType.EMBERSTRIDE_GREAVES.getId())) {
            // Immune to fire, lava, and magma
            event.setCancelled(true);
            player.setFireTicks(0); // Extinguish any fire
        }
    }

    /**
     * Called by PassiveEffectManager tick to handle Emberstride Greaves passives
     */
    public void tickEmberstrideGreaves(Player player) {
        ItemStack leggings = player.getInventory().getLeggings();
        String legendaryId = LegendaryItemFactory.getLegendaryId(leggings);

        if (legendaryId == null || !legendaryId.equals(LegendaryType.EMBERSTRIDE_GREAVES.getId())) {
            return;
        }

        UUID playerId = player.getUniqueId();

        // Flame trail when walking - damages and burns enemies
        Location currentLoc = player.getLocation();
        Location lastLoc = lastFlameLocation.get(playerId);

        if (lastLoc != null && lastLoc.getWorld().equals(currentLoc.getWorld())) {
            double distance = lastLoc.distance(currentLoc);
            // Only spawn flames if player moved and is on ground
            if (distance > 0.3 && player.isOnGround()) {
                // Spawn flame particles along path
                player.getWorld().spawnParticle(Particle.FLAME, currentLoc.clone().add(0, 0.1, 0), 5, 0.3, 0.1, 0.3, 0.02);
                player.getWorld().spawnParticle(Particle.SMALL_FLAME, currentLoc.clone().add(0, 0.1, 0), 3, 0.2, 0.05, 0.2, 0.01);

                // Damage enemies standing in the flame trail
                for (LivingEntity entity : currentLoc.getWorld().getNearbyLivingEntities(currentLoc, 1.0)) {
                    if (entity.equals(player)) continue;
                    if (entity instanceof Player) {
                        Player target = (Player) entity;
                        if (plugin.getTrustManager().isTrusted(player, target)) {
                            continue;
                        }
                    }

                    // Set on fire and deal small damage
                    entity.setFireTicks(40); // 2 seconds of fire
                    entity.damage(2.0, player); // 1 heart damage
                }
            }
        }
        lastFlameLocation.put(playerId, currentLoc.clone());

        // +10% attack speed when above 50% HP
        double maxHealth = player.getAttribute(Attribute.MAX_HEALTH).getValue();
        if (player.getHealth() > maxHealth * 0.5) {
            // Apply Haste I for attack speed (10% faster)
            player.addPotionEffect(new PotionEffect(PotionEffectType.HASTE, 15, 0, true, false));
        }

        // +500% movement speed in lava (Speed 24 gives roughly 500% boost)
        Material blockAtFeet = player.getLocation().getBlock().getType();
        if (blockAtFeet == Material.LAVA) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 15, 24, true, false)); // Speed 25 (~500%)
            player.addPotionEffect(new PotionEffect(PotionEffectType.FIRE_RESISTANCE, 15, 0, true, false));
        }
    }

    // ========== BLOODREAPER HOOD ==========

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

        if (legendaryId != null && legendaryId.equals(LegendaryType.BLOODREAPER_HOOD.getId())) {
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

        if (legendaryId != null && legendaryId.equals(LegendaryType.BLOODREAPER_HOOD.getId())) {
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
}
