package com.alterSMP.legendaryweapons.commands;

import com.alterSMP.legendaryweapons.LegendaryWeaponsPlugin;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class KResetPlayerCommand implements CommandExecutor, TabCompleter {

    private final LegendaryWeaponsPlugin plugin;

    public KResetPlayerCommand(LegendaryWeaponsPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("legendaryweapons.kresetplayer")) {
            sender.sendMessage(ChatColor.RED + "You do not have permission to use this command.");
            return true;
        }

        if (args.length == 0) {
            sender.sendMessage(ChatColor.RED + "Usage: /kresetplayer <player>");
            return true;
        }

        // Try to find the player (online or offline)
        Player onlinePlayer = Bukkit.getPlayer(args[0]);
        OfflinePlayer targetPlayer;

        if (onlinePlayer != null) {
            targetPlayer = onlinePlayer;
        } else {
            // Try to find offline player
            targetPlayer = Bukkit.getOfflinePlayer(args[0]);
            if (!targetPlayer.hasPlayedBefore() && !targetPlayer.isOnline()) {
                sender.sendMessage(ChatColor.RED + "Player not found: " + args[0]);
                sender.sendMessage(ChatColor.YELLOW + "Note: Player must have joined the server at least once.");
                return true;
            }
        }

        UUID playerUUID = targetPlayer.getUniqueId();
        String playerName = targetPlayer.getName();

        // Get list of legendaries this player has crafted
        List<String> craftedLegendaries = new ArrayList<>();
        for (String legendaryId : plugin.getDataManager().getAllCraftedLegendaries()) {
            UUID crafter = plugin.getDataManager().getCrafter(legendaryId);
            if (crafter != null && crafter.equals(playerUUID)) {
                craftedLegendaries.add(legendaryId);
            }
        }

        if (craftedLegendaries.isEmpty()) {
            sender.sendMessage(ChatColor.YELLOW + playerName + " has not crafted any legendary weapons.");
            return true;
        }

        // Reset all legendaries crafted by this player
        for (String legendaryId : craftedLegendaries) {
            plugin.getDataManager().resetLegendaryCrafting(legendaryId);
        }

        // Send confirmation messages
        sender.sendMessage(ChatColor.GREEN + "Reset " + craftedLegendaries.size() + " legendary weapon(s) for " + playerName);
        sender.sendMessage(ChatColor.GRAY + "Weapons reset: " + String.join(", ", craftedLegendaries));

        // Notify the player if online
        if (onlinePlayer != null) {
            onlinePlayer.sendMessage(ChatColor.YELLOW + "Your legendary weapon crafting history has been reset by " + sender.getName());
            onlinePlayer.sendMessage(ChatColor.YELLOW + "You can now craft legendary weapons again!");
        }

        plugin.getLogger().info(sender.getName() + " reset legendary crafting for " + playerName + " (" + craftedLegendaries.size() + " weapons)");

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            // Tab complete online player names
            String partial = args[0].toLowerCase();
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (player.getName().toLowerCase().startsWith(partial)) {
                    completions.add(player.getName());
                }
            }
        }

        return completions;
    }
}
