package com.alterSMP.combatlogger;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.time.Duration;

public class TogglePvPCommand implements CommandExecutor {

    private final CombatLoggerPlugin plugin;

    public TogglePvPCommand(CombatLoggerPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("combatlogger.togglepvp")) {
            sender.sendMessage(Component.text("You don't have permission to use this command!", NamedTextColor.RED));
            return true;
        }

        boolean newState = plugin.getCombatManager().togglePvP();

        // Create title components
        Component title;
        Component subtitle;

        if (newState) {
            title = Component.text("PVP ENABLED", NamedTextColor.RED, TextDecoration.BOLD);
            subtitle = Component.text("Watch your back!", NamedTextColor.GRAY);
        } else {
            title = Component.text("PVP DISABLED", NamedTextColor.GREEN, TextDecoration.BOLD);
            subtitle = Component.text("Peace has been restored.", NamedTextColor.GRAY);
        }

        // Create title with timing
        Title.Times times = Title.Times.times(
            Duration.ofMillis(500),   // Fade in
            Duration.ofSeconds(3),     // Stay
            Duration.ofMillis(500)     // Fade out
        );
        Title titleObj = Title.title(title, subtitle, times);

        // Broadcast to all players
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.showTitle(titleObj);
        }

        // Also send chat message
        String chatMessage = newState ?
            "§c§l⚔ PVP has been ENABLED! §7Watch your back!" :
            "§a§l☮ PVP has been DISABLED! §7Peace has been restored.";
        Bukkit.broadcastMessage(chatMessage);

        return true;
    }
}
