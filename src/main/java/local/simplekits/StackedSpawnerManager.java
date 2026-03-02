package local.simplekits;

import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.block.CreatureSpawner;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ArmorStand;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

/**
 * Manages stacked spawners and mobs
 * - Spawners can stack up to 10 on a single block
 * - Mobs can stack together and multiply spawn rates
 * - Each stack increases spawn rate by 1.5x at max (10 stacks)
 */
public class StackedSpawnerManager {

    private final JavaPlugin plugin;
    private final NamespacedKey spawnerStackKey;
    private final NamespacedKey mobStackKey;
    private final NamespacedKey stackCountKey;
    
    // Spawn rate multiplier constants
    private static final int MAX_STACKS = 10;
    private static final double BASE_SPAWN_RATE_MULTIPLIER = 6.0; // 6x default spawn rate
    private static final double MAX_STACK_BONUS = 2.0; // Additional 2x when at 10 stacks

    public StackedSpawnerManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.spawnerStackKey = new NamespacedKey(plugin, "spawner_stack_count");
        this.mobStackKey = new NamespacedKey(plugin, "mob_stack_count");
        this.stackCountKey = new NamespacedKey(plugin, "entity_stack_count");
    }

    /**
     * Get the number of spawners stacked at a location
     */
    public int getSpawnerStackCount(Block block) {
        if (!(block.getState() instanceof CreatureSpawner)) {
            return 0;
        }
        
        CreatureSpawner spawner = (CreatureSpawner) block.getState();
        PersistentDataContainer pdc = spawner.getPersistentDataContainer();
        
        Integer count = pdc.get(spawnerStackKey, PersistentDataType.INTEGER);
        return count != null ? count : 1;
    }

    /**
     * Set the number of stacked spawners at a location
     */
    public void setSpawnerStackCount(Block block, int count) {
        if (!(block.getState() instanceof CreatureSpawner)) {
            return;
        }
        
        count = Math.min(count, MAX_STACKS); // Cap at MAX_STACKS
        
        CreatureSpawner spawner = (CreatureSpawner) block.getState();
        PersistentDataContainer pdc = spawner.getPersistentDataContainer();
        pdc.set(spawnerStackKey, PersistentDataType.INTEGER, count);
        spawner.update(true, false);
    }

    /**
     * Add a spawner to the stack at a location
     * Returns true if successful, false if at max capacity
     */
    public boolean addSpawnerToStack(Block block) {
        int currentCount = getSpawnerStackCount(block);
        
        if (currentCount >= MAX_STACKS) {
            return false; // Already at max
        }
        
        setSpawnerStackCount(block, currentCount + 1);
        return true;
    }

    /**
     * Calculate spawn rate multiplier based on stack count
     * Base multiplier: 4x
     * At 10 stacks: 4x * 1.5 = 6x total spawn rate
     */
    public double getSpawnRateMultiplier(int stackCount) {
        if (stackCount <= 0) {
            return 1.0;
        }
        
        stackCount = Math.min(stackCount, MAX_STACKS);
        
        // Base multiplier of 6x
        double multiplier = BASE_SPAWN_RATE_MULTIPLIER;
        
        // Add bonus for reaching max stacks
        if (stackCount == MAX_STACKS) {
            multiplier *= MAX_STACK_BONUS;
        }
        
        return multiplier;
    }

    /**
     * Apply spawn rate settings to a spawner based on stack count
     */
    public void applySpawnerSettings(CreatureSpawner spawner, int stackCount) {
        double multiplier = getSpawnRateMultiplier(stackCount);

        // Base delays in ticks — lower base = faster out of the box
        int baseMinDelay = 80;
        int baseMaxDelay = 200;

        // Shorter delay = more frequent spawning
        int minDelay = Math.max(5, (int) (baseMinDelay / multiplier));
        int maxDelay = Math.max(10, (int) (baseMaxDelay / multiplier));

        // Spawn count: scale with stack size (1 stack = 4, 10 stacks = 10)
        int spawnCount = Math.min(10, 3 + stackCount);

        // Allow lots of nearby entities so the cap never blocks spawning
        int maxNearby = 200;

        spawner.setMinSpawnDelay(minDelay);
        spawner.setMaxSpawnDelay(maxDelay);
        // Set the running timer to the min delay so it fires quickly after placement
        spawner.setDelay(minDelay);
        spawner.setSpawnCount(spawnCount);
        spawner.setMaxNearbyEntities(maxNearby);
        spawner.setRequiredPlayerRange(32);
        spawner.setSpawnRange(6);
        spawner.update(true, false);
    }

    /**
     * Add stack count to an entity
     */
    public void addStackToEntity(Entity entity) {
        if (entity == null || entity.isDead()) {
            return;
        }
        
        PersistentDataContainer pdc = entity.getPersistentDataContainer();
        Integer current = pdc.get(stackCountKey, PersistentDataType.INTEGER);
        int stackCount = (current != null ? current : 1) + 1;
        
        pdc.set(stackCountKey, PersistentDataType.INTEGER, stackCount);
    }

    /**
     * Set stack count for an entity
     */
    public void setEntityStackCount(Entity entity, int stackCount) {
        if (entity == null || entity.isDead()) {
            return;
        }
        
        PersistentDataContainer pdc = entity.getPersistentDataContainer();
        pdc.set(stackCountKey, PersistentDataType.INTEGER, Math.max(1, stackCount));
    }

    /**
     * Get stack count for an entity
     */
    public int getEntityStackCount(Entity entity) {
        if (entity == null) {
            return 0;
        }
        
        PersistentDataContainer pdc = entity.getPersistentDataContainer();
        Integer count = pdc.get(stackCountKey, PersistentDataType.INTEGER);
        return count != null ? count : 1;
    }

    /**
     * Check if an entity has stack data
     */
    public boolean isStacked(Entity entity) {
        if (entity == null) {
            return false;
        }
        
        PersistentDataContainer pdc = entity.getPersistentDataContainer();
        return pdc.has(stackCountKey, PersistentDataType.INTEGER);
    }

    /**
     * Update entity display to show stack count
     * For armor stands or custom name
     */
    public void updateEntityDisplay(Entity entity) {
        int stackCount = getEntityStackCount(entity);
        
        if (stackCount > 1) {
            String displayName = "§cx" + stackCount + " §r" + entity.getType().name();
            entity.setCustomName(displayName);
            entity.setCustomNameVisible(true);
        }
    }

    /**
     * Get max stacks constant
     */
    public int getMaxStacks() {
        return MAX_STACKS;
    }

    /**
     * Get base spawn rate multiplier
     */
    public double getBaseSpawnRateMultiplier() {
        return BASE_SPAWN_RATE_MULTIPLIER;
    }
}
