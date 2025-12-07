package com.alterSMP.legendaryweapons.commands;

import com.alterSMP.legendaryweapons.LegendaryWeaponsPlugin;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class KResetCommand implements CommandExecutor {

    private final LegendaryWeaponsPlugin plugin;

    public KResetCommand(LegendaryWeaponsPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("legendaryweapons.kreset")) {
            sender.sendMessage(ChatColor.RED + "You do not have permission to use this command.");
            return true;
        }

        // Reset all crafting history
        plugin.getDataManager().resetAllCrafting();

        // Broadcast to all players
        Bukkit.broadcastMessage(ChatColor.RED + "" + ChatColor.BOLD + "LEGENDARY CRAFTING RESET");
        Bukkit.broadcastMessage(ChatColor.YELLOW + "All players can now craft legendary weapons again!");

        plugin.getLogger().warning("Legendary crafting was reset by " + sender.getName());

        return true;
    }
}
