package com.alterSMP.legendaryweapons.listeners;

import com.alterSMP.legendaryweapons.items.LegendaryItemFactory;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityResurrectEvent;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.ItemStack;

import java.util.EnumSet;
import java.util.Set;

/**
 * Listener that restricts usage of totems of undying and non-legendary netherite items.
 * Legendary netherite items are allowed.
 */
public class RestrictedItemsListener implements Listener {

    private static final Set<Material> NETHERITE_ITEMS = EnumSet.of(
        // Armor
        Material.NETHERITE_HELMET,
        Material.NETHERITE_CHESTPLATE,
        Material.NETHERITE_LEGGINGS,
        Material.NETHERITE_BOOTS,
        // Weapons
        Material.NETHERITE_SWORD,
        Material.NETHERITE_AXE,
        // Tools
        Material.NETHERITE_PICKAXE,
        Material.NETHERITE_SHOVEL,
        Material.NETHERITE_HOE
    );

    /**
     * Check if an item is a restricted netherite item (non-legendary netherite)
     */
    private boolean isRestrictedNetherite(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) {
            return false;
        }

        // Check if it's netherite
        if (!NETHERITE_ITEMS.contains(item.getType())) {
            return false;
        }

        // Allow legendary netherite items
        String legendaryId = LegendaryItemFactory.getLegendaryId(item);
        return legendaryId == null;
    }

    /**
     * Check if an item is a totem of undying
     */
    private boolean isTotem(ItemStack item) {
        return item != null && item.getType() == Material.TOTEM_OF_UNDYING;
    }

    /**
     * Prevent totem resurrection
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onEntityResurrect(EntityResurrectEvent event) {
        if (!(event.getEntity() instanceof Player)) {
            return;
        }

        // Cancel totem resurrection
        event.setCancelled(true);

        Player player = (Player) event.getEntity();
        player.sendMessage(ChatColor.RED + "Totems of Undying are disabled on this server!");
    }

    /**
     * Prevent equipping restricted netherite armor via inventory clicks
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }

        Player player = (Player) event.getWhoClicked();
        ItemStack cursor = event.getCursor();
        ItemStack current = event.getCurrentItem();

        // Check cursor item (item being placed)
        if (isRestrictedNetherite(cursor)) {
            int slot = event.getRawSlot();
            // Armor slots in player inventory are 5-8 (helmet, chestplate, leggings, boots)
            if (slot >= 5 && slot <= 8) {
                event.setCancelled(true);
                player.sendMessage(ChatColor.RED + "Non-legendary netherite armor is disabled!");
                player.updateInventory();
                return;
            }
        }

        // Check shift-click equipping
        if (event.isShiftClick() && isRestrictedNetherite(current)) {
            Material type = current.getType();
            if (type == Material.NETHERITE_HELMET || type == Material.NETHERITE_CHESTPLATE ||
                type == Material.NETHERITE_LEGGINGS || type == Material.NETHERITE_BOOTS) {
                event.setCancelled(true);
                player.sendMessage(ChatColor.RED + "Non-legendary netherite armor is disabled!");
                player.updateInventory();
                return;
            }
        }

        // Check number key swap to armor slots
        if (event.getClick().name().contains("NUMBER_KEY") && event.getRawSlot() >= 5 && event.getRawSlot() <= 8) {
            ItemStack hotbarItem = player.getInventory().getItem(event.getHotbarButton());
            if (isRestrictedNetherite(hotbarItem)) {
                event.setCancelled(true);
                player.sendMessage(ChatColor.RED + "Non-legendary netherite armor is disabled!");
                player.updateInventory();
                return;
            }
        }

        // Check totem placement in offhand
        if (isTotem(cursor)) {
            // Offhand slot is 45 in raw slot
            if (event.getRawSlot() == 45) {
                event.setCancelled(true);
                player.sendMessage(ChatColor.RED + "Totems of Undying are disabled on this server!");
                player.updateInventory();
            }
        }
    }

    /**
     * Prevent holding restricted netherite weapons in hand
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerItemHeld(PlayerItemHeldEvent event) {
        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItem(event.getNewSlot());

        if (isRestrictedNetherite(item)) {
            Material type = item.getType();
            if (type == Material.NETHERITE_SWORD || type == Material.NETHERITE_AXE ||
                type == Material.NETHERITE_PICKAXE || type == Material.NETHERITE_SHOVEL ||
                type == Material.NETHERITE_HOE) {
                event.setCancelled(true);
                player.sendMessage(ChatColor.RED + "Non-legendary netherite weapons/tools are disabled!");
            }
        }
    }

    /**
     * Prevent swapping totem or restricted netherite to offhand
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onSwapHandItems(PlayerSwapHandItemsEvent event) {
        Player player = event.getPlayer();
        ItemStack offhandItem = event.getOffHandItem();

        if (isTotem(offhandItem)) {
            event.setCancelled(true);
            player.sendMessage(ChatColor.RED + "Totems of Undying are disabled on this server!");
        }

        if (isRestrictedNetherite(offhandItem)) {
            event.setCancelled(true);
            player.sendMessage(ChatColor.RED + "Non-legendary netherite items are disabled!");
        }
    }

    /**
     * Prevent equipping restricted netherite armor via right-click in air
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack mainHand = player.getInventory().getItemInMainHand();

        // Check if right-clicking with armor (to equip)
        if (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            if (isRestrictedNetherite(mainHand)) {
                Material type = mainHand.getType();
                if (type == Material.NETHERITE_HELMET || type == Material.NETHERITE_CHESTPLATE ||
                    type == Material.NETHERITE_LEGGINGS || type == Material.NETHERITE_BOOTS) {
                    event.setCancelled(true);
                    player.sendMessage(ChatColor.RED + "Non-legendary netherite armor is disabled!");
                    player.updateInventory();
                }
            }
        }
    }
}
