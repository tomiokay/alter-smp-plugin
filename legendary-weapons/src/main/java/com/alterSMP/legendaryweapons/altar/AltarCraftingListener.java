package com.alterSMP.legendaryweapons.altar;

import com.alterSMP.legendaryweapons.LegendaryWeaponsPlugin;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.Arrays;
import java.util.Map;
import java.util.UUID;

public class AltarCraftingListener implements Listener {

    private final LegendaryWeaponsPlugin plugin;
    private final Map<String, AltarRecipe> recipes;

    public AltarCraftingListener(LegendaryWeaponsPlugin plugin) {
        this.plugin = plugin;
        this.recipes = AltarRecipe.createAllRecipes();
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }

        String title = event.getView().getTitle();
        if (!title.equals(ChatColor.DARK_PURPLE + "Legendary Forge")) {
            return;
        }

        Player player = (Player) event.getWhoClicked();
        int slot = event.getRawSlot();

        // Prevent clicking on glass panes
        if (slot >= 0 && slot < 54) {
            int[] gridSlots = AltarInteractListener.getGridSlots();
            int outputSlot = AltarInteractListener.getOutputSlot();

            boolean isGridSlot = false;
            for (int gridSlot : gridSlots) {
                if (slot == gridSlot) {
                    isGridSlot = true;
                    break;
                }
            }

            // Allow clicking grid slots and output slot
            if (isGridSlot || slot == outputSlot) {
                // If clicking output slot
                if (slot == outputSlot) {
                    // Block placing items INTO the output slot (only allow taking)
                    InventoryAction action = event.getAction();
                    ItemStack cursor = event.getCursor();

                    // If trying to place an item into output slot, cancel it
                    if (cursor != null && cursor.getType() != org.bukkit.Material.AIR) {
                        if (action == InventoryAction.PLACE_ALL ||
                            action == InventoryAction.PLACE_ONE ||
                            action == InventoryAction.PLACE_SOME ||
                            action == InventoryAction.SWAP_WITH_CURSOR) {
                            event.setCancelled(true);
                            return;
                        }
                    }

                    handleOutputClick(event, player);
                } else {
                    // Normal grid interaction - update result after a tick
                    plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                        updateCraftingResult(event.getInventory(), player);
                    }, 1L);
                }
            } else {
                // Clicking on decoration - cancel
                event.setCancelled(true);
            }

        // Handle shift-click from player inventory - block if it would go to output slot
        } else if (slot >= 54 && event.isShiftClick()) {
            // Shift-clicking from player inventory into the forge
            // We need to make sure items don't go into the output slot
            // The output slot should never receive items via shift-click
            // Since shift-click tries to put items in available slots,
            // and we want it to only go to grid slots, we handle it manually

            ItemStack clickedItem = event.getCurrentItem();
            if (clickedItem == null || clickedItem.getType() == org.bukkit.Material.AIR) {
                return;
            }

            // Cancel the default behavior
            event.setCancelled(true);

            // Manually place the item in an empty grid slot
            int[] gridSlots = AltarInteractListener.getGridSlots();
            Inventory inv = event.getInventory();

            for (int gridSlot : gridSlots) {
                ItemStack slotItem = inv.getItem(gridSlot);
                if (slotItem == null || slotItem.getType() == org.bukkit.Material.AIR) {
                    // Found empty slot, move item there
                    inv.setItem(gridSlot, clickedItem.clone());
                    clickedItem.setAmount(0);

                    // Update crafting result
                    plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                        updateCraftingResult(inv, player);
                    }, 1L);
                    return;
                }
            }
            // No empty grid slots - item stays in player inventory
        }
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        String title = event.getView().getTitle();
        if (!title.equals(ChatColor.DARK_PURPLE + "Legendary Forge")) {
            return;
        }

        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }

        Player player = (Player) event.getWhoClicked();

        // Update result after drag
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            updateCraftingResult(event.getInventory(), player);
        }, 1L);
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        String title = event.getView().getTitle();
        if (!title.equals(ChatColor.DARK_PURPLE + "Legendary Forge")) {
            return;
        }

        if (!(event.getPlayer() instanceof Player)) {
            return;
        }

        Player player = (Player) event.getPlayer();
        Inventory inv = event.getInventory();

        // Return all items from grid to player
        int[] gridSlots = AltarInteractListener.getGridSlots();
        for (int slot : gridSlots) {
            ItemStack item = inv.getItem(slot);
            if (item != null && item.getType() != org.bukkit.Material.AIR) {
                player.getInventory().addItem(item);
                inv.setItem(slot, null);
            }
        }
    }

    private void handleOutputClick(InventoryClickEvent event, Player player) {
        Inventory inv = event.getInventory();
        int outputSlot = AltarInteractListener.getOutputSlot();
        ItemStack result = inv.getItem(outputSlot);

        if (result == null || result.getType() == org.bukkit.Material.AIR) {
            event.setCancelled(true);
            return;
        }

        // Check if the recipe still matches
        ItemStack[] gridItems = getGridItems(inv);
        AltarRecipe matchedRecipe = findMatchingRecipe(gridItems);

        if (matchedRecipe == null) {
            event.setCancelled(true);
            inv.setItem(outputSlot, null);
            return;
        }

        // Check if this legendary has already been crafted globally
        String legendaryId = matchedRecipe.getResult().getId();
        if (plugin.getDataManager().hasCrafted(legendaryId)) {
            event.setCancelled(true);
            UUID crafter = plugin.getDataManager().getCrafter(legendaryId);
            String crafterName = crafter != null ? plugin.getServer().getOfflinePlayer(crafter).getName() : "Unknown";
            player.sendMessage(ChatColor.RED + "This legendary has already been forged by " + crafterName + "!");
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            return;
        }

        // Check if this legendary is disabled
        if (plugin.getDataManager().isLegendaryDisabled(legendaryId)) {
            event.setCancelled(true);
            player.sendMessage(ChatColor.RED + "This legendary is currently disabled and cannot be forged!");
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            return;
        }

        // Consume ingredients
        int[] gridSlots = AltarInteractListener.getGridSlots();
        for (int slot : gridSlots) {
            ItemStack item = inv.getItem(slot);
            if (item != null && item.getType() != org.bukkit.Material.AIR) {
                item.setAmount(item.getAmount() - 1);
                if (item.getAmount() <= 0) {
                    inv.setItem(slot, null);
                }
            }
        }

        // Give the legendary item
        ItemStack legendary = plugin.getItemFactory().createLegendary(matchedRecipe.getResult());
        event.setCancelled(true);
        inv.setItem(outputSlot, null);

        player.getInventory().addItem(legendary);

        // Get location for storage and broadcast
        org.bukkit.Location loc = player.getLocation();
        String locationStr = loc.getWorld().getName() + "," + loc.getBlockX() + "," + loc.getBlockY() + "," + loc.getBlockZ();
        plugin.getDataManager().markCrafted(player.getUniqueId(), legendaryId, locationStr);

        // Broadcast to entire server with coordinates
        org.bukkit.Bukkit.broadcastMessage(ChatColor.GOLD + "" + ChatColor.BOLD + player.getName() +
            " has forged " + matchedRecipe.getResult().getDisplayName() + ChatColor.GOLD + ChatColor.BOLD + "!");
        org.bukkit.Bukkit.broadcastMessage(ChatColor.GRAY + "Location: " + ChatColor.YELLOW +
            loc.getWorld().getName() + " " + loc.getBlockX() + ", " + loc.getBlockY() + ", " + loc.getBlockZ());

        // Effects for crafter
        player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);
        player.playSound(player.getLocation(), Sound.BLOCK_ENCHANTMENT_TABLE_USE, 1.0f, 0.8f);

        // Clear result and update
        updateCraftingResult(inv, player);
    }

    private void updateCraftingResult(Inventory inv, Player player) {
        ItemStack[] gridItems = getGridItems(inv);
        AltarRecipe matchedRecipe = findMatchingRecipe(gridItems);

        int outputSlot = AltarInteractListener.getOutputSlot();

        if (matchedRecipe != null) {
            String legendaryId = matchedRecipe.getResult().getId();

            // Check if disabled
            if (plugin.getDataManager().isLegendaryDisabled(legendaryId)) {
                ItemStack disabled = new ItemStack(org.bukkit.Material.BARRIER);
                org.bukkit.inventory.meta.ItemMeta meta = disabled.getItemMeta();
                if (meta != null) {
                    meta.setDisplayName(ChatColor.RED + "Disabled");
                    java.util.List<String> lore = new java.util.ArrayList<>();
                    lore.add(ChatColor.GRAY + "This legendary is currently disabled");
                    meta.setLore(lore);
                    disabled.setItemMeta(meta);
                }
                inv.setItem(outputSlot, disabled);
            }
            // Check if already crafted globally
            else if (plugin.getDataManager().hasCrafted(legendaryId)) {
                // Show locked item with crafter name
                ItemStack locked = new ItemStack(org.bukkit.Material.BARRIER);
                org.bukkit.inventory.meta.ItemMeta meta = locked.getItemMeta();
                if (meta != null) {
                    UUID crafter = plugin.getDataManager().getCrafter(legendaryId);
                    String crafterName = crafter != null ? plugin.getServer().getOfflinePlayer(crafter).getName() : "Unknown";
                    meta.setDisplayName(ChatColor.RED + "Already Crafted");
                    java.util.List<String> lore = new java.util.ArrayList<>();
                    lore.add(ChatColor.GRAY + "Forged by: " + ChatColor.YELLOW + crafterName);
                    meta.setLore(lore);
                    locked.setItemMeta(meta);
                }
                inv.setItem(outputSlot, locked);
            } else {
                ItemStack result = plugin.getItemFactory().createLegendary(matchedRecipe.getResult());
                inv.setItem(outputSlot, result);
            }
        } else {
            inv.setItem(outputSlot, null);
        }
    }

    private ItemStack[] getGridItems(Inventory inv) {
        int[] gridSlots = AltarInteractListener.getGridSlots();
        ItemStack[] items = new ItemStack[25];

        for (int i = 0; i < gridSlots.length; i++) {
            items[i] = inv.getItem(gridSlots[i]);
        }

        return items;
    }

    private AltarRecipe findMatchingRecipe(ItemStack[] gridItems) {
        for (AltarRecipe recipe : recipes.values()) {
            if (recipe.matches(gridItems)) {
                return recipe;
            }
        }
        return null;
    }
}
