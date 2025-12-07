package com.alterSMP.legendaryweapons.abilities;

import com.alterSMP.legendaryweapons.LegendaryWeaponsPlugin;
import com.alterSMP.legendaryweapons.items.LegendaryItemFactory;
import com.alterSMP.legendaryweapons.items.LegendaryType;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class CopperPickaxeListener implements Listener {

    private final LegendaryWeaponsPlugin plugin;

    public CopperPickaxeListener(LegendaryWeaponsPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onBlockBreak(BlockBreakEvent event) {
        if (event.isCancelled()) return;

        Player player = event.getPlayer();
        ItemStack tool = player.getInventory().getItemInMainHand();

        // Check if using Copper Pickaxe
        String legendaryId = LegendaryItemFactory.getLegendaryId(tool);
        if (legendaryId == null || !legendaryId.equals(LegendaryType.COPPER_PICKAXE.getId())) {
            return;
        }

        Block block = event.getBlock();

        // Apply Fortune or Silk Touch based on toggle
        applyEnchantmentToggle(tool, block, event);

        // Handle 3x3 mining if enabled
        if (plugin.getAbilityManager().is3x3MiningActive(player.getUniqueId())) {
            handle3x3Mining(player, block, event);
        }
    }

    private void applyEnchantmentToggle(ItemStack tool, Block block, BlockBreakEvent event) {
        ItemMeta meta = tool.getItemMeta();
        if (meta == null) return;

        // Remove any existing Fortune or Silk Touch
        meta.removeEnchant(Enchantment.FORTUNE);
        meta.removeEnchant(Enchantment.SILK_TOUCH);

        // Apply based on toggle state
        if (plugin.getAbilityManager().isFortuneModeActive(event.getPlayer().getUniqueId())) {
            meta.addEnchant(Enchantment.FORTUNE, 3, true);
        } else {
            meta.addEnchant(Enchantment.SILK_TOUCH, 1, true);
        }

        tool.setItemMeta(meta);
    }

    private void handle3x3Mining(Player player, Block centerBlock, BlockBreakEvent event) {
        if (player.getGameMode() == GameMode.CREATIVE) return;

        // Get the 3x3 area around the broken block
        List<Block> blocksToBreak = get3x3Blocks(centerBlock, player);

        // Break each block in the 3x3 area
        for (Block block : blocksToBreak) {
            if (block.equals(centerBlock)) continue; // Skip center block (already being broken)
            if (block.getType() == Material.AIR) continue;
            if (block.getType() == Material.BEDROCK) continue; // Can't break bedrock
            if (block.getType().getHardness() < 0) continue; // Can't break unbreakable blocks

            // Drop items as if player broke it
            Collection<ItemStack> drops = block.getDrops(player.getInventory().getItemInMainHand(), player);
            block.setType(Material.AIR);

            // Give drops to player
            for (ItemStack drop : drops) {
                if (player.getInventory().firstEmpty() != -1) {
                    player.getInventory().addItem(drop);
                } else {
                    player.getWorld().dropItemNaturally(block.getLocation(), drop);
                }
            }
        }
    }

    private List<Block> get3x3Blocks(Block center, Player player) {
        List<Block> blocks = new ArrayList<>();

        // Get player's facing direction
        org.bukkit.util.Vector direction = player.getLocation().getDirection();
        double pitch = player.getLocation().getPitch();

        // If looking mostly up or down, use horizontal 3x3
        if (Math.abs(pitch) > 45) {
            // Vertical mining (looking up/down)
            for (int x = -1; x <= 1; x++) {
                for (int z = -1; z <= 1; z++) {
                    blocks.add(center.getRelative(x, 0, z));
                }
            }
        } else {
            // Determine primary axis based on yaw
            float yaw = player.getLocation().getYaw();
            yaw = (yaw % 360 + 360) % 360; // Normalize to 0-360

            if ((yaw >= 315 || yaw < 45) || (yaw >= 135 && yaw < 225)) {
                // North/South - use X and Y
                for (int x = -1; x <= 1; x++) {
                    for (int y = -1; y <= 1; y++) {
                        blocks.add(center.getRelative(x, y, 0));
                    }
                }
            } else {
                // East/West - use Z and Y
                for (int z = -1; z <= 1; z++) {
                    for (int y = -1; y <= 1; y++) {
                        blocks.add(center.getRelative(0, y, z));
                    }
                }
            }
        }

        return blocks;
    }
}
