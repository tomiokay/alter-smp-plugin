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
        // Check if the placed item is a legendary forge
        if (LegendaryItemFactory.isForgeItem(event.getItemInHand())) {
            // Register this location as a forge
            plugin.getAltarManager().registerAltar(event.getBlock().getLocation());

            event.getPlayer().sendMessage(ChatColor.DARK_PURPLE + "You have placed a " +
                ChatColor.BOLD + "Legendary Forge" + ChatColor.DARK_PURPLE + "!");
            event.getPlayer().sendMessage(ChatColor.GRAY + "Right-click it to open the 5x5 crafting menu.");
        }
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        // Check if the broken block is a forge
        if (plugin.getAltarManager().isAltar(event.getBlock().getLocation())) {
            // Allow breaking in Creative mode only (like bedrock)
            if (event.getPlayer().getGameMode() != org.bukkit.GameMode.CREATIVE) {
                event.setCancelled(true);
                event.getPlayer().sendMessage(ChatColor.RED + "Legendary Forges can only be broken in Creative mode!");
                return;
            }

            // In Creative mode: unregister the forge and drop the item
            plugin.getAltarManager().unregisterAltar(event.getBlock().getLocation());
            event.getPlayer().sendMessage(ChatColor.YELLOW + "You have broken a Legendary Forge.");

            // Drop the forge item
            event.setDropItems(false);
            event.getBlock().getWorld().dropItemNaturally(
                event.getBlock().getLocation(),
                plugin.getItemFactory().createForgeItem()
            );
        }
    }
}
