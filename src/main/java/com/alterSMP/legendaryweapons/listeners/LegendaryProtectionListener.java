package com.alterSMP.legendaryweapons.listeners;

import com.alterSMP.legendaryweapons.items.LegendaryItemFactory;
import org.bukkit.ChatColor;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.ItemDespawnEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

/**
 * Listener that protects legendary items:
 * - Cannot be placed in containers (chests, ender chests, bundles, shulker boxes, etc.)
 * - Cannot be picked up by hoppers
 * - Immune to fire, explosions, cactus, and all damage (except void)
 * - Cannot despawn
 */
public class LegendaryProtectionListener implements Listener {

    /**
     * Check if an item is a legendary
     */
    private boolean isLegendary(ItemStack item) {
        if (item == null) return false;
        return LegendaryItemFactory.getLegendaryId(item) != null;
    }

    /**
     * Check if inventory is a container (not player inventory)
     */
    private boolean isContainer(Inventory inventory) {
        if (inventory == null) return false;
        InventoryType type = inventory.getType();

        // Allow player inventory and crafting
        if (type == InventoryType.PLAYER || type == InventoryType.CRAFTING) {
            return false;
        }

        // Block all containers
        switch (type) {
            case CHEST:
            case ENDER_CHEST:
            case SHULKER_BOX:
            case BARREL:
            case HOPPER:
            case DROPPER:
            case DISPENSER:
            case FURNACE:
            case BLAST_FURNACE:
            case SMOKER:
            case BREWING:
            case BEACON:
            case ANVIL:
            case ENCHANTING:
            case GRINDSTONE:
            case STONECUTTER:
            case CARTOGRAPHY:
            case LOOM:
            case SMITHING:
            case MERCHANT:
            case CREATIVE:
            case WORKBENCH:
            case LECTERN:
            case COMPOSTER:
            case CHISELED_BOOKSHELF:
            case DECORATED_POT:
            case CRAFTER:
            case SMITHING_NEW:
                return true;
            default:
                // Block any unknown container types to be safe
                return type != InventoryType.PLAYER && type != InventoryType.CRAFTING;
        }
    }

    /**
     * Prevent placing legendaries in containers via click
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();

        Inventory clickedInv = event.getClickedInventory();
        Inventory topInv = event.getView().getTopInventory();

        // Check if clicking into a container with a legendary on cursor
        ItemStack cursor = event.getCursor();
        if (isLegendary(cursor) && clickedInv != null && isContainer(clickedInv)) {
            event.setCancelled(true);
            player.sendMessage(ChatColor.RED + "Legendary items cannot be stored in containers!");
            return;
        }

        // Check shift-click from player inventory to container
        if (event.isShiftClick() && isLegendary(event.getCurrentItem())) {
            if (isContainer(topInv)) {
                event.setCancelled(true);
                player.sendMessage(ChatColor.RED + "Legendary items cannot be stored in containers!");
                return;
            }
        }

        // Check number key swap into container
        if (event.getClick().name().contains("NUMBER_KEY")) {
            ItemStack hotbarItem = player.getInventory().getItem(event.getHotbarButton());
            if (isLegendary(hotbarItem) && isContainer(clickedInv)) {
                event.setCancelled(true);
                player.sendMessage(ChatColor.RED + "Legendary items cannot be stored in containers!");
                return;
            }
        }

        // Prevent putting legendaries in bundles (bundle slot interaction)
        ItemStack currentItem = event.getCurrentItem();
        if (currentItem != null && currentItem.getType().name().contains("BUNDLE")) {
            if (isLegendary(cursor)) {
                event.setCancelled(true);
                player.sendMessage(ChatColor.RED + "Legendary items cannot be stored in bundles!");
                return;
            }
        }
    }

    /**
     * Prevent dragging legendaries into containers
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();

        if (!isLegendary(event.getOldCursor())) return;

        Inventory topInv = event.getView().getTopInventory();
        if (!isContainer(topInv)) return;

        // Check if any slots are in the top inventory (container)
        int topSize = topInv.getSize();
        for (int slot : event.getRawSlots()) {
            if (slot < topSize) {
                event.setCancelled(true);
                player.sendMessage(ChatColor.RED + "Legendary items cannot be stored in containers!");
                return;
            }
        }
    }

    /**
     * Prevent hoppers from moving legendary items
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryMoveItem(InventoryMoveItemEvent event) {
        if (isLegendary(event.getItem())) {
            event.setCancelled(true);
        }
    }

    /**
     * Prevent legendary items from taking damage (fire, explosions, cactus, etc.)
     * Only void can destroy them
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onItemDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Item)) return;

        Item itemEntity = (Item) event.getEntity();
        ItemStack item = itemEntity.getItemStack();

        if (!isLegendary(item)) return;

        // Allow void damage (item falls into void)
        if (event.getCause() == EntityDamageEvent.DamageCause.VOID) {
            return;
        }

        // Cancel all other damage types
        event.setCancelled(true);

        // Make sure fire is extinguished
        itemEntity.setFireTicks(0);
    }

    /**
     * Prevent legendary items from despawning
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onItemDespawn(ItemDespawnEvent event) {
        Item itemEntity = event.getEntity();
        ItemStack item = itemEntity.getItemStack();

        if (isLegendary(item)) {
            event.setCancelled(true);
        }
    }
}
