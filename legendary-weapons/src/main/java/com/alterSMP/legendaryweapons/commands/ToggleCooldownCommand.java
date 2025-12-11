package com.alterSMP.legendaryweapons.commands;

import com.alterSMP.legendaryweapons.LegendaryWeaponsPlugin;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class ToggleCooldownCommand implements CommandExecutor {

    private final LegendaryWeaponsPlugin plugin;

    public ToggleCooldownCommand(LegendaryWeaponsPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Only players can use this command!");
            return true;
        }

        Player player = (Player) sender;
        boolean enabled = plugin.getCooldownManager().toggleCooldownDisplay(player.getUniqueId());

        if (enabled) {
            player.sendMessage(ChatColor.GREEN + "Ability cooldown display " + ChatColor.WHITE + "enabled!");
        } else {
            player.sendMessage(ChatColor.RED + "Ability cooldown display " + ChatColor.WHITE + "disabled!");
        }

        return true;
    }
}
