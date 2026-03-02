package local.simplekits;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Handles Reroll Scroll and Black Scroll interactions.
 * - Right-click on a book while holding a reroll scroll to reroll success/destroy rates
 * - Right-click on gear while holding a black scroll to extract an enchant as a book
 */
public class ScrollListener implements Listener {

    private final JavaPlugin plugin;
    private final RerollScrollManager rerollScrollManager;
    private final BlackScrollManager blackScrollManager;

    public ScrollListener(JavaPlugin plugin, RerollScrollManager rerollScrollManager, BlackScrollManager blackScrollManager) {
        this.plugin = plugin;
        this.rerollScrollManager = rerollScrollManager;
        this.blackScrollManager = blackScrollManager;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;

        ItemStack cursor = event.getCursor();
        ItemStack clicked = event.getCurrentItem();
        boolean cursorIsReroll = rerollScrollManager.isRerollScroll(cursor);
        boolean cursorIsBlack = blackScrollManager.isBlackScroll(cursor);

        if (!cursorIsReroll && !cursorIsBlack) return;

        if (clicked == null || clicked.getType().isAir()) {
            debug(player, "No target item in clicked slot.");
            return;
        }

        if (cursorIsReroll) {
            if (!isBook(clicked)) {
                debug(player, "Reroll scroll target is not a book.");
                return;
            }

            event.setCancelled(true);

            if (rerollScrollManager.canReroll(cursor, clicked)) {
                rerollScrollManager.rerollBook(clicked, cursor);
                player.sendMessage("§a✓ §7Book rerolled! New rates generated.");
                consumeScroll(event, cursor);
            } else {
                player.sendMessage("§c✗ §7This scroll cannot reroll this book tier.");
                RerollScrollManager.RerollTier tier = rerollScrollManager.getRerollTier(cursor);
                if (tier == null) {
                    debug(player, "Reroll scroll tier could not be read from this item.");
                } else if (!rerollScrollManager.isEnchantedBook(clicked)) {
                    debug(player, "Target book is missing success/destroy data.");
                } else {
                    debug(player, "Tier restriction blocked reroll. Scroll tier: " + tier.name());
                }
            }
            return;
        }

        if (cursorIsBlack) {
            if (!isGear(clicked)) {
                debug(player, "Black scroll target is not recognized as gear.");
                return;
            }

            event.setCancelled(true);

            if (blackScrollManager.hasEnchantments(clicked)) {
                int successRate = blackScrollManager.getSuccessRate(cursor);
                if (successRate <= 0) {
                    debug(player, "Black scroll success rate is missing/invalid.");
                }
                ItemStack extractedBook = blackScrollManager.extractRandomEnchant(clicked, successRate);
                
                if (extractedBook != null) {
                    // Give the book to the player
                    if (player.getInventory().addItem(extractedBook).isEmpty()) {
                        player.sendMessage("§a✓ §7Enchant extracted! Book added to inventory.");
                    } else {
                        player.getWorld().dropItemNaturally(player.getLocation(), extractedBook);
                        player.sendMessage("§a✓ §7Enchant extracted! Book dropped on ground.");
                    }
                    consumeScroll(event, cursor);
                } else {
                    player.sendMessage("§c✗ §7No enchantments to extract.");
                    debug(player, "Extraction returned null after enchant check.");
                }
            } else {
                player.sendMessage("§c✗ §7This gear has no enchantments.");
                debug(player, "Target gear has zero enchantments.");
            }
        }
    }

    private boolean isBook(ItemStack item) {
        return item != null && (
                item.getType().name().contains("BOOK") || 
                rerollScrollManager.isEnchantedBook(item)
        );
    }

    private boolean isGear(ItemStack item) {
        if (item == null) return false;
        String name = item.getType().name();
        return name.contains("ARMOR") || 
               name.contains("HELMET") || 
               name.contains("CHESTPLATE") || 
               name.contains("LEGGINGS") || 
               name.contains("BOOTS") ||
               name.contains("SWORD") ||
               name.contains("PICKAXE") ||
               name.contains("AXE") ||
               name.contains("HOE") ||
               name.contains("SHOVEL");
    }

    private void consumeScroll(InventoryClickEvent event, ItemStack scroll) {
        if (scroll.getAmount() > 1) {
            scroll.setAmount(scroll.getAmount() - 1);
            event.setCursor(scroll);
        } else {
            event.setCursor(null);
        }
    }

    private void debug(Player player, String message) {
        if (!plugin.getConfig().getBoolean("debug-scrolls.enabled", false)) return;
        String permission = plugin.getConfig().getString("debug-scrolls.permission", "");
        if (permission != null && !permission.isBlank() && !player.hasPermission(permission)) return;
        player.sendMessage("§8[ScrollDebug] §7" + message);
    }
}
