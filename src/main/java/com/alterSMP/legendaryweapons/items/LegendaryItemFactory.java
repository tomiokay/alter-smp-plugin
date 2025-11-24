package com.alterSMP.legendaryweapons.items;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
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
            // Set display name with color
            meta.setDisplayName(ChatColor.GOLD + "" + ChatColor.BOLD + type.getDisplayName());

            // Add lore
            List<String> lore = getLoreForLegendary(type);
            meta.setLore(lore);

            // Add legendary ID to PDC
            NamespacedKey key = new NamespacedKey("legendaryweapons", LEGENDARY_KEY);
            meta.getPersistentDataContainer().set(key, PersistentDataType.STRING, type.getId());

            // Add enchant glint
            meta.addEnchant(Enchantment.UNBREAKING, 1, true);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);

            // Make unbreakable
            meta.setUnbreakable(true);
            meta.addItemFlags(ItemFlag.HIDE_UNBREAKABLE);

            // Initialize soul count for Oblivion Harvester
            if (type == LegendaryType.OBLIVION_HARVESTER) {
                NamespacedKey soulKey = new NamespacedKey("legendaryweapons", SOUL_COUNT_KEY);
                meta.getPersistentDataContainer().set(soulKey, PersistentDataType.INTEGER, 0);
            }

            item.setItemMeta(meta);
        }

        return item;
    }

    public ItemStack createAltarItem() {
        ItemStack item = new ItemStack(Material.CHISELED_STONE_BRICKS);
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            meta.setDisplayName(ChatColor.DARK_PURPLE + "" + ChatColor.BOLD + "Legendary Altar");

            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.GRAY + "A mystical altar used to forge");
            lore.add(ChatColor.GRAY + "legendary weapons with a 5x5 grid.");
            lore.add("");
            lore.add(ChatColor.YELLOW + "Place this block and right-click");
            lore.add(ChatColor.YELLOW + "to open the legendary crafting menu.");
            meta.setLore(lore);

            // Tag as altar item
            NamespacedKey key = new NamespacedKey("legendaryweapons", "legendary_altar_item");
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

    public static boolean isAltarItem(ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return false;
        }

        ItemMeta meta = item.getItemMeta();
        NamespacedKey key = new NamespacedKey("legendaryweapons", "legendary_altar_item");

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

    private List<String> getLoreForLegendary(LegendaryType type) {
        List<String> lore = new ArrayList<>();
        lore.add("");

        switch (type) {
            case BLADE_OF_FRACTURED_STARS:
                lore.add(ChatColor.AQUA + "Passive: " + ChatColor.WHITE + "Flashburst Counter");
                lore.add(ChatColor.GRAY + "  Every 20 hits blinds nearby enemies");
                lore.add("");
                lore.add(ChatColor.GREEN + "Ability 1: " + ChatColor.WHITE + "Star Rift Slash (25s)");
                lore.add(ChatColor.GRAY + "  Beam attack through walls");
                lore.add(ChatColor.GREEN + "Ability 2: " + ChatColor.WHITE + "Stargate Blink (45s)");
                lore.add(ChatColor.GRAY + "  Teleport up to 45 blocks");
                break;

            case EMBERHEART_SCYTHE:
                lore.add(ChatColor.AQUA + "Passive: " + ChatColor.WHITE + "Heat Shield");
                lore.add(ChatColor.GRAY + "  Immune to fire and explosions");
                lore.add("");
                lore.add(ChatColor.GREEN + "Ability 1: " + ChatColor.WHITE + "Flame Harvest (30s)");
                lore.add(ChatColor.GRAY + "  Fiery explosion grants absorption");
                lore.add(ChatColor.GREEN + "Ability 2: " + ChatColor.WHITE + "Fire Rebirth (180s)");
                lore.add(ChatColor.GRAY + "  Cheat death with flames");
                break;

            case TEMPESTBREAKER_SPEAR:
                lore.add(ChatColor.AQUA + "Passive: " + ChatColor.WHITE + "Windwalker");
                lore.add(ChatColor.GRAY + "  Water mobility buffs");
                lore.add("");
                lore.add(ChatColor.GREEN + "Ability 1: " + ChatColor.WHITE + "Gale Throw (25s)");
                lore.add(ChatColor.GRAY + "  Wind vortex on impact");
                lore.add(ChatColor.GREEN + "Ability 2: " + ChatColor.WHITE + "Stormcall (50s)");
                lore.add(ChatColor.GRAY + "  Cone of stunning lightning");
                break;

            case UMBRA_VEIL_DAGGER:
                lore.add(ChatColor.AQUA + "Passive: " + ChatColor.WHITE + "Shadow Presence");
                lore.add(ChatColor.GRAY + "  Speed III while sneaking");
                lore.add("");
                lore.add(ChatColor.GREEN + "Ability 1: " + ChatColor.WHITE + "Shadowstep (20s)");
                lore.add(ChatColor.GRAY + "  Dash and go invisible");
                lore.add(ChatColor.GREEN + "Ability 2: " + ChatColor.WHITE + "Soul Mark (60s)");
                lore.add(ChatColor.GRAY + "  Mark target for true damage");
                break;

            case HEARTROOT_GUARDIAN_AXE:
                lore.add(ChatColor.AQUA + "Passive: " + ChatColor.WHITE + "Nature Channel");
                lore.add(ChatColor.GRAY + "  Regeneration on natural blocks");
                lore.add("");
                lore.add(ChatColor.GREEN + "Ability 1: " + ChatColor.WHITE + "Nature Grasp (35s)");
                lore.add(ChatColor.GRAY + "  Root enemies in place");
                lore.add(ChatColor.GREEN + "Ability 2: " + ChatColor.WHITE + "Forest Shield (70s)");
                lore.add(ChatColor.GRAY + "  Axe becomes breach weapon");
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

            case GLACIERBOUND_HALBERD:
                lore.add(ChatColor.AQUA + "Passive: " + ChatColor.WHITE + "Frozen Path");
                lore.add(ChatColor.GRAY + "  Water freezes beneath you");
                lore.add("");
                lore.add(ChatColor.GREEN + "Ability 1: " + ChatColor.WHITE + "Frostbite Sweep (28s)");
                lore.add(ChatColor.GRAY + "  Cone that freezes enemies");
                lore.add(ChatColor.GREEN + "Ability 2: " + ChatColor.WHITE + "Winter's Embrace (75s)");
                lore.add(ChatColor.GRAY + "  Frost dome for protection");
                break;

            case CELESTIAL_AEGIS_SHIELD:
                lore.add(ChatColor.AQUA + "Passive: " + ChatColor.WHITE + "Aura of Protection");
                lore.add(ChatColor.GRAY + "  Allies gain Resistance I");
                lore.add("");
                lore.add(ChatColor.GREEN + "Ability 1: " + ChatColor.WHITE + "Radiant Block (40s)");
                lore.add(ChatColor.GRAY + "  Reflect 75% damage for 5s");
                lore.add(ChatColor.GREEN + "Ability 2: " + ChatColor.WHITE + "Heaven's Wall (90s)");
                lore.add(ChatColor.GRAY + "  Summon protective barrier");
                break;

            case CHRONO_EDGE:
                lore.add(ChatColor.AQUA + "Passive: " + ChatColor.WHITE + "Last Second");
                lore.add(ChatColor.GRAY + "  Buffs when low HP");
                lore.add("");
                lore.add(ChatColor.GREEN + "Ability 1: " + ChatColor.WHITE + "Echo Strike (40s)");
                lore.add(ChatColor.GRAY + "  Hits repeat after 1 second");
                lore.add(ChatColor.GREEN + "Ability 2: " + ChatColor.WHITE + "Time Rewind (120s)");
                lore.add(ChatColor.GRAY + "  Return to past state");
                break;

            case OBLIVION_HARVESTER:
                lore.add(ChatColor.AQUA + "Passive: " + ChatColor.WHITE + "Soul Collector");
                lore.add(ChatColor.GRAY + "  Gain damage from kills");
                lore.add(ChatColor.DARK_PURPLE + "Souls: " + ChatColor.LIGHT_PURPLE + "0/20");
                lore.add("");
                lore.add(ChatColor.GREEN + "Ability 1: " + ChatColor.WHITE + "Void Slice (30s)");
                lore.add(ChatColor.GRAY + "  Sweeping void attack");
                lore.add(ChatColor.GREEN + "Ability 2: " + ChatColor.WHITE + "Void Rift (85s)");
                lore.add(ChatColor.GRAY + "  Create damaging black hole");
                break;

            case ECLIPSE_DEVOURER:
                lore.add(ChatColor.AQUA + "Passive: " + ChatColor.WHITE + "Dragon's Gaze");
                lore.add(ChatColor.GRAY + "  Nearby players glow");
                lore.add("");
                lore.add(ChatColor.GREEN + "Ability 1: " + ChatColor.WHITE + "Void Rupture (35s)");
                lore.add(ChatColor.GRAY + "  Void arc with blindness");
                lore.add(ChatColor.GREEN + "Ability 2: " + ChatColor.WHITE + "Cataclysm Pulse (95s)");
                lore.add(ChatColor.GRAY + "  Dark explosion with pull");
                break;
        }

        lore.add("");
        lore.add(ChatColor.GOLD + "" + ChatColor.ITALIC + "Legendary Weapon");

        return lore;
    }
}
