package com.alterSMP.combatlogger;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class ToggleCombatTimerCommand implements CommandExecutor {

    private final CombatLoggerPlugin plugin;

    public ToggleCombatTimerCommand(CombatLoggerPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Only players can use this command!");
            return true;
        }

        Player player = (Player) sender;
        boolean enabled = plugin.getCombatManager().toggleCombatTimer(player.getUniqueId());

        if (enabled) {
            player.sendMessage(ChatColor.GREEN + "Combat timer display " + ChatColor.WHITE + "enabled!");
        } else {
            player.sendMessage(ChatColor.RED + "Combat timer display " + ChatColor.WHITE + "disabled!");
        }

        return true;
    }
}
