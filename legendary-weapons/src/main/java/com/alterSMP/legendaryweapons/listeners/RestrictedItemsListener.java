package com.alterSMP.legendaryweapons.listeners;

import com.alterSMP.legendaryweapons.LegendaryWeaponsPlugin;
import com.alterSMP.legendaryweapons.items.LegendaryItemFactory;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockDamageEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityResurrectEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.ItemStack;

import java.util.EnumSet;
import java.util.Set;

/**
 * Listener that restricts usage of totems of undying and non-legendary netherite items.
 * Legendary netherite items are allowed.
 *
 * Restrictions:
 * - Armor: Can be equipped but provides NO protection (damage isn't reduced)
 * - Weapons: Cannot deal damage (but can be held)
 * - Tools: Cannot mine/break blocks (but can be held)
 */
public class RestrictedItemsListener implements Listener {

    private final LegendaryWeaponsPlugin plugin;

    public RestrictedItemsListener(LegendaryWeaponsPlugin plugin) {
        this.plugin = plugin;
    }

    private static final Set<Material> NETHERITE_ARMOR = EnumSet.of(
        Material.NETHERITE_HELMET,
        Material.NETHERITE_CHESTPLATE,
        Material.NETHERITE_LEGGINGS,
        Material.NETHERITE_BOOTS
    );

    private static final Set<Material> NETHERITE_WEAPONS = EnumSet.of(
        Material.NETHERITE_SWORD,
        Material.NETHERITE_AXE
    );

    private static final Set<Material> NETHERITE_TOOLS = EnumSet.of(
        Material.NETHERITE_PICKAXE,
        Material.NETHERITE_SHOVEL,
        Material.NETHERITE_HOE
    );

    private static final Set<Material> ALL_NETHERITE = EnumSet.of(
        Material.NETHERITE_HELMET,
        Material.NETHERITE_CHESTPLATE,
        Material.NETHERITE_LEGGINGS,
        Material.NETHERITE_BOOTS,
        Material.NETHERITE_SWORD,
        Material.NETHERITE_AXE,
        Material.NETHERITE_PICKAXE,
        Material.NETHERITE_SHOVEL,
        Material.NETHERITE_HOE
    );

    /**
     * Check if an item is a restricted netherite item (non-legendary netherite)
     */
    private boolean isRestrictedNetherite(ItemStack item) {
        if (!plugin.getConfigManager().isNetheriteRestricted()) {
            return false;
        }

        if (item == null || item.getType() == Material.AIR) {
            return false;
        }

        if (!ALL_NETHERITE.contains(item.getType())) {
            return false;
        }

        // Allow legendary netherite items
        String legendaryId = LegendaryItemFactory.getLegendaryId(item);
        return legendaryId == null;
    }

    /**
     * Check if a player is wearing any restricted netherite armor
     */
    private boolean isWearingRestrictedNetheriteArmor(Player player) {
        ItemStack helmet = player.getInventory().getHelmet();
        ItemStack chest = player.getInventory().getChestplate();
        ItemStack legs = player.getInventory().getLeggings();
        ItemStack boots = player.getInventory().getBoots();

        return (isRestrictedNetherite(helmet) && NETHERITE_ARMOR.contains(helmet.getType())) ||
               (isRestrictedNetherite(chest) && NETHERITE_ARMOR.contains(chest.getType())) ||
               (isRestrictedNetherite(legs) && NETHERITE_ARMOR.contains(legs.getType())) ||
               (isRestrictedNetherite(boots) && NETHERITE_ARMOR.contains(boots.getType()));
    }

    /**
     * Check if an item is a totem of undying
     */
    private boolean isTotem(ItemStack item) {
        return item != null && item.getType() == Material.TOTEM_OF_UNDYING;
    }

    /**
     * Prevent totem resurrection
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onEntityResurrect(EntityResurrectEvent event) {
        if (!(event.getEntity() instanceof Player)) {
            return;
        }

        event.setCancelled(true);

        Player player = (Player) event.getEntity();
        player.sendMessage(ChatColor.RED + "Totems of Undying are disabled on this server!");
    }

    /**
     * Make restricted netherite armor provide NO protection.
     * Instead of blocking equip, we just make the armor useless.
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEntityDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player)) {
            return;
        }

        Player player = (Player) event.getEntity();

        // Check if player is wearing restricted netherite armor
        if (isWearingRestrictedNetheriteArmor(player)) {
            // Get the original damage and set it as final damage (bypassing armor reduction)
            double originalDamage = event.getDamage(EntityDamageEvent.DamageModifier.BASE);

            // Set final damage to bypass armor protection from netherite
            // This effectively makes the netherite armor provide 0 protection
            event.setDamage(originalDamage);

            // Send warning message (only occasionally to not spam)
            if (Math.random() < 0.1) { // 10% chance to show message
                player.sendMessage(ChatColor.RED + "Your non-legendary netherite armor provides no protection!");
            }
        }
    }

    /**
     * Prevent totem in offhand via inventory click
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }

        Player player = (Player) event.getWhoClicked();
        ItemStack cursor = event.getCursor();
        int rawSlot = event.getRawSlot();

        // Check totem placement in offhand (raw slot 45)
        if (isTotem(cursor) && rawSlot == 45) {
            event.setCancelled(true);
            player.sendMessage(ChatColor.RED + "Totems of Undying are disabled on this server!");
        }
    }

    /**
     * Prevent swapping totem to offhand
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onSwapHandItems(PlayerSwapHandItemsEvent event) {
        Player player = event.getPlayer();
        ItemStack offhandItem = event.getOffHandItem();

        if (isTotem(offhandItem)) {
            event.setCancelled(true);
            player.sendMessage(ChatColor.RED + "Totems of Undying are disabled on this server!");
        }
    }

    /**
     * Prevent restricted netherite weapons from dealing damage
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player)) {
            return;
        }

        Player player = (Player) event.getDamager();
        ItemStack mainHand = player.getInventory().getItemInMainHand();

        if (isRestrictedNetherite(mainHand) && NETHERITE_WEAPONS.contains(mainHand.getType())) {
            event.setCancelled(true);
            player.sendMessage(ChatColor.RED + "Non-legendary netherite weapons cannot deal damage!");
        }
    }

    /**
     * Prevent restricted netherite tools from breaking blocks
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        ItemStack mainHand = player.getInventory().getItemInMainHand();

        if (isRestrictedNetherite(mainHand) && NETHERITE_TOOLS.contains(mainHand.getType())) {
            event.setCancelled(true);
            player.sendMessage(ChatColor.RED + "Non-legendary netherite tools cannot break blocks!");
        }
    }

    /**
     * Prevent restricted netherite tools from damaging blocks (mining animation)
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onBlockDamage(BlockDamageEvent event) {
        Player player = event.getPlayer();
        ItemStack mainHand = player.getInventory().getItemInMainHand();

        if (isRestrictedNetherite(mainHand) && NETHERITE_TOOLS.contains(mainHand.getType())) {
            event.setCancelled(true);
            player.sendMessage(ChatColor.RED + "Non-legendary netherite tools cannot mine blocks!");
        }
    }
}
