package local.simplekits;

import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.*;
import java.util.concurrent.TimeUnit;

public class RankKitManager {

    private final Map<String, RankKit> kits = new LinkedHashMap<>();
    private final Map<String, Long> cooldowns = new HashMap<>();
    private final long defaultCooldownMs = TimeUnit.HOURS.toMillis(24);

    public void loadDefaultKits() {
        kits.clear();

        RankKit starter = new RankKit("starter", "§f§lStarter Kit", "§7Base building + raid starter kit", RankTier.DEFAULT, 24);
        starter.addItem(createEnchantedItem(Material.IRON_SWORD, 1, "§fStarter Sword", Enchantment.DAMAGE_ALL, 2, Enchantment.DURABILITY, 2));
        starter.addItem(createEnchantedItem(Material.IRON_HELMET, 1, "§fStarter Helmet", Enchantment.PROTECTION_ENVIRONMENTAL, 2, Enchantment.DURABILITY, 2));
        starter.addItem(createEnchantedItem(Material.IRON_CHESTPLATE, 1, "§fStarter Chestplate", Enchantment.PROTECTION_ENVIRONMENTAL, 2, Enchantment.DURABILITY, 2));
        starter.addItem(createEnchantedItem(Material.IRON_LEGGINGS, 1, "§fStarter Leggings", Enchantment.PROTECTION_ENVIRONMENTAL, 2, Enchantment.DURABILITY, 2));
        starter.addItem(createEnchantedItem(Material.IRON_BOOTS, 1, "§fStarter Boots", Enchantment.PROTECTION_ENVIRONMENTAL, 2, Enchantment.DURABILITY, 2));
        starter.addItem(createCrateItem("simple", "§7§lSimple Crate"));
        starter.addItem(new ItemStack(Material.OAK_LOG, 64));
        starter.addItem(new ItemStack(Material.SAND, 64));
        starter.addItem(new ItemStack(Material.OBSIDIAN, 32));
        starter.addItem(new ItemStack(Material.TNT, 32));
        starter.addItem(new ItemStack(Material.COOKED_BEEF, 32));
        kits.put(starter.getName(), starter);

        RankKit scout = new RankKit("scout", "§a§lScout Kit", "§7Starter rank kit", RankTier.SCOUT, 24);
        scout.addItem(createEnchantedItem(Material.IRON_SWORD, 1, "§aScout Sword", Enchantment.DAMAGE_ALL, 2, Enchantment.DURABILITY, 2));
        scout.addItem(createEnchantedItem(Material.IRON_HELMET, 1, "§aScout Helmet", Enchantment.PROTECTION_ENVIRONMENTAL, 2, Enchantment.DURABILITY, 2));
        scout.addItem(createEnchantedItem(Material.IRON_CHESTPLATE, 1, "§aScout Chestplate", Enchantment.PROTECTION_ENVIRONMENTAL, 2, Enchantment.DURABILITY, 2));
        scout.addItem(createEnchantedItem(Material.IRON_LEGGINGS, 1, "§aScout Leggings", Enchantment.PROTECTION_ENVIRONMENTAL, 2, Enchantment.DURABILITY, 2));
        scout.addItem(createEnchantedItem(Material.IRON_BOOTS, 1, "§aScout Boots", Enchantment.PROTECTION_ENVIRONMENTAL, 2, Enchantment.DURABILITY, 2));
        scout.addItem(createCrateItem("unique", "§a§lUnique Crate"));
        scout.addItem(new ItemStack(Material.BOW));
        scout.addItem(new ItemStack(Material.ARROW, 64));
        scout.addItem(new ItemStack(Material.COOKED_BEEF, 32));
        kits.put(scout.getName(), scout);

        RankKit militant = new RankKit("militant", "§e§lMilitant Kit", "§7Mid-tier PvP kit", RankTier.MILITANT, 24);
        militant.addItem(createEnchantedItem(Material.DIAMOND_SWORD, 1, "§eMilitant Sword", Enchantment.DAMAGE_ALL, 3, Enchantment.DURABILITY, 3));
        militant.addItem(createEnchantedItem(Material.DIAMOND_HELMET, 1, "§eMilitant Helmet", Enchantment.PROTECTION_ENVIRONMENTAL, 3, Enchantment.DURABILITY, 3));
        militant.addItem(createEnchantedItem(Material.DIAMOND_CHESTPLATE, 1, "§eMilitant Chestplate", Enchantment.PROTECTION_ENVIRONMENTAL, 3, Enchantment.DURABILITY, 3));
        militant.addItem(createEnchantedItem(Material.DIAMOND_LEGGINGS, 1, "§eMilitant Leggings", Enchantment.PROTECTION_ENVIRONMENTAL, 3, Enchantment.DURABILITY, 3));
        militant.addItem(createEnchantedItem(Material.DIAMOND_BOOTS, 1, "§eMilitant Boots", Enchantment.PROTECTION_ENVIRONMENTAL, 3, Enchantment.DURABILITY, 3));
        militant.addItem(createCrateItem("elite", "§b§lElite Crate"));
        militant.addItem(createEnchantedItem(Material.BOW, 1, "§eMilitant Bow", Enchantment.ARROW_DAMAGE, 3, Enchantment.DURABILITY, 3));
        militant.addItem(new ItemStack(Material.ARROW, 64));
        militant.addItem(new ItemStack(Material.GOLDEN_APPLE, 8));
        kits.put(militant.getName(), militant);

        RankKit tactician = new RankKit("tactician", "§6§lTactician Kit", "§7Advanced combat kit", RankTier.TACTICIAN, 24);
        tactician.addItem(createEnchantedItem(Material.DIAMOND_SWORD, 1, "§6Tactician Sword", Enchantment.DAMAGE_ALL, 4, Enchantment.DURABILITY, 3));
        tactician.addItem(createEnchantedItem(Material.DIAMOND_HELMET, 1, "§6Tactician Helmet", Enchantment.PROTECTION_ENVIRONMENTAL, 4, Enchantment.DURABILITY, 3));
        tactician.addItem(createEnchantedItem(Material.DIAMOND_CHESTPLATE, 1, "§6Tactician Chestplate", Enchantment.PROTECTION_ENVIRONMENTAL, 4, Enchantment.DURABILITY, 3));
        tactician.addItem(createEnchantedItem(Material.DIAMOND_LEGGINGS, 1, "§6Tactician Leggings", Enchantment.PROTECTION_ENVIRONMENTAL, 4, Enchantment.DURABILITY, 3));
        tactician.addItem(createEnchantedItem(Material.DIAMOND_BOOTS, 1, "§6Tactician Boots", Enchantment.PROTECTION_ENVIRONMENTAL, 4, Enchantment.DURABILITY, 3));
        tactician.addItem(createCrateItem("ultimate", "§5§lUltimate Crate"));
        tactician.addItem(createEnchantedItem(Material.BOW, 1, "§6Tactician Bow", Enchantment.ARROW_DAMAGE, 4, Enchantment.DURABILITY, 3));
        tactician.addItem(new ItemStack(Material.ARROW, 64));
        tactician.addItem(new ItemStack(Material.GOLDEN_APPLE, 12));
        kits.put(tactician.getName(), tactician);

        RankKit warlord = new RankKit("warlord", "§5§lWarlord Kit", "§7High-tier rank kit", RankTier.WARLORD, 24);
        warlord.addItem(createEnchantedItem(Material.NETHERITE_SWORD, 1, "§5Warlord Blade", Enchantment.DAMAGE_ALL, 5, Enchantment.DURABILITY, 3));
        warlord.addItem(createEnchantedItem(Material.NETHERITE_HELMET, 1, "§5Warlord Helmet", Enchantment.PROTECTION_ENVIRONMENTAL, 5, Enchantment.DURABILITY, 3));
        warlord.addItem(createEnchantedItem(Material.NETHERITE_CHESTPLATE, 1, "§5Warlord Chestplate", Enchantment.PROTECTION_ENVIRONMENTAL, 5, Enchantment.DURABILITY, 3));
        warlord.addItem(createEnchantedItem(Material.NETHERITE_LEGGINGS, 1, "§5Warlord Leggings", Enchantment.PROTECTION_ENVIRONMENTAL, 5, Enchantment.DURABILITY, 3));
        warlord.addItem(createEnchantedItem(Material.NETHERITE_BOOTS, 1, "§5Warlord Boots", Enchantment.PROTECTION_ENVIRONMENTAL, 5, Enchantment.DURABILITY, 3));
        warlord.addItem(createCrateItem("legendary", "§6§lLegendary Crate"));
        warlord.addItem(createEnchantedItem(Material.BOW, 1, "§5Warlord Bow", Enchantment.ARROW_DAMAGE, 5, Enchantment.DURABILITY, 3));
        warlord.addItem(new ItemStack(Material.ARROW, 64));
        warlord.addItem(new ItemStack(Material.ENCHANTED_GOLDEN_APPLE, 2));
        kits.put(warlord.getName(), warlord);

        RankKit sovereign = new RankKit("sovereign", "§c§lSovereign Kit", "§7Top rank kit", RankTier.SOVEREIGN, 24);
        sovereign.addItem(createEnchantedItem(Material.NETHERITE_SWORD, 1, "§cSovereign Blade", Enchantment.DAMAGE_ALL, 6, Enchantment.DURABILITY, 4));
        sovereign.addItem(createEnchantedItem(Material.NETHERITE_HELMET, 1, "§cSovereign Helmet", Enchantment.PROTECTION_ENVIRONMENTAL, 6, Enchantment.DURABILITY, 4));
        sovereign.addItem(createEnchantedItem(Material.NETHERITE_CHESTPLATE, 1, "§cSovereign Chestplate", Enchantment.PROTECTION_ENVIRONMENTAL, 6, Enchantment.DURABILITY, 4));
        sovereign.addItem(createEnchantedItem(Material.NETHERITE_LEGGINGS, 1, "§cSovereign Leggings", Enchantment.PROTECTION_ENVIRONMENTAL, 6, Enchantment.DURABILITY, 4));
        sovereign.addItem(createEnchantedItem(Material.NETHERITE_BOOTS, 1, "§cSovereign Boots", Enchantment.PROTECTION_ENVIRONMENTAL, 6, Enchantment.DURABILITY, 4));
        sovereign.addItem(createCrateItem("godly", "§d§lGodly Crate"));
        sovereign.addItem(createEnchantedItem(Material.BOW, 1, "§cSovereign Bow", Enchantment.ARROW_DAMAGE, 6, Enchantment.DURABILITY, 4));
        sovereign.addItem(new ItemStack(Material.ARROW, 64));
        sovereign.addItem(new ItemStack(Material.ENCHANTED_GOLDEN_APPLE, 4));
        sovereign.addItem(new ItemStack(Material.TOTEM_OF_UNDYING, 1));
        kits.put(sovereign.getName(), sovereign);
    }

    private ItemStack createNamedItem(Material material, int amount, String name) {
        ItemStack item = new ItemStack(material, amount);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createCrateItem(String tierId, String displayName) {
        ItemStack crate = new ItemStack(Material.CHEST, 1);
        ItemMeta meta = crate.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(displayName);
            List<String> lore = new ArrayList<>();
            lore.add("§7Right-click to open this crate");
            lore.add("§7Left-click to preview rewards");
            lore.add("§7");
            lore.add("§eTier: §f" + tierId);
            lore.add("§7Higher tiers = better rewards");
            lore.add("§7");
            lore.add("§6✦ Single Use ✦");
            meta.setLore(lore);
            meta.getPersistentDataContainer().set(new NamespacedKey("simplecrates", "crate_tier"), PersistentDataType.STRING, tierId);
            crate.setItemMeta(meta);
        }
        return crate;
    }

    private ItemStack createEnchantedItem(
            Material material,
            int amount,
            String name,
            Enchantment firstEnchant,
            int firstLevel,
            Enchantment secondEnchant,
            int secondLevel
    ) {
        ItemStack item = new ItemStack(material, amount);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            item.setItemMeta(meta);
        }
        item.addUnsafeEnchantment(firstEnchant, firstLevel);
        item.addUnsafeEnchantment(secondEnchant, secondLevel);
        return item;
    }

    public Collection<RankKit> getAllKits() {
        return kits.values();
    }

    public RankKit getKit(String name) {
        return kits.get(name.toLowerCase());
    }

    public void registerKit(RankKit kit) {
        kits.put(kit.getName().toLowerCase(), kit);
    }

    public boolean unregisterKit(String name) {
        return kits.remove(name.toLowerCase()) != null;
    }

    public RankTier getPlayerRank(Player player) {
        try {
            LuckPerms api = LuckPermsProvider.get();
            var user = api.getUserManager().getUser(player.getUniqueId());
            if (user != null) {
                return RankTier.fromGroup(user.getPrimaryGroup());
            }
        } catch (Throwable ignored) {
        }

        if (player.hasPermission("group.sovereign") || player.hasPermission("simplekits.rank.sovereign")) return RankTier.SOVEREIGN;
        if (player.hasPermission("group.warlord") || player.hasPermission("simplekits.rank.warlord")) return RankTier.WARLORD;
        if (player.hasPermission("group.tactician") || player.hasPermission("simplekits.rank.tactician")) return RankTier.TACTICIAN;
        if (player.hasPermission("group.militant") || player.hasPermission("simplekits.rank.militant")) return RankTier.MILITANT;
        if (player.hasPermission("group.scout") || player.hasPermission("simplekits.rank.scout")) return RankTier.SCOUT;
        return RankTier.DEFAULT;
    }

    public boolean hasAccess(Player player, RankKit kit) {
        RankTier playerRank = getPlayerRank(player);
        return playerRank.getLevel() >= kit.getRequiredRank().getLevel();
    }

    public boolean canClaim(Player player, RankKit kit) {
        if (!hasAccess(player, kit)) return false;
        String key = cooldownKey(player, kit);
        Long end = cooldowns.get(key);
        if (end == null) return true;
        if (System.currentTimeMillis() > end) {
            cooldowns.remove(key);
            return true;
        }
        return false;
    }

    public int getRemainingHours(Player player, RankKit kit) {
        String key = cooldownKey(player, kit);
        Long end = cooldowns.get(key);
        if (end == null || System.currentTimeMillis() > end) {
            cooldowns.remove(key);
            return 0;
        }
        return (int) ((end - System.currentTimeMillis()) / 3_600_000L);
    }

    public boolean claim(Player player, RankKit kit) {
        if (!canClaim(player, kit)) return false;

        for (ItemStack item : kit.getItemCopies()) {
            player.getInventory().addItem(item);
        }

        cooldowns.put(cooldownKey(player, kit), System.currentTimeMillis() + defaultCooldownMs);
        return true;
    }

    private String cooldownKey(Player player, RankKit kit) {
        return player.getUniqueId() + ":" + kit.getName();
    }
}
