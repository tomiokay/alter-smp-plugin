package com.alterSMP.legendaryweapons.listeners;

import com.alterSMP.legendaryweapons.LegendaryWeaponsPlugin;
import com.alterSMP.legendaryweapons.items.LegendaryItemFactory;
import com.alterSMP.legendaryweapons.items.LegendaryType;
import org.bukkit.ChatColor;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;

/**
 * Handles the Endbound Soulkeeper passive for Creation Splitter.
 *
 * - Heart stealing on player kills
 * - Heart return on holder death
 * - Apply heart modifiers on join
 */
public class CreationSplitterListener implements Listener {

    private final LegendaryWeaponsPlugin plugin;

    public CreationSplitterListener(LegendaryWeaponsPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Check if player has Creation Splitter in their inventory.
     */
    private boolean hasCreationSplitterInInventory(Player player) {
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null) {
                String legendaryId = LegendaryItemFactory.getLegendaryId(item);
                if (legendaryId != null && legendaryId.equals(LegendaryType.VOIDRENDER.getId())) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Check if player is holding Creation Splitter in main hand.
     */
    private boolean isHoldingCreationSplitter(Player player) {
        ItemStack mainHand = player.getInventory().getItemInMainHand();
        String legendaryId = LegendaryItemFactory.getLegendaryId(mainHand);
        return legendaryId != null && legendaryId.equals(LegendaryType.VOIDRENDER.getId());
    }

    /**
     * Handle player kill - steal hearts if holding Creation Splitter.
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerKill(EntityDeathEvent event) {
        if (!(event.getEntity() instanceof Player)) return;

        Player victim = (Player) event.getEntity();
        Player killer = victim.getKiller();

        if (killer == null) return;
        if (killer.equals(victim)) return; // No self-kills

        // Check if killer is HOLDING Creation Splitter (not just in inventory)
        if (!isHoldingCreationSplitter(killer)) return;

        // Trust check - don't steal hearts from trusted players
        if (plugin.getTrustManager().isTrusted(killer, victim)) {
            return;
        }

        // Try to steal a heart
        boolean stolen = plugin.getHeartManager().onPlayerKill(killer, victim);

        if (stolen) {
            int heartsStolen = plugin.getHeartManager().getHeartsStolen(killer.getUniqueId());

            // Notify killer
            killer.sendMessage(ChatColor.DARK_PURPLE + "☠ " + ChatColor.LIGHT_PURPLE + "Endbound Soulkeeper: " +
                ChatColor.WHITE + "Stole a heart from " + victim.getName() + "!");
            killer.sendMessage(ChatColor.DARK_PURPLE + "Hearts stolen: " + ChatColor.WHITE + heartsStolen + "/5");

            // Epic particles and sound
            killer.getWorld().spawnParticle(Particle.REVERSE_PORTAL, killer.getLocation().add(0, 1, 0), 100, 0.5, 0.5, 0.5, 0.5);
            killer.getWorld().spawnParticle(Particle.DRAGON_BREATH, killer.getLocation().add(0, 1, 0), 50, 0.5, 0.5, 0.5, 0.1);
            killer.playSound(killer.getLocation(), Sound.ENTITY_WITHER_SPAWN, 0.5f, 1.5f);
            killer.playSound(killer.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 0.3f, 1.5f);

            // Notify victim
            victim.sendMessage(ChatColor.DARK_RED + "☠ " + ChatColor.RED + "Your heart was stolen by " +
                killer.getName() + "'s Creation Splitter!");
            victim.sendMessage(ChatColor.RED + "You now have " + ChatColor.WHITE +
                (10 - plugin.getHeartManager().getHeartsLost(victim.getUniqueId())) + " max hearts.");
        } else {
            // Already stolen from this player or at max
            int heartsStolen = plugin.getHeartManager().getHeartsStolen(killer.getUniqueId());
            if (heartsStolen >= 5) {
                killer.sendMessage(ChatColor.GRAY + "You have already reached maximum stolen hearts (5).");
            } else if (plugin.getHeartManager().hasAlreadyStolenFrom(killer.getUniqueId(), victim.getUniqueId())) {
                killer.sendMessage(ChatColor.GRAY + "You have already stolen a heart from " + victim.getName() + ".");
            }
        }
    }

    /**
     * Handle Creation Splitter holder death - return all stolen hearts.
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();

        // Check if they have any stolen hearts
        int heartsStolen = plugin.getHeartManager().getHeartsStolen(player.getUniqueId());
        if (heartsStolen <= 0) return;

        // Check if they had Creation Splitter in inventory
        if (!hasCreationSplitterInInventory(player)) return;

        // Return all stolen hearts
        plugin.getHeartManager().onHolderDeath(player);

        player.sendMessage(ChatColor.DARK_RED + "☠ " + ChatColor.RED + "Endbound Soulkeeper: " +
            ChatColor.WHITE + "All stolen hearts have been returned!");
        player.sendMessage(ChatColor.RED + "Your heart progression has been reset.");

        // Dramatic effect
        player.getWorld().spawnParticle(Particle.REVERSE_PORTAL, player.getLocation().add(0, 1, 0), 200, 1, 1, 1, 1);
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_WITHER_DEATH, 0.5f, 0.5f);
    }

    /**
     * Apply heart modifiers when player joins.
     */
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        plugin.getHeartManager().onPlayerJoin(event.getPlayer());
    }
}
