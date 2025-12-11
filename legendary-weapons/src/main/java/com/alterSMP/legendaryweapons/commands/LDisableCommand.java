package com.alterSMP.legendaryweapons.commands;

import com.alterSMP.legendaryweapons.LegendaryWeaponsPlugin;
import com.alterSMP.legendaryweapons.items.LegendaryType;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class LDisableCommand implements CommandExecutor, TabCompleter {

    private final LegendaryWeaponsPlugin plugin;

    public LDisableCommand(LegendaryWeaponsPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("legendaryweapons.disable")) {
            sender.sendMessage(ChatColor.RED + "You do not have permission to use this command.");
            return true;
        }

        if (args.length < 1) {
            sender.sendMessage(ChatColor.RED + "Usage: /ldisable <legendary_id|list>");
            sender.sendMessage(ChatColor.YELLOW + "Toggle a legendary's craftability or view disabled legendaries.");
            sender.sendMessage(ChatColor.GRAY + "Use /ldisable list to see all disabled legendaries.");
            return true;
        }

        if (args[0].equalsIgnoreCase("list")) {
            Set<String> disabled = plugin.getDataManager().getDisabledLegendaries();
            if (disabled.isEmpty()) {
                sender.sendMessage(ChatColor.GREEN + "No legendaries are currently disabled.");
            } else {
                sender.sendMessage(ChatColor.YELLOW + "Disabled legendaries (" + disabled.size() + "):");
                for (String id : disabled) {
                    LegendaryType type = LegendaryType.fromId(id);
                    String displayName = type != null ? type.getDisplayName() : id;
                    sender.sendMessage(ChatColor.GRAY + "  - " + ChatColor.RED + displayName + ChatColor.GRAY + " (" + id + ")");
                }
            }
            return true;
        }

        String legendaryId = args[0].toLowerCase();

        // Find legendary type by ID
        LegendaryType legendaryType = null;
        for (LegendaryType type : LegendaryType.values()) {
            if (type.getId().equalsIgnoreCase(legendaryId)) {
                legendaryType = type;
                break;
            }
        }

        if (legendaryType == null) {
            sender.sendMessage(ChatColor.RED + "Unknown legendary: " + legendaryId);
            sender.sendMessage(ChatColor.YELLOW + "Use tab completion to see available IDs.");
            return true;
        }

        boolean nowDisabled = plugin.getDataManager().toggleLegendaryDisabled(legendaryType.getId());

        if (nowDisabled) {
            sender.sendMessage(ChatColor.RED + legendaryType.getDisplayName() + ChatColor.YELLOW + " is now " + ChatColor.RED + "DISABLED" + ChatColor.YELLOW + " and cannot be crafted.");
        } else {
            sender.sendMessage(ChatColor.GREEN + legendaryType.getDisplayName() + ChatColor.YELLOW + " is now " + ChatColor.GREEN + "ENABLED" + ChatColor.YELLOW + " and can be crafted.");
        }

        plugin.getLogger().info(sender.getName() + (nowDisabled ? " disabled " : " enabled ") + legendaryType.getId());

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            String partial = args[0].toLowerCase();

            // Add "list" option
            if ("list".startsWith(partial)) {
                completions.add("list");
            }

            // Tab complete legendary IDs
            for (LegendaryType type : LegendaryType.values()) {
                if (type.getId().toLowerCase().startsWith(partial)) {
                    completions.add(type.getId());
                }
            }
        }

        return completions;
    }
}
