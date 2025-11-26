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
    private Map<UUID, Integer> bladeHitCounter; // Blade of Fractured Stars
    private Map<UUID, Integer> chainsHitCounter; // Chains of Eternity
    private Map<UUID, Long> lastIceCreateTime; // Glacierbound path cooldown

    public PassiveEffectManager(LegendaryWeaponsPlugin plugin) {
        this.plugin = plugin;
        this.bladeHitCounter = new HashMap<>();
        this.chainsHitCounter = new HashMap<>();
        this.lastIceCreateTime = new HashMap<>();

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
        }.runTaskTimer(plugin, 0L, 10L); // Every 0.5 seconds
    }

    private void applyPassiveEffects(Player player) {
        // Check main hand
        ItemStack mainHand = player.getInventory().getItemInMainHand();
        String mainLegendary = LegendaryItemFactory.getLegendaryId(mainHand);

        // Check boots
        ItemStack boots = player.getInventory().getBoots();
        String bootsLegendary = LegendaryItemFactory.getLegendaryId(boots);

        // Check offhand
        ItemStack offhand = player.getInventory().getItemInOffHand();
        String offhandLegendary = LegendaryItemFactory.getLegendaryId(offhand);

        // Apply passives based on legendary held/worn
        if (mainLegendary != null) {
            applyMainHandPassive(player, mainLegendary, mainHand);
        }

        if (bootsLegendary != null) {
            applyBootsPassive(player, bootsLegendary);
        }

        if (offhandLegendary != null) {
            applyOffhandPassive(player, offhandLegendary);
        }

        // Tick Emberstride Greaves passives (flame trail, attack speed, lava speed)
        if (plugin.getArmorPassivesListener() != null) {
            plugin.getArmorPassivesListener().tickEmberstrideGreaves(player);
        }
    }

    private void applyMainHandPassive(Player player, String legendaryId, ItemStack item) {
        LegendaryType type = LegendaryType.fromId(legendaryId);
        if (type == null) return;

        switch (type) {
            case TEMPESTBREAKER_SPEAR:
                // Windwalker - Dolphin's Grace + Conduit Power
                player.addPotionEffect(new PotionEffect(PotionEffectType.DOLPHINS_GRACE, 20, 0, true, false));
                player.addPotionEffect(new PotionEffect(PotionEffectType.CONDUIT_POWER, 20, 0, true, false));
                break;

            case THOUSAND_DEMON_DAGGERS:
                // Shadow Presence - Speed III while sneaking
                if (player.isSneaking()) {
                    player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 20, 2, true, false));
                }
                break;

            case DIVINE_AXE_RHITTA:
                // Nature Channel - Regen 5 on natural blocks
                Block below = player.getLocation().subtract(0, 1, 0).getBlock();
                Material belowType = below.getType();
                if (belowType == Material.GRASS_BLOCK || belowType.name().contains("LOG") ||
                    belowType.name().contains("LEAVES")) {
                    player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 20, 4, true, false)); // Level 5 = amplifier 4
                }
                break;

            case CHRONO_BLADE:
                // Last Second - Buffs at low HP
                if (player.getHealth() <= 3.0) {
                    player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 20, 1, true, false));
                    player.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, 20, 0, true, false));
                }
                break;

            case CREATION_SPLITTER:
                // Dragon's Gaze - Nearby players glow
                for (Entity entity : player.getNearbyEntities(8, 8, 8)) {
                    if (entity instanceof Player) {
                        Player target = (Player) entity;
                        // Trust check
                        if (plugin.getTrustManager().isTrusted(player, target)) {
                            continue;
                        }
                        target.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, 20, 0, true, false));
                    }
                }
                break;
        }
    }

    private void applyBootsPassive(Player player, String legendaryId) {
        LegendaryType type = LegendaryType.fromId(legendaryId);
        if (type == null) return;

        if (type == LegendaryType.SKYBREAKER_BOOTS) {
            // Featherfall - No fall damage (handled in event listener)
            // No tick-based passive needed
        }
    }

    private void applyOffhandPassive(Player player, String legendaryId) {
        LegendaryType type = LegendaryType.fromId(legendaryId);
        if (type == null) return;

        if (type == LegendaryType.CELESTIAL_AEGIS_SHIELD) {
            // Aura of Protection - Allies gain Resistance (only trusted players)
            for (Entity entity : player.getNearbyEntities(5, 5, 5)) {
                if (entity instanceof Player) {
                    Player ally = (Player) entity;
                    // Only grant resistance to trusted players (or grant to all? Based on task, apply trust check)
                    // Trust check - only help trusted allies
                    if (!plugin.getTrustManager().isTrusted(player, ally)) {
                        continue;
                    }
                    ally.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, 20, 0, true, false));
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

        Player player = (Player) event.getDamager();
        ItemStack mainHand = player.getInventory().getItemInMainHand();
        String legendaryId = LegendaryItemFactory.getLegendaryId(mainHand);

        if (legendaryId == null) return;

        LegendaryType type = LegendaryType.fromId(legendaryId);
        if (type == null) return;

        // Holy Moonlight Sword - Flashburst Counter
        if (type == LegendaryType.HOLY_MOONLIGHT_SWORD) {
            int count = bladeHitCounter.getOrDefault(player.getUniqueId(), 0) + 1;
            bladeHitCounter.put(player.getUniqueId(), count);

            if (count >= 20) {
                // Trigger flashburst
                bladeHitCounter.put(player.getUniqueId(), 0);

                for (Entity entity : player.getNearbyEntities(5, 5, 5)) {
                    if (entity instanceof LivingEntity) {
                        // Trust check
                        if (entity instanceof Player) {
                            if (plugin.getTrustManager().isTrusted(player, (Player) entity)) {
                                continue;
                            }
                        }
                        LivingEntity living = (LivingEntity) entity;
                        // Use DARKNESS with max amplifier instead of BLINDNESS
                        living.addPotionEffect(new PotionEffect(PotionEffectType.DARKNESS, 50, 255));
                        living.addPotionEffect(new PotionEffect(PotionEffectType.NAUSEA, 20, 0));
                    }
                }

                player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_SCREAM, 1.0f, 1.0f);
                player.sendMessage(ChatColor.AQUA + "Flashburst triggered!");
            }
        }

        // Chains of Eternity - Soul Links
        if (type == LegendaryType.CHAINS_OF_ETERNITY) {
            int count = chainsHitCounter.getOrDefault(player.getUniqueId(), 0) + 1;
            chainsHitCounter.put(player.getUniqueId(), count);

            if (count >= 5) {
                chainsHitCounter.put(player.getUniqueId(), 0);

                if (event.getEntity() instanceof LivingEntity) {
                    LivingEntity target = (LivingEntity) event.getEntity();

                    // Trust check
                    if (target instanceof Player && plugin.getTrustManager().isTrusted(player, (Player) target)) {
                        return;
                    }

                    // Immobilize
                    target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 30, 255));
                    target.addPotionEffect(new PotionEffect(PotionEffectType.JUMP_BOOST, 30, 128));

                    // Visual effect
                    target.getWorld().spawnParticle(Particle.SOUL, target.getLocation(), 30, 0.5, 1, 0.5, 0);

                    player.sendMessage(ChatColor.DARK_GRAY + "Soul Link activated!");
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
                killer.sendMessage(ChatColor.DARK_PURPLE + "Soul collected: " +
                    ChatColor.LIGHT_PURPLE + (currentSouls + 1) + "/5");
            }
        }
    }
}
