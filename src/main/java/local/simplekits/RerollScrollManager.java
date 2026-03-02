package local.simplekits;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Manages Reroll Scrolls - paper items that reroll success/destroy percentages on custom enchanted books.
 * Tiers (Simple, Unique, Elite, Ultimate, Legendary, Godly) restrict which books can be rerolled:
 * - Simple: can reroll Simple books only
 * - Unique: can reroll Simple-Unique books
 * - Elite: can reroll Simple-Elite books
 * - Ultimate: can reroll Simple-Ultimate books
 * - Legendary: can reroll Simple-Legendary books
 * - Godly: can reroll any book (Simple-Godly)
 */
public class RerollScrollManager {

    private final JavaPlugin plugin;
    private final NamespacedKey rerollScrollKey;
    private final NamespacedKey rerollTierKey;
    private final NamespacedKey bookSuccessKey;
    private final NamespacedKey bookDestroyKey;
    private final NamespacedKey bookTierKey;
    private final Random random = new Random();

    public enum RerollTier {
        SIMPLE(1, "§7"),
        UNIQUE(2, "§a"),
        ELITE(3, "§b"),
        ULTIMATE(4, "§5"),
        LEGENDARY(5, "§6"),
        GODLY(6, "§d");

        final int level;
        final String color;

        RerollTier(int level, String color) {
            this.level = level;
            this.color = color;
        }
    }

    public RerollScrollManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.rerollScrollKey = new NamespacedKey(plugin, "reroll_scroll");
        this.rerollTierKey = new NamespacedKey(plugin, "reroll_tier");
        this.bookSuccessKey = new NamespacedKey(plugin, "book_success");
        this.bookDestroyKey = new NamespacedKey(plugin, "book_destroy");
        this.bookTierKey = new NamespacedKey(plugin, "book_tier");
    }

    public ItemStack createRerollScroll(RerollTier tier) {
        ItemStack scroll = new ItemStack(Material.PAPER);
        ItemMeta meta = scroll.getItemMeta();

        meta.setDisplayName(tier.color + "§l✦ " + tier.name() + " Reroll Scroll §l✦");
        List<String> lore = new ArrayList<>();
        lore.add("§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
        lore.add("§7Reroll success/destroy rates");
        lore.add("§7on enchanted books");
        lore.add("§7");
        lore.add("§eTarget: " + tier.color + tier.name() + " §eor lower");
        lore.add("§7");
        lore.add("§eRight-click on a book to apply");
        lore.add("§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
        meta.setLore(lore);

        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        pdc.set(rerollScrollKey, PersistentDataType.BYTE, (byte) 1);
        pdc.set(rerollTierKey, PersistentDataType.STRING, tier.name());

        scroll.setItemMeta(meta);
        return scroll;
    }

    public boolean isRerollScroll(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        PersistentDataContainer pdc = item.getItemMeta().getPersistentDataContainer();
        if (pdc.has(rerollScrollKey, PersistentDataType.BYTE)) return true;
        return looksLikeRerollScrollByLore(item);
    }

    public RerollTier getRerollTier(ItemStack item) {
        if (!isRerollScroll(item)) return null;
        String tierName = item.getItemMeta().getPersistentDataContainer().get(rerollTierKey, PersistentDataType.STRING);
        if (tierName == null || tierName.isBlank()) {
            tierName = parseTierFromScrollText(item);
        }
        try {
            return RerollTier.valueOf(tierName);
        } catch (Exception e) {
            return null;
        }
    }

    public boolean isEnchantedBook(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        PersistentDataContainer pdc = item.getItemMeta().getPersistentDataContainer();
        if (pdc.has(bookSuccessKey, PersistentDataType.INTEGER) &&
                pdc.has(bookDestroyKey, PersistentDataType.INTEGER)) {
            return true;
        }
        return looksLikeEnchantedBookByLore(item);
    }

    public boolean canReroll(ItemStack scroll, ItemStack book) {
        if (!isRerollScroll(scroll) || !isEnchantedBook(book)) return false;

        RerollTier scrollTier = getRerollTier(scroll);
        Integer bookTierValue = book.getItemMeta().getPersistentDataContainer().get(bookTierKey, PersistentDataType.INTEGER);

        if (bookTierValue == null) {
            RerollTier inferredBookTier = parseTierFromBookText(book);
            if (inferredBookTier != null) {
                bookTierValue = inferredBookTier.level;
            }
        }

        if (scrollTier == null || bookTierValue == null) return scrollTier != null;

        // Check if scroll tier is high enough to reroll this book
        return scrollTier.level >= bookTierValue;
    }

    private boolean looksLikeRerollScrollByLore(ItemStack item) {
        if (item.getType() != Material.PAPER) return false;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return false;

        String name = ChatColor.stripColor(meta.getDisplayName() == null ? "" : meta.getDisplayName());
        if (name.toLowerCase(Locale.ENGLISH).contains("reroll scroll")) return true;

        if (!meta.hasLore()) return false;
        boolean hasReroll = false;
        boolean hasTarget = false;
        for (String raw : meta.getLore()) {
            String line = ChatColor.stripColor(raw == null ? "" : raw).toLowerCase(Locale.ENGLISH);
            if (line.contains("reroll") && line.contains("book")) hasReroll = true;
            if (line.contains("target:")) hasTarget = true;
        }
        return hasReroll && hasTarget;
    }

    private String parseTierFromScrollText(ItemStack scroll) {
        ItemMeta meta = scroll.getItemMeta();
        if (meta == null) return null;

        String fromName = parseTierToken(meta.getDisplayName());
        if (fromName != null) return fromName;

        if (!meta.hasLore()) return null;
        for (String raw : meta.getLore()) {
            String cleaned = ChatColor.stripColor(raw == null ? "" : raw);
            if (!cleaned.toLowerCase(Locale.ENGLISH).contains("target:")) continue;
            String token = parseTierToken(cleaned);
            if (token != null) return token;
        }
        return null;
    }

    private RerollTier parseTierFromBookText(ItemStack book) {
        ItemMeta meta = book.getItemMeta();
        if (meta == null) return null;

        String token = parseTierToken(meta.getDisplayName());
        if (token != null) {
            try {
                return RerollTier.valueOf(token);
            } catch (IllegalArgumentException ignored) {
            }
        }

        if (!meta.hasLore()) return null;
        for (String raw : meta.getLore()) {
            token = parseTierToken(raw);
            if (token != null) {
                try {
                    return RerollTier.valueOf(token);
                } catch (IllegalArgumentException ignored) {
                }
            }
        }
        return null;
    }

    private boolean looksLikeEnchantedBookByLore(ItemStack item) {
        if (item.getType() != Material.ENCHANTED_BOOK) return false;
        ItemMeta meta = item.getItemMeta();
        if (meta == null || !meta.hasLore()) return false;

        boolean hasSuccess = false;
        boolean hasDestroy = false;
        Pattern percentage = Pattern.compile("\\d{1,3}%");
        for (String raw : meta.getLore()) {
            String line = ChatColor.stripColor(raw == null ? "" : raw);
            String lower = line.toLowerCase(Locale.ENGLISH);
            Matcher matcher = percentage.matcher(line);
            if ((lower.contains("success") || lower.contains("success rate")) && matcher.find()) hasSuccess = true;
            if ((lower.contains("destroy") || lower.contains("destroy rate")) && matcher.find()) hasDestroy = true;
        }

        return hasSuccess && hasDestroy;
    }

    private String parseTierToken(String text) {
        String clean = ChatColor.stripColor(text == null ? "" : text).toUpperCase(Locale.ENGLISH);
        for (RerollTier tier : RerollTier.values()) {
            if (clean.contains(tier.name())) return tier.name();
        }
        return null;
    }

    public void rerollBook(ItemStack book, ItemStack scroll) {
        if (!isEnchantedBook(book)) return;

        ItemMeta meta = book.getItemMeta();
        PersistentDataContainer pdc = meta.getPersistentDataContainer();

        // Generate new random values for success and destroy
        int newSuccess = random.nextInt(101); // 0-100
        int newDestroy = random.nextInt(101);  // 0-100

        pdc.set(bookSuccessKey, PersistentDataType.INTEGER, newSuccess);
        pdc.set(bookDestroyKey, PersistentDataType.INTEGER, newDestroy);

        // Update the lore to show new values
        updateBookLore(meta, newSuccess, newDestroy);

        book.setItemMeta(meta);
    }

    private void updateBookLore(ItemMeta meta, int success, int destroy) {
        List<String> lore = meta.hasLore() ? new ArrayList<>(meta.getLore()) : new ArrayList<>();
        
        // Find and update success/destroy lines
        boolean updatedSuccess = false;
        boolean updatedDestroy = false;

        for (int i = 0; i < lore.size(); i++) {
            String line = lore.get(i);
            if (line.contains("Success:") || line.contains("Success Rate:")) {
                lore.set(i, "§eSuccess: §f" + success + "%");
                updatedSuccess = true;
            } else if (line.contains("Destroy:") || line.contains("Destroy Rate:")) {
                lore.set(i, "§cDestroy: §f" + destroy + "%");
                updatedDestroy = true;
            }
        }

        // If not found in lore, add them
        if (!updatedSuccess) {
            lore.add("§eSuccess: §f" + success + "%");
        }
        if (!updatedDestroy) {
            lore.add("§cDestroy: §f" + destroy + "%");
        }

        meta.setLore(lore);
    }

    public int getBookSuccess(ItemStack book) {
        if (!isEnchantedBook(book)) return 0;
        Integer value = book.getItemMeta().getPersistentDataContainer().get(bookSuccessKey, PersistentDataType.INTEGER);
        return value != null ? value : 0;
    }

    public int getBookDestroy(ItemStack book) {
        if (!isEnchantedBook(book)) return 0;
        Integer value = book.getItemMeta().getPersistentDataContainer().get(bookDestroyKey, PersistentDataType.INTEGER);
        return value != null ? value : 0;
    }

    public void setBookTier(ItemStack book, RerollTier tier) {
        if (!book.hasItemMeta()) return;
        ItemMeta meta = book.getItemMeta();
        meta.getPersistentDataContainer().set(bookTierKey, PersistentDataType.INTEGER, tier.level);
        book.setItemMeta(meta);
    }
}
