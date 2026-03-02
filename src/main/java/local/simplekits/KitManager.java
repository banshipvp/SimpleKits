package local.simplekits;

import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * Enhanced GKit Manager with Cosmic-style kits featuring proper custom enchantments
 */
public class KitManager {

    private final JavaPlugin plugin;
    private final Map<String, GKit> kits = new HashMap<>();
    private final Map<String, Long> kitCooldowns = new HashMap<>();
    private final Random random = new Random();
    private static final long COOLDOWN_MS = TimeUnit.HOURS.toMillis(24);
    
    private Object enchantmentManager; // Using Object to avoid compile-time dependency
    
    public KitManager(JavaPlugin plugin) {
        this.plugin = plugin;
        tryLoadEnchantmentManager();
    }
    
    private void tryLoadEnchantmentManager() {
        try {
            Object factionPlugin = plugin.getServer().getPluginManager().getPlugin("FactionEnchants");
            if (factionPlugin != null) {
                enchantmentManager = factionPlugin.getClass().getMethod("getEnchantmentManager").invoke(factionPlugin);
                plugin.getLogger().info("Successfully hooked into FactionEnchants!");
            } else {
                plugin.getLogger().warning("FactionEnchants not found - custom enchants will show as lore only");
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to hook into FactionEnchants: " + e.getMessage());
        }
    }
    
    public void loadKits() {
        createCosmicStyleKits();
    }
    
    private void createCosmicStyleKits() {
       // Kit 1: Paladin - Heavy armor with defensive + support enchants
        GKit paladin = new GKit("paladin", "§6§lPaladin", "§7Blessed warrior with divine protection", 24);
        paladin.addItem(createCustomArmorPiece("§6§lHelmet of the Holy Order", Material.DIAMOND_HELMET,
            new String[]{"protection", "unbreaking", "armored", "enlighted", "angelicv"},
            new int[]{4, 3, 4, 3, 1}));
        paladin.addItem(createCustomArmorPiece("§b§lDivine Paladin Chestplate", Material.DIAMOND_CHESTPLATE,
            new String[]{"protection", "unbreaking", "overload", "blessed", "spirits", "tank", "shockwave"},
            new int[]{4, 3, 2, 1, 2, 3, 5}));
        paladin.addItem(createCustomArmorPiece("§6§lPristess of Athena", Material.DIAMOND_LEGGINGS,
            new String[]{"protection", "unbreaking", "inversion", "enlighted", "valor", "reinforced"},
            new int[]{4, 3, 1, 3, 3, 2}));
        paladin.addItem(createCustomArmorPiece("§9§lBoots of the Holy Order", Material.DIAMOND_BOOTS,
            new String[]{"protection", "unbreaking", "armored", "enlighted", "valor", "rocketesca"},
            new int[]{4, 3, 1, 1, 2, 2}));
        kits.put("paladin", paladin);
        
        // Kit 2: Berserker - Full offensive melee set
        GKit berserker = new GKit("berserker", "§c§lBerserker", "§7Unleash devastating fury", 24);
        berserker.addItem(createCustomWeapon("§c§lWrath of the Bloodmoon", Material.NETHERITE_SWORD,
            new String[]{"sharpness", "unbreaking", "lifesteal", "rage", "doublestrike", "enlighted"},
            new int[]{6, 3, 3, 5, 2, 2}));
        berserker.addItem(createCustomArmorPiece("§c§lBerserker's Helmet", Material.NETHERITE_HELMET,
            new String[]{"protection", "unbreaking", "adrenaline", "enrage"},
            new int[]{5, 3, 3, 2}));
        berserker.addItem(createCustomArmorPiece("§c§lCrimson Chestplate", Material.NETHERITE_CHESTPLATE,
            new String[]{"protection", "unbreaking", "overload", "implants", "reinforce"},
            new int[]{5, 3, 2, 3, 2}));
        berserker.addItem(createCustomArmorPiece("§c§lBerserker Leggings", Material.NETHERITE_LEGGINGS,
            new String[]{"protection", "unbreaking", "spirits"},
            new int[]{5, 3, 2}));
        berserker.addItem(createCustomArmorPiece("§c§lWarrior Boots", Material.NETHERITE_BOOTS,
            new String[]{"protection", "unbreaking", "springs","feather_falling"},
            new int[]{5, 3, 3, 4}));
        berserker.addItem(new ItemStack(Material.GOLDEN_APPLE, 16));
        kits.put("berserker", berserker);
        
        // Kit 3: Enchantment Kit - Random books and dust
        GKit enchantKit = new GKit("enchantment", "§5§lEnchantment", "§dRandomized enchantment rewards", 24);
        // This kit generates random items on claim
        kits.put("enchantment", enchantKit);
        
        // Kit 4: Raider - TNT, creeper eggs, explosive gear
        GKit raider = new GKit("raider", "§2§lRaider", "§7Breach enemy defenses", 24);
        raider.addItem(createCustomWeapon("§2§lRaider's Edge", Material.DIAMOND_SWORD,
            new String[]{"sharpness", "unbreaking", "explosive"},
            new int[]{5, 3, 1}));
        raider.addItem(createCustomArmorPiece("§2§lExplosive Helmet", Material.DIAMOND_HELMET,
            new String[]{"protection", "unbreaking", "blast_protection", "ghost"},
            new int[]{4, 3, 4, 2}));
        raider.addItem(createCustomArmorPiece("§2§lCreeper Chestplate", Material.DIAMOND_CHESTPLATE,
            new String[]{"protection", "unbreaking", "creeperarmor", "ghost"},
            new int[]{4, 3, 2, 2}));
        raider.addItem(createCustomArmorPiece("§2§lRaider Leggings", Material.DIAMOND_LEGGINGS,
            new String[]{"protection", "unbreaking", "ghost", "creeperarmor"},
            new int[]{4, 3, 2, 1}));
        raider.addItem(createCustomArmorPiece("§2§lTactical Boots", Material.DIAMOND_BOOTS,
            new String[]{"protection", "unbreaking", "depth_strider", "ghost"},
            new int[]{4, 3, 3, 1}));
        raider.addItem(new ItemStack(Material.TNT, 64));
        raider.addItem(new ItemStack(Material.TNT, 64)); // Extra TNT
        raider.addItem(createNamedItem("§aCreeper Spawn Egg", Material.CREEPER_SPAWN_EGG, 32));
        raider.addItem(new ItemStack(Material.DISPENSER, 16));
        raider.addItem(new ItemStack(Material.FLINT_AND_STEEL, 1));
        raider.addItem(new ItemStack(Material.REDSTONE, 64));
        raider.addItem(new ItemStack(Material.OBSERVER, 8));
        kits.put("raider", raider);
        
        // Kit 5: Archer - Bow focus with ranged enchants
        GKit archer = new GKit("archer", "§a§lArcher", "§7Master of ranged combat", 24);
        archer.addItem(createCustomWeapon("§a§lDragon's Breath Bow", Material.BOW,
            new String[]{"power", "unbreaking", "flame", "infinity", "snipe", "volley", "piercing"},
            new int[]{5, 3, 1, 1, 3, 2, 2}));
        archer.addItem(createCustomArmorPiece("§a§lHunter's Hood", Material.DIAMOND_HELMET,
            new String[]{"protection", "unbreaking", "dodge"},
            new int[]{4, 3, 2}));
        archer.addItem(createCustomArmorPiece("§a§lRanger Tunic", Material.DIAMOND_CHESTPLATE,
            new String[]{"protection", "unbreaking", "lightweight"},
            new int[]{4, 3, 2}));
               archer.addItem(createCustomArmorPiece("§a§lSwift Leggings", Material.DIAMOND_LEGGINGS,
            new String[]{"protection", "unbreaking", "featherweight"},
            new int[]{4, 3, 2}));
        archer.addItem(createCustomArmorPiece("§a§lTracker Boots", Material.DIAMOND_BOOTS,
            new String[]{"protection", "unbreaking", "depth_strider", "feather_falling"},
            new int[]{4, 3, 3, 4}));
        archer.addItem(new ItemStack(Material.ARROW, 64));
        archer.addItem(new ItemStack(Material.GOLDEN_APPLE, 8));
        kits.put("archer", archer);
        
        // Kit 6: Miner - Mining tools with custom enchants
        GKit miner = new GKit("miner", "§6§lMiner", "§7Ultimate mining efficiency", 24);
        miner.addItem(createCustomTool("§6§lDwarven Pickaxe", Material.DIAMOND_PICKAXE,
            new String[]{"efficiency", "fortune", "unbreaking", "autosmelt", "detonate", "telepathy", "haste"},
            new int[]{5, 3, 3, 1, 3, 1, 2}));
        miner.addItem(createCustomTool("§6§lExcavator Shovel", Material.DIAMOND_SHOVEL,
            new String[]{"efficiency", "unbreaking", "autosmelt", "telepathy", "haste"},
            new int[]{5, 3, 1, 1, 2}));
        miner.addItem(createCustomTool("§6§lDriller", Material.DIAMOND_PICKAXE,
            new String[]{"efficiency", "unbreaking", "obsidianbreaker"},
            new int[]{5, 3, 2}));
        miner.addItem(createCustomWeapon("§6§lMiner's Blade", Material.DIAMOND_SWORD,
            new String[]{"sharpness", "unbreaking"},
            new int[]{5, 3}));
        miner.addItem(new ItemStack(Material.TORCH, 64));
        miner.addItem(new ItemStack(Material.GOLDEN_APPLE, 8));
        kits.put("miner", miner);
        
        // Kit 7: Assassin - Stealth and critical damage
        GKit assassin = new GKit("assassin", "§8§lAssassin", "§7Strike from the shadows", 24);
        assassin.addItem(createCustomWeapon("§8§lVoid Dagger", Material.NETHERITE_SWORD,
            new String[]{"sharpness", "unbreaking", "assassin", "silence", "invisible"},
            new int[]{6, 3, 3, 2, 1}));
        assassin.addItem(createCustomArmorPiece("§8§lShadow Cowl", Material.NETHERITE_HELMET,
            new String[]{"protection", "unbreaking", "disappear"},
            new int[]{5, 3, 2}));
        assassin.addItem(createCustomArmorPiece("§8§lCloak of Shadows", Material.NETHERITE_CHESTPLATE,
            new String[]{"protection", "unbreaking", "lightweight"},
            new int[]{5, 3, 3}));
        assassin.addItem(new ItemStack(Material.GOLDEN_APPLE, 12));
        assassin.addItem(new ItemStack(Material.ENDER_PEARL, 16));
        kits.put("assassin", assassin);
        
        // Kit 8: Tank - Maximum defense
        GKit tank = new GKit("tank", "§7§lTank", "§7Unbreakable defense", 24);
        tank.addItem(createCustomWeapon("§7§lBastioneer Blade", Material.NETHERITE_SWORD,
            new String[]{"sharpness", "unbreaking"},
            new int[]{5, 3}));
        tank.addItem(createCustomArmorPiece("§7§lFortress Helmet", Material.NETHERITE_HELMET,
            new String[]{"protection", "unbreaking", "blast_protection", "obsidianshield", "guardians"},
            new int[]{5, 3, 4, 2, 3}));
        tank.addItem(createCustomArmorPiece("§7§lImpenetrable Chestplate", Material.NETHERITE_CHESTPLATE,
            new String[]{"protection", "unbreaking", "overload", "tank", "reinforcedtank", "hardened", "reflect"},
            new int[]{6, 3, 3, 4, 2, 3, 2}));
        tank.addItem(createCustomArmorPiece("§7§lBulwark Leggings", Material.NETHERITE_LEGGINGS,
            new String[]{"protection", "unbreaking", "heavy", "reinforced"},
            new int[]{5, 3, 3, 2}));
        tank.addItem(createCustomArmorPiece("§7§lIronclad Boots", Material.NETHERITE_BOOTS,
            new String[]{"protection", "unbreaking", "thorns"},
            new int[]{5, 3, 3}));
        tank.addItem(new ItemStack(Material.SHIELD, 1));
        tank.addItem(new ItemStack(Material.ENCHANTED_GOLDEN_APPLE, 8));
        tank.addItem(new ItemStack(Material.TOTEM_OF_UNDYING, 1));
        kits.put("tank", tank);
        
        // Kit 9: Warlock - Magic/Effects based
        GKit warlock = new GKit("warlock", "§5§lWarlock", "§7Master of dark arts", 24);
        warlock.addItem(createCustomWeapon("§5§lStaff of Corruption", Material.NETHERITE_SWORD,
            new String[]{"sharpness", "unbreaking", "corrupt", "wither", "voodoo", "hex"},
            new int[]{6, 3, 3, 2, 2, 2}));
        warlock.addItem(createCustomArmorPiece("§5§lSorcerer's Crown", Material.DIAMOND_HELMET,
            new String[]{"protection", "unbreaking", "metaphysical"},
            new int[]{4, 3, 2}));
        warlock.addItem(createCustomArmorPiece("§5§lRobes of Power", Material.DIAMOND_CHESTPLATE,
            new String[]{"protection", "unbreaking", "godlyoverload"},
            new int[]{4, 3, 1}));
        warlock.addItem(new ItemStack(Material.POTION, 5)); // Health potions
        warlock.addItem(new ItemStack(Material.GOLDEN_APPLE, 12));
        kits.put("warlock", warlock);
        
        // Kit 10: Duelist - Balanced PvP kit
        GKit duelist = new GKit("duelist", "§e§lDuelist", "§7Balanced warrior for fair combat", 24);
        duelist.addItem(createCustomWeapon("§e§lRapier of Honor", Material.DIAMOND_SWORD,
            new String[]{"sharpness", "unbreaking", "block", "dodge", "deathbringer"},
            new int[]{5, 3, 2, 2, 3}));
        duelist.addItem(createCustomArmorPiece("§e§lDuelist Helmet", Material.DIAMOND_HELMET,
            new String[]{"protection", "unbreaking", "clarity"},
            new int[]{4, 3, 2}));
        duelist.addItem(createCustomArmorPiece("§e§lBattleplate", Material.DIAMOND_CHESTPLATE,
            new String[]{"protection", "unbreaking", "overload", "valor"},
            new int[]{4, 3, 2, 3}));
        duelist.addItem(createCustomArmorPiece("§e§lCombat Leggings", Material.DIAMOND_LEGGINGS,
            new String[]{"protection", "unbreaking"},
            new int[]{4, 3}));
        duelist.addItem(createCustomArmorPiece("§e§lDueling Boots", Material.DIAMOND_BOOTS,
            new String[]{"protection", "unbreaking", "feather_falling"},
            new int[]{4, 3, 4}));
        duelist.addItem(new ItemStack(Material.GOLDEN_APPLE, 12));
        duelist.addItem(new ItemStack(Material.ENDER_PEARL, 8));
        kits.put("duelist", duelist);
    }
    
    private ItemStack createCustomWeapon(String name, Material material, String[] enchantIds, int[] levels) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        item.setItemMeta(meta);
        
        // Apply vanilla enchants
        if (material == Material.BOW) {
            item.addUnsafeEnchantment(Enchantment.ARROW_DAMAGE, 5);
        } else {
            item.addUnsafeEnchantment(Enchantment.DAMAGE_ALL, 5);
        }
        item.addUnsafeEnchantment(Enchantment.DURABILITY, 3);
        
        // Apply custom enchants with proper coloring
        return applyCustomEnchants(item, enchantIds, levels, true);
    }
    
    private ItemStack createCustomTool(String name, Material material, String[] enchantIds, int[] levels) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        item.setItemMeta(meta);
        
        // Apply vanilla enchants
        item.addUnsafeEnchantment(Enchantment.DIG_SPEED, 5);
        item.addUnsafeEnchantment(Enchantment.LOOT_BONUS_BLOCKS, 3);
        item.addUnsafeEnchantment(Enchantment.DURABILITY, 3);
        
        // Apply custom enchants
        return applyCustomEnchants(item, enchantIds, levels, true);
    }
    
    private ItemStack createCustomArmorPiece(String name, Material material, String[] enchantIds, int[] levels) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        item.setItemMeta(meta);
        
        // Apply vanilla enchants
        item.addUnsafeEnchantment(Enchantment.PROTECTION_ENVIRONMENTAL, 4);
        item.addUnsafeEnchantment(Enchantment.DURABILITY, 3);
        
        // Apply custom enchants
        return applyCustomEnchants(item, enchantIds, levels, true);
    }
    
    private ItemStack applyCustomEnchants(ItemStack item, String[] enchantIds, int[] levels, boolean addLore) {
        if (enchantmentManager == null || enchantIds == null || levels == null) {
            // If no enchantment manager, just add lore with default colors
            return applyCustomEnchantsAsLore(item, enchantIds, levels);
        }
        
        ItemMeta meta = item.getItemMeta();
        List<String> lore = meta.hasLore() ? new ArrayList<>(meta.getLore()) : new ArrayList<>();
        
        try {
            for (int i = 0; i < Math.min(enchantIds.length, levels.length); i++) {
                String enchantId = enchantIds[i].toLowerCase();
                int level = levels[i];
                
                // Skip vanilla enchants
                if (isVanillaEnchant(enchantId)) {
                    continue;
                }
                
                // Use reflection to get custom enchant
                Object customEnchant = enchantmentManager.getClass()
                    .getMethod("getEnchantment", String.class)
                    .invoke(enchantmentManager, enchantId);
                    
                if (customEnchant != null && addLore) {
                    String displayName = (String) customEnchant.getClass().getMethod("getDisplayName").invoke(customEnchant);
                    Object tierObj = customEnchant.getClass().getMethod("getTier").invoke(customEnchant);
                    String tierName = tierObj.toString();
                    String color = getTierColorByName(tierName);
                    String romanLevel = toRoman(level);
                    lore.add(color + displayName + " " + romanLevel);
                }
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Error applying custom enchants: " + e.getMessage());
            return applyCustomEnchantsAsLore(item, enchantIds, levels);
        }
        
        if (!lore.isEmpty()) {
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        
        return item;
    }
    
    private ItemStack applyCustomEnchantsAsLore(ItemStack item, String[] enchantIds, int[] levels) {
        ItemMeta meta = item.getItemMeta();
        List<String> lore = meta.hasLore() ? new ArrayList<>(meta.getLore()) : new ArrayList<>();
        
        for (int i = 0; i < Math.min(enchantIds.length, levels.length); i++) {
            String enchantId = enchantIds[i];
            int level = levels[i];
            
            if (isVanillaEnchant(enchantId)) {
                continue;
            }
            
            String color = guessEnchantColor(enchantId);
            String displayName = prettifyEnchantName(enchantId);
            String romanLevel = toRoman(level);
            lore.add(color + displayName + " " + romanLevel);
        }
        
        if (!lore.isEmpty()) {
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        
        return item;
    }
    
    private boolean isVanillaEnchant(String enchantId) {
        String id = enchantId.toLowerCase();
        return id.equals("sharpness") || id.equals("protection") || 
               id.equals("unbreaking") || id.equals("efficiency") ||
               id.equals("fortune") || id.equals("power") || 
               id.equals("flame") || id.equals("infinity") ||
               id.equals("blast_protection") || id.equals("thorns") ||
               id.equals("depth_strider") || id.equals("feather_falling");
    }
    
    private String guessEnchantColor(String enchantId) {
        String id = enchantId.toLowerCase();
        // Legendary enchants (orange/gold)
        if (id.contains("overload") || id.contains("armored") || id.contains("enlighted") || 
            id.contains("rage") || id.contains("blessed") || id.contains("lifesteal") || 
            id.contains("doublestrike") || id.contains("silence") || id.contains("deathbringer") || 
            id.contains("clarity")) {
            return "§6";
        }
        // Heroic enchants (purple)
        if (id.contains("angelic") || id.contains("godly") || id.contains("reinforcedtank") ||
            id.contains("reflect") || id.contains("soulbound") || id.contains("tank")) {
            return "§d";
        }
        // Ultimate enchants (yellow)
        if (id.contains("assassin") || id.contains("spirits") || id.contains("valor") || 
            id.contains("guardians") || id.contains("implants") || id.contains("obsidianshield") || 
            id.contains("disappear") || id.contains("heavy") || id.contains("marksman") || 
            id.contains("longbow") || id.contains("metaphysical") || id.contains("restore") || 
            id.contains("pickpocket") || id.contains("enrage") || id.contains("adrenaline") || 
            id.contains("dodge") || id.contains("block")) {
            return "§e";
        }
        // Elite enchants (cyan)
        if (id.contains("shockwave") || id.contains("frozen") || id.contains("wither") ||
            id.contains("voodoo") || id.contains("execute") || id.contains("trap") ||
            id.contains("reforged") || id.contains("hijack") || id.contains("infernal") ||
            id.contains("creeperarmor") || id.contains("rocket") || id.contains("greatsword")) {
            return "§b";
        }
        // Unique enchants (green)
        if (id.contains("ghost") || id.contains("lightweight") || id.contains("featherweight") ||
            id.contains("berserk") || id.contains("sustain")) {
            return "§a";
        }
        // Soul (red)
        if (id.contains("soul") || id.contains("corrupt")) {
            return "§c";
        }
        // Default to white (simple)
        return "§f";
    }
    
    private String prettifyEnchantName(String enchantId) {
        String[] words = enchantId.split("(?=[A-Z])");
        StringBuilder result = new StringBuilder();
        for (String word : words) {
            if (!word.isEmpty()) {
                result.append(Character.toUpperCase(word.charAt(0)))
                      .append(word.substring(1).toLowerCase())
                      .append(" ");
            }
        }
        return result.toString().trim();
    }
    
    private String getTierColorByName(String tierName) {
        return switch (tierName.toUpperCase()) {
            case "SIMPLE" -> "§f";
            case "UNIQUE" -> "§a";
            case "ELITE" -> "§b";
            case "ULTIMATE" -> "§e";
            case "LEGENDARY" -> "§6";
            case "SOUL" -> "§c";
            case "HEROIC" -> "§d";
            case "MASTERY" -> "§4";
            default -> "§7";
        };
    }
    
    private String toRoman(int number) {
        return switch (number) {
            case 1 -> "I";
            case 2 -> "II";
            case 3 -> "III";
            case 4 -> "IV";
            case 5 -> "V";
            case 6 -> "VI";
            case 7 -> "VII";
            case 8 -> "VIII";
            case 9 -> "IX";
            case 10 -> "X";
            default -> String.valueOf(number);
        };
    }
    
    private ItemStack createNamedItem(String name, Material material, int amount) {
        ItemStack item = new ItemStack(material, amount);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        item.setItemMeta(meta);
        return item;
    }
    
    public GKit getKit(String name) {
        return kits.get(name.toLowerCase());
    }
    
    public Collection<GKit> getAllKits() {
        return kits.values();
    }
    
    public int getKitCount() {
        return kits.size();
    }
    
    public boolean canClaimKit(UUID playerId, String kitName) {
        String key = playerId + ":" + kitName.toLowerCase();
        Long cooldownEnd = kitCooldowns.get(key);
        
        if (cooldownEnd == null) {
            return true;
        }
        
        if (System.currentTimeMillis() > cooldownEnd) {
            kitCooldowns.remove(key);
            return true;
        }
        
        return false;
    }
    
    public int getRemainingCooldownHours(UUID playerId, String kitName) {
        String key = playerId + ":" + kitName.toLowerCase();
        Long cooldownEnd = kitCooldowns.get(key);
        
        if (cooldownEnd == null || System.currentTimeMillis() > cooldownEnd) {
            kitCooldowns.remove(key);
            return 0;
        }
        
        long remainingMs = cooldownEnd - System.currentTimeMillis();
        return (int) (remainingMs / 3600000);
    }
    
    public void setKitCooldown(UUID playerId, String kitName) {
        String key = playerId + ":" + kitName.toLowerCase();
        long cooldownEnd = System.currentTimeMillis() + COOLDOWN_MS;
        kitCooldowns.put(key, cooldownEnd);
    }
    
    public void clearCooldown(UUID playerId, String kitName) {
        String key = playerId + ":" + kitName.toLowerCase();
        kitCooldowns.remove(key);
    }
}
