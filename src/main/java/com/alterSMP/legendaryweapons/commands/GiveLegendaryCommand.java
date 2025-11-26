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
            sender.sendMessage(ChatColor.RED + "Usage: /givelegendary <name> [player]");
            sender.sendMessage(ChatColor.YELLOW + "Available legendaries:");
            for (LegendaryType type : LegendaryType.values()) {
                sender.sendMessage(ChatColor.GRAY + "  - " + type.getDisplayName());
            }
            return true;
        }

        // Determine target player and legendary name
        Player target;
        String legendaryName;

        if (args.length == 1) {
            // /givelegendary <name> - give to self or error if console
            if (!(sender instanceof Player)) {
                sender.sendMessage(ChatColor.RED + "Console must specify a player: /givelegendary <name> <player>");
                return true;
            }
            target = (Player) sender;
            legendaryName = args[0];
        } else {
            // /givelegendary <name> <player> - name may have spaces, last arg is player
            // Check if last arg is an online player
            Player potentialTarget = Bukkit.getPlayer(args[args.length - 1]);
            if (potentialTarget != null && args.length > 1) {
                // Last arg is a player, everything before is the legendary name
                target = potentialTarget;
                StringBuilder nameBuilder = new StringBuilder();
                for (int i = 0; i < args.length - 1; i++) {
                    if (i > 0) nameBuilder.append(" ");
                    nameBuilder.append(args[i]);
                }
                legendaryName = nameBuilder.toString();
            } else {
                // No player specified or last arg is not a player, treat all as legendary name
                if (!(sender instanceof Player)) {
                    sender.sendMessage(ChatColor.RED + "Console must specify a player: /givelegendary <name> <player>");
                    return true;
                }
                target = (Player) sender;
                legendaryName = String.join(" ", args);
            }
        }

        // Find legendary type by display name (case-insensitive)
        LegendaryType legendaryType = null;
        for (LegendaryType type : LegendaryType.values()) {
            if (type.getDisplayName().equalsIgnoreCase(legendaryName)) {
                legendaryType = type;
                break;
            }
        }

        if (legendaryType == null) {
            sender.sendMessage(ChatColor.RED + "Unknown legendary: " + legendaryName);
            sender.sendMessage(ChatColor.YELLOW + "Available legendaries:");
            for (LegendaryType type : LegendaryType.values()) {
                sender.sendMessage(ChatColor.GRAY + "  - " + type.getDisplayName());
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

        // Build current input string
        String currentInput = String.join(" ", args).toLowerCase();

        // Check if any legendary display name starts with the current input
        boolean foundMatch = false;
        for (LegendaryType type : LegendaryType.values()) {
            String displayName = type.getDisplayName().toLowerCase();
            if (displayName.startsWith(currentInput)) {
                // Return the next word(s) needed
                String remaining = type.getDisplayName().substring(currentInput.length()).trim();
                if (!remaining.isEmpty()) {
                    String[] remainingParts = remaining.split(" ");
                    if (args.length > 0 && !args[args.length - 1].isEmpty()) {
                        // Complete current word
                        completions.add(args[args.length - 1] + remainingParts[0]);
                    } else {
                        completions.add(remainingParts[0]);
                    }
                }
                foundMatch = true;
            }
        }

        // If no partial match, show all display names for first arg
        if (args.length == 1 && !foundMatch) {
            String partial = args[0].toLowerCase();
            for (LegendaryType type : LegendaryType.values()) {
                String displayName = type.getDisplayName();
                // For names with spaces, just show first word if it matches
                String firstWord = displayName.split(" ")[0];
                if (firstWord.toLowerCase().startsWith(partial)) {
                    completions.add(firstWord);
                }
            }
        }

        // Also suggest player names if current input matches a complete legendary name
        for (LegendaryType type : LegendaryType.values()) {
            if (type.getDisplayName().equalsIgnoreCase(currentInput.trim())) {
                // Full legendary name entered, suggest players
                for (Player player : Bukkit.getOnlinePlayers()) {
                    completions.add(player.getName());
                }
                break;
            }
        }

        return completions;
    }
}
