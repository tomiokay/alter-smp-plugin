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
import java.util.List;

public class GiveLegendaryCommand implements CommandExecutor, TabCompleter {

    private final LegendaryWeaponsPlugin plugin;

    public GiveLegendaryCommand(LegendaryWeaponsPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("legendaryweapons.givelegendary")) {
            sender.sendMessage(ChatColor.RED + "You do not have permission to use this command.");
            return true;
        }

        if (args.length < 1) {
            sender.sendMessage(ChatColor.RED + "Usage: /givelegendary <legendary_id> [player]");
            sender.sendMessage(ChatColor.YELLOW + "Available legendary IDs:");
            for (LegendaryType type : LegendaryType.values()) {
                sender.sendMessage(ChatColor.GRAY + "  - " + type.getId());
            }
            return true;
        }

        // Determine if first arg is player or legendary_id
        Player target;
        String legendaryId;

        if (args.length == 1) {
            // /givelegendary <legendary_id> - give to self or error if console
            if (!(sender instanceof Player)) {
                sender.sendMessage(ChatColor.RED + "Console must specify a player: /givelegendary <legendary_id> <player>");
                return true;
            }
            target = (Player) sender;
            legendaryId = args[0];
        } else {
            // /givelegendary <legendary_id> <player>
            legendaryId = args[0];
            target = Bukkit.getPlayer(args[1]);
            if (target == null) {
                sender.sendMessage(ChatColor.RED + "Player not found: " + args[1]);
                return true;
            }
        }

        // Find legendary type
        LegendaryType legendaryType = null;
        for (LegendaryType type : LegendaryType.values()) {
            if (type.getId().equalsIgnoreCase(legendaryId)) {
                legendaryType = type;
                break;
            }
        }

        if (legendaryType == null) {
            sender.sendMessage(ChatColor.RED + "Unknown legendary ID: " + legendaryId);
            sender.sendMessage(ChatColor.YELLOW + "Available legendary IDs:");
            for (LegendaryType type : LegendaryType.values()) {
                sender.sendMessage(ChatColor.GRAY + "  - " + type.getId());
            }
            return true;
        }

        // Create and give the legendary weapon (does NOT mark as crafted)
        ItemStack legendary = plugin.getItemFactory().createLegendary(legendaryType);
        target.getInventory().addItem(legendary);

        // Send confirmation messages
        if (target.equals(sender)) {
            sender.sendMessage(ChatColor.GREEN + "You have been given " + legendaryType.getDisplayName());
        } else {
            sender.sendMessage(ChatColor.GREEN + "Gave " + legendaryType.getDisplayName() + ChatColor.GREEN + " to " + target.getName());
            target.sendMessage(ChatColor.GREEN + "You have been given " + legendaryType.getDisplayName() + ChatColor.GREEN + " by " + sender.getName());
        }

        plugin.getLogger().info(sender.getName() + " gave " + legendaryType.getId() + " to " + target.getName() + " (NOT marked as crafted)");

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            // Tab complete legendary IDs
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
