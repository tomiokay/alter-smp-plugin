package com.alterSMP.legendaryweapons.listeners;

import com.alterSMP.legendaryweapons.LegendaryWeaponsPlugin;
import com.alterSMP.legendaryweapons.items.LegendaryItemFactory;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockDamageEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityResurrectEvent;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
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
 * - Armor: Cannot be equipped in armor slots (but can be in hotbar/inventory)
 * - Weapons: Cannot deal damage (but can be held)
 * - Tools: Cannot mine/break blocks (but can be held)
 * - All items stay in inventory - they do NOT disappear or no-clip
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
     * Prevent equipping restricted netherite ARMOR in armor slots only.
     * Uses delayed inventory update to prevent ghosting.
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }

        Player player = (Player) event.getWhoClicked();
        ItemStack cursor = event.getCursor();
        ItemStack current = event.getCurrentItem();
        int rawSlot = event.getRawSlot();

        // Only restrict ARMOR going into ARMOR SLOTS (raw slots 5-8)
        // Armor slots: 5=helmet, 6=chestplate, 7=leggings, 8=boots
        boolean isArmorSlot = rawSlot >= 5 && rawSlot <= 8;

        // Check cursor item (item being placed) into armor slot
        if (isArmorSlot && isRestrictedNetherite(cursor) && NETHERITE_ARMOR.contains(cursor.getType())) {
            event.setCancelled(true);
            player.sendMessage(ChatColor.RED + "Non-legendary netherite armor cannot be equipped!");
            // Delayed update to prevent ghost items
            new BukkitRunnable() {
                @Override
                public void run() {
                    player.updateInventory();
                }
            }.runTaskLater(plugin, 1L);
            return;
        }

        // Check shift-click equipping armor
        if (event.isShiftClick() && isRestrictedNetherite(current) && NETHERITE_ARMOR.contains(current.getType())) {
            // Shift-click would auto-equip armor to armor slot
            event.setCancelled(true);
            player.sendMessage(ChatColor.RED + "Non-legendary netherite armor cannot be equipped!");
            // Delayed update to prevent ghost items
            new BukkitRunnable() {
                @Override
                public void run() {
                    player.updateInventory();
                }
            }.runTaskLater(plugin, 1L);
            return;
        }

        // Check number key swap to armor slots
        if (event.getClick() == ClickType.NUMBER_KEY && isArmorSlot) {
            ItemStack hotbarItem = player.getInventory().getItem(event.getHotbarButton());
            if (isRestrictedNetherite(hotbarItem) && NETHERITE_ARMOR.contains(hotbarItem.getType())) {
                event.setCancelled(true);
                player.sendMessage(ChatColor.RED + "Non-legendary netherite armor cannot be equipped!");
                // Delayed update to prevent ghost items
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        player.updateInventory();
                    }
                }.runTaskLater(plugin, 1L);
                return;
            }
        }

        // Check totem placement in offhand (raw slot 45)
        if (isTotem(cursor) && rawSlot == 45) {
            event.setCancelled(true);
            player.sendMessage(ChatColor.RED + "Totems of Undying are disabled on this server!");
            new BukkitRunnable() {
                @Override
                public void run() {
                    player.updateInventory();
                }
            }.runTaskLater(plugin, 1L);
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
     * Prevent equipping restricted netherite armor via right-click
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack mainHand = player.getInventory().getItemInMainHand();

        // Check if right-clicking with armor (to equip)
        if (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            if (isRestrictedNetherite(mainHand) && NETHERITE_ARMOR.contains(mainHand.getType())) {
                event.setCancelled(true);
                player.sendMessage(ChatColor.RED + "Non-legendary netherite armor cannot be equipped!");
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        player.updateInventory();
                    }
                }.runTaskLater(plugin, 1L);
            }
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
