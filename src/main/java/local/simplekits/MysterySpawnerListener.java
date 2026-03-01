package local.simplekits;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.CreatureSpawner;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.bukkit.inventory.meta.ItemMeta;

/**
 * Listener for right-clicking mystery spawners
 */
public class MysterySpawnerListener implements Listener {

    private final MysterySpawnerManager manager;

    public MysterySpawnerListener(MysterySpawnerManager manager) {
        this.manager = manager;
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        ItemStack item = event.getItem();
        
        // Must have item in hand
        if (item == null) return;
        
        // Must be holding mysterious spawner
        if (!isMysterySpawner(item)) return;
        
        // Must be right-click (air or block)
        org.bukkit.event.block.Action action = event.getAction();
        if (action != org.bukkit.event.block.Action.RIGHT_CLICK_AIR && 
            action != org.bukkit.event.block.Action.RIGHT_CLICK_BLOCK) {
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
        ItemStack heldItem = event.getItemInHand();
        
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
                if (placedBlock.getState() instanceof CreatureSpawner) {
                    CreatureSpawner placedSpawner = (CreatureSpawner) placedBlock.getState();
                    placedSpawner.setSpawnedType(mobType);
                    placedSpawner.setDelay(200);
                    placedSpawner.setMaxNearbyEntities(16);
                    placedSpawner.setMaxSpawnDelay(800);
                    placedSpawner.setMinSpawnDelay(200);
                    placedSpawner.setRequiredPlayerRange(16);
                    placedSpawner.setSpawnRange(4);
                    placedSpawner.update();
                    
                    event.getPlayer().sendMessage("§a✓ Placed " + mobType.name() + " spawner!");
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
