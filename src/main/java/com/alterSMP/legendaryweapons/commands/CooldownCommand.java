package com.alterSMP.legendaryweapons.commands;

import com.alterSMP.legendaryweapons.LegendaryWeaponsPlugin;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class CooldownCommand implements CommandExecutor {

    private final LegendaryWeaponsPlugin plugin;

    public CooldownCommand(LegendaryWeaponsPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("legendaryweapons.cooldown")) {
            sender.sendMessage(ChatColor.RED + "You do not have permission to use this command.");
            return true;
        }

        Player target;

        // If no args, clear sender's cooldowns (must be player)
        if (args.length == 0) {
            if (!(sender instanceof Player)) {
                sender.sendMessage(ChatColor.RED + "Console must specify a player: /cooldown <player>");
                return true;
            }
            target = (Player) sender;
        }
        // If args provided, clear specified player's cooldowns
        else {
            target = Bukkit.getPlayer(args[0]);
            if (target == null) {
                sender.sendMessage(ChatColor.RED + "Player not found: " + args[0]);
                return true;
            }
        }

        // Clear all cooldowns for the target player
        plugin.getCooldownManager().clearAllCooldowns(target.getUniqueId());

        // Send confirmation messages
        if (target.equals(sender)) {
            sender.sendMessage(ChatColor.GREEN + "All your ability cooldowns have been cleared!");
        } else {
            sender.sendMessage(ChatColor.GREEN + "Cleared all cooldowns for " + target.getName());
            target.sendMessage(ChatColor.GREEN + "Your ability cooldowns have been cleared by " + sender.getName());
        }

        plugin.getLogger().info(sender.getName() + " cleared cooldowns for " + target.getName());

        return true;
    }
}
