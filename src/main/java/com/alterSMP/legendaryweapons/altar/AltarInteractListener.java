package com.alterSMP.legendaryweapons.altar;

import com.alterSMP.legendaryweapons.LegendaryWeaponsPlugin;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class AltarInteractListener implements Listener {

    private final LegendaryWeaponsPlugin plugin;

    // Slot layout for 5x5 grid in a 6-row inventory (54 slots)
    // Rows: 0-8, 9-17, 18-26, 27-35, 36-44, 45-53
    // 5x5 grid uses: [1-5], [10-14], [19-23], [28-32], [37-41]
    // Output slot: 25 (center of row 3)

    public AltarInteractListener(LegendaryWeaponsPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        if (event.getClickedBlock() == null) {
            return;
        }

        // Check if the clicked block is an altar
        if (plugin.getAltarManager().isAltar(event.getClickedBlock().getLocation())) {
            event.setCancelled(true);

            Player player = event.getPlayer();
            openAltarGUI(player);
        }
    }

    private void openAltarGUI(Player player) {
        Inventory inv = Bukkit.createInventory(null, 54, ChatColor.DARK_PURPLE + "Legendary Altar");

        // Fill with glass panes for decoration
        ItemStack grayPane = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta paneMeta = grayPane.getItemMeta();
        if (paneMeta != null) {
            paneMeta.setDisplayName(" ");
            grayPane.setItemMeta(paneMeta);
        }

        // Fill all slots with panes initially
        for (int i = 0; i < 54; i++) {
            inv.setItem(i, grayPane);
        }

        // Clear the 5x5 grid slots
        int[][] gridSlots = {
            {1, 2, 3, 4, 5},
            {10, 11, 12, 13, 14},
            {19, 20, 21, 22, 23},
            {28, 29, 30, 31, 32},
            {37, 38, 39, 40, 41}
        };

        for (int[] row : gridSlots) {
            for (int slot : row) {
                inv.setItem(slot, null);
            }
        }

        // Clear output slot (center position)
        inv.setItem(25, null);

        // Add arrow indicator
        ItemStack arrow = new ItemStack(Material.ARROW);
        ItemMeta arrowMeta = arrow.getItemMeta();
        if (arrowMeta != null) {
            arrowMeta.setDisplayName(ChatColor.YELLOW + "Result");
            arrow.setItemMeta(arrowMeta);
        }
        inv.setItem(24, arrow);

        player.openInventory(inv);
    }

    public static int[] getGridSlots() {
        return new int[]{
            1, 2, 3, 4, 5,
            10, 11, 12, 13, 14,
            19, 20, 21, 22, 23,
            28, 29, 30, 31, 32,
            37, 38, 39, 40, 41
        };
    }

    public static int getOutputSlot() {
        return 25;
    }
}
