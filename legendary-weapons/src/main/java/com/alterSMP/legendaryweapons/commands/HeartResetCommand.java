package com.alterSMP.legendaryweapons.commands;

import com.alterSMP.legendaryweapons.LegendaryWeaponsPlugin;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class HeartResetCommand implements CommandExecutor, TabCompleter {

    private final LegendaryWeaponsPlugin plugin;

    public HeartResetCommand(LegendaryWeaponsPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("legendaryweapons.admin")) {
            sender.sendMessage(ChatColor.RED + "You don't have permission to use this command.");
            return true;
        }

        if (args.length == 0) {
            sender.sendMessage(ChatColor.RED + "Usage: /heartreset <player|all>");
            return true;
        }

        if (args[0].equalsIgnoreCase("all")) {
            plugin.getHeartManager().resetAllHearts();
            sender.sendMessage(ChatColor.GREEN + "All heart data has been reset!");
            Bukkit.broadcastMessage(ChatColor.YELLOW + "All stolen hearts have been reset by an admin.");
            return true;
        }

        // Reset specific player
        Player target = Bukkit.getPlayer(args[0]);
        if (target == null) {
            // Try offline player
            @SuppressWarnings("deprecation")
            org.bukkit.OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(args[0]);
            if (offlinePlayer.hasPlayedBefore() || offlinePlayer.isOnline()) {
                plugin.getHeartManager().resetPlayerHearts(offlinePlayer.getUniqueId());
                sender.sendMessage(ChatColor.GREEN + "Heart data reset for " + args[0] + "!");
            } else {
                sender.sendMessage(ChatColor.RED + "Player not found: " + args[0]);
            }
            return true;
        }

        plugin.getHeartManager().resetPlayerHearts(target.getUniqueId());
        sender.sendMessage(ChatColor.GREEN + "Heart data reset for " + target.getName() + "!");
        target.sendMessage(ChatColor.GREEN + "Your heart data has been reset by an admin. Max health restored to 10 hearts.");
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        if (args.length == 1) {
            String partial = args[0].toLowerCase();
            if ("all".startsWith(partial)) {
                completions.add("all");
            }
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (player.getName().toLowerCase().startsWith(partial)) {
                    completions.add(player.getName());
                }
            }
        }
        return completions;
    }
}
