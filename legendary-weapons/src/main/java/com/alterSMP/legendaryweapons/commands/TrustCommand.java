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

public class TrustCommand implements CommandExecutor, TabCompleter {

    private final LegendaryWeaponsPlugin plugin;

    public TrustCommand(LegendaryWeaponsPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Only players can use this command.");
            return true;
        }

        Player player = (Player) sender;

        if (args.length == 0) {
            sender.sendMessage(ChatColor.RED + "Usage: /trust <player>");
            return true;
        }

        Player target = Bukkit.getPlayer(args[0]);
        if (target == null) {
            sender.sendMessage(ChatColor.RED + "Player not found: " + args[0]);
            return true;
        }

        if (target.equals(player)) {
            sender.sendMessage(ChatColor.RED + "You cannot trust yourself.");
            return true;
        }

        // Toggle trust
        boolean nowTrusted = plugin.getTrustManager().toggleTrust(player.getUniqueId(), target.getUniqueId());

        if (nowTrusted) {
            player.sendMessage(ChatColor.GREEN + "You now trust " + target.getName() + ". Your legendary abilities will not affect them.");
        } else {
            player.sendMessage(ChatColor.YELLOW + "You no longer trust " + target.getName() + ". Your legendary abilities can affect them again.");
        }

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            String partial = args[0].toLowerCase();
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (player.getName().toLowerCase().startsWith(partial) && !player.equals(sender)) {
                    completions.add(player.getName());
                }
            }
        }

        return completions;
    }
}
