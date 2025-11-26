package com.alterSMP.legendaryweapons.altar;

import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;

/**
 * Represents a recipe ingredient that can be a simple material
 * or a special item like an enchanted book with a specific enchantment.
 *
 * Config format examples:
 * - DIAMOND_BLOCK (simple material)
 * - ENCHANTED_BOOK:FEATHER_FALLING:4 (enchanted book with specific enchant and level)
 * - ENCHANTED_BOOK:SHARPNESS:5
 */
public class RecipeIngredient {

    private final Material material;
    private final Enchantment requiredEnchant;
    private final int requiredLevel;

    public RecipeIngredient(Material material) {
        this.material = material;
        this.requiredEnchant = null;
        this.requiredLevel = 0;
    }

    public RecipeIngredient(Material material, Enchantment enchant, int level) {
        this.material = material;
        this.requiredEnchant = enchant;
        this.requiredLevel = level;
    }

    /**
     * Parse a recipe ingredient from config string.
     * Supports formats:
     * - MATERIAL_NAME
     * - ENCHANTED_BOOK:ENCHANT_NAME:LEVEL
     */
    public static RecipeIngredient parse(String input) {
        if (input == null || input.isEmpty()) {
            return new RecipeIngredient(Material.AIR);
        }

        String[] parts = input.split(":");

        if (parts.length == 1) {
            // Simple material
            try {
                return new RecipeIngredient(Material.valueOf(parts[0]));
            } catch (IllegalArgumentException e) {
                return new RecipeIngredient(Material.AIR);
            }
        } else if (parts.length == 3 && parts[0].equals("ENCHANTED_BOOK")) {
            // Enchanted book with specific enchantment
            try {
                Enchantment enchant = getEnchantmentByName(parts[1]);
                int level = Integer.parseInt(parts[2]);

                if (enchant != null) {
                    return new RecipeIngredient(Material.ENCHANTED_BOOK, enchant, level);
                }
            } catch (NumberFormatException e) {
                // Invalid level
            }
            // Fall back to just requiring any enchanted book
            return new RecipeIngredient(Material.ENCHANTED_BOOK);
        }

        // Unknown format, try as material
        try {
            return new RecipeIngredient(Material.valueOf(parts[0]));
        } catch (IllegalArgumentException e) {
            return new RecipeIngredient(Material.AIR);
        }
    }

    /**
     * Check if the given ItemStack matches this ingredient.
     */
    public boolean matches(ItemStack item) {
        if (material == Material.AIR) {
            return item == null || item.getType() == Material.AIR;
        }

        if (item == null || item.getType() != material) {
            return false;
        }

        // If we require a specific enchantment
        if (requiredEnchant != null && material == Material.ENCHANTED_BOOK) {
            if (!(item.getItemMeta() instanceof EnchantmentStorageMeta)) {
                return false;
            }

            EnchantmentStorageMeta meta = (EnchantmentStorageMeta) item.getItemMeta();
            int actualLevel = meta.getStoredEnchantLevel(requiredEnchant);

            // Must have at least the required level
            return actualLevel >= requiredLevel;
        }

        return true;
    }

    public Material getMaterial() {
        return material;
    }

    public Enchantment getRequiredEnchant() {
        return requiredEnchant;
    }

    public int getRequiredLevel() {
        return requiredLevel;
    }

    /**
     * Get enchantment by name (supports common names).
     */
    private static Enchantment getEnchantmentByName(String name) {
        // Direct registry lookup
        try {
            return Enchantment.getByName(name);
        } catch (Exception e) {
            // Continue to manual mapping
        }

        // Manual mapping for common enchantment names
        switch (name.toUpperCase()) {
            // Protection enchantments
            case "PROTECTION":
            case "PROTECTION_ENVIRONMENTAL":
                return Enchantment.PROTECTION;
            case "FIRE_PROTECTION":
            case "PROTECTION_FIRE":
                return Enchantment.FIRE_PROTECTION;
            case "FEATHER_FALLING":
            case "PROTECTION_FALL":
                return Enchantment.FEATHER_FALLING;
            case "BLAST_PROTECTION":
            case "PROTECTION_EXPLOSIONS":
                return Enchantment.BLAST_PROTECTION;
            case "PROJECTILE_PROTECTION":
            case "PROTECTION_PROJECTILE":
                return Enchantment.PROJECTILE_PROTECTION;
            case "RESPIRATION":
            case "OXYGEN":
                return Enchantment.RESPIRATION;
            case "AQUA_AFFINITY":
            case "WATER_WORKER":
                return Enchantment.AQUA_AFFINITY;
            case "THORNS":
                return Enchantment.THORNS;
            case "DEPTH_STRIDER":
                return Enchantment.DEPTH_STRIDER;
            case "FROST_WALKER":
                return Enchantment.FROST_WALKER;
            case "SOUL_SPEED":
                return Enchantment.SOUL_SPEED;
            case "SWIFT_SNEAK":
                return Enchantment.SWIFT_SNEAK;

            // Weapon enchantments
            case "SHARPNESS":
            case "DAMAGE_ALL":
                return Enchantment.SHARPNESS;
            case "SMITE":
            case "DAMAGE_UNDEAD":
                return Enchantment.SMITE;
            case "BANE_OF_ARTHROPODS":
            case "DAMAGE_ARTHROPODS":
                return Enchantment.BANE_OF_ARTHROPODS;
            case "KNOCKBACK":
                return Enchantment.KNOCKBACK;
            case "FIRE_ASPECT":
                return Enchantment.FIRE_ASPECT;
            case "LOOTING":
            case "LOOT_BONUS_MOBS":
                return Enchantment.LOOTING;
            case "SWEEPING":
            case "SWEEPING_EDGE":
                return Enchantment.SWEEPING_EDGE;

            // Tool enchantments
            case "EFFICIENCY":
            case "DIG_SPEED":
                return Enchantment.EFFICIENCY;
            case "SILK_TOUCH":
                return Enchantment.SILK_TOUCH;
            case "UNBREAKING":
            case "DURABILITY":
                return Enchantment.UNBREAKING;
            case "FORTUNE":
            case "LOOT_BONUS_BLOCKS":
                return Enchantment.FORTUNE;

            // Bow enchantments
            case "POWER":
            case "ARROW_DAMAGE":
                return Enchantment.POWER;
            case "PUNCH":
            case "ARROW_KNOCKBACK":
                return Enchantment.PUNCH;
            case "FLAME":
            case "ARROW_FIRE":
                return Enchantment.FLAME;
            case "INFINITY":
            case "ARROW_INFINITE":
                return Enchantment.INFINITY;

            // Fishing rod enchantments
            case "LUCK_OF_THE_SEA":
            case "LUCK":
                return Enchantment.LUCK_OF_THE_SEA;
            case "LURE":
                return Enchantment.LURE;

            // Trident enchantments
            case "LOYALTY":
                return Enchantment.LOYALTY;
            case "IMPALING":
                return Enchantment.IMPALING;
            case "RIPTIDE":
                return Enchantment.RIPTIDE;
            case "CHANNELING":
                return Enchantment.CHANNELING;

            // Crossbow enchantments
            case "MULTISHOT":
                return Enchantment.MULTISHOT;
            case "QUICK_CHARGE":
                return Enchantment.QUICK_CHARGE;
            case "PIERCING":
                return Enchantment.PIERCING;

            // Other
            case "MENDING":
                return Enchantment.MENDING;
            case "VANISHING_CURSE":
            case "CURSE_OF_VANISHING":
                return Enchantment.VANISHING_CURSE;
            case "BINDING_CURSE":
            case "CURSE_OF_BINDING":
                return Enchantment.BINDING_CURSE;

            default:
                return null;
        }
    }
}
