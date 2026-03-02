package local.simplekits;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.NamespacedKey;

import java.util.*;

/**
 * Stacks mobs of the same type within the same chunk
 */
public class MobStackManager implements Listener {
    
    private final SimpleKitsPlugin plugin;
    private final NamespacedKey stackSizeKey;
    private final Set<EntityType> stackableTypes = EnumSet.of(
        EntityType.ZOMBIE, EntityType.SKELETON, EntityType.CREEPER, EntityType.SPIDER,
        EntityType.ENDERMAN, EntityType.BLAZE, EntityType.MAGMA_CUBE, EntityType.SLIME,
        EntityType.WITCH, EntityType.PIGLIN, EntityType.ZOMBIFIED_PIGLIN,
        EntityType.COW, EntityType.PIG, EntityType.SHEEP, EntityType.CHICKEN,
        EntityType.IRON_GOLEM, EntityType.WARDEN, EntityType.GHAST, EntityType.GUARDIAN,
        EntityType.DOLPHIN
    );
    
    public MobStackManager(SimpleKitsPlugin plugin) {
        this.plugin = plugin;
        this.stackSizeKey = new NamespacedKey(plugin, "stack_size");
        
        // Check for stackable mobs every second
        Bukkit.getScheduler().runTaskTimer(plugin, this::checkAndStackMobs, 20L, 20L);
    }
    
    /**
     * Get stack size of an entity
     */
    public int getStackSize(LivingEntity entity) {
        if (!entity.getPersistentDataContainer().has(stackSizeKey, PersistentDataType.INTEGER)) {
            return 1;
        }
        return entity.getPersistentDataContainer().get(stackSizeKey, PersistentDataType.INTEGER);
    }
    
    /**
     * Set stack size of an entity
     */
    public void setStackSize(LivingEntity entity, int size) {
        entity.getPersistentDataContainer().set(stackSizeKey, PersistentDataType.INTEGER, size);
        updateStackDisplay(entity, size);
    }
    
    /**
     * Update entity name to show stack size
     */
    private void updateStackDisplay(LivingEntity entity, int size) {
        if (size > 1) {
            String mobName = formatMobName(entity.getType());
            entity.setCustomName("§6" + mobName + " §e✖" + size);
            entity.setCustomNameVisible(true);
        } else {
            entity.setCustomNameVisible(false);
            entity.setCustomName(null);
        }
    }
    
    /**
     * Check all loaded chunks and stack mobs
     */
    private void checkAndStackMobs() {
        for (org.bukkit.World world : Bukkit.getWorlds()) {
            for (Chunk chunk : world.getLoadedChunks()) {
                stackMobsInChunk(chunk);
            }
        }
    }
    
    /**
     * Stack all mobs of the same type in a chunk
     */
    private void stackMobsInChunk(Chunk chunk) {
        Map<EntityType, List<LivingEntity>> mobsByType = new HashMap<>();
        
        // Group mobs by type
        for (Entity entity : chunk.getEntities()) {
            if (entity instanceof LivingEntity living && !(entity instanceof Player)) {
                if (stackableTypes.contains(entity.getType())) {
                    mobsByType.computeIfAbsent(entity.getType(), k -> new ArrayList<>()).add(living);
                }
            }
        }
        
        // Stack mobs of each type
        for (List<LivingEntity> mobs : mobsByType.values()) {
            if (mobs.size() > 1) {
                stackMobGroup(mobs);
            }
        }
    }
    
    /**
     * Combine multiple mobs into one stack
     */
    private void stackMobGroup(List<LivingEntity> mobs) {
        if (mobs.isEmpty()) return;
        
        // Find the mob with the largest stack (or first mob)
        LivingEntity mainMob = mobs.get(0);
        int mainStack = getStackSize(mainMob);
        
        for (LivingEntity mob : mobs) {
            int stack = getStackSize(mob);
            if (stack > mainStack) {
                mainMob = mob;
                mainStack = stack;
            }
        }
        
        // Combine all other mobs into the main one
        int totalStack = 0;
        for (LivingEntity mob : mobs) {
            if (mob.equals(mainMob)) {
                totalStack += getStackSize(mob);
            } else {
                // Check if mobs are close enough (within 5 blocks)
                if (mob.getLocation().distance(mainMob.getLocation()) <= 5.0) {
                    totalStack += getStackSize(mob);
                    mob.remove();
                } else {
                    totalStack += getStackSize(mob);
                }
            }
        }
        
        // Only keep the main mob
        for (LivingEntity mob : mobs) {
            if (!mob.equals(mainMob) && !mob.isDead()) {
                // Check distance again
                if (mob.getLocation().distance(mainMob.getLocation()) <= 5.0) {
                    mob.remove();
                }
            }
        }
        
        setStackSize(mainMob, totalStack);
    }
    
    /**
     * When mob spawns from spawner, initialize stack size
     */
    @EventHandler
    public void onMobSpawn(CreatureSpawnEvent event) {
        if (event.getSpawnReason() == CreatureSpawnEvent.SpawnReason.SPAWNER) {
            LivingEntity entity = event.getEntity();
            if (stackableTypes.contains(entity.getType())) {
                if (!entity.getPersistentDataContainer().has(stackSizeKey, PersistentDataType.INTEGER)) {
                    setStackSize(entity, 1);
                }
            }
        } else if (event.getSpawnReason() != CreatureSpawnEvent.SpawnReason.CUSTOM) {
            // Block natural spawning (only allow spawner and custom spawns)
            if (event.getEntityType() != EntityType.ARMOR_STAND && 
                event.getEntityType() != EntityType.ITEM_FRAME &&
                event.getEntityType() != EntityType.PAINTING) {
                event.setCancelled(true);
            }
        }
    }
    
    /**
     * When stacked mob takes damage, reduce stack count instead of killing
     */
    @EventHandler
    public void onMobDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof LivingEntity living)) return;
        if (living instanceof Player) return;
        if (!stackableTypes.contains(living.getType())) return;
        
        int stackSize = getStackSize(living);
        if (stackSize <= 1) return;
        
        // Check if this damage would kill the mob
        if (living.getHealth() - event.getFinalDamage() <= 0) {
            event.setCancelled(true);
            
            // Reduce stack by 1
            setStackSize(living, stackSize - 1);
            
            // Reset mob health
            living.setHealth(living.getMaxHealth());
            
            // Drop loot for one mob
            dropLootForOneMob(living);
        }
    }
    
    /**
     * When stacked mob dies, drop loot for all stacked mobs
     */
    @EventHandler
    public void onMobDeath(EntityDeathEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity instanceof Player) return;
        if (!stackableTypes.contains(entity.getType())) return;
        
        int stackSize = getStackSize(entity);
        if (stackSize > 1) {
            // Multiply drops by stack size
            List<org.bukkit.inventory.ItemStack> originalDrops = new ArrayList<>(event.getDrops());
            event.getDrops().clear();
            
            for (org.bukkit.inventory.ItemStack item : originalDrops) {
                org.bukkit.inventory.ItemStack multiplied = item.clone();
                multiplied.setAmount(item.getAmount() * stackSize);
                event.getDrops().add(multiplied);
            }
            
            // Multiply XP
            event.setDroppedExp(event.getDroppedExp() * stackSize);
        }
    }
    
    /**
     * Drop loot for a single mob from the stack
     */
    private void dropLootForOneMob(LivingEntity entity) {
        // Simulate one mob death for drops
        switch (entity.getType()) {
            case ZOMBIE -> {
                if (Math.random() < 0.025) entity.getWorld().dropItemNaturally(entity.getLocation(), new org.bukkit.inventory.ItemStack(org.bukkit.Material.IRON_INGOT));
                entity.getWorld().dropItemNaturally(entity.getLocation(), new org.bukkit.inventory.ItemStack(org.bukkit.Material.ROTTEN_FLESH, (int)(Math.random() * 3)));
            }
            case SKELETON -> {
                entity.getWorld().dropItemNaturally(entity.getLocation(), new org.bukkit.inventory.ItemStack(org.bukkit.Material.BONE, (int)(Math.random() * 3)));
                entity.getWorld().dropItemNaturally(entity.getLocation(), new org.bukkit.inventory.ItemStack(org.bukkit.Material.ARROW, (int)(Math.random() * 3)));
            }
            case CREEPER -> {
                entity.getWorld().dropItemNaturally(entity.getLocation(), new org.bukkit.inventory.ItemStack(org.bukkit.Material.GUNPOWDER, (int)(Math.random() * 3)));
            }
            case SPIDER -> {
                entity.getWorld().dropItemNaturally(entity.getLocation(), new org.bukkit.inventory.ItemStack(org.bukkit.Material.STRING, (int)(Math.random() * 3)));
            }
            case ENDERMAN -> {
                if (Math.random() < 0.5) entity.getWorld().dropItemNaturally(entity.getLocation(), new org.bukkit.inventory.ItemStack(org.bukkit.Material.ENDER_PEARL));
            }
            case BLAZE -> {
                if (Math.random() < 0.5) entity.getWorld().dropItemNaturally(entity.getLocation(), new org.bukkit.inventory.ItemStack(org.bukkit.Material.BLAZE_ROD));
            }
            case COW -> {
                entity.getWorld().dropItemNaturally(entity.getLocation(), new org.bukkit.inventory.ItemStack(org.bukkit.Material.LEATHER, (int)(Math.random() * 3)));
                entity.getWorld().dropItemNaturally(entity.getLocation(), new org.bukkit.inventory.ItemStack(org.bukkit.Material.BEEF, (int)(Math.random() * 4)));
            }
            case PIG -> {
                entity.getWorld().dropItemNaturally(entity.getLocation(), new org.bukkit.inventory.ItemStack(org.bukkit.Material.PORKCHOP, (int)(Math.random() * 4)));
            }
            case SHEEP -> {
                entity.getWorld().dropItemNaturally(entity.getLocation(), new org.bukkit.inventory.ItemStack(org.bukkit.Material.WHITE_WOOL, 1));
            }
            case CHICKEN -> {
                entity.getWorld().dropItemNaturally(entity.getLocation(), new org.bukkit.inventory.ItemStack(org.bukkit.Material.FEATHER, (int)(Math.random() * 3)));
                entity.getWorld().dropItemNaturally(entity.getLocation(), new org.bukkit.inventory.ItemStack(org.bukkit.Material.CHICKEN, 1));
            }
            case IRON_GOLEM -> {
                entity.getWorld().dropItemNaturally(entity.getLocation(), new org.bukkit.inventory.ItemStack(org.bukkit.Material.IRON_INGOT, 3 + (int)(Math.random() * 3)));
                entity.getWorld().dropItemNaturally(entity.getLocation(), new org.bukkit.inventory.ItemStack(org.bukkit.Material.POPPY, 1));
            }
        }
    }
    
    private String formatMobName(EntityType type) {
        String[] words = type.name().toLowerCase().split("_");
        StringBuilder result = new StringBuilder();
        for (String word : words) {
            if (!word.isEmpty()) {
                result.append(Character.toUpperCase(word.charAt(0)));
                result.append(word.substring(1));
                result.append(" ");
            }
        }
        return result.toString().trim();
    }
}
