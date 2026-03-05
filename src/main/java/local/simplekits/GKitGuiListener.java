package local.simplekits;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
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
        // Check if this is a gkits GUI
        if (event.getView().getTitle() == null || !event.getView().getTitle().contains("GKits")) {
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
                handleKitSelection(player, kit);
                break;
            }
        }
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
