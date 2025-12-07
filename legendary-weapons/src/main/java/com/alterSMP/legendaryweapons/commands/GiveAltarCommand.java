package com.alterSMP.legendaryweapons.commands;

import com.alterSMP.legendaryweapons.LegendaryWeaponsPlugin;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class GiveAltarCommand implements CommandExecutor {

    private final LegendaryWeaponsPlugin plugin;

    public GiveAltarCommand(LegendaryWeaponsPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("legendaryweapons.giveforge")) {
            sender.sendMessage(ChatColor.RED + "You do not have permission to use this command.");
            return true;
        }

        Player target;

        if (args.length == 0) {
            // Give to self
            if (!(sender instanceof Player)) {
                sender.sendMessage(ChatColor.RED + "Console must specify a player: /giveforge <player>");
                return true;
            }
            target = (Player) sender;
        } else {
            // Give to specified player
            target = Bukkit.getPlayer(args[0]);
            if (target == null) {
                sender.sendMessage(ChatColor.RED + "Player not found: " + args[0]);
                return true;
            }
        }

        ItemStack forge = plugin.getItemFactory().createForgeItem();
        target.getInventory().addItem(forge);

        target.sendMessage(ChatColor.DARK_PURPLE + "You have received a " +
            ChatColor.BOLD + "Legendary Forge" + ChatColor.DARK_PURPLE + "!");

        if (!target.equals(sender)) {
            sender.sendMessage(ChatColor.GREEN + "Gave Legendary Forge to " + target.getName());
        }

        return true;
    }
}
