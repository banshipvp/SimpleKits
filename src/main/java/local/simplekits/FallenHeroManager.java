package local.simplekits;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.NamespacedKey;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;

/**
 * Manages Fallen Hero bones that spawn mobs with a 50% chance of dropping:
 * - GKit Gem for that kit, or
 * - GKit Loot (items) for that kit
 */
public class FallenHeroManager {

    private final JavaPlugin plugin;
    private final NamespacedKey fallenHeroKey;
    private final Random random = new Random();
    private final Map<GKit, EntityType[]> kitMobPools = new HashMap<>();

    public FallenHeroManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.fallenHeroKey = new NamespacedKey(plugin, "fallen_hero_kit");
        initializeMobPools();
    }

    private void initializeMobPools() {
        // Map each kit to possible entity types it can spawn
        // These are just examples - you can customize per kit
        // Note: We don't actually use these in the map, just placeholder initialization
    }

    public ItemStack createFallenHero(GKit kit) {
        ItemStack item = new ItemStack(Material.BONE);
        ItemMeta meta = item.getItemMeta();

        meta.setDisplayName("§c§l☠ Fallen §f" + kit.getDisplayName() + " §c§l☠");
        List<String> lore = new ArrayList<>();
        lore.add("§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
        lore.add("§7Place or right-click to spawn");
        lore.add("§7the §f" + kit.getDisplayName() + " §7fallen hero");
        lore.add("§7");
        lore.add("§6Kill the mob to receive:");
        lore.add("§e• 50% chance: §f" + kit.getDisplayName() + " GKit Gem");
        lore.add("§e• 50% chance: §f" + kit.getDisplayName() + " GKit Loot");
        lore.add("§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
        meta.setLore(lore);

        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        pdc.set(fallenHeroKey, PersistentDataType.STRING, kit.getName());

        item.setItemMeta(meta);
        return item;
    }

    public boolean isFallenHero(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        PersistentDataContainer pdc = item.getItemMeta().getPersistentDataContainer();
        return pdc.has(fallenHeroKey, PersistentDataType.STRING);
    }

    public String getFallenHeroKitName(ItemStack item) {
        if (!isFallenHero(item)) return null;
        return item.getItemMeta().getPersistentDataContainer().get(fallenHeroKey, PersistentDataType.STRING);
    }

    public EntityType getRandomMobForKit(String kitName) {
        // For now, return a generic mob; in a full implementation,
        // map each kit to its boss mob
        EntityType[] common = {
                EntityType.ZOMBIE, EntityType.SKELETON, EntityType.CREEPER,
                EntityType.ENDERMAN, EntityType.BLAZE, EntityType.GHAST
        };
        return common[random.nextInt(common.length)];
    }

    public void spawnFallenHeroMob(Player player, String kitName) {
        EntityType mobType = getRandomMobForKit(kitName);
        org.bukkit.entity.Entity entity = player.getWorld().spawnEntity(player.getLocation(), mobType);
        
        if (entity instanceof LivingEntity living) {
            living.setMetadata("fallen_hero_kit", new org.bukkit.metadata.FixedMetadataValue(plugin, kitName));
            living.setCustomName("§c☠ Fallen §f" + capitalize(kitName) + " §c☠");
            living.setCustomNameVisible(true);
        }
    }

    public void awardKillReward(LivingEntity mob, Player killer) {
        if (!mob.hasMetadata("fallen_hero_kit")) return;

        String kitName = mob.getMetadata("fallen_hero_kit").get(0).asString();
        boolean dropGem = random.nextBoolean();

        if (dropGem) {
            awardGem(killer, kitName);
        } else {
            awardLoot(killer, kitName);
        }
    }

    private void awardGem(Player player, String kitName) {
        if (plugin.getServer().getPluginManager().getPlugin("SimpleKits") instanceof SimpleKitsPlugin kitsPlugin) {
            GKitGemManager gemManager = kitsPlugin.getGKitGemManager();
            ItemStack gem = gemManager.getGem(kitName);
            if (gem != null) {
                HashMap<Integer, ItemStack> left = player.getInventory().addItem(gem);
                if (!left.isEmpty()) {
                    for (ItemStack drop : left.values()) {
                        player.getWorld().dropItemNaturally(player.getLocation(), drop);
                    }
                }
                player.sendMessage("§a[Fallen Hero] §7You received §f" + capitalize(kitName) + " §7GKit Gem!");
            }
        }
    }

    private void awardLoot(Player player, String kitName) {
        if (plugin.getServer().getPluginManager().getPlugin("SimpleKits") instanceof SimpleKitsPlugin kitsPlugin) {
            KitManager kitManager = kitsPlugin.getKitManager();
            GKit kit = kitManager.getKit(kitName);
            if (kit != null) {
                GKitGemManager gemManager = kitsPlugin.getGKitGemManager();
                // Award random armor set from the kit
                java.util.List<org.bukkit.inventory.ItemStack> set = gemManager.createPreviewSet(kit);
                for (org.bukkit.inventory.ItemStack item : set) {
                    if (item == null) continue;
                    HashMap<Integer, ItemStack> left = player.getInventory().addItem(item);
                    if (!left.isEmpty()) {
                        for (ItemStack drop : left.values()) {
                            player.getWorld().dropItemNaturally(player.getLocation(), drop);
                        }
                    }
                }
                player.sendMessage("§a[Fallen Hero] §7You looted §f" + capitalize(kitName) + " §7equipment!");
            }
        }
    }

    private String capitalize(String str) {
        if (str == null || str.isEmpty()) return str;
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }
}
