package local.simplekits;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

/**
 * Listener for gkits GUI interactions
 */
public class GKitGuiListener implements Listener {

    private final KitManager kitManager;
    private final GKitGemManager gemManager;

    public GKitGuiListener(KitManager kitManager, GKitGemManager gemManager) {
        this.kitManager = kitManager;
        this.gemManager = gemManager;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        String title = event.getView().getTitle();
        if (title != null && title.startsWith("§8GKit Preview:")) {
            event.setCancelled(true);
            if (!(event.getWhoClicked() instanceof Player player)) return;
            if (event.getCurrentItem() == null || !event.getCurrentItem().hasItemMeta()) return;
            String name = event.getCurrentItem().getItemMeta().getDisplayName();
            if (name != null && name.contains("Back to GKits")) {
                player.performCommand("gkits");
            }
            return;
        }

        // Check if this is a gkits GUI
        if (title == null || !title.contains("GKits")) {
            return;
        }
        
        event.setCancelled(true);
        
        if (!(event.getWhoClicked() instanceof Player)) return;
        
        Player player = (Player) event.getWhoClicked();
        ItemStack clicked = event.getCurrentItem();
        
        if (clicked == null || !clicked.hasItemMeta()) return;
        
        String displayName = clicked.getItemMeta().getDisplayName();
        
        // Find which kit was clicked
        for (GKit kit : kitManager.getAllKits()) {
            if (displayName.contains(kit.getDisplayName())) {
                if (event.getClick().isRightClick()) {
                    openKitPreview(player, kit);
                } else {
                    handleKitSelection(player, kit);
                }
                break;
            }
        }
    }

    private void openKitPreview(Player player, GKit kit) {
        Inventory preview = Bukkit.createInventory(null, 54, "§8GKit Preview: " + kit.getDisplayName());
        java.util.List<ItemStack> items = gemManager.createPreviewSet(kit);

        int slot = 0;
        for (ItemStack item : items) {
            if (item == null || item.getType() == Material.AIR) continue;
            if (slot >= 45) break;
            preview.setItem(slot++, item.clone());
        }

        preview.setItem(49, named(Material.ARROW, "§eBack to GKits", java.util.List.of("§7Return to the GKits menu")));
        player.openInventory(preview);
    }

    private ItemStack named(Material material, String name, java.util.List<String> lore) {
        ItemStack item = new ItemStack(material);
        var meta = item.getItemMeta();
        meta.setDisplayName(name);
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }
    
    /**
     * Handle kit selection from GUI
     */
    private void handleKitSelection(Player player, GKit kit) {
        // Check if player has unlocked this kit
        if (!player.hasPermission("simplekits.admin") && !gemManager.hasUnlockedKit(player.getUniqueId(), kit.getName())) {
            player.sendMessage("§c§lKit Locked!");
            player.sendMessage("§7You must use a §b" + kit.getDisplayName() + " Gem §7to unlock this kit first.");
            player.sendMessage("§7Gems can be obtained from crates, events, or purchases.");
            player.closeInventory();
            return;
        }
        
        // Check if player can claim kit (cooldown)
        if (!kitManager.canClaimKit(player.getUniqueId(), kit.getName())) {
            int remainingHours = kitManager.getRemainingCooldownHours(player.getUniqueId(), kit.getName());
            player.sendMessage("§cKit is on cooldown for §6" + remainingHours + " more hours§c.");
            player.closeInventory();
            return;
        }
        
        // Claim the kit
        player.closeInventory();
        gemManager.giveKitItems(player, kit);
    }
}
