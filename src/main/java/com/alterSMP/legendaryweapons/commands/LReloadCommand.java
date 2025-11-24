package com.alterSMP.legendaryweapons.commands;

import com.alterSMP.legendaryweapons.LegendaryWeaponsPlugin;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class LReloadCommand implements CommandExecutor {

    private final LegendaryWeaponsPlugin plugin;

    public LReloadCommand(LegendaryWeaponsPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("legendaryweapons.reload")) {
            sender.sendMessage(ChatColor.RED + "You do not have permission to use this command.");
            return true;
        }

        sender.sendMessage(ChatColor.YELLOW + "Reloading Legendary Weapons configuration...");

        try {
            plugin.getConfigManager().reloadConfiguration();
            sender.sendMessage(ChatColor.GREEN + "Configuration reloaded successfully!");
            sender.sendMessage(ChatColor.GRAY + "Note: Some changes may require a server restart to fully apply.");
        } catch (Exception e) {
            sender.sendMessage(ChatColor.RED + "Error reloading configuration: " + e.getMessage());
            plugin.getLogger().severe("Error reloading configuration:");
            e.printStackTrace();
        }

        return true;
    }
}
