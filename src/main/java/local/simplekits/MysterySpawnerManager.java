package local.simplekits;

import org.bukkit.Material;
import org.bukkit.block.CreatureSpawner;
import org.bukkit.entity.EntityType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;

/**
 * Manages mystery mob spawners - right-click to get a random spawner based on chance
 */
public class MysterySpawnerManager {

    @SuppressWarnings("unused")
    private final JavaPlugin plugin;
    
    // Spawner drop chances (as percentages)
    private final Map<EntityType, Double> spawnerChances = new LinkedHashMap<>();
    
    public MysterySpawnerManager(JavaPlugin plugin) {
        this.plugin = plugin;
        setupSpawnerChances();
    }
    
    /**
     * Setup the drop chances for each mob type
     * Adjusted for factions server balance
     */
    private void setupSpawnerChances() {
        // Common - Basic farming mobs (50%)
        spawnerChances.put(EntityType.PIG, 15.0);
        spawnerChances.put(EntityType.COW, 15.0);
        spawnerChances.put(EntityType.CHICKEN, 12.0);
        spawnerChances.put(EntityType.SHEEP, 8.0);
        
        // Uncommon - Grinder mobs (31%)
        spawnerChances.put(EntityType.BLAZE, 10.0);
        spawnerChances.put(EntityType.ENDERMAN, 9.0);
        spawnerChances.put(EntityType.CREEPER, 6.0);
        spawnerChances.put(EntityType.GHAST, 6.0);
        spawnerChances.put(EntityType.MAGMA_CUBE, 5.0);
        spawnerChances.put(EntityType.GUARDIAN, 5.0);
        
        // Rare - High value (12%)
        spawnerChances.put(EntityType.IRON_GOLEM, 5.0);
        spawnerChances.put(EntityType.DOLPHIN, 4.0);
        spawnerChances.put(EntityType.WARDEN, 3.0);
    }
    
    /**
     * Create a mystery spawner item
     */
    public ItemStack createMysterySpawner() {
        ItemStack spawner = new ItemStack(Material.SPAWNER);
        ItemMeta meta = spawner.getItemMeta();
        
        if (meta != null) {
            meta.setDisplayName("§6§l✦ Mystery Spawner ✦");
            
            List<String> lore = new ArrayList<>();
            lore.add("§7Right-click to unlock");
            lore.add("§7a random mob spawner!");
            lore.add("§7");
            lore.add("§eChances:");
            lore.add("§7• §aCommon §7(Farm): §a50%");
            lore.add("§7• §bUncommon §7(Grinders): §b35%");
            lore.add("§7• §6Rare §7(Iron Golem, Dolphin): §69%");
            lore.add("§7• §d§lWarden Spawner §7- §d3% Chance!");
            meta.setLore(lore);
            
            spawner.setItemMeta(meta);
        }
        
        return spawner;
    }
    
    /**
     * Check if item is a mystery spawner
     */
    public boolean isMysterySpawner(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        
        ItemMeta meta = item.getItemMeta();
        String displayName = meta.getDisplayName();
        
        return displayName != null && displayName.contains("Mystery Spawner");
    }
    
    /**
     * Get a random spawner type based on configured chances
     */
    public EntityType getRandomSpawner() {
        double random = Math.random() * 100;
        double cumulative = 0;
        
        for (Map.Entry<EntityType, Double> entry : spawnerChances.entrySet()) {
            cumulative += entry.getValue();
            if (random <= cumulative) {
                return entry.getKey();
            }
        }
        
        // Fallback to first entry
        return spawnerChances.keySet().iterator().next();
    }
    
    /**
     * Create a spawner item for a specific mob type
     */
    public ItemStack createSpawnerForMob(EntityType mobType) {
        ItemStack spawner = new ItemStack(Material.SPAWNER);
        
        try {
            // Set NBT data for the spawner entity type
            BlockStateMeta meta = (BlockStateMeta) spawner.getItemMeta();
            if (meta != null) {
                CreatureSpawner spawnerState = (CreatureSpawner) meta.getBlockState();
                spawnerState.setSpawnedType(mobType);
                spawnerState.setDelay(200);
                spawnerState.setMaxNearbyEntities(16);
                spawnerState.setMaxSpawnDelay(800);
                spawnerState.setMinSpawnDelay(200);
                spawnerState.setRequiredPlayerRange(16);
                spawnerState.setSpawnRange(4);
                
                meta.setBlockState(spawnerState);
                
                String mobName = formatMobName(mobType);
                meta.setDisplayName("§b§l" + mobName + " Spawner");
                
                List<String> lore = new ArrayList<>();
                lore.add("§7Mob Type: §f" + mobType.name());
                lore.add("§7Place to use");
                meta.setLore(lore);
                
                spawner.setItemMeta(meta);
            }
        } catch (Exception e) {
            // Fallback if BlockStateMeta fails
            System.out.println("[SimpleKits] Error setting spawner NBT: " + e.getMessage());
            ItemMeta meta = spawner.getItemMeta();
            if (meta != null) {
                String mobName = formatMobName(mobType);
                meta.setDisplayName("§b§l" + mobName + " Spawner");
                List<String> lore = new ArrayList<>();
                lore.add("§7Mob Type: §f" + mobType.name());
                lore.add("§7Place to use");
                meta.setLore(lore);
                spawner.setItemMeta(meta);
            }
        }
        
        return spawner;
    }
    
    /**
     * Format mob entity type name for display
     */
    private String formatMobName(EntityType type) {
        String name = type.name().replace("_", " ");
        String[] words = name.split(" ");
        
        StringBuilder result = new StringBuilder();
        for (String word : words) {
            result.append(word.substring(0, 1).toUpperCase())
                  .append(word.substring(1).toLowerCase())
                  .append(" ");
        }
        
        return result.toString().trim();
    }
    
    /**
     * Get rarity color for a mob type
     */
    public String getRarityColor(EntityType type) {
        double chance = spawnerChances.getOrDefault(type, 0.0);
        
        if (chance >= 10) {
            return "§a";  // Green - Common
        } else if (chance >= 3) {
            return "§e";  // Yellow - Uncommon
        } else if (chance >= 1) {
            return "§5";  // Purple - Rare
        } else {
            return "§4";  // Red - Legendary
        }
    }
    
    /**
     * Get all mob types with their chances
     */
    public Map<EntityType, Double> getAllSpawnerChances() {
        return new LinkedHashMap<>(spawnerChances);
    }
    
    /**
     * Set chance for a mob type
     */
    public void setSpawnerChance(EntityType type, double chance) {
        spawnerChances.put(type, chance);
    }
}
