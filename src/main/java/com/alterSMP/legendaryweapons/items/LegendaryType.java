package com.alterSMP.legendaryweapons.items;

import org.bukkit.Material;

public enum LegendaryType {
    BLADE_OF_FRACTURED_STARS("blade_of_the_fractured_stars", "Blade of the Fractured Stars", Material.NETHERITE_SWORD),
    EMBERHEART_SCYTHE("emberheart_scythe", "Emberheart Scythe", Material.NETHERITE_SWORD),
    TEMPESTBREAKER_SPEAR("tempestbreaker_spear", "Tempestbreaker Spear", Material.TRIDENT),
    UMBRA_VEIL_DAGGER("umbra_veil_dagger", "Umbra Veil Dagger", Material.NETHERITE_SWORD),
    HEARTROOT_GUARDIAN_AXE("heartroot_guardian_axe", "Heartroot Guardian Axe", Material.NETHERITE_AXE),
    CHAINS_OF_ETERNITY("chains_of_eternity", "Chains of Eternity", Material.WOODEN_SHOVEL),
    SKYBREAKER_BOOTS("skybreaker_boots", "Skybreaker Boots", Material.DIAMOND_BOOTS),
    CELESTIAL_AEGIS_SHIELD("celestial_aegis_shield", "Celestial Aegis Shield", Material.SHIELD),
    CHRONO_EDGE("chrono_edge", "Chrono Edge", Material.NETHERITE_SWORD),
    OBLIVION_HARVESTER("oblivion_harvester", "Oblivion Harvester", Material.NETHERITE_SWORD),
    ECLIPSE_DEVOURER("eclipse_devourer", "Eclipse Devourer", Material.NETHERITE_SWORD),
    COPPER_PICKAXE("copper_pickaxe", "Copper Pickaxe", Material.NETHERITE_PICKAXE),
    THUNDERFORGE_CHESTPLATE("thunderforge_chestplate", "Thunderforge Chestplate", Material.DIAMOND_CHESTPLATE),
    IONFLARE_LEGGINGS("ionflare_leggings", "Ionflare Leggings", Material.DIAMOND_LEGGINGS),
    BLOODREAPER_HOOD("bloodreaper_hood", "Bloodreaper Hood", Material.DIAMOND_HELMET);

    private final String id;
    private final String displayName;
    private final Material material;

    LegendaryType(String id, String displayName, Material material) {
        this.id = id;
        this.displayName = displayName;
        this.material = material;
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

    public static LegendaryType fromId(String id) {
        for (LegendaryType type : values()) {
            if (type.getId().equals(id)) {
                return type;
            }
        }
        return null;
    }
}
