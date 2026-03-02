package local.simplekits;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.CreatureSpawner;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.bukkit.inventory.meta.ItemMeta;

/**
 * Listener for right-clicking mystery spawners
 * Also handles spawner stacking when placed on top of each other
 */
public class MysterySpawnerListener implements Listener {

    private final MysterySpawnerManager manager;
    private final StackedSpawnerManager stackedSpawnerManager;

    public MysterySpawnerListener(MysterySpawnerManager manager, StackedSpawnerManager stackedSpawnerManager) {
        this.manager = manager;
        this.stackedSpawnerManager = stackedSpawnerManager;
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        // Left-click spawner info shortcut
        if (event.getAction() == Action.LEFT_CLICK_BLOCK
                && event.getClickedBlock() != null
            && event.getClickedBlock().getType() == Material.SPAWNER) {

            CreatureSpawner spawner = (CreatureSpawner) event.getClickedBlock().getState();
            int stackCount = stackedSpawnerManager.getSpawnerStackCount(event.getClickedBlock());
            double multiplier = stackedSpawnerManager.getSpawnRateMultiplier(stackCount);

            event.getPlayer().sendMessage("§6Spawner Stack: §f" + spawner.getSpawnedType().name() + " §7x§f" + stackCount + "§7/" + stackedSpawnerManager.getMaxStacks());
            event.getPlayer().sendMessage("§7Spawn multiplier: §f" + String.format("%.2fx", multiplier));
            return;
        }

        ItemStack item = event.getItem();
        
        // Must have item in hand
        if (item == null) return;
        
        // Must be holding mysterious spawner
        if (!isMysterySpawner(item)) return;
        
        // Must be right-click (air or block)
        Action action = event.getAction();
        if (action != Action.RIGHT_CLICK_AIR && action != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        
        event.setCancelled(true);
        
        Player player = event.getPlayer();
        
        // Get random mob type
        org.bukkit.entity.EntityType mobType = manager.getRandomSpawner();
        
        // Create spawner item for that mob
        ItemStack spawner = manager.createSpawnerForMob(mobType);
        
        if (spawner == null) {
            player.sendMessage("§cFailed to create spawner!");
            return;
        }
        
        // Give spawner to player
        if (player.getInventory().firstEmpty() == -1) {
            // Inventory full, drop it
            player.getWorld().dropItem(player.getLocation(), spawner);
            player.sendMessage("§e! Spawner dropped (inventory full)");
        } else {
            player.getInventory().addItem(spawner);
        }
        
        // Remove the mystery spawner gem
        if (item.getAmount() > 1) {
            item.setAmount(item.getAmount() - 1);
        } else {
            player.getInventory().removeItem(item);
        }
        
        player.sendMessage("§a✓ You obtained a mystery spawner!");
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        // Check if it's a spawner block
        if (event.getBlockPlaced().getType() != Material.SPAWNER) return;
        
        Block placedBlock = event.getBlockPlaced();
        Block againstBlock = event.getBlockAgainst();
        ItemStack heldItem = event.getItemInHand();
        Player player = event.getPlayer();
        
        // Check if it's our custom spawner
        if (!heldItem.hasItemMeta()) return;
        ItemMeta meta = heldItem.getItemMeta();
        if (meta == null) return;
        
        String displayName = meta.getDisplayName();
        if (displayName == null || (!displayName.contains("Spawner"))) return;
        
        // Try to extract entity type from BlockStateMeta
        EntityType mobType = null;
        
        try {
            if (meta instanceof BlockStateMeta) {
                BlockStateMeta blockMeta = (BlockStateMeta) meta;
                if (blockMeta.getBlockState() instanceof CreatureSpawner) {
                    CreatureSpawner spawner = (CreatureSpawner) blockMeta.getBlockState();
                    mobType = spawner.getSpawnedType();
                }
            }
        } catch (Exception e) {
            System.out.println("[SimpleKits] Error reading spawner meta: " + e.getMessage());
        }
        
        // If we found a mob type, set it on the placed block
        if (mobType != null && mobType != EntityType.UNKNOWN) {
            try {
                // Stack against any side if clicking an existing spawner, unless sneaking
                if (!player.isSneaking() && againstBlock != null && againstBlock.getState() instanceof CreatureSpawner) {
                    CreatureSpawner againstSpawner = (CreatureSpawner) againstBlock.getState();
                    EntityType againstMobType = againstSpawner.getSpawnedType();
                    
                    // Check if both spawners are the same type
                    if (againstMobType == mobType) {
                        // Try to add to stack
                        if (stackedSpawnerManager.addSpawnerToStack(againstBlock)) {
                            int newStackCount = stackedSpawnerManager.getSpawnerStackCount(againstBlock);
                            
                            // Apply updated spawner settings using fresh block state
                            CreatureSpawner refreshedSpawner = (CreatureSpawner) againstBlock.getState();
                            stackedSpawnerManager.applySpawnerSettings(refreshedSpawner, newStackCount);
                            
                            // Cancel the block place and notify player
                            event.setCancelled(true);
                            // Consume the stacked spawner from player's hand
                            if (heldItem.getAmount() > 1) {
                                heldItem.setAmount(heldItem.getAmount() - 1);
                            } else {
                                player.getInventory().setItemInMainHand(null);
                            }
                            player.sendMessage("§a✓ Stacked spawner! " + mobType.name() + " x" + newStackCount + " (Max: 10)");
                            return;
                        } else {
                            // Stack is full
                            event.setCancelled(true);
                            player.sendMessage("§c✗ This spawner stack is at maximum capacity (10)!");
                            return;
                        }
                    }
                }
                
                // No spawner below or different type, place as normal
                if (placedBlock.getState() instanceof CreatureSpawner) {
                    CreatureSpawner placedSpawner = (CreatureSpawner) placedBlock.getState();
                    placedSpawner.setSpawnedType(mobType);
                    
                    // Initialize with single spawner settings (4x multiplier)
                    stackedSpawnerManager.setSpawnerStackCount(placedBlock, 1);
                    stackedSpawnerManager.applySpawnerSettings(placedSpawner, 1);
                    
                    player.sendMessage("§a✓ Placed " + mobType.name() + " spawner! (Place another of the same type on top to stack)");
                }
            } catch (Exception e) {
                System.out.println("[SimpleKits] Error setting spawner on placed block: " + e.getMessage());
            }
        }
    }

    private boolean isMysterySpawner(ItemStack item) {
        if (!item.hasItemMeta()) return false;
        
        ItemMeta meta = item.getItemMeta();
        if (meta == null || !meta.hasDisplayName()) return false;
        
        String displayName = meta.getDisplayName();
        return displayName.contains("Mystery Spawner") || displayName.contains("mystery");
    }
}
