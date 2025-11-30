package com.alterSMP.legendaryweapons.items;

import org.bukkit.Material;

public enum LegendaryType {
    // Weapons with CustomModelData
    HOLY_MOONLIGHT_SWORD("holy_moonlight_sword", "Holy Moonlight Sword", Material.DIAMOND_SWORD, 1),
    PHEONIX_GRACE("pheonix_grace", "Pheonix Grace", Material.DIAMOND_SWORD, 2),
    THOUSAND_DEMON_DAGGERS("thousand_demon_daggers", "Thousand Demon Daggers", Material.DIAMOND_SWORD, 3),
    CHRONO_BLADE("chrono_blade", "Chrono Blade", Material.DIAMOND_SWORD, 4),
    SOUL_DEVOURER("soul_devourer", "Soul Devourer", Material.DIAMOND_SWORD, 5),
    CREATION_SPLITTER("creation_splitter", "Creation Splitter", Material.DIAMOND_SWORD, 6),
    TEMPESTBREAKER_SPEAR("tempestbreaker_spear", "Tempestbreaker Spear", Material.TRIDENT, 7),
    DIVINE_AXE_RHITTA("divine_axe_rhitta", "Divine Axe Rhitta", Material.DIAMOND_AXE, 8),
    CHAINS_OF_ETERNITY("chains_of_eternity", "Chains of Eternity", Material.WOODEN_SHOVEL, 9),
    
    // Armor (no custom models, just for completeness)
    SKYBREAKER_BOOTS("skybreaker_boots", "Skybreaker Boots", Material.DIAMOND_BOOTS, 10),
    CELESTIAL_AEGIS_SHIELD("celestial_aegis_shield", "Celestial Aegis Shield", Material.SHIELD, 11),
    COPPER_PICKAXE("copper_pickaxe", "Copper Pickaxe", Material.NETHERITE_PICKAXE, 12),
    THUNDERFORGE_CHESTPLATE("thunderforge_chestplate", "Thunderforge Chestplate", Material.DIAMOND_CHESTPLATE, 13),
    EMBERSTRIDE_GREAVES("emberstride_greaves", "Emberstride Greaves", Material.DIAMOND_LEGGINGS, 14),
    BLOODREAPER_HOOD("bloodreaper_hood", "Bloodreaper Hood", Material.DIAMOND_HELMET, 15);

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