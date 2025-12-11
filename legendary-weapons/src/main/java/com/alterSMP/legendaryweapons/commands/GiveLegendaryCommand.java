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
            sender.sendMessage(ChatColor.RED + "Usage: /givelegendary <id> [player]");
            sender.sendMessage(ChatColor.YELLOW + "Available IDs:");
            sender.sendMessage(ChatColor.GRAY + "  holy_moonlight_sword, pheonix_grace, tempestbreaker_spear,");
            sender.sendMessage(ChatColor.GRAY + "  thousand_demon_daggers, divine_axe_rhitta, chains_of_eternity,");
            sender.sendMessage(ChatColor.GRAY + "  forge_boots, celestial_aegis_shield, chrono_blade,");
            sender.sendMessage(ChatColor.GRAY + "  dragonborn_blade, soul_devourer, forge_pickaxe,");
            sender.sendMessage(ChatColor.GRAY + "  forge_chestplate, forge_leggings, forge_helmet,");
            sender.sendMessage(ChatColor.GRAY + "  lantern_of_lost_names, rift_key_of_endkeeper, chaos_dice_of_fate");
            return true;
        }

        String legendaryId = args[0].toLowerCase();

        // Find legendary type by ID (case-insensitive)
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
                sender.sendMessage(ChatColor.RED + "Console must specify a player: /givelegendary <id> <player>");
                return true;
            }
            target = (Player) sender;
        }

        // Create and give the legendary weapon (does NOT mark as crafted)
        ItemStack legendary = plugin.getItemFactory().createLegendary(legendaryType);
        target.getInventory().addItem(legendary);

        // Send confirmation messages
        if (target.equals(sender)) {
            sender.sendMessage(ChatColor.GREEN + "You have been given " + ChatColor.GOLD + legendaryType.getDisplayName());
        } else {
            sender.sendMessage(ChatColor.GREEN + "Gave " + ChatColor.GOLD + legendaryType.getDisplayName() + ChatColor.GREEN + " to " + target.getName());
            target.sendMessage(ChatColor.GREEN + "You have been given " + ChatColor.GOLD + legendaryType.getDisplayName() + ChatColor.GREEN + " by " + sender.getName());
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
