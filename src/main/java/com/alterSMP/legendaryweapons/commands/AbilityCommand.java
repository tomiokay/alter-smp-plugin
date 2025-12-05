package com.alterSMP.legendaryweapons.commands;

import com.alterSMP.legendaryweapons.LegendaryWeaponsPlugin;
import com.alterSMP.legendaryweapons.items.LegendaryItemFactory;
import com.alterSMP.legendaryweapons.items.LegendaryType;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class AbilityCommand implements CommandExecutor {

    private final LegendaryWeaponsPlugin plugin;

    public AbilityCommand(LegendaryWeaponsPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Only players can use abilities.");
            return true;
        }

        Player player = (Player) sender;

        if (args.length == 0) {
            player.sendMessage(ChatColor.RED + "Usage: /ability <1|2>");
            return true;
        }

        int abilityNum;
        try {
            abilityNum = Integer.parseInt(args[0]);
            if (abilityNum != 1 && abilityNum != 2) {
                throw new NumberFormatException();
            }
        } catch (NumberFormatException e) {
            player.sendMessage(ChatColor.RED + "Usage: /ability <1|2>");
            return true;
        }

        // Find legendary in player's hand or equipment
        String legendaryId = findLegendaryInUse(player);

        if (legendaryId == null) {
            player.sendMessage(ChatColor.RED + "You must be holding or wearing a legendary weapon!");
            return true;
        }

        // Check if this legendary has active abilities (some armor pieces don't use /ability)
        LegendaryType type = LegendaryType.fromId(legendaryId);
        if (type == LegendaryType.COPPER_BOOTS || type == LegendaryType.COPPER_CHESTPLATE
                || type == LegendaryType.COPPER_LEGGINGS || type == LegendaryType.COPPER_HELMET) {
            player.sendMessage(ChatColor.RED + "This legendary only has passive abilities!");
            return true;
        }

        // Check cooldown - but skip for Chrono Blade ability 2 if position is already marked
        // Also skip for Chaos Dice ability 2 if player has free scans active
        boolean skipCooldownCheck = (type == LegendaryType.CHRONO_BLADE && abilityNum == 2
            && plugin.getAbilityManager().hasChronoShiftMarked(player.getUniqueId()))
            || (type == LegendaryType.CHAOS_DICE_OF_FATE && abilityNum == 2
            && plugin.getAbilityManager().hasFreeScan(player.getUniqueId()));

        if (!skipCooldownCheck && plugin.getCooldownManager().isOnCooldown(player.getUniqueId(), legendaryId, abilityNum)) {
            int remaining = plugin.getCooldownManager().getRemainingCooldown(
                player.getUniqueId(), legendaryId, abilityNum);
            player.sendMessage(ChatColor.YELLOW + "Ability on cooldown: " +
                ChatColor.RED + remaining + "s");
            return true;
        }

        // Execute ability
        boolean success = plugin.getAbilityManager().executeAbility(player, legendaryId, abilityNum);

        if (!success) {
            player.sendMessage(ChatColor.RED + "Failed to execute ability!");
        }

        return true;
    }

    private String findLegendaryInUse(Player player) {
        // Check main hand
        ItemStack mainHand = player.getInventory().getItemInMainHand();
        String legendaryId = LegendaryItemFactory.getLegendaryId(mainHand);
        if (legendaryId != null) {
            return legendaryId;
        }

        // Check offhand (for shield)
        ItemStack offHand = player.getInventory().getItemInOffHand();
        legendaryId = LegendaryItemFactory.getLegendaryId(offHand);
        if (legendaryId != null) {
            return legendaryId;
        }

        // Check armor slots
        ItemStack helmet = player.getInventory().getHelmet();
        legendaryId = LegendaryItemFactory.getLegendaryId(helmet);
        if (legendaryId != null) {
            return legendaryId;
        }

        ItemStack chestplate = player.getInventory().getChestplate();
        legendaryId = LegendaryItemFactory.getLegendaryId(chestplate);
        if (legendaryId != null) {
            return legendaryId;
        }

        ItemStack leggings = player.getInventory().getLeggings();
        legendaryId = LegendaryItemFactory.getLegendaryId(leggings);
        if (legendaryId != null) {
            return legendaryId;
        }

        ItemStack boots = player.getInventory().getBoots();
        legendaryId = LegendaryItemFactory.getLegendaryId(boots);
        if (legendaryId != null) {
            return legendaryId;
        }

        return null;
    }
}
