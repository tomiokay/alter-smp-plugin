package com.alterSMP.legendaryweapons.listeners;

import com.alterSMP.legendaryweapons.items.LegendaryItemFactory;
import com.alterSMP.legendaryweapons.items.LegendaryType;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.inventory.AnvilInventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

/**
 * Listener that protects legendary item names:
 * 1. Prevents normal items from being renamed to legendary names
 * 2. Forces legendary items to keep their canonical names (cannot be renamed)
 */
public class AnvilProtectionListener implements Listener {

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPrepareAnvil(PrepareAnvilEvent event) {
        AnvilInventory anvil = event.getInventory();
        ItemStack result = event.getResult();
        ItemStack firstItem = anvil.getItem(0);

        if (result == null || firstItem == null) {
            return;
        }

        String legendaryId = LegendaryItemFactory.getLegendaryId(firstItem);
        ItemMeta resultMeta = result.getItemMeta();

        if (resultMeta == null) {
            return;
        }

        String resultName = resultMeta.hasDisplayName() ? resultMeta.getDisplayName() : null;
        String strippedResultName = resultName != null ? ChatColor.stripColor(resultName) : null;

        // Case 1: If the item is a legendary, prevent renaming entirely
        if (legendaryId != null) {
            LegendaryType type = LegendaryType.fromId(legendaryId);
            if (type != null) {
                String expectedName = ChatColor.GOLD + "" + ChatColor.BOLD + type.getDisplayName();

                // If the result name doesn't match the canonical name, cancel
                if (!expectedName.equals(resultName)) {
                    // Force the name back to canonical
                    resultMeta.setDisplayName(expectedName);
                    result.setItemMeta(resultMeta);
                    event.setResult(result);

                    // Notify the player
                    if (event.getViewers().size() > 0 && event.getViewers().get(0) instanceof Player) {
                        Player player = (Player) event.getViewers().get(0);
                        player.sendMessage(ChatColor.RED + "Legendary items cannot be renamed!");
                    }
                }
            }
            return;
        }

        // Case 2: If the item is NOT a legendary, check if they're trying to use a protected name
        if (strippedResultName != null && !strippedResultName.isEmpty()) {
            // Check if the result name matches any legendary display name
            if (LegendaryType.isProtectedName(strippedResultName)) {
                // Cancel the result
                event.setResult(null);

                // Notify the player
                if (event.getViewers().size() > 0 && event.getViewers().get(0) instanceof Player) {
                    Player player = (Player) event.getViewers().get(0);
                    player.sendMessage(ChatColor.RED + "You cannot rename items to legendary names!");
                }
            }
        }
    }
}
