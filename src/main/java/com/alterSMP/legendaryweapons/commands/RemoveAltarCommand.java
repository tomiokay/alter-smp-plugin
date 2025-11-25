package com.alterSMP.legendaryweapons.commands;

import com.alterSMP.legendaryweapons.LegendaryWeaponsPlugin;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class RemoveAltarCommand implements CommandExecutor {

    private final LegendaryWeaponsPlugin plugin;

    public RemoveAltarCommand(LegendaryWeaponsPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("legendaryweapons.removealtar")) {
            sender.sendMessage(ChatColor.RED + "You do not have permission to use this command.");
            return true;
        }

        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Only players can use this command.");
            return true;
        }

        Player player = (Player) sender;

        // Get the block the player is looking at
        Block targetBlock = player.getTargetBlockExact(5);

        if (targetBlock == null) {
            player.sendMessage(ChatColor.RED + "You must be looking at a block.");
            return true;
        }

        Location location = targetBlock.getLocation();

        // Check if it's an altar
        if (!plugin.getAltarManager().isAltar(location)) {
            player.sendMessage(ChatColor.RED + "That block is not a Legendary Altar.");
            return true;
        }

        // Remove the altar
        plugin.getAltarManager().unregisterAltar(location);
        targetBlock.setType(org.bukkit.Material.AIR);

        // Drop the altar item
        location.getWorld().dropItemNaturally(location, plugin.getItemFactory().createAltarItem());

        player.sendMessage(ChatColor.GREEN + "Legendary Altar removed successfully!");
        plugin.getLogger().info(player.getName() + " removed a Legendary Altar at " + location);

        return true;
    }
}
