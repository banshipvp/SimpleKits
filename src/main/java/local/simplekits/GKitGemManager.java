package local.simplekits;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.*;

/**
 * Manages gkit gems - items that unlock gkits when right-clicked
 * Players must use a gem to unlock a kit before they can claim it with /gkit
 */
public class GKitGemManager {

    private final SimpleKitsPlugin plugin;
    private final KitManager kitManager;
    private final NamespacedKey gemKey;
    private final Random random = new Random();
    private final Map<UUID, Set<String>> unlockedKits = new HashMap<>();

    public GKitGemManager(SimpleKitsPlugin plugin, KitManager kitManager) {
        this.plugin = plugin;
        this.kitManager = kitManager;
        this.gemKey = new NamespacedKey(plugin, "gkit_gem");
    }

    /**
     * Create a gkit gem for a specific kit
     */
    public ItemStack createGKitGem(String kitName) {
        GKit kit = kitManager.getKit(kitName);
        if (kit == null) return null;

        ItemStack gem = new ItemStack(Material.DIAMOND);
        ItemMeta meta = gem.getItemMeta();

        meta.setDisplayName("§b§l✦ " + kit.getDisplayName() + " Gem §b§l✦");

        List<String> lore = new ArrayList<>();
        lore.add("§7");
        lore.add("§7Right-click to unlock this gkit");
        lore.add("§7and claim its items.");
        lore.add("§7");
        lore.add("§eKit: §f" + kit.getName());
        lore.add("§7" + kit.getDescription());
        lore.add("§7");
        lore.add("§6⚠ Single Use Only ⚠");
        meta.setLore(lore);

        PersistentDataContainer container = meta.getPersistentDataContainer();
        container.set(gemKey, PersistentDataType.STRING, kitName.toLowerCase());

        gem.setItemMeta(meta);
        return gem;
    }

    public ItemStack getGem(String kitName) {
        return createGKitGem(kitName);
    }

    /**
     * Check if item is a gkit gem
     */
    public boolean isGKitGem(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;

        ItemMeta meta = item.getItemMeta();
        PersistentDataContainer container = meta.getPersistentDataContainer();

        return container.has(gemKey, PersistentDataType.STRING);
    }

    /**
     * Extract kit name from gem item
     */
    public String extractKitNameFromGem(ItemStack gem) {
        if (!isGKitGem(gem)) return null;

        ItemMeta meta = gem.getItemMeta();
        PersistentDataContainer container = meta.getPersistentDataContainer();

        return container.get(gemKey, PersistentDataType.STRING);
    }

    /**
     * Check if a player has unlocked a specific kit
     */
    public boolean hasUnlockedKit(UUID playerId, String kitName) {
        Set<String> playerKits = unlockedKits.get(playerId);
        return playerKits != null && playerKits.contains(kitName.toLowerCase());
    }

    /**
     * Get all unlocked kits for a player
     */
    public Set<String> getUnlockedKits(UUID playerId) {
        return unlockedKits.getOrDefault(playerId, new HashSet<>());
    }

    public void lockPlayer(UUID playerId) {
        unlockedKits.remove(playerId);
    }

    public void lockAllPlayers() {
        unlockedKits.clear();
    }

    /**
     * Unlock a kit for a player (called when they use a gem or /gkit command)
     * NOTE: This only unlocks the kit, does NOT give items automatically
     */
    public boolean unlockKit(Player player, String kitName) {
        GKit kit = kitManager.getKit(kitName);

        if (kit == null) {
            player.sendMessage("§cKit not found: " + kitName);
            return false;
        }

        boolean isFirstUnlock = !hasUnlockedKit(player.getUniqueId(), kitName);

        if (isFirstUnlock) {
            Set<String> playerKits = unlockedKits.computeIfAbsent(player.getUniqueId(), k -> new HashSet<>());
            playerKits.add(kitName.toLowerCase());

            player.sendMessage("§a§l✓ Kit Unlocked!");
            player.sendMessage("§7You have unlocked the §6" + kit.getDisplayName() + " §7kit!");
            player.sendMessage("§7Use §e/gkits §7to open it or §e/gkit " + kitName + " §7to claim it.");
            player.sendMessage("§7");
            return true;
        } else {
            player.sendMessage("§cYou have already unlocked this kit.");
            player.sendMessage("§7Use §e/gkits §7to claim it or §e/gkit " + kitName + " §7to receive the items.");
            return false;
        }
    }

    /**
     * Give kit items to a player and set cooldown
     * This is called when player claims from GUI or uses /gkit command
     */
    public boolean giveKitItems(Player player, GKit kit) {
        List<ItemStack> randomSet = createRandomDiamondSet(kit);
        maybeAddRaiderExplosives(kit, randomSet);

        int requiredSlots = randomSet.size();
        int emptySlots = 0;
        for (ItemStack slot : player.getInventory().getContents()) {
            if (slot == null || slot.getType() == Material.AIR) {
                emptySlots++;
            }
        }

        if (emptySlots < requiredSlots) {
            player.sendMessage("§cYou need at least §e" + requiredSlots + " empty inventory slots§c to claim this kit!");
            return false;
        }

        for (ItemStack item : randomSet) {
            player.getInventory().addItem(item);
        }

        kitManager.setKitCooldown(player.getUniqueId(), kit.getName());

        player.sendMessage("§a§l✓ Kit Claimed!");
        player.sendMessage("§7You received your §6" + kit.getDisplayName() + " §7gear set!");
        player.sendMessage("§7Next claim available in §e24 hours§7.");

        return true;
    }

    private void maybeAddRaiderExplosives(GKit kit, List<ItemStack> items) {
        if (kit == null || !"raider".equalsIgnoreCase(kit.getName())) {
            return;
        }

        String[] variants = {"LETHAL", "GIGANTIC", "LUCKY"};
        String chosen = variants[random.nextInt(variants.length)];

        // Chance to add one stack (64) of custom TNT variant
        if (random.nextDouble() <= 0.60) {
            items.add(createCustomTntStack(chosen, 64));
        }

        // Always add 8 custom creeper eggs of one random variant
        items.add(createCustomCreeperEggs(chosen, 8));
    }

    private ItemStack createCustomTntStack(String variant, int amount) {
        String color = switch (variant) {
            case "LETHAL" -> "§c";
            case "GIGANTIC" -> "§6";
            case "LUCKY" -> "§e";
            default -> "§f";
        };

        String symbol = switch (variant) {
            case "LETHAL" -> "⚡";
            case "GIGANTIC" -> "✦";
            case "LUCKY" -> "♻";
            default -> "•";
        };

        ItemStack item = new ItemStack(Material.TNT, Math.max(1, amount));
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(color + symbol + " " + variant + " TNT");
            meta.setLore(List.of(
                    "§7Custom raiding explosive",
                    "§7Use with cannons/dispensers",
                    "§8Recognized by SimpleFactionsRaiding"
            ));
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createCustomCreeperEggs(String variant, int amount) {
        String color = switch (variant) {
            case "LETHAL" -> "§c";
            case "GIGANTIC" -> "§6";
            case "LUCKY" -> "§e";
            default -> "§f";
        };

        String symbol = switch (variant) {
            case "LETHAL" -> "⚡";
            case "GIGANTIC" -> "✦";
            case "LUCKY" -> "♻";
            default -> "•";
        };

        ItemStack item = new ItemStack(Material.CREEPER_SPAWN_EGG, Math.max(1, amount));
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(color + symbol + " " + variant + " CREEPER EGG");
            meta.setLore(List.of(
                    "§7Spawns a custom raiding creeper",
                    "§7Right-click to deploy",
                    "§8Recognized by SimpleFactionsRaiding"
            ));
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
            item.setItemMeta(meta);
        }
        return item;
    }

    public List<ItemStack> createPreviewSet(GKit kit) {
        return createRandomDiamondSet(kit);
    }

    private List<ItemStack> createRandomDiamondSet(GKit kit) {
        List<ItemStack> set = new ArrayList<>();
        set.add(createRandomArmorPiece(kit, Material.DIAMOND_HELMET, "Helmet"));
        set.add(createRandomArmorPiece(kit, Material.DIAMOND_CHESTPLATE, "Chestplate"));
        set.add(createRandomArmorPiece(kit, Material.DIAMOND_LEGGINGS, "Leggings"));
        set.add(createRandomArmorPiece(kit, Material.DIAMOND_BOOTS, "Boots"));
        set.add(createSword(kit));
        set.add(createPickaxe(kit));
        return set;
    }

    private ItemStack createSword(GKit kit) {
        ItemStack sword = new ItemStack(Material.DIAMOND_SWORD, 1);
        ItemMeta meta = sword.getItemMeta();
        meta.setDisplayName(kit.getDisplayName() + " §7Sword");
        sword.setItemMeta(meta);

        sword.addUnsafeEnchantment(Enchantment.DAMAGE_ALL, 4 + random.nextInt(2));
        sword.addUnsafeEnchantment(Enchantment.DURABILITY, 2 + random.nextInt(2));

        String kitName = kit.getName().toLowerCase(Locale.ROOT);
        switch (kitName) {
            case "fire" -> {
                sword.addUnsafeEnchantment(Enchantment.FIRE_ASPECT, 1 + random.nextInt(2));
                sword.addUnsafeEnchantment(Enchantment.SWEEPING_EDGE, 2 + random.nextInt(2));
            }
            case "vampire", "royalty" -> {
                sword.addUnsafeEnchantment(Enchantment.LOOT_BONUS_MOBS, 2 + random.nextInt(2));
                sword.addUnsafeEnchantment(Enchantment.FIRE_ASPECT, 1 + random.nextInt(2));
            }
            case "tank", "thunder" -> {
                sword.addUnsafeEnchantment(Enchantment.KNOCKBACK, 1 + random.nextInt(2));
                sword.addUnsafeEnchantment(Enchantment.SWEEPING_EDGE, 2 + random.nextInt(2));
            }
            case "assassin" -> {
                sword.addUnsafeEnchantment(Enchantment.LOOT_BONUS_MOBS, 2 + random.nextInt(2));
                sword.addUnsafeEnchantment(Enchantment.FIRE_ASPECT, 1 + random.nextInt(2));
            }
            case "archer" -> {
                sword.addUnsafeEnchantment(Enchantment.SWEEPING_EDGE, 2 + random.nextInt(2));
            }
            default -> {
                sword.addUnsafeEnchantment(Enchantment.SWEEPING_EDGE, 1 + random.nextInt(2));
            }
        }
        return sword;
    }

    private ItemStack createPickaxe(GKit kit) {
        ItemStack pick = new ItemStack(Material.DIAMOND_PICKAXE, 1);
        ItemMeta meta = pick.getItemMeta();
        meta.setDisplayName(kit.getDisplayName() + " §7Pickaxe");
        pick.setItemMeta(meta);

        pick.addUnsafeEnchantment(Enchantment.DIG_SPEED, 4 + random.nextInt(2));
        pick.addUnsafeEnchantment(Enchantment.DURABILITY, 2 + random.nextInt(2));

        String kitName = kit.getName().toLowerCase(Locale.ROOT);
        if (kitName.equals("miner")) {
            pick.addUnsafeEnchantment(Enchantment.LOOT_BONUS_BLOCKS, 3);
        } else {
            pick.addUnsafeEnchantment(Enchantment.LOOT_BONUS_BLOCKS, 2 + random.nextInt(2));
        }
        return pick;
    }

    private ItemStack createRandomArmorPiece(GKit kit, Material material, String pieceName) {
        ItemStack piece = new ItemStack(material, 1);
        ItemMeta meta = piece.getItemMeta();

        meta.setDisplayName(kit.getDisplayName() + " §7" + pieceName);
        piece.setItemMeta(meta);

        piece.addUnsafeEnchantment(Enchantment.PROTECTION_ENVIRONMENTAL, 4 + random.nextInt(2));
        piece.addUnsafeEnchantment(Enchantment.DURABILITY, 2 + random.nextInt(2));

        List<Enchantment> applied = addKitSpecificBonusEnchants(piece, material, kit.getName().toLowerCase(Locale.ROOT));

        ItemMeta updatedMeta = piece.getItemMeta();
        List<String> lore = getKitCustomEnchantLore(kit.getName().toLowerCase(Locale.ROOT), Math.max(1, applied.size()));

        // Add Overload ONLY to chestplates
        if (material == Material.DIAMOND_CHESTPLATE) {
            String kitName = kit.getName().toLowerCase();
            String overloadLevel = switch (kitName) {
                case "starter" -> "§6Overload I";
                case "fire", "vampire", "assassin" -> "§6Overload II";
                case "tank", "royalty" -> "§6Overload III";
                default -> null;
            };
            
            if (overloadLevel != null) {
                lore.add(overloadLevel);
            }
        }

        if (!lore.isEmpty()) {
            updatedMeta.setLore(lore);
            piece.setItemMeta(updatedMeta);
        }

        return piece;
    }

    private List<Enchantment> addKitSpecificBonusEnchants(ItemStack piece, Material material, String kitName) {
        List<Enchantment> pool = new ArrayList<>();

        switch (kitName) {
            case "starter" -> {
                pool.add(Enchantment.PROTECTION_FIRE);
                pool.add(Enchantment.PROTECTION_PROJECTILE);
                pool.add(Enchantment.PROTECTION_FALL);
                pool.add(Enchantment.OXYGEN);
            }
            case "fire" -> {
                pool.add(Enchantment.PROTECTION_FIRE);
                pool.add(Enchantment.THORNS);
                pool.add(Enchantment.PROTECTION_EXPLOSIONS);
            }
            case "ice" -> {
                pool.add(Enchantment.PROTECTION_PROJECTILE);
                pool.add(Enchantment.PROTECTION_EXPLOSIONS);
                pool.add(Enchantment.FROST_WALKER);
            }
            case "miner" -> {
                pool.add(Enchantment.PROTECTION_EXPLOSIONS);
                pool.add(Enchantment.OXYGEN);
                pool.add(Enchantment.SWIFT_SNEAK);
            }
            case "vampire" -> {
                pool.add(Enchantment.THORNS);
                pool.add(Enchantment.PROTECTION_PROJECTILE);
                pool.add(Enchantment.DEPTH_STRIDER);
            }
            case "thunder" -> {
                pool.add(Enchantment.PROTECTION_PROJECTILE);
                pool.add(Enchantment.PROTECTION_EXPLOSIONS);
                pool.add(Enchantment.DEPTH_STRIDER);
            }
            case "assassin" -> {
                pool.add(Enchantment.PROTECTION_FALL);
                pool.add(Enchantment.SWIFT_SNEAK);
                pool.add(Enchantment.SOUL_SPEED);
            }
            case "tank" -> {
                pool.add(Enchantment.THORNS);
                pool.add(Enchantment.PROTECTION_EXPLOSIONS);
                pool.add(Enchantment.PROTECTION_PROJECTILE);
            }
            case "archer" -> {
                pool.add(Enchantment.PROTECTION_PROJECTILE);
                pool.add(Enchantment.PROTECTION_FALL);
                pool.add(Enchantment.DEPTH_STRIDER);
            }
            case "royalty" -> {
                pool.add(Enchantment.THORNS);
                pool.add(Enchantment.PROTECTION_EXPLOSIONS);
                pool.add(Enchantment.PROTECTION_PROJECTILE);
                pool.add(Enchantment.SWIFT_SNEAK);
                pool.add(Enchantment.SOUL_SPEED);
            }
            default -> {
                pool.add(Enchantment.PROTECTION_FIRE);
                pool.add(Enchantment.PROTECTION_PROJECTILE);
                pool.add(Enchantment.PROTECTION_EXPLOSIONS);
            }
        }

        if (material == Material.DIAMOND_HELMET) {
            pool.add(Enchantment.OXYGEN);
            pool.add(Enchantment.WATER_WORKER);
        } else if (material == Material.DIAMOND_BOOTS) {
            pool.add(Enchantment.PROTECTION_FALL);
            pool.add(Enchantment.DEPTH_STRIDER);
            pool.add(Enchantment.FROST_WALKER);
        }

        Collections.shuffle(pool, random);
        int extras = kitName.equals("starter") ? 1 + random.nextInt(2) : 2 + random.nextInt(2);
        List<Enchantment> applied = new ArrayList<>();

        for (int i = 0; i < Math.min(extras, pool.size()); i++) {
            Enchantment enchantment = pool.get(i);
            int level = getRandomLevel(enchantment);
            piece.addUnsafeEnchantment(enchantment, level);
            applied.add(enchantment);
        }

        return applied;
    }

    private int getRandomLevel(Enchantment enchantment) {
        return switch (enchantment.getKey().getKey()) {
            case "respiration" -> 1 + random.nextInt(3);
            case "aqua_affinity" -> 1;
            case "thorns" -> 1 + random.nextInt(3);
            case "swift_sneak" -> 1 + random.nextInt(3);
            case "feather_falling" -> 2 + random.nextInt(3);
            case "depth_strider" -> 1 + random.nextInt(3);
            case "frost_walker" -> 1 + random.nextInt(2);
            case "soul_speed" -> 1 + random.nextInt(3);
            case "fire_protection" -> 2 + random.nextInt(3);
            case "blast_protection" -> 2 + random.nextInt(3);
            case "projectile_protection" -> 2 + random.nextInt(3);
            default -> 1;
        };
    }

    private List<String> getKitCustomEnchantLore(String kitName, int count) {
        List<String> pool = switch (kitName) {
            case "starter" -> new ArrayList<>(List.of("Armored I", "Enlighted I"));
            case "fire" -> new ArrayList<>(List.of("Armored II", "Valor II", "Enlighted I"));
            case "ice" -> new ArrayList<>(List.of("Armored II", "Enlighted II", "Valor II", "Tank I"));
            case "miner" -> new ArrayList<>(List.of("Armored I", "Enlighted I", "Tank I"));
            case "vampire" -> new ArrayList<>(List.of("Armored II", "Angelic I"));
            case "thunder" -> new ArrayList<>(List.of("Armored II", "Valor II", "Tank II"));
            case "assassin" -> new ArrayList<>(List.of("Armored II", "Angelic II"));
            case "tank" -> new ArrayList<>(List.of("Armored III", "Tank III", "Enlighted II"));
            case "archer" -> new ArrayList<>(List.of("Armored II", "Valor II", "Enlighted II"));
            case "royalty" -> new ArrayList<>(List.of("Armored III", "Tank III", "Enlighted III", "Angelic II"));
            default -> new ArrayList<>(List.of("Armored I", "Enlighted I"));
        };
        
        // Note: Overload is ONLY applied to chestplates in createRandomArmorPiece

        Collections.shuffle(pool, random);
        int lines = Math.min(Math.max(1, count), pool.size());
        List<String> selected = new ArrayList<>(pool.subList(0, lines));
        List<String> colored = new ArrayList<>();
        for (String line : selected) {
            colored.add(colorByEnchantTier(line) + line);
        }
        return colored;
    }

    private String colorByEnchantTier(String enchantLine) {
        String enchantName = enchantLine.split(" ")[0].toLowerCase(Locale.ROOT);
        return switch (enchantName) {
            case "overload", "armored", "enlighted" -> "§6"; // legendary (orange)
            case "tank" -> "§e"; // ultimate (yellow)
            case "angelic" -> "§d"; // heroic (purple)
            case "valor" -> "§e"; // ultimate (yellow)
            default -> "§f";
        };
    }
}
