package local.simplekits;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;

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

        if (title != null && title.contains("GKit Preview")) {
            event.setCancelled(true);
            if (event.getCurrentItem() != null && event.getCurrentItem().getType() == Material.ARROW) {
                event.getWhoClicked().closeInventory();
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
                if (event.getClick() == ClickType.RIGHT || event.getClick() == ClickType.SHIFT_RIGHT) {
                    openKitPreview(player, kit);
                } else if (event.getClick() == ClickType.LEFT || event.getClick() == ClickType.SHIFT_LEFT) {
                    handleKitSelection(player, kit);
                }
                break;
            }
        }
    }

    private void openKitPreview(Player player, GKit kit) {
        Inventory preview = Bukkit.createInventory(null, 27, "§9§lGKit Preview: §f" + kit.getName());

        for (int option = 0; option < 3; option++) {
            List<ItemStack> set = gemManager.createPreviewSet(kit);
            int baseRow = option * 9;

            ItemStack label = new ItemStack(Material.PAPER);
            ItemMeta labelMeta = label.getItemMeta();
            if (labelMeta != null) {
                labelMeta.setDisplayName("§eOption #" + (option + 1));
                label.setItemMeta(labelMeta);
            }
            preview.setItem(baseRow, label);

            for (int index = 0; index < Math.min(4, set.size()); index++) {
                preview.setItem(baseRow + 1 + index, set.get(index));
            }
        }

        ItemStack close = new ItemStack(Material.ARROW);
        ItemMeta closeMeta = close.getItemMeta();
        if (closeMeta != null) {
            closeMeta.setDisplayName("§cClose Preview");
            close.setItemMeta(closeMeta);
        }
        preview.setItem(26, close);

        player.openInventory(preview);
    }
    
    /**
     * Handle kit selection from GUI
     */
    private void handleKitSelection(Player player, GKit kit) {
        // Check if player has unlocked this kit
        if (!gemManager.hasUnlockedKit(player.getUniqueId(), kit.getName())) {
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
        
        // Claim the kit (give items)
        player.closeInventory();
        gemManager.giveKitItems(player, kit);
    }
}
