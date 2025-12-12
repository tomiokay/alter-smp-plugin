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

            // Set custom model data
            meta.setCustomModelData(type.getCustomModelData());

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
            case DRAGONBORN_BLADE:
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
            case FORGE_BOOTS:
                meta.addEnchant(Enchantment.PROTECTION, 4, true);
                meta.addEnchant(Enchantment.FEATHER_FALLING, 4, true);
                meta.addEnchant(Enchantment.DEPTH_STRIDER, 3, true);
                break;

            // Leggings
            case FORGE_LEGGINGS:
                meta.addEnchant(Enchantment.PROTECTION, 4, true);
                meta.addEnchant(Enchantment.FIRE_PROTECTION, 4, true);
                break;

            // Chestplate
            case FORGE_CHESTPLATE:
                meta.addEnchant(Enchantment.PROTECTION, 4, true);
                break;

            // Helmet
            case FORGE_HELMET:
                meta.addEnchant(Enchantment.PROTECTION, 4, true);
                meta.addEnchant(Enchantment.RESPIRATION, 4, true);
                meta.addEnchant(Enchantment.AQUA_AFFINITY, 1, true);
                break;

            // Pickaxe
            case FORGE_PICKAXE:
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
                lore.add(ChatColor.AQUA + "Passive: " + ChatColor.WHITE + "Lunar Blessing");
                lore.add(ChatColor.GRAY + "  Full Moon: Strength III");
                lore.add(ChatColor.GRAY + "  Gibbous: Speed I");
                lore.add(ChatColor.GRAY + "  Quarter: Strength I");
                lore.add("");
                lore.add(ChatColor.GREEN + "Ability 1: " + ChatColor.WHITE + "Star Rift Slash (25s)");
                lore.add(ChatColor.GRAY + "  30-block beam through walls");
                lore.add(ChatColor.GRAY + "  ~7 hearts through Prot 4");
                lore.add(ChatColor.GREEN + "Ability 2: " + ChatColor.WHITE + "Stargate Blink (45s)");
                lore.add(ChatColor.GRAY + "  Teleport up to 45 blocks");
                break;

            case PHEONIX_GRACE:
                lore.add(ChatColor.AQUA + "Passive: " + ChatColor.WHITE + "Heat Shield");
                lore.add(ChatColor.GRAY + "  Immune to fire and explosions");
                lore.add("");
                lore.add(ChatColor.GREEN + "Ability 1: " + ChatColor.WHITE + "Flame Harvest (90s)");
                lore.add(ChatColor.GRAY + "  8-block fire explosion");
                lore.add(ChatColor.GRAY + "  Grants 3 absorption hearts");
                lore.add(ChatColor.GREEN + "Ability 2: " + ChatColor.WHITE + "Fire Rebirth (5min)");
                lore.add(ChatColor.GRAY + "  Survive death for 30s window");
                lore.add(ChatColor.GRAY + "  Revive at 6 hearts with fire resist");
                break;

            case TEMPESTBREAKER_SPEAR:
                lore.add(ChatColor.AQUA + "Passive: " + ChatColor.WHITE + "Windwalker");
                lore.add(ChatColor.GRAY + "  Dolphin's Grace and water breathing");
                lore.add("");
                lore.add(ChatColor.GREEN + "Ability 1: " + ChatColor.WHITE + "Gale Throw (25s)");
                lore.add(ChatColor.GRAY + "  Wind vortex pulls enemies in");
                lore.add(ChatColor.GRAY + "  ~3 hearts damage, levitation");
                lore.add(ChatColor.GREEN + "Ability 2: " + ChatColor.WHITE + "Stormcall (50s)");
                lore.add(ChatColor.GRAY + "  8-block cone lightning strike");
                lore.add(ChatColor.GRAY + "  ~5 hearts, 2s stun");
                break;

            case THOUSAND_DEMON_DAGGERS:
                lore.add(ChatColor.AQUA + "Passive: " + ChatColor.WHITE + "Shadow Presence");
                lore.add(ChatColor.GRAY + "  Speed III while sneaking");
                lore.add("");
                lore.add(ChatColor.GREEN + "Ability 1: " + ChatColor.WHITE + "Shadowstep (20s)");
                lore.add(ChatColor.GRAY + "  Teleport behind target (15 blocks)");
                lore.add(ChatColor.GRAY + "  Next attack: +1 heart true damage");
                lore.add(ChatColor.GREEN + "Ability 2: " + ChatColor.WHITE + "Soul Mark (60s)");
                lore.add(ChatColor.GRAY + "  Mark target for 10s");
                lore.add(ChatColor.GRAY + "  All hits deal +2 true damage");
                break;

            case DIVINE_AXE_RHITTA:
                lore.add(ChatColor.AQUA + "Passive: " + ChatColor.WHITE + "Nature Channel");
                lore.add(ChatColor.GRAY + "  Regeneration on grass/dirt blocks");
                lore.add("");
                lore.add(ChatColor.GREEN + "Ability 1: " + ChatColor.WHITE + "Nature Grasp (35s)");
                lore.add(ChatColor.GRAY + "  Root enemies for 3s (8 blocks)");
                lore.add(ChatColor.GRAY + "  Creates vine particles");
                lore.add(ChatColor.GREEN + "Ability 2: " + ChatColor.WHITE + "Forest Shield (70s)");
                lore.add(ChatColor.GRAY + "  Axe becomes Breach V weapon");
                lore.add(ChatColor.GRAY + "  Shreds shields for 15s");
                break;

            case CHAINS_OF_ETERNITY:
                lore.add(ChatColor.AQUA + "Passive: " + ChatColor.WHITE + "Eternal Resilience");
                lore.add(ChatColor.GRAY + "  Resistance I while holding/offhand");
                lore.add("");
                lore.add(ChatColor.GREEN + "Ability 1: " + ChatColor.WHITE + "Soul Bind (35s)");
                lore.add(ChatColor.GRAY + "  Pull target to you (20 blocks)");
                lore.add(ChatColor.GRAY + "  Deals damage + Slowness V for 3s");
                lore.add(ChatColor.GREEN + "Ability 2: " + ChatColor.WHITE + "Prison of Damned (65s)");
                lore.add(ChatColor.GRAY + "  Cage target in iron bars (5s)");
                lore.add(ChatColor.GRAY + "  Unbreakable cage");
                break;

            case FORGE_BOOTS:
                lore.add(ChatColor.AQUA + "Passive: " + ChatColor.WHITE + "Featherfall");
                lore.add(ChatColor.GRAY + "  Immune to fall damage");
                lore.add("");
                lore.add(ChatColor.GREEN + "Ability: " + ChatColor.WHITE + "Meteor Slam");
                lore.add(ChatColor.GRAY + "  Shift mid-air to slam down");
                lore.add(ChatColor.GRAY + "  Damage scales with fall height");
                lore.add(ChatColor.GRAY + "  Creates shockwave on landing");
                break;

            case CELESTIAL_AEGIS_SHIELD:
                lore.add(ChatColor.AQUA + "Passive: " + ChatColor.WHITE + "Aura of Protection");
                lore.add(ChatColor.GRAY + "  You and trusted allies get Resistance I");
                lore.add(ChatColor.GRAY + "  Requires shield in offhand");
                lore.add("");
                lore.add(ChatColor.GREEN + "Ability 1: " + ChatColor.WHITE + "Radiant Block (40s)");
                lore.add(ChatColor.GRAY + "  Reflect 75% damage for 5s");
                lore.add(ChatColor.GREEN + "Ability 2: " + ChatColor.WHITE + "Heaven's Wall (90s)");
                lore.add(ChatColor.GRAY + "  16x16 glass barrier for 32s");
                lore.add(ChatColor.GRAY + "  Only trusted players can pass");
                break;

            case CHRONO_BLADE:
                lore.add(ChatColor.AQUA + "Passive: " + ChatColor.WHITE + "Time Freeze");
                lore.add(ChatColor.GRAY + "  Every 20th melee hit freezes for 3s");
                lore.add("");
                lore.add(ChatColor.GREEN + "Ability 1: " + ChatColor.WHITE + "Time Distortion (40s)");
                lore.add(ChatColor.GRAY + "  6-block bubble freezes enemies 3s");
                lore.add(ChatColor.GRAY + "  Ends with 4 hearts true damage");
                lore.add(ChatColor.GREEN + "Ability 2: " + ChatColor.WHITE + "Chrono Shift (120s)");
                lore.add(ChatColor.GRAY + "  Mark position, recast to return");
                lore.add(ChatColor.GRAY + "  Clears debuffs, grants Speed II");
                break;

            case SOUL_DEVOURER:
                lore.add(ChatColor.AQUA + "Passive: " + ChatColor.WHITE + "Soul Collector");
                lore.add(ChatColor.GRAY + "  +1 heart damage per player kill (max 5)");
                lore.add(ChatColor.GRAY + "  Souls lost on death");
                lore.add(ChatColor.DARK_PURPLE + "Souls: " + ChatColor.LIGHT_PURPLE + "0/5");
                lore.add("");
                lore.add(ChatColor.GREEN + "Ability 1: " + ChatColor.WHITE + "Void Slice (30s)");
                lore.add(ChatColor.GRAY + "  10-block void crescent attack");
                lore.add(ChatColor.GRAY + "  ~6 hearts + wither effect");
                lore.add(ChatColor.GREEN + "Ability 2: " + ChatColor.WHITE + "Void Rift (85s)");
                lore.add(ChatColor.GRAY + "  Black hole for 5s (10 blocks)");
                lore.add(ChatColor.GRAY + "  Pulls and damages all nearby");
                break;

            case DRAGONBORN_BLADE:
                lore.add(ChatColor.AQUA + "Passive: " + ChatColor.WHITE + "Dragon's Gaze");
                lore.add(ChatColor.GRAY + "  Nearby players glow (30 blocks)");
                lore.add(ChatColor.AQUA + "Passive: " + ChatColor.WHITE + "Heart Steal");
                lore.add(ChatColor.GRAY + "  Steal 1 heart per player kill (max 5)");
                lore.add(ChatColor.GRAY + "  Victim loses 1 max heart permanently");
                lore.add(ChatColor.GRAY + "  All hearts return when you die");
                lore.add("");
                lore.add(ChatColor.GREEN + "Ability 1: " + ChatColor.WHITE + "End Sever (30s)");
                lore.add(ChatColor.GRAY + "  Wide 12-block purple blade arc");
                lore.add(ChatColor.GRAY + "  ~2.5 hearts + Weakness + Levitation");
                lore.add(ChatColor.GREEN + "Ability 2: " + ChatColor.WHITE + "Dragon Dash (120s)");
                lore.add(ChatColor.GRAY + "  15-block dash through enemies");
                lore.add(ChatColor.GRAY + "  4 hearts damage + 0.5s stun each");
                break;

            case FORGE_PICKAXE:
                lore.add(ChatColor.AQUA + "Passive: " + ChatColor.WHITE + "None");
                lore.add("");
                lore.add(ChatColor.GREEN + "Ability 1: " + ChatColor.WHITE + "3x3 Mining Toggle");
                lore.add(ChatColor.GRAY + "  Toggle 3x3 area mining");
                lore.add(ChatColor.GREEN + "Ability 2: " + ChatColor.WHITE + "Enchant Switch");
                lore.add(ChatColor.GRAY + "  Toggle Silk Touch / Fortune III");
                break;

            case FORGE_CHESTPLATE:
                lore.add(ChatColor.AQUA + "Passive: " + ChatColor.WHITE + "Lightning Storm");
                lore.add(ChatColor.GRAY + "  Every 10 melee hits: lightning strike");
                lore.add(ChatColor.GRAY + "  Deals ~2.5 hearts to target");
                break;

            case FORGE_LEGGINGS:
                lore.add(ChatColor.AQUA + "Passive: " + ChatColor.WHITE + "Flamebound Feet");
                lore.add(ChatColor.GRAY + "  Permanent Fire Resistance");
                lore.add(ChatColor.GRAY + "  Permanent Haste 2");
                break;

            case FORGE_HELMET:
                lore.add(ChatColor.AQUA + "Passive: " + ChatColor.WHITE + "Blood Harvest");
                lore.add(ChatColor.GRAY + "  Player kill: +5 max hearts (5min)");
                lore.add(ChatColor.AQUA + "Passive: " + ChatColor.WHITE + "Aqua Abilities");
                lore.add(ChatColor.GRAY + "  Permanent Conduit Power and Dolphin's Grace");
                break;

            case LANTERN_OF_LOST_NAMES:
                lore.add(ChatColor.AQUA + "Passive: " + ChatColor.WHITE + "Phantom Veil");
                lore.add(ChatColor.GRAY + "  You are invisible to players");
                lore.add(ChatColor.GRAY + "  you haven't killed yet");
                lore.add(ChatColor.GRAY + "  Attacking reveals you for 5min");
                lore.add(ChatColor.GRAY + "  Hold in main or offhand");
                lore.add("");
                lore.add(ChatColor.YELLOW + "Strategic assassin tool");
                break;

            case RIFT_KEY_OF_ENDKEEPER:
                lore.add(ChatColor.AQUA + "Passive: " + ChatColor.WHITE + "None");
                lore.add("");
                lore.add(ChatColor.GREEN + "Ability 1: " + ChatColor.WHITE + "Rift Teleport (4h)");
                lore.add(ChatColor.GRAY + "  Teleport to any coordinates");
                lore.add(ChatColor.GRAY + "  Type coords in chat: X Y Z");
                lore.add(ChatColor.GRAY + "  Cannot use while in combat");
                lore.add("");
                lore.add(ChatColor.YELLOW + "Cross-world emergency escape");
                break;

            case CHAOS_DICE_OF_FATE:
                lore.add(ChatColor.AQUA + "Passive: " + ChatColor.WHITE + "None");
                lore.add("");
                lore.add(ChatColor.GREEN + "Ability 1: " + ChatColor.WHITE + "Roll Dice (30min)");
                lore.add(ChatColor.GRAY + "  Random powerful effect:");
                lore.add(ChatColor.GRAY + "  1. +5 hearts (15min)");
                lore.add(ChatColor.GRAY + "  2. 5 Iron Golems (5min)");
                lore.add(ChatColor.GRAY + "  3. Speed+Strength III (10min)");
                lore.add(ChatColor.GRAY + "  4. Scramble enemy hotbar");
                lore.add(ChatColor.GRAY + "  5. Free player scans (20min)");
                lore.add(ChatColor.GRAY + "  6. All crits (15min)");
                lore.add(ChatColor.GRAY + "  7. Resistance II (5min)");
                lore.add("");
                lore.add(ChatColor.GREEN + "Ability 2: " + ChatColor.WHITE + "Player Scan (10s)");
                lore.add(ChatColor.GRAY + "  Only available with #5 buff");
                lore.add(ChatColor.GRAY + "  Shows all player locations");
                break;
        }

        lore.add("");
        lore.add(ChatColor.GOLD + "" + ChatColor.ITALIC + "Legendary Weapon");

        return lore;
    }
}