package com.alterSMP.legendaryweapons.config;

import com.alterSMP.legendaryweapons.LegendaryWeaponsPlugin;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.ArrayList;
import java.util.List;

public class ConfigManager {

    private final LegendaryWeaponsPlugin plugin;
    private FileConfiguration config;

    public ConfigManager(LegendaryWeaponsPlugin plugin) {
        this.plugin = plugin;
        loadConfig();
    }

    public void loadConfig() {
        plugin.saveDefaultConfig();
        plugin.reloadConfig();
        config = plugin.getConfig();
        plugin.getLogger().info("Configuration loaded successfully!");
    }

    public void reloadConfiguration() {
        loadConfig();
        plugin.getLogger().info("Configuration reloaded!");
    }

    // ========== GLOBAL SETTINGS ==========

    public boolean isBroadcastCrafting() {
        return config.getBoolean("global.broadcast-crafting", true);
    }

    public boolean isParticlesEnabled() {
        return config.getBoolean("global.enable-particles", true);
    }

    public double getParticleDensity() {
        return config.getDouble("global.particle-density", 1.0);
    }

    public boolean arePassivesEnabled() {
        return config.getBoolean("global.enable-passives", true);
    }

    public boolean areAbilitiesEnabled() {
        return config.getBoolean("global.enable-abilities", true);
    }

    public boolean isNetheriteRestricted() {
        return config.getBoolean("global.restrict-netherite", true);
    }

    // ========== COOLDOWNS ==========

    public int getCooldown(String legendaryId, int abilityNum) {
        String path = "cooldowns." + legendaryId + ".ability" + abilityNum;
        return config.getInt(path, 30);
    }

    // ========== DAMAGE VALUES ==========

    public double getStarRiftDamage() {
        return config.getDouble("damage.star_rift_slash", 12.0);
    }

    public double getFlameHarvestPercent() {
        return config.getDouble("damage.flame_harvest_percent", 0.2);
    }

    public double getFireRebirthRestore() {
        return config.getDouble("damage.fire_rebirth_restore", 12.0);
    }

    public double getFireRebirthExplosion() {
        return config.getDouble("damage.fire_rebirth_explosion", 10.0);
    }

    public double getStormcallDamage() {
        return config.getDouble("damage.stormcall_damage", 4.0);
    }

    public double getSoulMarkTrueDamage() {
        return config.getDouble("damage.soul_mark_true_damage", 6.0);
    }

    public int getPrisonDuration() {
        return config.getInt("damage.prison_duration", 5);
    }

    public double getVoidSliceDamage() {
        return config.getDouble("damage.void_slice_damage", 5.0);
    }

    public double getVoidRiftDamage() {
        return config.getDouble("damage.void_rift_damage", 3.0);
    }

    public int getOblivionMaxSouls() {
        return config.getInt("damage.oblivion_max_souls", 20);
    }

    public double getOblivionDamagePerSoul() {
        return config.getDouble("damage.oblivion_damage_per_soul", 0.2);
    }

    public double getVoidRuptureDamage() {
        return config.getDouble("damage.void_rupture_damage", 7.0);
    }

    public double getCataclysmPulseDamage() {
        return config.getDouble("damage.cataclysm_pulse_damage", 8.0);
    }

    // ========== RANGES & DURATIONS ==========

    public int getStarRiftRange() {
        return config.getInt("ranges.star_rift_range", 30);
    }

    public int getStargateBlinkRange() {
        return config.getInt("ranges.stargate_blink_range", 45);
    }

    public int getFlashburstRadius() {
        return config.getInt("ranges.flashburst_radius", 5);
    }

    public int getFlashburstHitsRequired() {
        return config.getInt("ranges.flashburst_hits_required", 20);
    }

    public int getFlameHarvestRadius() {
        return config.getInt("ranges.flame_harvest_radius", 6);
    }

    public int getFireRebirthDuration() {
        return config.getInt("ranges.fire_rebirth_duration", 10);
    }

    public int getStormcallRange() {
        return config.getInt("ranges.stormcall_range", 15);
    }

    public int getStormcallStunDuration() {
        return config.getInt("ranges.stormcall_stun_duration", 3);
    }

    public int getShadowstepDistance() {
        return config.getInt("ranges.shadowstep_distance", 8);
    }

    public int getShadowstepInvisibility() {
        return config.getInt("ranges.shadowstep_invisibility", 4);
    }

    public int getSoulMarkDelay() {
        return config.getInt("ranges.soul_mark_delay", 15);
    }

    public int getShadowPresenceSpeed() {
        return config.getInt("ranges.shadow_presence_sneak_speed", 3);
    }

    public int getNatureGraspRadius() {
        return config.getInt("ranges.nature_grasp_radius", 6);
    }

    public int getNatureGraspDuration() {
        return config.getInt("ranges.nature_grasp_duration", 2);
    }

    public int getForestShieldDuration() {
        return config.getInt("ranges.forest_shield_duration", 10);
    }

    public int getSoulBindRange() {
        return config.getInt("ranges.soul_bind_range", 20);
    }

    public int getSoulLinksHitsRequired() {
        return config.getInt("ranges.soul_links_hits_required", 5);
    }

    public int getPrisonCageDuration() {
        return config.getInt("ranges.prison_cage_duration", 5);
    }

    public int getFrostbiteSweepRange() {
        return config.getInt("ranges.frostbite_sweep_range", 8);
    }

    public int getFrostbiteFreezeDuration() {
        return config.getInt("ranges.frostbite_freeze_duration", 3);
    }

    public int getWintersEmbraceRadius() {
        return config.getInt("ranges.winters_embrace_radius", 7);
    }

    public int getWintersEmbraceDuration() {
        return config.getInt("ranges.winters_embrace_duration", 5);
    }

    public int getAuraRadius() {
        return config.getInt("ranges.aura_radius", 5);
    }

    public int getRadiantBlockDuration() {
        return config.getInt("ranges.radiant_block_duration", 5);
    }

    public double getRadiantBlockReflectPercent() {
        return config.getDouble("ranges.radiant_block_reflect_percent", 0.75);
    }

    public int getHeavensWallRadius() {
        return config.getInt("ranges.heavens_wall_radius", 5);
    }

    public int getHeavensWallDuration() {
        return config.getInt("ranges.heavens_wall_duration", 6);
    }

    public int getEchoStrikeDuration() {
        return config.getInt("ranges.echo_strike_duration", 6);
    }

    public int getEchoStrikeDelay() {
        return config.getInt("ranges.echo_strike_delay", 1);
    }

    public int getTimeRewindDelay() {
        return config.getInt("ranges.time_rewind_delay", 5);
    }

    public double getLastSecondHpThreshold() {
        return config.getDouble("ranges.last_second_hp_threshold", 3.0);
    }

    public int getVoidRuptureRange() {
        return config.getInt("ranges.void_rupture_range", 35);
    }

    public int getDragonsGazeRadius() {
        return config.getInt("ranges.dragons_gaze_radius", 8);
    }

    public int getCataclysmPulseRadius() {
        return config.getInt("ranges.cataclysm_pulse_radius", 7);
    }

    public int getCataclysmExplosionDelay() {
        return config.getInt("ranges.cataclysm_explosion_delay", 2);
    }

    // ========== RECIPES ==========

    /**
     * Get recipe ingredients for a legendary item.
     * Supports special syntax for enchanted books: ENCHANTED_BOOK:ENCHANT_NAME:LEVEL
     * Example: ENCHANTED_BOOK:FEATHER_FALLING:4
     */
    public com.alterSMP.legendaryweapons.altar.RecipeIngredient[][] getRecipeIngredients(String legendaryId) {
        List<String> ingredientStrings = config.getStringList("recipes." + legendaryId);

        if (ingredientStrings == null || ingredientStrings.size() != 25) {
            plugin.getLogger().warning("Invalid recipe for " + legendaryId + " - using empty recipe");
            return createEmptyIngredientRecipe();
        }

        com.alterSMP.legendaryweapons.altar.RecipeIngredient[][] ingredients =
            new com.alterSMP.legendaryweapons.altar.RecipeIngredient[5][5];
        int index = 0;

        for (int row = 0; row < 5; row++) {
            for (int col = 0; col < 5; col++) {
                String ingredientStr = ingredientStrings.get(index++);
                ingredients[row][col] = com.alterSMP.legendaryweapons.altar.RecipeIngredient.parse(ingredientStr);
            }
        }

        return ingredients;
    }

    private com.alterSMP.legendaryweapons.altar.RecipeIngredient[][] createEmptyIngredientRecipe() {
        com.alterSMP.legendaryweapons.altar.RecipeIngredient[][] ingredients =
            new com.alterSMP.legendaryweapons.altar.RecipeIngredient[5][5];
        for (int i = 0; i < 5; i++) {
            for (int j = 0; j < 5; j++) {
                ingredients[i][j] = new com.alterSMP.legendaryweapons.altar.RecipeIngredient(Material.AIR);
            }
        }
        return ingredients;
    }

    // ========== MESSAGES ==========

    public String getMessage(String key) {
        String message = config.getString("messages." + key, "&cMessage not found: " + key);
        return ChatColor.translateAlternateColorCodes('&', message);
    }

    public String getMessage(String key, String... replacements) {
        String message = getMessage(key);

        // Replace placeholders
        if (replacements.length % 2 == 0) {
            for (int i = 0; i < replacements.length; i += 2) {
                String placeholder = "{" + replacements[i] + "}";
                String value = replacements[i + 1];
                message = message.replace(placeholder, value);
            }
        }

        return message;
    }

    // ========== PERFORMANCE ==========

    public int getPassiveCheckInterval() {
        return config.getInt("performance.passive-check-interval", 10);
    }

    public long getIceCreationCooldown() {
        return config.getLong("performance.ice-creation-cooldown", 500);
    }

    public int getVoidRiftDamageInterval() {
        return config.getInt("performance.void-rift-damage-interval", 10);
    }
}
