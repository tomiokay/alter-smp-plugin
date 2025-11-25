package com.alterSMP.legendaryweapons.altar;

import com.alterSMP.legendaryweapons.LegendaryWeaponsPlugin;
import com.alterSMP.legendaryweapons.items.LegendaryItemFactory;
import org.bukkit.ChatColor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;

public class AltarPlaceListener implements Listener {

    private final LegendaryWeaponsPlugin plugin;

    public AltarPlaceListener(LegendaryWeaponsPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        // Check if the placed item is a legendary altar
        if (LegendaryItemFactory.isAltarItem(event.getItemInHand())) {
            // Register this location as an altar
            plugin.getAltarManager().registerAltar(event.getBlock().getLocation());

            event.getPlayer().sendMessage(ChatColor.DARK_PURPLE + "You have placed a " +
                ChatColor.BOLD + "Legendary Altar" + ChatColor.DARK_PURPLE + "!");
            event.getPlayer().sendMessage(ChatColor.GRAY + "Right-click it to open the 5x5 crafting menu.");
        }
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        // Check if the broken block is an altar
        if (plugin.getAltarManager().isAltar(event.getBlock().getLocation())) {
            // Cancel the break event - altars are unbreakable
            event.setCancelled(true);
            event.getPlayer().sendMessage(ChatColor.RED + "Legendary Altars cannot be broken!");
            event.getPlayer().sendMessage(ChatColor.GRAY + "Contact an administrator if you need to remove this altar.");
        }
    }
}
