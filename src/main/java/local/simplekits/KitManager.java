package local.simplekits;

import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * Manages all gkits and player kit cooldowns
 * Each kit has a 24-hour cooldown
 */
public class KitManager {

    private final JavaPlugin plugin;
    private final Map<String, GKit> kits = new HashMap<>();
    
    // Tracks kit cooldowns: UUID + kitName -> cooldown end time (ms)
    private final Map<String, Long> kitCooldowns = new HashMap<>();
    
    private static final long COOLDOWN_MS = TimeUnit.HOURS.toMillis(24);  // 24 hour cooldown
    
    public KitManager(JavaPlugin plugin) {
        this.plugin = plugin;
    }
    
    /**
     * Load all kits from config and create default ones
     */
    public void loadKits() {
        // Create the 10 default gkits with placeholder items
        createDefaultKits();
    }
    
    /**
     * Create 10 default gkits with proper vanilla enchantments
     */
    private void createDefaultKits() {
        // gkit 1: Starter Kit
        GKit starter = new GKit("starter", "§b§lStarter Kit", "§7Starter raid/build essentials", 24);
        starter.addItem(createStarterSword("§6Starter Sword", Material.IRON_SWORD));
        starter.addItem(createStarterArmor("§6Starter Helmet", Material.IRON_HELMET));
        starter.addItem(createStarterArmor("§6Starter Chestplate", Material.IRON_CHESTPLATE));
        starter.addItem(createStarterArmor("§6Starter Leggings", Material.IRON_LEGGINGS));
        starter.addItem(createStarterArmor("§6Starter Boots", Material.IRON_BOOTS));
        starter.addItem(new ItemStack(Material.SHIELD));
        starter.addItem(createNamedItem("§eSimple Crate", Material.CHEST, 1));
        starter.addItem(new ItemStack(Material.OAK_LOG, 64));
        starter.addItem(new ItemStack(Material.SAND, 64));
        starter.addItem(new ItemStack(Material.OBSIDIAN, 32));
        starter.addItem(new ItemStack(Material.TNT, 32));
        starter.addItem(new ItemStack(Material.BREAD, 64));
        kits.put("starter", starter);
        
        // gkit 2: Fire Guardian
        GKit fire = new GKit("fire", "§c§lFire Guardian", "§7Fire-themed combat kit", 24);
        fire.addItem(createEnchantedSword("§cIncinerate Sword", Material.DIAMOND_SWORD));
        fire.addItem(createEnchantedArmor("§cMolten Helmet", Material.DIAMOND_HELMET));
        fire.addItem(createEnchantedArmor("§cMolten Chestplate", Material.DIAMOND_CHESTPLATE));
        fire.addItem(createEnchantedArmor("§cMolten Leggings", Material.DIAMOND_LEGGINGS));
        fire.addItem(createEnchantedArmor("§cMolten Boots", Material.DIAMOND_BOOTS));
        fire.addItem(new ItemStack(Material.SHIELD));
        fire.addItem(new ItemStack(Material.GOLDEN_APPLE, 16));
        kits.put("fire", fire);
        
        // gkit 3: Ice Warrior
        GKit ice = new GKit("ice", "§b§lIce Warrior", "§7Frost-focused combat kit", 24);
        ice.addItem(createEnchantedSword("§bFrozen Blade", Material.DIAMOND_SWORD));
        ice.addItem(createEnchantedArmor("§bFrozen Helmet", Material.DIAMOND_HELMET));
        ice.addItem(createEnchantedArmor("§bFrozen Chestplate", Material.DIAMOND_CHESTPLATE));
        ice.addItem(createEnchantedArmor("§bFrozen Leggings", Material.DIAMOND_LEGGINGS));
        ice.addItem(createEnchantedArmor("§bFrozen Boots", Material.DIAMOND_BOOTS));
        ice.addItem(new ItemStack(Material.SHIELD));
        ice.addItem(new ItemStack(Material.GOLDEN_APPLE, 16));
        kits.put("ice", ice);
        
        // gkit 4: Miner's Fortune
        GKit miner = new GKit("miner", "§6§lMiner's Fortune", "§7Mining and resource gathering", 24);
        miner.addItem(createEnchantedPickaxe("§6Lucky Pickaxe", Material.DIAMOND_PICKAXE));
        miner.addItem(createEnchantedPickaxe("§6Fortune Pick", Material.DIAMOND_PICKAXE));
        miner.addItem(createEnchantedArmor("§6Miner Helmet", Material.DIAMOND_HELMET));
        miner.addItem(createEnchantedArmor("§6Miner Chestplate", Material.DIAMOND_CHESTPLATE));
        miner.addItem(createEnchantedArmor("§6Miner Leggings", Material.DIAMOND_LEGGINGS));
        miner.addItem(createEnchantedArmor("§6Miner Boots", Material.DIAMOND_BOOTS));
        miner.addItem(createEnchantedSword("§6Miner Sword", Material.DIAMOND_SWORD));
        miner.addItem(new ItemStack(Material.TORCH, 64));
        miner.addItem(new ItemStack(Material.GOLDEN_APPLE, 8));
        kits.put("miner", miner);
        
        // gkit 5: Vampire's Kiss
        GKit vampire = new GKit("vampire", "§4§lVampire's Kiss", "§7Life-steal combat kit", 24);
        vampire.addItem(createEnchantedSword("§4Vampiric Blade", Material.DIAMOND_SWORD));
        vampire.addItem(createEnchantedArmor("§4Dark Helmet", Material.DIAMOND_HELMET));
        vampire.addItem(createEnchantedArmor("§4Dark Chestplate", Material.DIAMOND_CHESTPLATE));
        vampire.addItem(createEnchantedArmor("§4Dark Leggings", Material.DIAMOND_LEGGINGS));
        vampire.addItem(createEnchantedArmor("§4Dark Boots", Material.DIAMOND_BOOTS));
        vampire.addItem(new ItemStack(Material.SHIELD));
        vampire.addItem(new ItemStack(Material.GOLDEN_APPLE, 16));
        kits.put("vampire", vampire);
        
        // gkit 6: Thunder Lord
        GKit thunder = new GKit("thunder", "§e§lThunder Lord", "§7Lightning-based combat", 24);
        thunder.addItem(createEnchantedSword("§eThundering Blade", Material.DIAMOND_SWORD));
        thunder.addItem(createEnchantedArmor("§eThunder Helmet", Material.DIAMOND_HELMET));
        thunder.addItem(createEnchantedArmor("§eThunder Chestplate", Material.DIAMOND_CHESTPLATE));
        thunder.addItem(createEnchantedArmor("§eThunder Leggings", Material.DIAMOND_LEGGINGS));
        thunder.addItem(createEnchantedArmor("§eThunder Boots", Material.DIAMOND_BOOTS));
        thunder.addItem(createEnchantedBow("§eVoltage Bow", Material.BOW));
        thunder.addItem(new ItemStack(Material.ARROW, 64));
        thunder.addItem(new ItemStack(Material.GOLDEN_APPLE, 16));
        kits.put("thunder", thunder);
        
        // gkit 7: Assassin's Edge
        GKit assassin = new GKit("assassin", "§8§lAssassin's Edge", "§7High damage and speed", 24);
        assassin.addItem(createEnchantedSword("§8Shadow Blade", Material.NETHERITE_SWORD));
        assassin.addItem(createEnchantedArmor("§8Shadow Helmet", Material.NETHERITE_HELMET));
        assassin.addItem(createEnchantedArmor("§8Shadow Chestplate", Material.NETHERITE_CHESTPLATE));
        assassin.addItem(createEnchantedArmor("§8Shadow Leggings", Material.NETHERITE_LEGGINGS));
        assassin.addItem(createEnchantedArmor("§8Shadow Boots", Material.NETHERITE_BOOTS));
        assassin.addItem(new ItemStack(Material.SHIELD));
        assassin.addItem(new ItemStack(Material.GOLDEN_APPLE, 16));
        kits.put("assassin", assassin);
        
        // gkit 8: Tank's Fortress
        GKit tank = new GKit("tank", "§7§lTank's Fortress", "§7Heavy defense and endurance", 24);
        tank.addItem(createEnchantedSword("§7Fortress Sword", Material.DIAMOND_SWORD));
        tank.addItem(createEnchantedArmor("§7Tanky Helm", Material.NETHERITE_HELMET));
        tank.addItem(createEnchantedArmor("§7Tanky Chestplate", Material.NETHERITE_CHESTPLATE));
        tank.addItem(createEnchantedArmor("§7Tanky Leggings", Material.NETHERITE_LEGGINGS));
        tank.addItem(createEnchantedArmor("§7Tanky Boots", Material.NETHERITE_BOOTS));
        tank.addItem(new ItemStack(Material.SHIELD));
        tank.addItem(new ItemStack(Material.ENCHANTED_GOLDEN_APPLE, 8));
        kits.put("tank", tank);
        
        // gkit 9: Archer's Precision
        GKit archer = new GKit("archer", "§a§lArcher's Precision", "§7Bow-based combat kit", 24);
        archer.addItem(createEnchantedBow("§aSnipe Bow", Material.BOW));
        archer.addItem(createEnchantedArmor("§aArcher Helmet", Material.DIAMOND_HELMET));
        archer.addItem(createEnchantedArmor("§aArcher Chestplate", Material.DIAMOND_CHESTPLATE));
        archer.addItem(createEnchantedArmor("§aArcher Leggings", Material.DIAMOND_LEGGINGS));
        archer.addItem(createEnchantedArmor("§aArcher Boots", Material.DIAMOND_BOOTS));
        archer.addItem(createEnchantedBow("§aExplosive Bow", Material.BOW));
        archer.addItem(new ItemStack(Material.ARROW, 128));
        archer.addItem(new ItemStack(Material.GOLDEN_APPLE, 16));
        kits.put("archer", archer);
        
        // gkit 10: Royalty Kit
        GKit royalty = new GKit("royalty", "§d§lRoyalty", "§7Premium endgame kit", 24);
        royalty.addItem(createEnchantedSword("§dRoyal Blade", Material.NETHERITE_SWORD));
        royalty.addItem(createEnchantedArmor("§dRoyal Helm", Material.NETHERITE_HELMET));
        royalty.addItem(createEnchantedArmor("§dRoyal Chestplate", Material.NETHERITE_CHESTPLATE));
        royalty.addItem(createEnchantedArmor("§dRoyal Leggings", Material.NETHERITE_LEGGINGS));
        royalty.addItem(createEnchantedArmor("§dRoyal Boots", Material.NETHERITE_BOOTS));
        royalty.addItem(new ItemStack(Material.SHIELD));
        royalty.addItem(new ItemStack(Material.ENCHANTED_GOLDEN_APPLE, 16));
        royalty.addItem(new ItemStack(Material.TOTEM_OF_UNDYING, 1));
        kits.put("royalty", royalty);
        
        // gkit 11: Raider Kit - Cannon and base materials
        GKit raider = new GKit("raider", "§c§lRaider", "§7Ultimate raiding and cannon kit", 24);
        raider.addItem(createCustomEnchantedPickaxe("§cRaider's Pickaxe", Material.NETHERITE_PICKAXE));
        raider.addItem(createEnchantedSword("§cRaider's Blade", Material.NETHERITE_SWORD));
        raider.addItem(createEnchantedArmor("§cRaider Helmet", Material.NETHERITE_HELMET));
        raider.addItem(createEnchantedArmor("§cRaider Chestplate", Material.NETHERITE_CHESTPLATE));
        raider.addItem(createEnchantedArmor("§cRaider Leggings", Material.NETHERITE_LEGGINGS));
        raider.addItem(createEnchantedArmor("§cRaider Boots", Material.NETHERITE_BOOTS));
        raider.addItem(new ItemStack(Material.TNT, 64));
        raider.addItem(new ItemStack(Material.TNT, 64));
        raider.addItem(new ItemStack(Material.SAND, 64));
        raider.addItem(new ItemStack(Material.SAND, 64));
        raider.addItem(new ItemStack(Material.OBSIDIAN, 64));
        raider.addItem(new ItemStack(Material.WATER_BUCKET, 1));
        raider.addItem(new ItemStack(Material.REDSTONE, 64));
        raider.addItem(new ItemStack(Material.GOLDEN_APPLE, 16));
        kits.put("raider", raider);
    }
    
    /**
     * Helper to create enchanted sword with Sharpness 6 + Unbreaking 3 + 6-9 random custom enchants
     */
    private ItemStack createEnchantedSword(String name, Material material) {
        ItemStack sword = new ItemStack(material);
        ItemMeta meta = sword.getItemMeta();
        meta.setDisplayName(name);
        sword.setItemMeta(meta);
        
        // Add vanilla enchantments with higher levels
        sword.addUnsafeEnchantment(Enchantment.DAMAGE_ALL, 6);
        sword.addUnsafeEnchantment(Enchantment.DURABILITY, 3);
        
        // Apply 6-9 random custom enchants if FactionEnchants plugin is available
        try {
            var factionEnchantsPlugin = plugin.getServer().getPluginManager().getPlugin("FactionEnchants");
            if (factionEnchantsPlugin != null) {
                plugin.getLogger().info("Found FactionEnchants plugin, applying custom enchants to sword");
                var getRandomGearManagerMethod = factionEnchantsPlugin.getClass().getMethod("getRandomGearManager");
                var randomGearManager = getRandomGearManagerMethod.invoke(factionEnchantsPlugin);
                
                var generateMethod = randomGearManager.getClass().getMethod("generateRandomEnchantedGear", ItemStack.class, int.class, int.class);
                sword = (ItemStack) generateMethod.invoke(randomGearManager, sword, 6, 9);
                plugin.getLogger().info("Successfully applied custom enchants to sword");
            } else {
                plugin.getLogger().warning("FactionEnchants plugin not found! Custom enchants will not be applied.");
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Could not apply random custom enchants to sword: " + e.getMessage());
            e.printStackTrace();
        }
        
        return sword;
    }
    
    /**
     * Helper to create enchanted pickaxe with Efficiency 5, Fortune 3, Unbreaking 3 + 4-6 random custom enchants
     */
    private ItemStack createEnchantedPickaxe(String name, Material material) {
        ItemStack pick = new ItemStack(material);
        ItemMeta meta = pick.getItemMeta();
        meta.setDisplayName(name);
        meta.setLore(Arrays.asList(
            "§dAutoSmelt",
            "§dDetonate",
            "§dTelepathy"
        ));
        pick.setItemMeta(meta);
        
        pick.addUnsafeEnchantment(Enchantment.DIG_SPEED, 5);
        pick.addUnsafeEnchantment(Enchantment.LOOT_BONUS_BLOCKS, 3);
        pick.addUnsafeEnchantment(Enchantment.DURABILITY, 3);
        
        // Apply 4-6 random custom enchants if FactionEnchants plugin is available
        try {
            var factionEnchantsPlugin = plugin.getServer().getPluginManager().getPlugin("FactionEnchants");
            if (factionEnchantsPlugin != null) {
                var getRandomGearManagerMethod = factionEnchantsPlugin.getClass().getMethod("getRandomGearManager");
                var randomGearManager = getRandomGearManagerMethod.invoke(factionEnchantsPlugin);
                
                var generateMethod = randomGearManager.getClass().getMethod("generateRandomEnchantedGear", ItemStack.class, int.class, int.class);
                pick = (ItemStack) generateMethod.invoke(randomGearManager, pick, 4, 6);
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Could not apply random custom enchants to pickaxe: " + e.getMessage());
        }
        
        return pick;
    }
    
    /**
     * Helper to create pickaxe with custom faction enchants properly applied
     */
    private ItemStack createCustomEnchantedPickaxe(String name, Material material) {
        ItemStack pick = new ItemStack(material);
        ItemMeta meta = pick.getItemMeta();
        meta.setDisplayName(name);
        pick.setItemMeta(meta);
        
        // Add vanilla enchantments
        pick.addUnsafeEnchantment(Enchantment.DIG_SPEED, 5);
        pick.addUnsafeEnchantment(Enchantment.LOOT_BONUS_BLOCKS, 4);
        pick.addUnsafeEnchantment(Enchantment.DURABILITY, 3);
        
        // Apply custom enchants if faction-enchants-plugin is available
        try {
            var factionEnchantsPlugin = plugin.getServer().getPluginManager().getPlugin("faction-enchants-plugin");
            if (factionEnchantsPlugin != null) {
                // Use reflection to avoid hard dependency
                var getEnchantManagerMethod = factionEnchantsPlugin.getClass().getMethod("getEnchantmentManager");
                var enchantManager = getEnchantManagerMethod.invoke(factionEnchantsPlugin);
                
                // Get enchantment methods via reflection
                var getEnchantmentMethod = enchantManager.getClass().getMethod("getEnchantment", String.class);
                var applyEnchantmentMethod = enchantManager.getClass().getMethod("applyEnchantment", ItemStack.class, Object.class, int.class);
                
                // Apply AutoSmelt III
                var autoSmelt = getEnchantmentMethod.invoke(enchantManager, "autosmelt");
                if (autoSmelt != null) {
                    pick = (ItemStack) applyEnchantmentMethod.invoke(enchantManager, pick, autoSmelt, 3);
                }
                
                // Apply Detonate III
                var detonate = getEnchantmentMethod.invoke(enchantManager, "detonate");
                if (detonate != null) {
                    pick = (ItemStack) applyEnchantmentMethod.invoke(enchantManager, pick, detonate, 3);
                }
                
                // Apply Telepathy III
                var telepathy = getEnchantmentMethod.invoke(enchantManager, "telepathy");
                if (telepathy != null) {
                    pick = (ItemStack) applyEnchantmentMethod.invoke(enchantManager, pick, telepathy, 3);
                }
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Could not apply custom enchants to raider pickaxe: " + e.getMessage());
        }
        
        return pick;
    }
    
    /**
     * Helper to create enchanted bow with Power 5 + Unbreaking 3 + 6-9 random custom enchants
     */
    private ItemStack createEnchantedBow(String name, Material material) {
        ItemStack bow = new ItemStack(material);
        ItemMeta meta = bow.getItemMeta();
        meta.setDisplayName(name);
        bow.setItemMeta(meta);
        
        bow.addUnsafeEnchantment(Enchantment.ARROW_DAMAGE, 5);
        bow.addUnsafeEnchantment(Enchantment.DURABILITY, 3);
        bow.addUnsafeEnchantment(Enchantment.ARROW_INFINITE, 1);
        
        // Apply 6-9 random custom enchants if FactionEnchants plugin is available
        try {
            var factionEnchantsPlugin = plugin.getServer().getPluginManager().getPlugin("FactionEnchants");
            if (factionEnchantsPlugin != null) {
                var getRandomGearManagerMethod = factionEnchantsPlugin.getClass().getMethod("getRandomGearManager");
                var randomGearManager = getRandomGearManagerMethod.invoke(factionEnchantsPlugin);
                
                var generateMethod = randomGearManager.getClass().getMethod("generateRandomEnchantedGear", ItemStack.class, int.class, int.class);
                bow = (ItemStack) generateMethod.invoke(randomGearManager, bow, 6, 9);
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Could not apply random custom enchants to bow: " + e.getMessage());
        }
        
        return bow;
    }
    
    /**
     * Helper to create enchanted armor with Protection 5 + Unbreaking 3 + 6-9 random custom enchants
     */
    private ItemStack createEnchantedArmor(String name, Material material) {
        ItemStack armor = new ItemStack(material);
        ItemMeta meta = armor.getItemMeta();
        meta.setDisplayName(name);
        armor.setItemMeta(meta);
        
        // Add vanilla enchantments with higher levels
        armor.addUnsafeEnchantment(Enchantment.PROTECTION_ENVIRONMENTAL, 5);
        armor.addUnsafeEnchantment(Enchantment.DURABILITY, 3);
        
        // Apply 6-9 random custom enchants if FactionEnchants plugin is available
        try {
            var factionEnchantsPlugin = plugin.getServer().getPluginManager().getPlugin("FactionEnchants");
            if (factionEnchantsPlugin != null) {
                // Use reflection to access RandomGearManager
                var getRandomGearManagerMethod = factionEnchantsPlugin.getClass().getMethod("getRandomGearManager");
                var randomGearManager = getRandomGearManagerMethod.invoke(factionEnchantsPlugin);
                
                // Call generateRandomEnchantedGear(ItemStack baseItem, int minEnchants, int maxEnchants)
                var generateMethod = randomGearManager.getClass().getMethod("generateRandomEnchantedGear", ItemStack.class, int.class, int.class);
                armor = (ItemStack) generateMethod.invoke(randomGearManager, armor, 6, 9);
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Could not apply random custom enchants to armor: " + e.getMessage());
        }
        
        return armor;
    }

    private ItemStack createStarterArmor(String name, Material material) {
        ItemStack armor = new ItemStack(material);
        ItemMeta meta = armor.getItemMeta();
        meta.setDisplayName(name);
        armor.setItemMeta(meta);

        armor.addUnsafeEnchantment(Enchantment.PROTECTION_ENVIRONMENTAL, 2);
        armor.addUnsafeEnchantment(Enchantment.DURABILITY, 2);
        
        // Apply 2-4 random custom enchants for starter kit
        try {
            var factionEnchantsPlugin = plugin.getServer().getPluginManager().getPlugin("FactionEnchants");
            if (factionEnchantsPlugin != null) {
                var getRandomGearManagerMethod = factionEnchantsPlugin.getClass().getMethod("getRandomGearManager");
                var randomGearManager = getRandomGearManagerMethod.invoke(factionEnchantsPlugin);
                
                var generateMethod = randomGearManager.getClass().getMethod("generateRandomEnchantedGear", ItemStack.class, int.class, int.class);
                armor = (ItemStack) generateMethod.invoke(randomGearManager, armor, 2, 4);
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Could not apply random custom enchants to starter armor: " + e.getMessage());
        }

        return armor;
    }

    private ItemStack createStarterSword(String name, Material material) {
        ItemStack sword = new ItemStack(material);
        ItemMeta meta = sword.getItemMeta();
        meta.setDisplayName(name);
        sword.setItemMeta(meta);

        sword.addUnsafeEnchantment(Enchantment.DAMAGE_ALL, 2);
        sword.addUnsafeEnchantment(Enchantment.DURABILITY, 2);
        
        // Apply 2-4 random custom enchants for starter kit
        try {
            var factionEnchantsPlugin = plugin.getServer().getPluginManager().getPlugin("FactionEnchants");
            if (factionEnchantsPlugin != null) {
                var getRandomGearManagerMethod = factionEnchantsPlugin.getClass().getMethod("getRandomGearManager");
                var randomGearManager = getRandomGearManagerMethod.invoke(factionEnchantsPlugin);
                
                var generateMethod = randomGearManager.getClass().getMethod("generateRandomEnchantedGear", ItemStack.class, int.class, int.class);
                sword = (ItemStack) generateMethod.invoke(randomGearManager, sword, 2, 4);
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Could not apply random custom enchants to starter sword: " + e.getMessage());
        }

        return sword;
    }

    private ItemStack createNamedItem(String name, Material material, int amount) {
        ItemStack item = new ItemStack(material, amount);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        item.setItemMeta(meta);
        return item;
    }
    
    /**
     * Get a gkit by name
     */
    public GKit getKit(String name) {
        return kits.get(name.toLowerCase());
    }
    
    /**
     * Get all kits
     */
    public Collection<GKit> getAllKits() {
        return kits.values();
    }
    
    /**
     * Get kit count
     */
    public int getKitCount() {
        return kits.size();
    }
    
    /**
     * Check if player can claim a kit
     */
    public boolean canClaimKit(UUID playerId, String kitName) {
        String key = playerId + ":" + kitName.toLowerCase();
        Long cooldownEnd = kitCooldowns.get(key);
        
        if (cooldownEnd == null) {
            return true;  // Never claimed before
        }
        
        if (System.currentTimeMillis() > cooldownEnd) {
            kitCooldowns.remove(key);
            return true;  // Cooldown expired
        }
        
        return false;  // Still on cooldown
    }
    
    /**
     * Get remaining cooldown time in hours for a kit
     */
    public int getRemainingCooldownHours(UUID playerId, String kitName) {
        String key = playerId + ":" + kitName.toLowerCase();
        Long cooldownEnd = kitCooldowns.get(key);
        
        if (cooldownEnd == null || System.currentTimeMillis() > cooldownEnd) {
            kitCooldowns.remove(key);
            return 0;
        }
        
        long remainingMs = cooldownEnd - System.currentTimeMillis();
        return (int) (remainingMs / 3600000);  // Convert to hours
    }
    
    /**
     * Set cooldown for a player's kit claim
     */
    public void setKitCooldown(UUID playerId, String kitName) {
        String key = playerId + ":" + kitName.toLowerCase();
        long cooldownEnd = System.currentTimeMillis() + COOLDOWN_MS;
        kitCooldowns.put(key, cooldownEnd);
    }
    
    /**
     * Clear all cooldowns for admin (future use)
     */
    public void clearCooldown(UUID playerId, String kitName) {
        String key = playerId + ":" + kitName.toLowerCase();
        kitCooldowns.remove(key);
    }
}
