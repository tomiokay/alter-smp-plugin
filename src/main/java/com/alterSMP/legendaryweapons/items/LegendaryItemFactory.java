package com.alterSMP.legendaryweapons.items;

import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.Equippable;
import org.bukkit.ChatColor;
import org.bukkit.DyeColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Banner;
import org.bukkit.block.banner.Pattern;
import org.bukkit.block.banner.PatternType;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;


import java.util.ArrayList;
import java.util.List;

public class LegendaryItemFactory {

    private static final String LEGENDARY_KEY = "legendary_id";
    private static final String SOUL_COUNT_KEY = "soul_count";

    public ItemStack createLegendary(LegendaryType type) {
        ItemStack item = new ItemStack(type.getMaterial());
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            // Set display name with bold
            meta.setDisplayName(ChatColor.GOLD + "" + ChatColor.BOLD + type.getDisplayName());

            // Add lore
            List<String> lore = getLoreForLegendary(type);
            meta.setLore(lore);

            // Add legendary ID to PDC
            NamespacedKey key = new NamespacedKey("legendaryweapons", LEGENDARY_KEY);
            meta.getPersistentDataContainer().set(key, PersistentDataType.STRING, type.getId());

            // Set custom model data
            meta.setCustomModelData(type.getCustomModelData());

            // Add max enchantments based on type
            addMaxEnchantments(meta, type);
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);

            // Disable enchantment glint to improve FPS (glint shader is expensive on ARM/lower-end hardware)
            meta.setEnchantmentGlintOverride(false);

            // Make unbreakable
            meta.setUnbreakable(true);
            meta.addItemFlags(ItemFlag.HIDE_UNBREAKABLE);

            // Initialize soul count for Voidrender
            if (type == LegendaryType.SOUL_DEVOURER) {
                NamespacedKey soulKey = new NamespacedKey("legendaryweapons", SOUL_COUNT_KEY);
                meta.getPersistentDataContainer().set(soulKey, PersistentDataType.INTEGER, 0);
            }

            // Custom armor model requires Paper 1.21.5+ data component API
            // Commenting out for now until Paper version is updated
            // if (isArmorPiece(type)) {
            //     EquipmentSlot slot = getArmorSlot(type);
            //     if (slot != null) {
            //         Equippable equippable = Equippable.equippable(slot)
            //             .setModel(NamespacedKey.minecraft("legendary"))
            //             .build();
            //         meta.setData(DataComponentTypes.EQUIPPABLE, equippable);
            //     }
            // }

            item.setItemMeta(meta);

            // Apply yellow cross banner pattern to Celestial Aegis Shield
            if (type == LegendaryType.CELESTIAL_AEGIS_SHIELD) {
                applyShieldBannerPattern(item);
            }
        }

        return item;
    }

    // NEW: Helper method to check if legendary is an armor piece
    private boolean isArmorPiece(LegendaryType type) {
        return type == LegendaryType.COPPER_HELMET ||
               type == LegendaryType.COPPER_CHESTPLATE ||
               type == LegendaryType.COPPER_LEGGINGS ||
               type == LegendaryType.COPPER_BOOTS;
    }

    // NEW: Helper method to get the equipment slot for armor
    private EquipmentSlot getArmorSlot(LegendaryType type) {
        switch (type) {
            case COPPER_HELMET:
                return EquipmentSlot.HEAD;
            case COPPER_CHESTPLATE:
                return EquipmentSlot.CHEST;
            case COPPER_LEGGINGS:
                return EquipmentSlot.LEGS;
            case COPPER_BOOTS:
                return EquipmentSlot.FEET;
            default:
                return null;
        }
    }

    public ItemStack createForgeItem() {
        ItemStack item = new ItemStack(Material.CHISELED_STONE_BRICKS);
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            meta.setDisplayName(ChatColor.DARK_PURPLE + "" + ChatColor.BOLD + "Legendary Forge");

            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.GRAY + "A mystical forge used to craft");
            lore.add(ChatColor.GRAY + "legendary weapons with a 5x5 grid.");
            lore.add("");
            lore.add(ChatColor.YELLOW + "Place this block and right-click");
            lore.add(ChatColor.YELLOW + "to open the legendary crafting menu.");
            meta.setLore(lore);

            // Tag as forge item
            NamespacedKey key = new NamespacedKey("legendaryweapons", "legendary_forge_item");
            meta.getPersistentDataContainer().set(key, PersistentDataType.BOOLEAN, true);

            // Add glint
            meta.addEnchant(Enchantment.UNBREAKING, 1, true);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);

            item.setItemMeta(meta);
        }

        return item;
    }

    public static String getLegendaryId(ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return null;
        }

        ItemMeta meta = item.getItemMeta();
        NamespacedKey key = new NamespacedKey("legendaryweapons", LEGENDARY_KEY);

        if (meta.getPersistentDataContainer().has(key, PersistentDataType.STRING)) {
            return meta.getPersistentDataContainer().get(key, PersistentDataType.STRING);
        }

        return null;
    }

    public static boolean isForgeItem(ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return false;
        }

        ItemMeta meta = item.getItemMeta();
        NamespacedKey key = new NamespacedKey("legendaryweapons", "legendary_forge_item");

        return meta.getPersistentDataContainer().has(key, PersistentDataType.BOOLEAN);
    }

    public static int getSoulCount(ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return 0;
        }

        ItemMeta meta = item.getItemMeta();
        NamespacedKey key = new NamespacedKey("legendaryweapons", SOUL_COUNT_KEY);

        if (meta.getPersistentDataContainer().has(key, PersistentDataType.INTEGER)) {
            return meta.getPersistentDataContainer().get(key, PersistentDataType.INTEGER);
        }

        return 0;
    }

    public static void setSoulCount(ItemStack item, int count) {
        if (item == null || !item.hasItemMeta()) {
            return;
        }

        ItemMeta meta = item.getItemMeta();
        NamespacedKey key = new NamespacedKey("legendaryweapons", SOUL_COUNT_KEY);
        meta.getPersistentDataContainer().set(key, PersistentDataType.INTEGER, Math.min(count, 20));

        // Update lore
        List<String> lore = meta.getLore();
        if (lore != null && lore.size() > 0) {
            // Find and update soul count line
            for (int i = 0; i < lore.size(); i++) {
                if (lore.get(i).contains("Souls:")) {
                    lore.set(i, ChatColor.DARK_PURPLE + "Souls: " + ChatColor.LIGHT_PURPLE + count + "/20");
                    break;
                }
            }
            meta.setLore(lore);
        }

        item.setItemMeta(meta);
    }

    /**
     * Apply yellow cross banner pattern to the Celestial Aegis Shield.
     */
    private void applyShieldBannerPattern(ItemStack shield) {
        if (shield == null || shield.getType() != Material.SHIELD) {
            return;
        }

        BlockStateMeta blockStateMeta = (BlockStateMeta) shield.getItemMeta();
        if (blockStateMeta == null) {
            return;
        }

        Banner banner = (Banner) blockStateMeta.getBlockState();

        // Set base color to blue (celestial theme)
        banner.setBaseColor(DyeColor.BLUE);

        // Add yellow cross pattern
        banner.addPattern(new Pattern(DyeColor.YELLOW, PatternType.CROSS));

        // Apply the banner state back to the shield
        banner.update();
        blockStateMeta.setBlockState(banner);
        shield.setItemMeta(blockStateMeta);
    }

    private void addMaxEnchantments(ItemMeta meta, LegendaryType type) {
        switch (type) {
            // Swords
            case HOLY_MOONLIGHT_SWORD:
            case PHEONIX_GRACE:
            case THOUSAND_DEMON_DAGGERS:
            case CHRONO_BLADE:
            case SOUL_DEVOURER:
            case VOIDRENDER:
                meta.addEnchant(Enchantment.SHARPNESS, 5, true);
                meta.addEnchant(Enchantment.SWEEPING_EDGE, 3, true);
                meta.addEnchant(Enchantment.LOOTING, 3, true);
                break;

            // Trident
            case TEMPESTBREAKER_SPEAR:
                meta.addEnchant(Enchantment.SHARPNESS, 5, true);
                meta.addEnchant(Enchantment.IMPALING, 5, true);
                meta.addEnchant(Enchantment.LOYALTY, 3, true);
                break;

            // Axe
            case DIVINE_AXE_RHITTA:
                meta.addEnchant(Enchantment.SHARPNESS, 5, true);
                meta.addEnchant(Enchantment.EFFICIENCY, 5, true);
                break;

            // Shovel (Chains of Eternity)
            case CHAINS_OF_ETERNITY:
                meta.addEnchant(Enchantment.SHARPNESS, 5, true);
                break;

            // Boots
            case COPPER_BOOTS:
                meta.addEnchant(Enchantment.PROTECTION, 4, true);
                meta.addEnchant(Enchantment.FEATHER_FALLING, 4, true);
                meta.addEnchant(Enchantment.DEPTH_STRIDER, 3, true);
                break;

            // Leggings
            case COPPER_LEGGINGS:
                meta.addEnchant(Enchantment.PROTECTION, 4, true);
                meta.addEnchant(Enchantment.FIRE_PROTECTION, 4, true);
                break;

            // Chestplate
            case COPPER_CHESTPLATE:
                meta.addEnchant(Enchantment.PROTECTION, 4, true);
                break;

            // Helmet
            case COPPER_HELMET:
                meta.addEnchant(Enchantment.PROTECTION, 4, true);
                meta.addEnchant(Enchantment.RESPIRATION, 4, true);
                meta.addEnchant(Enchantment.AQUA_AFFINITY, 1, true);
                break;

            // Pickaxe
            case COPPER_PICKAXE:
                meta.addEnchant(Enchantment.EFFICIENCY, 5, true);
                // Silk Touch/Fortune toggle handled by ability
                break;

            // Shield
            case CELESTIAL_AEGIS_SHIELD:
                meta.addEnchant(Enchantment.MENDING, 1, true);
                break;
        }
    }

    private List<String> getLoreForLegendary(LegendaryType type) {
        List<String> lore = new ArrayList<>();
        lore.add("");

        switch (type) {
            case HOLY_MOONLIGHT_SWORD:
                lore.add(ChatColor.AQUA + "Passive: " + ChatColor.WHITE + "Flashburst Counter");
                lore.add(ChatColor.GRAY + "  Every 20 hits blinds nearby enemies");
                lore.add("");
                lore.add(ChatColor.GREEN + "Ability 1: " + ChatColor.WHITE + "Star Rift Slash (25s)");
                lore.add(ChatColor.GRAY + "  30-block beam through walls");
                lore.add(ChatColor.GREEN + "Ability 2: " + ChatColor.WHITE + "Stargate Blink (45s)");
                lore.add(ChatColor.GRAY + "  Teleport up to 45 blocks");
                break;

            case PHEONIX_GRACE:
                lore.add(ChatColor.AQUA + "Passive: " + ChatColor.WHITE + "Heat Shield");
                lore.add(ChatColor.GRAY + "  Immune to fire and explosions");
                lore.add("");
                lore.add(ChatColor.GREEN + "Ability 1: " + ChatColor.WHITE + "Flame Harvest (90s)");
                lore.add(ChatColor.GRAY + "  Deal 40% HP damage to nearby enemies");
                lore.add(ChatColor.GRAY + "  Gain absorption hearts per enemy hit");
                lore.add(ChatColor.GREEN + "Ability 2: " + ChatColor.WHITE + "Fire Rebirth (300s)");
                lore.add(ChatColor.GRAY + "  Cheat death for 10 seconds");
                break;

            case TEMPESTBREAKER_SPEAR:
                lore.add(ChatColor.AQUA + "Passive: " + ChatColor.WHITE + "Storm's Fury");
                lore.add(ChatColor.GRAY + "  Trident hits strike lightning (1 heart)");
                lore.add("");
                lore.add(ChatColor.GREEN + "Ability 1: " + ChatColor.WHITE + "Gale Throw (25s)");
                lore.add(ChatColor.GRAY + "  Next throw creates wind vortex");
                lore.add(ChatColor.GREEN + "Ability 2: " + ChatColor.WHITE + "Stormcall (50s)");
                lore.add(ChatColor.GRAY + "  8-block lightning storm for 2s");
                break;

            case THOUSAND_DEMON_DAGGERS:
                lore.add(ChatColor.AQUA + "Passive: " + ChatColor.WHITE + "Shadow Presence");
                lore.add(ChatColor.GRAY + "  Speed III while sneaking");
                lore.add("");
                lore.add(ChatColor.GREEN + "Ability 1: " + ChatColor.WHITE + "Shadowstep (20s)");
                lore.add(ChatColor.GRAY + "  Teleport behind target enemy");
                lore.add(ChatColor.GRAY + "  Next attack deals +1 heart true damage");
                lore.add(ChatColor.GREEN + "Ability 2: " + ChatColor.WHITE + "Soul Mark (60s)");
                lore.add(ChatColor.GRAY + "  Mark target for +4 hearts true damage");
                lore.add(ChatColor.GRAY + "  on each hit for 15 seconds");
                break;

            case DIVINE_AXE_RHITTA:
                lore.add(ChatColor.AQUA + "Passive: " + ChatColor.WHITE + "Nature Channel");
                lore.add(ChatColor.GRAY + "  Regen III on grass/logs/leaves");
                lore.add("");
                lore.add(ChatColor.GREEN + "Ability 1: " + ChatColor.WHITE + "Nature Grasp (35s)");
                lore.add(ChatColor.GRAY + "  Root enemies in 6-block radius");
                lore.add(ChatColor.GREEN + "Ability 2: " + ChatColor.WHITE + "Verdant Cyclone (70s)");
                lore.add(ChatColor.GRAY + "  360° spin attack with knockback");
                break;

            case CHAINS_OF_ETERNITY:
                lore.add(ChatColor.AQUA + "Passive: " + ChatColor.WHITE + "Soul Links");
                lore.add(ChatColor.GRAY + "  Every 5th hit immobilizes target");
                lore.add("");
                lore.add(ChatColor.GREEN + "Ability 1: " + ChatColor.WHITE + "Soul Bind (35s)");
                lore.add(ChatColor.GRAY + "  Pull target, deal damage, and slow");
                lore.add(ChatColor.GREEN + "Ability 2: " + ChatColor.WHITE + "Prison of the Damned (65s)");
                lore.add(ChatColor.GRAY + "  Cage target in iron bars for 5s");
                break;

            case COPPER_BOOTS:
                lore.add(ChatColor.AQUA + "Passive: " + ChatColor.WHITE + "Featherfall");
                lore.add(ChatColor.GRAY + "  No fall damage + Speed II");
                lore.add("");
                lore.add(ChatColor.GREEN + "Ability: " + ChatColor.WHITE + "Meteor Slam");
                lore.add(ChatColor.GRAY + "  Shift mid-air to slam down");
                lore.add(ChatColor.GRAY + "  Mace-like damage in 4-block radius");
                break;

            case CELESTIAL_AEGIS_SHIELD:
                lore.add(ChatColor.AQUA + "Passive: " + ChatColor.WHITE + "Aura of Protection");
                lore.add(ChatColor.GRAY + "  You and trusted allies gain Resistance I");
                lore.add("");
                lore.add(ChatColor.GREEN + "Ability 1: " + ChatColor.WHITE + "Radiant Block (40s)");
                lore.add(ChatColor.GRAY + "  Reflect 75% damage for 5s");
                lore.add(ChatColor.GREEN + "Ability 2: " + ChatColor.WHITE + "Heaven's Wall (90s)");
                lore.add(ChatColor.GRAY + "  16x16 barrier for 32s");
                lore.add(ChatColor.GRAY + "  Only trusted players can pass");
                break;

            case CHRONO_BLADE:
                lore.add(ChatColor.AQUA + "Passive: " + ChatColor.WHITE + "Time Slow");
                lore.add(ChatColor.GRAY + "  First hit on each enemy slows them");
                lore.add("");
                lore.add(ChatColor.GREEN + "Ability 1: " + ChatColor.WHITE + "Time Distortion (40s)");
                lore.add(ChatColor.GRAY + "  6-block bubble freezes enemies for 3s");
                lore.add(ChatColor.GRAY + "  Then deals 4 hearts true damage");
                lore.add(ChatColor.GREEN + "Ability 2: " + ChatColor.WHITE + "Chrono Shift (120s)");
                lore.add(ChatColor.GRAY + "  Mark position, re-cast to return");
                lore.add(ChatColor.GRAY + "  Clears debuffs, grants Speed II");
                break;

            case SOUL_DEVOURER:
                lore.add(ChatColor.AQUA + "Passive: " + ChatColor.WHITE + "Soul Collector");
                lore.add(ChatColor.GRAY + "  +2 damage per player kill (max 5)");
                lore.add(ChatColor.DARK_PURPLE + "Souls: " + ChatColor.LIGHT_PURPLE + "0/5");
                lore.add("");
                lore.add(ChatColor.GREEN + "Ability 1: " + ChatColor.WHITE + "Void Slice (30s)");
                lore.add(ChatColor.GRAY + "  8-block horizontal purple arc");
                lore.add(ChatColor.GREEN + "Ability 2: " + ChatColor.WHITE + "Void Rift (85s)");
                lore.add(ChatColor.GRAY + "  Black hole that pulls and damages");
                break;

            case VOIDRENDER:
                lore.add(ChatColor.AQUA + "Passive: " + ChatColor.WHITE + "Dragon's Gaze");
                lore.add(ChatColor.GRAY + "  Nearby enemies glow");
                lore.add("");
                lore.add(ChatColor.GREEN + "Ability 1: " + ChatColor.WHITE + "End Sever (18s)");
                lore.add(ChatColor.GRAY + "  7-block cone, 2 hearts true damage");
                lore.add(ChatColor.GRAY + "  Applies blindness");
                lore.add(ChatColor.GREEN + "Ability 2: " + ChatColor.WHITE + "Genesis Collapse (120s)");
                lore.add(ChatColor.GRAY + "  10-block explosion, 5 hearts true damage");
                break;

            case COPPER_PICKAXE:
                lore.add(ChatColor.AQUA + "Passive: " + ChatColor.WHITE + "None");
                lore.add("");
                lore.add(ChatColor.GREEN + "Ability 1: " + ChatColor.WHITE + "3x3 Mining Toggle");
                lore.add(ChatColor.GRAY + "  Toggle 3x3 area mining");
                lore.add(ChatColor.GREEN + "Ability 2: " + ChatColor.WHITE + "Enchant Switch");
                lore.add(ChatColor.GRAY + "  Toggle Silk Touch/Fortune III");
                break;

            case COPPER_CHESTPLATE:
                lore.add(ChatColor.AQUA + "Passive: " + ChatColor.WHITE + "Storm Strike");
                lore.add(ChatColor.GRAY + "  Every 10 hits: lightning storm");
                lore.add(ChatColor.GRAY + "  Deals 2.5 hearts through prot 4");
                break;

            case COPPER_LEGGINGS:
                lore.add(ChatColor.AQUA + "Passive: " + ChatColor.WHITE + "Flamebound Feet");
                lore.add(ChatColor.GRAY + "  Immune to fire, lava, and magma");
                lore.add(ChatColor.GRAY + "  Walking leaves damaging flame trails");
                lore.add(ChatColor.GRAY + "  Haste I above 50% HP");
                lore.add(ChatColor.GRAY + "  Super speed in lava");
                break;

            case COPPER_HELMET:
                lore.add(ChatColor.AQUA + "Passive: " + ChatColor.WHITE + "Blood Harvest");
                lore.add(ChatColor.GRAY + "  Player kills grant +5 hearts (5min)");
                lore.add(ChatColor.AQUA + "Passive: " + ChatColor.WHITE + "Critical Rush");
                lore.add(ChatColor.GRAY + "  Crits grant Speed I (3s)");
                lore.add(ChatColor.AQUA + "Passive: " + ChatColor.WHITE + "Water Mobility");
                lore.add(ChatColor.GRAY + "  Dolphin's Grace + Conduit Power");
                break;

            case LANTERN_OF_LOST_NAMES:
                lore.add(ChatColor.AQUA + "Passive: " + ChatColor.WHITE + "Phantom Veil");
                lore.add(ChatColor.GRAY + "  Invisible to players you've never killed");
                lore.add(ChatColor.GRAY + "  They cannot see you until you kill them once");
                lore.add(ChatColor.GRAY + "  Deactivates for 5 min after attacking");
                lore.add("");
                lore.add(ChatColor.YELLOW + "Hold in offhand for effect");
                break;

            case RIFT_KEY_OF_ENDKEEPER:
                lore.add(ChatColor.GREEN + "Ability: " + ChatColor.WHITE + "End Rift (24h cooldown)");
                lore.add(ChatColor.GRAY + "  Open a portal to ANY coordinates");
                lore.add(ChatColor.GRAY + "  Rift stays open for 30 seconds");
                lore.add(ChatColor.GRAY + "  Teammates can follow through");
                lore.add("");
                lore.add(ChatColor.YELLOW + "Usage: /ability 1 <x> <y> <z>");
                break;

            case CHAOS_DICE_OF_FATE:
                lore.add(ChatColor.GREEN + "Ability 1: " + ChatColor.WHITE + "Roll Dice (30min)");
                lore.add(ChatColor.GRAY + "  Random effect:");
                lore.add(ChatColor.GRAY + "  - +5 hearts (15 min)");
                lore.add(ChatColor.GRAY + "  - Summon 5 iron golems");
                lore.add(ChatColor.GRAY + "  - Speed III + Strength III (10 min)");
                lore.add(ChatColor.GRAY + "  - Jumble opponent's hotbar");
                lore.add(ChatColor.GRAY + "  - Free player scans (20 min)");
                lore.add(ChatColor.GRAY + "  - Insta-crit (15 min)");
                lore.add(ChatColor.GRAY + "  - Resistance II (5 min)");
                lore.add("");
                lore.add(ChatColor.GREEN + "Ability 2: " + ChatColor.WHITE + "Player Scan (10s)");
                lore.add(ChatColor.GRAY + "  Show all player locations + coords");
                break;
        }

        lore.add("");
        lore.add(ChatColor.GOLD + "" + ChatColor.ITALIC + "Legendary Weapon");

        return lore;
    }
}
