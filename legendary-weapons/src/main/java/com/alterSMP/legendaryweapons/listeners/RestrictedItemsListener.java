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
import org.bukkit.event.entity.EntityResurrectEvent;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.EnumSet;
import java.util.Set;

/**
 * Listener that restricts usage of totems of undying and non-legendary netherite items.
 * Legendary netherite items are allowed.
 *
 * Restrictions:
 * - Armor: Cannot be equipped at all
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

    // Armor slot indices in player inventory
    private static final int HELMET_SLOT = 39;
    private static final int CHESTPLATE_SLOT = 38;
    private static final int LEGGINGS_SLOT = 37;
    private static final int BOOTS_SLOT = 36;

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
     * Check if an item is restricted netherite armor
     */
    private boolean isRestrictedNetheriteArmor(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) {
            return false;
        }
        return isRestrictedNetherite(item) && NETHERITE_ARMOR.contains(item.getType());
    }

    /**
     * Check if a slot is an armor slot
     */
    private boolean isArmorSlot(int slot) {
        return slot == HELMET_SLOT || slot == CHESTPLATE_SLOT ||
               slot == LEGGINGS_SLOT || slot == BOOTS_SLOT;
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
     * Block netherite armor from being equipped via inventory click
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }

        Player player = (Player) event.getWhoClicked();
        ItemStack cursor = event.getCursor();
        ItemStack currentItem = event.getCurrentItem();
        int slot = event.getSlot();
        ClickType clickType = event.getClick();

        // Check totem placement in offhand (raw slot 45)
        if (isTotem(cursor) && event.getRawSlot() == 45) {
            event.setCancelled(true);
            player.sendMessage(ChatColor.RED + "Totems of Undying are disabled on this server!");
            updateInventoryLater(player);
            return;
        }

        // Only check for player inventory armor slots
        if (event.getClickedInventory() == null ||
            event.getClickedInventory().getType() != InventoryType.PLAYER) {
            return;
        }

        // Block placing restricted netherite armor in armor slots
        if (isArmorSlot(slot)) {
            // Placing item with cursor
            if (isRestrictedNetheriteArmor(cursor)) {
                event.setCancelled(true);
                player.sendMessage(ChatColor.RED + "Netherite armor cannot be equipped!");
                updateInventoryLater(player);
                return;
            }

            // Shift-click wouldn't place in armor slot if clicking on armor slot
        }

        // Handle shift-click from any slot to armor slot
        if (clickType.isShiftClick() && isRestrictedNetheriteArmor(currentItem)) {
            // Check if this armor would go to an armor slot
            Material type = currentItem.getType();
            if (type == Material.NETHERITE_HELMET || type == Material.NETHERITE_CHESTPLATE ||
                type == Material.NETHERITE_LEGGINGS || type == Material.NETHERITE_BOOTS) {

                // Check if the corresponding armor slot is empty (shift-click would fill it)
                ItemStack targetSlot = null;
                if (type == Material.NETHERITE_HELMET) {
                    targetSlot = player.getInventory().getHelmet();
                } else if (type == Material.NETHERITE_CHESTPLATE) {
                    targetSlot = player.getInventory().getChestplate();
                } else if (type == Material.NETHERITE_LEGGINGS) {
                    targetSlot = player.getInventory().getLeggings();
                } else if (type == Material.NETHERITE_BOOTS) {
                    targetSlot = player.getInventory().getBoots();
                }

                if (targetSlot == null || targetSlot.getType() == Material.AIR) {
                    event.setCancelled(true);
                    player.sendMessage(ChatColor.RED + "Netherite armor cannot be equipped!");
                    updateInventoryLater(player);
                    return;
                }
            }
        }

        // Handle number key swap to armor slots
        if (clickType == ClickType.NUMBER_KEY && isArmorSlot(slot)) {
            int hotbarSlot = event.getHotbarButton();
            ItemStack hotbarItem = player.getInventory().getItem(hotbarSlot);
            if (isRestrictedNetheriteArmor(hotbarItem)) {
                event.setCancelled(true);
                player.sendMessage(ChatColor.RED + "Netherite armor cannot be equipped!");
                updateInventoryLater(player);
                return;
            }
        }
    }

    /**
     * Block netherite armor from being equipped via drag
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }

        Player player = (Player) event.getWhoClicked();
        ItemStack draggedItem = event.getOldCursor();

        if (!isRestrictedNetheriteArmor(draggedItem)) {
            return;
        }

        // Check if any of the slots are armor slots
        for (int slot : event.getRawSlots()) {
            // Raw slots 5-8 are armor slots in player inventory view
            if (slot >= 5 && slot <= 8) {
                event.setCancelled(true);
                player.sendMessage(ChatColor.RED + "Netherite armor cannot be equipped!");
                updateInventoryLater(player);
                return;
            }
        }
    }

    /**
     * Block right-click equipping of netherite armor
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem();

        if (item == null) {
            return;
        }

        // Check if trying to right-click equip netherite armor
        if (isRestrictedNetheriteArmor(item)) {
            if (event.getAction().name().contains("RIGHT")) {
                event.setCancelled(true);
                player.sendMessage(ChatColor.RED + "Netherite armor cannot be equipped!");
            }
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
            player.sendMessage(ChatColor.RED + "Netherite weapons cannot deal damage!");
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
            player.sendMessage(ChatColor.RED + "Netherite tools cannot break blocks!");
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
            player.sendMessage(ChatColor.RED + "Netherite tools cannot mine blocks!");
        }
    }

    /**
     * Update player inventory on next tick to prevent ghost items
     */
    private void updateInventoryLater(Player player) {
        new BukkitRunnable() {
            @Override
            public void run() {
                player.updateInventory();
            }
        }.runTaskLater(plugin, 1L);
    }
}
