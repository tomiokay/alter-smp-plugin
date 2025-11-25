package com.alterSMP.legendaryweapons.altar;

import com.alterSMP.legendaryweapons.LegendaryWeaponsPlugin;
import com.alterSMP.legendaryweapons.items.LegendaryType;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;

public class AltarRecipe {

    private final LegendaryType result;
    private final Material[][] pattern;

    public AltarRecipe(LegendaryType result, Material[][] pattern) {
        this.result = result;
        this.pattern = pattern;
    }

    public LegendaryType getResult() {
        return result;
    }

    public Material[][] getPattern() {
        return pattern;
    }

    public boolean matches(ItemStack[] gridItems) {
        if (gridItems.length != 25) {
            return false;
        }

        // Check each position in 5x5 grid
        for (int row = 0; row < 5; row++) {
            for (int col = 0; col < 5; col++) {
                int index = row * 5 + col;
                Material expected = pattern[row][col];
                ItemStack actual = gridItems[index];

                if (expected == Material.AIR) {
                    // Expecting empty slot
                    if (actual != null && actual.getType() != Material.AIR) {
                        return false;
                    }
                } else {
                    // Expecting specific material
                    if (actual == null || actual.getType() != expected) {
                        return false;
                    }
                }
            }
        }

        return true;
    }

    // Factory method to create all recipes from config
    public static Map<String, AltarRecipe> createAllRecipes() {
        Map<String, AltarRecipe> recipes = new HashMap<>();
        LegendaryWeaponsPlugin plugin = LegendaryWeaponsPlugin.getInstance();

        // Load all recipes from config
        for (LegendaryType type : LegendaryType.values()) {
            Material[][] pattern = plugin.getConfigManager().getRecipe(type.getId());
            recipes.put(type.getId(), new AltarRecipe(type, pattern));
        }

        return recipes;
    }

    // OLD HARDCODED RECIPES - KEPT FOR REFERENCE BUT NOT USED
    // (Recipes are now loaded from config.yml)
    @Deprecated
    private static Map<String, AltarRecipe> createHardcodedRecipes() {
        Map<String, AltarRecipe> recipes = new HashMap<>();

        // Recipe 1: Blade of the Fractured Stars (EXAMPLE)
        Material[][] bladePattern = {
            {Material.NETHER_STAR, Material.DIAMOND_BLOCK, Material.DIAMOND_BLOCK, Material.DIAMOND_BLOCK, Material.NETHER_STAR},
            {Material.DIAMOND_BLOCK, Material.CRYING_OBSIDIAN, Material.AMETHYST_BLOCK, Material.CRYING_OBSIDIAN, Material.DIAMOND_BLOCK},
            {Material.DIAMOND_BLOCK, Material.AMETHYST_BLOCK, Material.NETHERITE_SWORD, Material.AMETHYST_BLOCK, Material.DIAMOND_BLOCK},
            {Material.DIAMOND_BLOCK, Material.CRYING_OBSIDIAN, Material.AMETHYST_BLOCK, Material.CRYING_OBSIDIAN, Material.DIAMOND_BLOCK},
            {Material.NETHER_STAR, Material.DIAMOND_BLOCK, Material.DIAMOND_BLOCK, Material.DIAMOND_BLOCK, Material.NETHER_STAR}
        };
        recipes.put(LegendaryType.BLADE_OF_FRACTURED_STARS.getId(),
            new AltarRecipe(LegendaryType.BLADE_OF_FRACTURED_STARS, bladePattern));

        // Recipe 2: Emberheart Scythe
        Material[][] emberPattern = {
            {Material.BLAZE_ROD, Material.FIRE_CHARGE, Material.MAGMA_BLOCK, Material.FIRE_CHARGE, Material.BLAZE_ROD},
            {Material.FIRE_CHARGE, Material.NETHERITE_INGOT, Material.NETHERITE_INGOT, Material.NETHERITE_INGOT, Material.FIRE_CHARGE},
            {Material.MAGMA_BLOCK, Material.NETHERITE_INGOT, Material.NETHERITE_SWORD, Material.NETHERITE_INGOT, Material.MAGMA_BLOCK},
            {Material.FIRE_CHARGE, Material.NETHERITE_INGOT, Material.NETHERITE_INGOT, Material.NETHERITE_INGOT, Material.FIRE_CHARGE},
            {Material.BLAZE_ROD, Material.FIRE_CHARGE, Material.MAGMA_BLOCK, Material.FIRE_CHARGE, Material.BLAZE_ROD}
        };
        recipes.put(LegendaryType.EMBERHEART_SCYTHE.getId(),
            new AltarRecipe(LegendaryType.EMBERHEART_SCYTHE, emberPattern));

        // Recipe 3: Tempestbreaker Spear
        Material[][] tempestPattern = {
            {Material.FEATHER, Material.PHANTOM_MEMBRANE, Material.PHANTOM_MEMBRANE, Material.PHANTOM_MEMBRANE, Material.FEATHER},
            {Material.PHANTOM_MEMBRANE, Material.DIAMOND, Material.PRISMARINE_CRYSTALS, Material.DIAMOND, Material.PHANTOM_MEMBRANE},
            {Material.PHANTOM_MEMBRANE, Material.PRISMARINE_CRYSTALS, Material.TRIDENT, Material.PRISMARINE_CRYSTALS, Material.PHANTOM_MEMBRANE},
            {Material.PHANTOM_MEMBRANE, Material.DIAMOND, Material.PRISMARINE_CRYSTALS, Material.DIAMOND, Material.PHANTOM_MEMBRANE},
            {Material.FEATHER, Material.PHANTOM_MEMBRANE, Material.PHANTOM_MEMBRANE, Material.PHANTOM_MEMBRANE, Material.FEATHER}
        };
        recipes.put(LegendaryType.TEMPESTBREAKER_SPEAR.getId(),
            new AltarRecipe(LegendaryType.TEMPESTBREAKER_SPEAR, tempestPattern));

        // Recipe 4: Umbra Veil Dagger
        Material[][] umbraPattern = {
            {Material.ENDER_PEARL, Material.OBSIDIAN, Material.OBSIDIAN, Material.OBSIDIAN, Material.ENDER_PEARL},
            {Material.OBSIDIAN, Material.SCULK, Material.ECHO_SHARD, Material.SCULK, Material.OBSIDIAN},
            {Material.OBSIDIAN, Material.ECHO_SHARD, Material.NETHERITE_SWORD, Material.ECHO_SHARD, Material.OBSIDIAN},
            {Material.OBSIDIAN, Material.SCULK, Material.ECHO_SHARD, Material.SCULK, Material.OBSIDIAN},
            {Material.ENDER_PEARL, Material.OBSIDIAN, Material.OBSIDIAN, Material.OBSIDIAN, Material.ENDER_PEARL}
        };
        recipes.put(LegendaryType.UMBRA_VEIL_DAGGER.getId(),
            new AltarRecipe(LegendaryType.UMBRA_VEIL_DAGGER, umbraPattern));

        // Recipe 5: Heartroot Guardian Axe
        Material[][] heartrootPattern = {
            {Material.OAK_LOG, Material.MOSS_BLOCK, Material.MOSS_BLOCK, Material.MOSS_BLOCK, Material.OAK_LOG},
            {Material.MOSS_BLOCK, Material.EMERALD_BLOCK, Material.GLOW_BERRIES, Material.EMERALD_BLOCK, Material.MOSS_BLOCK},
            {Material.MOSS_BLOCK, Material.GLOW_BERRIES, Material.NETHERITE_AXE, Material.GLOW_BERRIES, Material.MOSS_BLOCK},
            {Material.MOSS_BLOCK, Material.EMERALD_BLOCK, Material.GLOW_BERRIES, Material.EMERALD_BLOCK, Material.MOSS_BLOCK},
            {Material.OAK_LOG, Material.MOSS_BLOCK, Material.MOSS_BLOCK, Material.MOSS_BLOCK, Material.OAK_LOG}
        };
        recipes.put(LegendaryType.HEARTROOT_GUARDIAN_AXE.getId(),
            new AltarRecipe(LegendaryType.HEARTROOT_GUARDIAN_AXE, heartrootPattern));

        // Recipe 6: Chains of Eternity
        Material[][] chainsPattern = {
            {Material.CHAIN, Material.SOUL_SAND, Material.WITHER_SKELETON_SKULL, Material.SOUL_SAND, Material.CHAIN},
            {Material.SOUL_SAND, Material.NETHERITE_SCRAP, Material.NETHERITE_SCRAP, Material.NETHERITE_SCRAP, Material.SOUL_SAND},
            {Material.WITHER_SKELETON_SKULL, Material.NETHERITE_SCRAP, Material.WOODEN_SHOVEL, Material.NETHERITE_SCRAP, Material.WITHER_SKELETON_SKULL},
            {Material.SOUL_SAND, Material.NETHERITE_SCRAP, Material.NETHERITE_SCRAP, Material.NETHERITE_SCRAP, Material.SOUL_SAND},
            {Material.CHAIN, Material.SOUL_SAND, Material.WITHER_SKELETON_SKULL, Material.SOUL_SAND, Material.CHAIN}
        };
        recipes.put(LegendaryType.CHAINS_OF_ETERNITY.getId(),
            new AltarRecipe(LegendaryType.CHAINS_OF_ETERNITY, chainsPattern));

        // Recipe 7: Glacierbound Halberd
        Material[][] skybreakerPattern = {
            {Material.FEATHER, Material.PHANTOM_MEMBRANE, Material.DIAMOND_BLOCK, Material.PHANTOM_MEMBRANE, Material.FEATHER},
            {Material.PHANTOM_MEMBRANE, Material.DIAMOND, Material.DIAMOND, Material.DIAMOND, Material.PHANTOM_MEMBRANE},
            {Material.DIAMOND_BLOCK, Material.DIAMOND, Material.DIAMOND_BOOTS, Material.DIAMOND, Material.DIAMOND_BLOCK},
            {Material.PHANTOM_MEMBRANE, Material.DIAMOND, Material.DIAMOND, Material.DIAMOND, Material.PHANTOM_MEMBRANE},
            {Material.FEATHER, Material.PHANTOM_MEMBRANE, Material.DIAMOND_BLOCK, Material.PHANTOM_MEMBRANE, Material.FEATHER}
        };
        recipes.put(LegendaryType.SKYBREAKER_BOOTS.getId(),
            new AltarRecipe(LegendaryType.SKYBREAKER_BOOTS, skybreakerPattern));

        // Recipe 8: Celestial Aegis Shield
        Material[][] celestialPattern = {
            {Material.END_STONE, Material.GLOWSTONE, Material.GLOWSTONE, Material.GLOWSTONE, Material.END_STONE},
            {Material.GLOWSTONE, Material.GOLD_BLOCK, Material.TOTEM_OF_UNDYING, Material.GOLD_BLOCK, Material.GLOWSTONE},
            {Material.GLOWSTONE, Material.TOTEM_OF_UNDYING, Material.SHIELD, Material.TOTEM_OF_UNDYING, Material.GLOWSTONE},
            {Material.GLOWSTONE, Material.GOLD_BLOCK, Material.TOTEM_OF_UNDYING, Material.GOLD_BLOCK, Material.GLOWSTONE},
            {Material.END_STONE, Material.GLOWSTONE, Material.GLOWSTONE, Material.GLOWSTONE, Material.END_STONE}
        };
        recipes.put(LegendaryType.CELESTIAL_AEGIS_SHIELD.getId(),
            new AltarRecipe(LegendaryType.CELESTIAL_AEGIS_SHIELD, celestialPattern));

        // Recipe 9: Chrono Edge
        Material[][] chronoPattern = {
            {Material.CLOCK, Material.REDSTONE_BLOCK, Material.REDSTONE_BLOCK, Material.REDSTONE_BLOCK, Material.CLOCK},
            {Material.REDSTONE_BLOCK, Material.AMETHYST_SHARD, Material.RECOVERY_COMPASS, Material.AMETHYST_SHARD, Material.REDSTONE_BLOCK},
            {Material.REDSTONE_BLOCK, Material.RECOVERY_COMPASS, Material.NETHERITE_SWORD, Material.RECOVERY_COMPASS, Material.REDSTONE_BLOCK},
            {Material.REDSTONE_BLOCK, Material.AMETHYST_SHARD, Material.RECOVERY_COMPASS, Material.AMETHYST_SHARD, Material.REDSTONE_BLOCK},
            {Material.CLOCK, Material.REDSTONE_BLOCK, Material.REDSTONE_BLOCK, Material.REDSTONE_BLOCK, Material.CLOCK}
        };
        recipes.put(LegendaryType.CHRONO_EDGE.getId(),
            new AltarRecipe(LegendaryType.CHRONO_EDGE, chronoPattern));

        // Recipe 10: Oblivion Harvester
        Material[][] oblivionPattern = {
            {Material.NETHERITE_BLOCK, Material.WITHER_ROSE, Material.WITHER_ROSE, Material.WITHER_ROSE, Material.NETHERITE_BLOCK},
            {Material.WITHER_ROSE, Material.OBSIDIAN, Material.NETHER_STAR, Material.OBSIDIAN, Material.WITHER_ROSE},
            {Material.WITHER_ROSE, Material.NETHER_STAR, Material.NETHERITE_SWORD, Material.NETHER_STAR, Material.WITHER_ROSE},
            {Material.WITHER_ROSE, Material.OBSIDIAN, Material.NETHER_STAR, Material.OBSIDIAN, Material.WITHER_ROSE},
            {Material.NETHERITE_BLOCK, Material.WITHER_ROSE, Material.WITHER_ROSE, Material.WITHER_ROSE, Material.NETHERITE_BLOCK}
        };
        recipes.put(LegendaryType.OBLIVION_HARVESTER.getId(),
            new AltarRecipe(LegendaryType.OBLIVION_HARVESTER, oblivionPattern));

        // Recipe 11: Eclipse Devourer
        Material[][] eclipsePattern = {
            {Material.DRAGON_HEAD, Material.DRAGON_BREATH, Material.DRAGON_EGG, Material.DRAGON_BREATH, Material.DRAGON_HEAD},
            {Material.DRAGON_BREATH, Material.END_CRYSTAL, Material.ELYTRA, Material.END_CRYSTAL, Material.DRAGON_BREATH},
            {Material.DRAGON_EGG, Material.ELYTRA, Material.NETHERITE_SWORD, Material.ELYTRA, Material.DRAGON_EGG},
            {Material.DRAGON_BREATH, Material.END_CRYSTAL, Material.ELYTRA, Material.END_CRYSTAL, Material.DRAGON_BREATH},
            {Material.DRAGON_HEAD, Material.DRAGON_BREATH, Material.DRAGON_EGG, Material.DRAGON_BREATH, Material.DRAGON_HEAD}
        };
        recipes.put(LegendaryType.ECLIPSE_DEVOURER.getId(),
            new AltarRecipe(LegendaryType.ECLIPSE_DEVOURER, eclipsePattern));

        return recipes;
    }
}
