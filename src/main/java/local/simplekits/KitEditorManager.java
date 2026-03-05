package local.simplekits;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.*;

public class KitEditorManager {

    public enum CreationType { KIT, GKIT }

    enum MenuState { MAIN, GEAR, TOOLS, CRATES, XP, MISC, ENCHANTS, PREVIEW }

    private static final String TITLE_MAIN_PREFIX    = "§8Builder: ";
    private static final String TITLE_GEAR_PREFIX     = "§8Gear: ";
    private static final String TITLE_TOOLS_PREFIX    = "§8Tools: ";
    private static final String TITLE_CRATES_PREFIX   = "§8Crates: ";
    private static final String TITLE_XP_PREFIX       = "§8XP: ";
    private static final String TITLE_MISC_PREFIX     = "§8Misc: ";
    private static final String TITLE_ENCHANTS_PREFIX = "§8Enchants: ";
    private static final String TITLE_PREVIEW_PREFIX  = "§8Preview: ";

    // Armour sets ordered best → worst; each row in the editor shows one full set.
    private static final Material[][] GEAR_SETS = {
        { Material.NETHERITE_HELMET, Material.NETHERITE_CHESTPLATE, Material.NETHERITE_LEGGINGS, Material.NETHERITE_BOOTS },
        { Material.DIAMOND_HELMET,   Material.DIAMOND_CHESTPLATE,   Material.DIAMOND_LEGGINGS,   Material.DIAMOND_BOOTS   },
        { Material.IRON_HELMET,      Material.IRON_CHESTPLATE,      Material.IRON_LEGGINGS,      Material.IRON_BOOTS      },
        { Material.CHAINMAIL_HELMET, Material.CHAINMAIL_CHESTPLATE, Material.CHAINMAIL_LEGGINGS, Material.CHAINMAIL_BOOTS },
        { Material.LEATHER_HELMET,   Material.LEATHER_CHESTPLATE,   Material.LEATHER_LEGGINGS,   Material.LEATHER_BOOTS   },
    };
    // First slot in each row for the above sets (4 items per set, gap in slot+4)
    private static final int[] GEAR_ROW_START = { 0, 9, 18, 27, 36 };

    private static final List<Material> TOOL_OPTIONS = List.of(
            Material.WOODEN_SWORD, Material.STONE_SWORD, Material.IRON_SWORD, Material.DIAMOND_SWORD, Material.NETHERITE_SWORD,
            Material.WOODEN_AXE, Material.STONE_AXE, Material.IRON_AXE, Material.DIAMOND_AXE, Material.NETHERITE_AXE,
            Material.IRON_PICKAXE, Material.DIAMOND_PICKAXE, Material.NETHERITE_PICKAXE,
            Material.BOW, Material.CROSSBOW, Material.TRIDENT, Material.SHIELD
    );

    private final SimpleKitsPlugin plugin;
    private final RankKitManager rankKitManager;
    private final KitManager kitManager;

    private final Map<UUID, Session> sessions = new HashMap<>();
    private final Map<UUID, CreationType> pendingNamePrompt = new HashMap<>();

    public KitEditorManager(SimpleKitsPlugin plugin, RankKitManager rankKitManager, KitManager kitManager) {
        this.plugin = plugin;
        this.rankKitManager = rankKitManager;
        this.kitManager = kitManager;
    }

    public void promptForName(Player player, CreationType type) {
        pendingNamePrompt.put(player.getUniqueId(), type);
        player.sendMessage("§eType the " + (type == CreationType.GKIT ? "gkit" : "kit") + " name in chat.");
        player.sendMessage("§7Color codes are supported, e.g. §f&3Starter");
    }

    public boolean isAwaitingName(UUID playerId) {
        return pendingNamePrompt.containsKey(playerId);
    }

    public void handleChatName(Player player, String rawName) {
        CreationType type = pendingNamePrompt.remove(player.getUniqueId());
        if (type == null) return;
        Bukkit.getScheduler().runTask(plugin, () -> startEditor(player, type, rawName));
    }

    public void startEditor(Player player, CreationType type, String rawName) {
        String displayName = color(rawName == null ? "" : rawName.trim());
        String plain = strip(displayName);
        if (plain.isEmpty()) {
            player.sendMessage("§cName cannot be empty.");
            return;
        }
        String keyName = plain.toLowerCase(Locale.ROOT).replace(" ", "_");

        if (type == CreationType.KIT && rankKitManager.getKit(keyName) != null) {
            player.sendMessage("§cA kit with that name already exists.");
            return;
        }
        if (type == CreationType.GKIT && kitManager.getKit(keyName) != null) {
            player.sendMessage("§cA gkit with that name already exists.");
            return;
        }

        Session session = new Session(type, keyName, displayName);
        sessions.put(player.getUniqueId(), session);
        openMainMenu(player, session);
    }

    public boolean handleInventoryClick(Player player, Inventory inventory, String title, int slot, org.bukkit.event.inventory.ClickType clickType, ItemStack clicked) {
        Session session = sessions.get(player.getUniqueId());
        if (session == null) return false;

        if (title.startsWith(TITLE_MAIN_PREFIX)) {
            handleMainClick(player, session, slot);
            return true;
        }
        if (title.startsWith(TITLE_GEAR_PREFIX)) {
            handleSelectionClick(player, session, MenuState.GEAR, slot, clickType, clicked);
            return true;
        }
        if (title.startsWith(TITLE_TOOLS_PREFIX)) {
            handleSelectionClick(player, session, MenuState.TOOLS, slot, clickType, clicked);
            return true;
        }
        if (title.startsWith(TITLE_CRATES_PREFIX)) {
            handleCratesClick(player, session, slot, clickType, clicked);
            return true;
        }
        if (title.startsWith(TITLE_XP_PREFIX)) {
            handleXpClick(player, session, slot);
            return true;
        }
        if (title.startsWith(TITLE_MISC_PREFIX)) {
            handleMiscClick(player, session, slot, clickType, clicked);
            return true;
        }
        if (title.startsWith(TITLE_ENCHANTS_PREFIX)) {
            handleEnchantClick(player, session, slot, clickType, clicked);
            return true;
        }
        if (title.startsWith(TITLE_PREVIEW_PREFIX)) {
            if (slot == 45) openMainMenu(player, session);
            return true;
        }
        return false;
    }

    private void openMainMenu(Player player, Session session) {
        Inventory inv = Bukkit.createInventory(null, 36, TITLE_MAIN_PREFIX + session.displayName);

        // Row 1 — editor sections
        inv.setItem(10, named(Material.DIAMOND_CHESTPLATE, "§bGear", List.of("§7Configure armor pieces")));
        inv.setItem(11, named(Material.DIAMOND_SWORD, "§bTools / Weapons", List.of("§7Configure weapons and tools")));
        inv.setItem(12, named(Material.CHEST, "§bCrates", List.of("§7Use exact crates from SimpleCrates")));
        inv.setItem(13, named(Material.EXPERIENCE_BOTTLE, "§bXP", List.of("§7Set custom XP value for 1 bottle")));
        if (session.type == CreationType.GKIT) {
            inv.setItem(14, named(Material.BOOK, "§bMiscellaneous", List.of("§7Black scrolls & unopened custom books")));
        }
        inv.setItem(15, named(Material.EMERALD_BLOCK, "§aConfirm & Create", List.of(
                "§7Name: §f" + session.displayName,
                "§7Selected Items: §f" + session.selectedItems.size(),
                "§7XP Bottle Value: §f" + session.xpBottleValue
        )));
        inv.setItem(16, named(Material.BARRIER, "§cCancel", List.of("§7Cancel this creation session")));

        // Row 2 — preview
        inv.setItem(22, named(Material.ENDER_EYE, "§ePreview Kit", List.of(
                "§7View all selected items with",
                "§7their enchants applied."
        )));

        fill(inv, Material.GRAY_STAINED_GLASS_PANE);
        player.openInventory(inv);
        session.menuState = MenuState.MAIN;
    }

    private void openGearMenu(Player player, Session session) {
        Inventory inv = Bukkit.createInventory(null, 54, TITLE_GEAR_PREFIX + session.displayName);
        // Place each armour set on its own row: Helmet | Chestplate | Leggings | Boots
        for (int s = 0; s < GEAR_SETS.length; s++) {
            int start = GEAR_ROW_START[s];
            for (int i = 0; i < GEAR_SETS[s].length; i++) {
                inv.setItem(start + i, decoratedSelectable(session, GEAR_SETS[s][i]));
            }
        }
        addBackDone(inv);
        player.openInventory(inv);
        session.menuState = MenuState.GEAR;
    }

    private void openToolsMenu(Player player, Session session) {
        Inventory inv = Bukkit.createInventory(null, 54, TITLE_TOOLS_PREFIX + session.displayName);
        int slot = 0;
        for (Material material : TOOL_OPTIONS) {
            inv.setItem(slot++, decoratedSelectable(session, material));
        }
        addBackDone(inv);
        player.openInventory(inv);
        session.menuState = MenuState.TOOLS;
    }

    private void openCratesMenu(Player player, Session session) {
        Inventory inv = Bukkit.createInventory(null, 27, TITLE_CRATES_PREFIX + session.displayName);

        int slot = 10;
        for (String tier : List.of("simple", "unique", "elite", "ultimate", "legendary", "godly")) {
            inv.setItem(slot++, crateItemFromSimpleCrates(tier, session));
        }

        inv.setItem(18, named(Material.ARROW, "§eBack", List.of("§7Return to main menu")));
        inv.setItem(26, named(Material.EMERALD, "§aDone", List.of("§7Return to main menu")));
        fill(inv, Material.GRAY_STAINED_GLASS_PANE);
        player.openInventory(inv);
        session.menuState = MenuState.CRATES;
    }

    private void openXpMenu(Player player, Session session) {
        Inventory inv = Bukkit.createInventory(null, 27, TITLE_XP_PREFIX + session.displayName);
        inv.setItem(10, named(Material.LIME_DYE, "§a+500", List.of("§7Add 500 XP value")));
        inv.setItem(11, named(Material.LIME_DYE, "§a+1000", List.of("§7Add 1000 XP value")));
        inv.setItem(14, named(Material.RED_DYE, "§c-500", List.of("§7Subtract 500 XP value")));
        inv.setItem(15, named(Material.RED_DYE, "§c-1000", List.of("§7Subtract 1000 XP value")));
        inv.setItem(13, named(Material.EXPERIENCE_BOTTLE, "§eCurrent Bottle Value: §f" + session.xpBottleValue, List.of("§7Final kit gives 1 custom XP bottle")));
        inv.setItem(18, named(Material.ARROW, "§eBack", List.of("§7Return to main menu")));
        inv.setItem(26, named(Material.EMERALD, "§aDone", List.of("§7Return to main menu")));
        fill(inv, Material.GRAY_STAINED_GLASS_PANE);
        player.openInventory(inv);
        session.menuState = MenuState.XP;
    }

    private void openMiscMenu(Player player, Session session) {
        Inventory inv = Bukkit.createInventory(null, 27, TITLE_MISC_PREFIX + session.displayName);

        inv.setItem(10, miscSelectable(session, "BLACK_SCROLL", createBlackScrollItem()));
        inv.setItem(11, miscSelectable(session, "BOOK_UNIQUE", createRandomEnchantBook("UNIQUE")));
        inv.setItem(12, miscSelectable(session, "BOOK_ELITE", createRandomEnchantBook("ELITE")));
        inv.setItem(13, miscSelectable(session, "BOOK_ULTIMATE", createRandomEnchantBook("ULTIMATE")));
        inv.setItem(14, miscSelectable(session, "BOOK_LEGENDARY", createRandomEnchantBook("LEGENDARY")));
        inv.setItem(15, miscSelectable(session, "BOOK_SOUL", createRandomEnchantBook("SOUL")));

        inv.setItem(18, named(Material.ARROW, "§eBack", List.of("§7Return to main menu")));
        inv.setItem(26, named(Material.EMERALD, "§aDone", List.of("§7Return to main menu")));
        fill(inv, Material.GRAY_STAINED_GLASS_PANE);
        player.openInventory(inv);
        session.menuState = MenuState.MISC;
    }

    private void openEnchantMenu(Player player, Session session, Material targetMaterial, MenuState returnMenu) {
        session.enchantTarget = targetMaterial;
        session.returnFromEnchant = returnMenu;

        Inventory inv = Bukkit.createInventory(null, 54, TITLE_ENCHANTS_PREFIX + readable(targetMaterial));
        int slot = 0;

        if (session.type == CreationType.KIT) {
            for (EnchantOption option : buildVanillaOptions(targetMaterial)) {
                if (slot >= 45) break;
                inv.setItem(slot++, enchantOptionItem(option, session));
            }
        } else {
            for (CustomEnchantOption option : getCustomOptions(targetMaterial)) {
                if (slot >= 45) break;
                inv.setItem(slot++, customEnchantOptionItem(option, session));
            }
        }

        inv.setItem(45, named(Material.ARROW, "§eBack", List.of("§7Return to previous page")));
        inv.setItem(53, named(Material.EMERALD, "§aConfirm", List.of("§7Save selections and go back")));
        fillMainAreaEmpty(inv, Material.BLACK_STAINED_GLASS_PANE);
        player.openInventory(inv);
        session.menuState = MenuState.ENCHANTS;
    }

    private void handleMainClick(Player player, Session session, int slot) {
        switch (slot) {
            case 10 -> openGearMenu(player, session);
            case 11 -> openToolsMenu(player, session);
            case 12 -> openCratesMenu(player, session);
            case 13 -> openXpMenu(player, session);
            case 14 -> {
                if (session.type == CreationType.GKIT) openMiscMenu(player, session);
            }
            case 15 -> finalizeCreation(player, session);
            case 16 -> {
                sessions.remove(player.getUniqueId());
                player.closeInventory();
                player.sendMessage("§cCreation cancelled.");
            }
            case 22 -> openPreviewMenu(player, session);
            default -> {
            }
        }
    }

    private void handleSelectionClick(Player player, Session session, MenuState sourceMenu, int slot, org.bukkit.event.inventory.ClickType clickType, ItemStack clicked) {
        if (slot == 45 || slot == 53) {
            openMainMenu(player, session);
            return;
        }
        if (clicked == null) return;

        Material material = clicked.getType();
        if (material == Material.AIR || material.name().contains("GLASS") || material == Material.ARROW || material == Material.EMERALD || material == Material.BARRIER) return;

        if (clickType.isLeftClick()) {
            if (isArmor(material)) {
                clearExistingArmorSlot(session, material);
            }
            session.selectedItems.put(material.name(), baseItem(material, session.type == CreationType.GKIT));
            player.sendMessage("§aSelected §f" + readable(material));
            reopenCurrentSelectionMenu(player, session, sourceMenu);
            return;
        }

        if (clickType == org.bukkit.event.inventory.ClickType.MIDDLE
                || clickType == org.bukkit.event.inventory.ClickType.SHIFT_RIGHT) {
            session.selectedItems.putIfAbsent(material.name(), baseItem(material, session.type == CreationType.GKIT));
            openEnchantMenu(player, session, material, sourceMenu);
            return;
        }

        if (clickType.isRightClick()) {
            session.selectedItems.remove(material.name());
            session.vanillaEnchantSelections.remove(material.name());
            session.customEnchantSelections.remove(material.name());
            player.sendMessage("§cUnselected §f" + readable(material));
            reopenCurrentSelectionMenu(player, session, sourceMenu);
            return;
        }

    }

    private void handleCratesClick(Player player, Session session, int slot, org.bukkit.event.inventory.ClickType clickType, ItemStack clicked) {
        if (slot == 18 || slot == 26) {
            openMainMenu(player, session);
            return;
        }
        if (clicked == null || !clicked.hasItemMeta()) return;

        String tier = clicked.getItemMeta().getPersistentDataContainer().get(new NamespacedKey(plugin, "editor_crate_tier"), PersistentDataType.STRING);
        if (tier == null) return;
        String mapKey = "CRATE_" + tier;

        if (clickType.isLeftClick()) {
            session.extraItems.put(mapKey, clicked.clone());
            player.sendMessage("§aSelected crate: §f" + tier);
        } else if (clickType.isRightClick()) {
            session.extraItems.remove(mapKey);
            player.sendMessage("§cRemoved crate: §f" + tier);
        }

        openCratesMenu(player, session);
    }

    private void handleXpClick(Player player, Session session, int slot) {
        if (slot == 18 || slot == 26) {
            openMainMenu(player, session);
            return;
        }

        switch (slot) {
            case 10 -> session.xpBottleValue += 500;
            case 11 -> session.xpBottleValue += 1000;
            case 14 -> session.xpBottleValue = Math.max(0, session.xpBottleValue - 500);
            case 15 -> session.xpBottleValue = Math.max(0, session.xpBottleValue - 1000);
            default -> {
            }
        }

        openXpMenu(player, session);
    }

    private void handleMiscClick(Player player, Session session, int slot, org.bukkit.event.inventory.ClickType clickType, ItemStack clicked) {
        if (slot == 18 || slot == 26) {
            openMainMenu(player, session);
            return;
        }
        if (clicked == null || !clicked.hasItemMeta()) return;
        String key = clicked.getItemMeta().getPersistentDataContainer().get(new NamespacedKey(plugin, "editor_misc_key"), PersistentDataType.STRING);
        if (key == null) return;

        if (clickType.isLeftClick()) {
            session.extraItems.put("MISC_" + key, clicked.clone());
            player.sendMessage("§aSelected misc item: §f" + key);
        } else if (clickType.isRightClick()) {
            session.extraItems.remove("MISC_" + key);
            player.sendMessage("§cRemoved misc item: §f" + key);
        }
        openMiscMenu(player, session);
    }

    private void handleEnchantClick(Player player, Session session, int slot, org.bukkit.event.inventory.ClickType clickType, ItemStack clicked) {
        if (slot == 45 || slot == 53) {
            reopenCurrentSelectionMenu(player, session, session.returnFromEnchant);
            return;
        }
        if (clicked == null || !clicked.hasItemMeta() || session.enchantTarget == null) return;

        if (session.type == CreationType.KIT) {
            ItemMeta meta = clicked.getItemMeta();
            String key = meta.getPersistentDataContainer().get(new NamespacedKey(plugin, "vanilla_key"), PersistentDataType.STRING);
            Integer lvl = meta.getPersistentDataContainer().get(new NamespacedKey(plugin, "vanilla_lvl"), PersistentDataType.INTEGER);
            if (key == null || lvl == null) return;
            Enchantment enchantment = Enchantment.getByName(key);
            if (enchantment == null) return;

            Map<Enchantment, Integer> selected = session.vanillaEnchantSelections.computeIfAbsent(session.enchantTarget.name(), k -> new HashMap<>());
            if (clickType.isLeftClick()) {
                selected.put(enchantment, lvl);
                player.sendMessage("§aSet §f" + prettyEnchant(enchantment) + " §ato level §f" + lvl);
            } else if (clickType.isRightClick()) {
                selected.remove(enchantment);
                player.sendMessage("§cRemoved §f" + prettyEnchant(enchantment));
            }
        } else {
            ItemMeta meta = clicked.getItemMeta();
            String id = meta.getPersistentDataContainer().get(new NamespacedKey(plugin, "custom_id"), PersistentDataType.STRING);
            if (id == null) return;
            Set<String> selected = session.customEnchantSelections.computeIfAbsent(session.enchantTarget.name(), k -> new HashSet<>());
            if (selected.contains(id)) {
                selected.remove(id);
                player.sendMessage("§cRemoved custom enchant §f" + id);
            } else {
                selected.add(id);
                player.sendMessage("§aSelected custom enchant §f" + id);
            }
        }

        openEnchantMenu(player, session, session.enchantTarget, session.returnFromEnchant);
    }

    private void finalizeCreation(Player player, Session session) {
        List<ItemStack> items = new ArrayList<>();

        for (Map.Entry<String, ItemStack> entry : session.selectedItems.entrySet()) {
            ItemStack item = entry.getValue().clone();
            applyVanillaEnchants(session, entry.getKey(), item);
            if (session.type == CreationType.GKIT) {
                addCustomEnchantMarker(session, entry.getKey(), item);
            }
            items.add(item);
        }

        for (ItemStack extra : session.extraItems.values()) {
            items.add(extra.clone());
        }

        if (session.xpBottleValue > 0) {
            items.add(createCustomXpBottle(session.xpBottleValue));
        }

        if (items.isEmpty()) {
            player.sendMessage("§cYou must select at least one item before confirming.");
            return;
        }

        if (session.type == CreationType.KIT) {
            RankKit kit = new RankKit(session.name, session.displayName, "§7Custom created kit", RankTier.DEFAULT, 24);
            for (ItemStack item : items) {
                kit.addItem(item);
            }
            rankKitManager.registerKit(kit);
            saveCustomRankKit(kit);
            player.sendMessage("§aCreated kit §f" + session.displayName + "§a. Use §e/kit " + session.name + "§a.");
        } else {
            GKit gkit = new GKit(session.name, session.displayName, "§7Custom created gkit", 24);
            for (ItemStack item : items) {
                gkit.addItem(item);
            }
            kitManager.registerKit(gkit);
            saveCustomGKit(gkit);
            player.sendMessage("§aCreated gkit §f" + session.displayName + "§a. Use §e/gkit " + session.name + "§a.");
        }

        sessions.remove(player.getUniqueId());
        player.closeInventory();
    }

    // ── Persistence ────────────────────────────────────────────────────────────

    /** Load all custom kits/gkits saved by this editor from YAML files. */
    public void loadSavedKits() {
        loadSavedGKits();
        loadSavedRankKits();
    }

    private void loadSavedGKits() {
        File file = new File(plugin.getDataFolder(), "custom_gkits.yml");
        if (!file.exists()) return;
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        for (String key : config.getKeys(false)) {
            ConfigurationSection sec = config.getConfigurationSection(key);
            if (sec == null) continue;
            String displayName = sec.getString("displayName", key);
            String description  = sec.getString("description", "§7Custom created gkit");
            int cooldown        = sec.getInt("cooldownHours", 24);
            GKit gkit = new GKit(key, displayName, description, cooldown);
            ConfigurationSection itemsSec = sec.getConfigurationSection("items");
            if (itemsSec != null) {
                for (String i : itemsSec.getKeys(false)) {
                    ItemStack item = itemsSec.getItemStack(i);
                    if (item != null) gkit.addItem(item);
                }
            }
            kitManager.registerKit(gkit);
        }
        plugin.getLogger().info("Loaded " + config.getKeys(false).size() + " custom gkit(s).");
    }

    private void loadSavedRankKits() {
        File file = new File(plugin.getDataFolder(), "custom_kits.yml");
        if (!file.exists()) return;
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        for (String key : config.getKeys(false)) {
            ConfigurationSection sec = config.getConfigurationSection(key);
            if (sec == null) continue;
            String displayName = sec.getString("displayName", key);
            String description  = sec.getString("description", "§7Custom created kit");
            int cooldown        = sec.getInt("cooldownHours", 24);
            String rankName     = sec.getString("requiredRank", "DEFAULT");
            RankTier tier = RankTier.DEFAULT;
            try { tier = RankTier.valueOf(rankName); } catch (Exception ignored) {}
            RankKit kit = new RankKit(key, displayName, description, tier, cooldown);
            ConfigurationSection itemsSec = sec.getConfigurationSection("items");
            if (itemsSec != null) {
                for (String i : itemsSec.getKeys(false)) {
                    ItemStack item = itemsSec.getItemStack(i);
                    if (item != null) kit.addItem(item);
                }
            }
            rankKitManager.registerKit(kit);
        }
        plugin.getLogger().info("Loaded " + config.getKeys(false).size() + " custom kit(s).");
    }

    private void saveCustomGKit(GKit gkit) {
        File file = new File(plugin.getDataFolder(), "custom_gkits.yml");
        YamlConfiguration config = file.exists()
                ? YamlConfiguration.loadConfiguration(file)
                : new YamlConfiguration();
        String path = gkit.getName();
        config.set(path + ".displayName", gkit.getDisplayName());
        config.set(path + ".description", gkit.getDescription());
        config.set(path + ".cooldownHours", gkit.getCooldownHours());
        ItemStack[] items = gkit.getItems();
        for (int i = 0; i < items.length; i++) {
            config.set(path + ".items." + i, items[i]);
        }
        plugin.getDataFolder().mkdirs();
        try {
            config.save(file);
        } catch (IOException e) {
            plugin.getLogger().warning("Failed to save custom gkit '" + gkit.getName() + "': " + e.getMessage());
        }
    }

    private void saveCustomRankKit(RankKit kit) {
        File file = new File(plugin.getDataFolder(), "custom_kits.yml");
        YamlConfiguration config = file.exists()
                ? YamlConfiguration.loadConfiguration(file)
                : new YamlConfiguration();
        String path = kit.getName();
        config.set(path + ".displayName", kit.getDisplayName());
        config.set(path + ".description", kit.getDescription());
        config.set(path + ".cooldownHours", kit.getCooldownHours());
        config.set(path + ".requiredRank", kit.getRequiredRank().name());
        List<ItemStack> items = kit.getItems();
        for (int i = 0; i < items.size(); i++) {
            config.set(path + ".items." + i, items.get(i));
        }
        plugin.getDataFolder().mkdirs();
        try {
            config.save(file);
        } catch (IOException e) {
            plugin.getLogger().warning("Failed to save custom kit '" + kit.getName() + "': " + e.getMessage());
        }
    }

    private void applyVanillaEnchants(Session session, String itemKey, ItemStack item) {
        Map<Enchantment, Integer> enchants = session.vanillaEnchantSelections.get(itemKey);
        if (enchants != null) {
            enchants.forEach(item::addUnsafeEnchantment);
        }
    }

    private void addCustomEnchantMarker(Session session, String itemKey, ItemStack item) {
        Set<String> selectedIds = session.customEnchantSelections.get(itemKey);
        if (selectedIds == null || selectedIds.isEmpty()) return;
        ItemMeta meta = item.getItemMeta();
        meta.getPersistentDataContainer().set(new NamespacedKey(plugin, "gkit_custom_enchants"), PersistentDataType.STRING, String.join(",", selectedIds));
        item.setItemMeta(meta);
    }

    private ItemStack baseItem(Material material, boolean gkitDefaults) {
        ItemStack item = new ItemStack(material);
        if (gkitDefaults && isArmor(material)) {
            item.addUnsafeEnchantment(Enchantment.PROTECTION_ENVIRONMENTAL, 5);
            item.addUnsafeEnchantment(Enchantment.DURABILITY, 3);
        }
        return item;
    }

    private ItemStack decoratedSelectable(Session session, Material material) {
        boolean selected = session.selectedItems.containsKey(material.name());

        ItemStack item;
        if (selected) {
            // Show the real item with all enchants applied so the admin can see exactly what players will receive
            item = session.selectedItems.get(material.name()).clone();
            applyVanillaEnchants(session, material.name(), item);
            ItemMeta meta = item.getItemMeta();
            meta.setDisplayName("§a" + readable(material));
            List<String> lore = new ArrayList<>();
            // List custom enchant selections in lore for gkits
            if (session.type == CreationType.GKIT) {
                Set<String> customs = session.customEnchantSelections.getOrDefault(material.name(), Collections.emptySet());
                if (!customs.isEmpty()) {
                    lore.add("§bCustom enchants:");
                    for (String id : customs) lore.add("  §7• §b" + id);
                }
            }
            lore.add("§7");
            lore.add("§aSelected");
            lore.add("§7RClick: Unselect  §7Shift+RClick: Enchants");
            meta.setLore(lore);
            item.setItemMeta(meta);
        } else {
            item = new ItemStack(material);
            ItemMeta meta = item.getItemMeta();
            meta.setDisplayName("§7" + readable(material));
            List<String> lore = new ArrayList<>();
            lore.add("§7Not selected");
            lore.add("§7LClick: Select");
            lore.add("§7Shift+RClick: Configure enchants");
            if (session.type == CreationType.GKIT && isArmor(material)) {
                lore.add("§bGKit default: Prot V + Unbreaking III");
            }
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack crateItemFromSimpleCrates(String tier, Session session) {
        ItemStack crate = createSimpleCratesItem(tier);
        ItemMeta meta = crate.getItemMeta();
        boolean selected = session.extraItems.containsKey("CRATE_" + tier);
        List<String> lore = meta.hasLore() ? new ArrayList<>(meta.getLore()) : new ArrayList<>();
        lore.add("");
        lore.add(selected ? "§aSelected" : "§7Not selected");
        lore.add("§7Left click: Select");
        lore.add("§7Right click: Unselect");
        meta.setLore(lore);
        meta.getPersistentDataContainer().set(new NamespacedKey(plugin, "editor_crate_tier"), PersistentDataType.STRING, tier);
        if (selected) {
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            crate.addUnsafeEnchantment(Enchantment.DURABILITY, 1);
        }
        crate.setItemMeta(meta);
        return crate;
    }

    private ItemStack createSimpleCratesItem(String tier) {
        Plugin cratesPlugin = plugin.getServer().getPluginManager().getPlugin("SimpleCrates");
        if (cratesPlugin != null) {
            try {
                var managerField = cratesPlugin.getClass().getDeclaredField("crateManager");
                managerField.setAccessible(true);
                Object crateManager = managerField.get(cratesPlugin);

                Class<?> tierClass = Class.forName("local.simplecrates.CrateTier");
                Object tierEnum = Enum.valueOf((Class<Enum>) tierClass.asSubclass(Enum.class), tier.toUpperCase(Locale.ROOT));

                Method createMethod = crateManager.getClass().getMethod("createCrateItem", tierClass, int.class);
                ItemStack created = (ItemStack) createMethod.invoke(crateManager, tierEnum, 1);
                if (created != null) return created;
            } catch (Exception ignored) {
            }
        }

        ItemStack fallback = new ItemStack(Material.CHEST);
        ItemMeta meta = fallback.getItemMeta();
        String pretty = Character.toUpperCase(tier.charAt(0)) + tier.substring(1).toLowerCase(Locale.ROOT);
        meta.setDisplayName("§f§l" + pretty + " Crate");
        meta.getPersistentDataContainer().set(new NamespacedKey(plugin, "crate_tier"), PersistentDataType.STRING, tier);
        fallback.setItemMeta(meta);
        return fallback;
    }

    private ItemStack createCustomXpBottle(int amount) {
        Plugin economyPlugin = plugin.getServer().getPluginManager().getPlugin("SimpleEconomy");
        if (economyPlugin != null) {
            try {
                Method managerMethod = economyPlugin.getClass().getMethod("getXPBottleManager");
                Object manager = managerMethod.invoke(economyPlugin);
                Method createMethod = manager.getClass().getMethod("createXPBottle", int.class);
                ItemStack bottle = (ItemStack) createMethod.invoke(manager, amount);
                if (bottle != null) return bottle;
            } catch (Exception ignored) {
            }
        }

        ItemStack bottle = new ItemStack(Material.EXPERIENCE_BOTTLE);
        ItemMeta meta = bottle.getItemMeta();
        meta.setDisplayName("§b§lXP Bottle - " + amount + " XP");
        meta.setLore(List.of("§7Right-click to consume", "§7Restores §b" + amount + "§7 experience"));
        bottle.setItemMeta(meta);
        return bottle;
    }

    private ItemStack createBlackScrollItem() {
        ItemStack item = new ItemStack(Material.PAPER);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName("§8§lBlack Scroll");
        meta.setLore(List.of(
                "§7Extracts one random custom enchant",
                "§7from an item with custom enchants."
        ));
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createRandomEnchantBook(String tierName) {
        Plugin fe = plugin.getServer().getPluginManager().getPlugin("FactionEnchants");
        if (fe != null) {
            try {
                Class<?> tierClass = Class.forName("com.factionenchants.enchantments.CustomEnchantment$EnchantTier");
                Object tierEnum = Enum.valueOf((Class<Enum>) tierClass.asSubclass(Enum.class), tierName.toUpperCase(Locale.ROOT));
                Class<?> enchantBookClass = Class.forName("com.factionenchants.books.EnchantBook");
                Method createRandomBook = enchantBookClass.getMethod("createRandomBook", tierClass);
                ItemStack item = (ItemStack) createRandomBook.invoke(null, tierEnum);
                if (item != null) return item;
            } catch (Exception ignored) {
            }
        }

        ItemStack fallback = new ItemStack(Material.BOOK);
        ItemMeta meta = fallback.getItemMeta();
        meta.setDisplayName("§f§l" + tierName + " Enchant Book");
        fallback.setItemMeta(meta);
        return fallback;
    }

    private ItemStack miscSelectable(Session session, String key, ItemStack base) {
        ItemStack item = base.clone();
        ItemMeta meta = item.getItemMeta();
        boolean selected = session.extraItems.containsKey("MISC_" + key);
        List<String> lore = meta.hasLore() ? new ArrayList<>(meta.getLore()) : new ArrayList<>();
        lore.add("");
        lore.add(selected ? "§aSelected" : "§7Not selected");
        lore.add("§7Left click: Select");
        lore.add("§7Right click: Unselect");
        meta.setLore(lore);
        meta.getPersistentDataContainer().set(new NamespacedKey(plugin, "editor_misc_key"), PersistentDataType.STRING, key);
        if (selected) {
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            item.addUnsafeEnchantment(Enchantment.DURABILITY, 1);
        }
        item.setItemMeta(meta);
        return item;
    }

    private List<EnchantOption> buildVanillaOptions(Material material) {
        List<EnchantOption> options = new ArrayList<>();
        ItemStack probe = new ItemStack(material);
        List<Enchantment> common = List.of(
                Enchantment.PROTECTION_ENVIRONMENTAL,
                Enchantment.PROTECTION_FIRE,
                Enchantment.PROTECTION_PROJECTILE,
                Enchantment.PROTECTION_EXPLOSIONS,
                Enchantment.PROTECTION_FALL,
                Enchantment.DURABILITY,
                Enchantment.DAMAGE_ALL,
                Enchantment.DAMAGE_UNDEAD,
                Enchantment.DAMAGE_ARTHROPODS,
                Enchantment.FIRE_ASPECT,
                Enchantment.KNOCKBACK,
                Enchantment.LOOT_BONUS_MOBS,
                Enchantment.DIG_SPEED,
                Enchantment.LOOT_BONUS_BLOCKS,
                Enchantment.SILK_TOUCH,
                Enchantment.ARROW_DAMAGE,
                Enchantment.ARROW_FIRE,
                Enchantment.ARROW_KNOCKBACK,
                Enchantment.ARROW_INFINITE,
                Enchantment.THORNS,
                Enchantment.OXYGEN,
                Enchantment.WATER_WORKER,
                Enchantment.DEPTH_STRIDER,
                Enchantment.FROST_WALKER,
                Enchantment.SOUL_SPEED
        );

        for (Enchantment enchantment : common) {
            if (!enchantment.canEnchantItem(probe)) continue;
            int max = Math.min(5, Math.max(1, enchantment.getMaxLevel()));
            for (int lvl = 1; lvl <= max; lvl++) {
                options.add(new EnchantOption(enchantment, lvl));
            }
        }
        return options;
    }

    private ItemStack enchantOptionItem(EnchantOption option, Session session) {
        ItemStack book = new ItemStack(Material.BOOK);
        ItemMeta meta = book.getItemMeta();
        String key = session.enchantTarget == null ? "" : session.enchantTarget.name();
        int current = session.vanillaEnchantSelections.getOrDefault(key, Collections.emptyMap()).getOrDefault(option.enchantment(), 0);
        boolean selected = current == option.level();

        meta.setDisplayName((selected ? "§a" : "§f") + prettyEnchant(option.enchantment()) + " " + option.level());
        meta.setLore(List.of(
                selected ? "§aSelected" : "§7Not selected",
                "§7Left click: Set this level",
                "§7Right click: Remove this enchant"
        ));
        meta.getPersistentDataContainer().set(new NamespacedKey(plugin, "vanilla_key"), PersistentDataType.STRING, option.enchantment().getName());
        meta.getPersistentDataContainer().set(new NamespacedKey(plugin, "vanilla_lvl"), PersistentDataType.INTEGER, option.level());
        if (selected) {
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            book.addUnsafeEnchantment(Enchantment.DURABILITY, 1);
        }
        book.setItemMeta(meta);
        return book;
    }

    private ItemStack customEnchantOptionItem(CustomEnchantOption option, Session session) {
        ItemStack book = new ItemStack(Material.BOOK);
        ItemMeta meta = book.getItemMeta();
        String key = session.enchantTarget == null ? "" : session.enchantTarget.name();
        boolean selected = session.customEnchantSelections.getOrDefault(key, Collections.emptySet()).contains(option.id);

        meta.setDisplayName((selected ? "§a" : option.colorCode) + option.displayName);
        meta.setLore(List.of(
                "§7Tier: " + option.colorCode + option.tierName,
                "§7Max Level: §f" + option.maxLevel,
                selected ? "§aSelected" : "§7Not selected",
                "§7Left click: Toggle selection",
                "§7These levels are randomized on claim"
        ));
        meta.getPersistentDataContainer().set(new NamespacedKey(plugin, "custom_id"), PersistentDataType.STRING, option.id);
        if (selected) {
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            book.addUnsafeEnchantment(Enchantment.DURABILITY, 1);
        }
        book.setItemMeta(meta);
        return book;
    }

    private List<CustomEnchantOption> getCustomOptions(Material material) {
        Plugin fe = plugin.getServer().getPluginManager().getPlugin("FactionEnchants");
        if (fe == null) return Collections.emptyList();

        try {
            Method managerMethod = fe.getClass().getMethod("getEnchantmentManager");
            Object manager = managerMethod.invoke(fe);
            Method getAll = manager.getClass().getMethod("getAllEnchantments");
            Collection<?> all = (Collection<?>) getAll.invoke(manager);
            List<CustomEnchantOption> out = new ArrayList<>();
            ItemStack probe = new ItemStack(material);

            for (Object enchant : all) {
                boolean applies = (boolean) enchant.getClass().getMethod("canApplyTo", ItemStack.class).invoke(enchant, probe);
                if (!applies) continue;

                Object tierObj = enchant.getClass().getMethod("getTier").invoke(enchant);
                String tierName = tierObj.toString();
                int order = tierOrder(tierName);
                if (order > 6) continue;

                String colorCode = "§" + tierObj.getClass().getMethod("getColor").invoke(tierObj);
                String id = (String) enchant.getClass().getMethod("getId").invoke(enchant);
                String display = (String) enchant.getClass().getMethod("getDisplayName").invoke(enchant);
                int maxLevel = (int) enchant.getClass().getMethod("getMaxLevel").invoke(enchant);
                out.add(new CustomEnchantOption(id, display, maxLevel, tierName, colorCode, order));
            }

            out.sort(Comparator.comparingInt((CustomEnchantOption o) -> o.order)
                    .thenComparing(o -> strip(o.displayName).toLowerCase(Locale.ROOT)));
            return out;
        } catch (Exception e) {
            plugin.getLogger().warning("Could not load FactionEnchants options: " + e.getMessage());
            return Collections.emptyList();
        }
    }

    private int tierOrder(String tierName) {
        return switch (tierName.toUpperCase(Locale.ROOT)) {
            case "SIMPLE" -> 1;
            case "UNIQUE" -> 2;
            case "ELITE" -> 3;
            case "ULTIMATE" -> 4;
            case "LEGENDARY" -> 5;
            case "SOUL" -> 6;
            case "HEROIC" -> 7;
            case "MASTERY" -> 8;
            default -> 99;
        };
    }

    private void openPreviewMenu(Player player, Session session) {
        Inventory inv = Bukkit.createInventory(null, 54, TITLE_PREVIEW_PREFIX + session.displayName);

        List<ItemStack> items = new ArrayList<>();

        for (Map.Entry<String, ItemStack> entry : session.selectedItems.entrySet()) {
            ItemStack item = entry.getValue().clone();
            applyVanillaEnchants(session, entry.getKey(), item);
            // Annotate custom enchant selections in lore so they're visible
            if (session.type == CreationType.GKIT) {
                Set<String> customs = session.customEnchantSelections.getOrDefault(entry.getKey(), Collections.emptySet());
                if (!customs.isEmpty()) {
                    ItemMeta meta = item.getItemMeta();
                    List<String> lore = meta.hasLore() ? new ArrayList<>(meta.getLore()) : new ArrayList<>();
                    lore.add("§bCustom enchants:");
                    for (String id : customs) lore.add("  §7• §b" + id);
                    meta.setLore(lore);
                    item.setItemMeta(meta);
                }
            }
            items.add(item);
        }
        for (ItemStack extra : session.extraItems.values()) items.add(extra.clone());
        if (session.xpBottleValue > 0) items.add(createCustomXpBottle(session.xpBottleValue));

        for (int i = 0; i < Math.min(items.size(), 45); i++) {
            inv.setItem(i, items.get(i));
        }

        inv.setItem(45, named(Material.ARROW, "§eBack", List.of("§7Return to main menu")));
        fill(inv, Material.BLACK_STAINED_GLASS_PANE);
        player.openInventory(inv);
        session.menuState = MenuState.PREVIEW;
    }

    private void reopenCurrentSelectionMenu(Player player, Session session, MenuState source) {
        if (source == MenuState.GEAR) {
            openGearMenu(player, session);
        } else {
            openToolsMenu(player, session);
        }
    }

    private void clearExistingArmorSlot(Session session, Material newMat) {
        for (String key : new ArrayList<>(session.selectedItems.keySet())) {
            Material existing = Material.matchMaterial(key);
            if (existing == null || !isArmor(existing)) continue;
            if (armorSuffix(existing).equals(armorSuffix(newMat))) {
                session.selectedItems.remove(key);
                session.vanillaEnchantSelections.remove(key);
                session.customEnchantSelections.remove(key);
            }
        }
    }

    private String armorSuffix(Material material) {
        String n = material.name();
        if (n.endsWith("_HELMET")) return "HELMET";
        if (n.endsWith("_CHESTPLATE")) return "CHESTPLATE";
        if (n.endsWith("_LEGGINGS")) return "LEGGINGS";
        if (n.endsWith("_BOOTS")) return "BOOTS";
        return n;
    }

    private boolean isArmor(Material material) {
        String n = material.name();
        return n.endsWith("_HELMET") || n.endsWith("_CHESTPLATE") || n.endsWith("_LEGGINGS") || n.endsWith("_BOOTS");
    }

    private ItemStack named(Material material, String name, List<String> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private void fill(Inventory inv, Material material) {
        for (int i = 0; i < inv.getSize(); i++) {
            if (inv.getItem(i) == null) {
                inv.setItem(i, named(material, " ", Collections.emptyList()));
            }
        }
    }

    private void fillMainAreaEmpty(Inventory inv, Material material) {
        for (int i = 0; i < 45; i++) {
            if (inv.getItem(i) == null) {
                inv.setItem(i, named(material, " ", Collections.emptyList()));
            }
        }
    }

    private void addBackDone(Inventory inv) {
        inv.setItem(45, named(Material.ARROW, "§eBack", List.of("§7Return to main menu")));
        inv.setItem(53, named(Material.EMERALD, "§aDone", List.of("§7Return to main menu")));
        fill(inv, Material.GRAY_STAINED_GLASS_PANE);
    }

    private String color(String value) {
        return value.replace('&', '§');
    }

    private String strip(String value) {
        return value.replaceAll("§[0-9a-fk-or]", "").trim();
    }

    private String readable(Material material) {
        String[] parts = material.name().toLowerCase(Locale.ROOT).split("_");
        StringBuilder sb = new StringBuilder();
        for (String part : parts) {
            if (sb.length() > 0) sb.append(' ');
            sb.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1));
        }
        return sb.toString();
    }

    private String prettyEnchant(Enchantment enchantment) {
        String raw = enchantment.getKey().getKey().replace('_', ' ');
        String[] parts = raw.split(" ");
        StringBuilder sb = new StringBuilder();
        for (String part : parts) {
            if (sb.length() > 0) sb.append(' ');
            sb.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1));
        }
        return sb.toString();
    }

    private record EnchantOption(Enchantment enchantment, int level) {}

    private static final class CustomEnchantOption {
        private final String id;
        private final String displayName;
        private final int maxLevel;
        private final String tierName;
        private final String colorCode;
        private final int order;

        private CustomEnchantOption(String id, String displayName, int maxLevel, String tierName, String colorCode, int order) {
            this.id = id;
            this.displayName = displayName;
            this.maxLevel = maxLevel;
            this.tierName = tierName;
            this.colorCode = colorCode;
            this.order = order;
        }
    }

    private static final class Session {
        private final CreationType type;
        private final String name;
        private final String displayName;

        private MenuState menuState = MenuState.MAIN;
        private MenuState returnFromEnchant = MenuState.MAIN;
        private Material enchantTarget;

        private final Map<String, ItemStack> selectedItems = new LinkedHashMap<>();
        private final Map<String, ItemStack> extraItems = new LinkedHashMap<>();
        private final Map<String, Map<Enchantment, Integer>> vanillaEnchantSelections = new HashMap<>();
        private final Map<String, Set<String>> customEnchantSelections = new HashMap<>();
        private int xpBottleValue = 0;

        private Session(CreationType type, String name, String displayName) {
            this.type = type;
            this.name = name;
            this.displayName = displayName;
        }
    }
}
