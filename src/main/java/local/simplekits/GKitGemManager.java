package local.simplekits;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
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
    private final NamespacedKey gkitCustomEnchantsKey;
    private final Random random = new Random();
    private final Map<UUID, Set<String>> unlockedKits = new HashMap<>();

    public GKitGemManager(SimpleKitsPlugin plugin, KitManager kitManager) {
        this.plugin = plugin;
        this.kitManager = kitManager;
        this.gemKey = new NamespacedKey(plugin, "gkit_gem");
        this.gkitCustomEnchantsKey = new NamespacedKey(plugin, "gkit_custom_enchants");
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
     * Admin unlock — directly grants a kit without requiring a live Player reference.
     * Use from command handlers; does NOT send any chat messages.
     *
     * @return true  if the kit was newly unlocked
     *         false if it was already unlocked
     */
    public boolean unlockKitSilent(UUID playerId, String kitName) {
        if (kitManager.getKit(kitName) == null) return false;
        Set<String> playerKits = unlockedKits.computeIfAbsent(playerId, k -> new HashSet<>());
        return playerKits.add(kitName.toLowerCase());
    }

    /**
     * Admin unlock — grants all registered kits to a player without messages.
     *
     * @return number of kits newly unlocked
     */
    public int unlockAllKitsSilent(UUID playerId) {
        Set<String> playerKits = unlockedKits.computeIfAbsent(playerId, k -> new HashSet<>());
        int count = 0;
        for (GKit kit : kitManager.getAllKits()) {
            if (playerKits.add(kit.getName().toLowerCase())) count++;
        }
        return count;
    }

    /**
     * Unlock a kit for a player (called when they use a gem)
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
            player.sendMessage("§7Use §e/gkits §7to open the GUI and claim your kit.");
            player.sendMessage("§7");
            return true;
        } else {
            player.sendMessage("§eYou have already unlocked this kit.");
            player.sendMessage("§7Use §e/gkits §7to claim it if available.");
            return false;
        }
    }

    /**
     * Give kit items to a player and set cooldown (called from GUI)
     */
    public boolean giveKitItems(Player player, GKit kit) {
        List<ItemStack> randomSet = createRandomDiamondSet(kit);

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

    private List<ItemStack> createRandomDiamondSet(GKit kit) {
        List<ItemStack> generated = new ArrayList<>();
        for (ItemStack item : kit.getItemsCopy()) {
            generated.add(applyRandomizedCustomEnchants(item));
        }
        return generated;
    }

    private ItemStack applyRandomizedCustomEnchants(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return item;
        ItemMeta meta = item.getItemMeta();
        String csv = meta.getPersistentDataContainer().get(gkitCustomEnchantsKey, PersistentDataType.STRING);
        if (csv == null || csv.isBlank()) return item;

        var fe = plugin.getServer().getPluginManager().getPlugin("FactionEnchants");
        if (fe == null) return item;

        try {
            var enchantManager = fe.getClass().getMethod("getEnchantmentManager").invoke(fe);
            var getEnchantment = enchantManager.getClass().getMethod("getEnchantment", String.class);
            java.lang.reflect.Method applyEnchantment = null;
            for (java.lang.reflect.Method method : enchantManager.getClass().getMethods()) {
                if (method.getName().equals("applyEnchantment") && method.getParameterCount() == 3) {
                    applyEnchantment = method;
                    break;
                }
            }
            if (applyEnchantment == null) return item;

            for (String id : csv.split(",")) {
                String cleaned = id.trim();
                if (cleaned.isEmpty()) continue;
                Object enchant = getEnchantment.invoke(enchantManager, cleaned);
                if (enchant == null) continue;

                int maxLevel = (int) enchant.getClass().getMethod("getMaxLevel").invoke(enchant);
                int rolled = 1 + random.nextInt(Math.max(1, maxLevel));
                item = (ItemStack) applyEnchantment.invoke(enchantManager, item, enchant, rolled);
            }
        } catch (Exception ex) {
            plugin.getLogger().warning("Failed to apply randomized custom gkit enchants: " + ex.getMessage());
        }

        return item;
    }

    private ItemStack createRandomArmorPiece(GKit kit, Material material, String pieceName) {
        ItemStack piece = new ItemStack(material, 1);
        ItemMeta meta = piece.getItemMeta();

        meta.setDisplayName(kit.getDisplayName() + " §7" + pieceName);
        piece.setItemMeta(meta);

        piece.addUnsafeEnchantment(Enchantment.PROTECTION_ENVIRONMENTAL, 5);
        piece.addUnsafeEnchantment(Enchantment.DURABILITY, 3);

        List<Enchantment> applied = addKitSpecificBonusEnchants(piece, material, kit.getName().toLowerCase(Locale.ROOT));

        ItemMeta updatedMeta = piece.getItemMeta();
        List<String> lore = getKitCustomEnchantLore(kit.getName().toLowerCase(Locale.ROOT), Math.max(1, applied.size()));

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
            case "starter" -> new ArrayList<>(List.of("Armored I", "Overload I", "Enlighted I"));
            case "fire" -> new ArrayList<>(List.of("Armored II", "Overload II", "Valor II", "Enlighted I"));
            case "ice" -> new ArrayList<>(List.of("Armored II", "Enlighted II", "Valor II", "Tank I"));
            case "miner" -> new ArrayList<>(List.of("Armored I", "Enlighted I", "Tank I"));
            case "vampire" -> new ArrayList<>(List.of("Armored II", "Overload II", "Angelic I"));
            case "thunder" -> new ArrayList<>(List.of("Armored II", "Valor II", "Tank II"));
            case "assassin" -> new ArrayList<>(List.of("Armored II", "Overload II", "Angelic II"));
            case "tank" -> new ArrayList<>(List.of("Armored III", "Tank III", "Enlighted II", "Overload II"));
            case "archer" -> new ArrayList<>(List.of("Armored II", "Valor II", "Enlighted II"));
            case "royalty" -> new ArrayList<>(List.of("Armored III", "Overload III", "Tank III", "Enlighted III", "Angelic II"));
            default -> new ArrayList<>(List.of("Armored I", "Overload I", "Enlighted I"));
        };

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
            case "overload" -> "§6"; // legendary (orange)
            case "tank" -> "§d"; // ultimate+
            case "angelic" -> "§5"; // ultimate
            case "armored", "valor" -> "§b"; // elite
            case "enlighted" -> "§a"; // unique
            default -> "§f";
        };
    }
}
