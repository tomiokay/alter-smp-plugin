package com.alterSMP.legendaryweapons.items;

import org.bukkit.ChatColor;
import org.bukkit.DyeColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Banner;
import org.bukkit.block.banner.Pattern;
import org.bukkit.block.banner.PatternType;
import org.bukkit.enchantments.Enchantment;
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

            // Add max enchantments based on type
            addMaxEnchantments(meta, type);
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);

            // Make unbreakable
            meta.setUnbreakable(true);
            meta.addItemFlags(ItemFlag.HIDE_UNBREAKABLE);

            // Initialize soul count for Soul Devourer
            if (type == LegendaryType.SOUL_DEVOURER) {
                NamespacedKey soulKey = new NamespacedKey("legendaryweapons", SOUL_COUNT_KEY);
                meta.getPersistentDataContainer().set(soulKey, PersistentDataType.INTEGER, 0);
            }

            item.setItemMeta(meta);

            // Apply yellow cross banner pattern to Celestial Aegis Shield
            if (type == LegendaryType.CELESTIAL_AEGIS_SHIELD) {
                applyShieldBannerPattern(item);
            }
        }

        return item;
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
        meta.getPersistentDataContainer().set(key, PersistentDataType.INTEGER, Math.min(count, 5));

        // Update lore
        List<String> lore = meta.getLore();
        if (lore != null && lore.size() > 0) {
            // Find and update soul count line
            for (int i = 0; i < lore.size(); i++) {
                if (lore.get(i).contains("Souls:")) {
                    lore.set(i, ChatColor.DARK_PURPLE + "Souls: " + ChatColor.LIGHT_PURPLE + count + "/5");
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
        // Add some additional celestial flair
        banner.addPattern(new Pattern(DyeColor.LIGHT_BLUE, PatternType.BORDER));

        banner.update();
        blockStateMeta.setBlockState(banner);

        // Re-apply the legendary metadata that was set before
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
            case CREATION_SPLITTER:
                meta.addEnchant(Enchantment.SHARPNESS, 5, true);
                meta.addEnchant(Enchantment.LOOTING, 3, true);
                meta.addEnchant(Enchantment.SWEEPING_EDGE, 3, true);
                meta.addEnchant(Enchantment.FIRE_ASPECT, 2, true);
                break;

            // Axe
            case DIVINE_AXE_RHITTA:
                meta.addEnchant(Enchantment.SHARPNESS, 5, true);
                meta.addEnchant(Enchantment.EFFICIENCY, 5, true);
                meta.addEnchant(Enchantment.SILK_TOUCH, 1, true);
                break;

            // Trident
            case TEMPESTBREAKER_SPEAR:
                meta.addEnchant(Enchantment.LOYALTY, 3, true);
                meta.addEnchant(Enchantment.IMPALING, 5, true);
                break;

            // Shovel
            case CHAINS_OF_ETERNITY:
                meta.addEnchant(Enchantment.SHARPNESS, 5, true);
                meta.addEnchant(Enchantment.EFFICIENCY, 5, true);
                meta.addEnchant(Enchantment.FORTUNE, 3, true);
                break;

            // Boots
            case SKYBREAKER_BOOTS:
                meta.addEnchant(Enchantment.PROTECTION, 4, true);
                meta.addEnchant(Enchantment.FEATHER_FALLING, 4, true);
                meta.addEnchant(Enchantment.DEPTH_STRIDER, 3, true);
                meta.addEnchant(Enchantment.SOUL_SPEED, 3, true);
                break;

            // Leggings
            case EMBERSTRIDE_GREAVES:
                meta.addEnchant(Enchantment.PROTECTION, 4, true);
                meta.addEnchant(Enchantment.SWIFT_SNEAK, 3, true);
                break;

            // Chestplate
            case THUNDERFORGE_CHESTPLATE:
                meta.addEnchant(Enchantment.PROTECTION, 4, true);
                break;

            // Helmet
            case BLOODREAPER_HOOD:
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
                lore.add(ChatColor.GRAY + "  Beam attack through walls");
                lore.add(ChatColor.GREEN + "Ability 2: " + ChatColor.WHITE + "Stargate Blink (45s)");
                lore.add(ChatColor.GRAY + "  Teleport up to 45 blocks");
                break;

            case PHEONIX_GRACE:
                lore.add(ChatColor.AQUA + "Passive: " + ChatColor.WHITE + "Heat Shield");
                lore.add(ChatColor.GRAY + "  Immune to fire and explosions");
                lore.add("");
                lore.add(ChatColor.GREEN + "Ability 1: " + ChatColor.WHITE + "Flame Harvest (1m30s)");
                lore.add(ChatColor.GRAY + "  Fiery explosion, +6 absorption per enemy");
                lore.add(ChatColor.GRAY + "  Max 18 absorption hearts");
                lore.add(ChatColor.GREEN + "Ability 2: " + ChatColor.WHITE + "Fire Rebirth (5min)");
                lore.add(ChatColor.GRAY + "  Cheat death with flames");
                break;

            case TEMPESTBREAKER_SPEAR:
                lore.add(ChatColor.AQUA + "Passive: " + ChatColor.WHITE + "Windwalker");
                lore.add(ChatColor.GRAY + "  Water mobility buffs");
                lore.add("");
                lore.add(ChatColor.GREEN + "Ability 1: " + ChatColor.WHITE + "Gale Throw (25s)");
                lore.add(ChatColor.GRAY + "  Wind vortex on impact");
                lore.add(ChatColor.GREEN + "Ability 2: " + ChatColor.WHITE + "Stormcall (50s)");
                lore.add(ChatColor.GRAY + "  8-block lightning storm radius");
                break;

            case THOUSAND_DEMON_DAGGERS:
                lore.add(ChatColor.AQUA + "Passive: " + ChatColor.WHITE + "Shadow Presence");
                lore.add(ChatColor.GRAY + "  Speed III while sneaking");
                lore.add("");
                lore.add(ChatColor.GREEN + "Ability 1: " + ChatColor.WHITE + "Shadowstep (20s)");
                lore.add(ChatColor.GRAY + "  Teleport behind target enemy");
                lore.add(ChatColor.GRAY + "  Next attack deals +1 heart true damage");
                lore.add(ChatColor.GREEN + "Ability 2: " + ChatColor.WHITE + "Soul Mark (60s)");
                lore.add(ChatColor.GRAY + "  Mark target, next hit deals +4 hearts true damage");
                break;

            case DIVINE_AXE_RHITTA:
                lore.add(ChatColor.AQUA + "Passive: " + ChatColor.WHITE + "Nature Channel");
                lore.add(ChatColor.GRAY + "  Regen III on natural blocks");
                lore.add("");
                lore.add(ChatColor.GREEN + "Ability 1: " + ChatColor.WHITE + "Nature Grasp (35s)");
                lore.add(ChatColor.GRAY + "  Root enemies in place");
                lore.add(ChatColor.GREEN + "Ability 2: " + ChatColor.WHITE + "Verdant Cyclone (70s)");
                lore.add(ChatColor.GRAY + "  360° spin attack, 2 hearts damage");
                break;

            case CHAINS_OF_ETERNITY:
                lore.add(ChatColor.AQUA + "Passive: " + ChatColor.WHITE + "Soul Links");
                lore.add(ChatColor.GRAY + "  Every 5th hit immobilizes");
                lore.add("");
                lore.add(ChatColor.GREEN + "Ability 1: " + ChatColor.WHITE + "Soul Bind (35s)");
                lore.add(ChatColor.GRAY + "  Pull and slow target");
                lore.add(ChatColor.GREEN + "Ability 2: " + ChatColor.WHITE + "Prison of the Damned (65s)");
                lore.add(ChatColor.GRAY + "  Cage target in iron bars");
                break;

            case SKYBREAKER_BOOTS:
                lore.add(ChatColor.AQUA + "Passive: " + ChatColor.WHITE + "Featherfall");
                lore.add(ChatColor.GRAY + "  No fall damage");
                lore.add("");
                lore.add(ChatColor.GREEN + "Ability: " + ChatColor.WHITE + "Meteor Slam");
                lore.add(ChatColor.GRAY + "  Shift mid-air to slam down");
                lore.add(ChatColor.GRAY + "  Damage based on fall distance");
                break;

            case CELESTIAL_AEGIS_SHIELD:
                lore.add(ChatColor.AQUA + "Passive: " + ChatColor.WHITE + "Aura of Protection");
                lore.add(ChatColor.GRAY + "  You and trusted allies gain Resistance I");
                lore.add("");
                lore.add(ChatColor.GREEN + "Ability 1: " + ChatColor.WHITE + "Radiant Block (35s)");
                lore.add(ChatColor.GRAY + "  Reflect 75% damage for 5s");
                lore.add(ChatColor.GREEN + "Ability 2: " + ChatColor.WHITE + "Heaven's Wall (90s)");
                lore.add(ChatColor.GRAY + "  16x16 barrier for 32s");
                lore.add(ChatColor.GRAY + "  Only trusted players can pass");
                break;

            case CHRONO_BLADE:
                lore.add(ChatColor.AQUA + "Passive: " + ChatColor.WHITE + "Time Slow");
                lore.add(ChatColor.GRAY + "  First hit slows target (20s CD/target)");
                lore.add(ChatColor.GRAY + "  -20% move/attack speed for 3s");
                lore.add("");
                lore.add(ChatColor.GREEN + "Ability 1: " + ChatColor.WHITE + "Time Distortion (40s)");
                lore.add(ChatColor.GRAY + "  6-block slow bubble for 3s");
                lore.add(ChatColor.GRAY + "  Enemies at 20% speed, 4 true dmg on end");
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
                lore.add(ChatColor.GRAY + "  Sweeping void attack");
                lore.add(ChatColor.GREEN + "Ability 2: " + ChatColor.WHITE + "Void Rift (85s)");
                lore.add(ChatColor.GRAY + "  Create damaging black hole");
                break;

            case CREATION_SPLITTER:
                lore.add(ChatColor.AQUA + "Passive: " + ChatColor.WHITE + "Endbound Soulkeeper");
                lore.add(ChatColor.GRAY + "  Kill players to steal hearts (+1 max)");
                lore.add(ChatColor.GRAY + "  Max +5 hearts, unique per victim");
                lore.add(ChatColor.GRAY + "  Death returns all stolen hearts");
                lore.add("");
                lore.add(ChatColor.GREEN + "Ability 1: " + ChatColor.WHITE + "End Sever (18s)");
                lore.add(ChatColor.GRAY + "  7-block cone slash, 2 true damage");
                lore.add(ChatColor.GRAY + "  Ender Decay + Levitation, recoil back");
                lore.add(ChatColor.GREEN + "Ability 2: " + ChatColor.WHITE + "Genesis Collapse (2min)");
                lore.add(ChatColor.GRAY + "  Pull enemies, 5 true damage, shield");
                break;

            case COPPER_PICKAXE:
                lore.add(ChatColor.AQUA + "Passive: " + ChatColor.WHITE + "None");
                lore.add("");
                lore.add(ChatColor.GREEN + "Ability 1: " + ChatColor.WHITE + "3x3 Mining Toggle");
                lore.add(ChatColor.GRAY + "  Toggle 3x3 area mining");
                lore.add(ChatColor.GREEN + "Ability 2: " + ChatColor.WHITE + "Enchant Switch");
                lore.add(ChatColor.GRAY + "  Toggle Silk Touch/Fortune III");
                break;

            case THUNDERFORGE_CHESTPLATE:
                lore.add(ChatColor.AQUA + "Passive: " + ChatColor.WHITE + "Thunder Strike");
                lore.add(ChatColor.GRAY + "  Every 10 melee hits: lightning strike");
                lore.add(ChatColor.GRAY + "  Deals 3 hearts true damage");
                break;

            case EMBERSTRIDE_GREAVES:
                lore.add(ChatColor.AQUA + "Passive: " + ChatColor.WHITE + "Flamebound Feet");
                lore.add(ChatColor.GRAY + "  Immune to fire, lava, and magma");
                lore.add(ChatColor.GRAY + "  Flame trail damages enemies");
                lore.add(ChatColor.GRAY + "  +10% attack speed above 50% HP");
                lore.add(ChatColor.GRAY + "  +500% movement speed in lava");
                break;

            case BLOODREAPER_HOOD:
                lore.add(ChatColor.AQUA + "Passive: " + ChatColor.WHITE + "Blood Harvest");
                lore.add(ChatColor.GRAY + "  Kills grant +5 hearts (5min)");
                lore.add(ChatColor.AQUA + "Passive: " + ChatColor.WHITE + "Critical Rush");
                lore.add(ChatColor.GRAY + "  Crits grant +10% speed (3s)");
                break;
        }

        lore.add("");
        lore.add(ChatColor.GOLD + "" + ChatColor.ITALIC + "Legendary Weapon");

        return lore;
    }
}
