package local.simplekits;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.ChatColor;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Manages Black Scrolls - ink sac items that extract a random enchant from armor gear.
 * Black scrolls have random success rates (25%-100%) and provide extracted enchants as books
 * with that success rate and 100% destroy rate.
 */
public class BlackScrollManager {

    private final JavaPlugin plugin;
    private final NamespacedKey blackScrollKey;
    private final NamespacedKey blackScrollSuccessKey;
    private final NamespacedKey bookEnchantKey;
    private final NamespacedKey bookLevelKey;
    private final NamespacedKey bookSuccessKey;
    private final NamespacedKey bookDestroyKey;
    private final NamespacedKey bookTierKey;
    private final Random random = new Random();

    public BlackScrollManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.blackScrollKey = new NamespacedKey(plugin, "black_scroll");
        this.blackScrollSuccessKey = new NamespacedKey(plugin, "black_scroll_success");
        this.bookEnchantKey = new NamespacedKey(plugin, "book_enchant");
        this.bookLevelKey = new NamespacedKey(plugin, "book_level");
        this.bookSuccessKey = new NamespacedKey(plugin, "book_success");
        this.bookDestroyKey = new NamespacedKey(plugin, "book_destroy");
        this.bookTierKey = new NamespacedKey(plugin, "book_tier");
    }

    public ItemStack createBlackScroll() {
        int successRate = 25 + random.nextInt(76); // 25-100%
        ItemStack scroll = new ItemStack(Material.INK_SAC);
        ItemMeta meta = scroll.getItemMeta();

        meta.setDisplayName("§8§l⚫ Black Scroll §l⚫");
        List<String> lore = new ArrayList<>();
        lore.add("§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
        lore.add("§7Extract an enchant from");
        lore.add("§7enchanted gear or armor");
        lore.add("§7");
        lore.add("§eSuccess Rate: §f" + successRate + "%");
        lore.add("§cDestroy Rate: §f100%");
        lore.add("§7");
        lore.add("§eRight-click gear to extract");
        lore.add("§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
        meta.setLore(lore);

        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        pdc.set(blackScrollKey, PersistentDataType.BYTE, (byte) 1);
        pdc.set(blackScrollSuccessKey, PersistentDataType.INTEGER, successRate);

        scroll.setItemMeta(meta);
        return scroll;
    }

    public boolean isBlackScroll(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        PersistentDataContainer pdc = item.getItemMeta().getPersistentDataContainer();
        if (pdc.has(blackScrollKey, PersistentDataType.BYTE)) return true;
        return looksLikeBlackScrollByLore(item);
    }

    public int getSuccessRate(ItemStack blackScroll) {
        if (!isBlackScroll(blackScroll)) return 0;
        Integer rate = blackScroll.getItemMeta().getPersistentDataContainer().get(blackScrollSuccessKey, PersistentDataType.INTEGER);
        if (rate != null && rate > 0) return rate;

        ItemMeta meta = blackScroll.getItemMeta();
        if (meta == null || meta.getLore() == null) return 0;

        Pattern successPattern = Pattern.compile("(\\d{1,3})%");
        for (String rawLine : meta.getLore()) {
            if (rawLine == null) continue;
            String line = ChatColor.stripColor(rawLine);
            if (line == null || !line.toLowerCase(Locale.ENGLISH).contains("success")) continue;

            Matcher matcher = successPattern.matcher(line);
            if (matcher.find()) {
                try {
                    int parsed = Integer.parseInt(matcher.group(1));
                    if (parsed >= 1 && parsed <= 100) return parsed;
                } catch (NumberFormatException ignored) {
                }
            }
        }

        return 0;
    }

    private boolean looksLikeBlackScrollByLore(ItemStack item) {
        if (item.getType() != Material.INK_SAC) return false;

        ItemMeta meta = item.getItemMeta();
        if (meta == null) return false;

        String displayName = meta.getDisplayName();
        if (displayName != null) {
            String clean = ChatColor.stripColor(displayName);
            if (clean != null && clean.toLowerCase(Locale.ENGLISH).contains("black scroll")) {
                return true;
            }
        }

        if (meta.getLore() == null) return false;
        boolean hasSuccess = false;
        boolean hasExtract = false;
        for (String rawLine : meta.getLore()) {
            if (rawLine == null) continue;
            String line = ChatColor.stripColor(rawLine);
            if (line == null) continue;
            String lower = line.toLowerCase(Locale.ENGLISH);
            if (lower.contains("success rate")) hasSuccess = true;
            if (lower.contains("extract") && lower.contains("enchant")) hasExtract = true;
        }

        return hasSuccess && hasExtract;
    }

    public boolean hasEnchantments(ItemStack gear) {
        return gear != null && !gear.getEnchantments().isEmpty();
    }

    public ItemStack extractRandomEnchant(ItemStack gear, int successRate) {
        if (!hasEnchantments(gear)) return null;

        // Get a random enchantment from the gear
        List<Enchantment> enchants = new ArrayList<>(gear.getEnchantments().keySet());
        if (enchants.isEmpty()) return null;

        Enchantment randomEnchant = enchants.get(random.nextInt(enchants.size()));
        int level = gear.getEnchantmentLevel(randomEnchant);

        // Remove the enchant from gear
        gear.removeEnchantment(randomEnchant);

        // Create an enchanted book with this enchant
        return createEnchantBook(randomEnchant, level, successRate);
    }

    public ItemStack createEnchantBook(Enchantment enchant, int level, int successRate) {
        ItemStack book = new ItemStack(Material.ENCHANTED_BOOK);
        ItemMeta meta = book.getItemMeta();

        String enchantName = prettify(enchant.getKey().getKey());
        meta.setDisplayName("§dEnchanted Book");

        List<String> lore = new ArrayList<>();
        lore.add("§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
        lore.add("§eEnchant: §f" + enchantName + " §8[" + toRoman(level) + "]");
        lore.add("§7");
        lore.add("§eSuccess: §f" + successRate + "%");
        lore.add("§cDestroy: §f100%");
        lore.add("§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
        meta.setLore(lore);

        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        pdc.set(bookEnchantKey, PersistentDataType.STRING, enchant.getKey().toString());
        pdc.set(bookLevelKey, PersistentDataType.INTEGER, level);
        pdc.set(bookSuccessKey, PersistentDataType.INTEGER, successRate);
        pdc.set(bookDestroyKey, PersistentDataType.INTEGER, 100); // Always 100% destroy from black scroll
        pdc.set(bookTierKey, PersistentDataType.INTEGER, 1); // Default tier

        book.setItemMeta(meta);
        return book;
    }

    public boolean isEnchantBook(ItemStack item) {
        if (item == null || item.getType() != Material.ENCHANTED_BOOK || !item.hasItemMeta()) return false;
        PersistentDataContainer pdc = item.getItemMeta().getPersistentDataContainer();
        return pdc.has(bookEnchantKey, PersistentDataType.STRING);
    }

    public String getBookEnchant(ItemStack book) {
        if (!isEnchantBook(book)) return null;
        return book.getItemMeta().getPersistentDataContainer().get(bookEnchantKey, PersistentDataType.STRING);
    }

    public int getBookLevel(ItemStack book) {
        if (!isEnchantBook(book)) return 0;
        Integer level = book.getItemMeta().getPersistentDataContainer().get(bookLevelKey, PersistentDataType.INTEGER);
        return level != null ? level : 0;
    }

    public int getBookSuccess(ItemStack book) {
        if (!isEnchantBook(book)) return 0;
        Integer success = book.getItemMeta().getPersistentDataContainer().get(bookSuccessKey, PersistentDataType.INTEGER);
        return success != null ? success : 0;
    }

    public int getBookDestroy(ItemStack book) {
        if (!isEnchantBook(book)) return 0;
        Integer destroy = book.getItemMeta().getPersistentDataContainer().get(bookDestroyKey, PersistentDataType.INTEGER);
        return destroy != null ? destroy : 0;
    }

    private String prettify(String input) {
        return input.replace('_', ' ');
    }

    private String toRoman(int num) {
        String[] thousands = {"", "M"};
        String[] hundreds = {"", "C", "CC", "CCC", "CD", "D", "DC", "DCC", "DCCC", "CM"};
        String[] tens = {"", "X", "XX", "XXX", "XL", "L", "LX", "LXX", "LXXX", "XC"};
        String[] ones = {"", "I", "II", "III", "IV", "V", "VI", "VII", "VIII", "IX"};

        return thousands[num / 1000] + hundreds[(num % 1000) / 100] + tens[(num % 100) / 10] + ones[num % 10];
    }
}
