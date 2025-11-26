package com.alterSMP.legendaryweapons.passives;

import com.alterSMP.legendaryweapons.LegendaryWeaponsPlugin;
import com.alterSMP.legendaryweapons.items.LegendaryItemFactory;
import com.alterSMP.legendaryweapons.items.LegendaryType;
import org.bukkit.*;
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

    // Ionflare Leggings - ion charges
    private Map<UUID, Integer> ionCharges = new HashMap<>();
    private Set<UUID> chainLightningActive = new HashSet<>(); // Prevent recursive chain lightning
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
    public void onPlayerDamagedMelee(EntityDamageByEntityEvent event) {
        if (event.isCancelled()) return;
        if (!(event.getEntity() instanceof Player)) return;

        // Only count melee attacks (direct entity damage, not projectiles)
        if (event.getCause() != EntityDamageEvent.DamageCause.ENTITY_ATTACK &&
            event.getCause() != EntityDamageEvent.DamageCause.ENTITY_SWEEP_ATTACK) {
            return;
        }

        Player player = (Player) event.getEntity();
        ItemStack chestplate = player.getInventory().getChestplate();
        String legendaryId = LegendaryItemFactory.getLegendaryId(chestplate);

        if (legendaryId != null && legendaryId.equals(LegendaryType.THUNDERFORGE_CHESTPLATE.getId())) {
            // Increment hit counter
            int hits = hitCounter.getOrDefault(player.getUniqueId(), 0) + 1;

            if (hits >= 7) {
                // Trigger shockwave
                electricShockwave(player);
                hitCounter.put(player.getUniqueId(), 0); // Reset counter
            } else {
                hitCounter.put(player.getUniqueId(), hits);
                player.sendMessage(ChatColor.YELLOW + "⚡ " + hits + "/7 hits");
            }
        }
    }

    private void electricShockwave(Player player) {
        Location loc = player.getLocation();
        double radius = 5.0;

        // Mark shockwave as active to prevent ion charge counting
        shockwaveActive.add(player.getUniqueId());

        // Particles
        player.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, loc, 200, 2, 1, 2, 0.3);
        player.getWorld().spawnParticle(Particle.FIREWORK, loc, 100, 2, 1, 2, 0.2);

        // Sound
        player.getWorld().playSound(loc, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 1.5f, 1.5f);

        // Damage and knockback
        for (LivingEntity entity : loc.getWorld().getNearbyLivingEntities(loc, radius)) {
            if (entity.equals(player)) continue;
            if (entity instanceof Player) {
                Player target = (Player) entity;
                if (plugin.getTrustManager().isTrusted(player, target)) {
                    continue;
                }
            }

            // 2 hearts damage to full prot 4 diamond = ~4 damage total
            entity.damage(4.0, player);

            // Knockback
            Vector direction = entity.getLocation().toVector().subtract(loc.toVector()).normalize();
            direction.multiply(1.5).setY(0.5);
            entity.setVelocity(direction);

            if (entity instanceof Player) {
                ((Player) entity).sendMessage(ChatColor.YELLOW + "⚡ Hit by " + ChatColor.GOLD + "Electric Shockwave" +
                    ChatColor.YELLOW + " from " + player.getName() + "!");
            }
        }

        // Remove shockwave flag
        shockwaveActive.remove(player.getUniqueId());

        player.sendMessage(ChatColor.GOLD + "⚡ Electric Shockwave Released!");
    }

    // ========== IONFLARE LEGGINGS ==========

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerHit(EntityDamageByEntityEvent event) {
        if (event.isCancelled()) return;
        if (!(event.getDamager() instanceof Player)) return;
        if (!(event.getEntity() instanceof LivingEntity)) return;

        // Only count melee attacks (not projectiles, abilities, etc.)
        if (event.getCause() != EntityDamageEvent.DamageCause.ENTITY_ATTACK &&
            event.getCause() != EntityDamageEvent.DamageCause.ENTITY_SWEEP_ATTACK) {
            return;
        }

        Player player = (Player) event.getDamager();

        // Prevent chain lightning from triggering itself
        if (chainLightningActive.contains(player.getUniqueId())) {
            return;
        }

        // Prevent shockwave damage from counting towards ion charges
        if (shockwaveActive.contains(player.getUniqueId())) {
            return;
        }

        ItemStack leggings = player.getInventory().getLeggings();
        String legendaryId = LegendaryItemFactory.getLegendaryId(leggings);

        if (legendaryId != null && legendaryId.equals(LegendaryType.IONFLARE_LEGGINGS.getId())) {
            // Add ion charge
            int charges = ionCharges.getOrDefault(player.getUniqueId(), 0) + 1;

            if (charges >= 5) {
                // Release chain lightning
                chainLightning(player, (LivingEntity) event.getEntity());
                ionCharges.put(player.getUniqueId(), 0);
            } else {
                ionCharges.put(player.getUniqueId(), charges);
                player.sendMessage(ChatColor.AQUA + "⚡ Ion Charge: " + ChatColor.YELLOW + charges + "/5");
            }
        }
    }

    private void chainLightning(Player player, LivingEntity firstTarget) {
        // Mark chain lightning as active to prevent recursive triggers
        chainLightningActive.add(player.getUniqueId());

        List<LivingEntity> targets = new ArrayList<>();
        targets.add(firstTarget);

        // Find up to 2 more targets
        Location center = firstTarget.getLocation();
        List<LivingEntity> nearby = new ArrayList<>(center.getWorld().getNearbyLivingEntities(center, 8.0));
        nearby.remove(player); // Don't hit caster
        nearby.remove(firstTarget); // Don't hit first target again

        // Filter out trusted players
        nearby.removeIf(entity -> {
            if (entity instanceof Player) {
                return plugin.getTrustManager().isTrusted(player, (Player) entity);
            }
            return false;
        });

        // Add up to 2 more
        for (int i = 0; i < Math.min(2, nearby.size()); i++) {
            targets.add(nearby.get(i));
        }

        // Chain lightning effect
        LivingEntity previous = null;
        for (LivingEntity target : targets) {
            Location from = previous == null ? player.getLocation().add(0, 1, 0) : previous.getEyeLocation();
            Location to = target.getEyeLocation();

            // Lightning particles
            drawLightningLine(from, to);

            // Damage
            target.damage(6.0, player);
            target.getWorld().playSound(target.getLocation(), Sound.ENTITY_LIGHTNING_BOLT_IMPACT, 0.8f, 1.5f);

            if (target instanceof Player) {
                ((Player) target).sendMessage(ChatColor.AQUA + "⚡ Hit by " + ChatColor.GOLD + "Chain Lightning" +
                    ChatColor.AQUA + " from " + player.getName() + "!");
            }

            previous = target;
        }

        player.sendMessage(ChatColor.GOLD + "⚡ Chain Lightning! Hit " + targets.size() + " targets!");

        // Remove flag after chain lightning completes
        chainLightningActive.remove(player.getUniqueId());
    }

    private void drawLightningLine(Location from, Location to) {
        Vector direction = to.toVector().subtract(from.toVector());
        double distance = direction.length();
        direction.normalize();

        for (double i = 0; i < distance; i += 0.3) {
            Location point = from.clone().add(direction.clone().multiply(i));
            from.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, point, 3, 0.1, 0.1, 0.1, 0);
            from.getWorld().spawnParticle(Particle.END_ROD, point, 1, 0.05, 0.05, 0.05, 0);
        }
    }

    // ========== BLOODREAPER HOOD ==========

    @EventHandler
    public void onEntityKill(EntityDeathEvent event) {
        // Only trigger on PLAYER kills, not all entity kills
        if (!(event.getEntity() instanceof Player)) return;

        Player killer = event.getEntity().getKiller();
        if (killer == null) return;

        ItemStack helmet = killer.getInventory().getHelmet();
        String legendaryId = LegendaryItemFactory.getLegendaryId(helmet);

        if (legendaryId != null && legendaryId.equals(LegendaryType.BLOODREAPER_HOOD.getId())) {
            UUID killerId = killer.getUniqueId();

            // Store original max health if not already stored
            if (!originalMaxHealth.containsKey(killerId)) {
                originalMaxHealth.put(killerId, killer.getAttribute(org.bukkit.attribute.Attribute.GENERIC_MAX_HEALTH).getBaseValue());
            }

            // Cancel any existing health restore task
            if (originalMaxHealth.containsKey(killerId)) {
                // Set max health to +10 HP (5 hearts)
                double originalMax = originalMaxHealth.get(killerId);
                killer.getAttribute(org.bukkit.attribute.Attribute.GENERIC_MAX_HEALTH).setBaseValue(originalMax + 10.0);

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
                    killer.getAttribute(org.bukkit.attribute.Attribute.GENERIC_MAX_HEALTH).setBaseValue(originalMax);

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
                player.sendMessage(ChatColor.RED + "Critical Rush! " + ChatColor.YELLOW + "+Speed for 3s");

                // Particles
                player.getWorld().spawnParticle(Particle.CRIT, event.getEntity().getLocation(), 20, 0.3, 0.5, 0.3, 0);
            }
        }
    }
}
