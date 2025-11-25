package com.alterSMP.legendaryweapons.commands;

import com.alterSMP.legendaryweapons.LegendaryWeaponsPlugin;
import com.alterSMP.legendaryweapons.items.LegendaryType;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class GiveWeaponCommand implements CommandExecutor, TabCompleter {

    private final LegendaryWeaponsPlugin plugin;

    public GiveWeaponCommand(LegendaryWeaponsPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("legendaryweapons.giveweapon")) {
            sender.sendMessage(ChatColor.RED + "You do not have permission to use this command.");
            return true;
        }

        if (args.length < 1) {
            sender.sendMessage(ChatColor.RED + "Usage: /giveweapon <weapon> [player]");
            sender.sendMessage(ChatColor.YELLOW + "Available weapons:");
            for (LegendaryType type : LegendaryType.values()) {
                sender.sendMessage(ChatColor.GRAY + "  - " + type.getId());
            }
            return true;
        }

        // Find legendary type
        String weaponId = args[0].toLowerCase();
        LegendaryType legendaryType = null;

        for (LegendaryType type : LegendaryType.values()) {
            if (type.getId().equalsIgnoreCase(weaponId)) {
                legendaryType = type;
                break;
            }
        }

        if (legendaryType == null) {
            sender.sendMessage(ChatColor.RED + "Unknown weapon: " + args[0]);
            sender.sendMessage(ChatColor.YELLOW + "Available weapons:");
            for (LegendaryType type : LegendaryType.values()) {
                sender.sendMessage(ChatColor.GRAY + "  - " + type.getId());
            }
            return true;
        }

        // Determine target player
        Player target;
        if (args.length >= 2) {
            target = Bukkit.getPlayer(args[1]);
            if (target == null) {
                sender.sendMessage(ChatColor.RED + "Player not found: " + args[1]);
                return true;
            }
        } else {
            if (!(sender instanceof Player)) {
                sender.sendMessage(ChatColor.RED + "Console must specify a player: /giveweapon <weapon> <player>");
                return true;
            }
            target = (Player) sender;
        }

        // Create and give the legendary weapon
        ItemStack legendary = plugin.getItemFactory().createLegendary(legendaryType);
        target.getInventory().addItem(legendary);

        // Send confirmation messages
        if (target.equals(sender)) {
            sender.sendMessage(ChatColor.GREEN + "You have been given " + legendaryType.getDisplayName());
        } else {
            sender.sendMessage(ChatColor.GREEN + "Gave " + legendaryType.getDisplayName() + ChatColor.GREEN + " to " + target.getName());
            target.sendMessage(ChatColor.GREEN + "You have been given " + legendaryType.getDisplayName() + ChatColor.GREEN + " by " + sender.getName());
        }

        plugin.getLogger().info(sender.getName() + " gave " + legendaryType.getId() + " to " + target.getName());

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            // Tab complete weapon names
            String partial = args[0].toLowerCase();
            for (LegendaryType type : LegendaryType.values()) {
                if (type.getId().toLowerCase().startsWith(partial)) {
                    completions.add(type.getId());
                }
            }
        } else if (args.length == 2) {
            // Tab complete player names
            String partial = args[1].toLowerCase();
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (player.getName().toLowerCase().startsWith(partial)) {
                    completions.add(player.getName());
                }
            }
        }

        return completions;
    }
}
