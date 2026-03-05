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

import java.lang.reflect.Method;
import java.util.*;

public class KitEditorManager {

    public enum CreationType { KIT, GKIT }

    enum MenuState { MAIN, GEAR, TOOLS, CRATES, XP, ENCHANTS }

    private static final String TITLE_MAIN_PREFIX = "§8Builder: ";
    private static final String TITLE_GEAR_PREFIX = "§8Gear: ";
    private static final String TITLE_TOOLS_PREFIX = "§8Tools: ";
    private static final String TITLE_CRATES_PREFIX = "§8Crates: ";
    private static final String TITLE_XP_PREFIX = "§8XP: ";
    private static final String TITLE_ENCHANTS_PREFIX = "§8Enchants: ";

    private static final List<Material> GEAR_OPTIONS = List.of(
            Material.LEATHER_HELMET, Material.CHAINMAIL_HELMET, Material.IRON_HELMET, Material.DIAMOND_HELMET, Material.NETHERITE_HELMET,
            Material.LEATHER_CHESTPLATE, Material.CHAINMAIL_CHESTPLATE, Material.IRON_CHESTPLATE, Material.DIAMOND_CHESTPLATE, Material.NETHERITE_CHESTPLATE,
            Material.LEATHER_LEGGINGS, Material.CHAINMAIL_LEGGINGS, Material.IRON_LEGGINGS, Material.DIAMOND_LEGGINGS, Material.NETHERITE_LEGGINGS,
            Material.LEATHER_BOOTS, Material.CHAINMAIL_BOOTS, Material.IRON_BOOTS, Material.DIAMOND_BOOTS, Material.NETHERITE_BOOTS
    );

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

        Session session = new Session(player.getUniqueId(), type, keyName, displayName);
        sessions.put(player.getUniqueId(), session);
        openMainMenu(player, session);
    }

    public boolean hasSession(UUID playerId) {
        return sessions.containsKey(playerId);
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
            handleXpClick(player, session, slot, clickType, clicked);
            return true;
        }
        if (title.startsWith(TITLE_ENCHANTS_PREFIX)) {
            handleEnchantClick(player, session, slot, clickType, clicked);
            return true;
        }
        return false;
    }

    private void openMainMenu(Player player, Session session) {
        Inventory inv = Bukkit.createInventory(null, 27, TITLE_MAIN_PREFIX + session.displayName);

        inv.setItem(10, named(Material.DIAMOND_CHESTPLATE, "§bGear", List.of("§7Configure armor pieces")));
        inv.setItem(11, named(Material.DIAMOND_SWORD, "§bTools / Weapons", List.of("§7Configure weapons and tools")));
        inv.setItem(12, named(Material.CHEST, "§bCrates", List.of("§7Add crate items")));
        inv.setItem(13, named(Material.EXPERIENCE_BOTTLE, "§bXP", List.of("§7Set XP bottles amount")));

        inv.setItem(15, named(Material.EMERALD_BLOCK, "§aConfirm & Create", List.of(
                "§7Kit Name: §f" + session.displayName,
                "§7Selected Items: §f" + session.selectedItems.size(),
                "§7XP Bottles: §f" + session.xpBottles
        )));
        inv.setItem(16, named(Material.BARRIER, "§cCancel", List.of("§7Cancel this creation session")));

        fill(inv, Material.GRAY_STAINED_GLASS_PANE);
        player.openInventory(inv);
        session.menuState = MenuState.MAIN;
    }

    private void openGearMenu(Player player, Session session) {
        Inventory inv = Bukkit.createInventory(null, 54, TITLE_GEAR_PREFIX + session.displayName);
        int slot = 0;
        for (Material material : GEAR_OPTIONS) {
            inv.setItem(slot++, decoratedSelectable(session, material));
        }
        addBackConfirmHints(inv);
        player.openInventory(inv);
        session.menuState = MenuState.GEAR;
    }

    private void openToolsMenu(Player player, Session session) {
        Inventory inv = Bukkit.createInventory(null, 54, TITLE_TOOLS_PREFIX + session.displayName);
        int slot = 0;
        for (Material material : TOOL_OPTIONS) {
            inv.setItem(slot++, decoratedSelectable(session, material));
        }
        addBackConfirmHints(inv);
        player.openInventory(inv);
        session.menuState = MenuState.TOOLS;
    }

    private void openCratesMenu(Player player, Session session) {
        Inventory inv = Bukkit.createInventory(null, 27, TITLE_CRATES_PREFIX + session.displayName);

        inv.setItem(10, crateItem("simple", "§7§lSimple Crate", session));
        inv.setItem(11, crateItem("unique", "§a§lUnique Crate", session));
        inv.setItem(12, crateItem("elite", "§b§lElite Crate", session));
        inv.setItem(13, crateItem("ultimate", "§5§lUltimate Crate", session));
        inv.setItem(14, crateItem("legendary", "§6§lLegendary Crate", session));
        inv.setItem(15, crateItem("godly", "§d§lGodly Crate", session));

        inv.setItem(18, named(Material.ARROW, "§eBack", List.of("§7Return to main menu")));
        inv.setItem(26, named(Material.EMERALD, "§aDone", List.of("§7Return to main menu")));
        fill(inv, Material.GRAY_STAINED_GLASS_PANE);
        player.openInventory(inv);
        session.menuState = MenuState.CRATES;
    }

    private void openXpMenu(Player player, Session session) {
        Inventory inv = Bukkit.createInventory(null, 27, TITLE_XP_PREFIX + session.displayName);
        inv.setItem(10, named(Material.EXPERIENCE_BOTTLE, "§a+16 XP Bottles", List.of("§7Left click to add")));
        inv.setItem(11, named(Material.EXPERIENCE_BOTTLE, "§a+32 XP Bottles", List.of("§7Left click to add")));
        inv.setItem(12, named(Material.EXPERIENCE_BOTTLE, "§a+64 XP Bottles", List.of("§7Left click to add")));
        inv.setItem(14, named(Material.REDSTONE, "§c-16 XP Bottles", List.of("§7Right click style remove")));
        inv.setItem(15, named(Material.REDSTONE, "§c-32 XP Bottles", List.of("§7Right click style remove")));
        inv.setItem(16, named(Material.REDSTONE, "§c-64 XP Bottles", List.of("§7Right click style remove")));
        inv.setItem(13, named(Material.BOOK, "§eCurrent: §f" + session.xpBottles, List.of("§7Stored as experience bottles")));
        inv.setItem(18, named(Material.ARROW, "§eBack", List.of("§7Return to main menu")));
        inv.setItem(26, named(Material.EMERALD, "§aDone", List.of("§7Return to main menu")));
        fill(inv, Material.GRAY_STAINED_GLASS_PANE);
        player.openInventory(inv);
        session.menuState = MenuState.XP;
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

        inv.setItem(45, named(Material.ARROW, "§eBack", List.of("§7Return without closing editor")));
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
            case 15 -> finalizeCreation(player, session);
            case 16 -> {
                sessions.remove(player.getUniqueId());
                player.closeInventory();
                player.sendMessage("§cCreation cancelled.");
            }
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
        if (material == Material.AIR || material.name().contains("GLASS") || material == Material.ARROW || material == Material.EMERALD) return;

        if (clickType.isLeftClick()) {
            if (isArmor(material)) {
                clearExistingArmorSlot(session, material);
            }
            session.selectedItems.put(material.name(), baseItem(material, session.type == CreationType.GKIT));
            player.sendMessage("§aSelected §f" + readable(material));
            reopenCurrentSelectionMenu(player, session, sourceMenu);
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

        if (clickType == org.bukkit.event.inventory.ClickType.MIDDLE) {
            session.selectedItems.putIfAbsent(material.name(), baseItem(material, session.type == CreationType.GKIT));
            openEnchantMenu(player, session, material, sourceMenu);
        }
    }

    private void handleCratesClick(Player player, Session session, int slot, org.bukkit.event.inventory.ClickType clickType, ItemStack clicked) {
        if (slot == 18 || slot == 26) {
            openMainMenu(player, session);
            return;
        }
        if (clicked == null || clicked.getType() != Material.CHEST || !clicked.hasItemMeta()) return;
        String key = clicked.getItemMeta().getPersistentDataContainer().get(new NamespacedKey(plugin, "crate_tier"), PersistentDataType.STRING);
        if (key == null) return;
        String mapKey = "CRATE_" + key;
        if (clickType.isLeftClick()) {
            session.extraItems.put(mapKey, clicked.clone());
            player.sendMessage("§aSelected crate: §f" + key);
        } else if (clickType.isRightClick()) {
            session.extraItems.remove(mapKey);
            player.sendMessage("§cRemoved crate: §f" + key);
        }
        openCratesMenu(player, session);
    }

    private void handleXpClick(Player player, Session session, int slot, org.bukkit.event.inventory.ClickType clickType, ItemStack clicked) {
        if (slot == 18 || slot == 26) {
            openMainMenu(player, session);
            return;
        }
        if (clicked == null) return;
        switch (slot) {
            case 10 -> session.xpBottles += 16;
            case 11 -> session.xpBottles += 32;
            case 12 -> session.xpBottles += 64;
            case 14 -> session.xpBottles = Math.max(0, session.xpBottles - 16);
            case 15 -> session.xpBottles = Math.max(0, session.xpBottles - 32);
            case 16 -> session.xpBottles = Math.max(0, session.xpBottles - 64);
            default -> {
            }
        }
        openXpMenu(player, session);
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
        if (session.xpBottles > 0) {
            items.add(new ItemStack(Material.EXPERIENCE_BOTTLE, Math.min(64, session.xpBottles)));
            int rem = session.xpBottles - 64;
            while (rem > 0) {
                items.add(new ItemStack(Material.EXPERIENCE_BOTTLE, Math.min(64, rem)));
                rem -= 64;
            }
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
            player.sendMessage("§aCreated kit §f" + session.displayName + "§a. Use §e/kit " + session.name + "§a.");
        } else {
            GKit gkit = new GKit(session.name, session.displayName, "§7Custom created gkit", 24);
            for (ItemStack item : items) {
                gkit.addItem(item);
            }
            kitManager.registerKit(gkit);
            player.sendMessage("§aCreated gkit §f" + session.displayName + "§a. Use §e/gkit " + session.name + "§a.");
        }

        sessions.remove(player.getUniqueId());
        player.closeInventory();
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
        if (!gkitDefaults) return item;
        if (isArmor(material)) {
            item.addUnsafeEnchantment(Enchantment.PROTECTION_ENVIRONMENTAL, 5);
            item.addUnsafeEnchantment(Enchantment.DURABILITY, 3);
        }
        return item;
    }

    private ItemStack decoratedSelectable(Session session, Material material) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        boolean selected = session.selectedItems.containsKey(material.name());
        meta.setDisplayName((selected ? "§a" : "§f") + readable(material));
        List<String> lore = new ArrayList<>();
        lore.add(selected ? "§aSelected" : "§7Not selected");
        lore.add("§7Left click: Select");
        lore.add("§7Right click: Unselect");
        lore.add("§7Middle click: Configure enchants");
        if (session.type == CreationType.GKIT && isArmor(material)) {
            lore.add("§bGKit default: Prot V + Unbreaking III");
        }
        meta.setLore(lore);
        if (selected) {
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            item.addUnsafeEnchantment(Enchantment.DURABILITY, 1);
        }
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack crateItem(String tier, String displayName, Session session) {
        ItemStack item = new ItemStack(Material.CHEST);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(displayName);
        boolean selected = session.extraItems.containsKey("CRATE_" + tier);
        meta.setLore(List.of(
                selected ? "§aSelected" : "§7Not selected",
                "§7Left click: Select",
                "§7Right click: Unselect"
        ));
        meta.getPersistentDataContainer().set(new NamespacedKey(plugin, "crate_tier"), PersistentDataType.STRING, tier);
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
        ItemStack book = new ItemStack(Material.ENCHANTED_BOOK);
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
        ItemStack book = new ItemStack(Material.ENCHANTED_BOOK);
        ItemMeta meta = book.getItemMeta();
        String key = session.enchantTarget == null ? "" : session.enchantTarget.name();
        boolean selected = session.customEnchantSelections.getOrDefault(key, Collections.emptySet()).contains(option.id());
        meta.setDisplayName((selected ? "§a" : "§f") + option.displayName());
        meta.setLore(List.of(
                "§7Max Level: §f" + option.maxLevel(),
                selected ? "§aSelected" : "§7Not selected",
                "§7Left click: Toggle selection",
                "§7These levels are randomized on claim"
        ));
        meta.getPersistentDataContainer().set(new NamespacedKey(plugin, "custom_id"), PersistentDataType.STRING, option.id());
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
                Method canApply = enchant.getClass().getMethod("canApplyTo", ItemStack.class);
                boolean applies = (boolean) canApply.invoke(enchant, probe);
                if (!applies) continue;
                String id = (String) enchant.getClass().getMethod("getId").invoke(enchant);
                String display = (String) enchant.getClass().getMethod("getDisplayName").invoke(enchant);
                int maxLevel = (int) enchant.getClass().getMethod("getMaxLevel").invoke(enchant);
                out.add(new CustomEnchantOption(id, display, maxLevel));
            }
            out.sort(Comparator.comparing(CustomEnchantOption::displayName));
            return out;
        } catch (Exception e) {
            plugin.getLogger().warning("Could not load FactionEnchants options: " + e.getMessage());
            return Collections.emptyList();
        }
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
            if (sameArmorSlot(existing, newMat)) {
                session.selectedItems.remove(key);
                session.vanillaEnchantSelections.remove(key);
                session.customEnchantSelections.remove(key);
            }
        }
    }

    private boolean sameArmorSlot(Material a, Material b) {
        return armorSuffix(a).equals(armorSuffix(b));
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

    private void addBackConfirmHints(Inventory inv) {
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
    private record CustomEnchantOption(String id, String displayName, int maxLevel) {}

    private static final class Session {
        private final UUID playerId;
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
        private int xpBottles = 0;

        private Session(UUID playerId, CreationType type, String name, String displayName) {
            this.playerId = playerId;
            this.type = type;
            this.name = name;
            this.displayName = displayName;
        }
    }
}
