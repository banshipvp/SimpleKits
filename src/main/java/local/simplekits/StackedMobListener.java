package local.simplekits;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityDeathEvent;

/**
 * Listener for mob stacking mechanics
 * Handles entity spawning, stacking, and death logic
 */
public class StackedMobListener implements Listener {

    private final StackedSpawnerManager stackedSpawnerManager;

    public StackedMobListener(StackedSpawnerManager stackedSpawnerManager) {
        this.stackedSpawnerManager = stackedSpawnerManager;
    }

    /**
     * When a creature spawns, try to stack it with nearby mobs
     */
    @EventHandler
    public void onCreatureSpawn(CreatureSpawnEvent event) {
        Entity entity = event.getEntity();

        // Only process mobs coming from a spawner.
        // Ignore natural/chunk/breeding/reinforcement/etc. spawns.
        if (event.getSpawnReason() != CreatureSpawnEvent.SpawnReason.SPAWNER) {
            return;
        }

        // Safety check: if spawn reason says SPAWNER but there is no nearby
        // spawner block, cancel to prevent ghost spawning at old locations.
        if (!hasNearbySpawnerBlock(entity.getLocation(), 6)) {
            event.setCancelled(true);
            return;
        }
        
        // Only stack certain entity types (not bosses or special entities)
        if (!canEntityStack(entity)) {
            return;
        }
        
        // Look for nearby entities of the same type within 3 blocks
        Entity nearbyMob = findNearbyStackableMob(entity);
        
        if (nearbyMob != null && !nearbyMob.isDead()) {
            // Found a mob to stack with
            int nearbyStackCount = stackedSpawnerManager.getEntityStackCount(nearbyMob);
            
            // Add to the nearby mob's stack
            stackedSpawnerManager.setEntityStackCount(nearbyMob, nearbyStackCount + 1);
            stackedSpawnerManager.updateEntityDisplay(nearbyMob);
            
            // Kill the newly spawned entity (represented by stack count instead)
            entity.remove();
        } else {
            // Initialize stack count to 1
            stackedSpawnerManager.setEntityStackCount(entity, 1);
            stackedSpawnerManager.updateEntityDisplay(entity);
        }
    }

    /**
     * When a stacked entity dies, handle the stack count properly
     */
    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        Entity entity = event.getEntity();
        int stackCount = stackedSpawnerManager.getEntityStackCount(entity);
        
        if (stackCount > 1) {
            // Decrease stack count instead of fully removing
            stackCount--;
            
            // Multiply drops by stack count (represents killing multiple mobs)
            int dropMultiplier = stackCount;
            event.getDrops().forEach(drop -> drop.setAmount(drop.getAmount() * dropMultiplier));
            
            // Also increase experience
            event.setDroppedExp(event.getDroppedExp() * (stackCount + 1));
        }
    }

    /**
     * Find a mob of the same type in the whole chunk that can be stacked
     */
    private Entity findNearbyStackableMob(Entity entity) {
        for (Entity nearby : entity.getLocation().getChunk().getEntities()) {
            // Must be same entity type
            if (nearby.getType() != entity.getType()) {
                continue;
            }

            // Skip self, dead, or non-living
            if (nearby == entity || nearby.isDead()) {
                continue;
            }

            if (!(nearby instanceof LivingEntity)) {
                continue;
            }

            return nearby;
        }

        return null;
    }

    /**
     * Check if entity type can be stacked
     */
    private boolean canEntityStack(Entity entity) {
        if (!(entity instanceof LivingEntity)) {
            return false;
        }
        
        // Don't stack bosses
        if (entity instanceof Wither || entity instanceof EnderDragon) {
            return false;
        }
        
        // Stack all other creatures
        return true;
    }

    private boolean hasNearbySpawnerBlock(Location location, int radius) {
        int baseX = location.getBlockX();
        int baseY = location.getBlockY();
        int baseZ = location.getBlockZ();

        for (int x = baseX - radius; x <= baseX + radius; x++) {
            for (int y = baseY - radius; y <= baseY + radius; y++) {
                for (int z = baseZ - radius; z <= baseZ + radius; z++) {
                    Block block = location.getWorld().getBlockAt(x, y, z);
                    if (block.getType() == Material.SPAWNER) {
                        return true;
                    }
                }
            }
        }

        return false;
    }
}
