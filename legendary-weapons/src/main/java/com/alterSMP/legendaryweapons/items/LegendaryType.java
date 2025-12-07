package com.alterSMP.legendaryweapons.items;

import org.bukkit.Material;

public enum LegendaryType {
    // Weapons with CustomModelData
    HOLY_MOONLIGHT_SWORD("holy_moonlight_sword", "Holy Moonlight Sword", Material.DIAMOND_SWORD, 1),
    PHEONIX_GRACE("pheonix_grace", "Pheonix Grace", Material.DIAMOND_SWORD, 2),
    THOUSAND_DEMON_DAGGERS("thousand_demon_daggers", "Thousand Demon Daggers", Material.DIAMOND_SWORD, 3),
    CHRONO_BLADE("chrono_blade", "Chrono Blade", Material.DIAMOND_SWORD, 4),
    SOUL_DEVOURER("soul_devourer", "Soul Devourer", Material.DIAMOND_SWORD, 5),
    DRAGONBORN_BLADE("dragonborn_blade", "Dragonborn Blade", Material.DIAMOND_SWORD, 6),
    TEMPESTBREAKER_SPEAR("tempestbreaker_spear", "Tempestbreaker Spear", Material.TRIDENT, 7),
    DIVINE_AXE_RHITTA("divine_axe_rhitta", "Divine Axe Rhitta", Material.DIAMOND_AXE, 8),
    CHAINS_OF_ETERNITY("chains_of_eternity", "Chains of Eternity", Material.WOODEN_SHOVEL, 9),
    
    // Armor (no custom models, just for completeness)
    COPPER_BOOTS("copper_boots", "Copper Boots", Material.DIAMOND_BOOTS, 10),
    CELESTIAL_AEGIS_SHIELD("celestial_aegis_shield", "Celestial Aegis Shield", Material.SHIELD, 11),
    FORGE_PICKAXE("forge_pickaxe", "Forge Pickaxe", Material.NETHERITE_PICKAXE, 12),
    COPPER_CHESTPLATE("copper_chestplate", "Copper Chestplate", Material.DIAMOND_CHESTPLATE, 13),
    COPPER_LEGGINGS("copper_leggings", "Copper Leggings", Material.DIAMOND_LEGGINGS, 14),
    COPPER_HELMET("copper_helmet", "Copper Helmet", Material.DIAMOND_HELMET, 15),

    // New Legendary Items
    LANTERN_OF_LOST_NAMES("lantern_of_lost_names", "Lantern of Lost Names", Material.SOUL_LANTERN, 16),
    RIFT_KEY_OF_ENDKEEPER("rift_key_of_endkeeper", "Rift Key of the Endkeeper", Material.TRIPWIRE_HOOK, 17),
    CHAOS_DICE_OF_FATE("chaos_dice_of_fate", "Chaos Dice of Fate", Material.AMETHYST_SHARD, 18);

    private final String id;
    private final String displayName;
    private final Material material;
    private final int customModelData;

    LegendaryType(String id, String displayName, Material material, int customModelData) {
        this.id = id;
        this.displayName = displayName;
        this.material = material;
        this.customModelData = customModelData;
    }

    public String getId() {
        return id;
    }

    public String getDisplayName() {
        return displayName;
    }

    public Material getMaterial() {
        return material;
    }

    public int getCustomModelData() {
        return customModelData;
    }

    public static LegendaryType fromId(String id) {
        for (LegendaryType type : values()) {
            if (type.getId().equals(id)) {
                return type;
            }
        }
        return null;
    }

    /**
     * Check if a display name matches any legendary's canonical name.
     * Used to prevent normal items from being renamed to legendary names.
     */
    public static boolean isProtectedName(String name) {
        if (name == null) return false;
        String stripped = org.bukkit.ChatColor.stripColor(name);
        for (LegendaryType type : values()) {
            if (type.getDisplayName().equals(stripped)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Get the LegendaryType that has a specific display name.
     */
    public static LegendaryType fromDisplayName(String name) {
        if (name == null) return null;
        String stripped = org.bukkit.ChatColor.stripColor(name);
        for (LegendaryType type : values()) {
            if (type.getDisplayName().equals(stripped)) {
                return type;
            }
        }
        return null;
    }
}