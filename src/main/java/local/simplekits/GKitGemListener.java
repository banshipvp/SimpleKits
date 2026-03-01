package local.simplekits;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

/**
 * Listener for gkit gem interactions
 */
public class GKitGemListener implements Listener {

    private final GKitGemManager gemManager;
    private final KitManager kitManager;

    public GKitGemListener(GKitGemManager gemManager, KitManager kitManager) {
        this.gemManager = gemManager;
        this.kitManager = kitManager;
    }

    @EventHandler
    public void onPlayerRightClick(PlayerInteractEvent event) {
        ItemStack item = event.getItem();
        
        if (item == null) return;
        
        Player player = event.getPlayer();
        
        // Check if it's a gkit gem
        if (gemManager.isGKitGem(item)) {
            event.setCancelled(true);
            
            String kitName = gemManager.extractKitNameFromGem(item);
            
            if (kitName == null) {
                player.sendMessage("§cCould not identify kit from gem.");
                return;
            }
            
            // Check if player can claim the kit
            if (!kitManager.canClaimKit(player.getUniqueId(), kitName)) {
                int remainingHours = kitManager.getRemainingCooldownHours(player.getUniqueId(), kitName);
                player.sendMessage("§cKit is on cooldown for §6" + remainingHours + " more hours§c.");
                return;
            }
            
            // Unlock the kit
            gemManager.unlockKit(player, kitName);
            
            // Remove gem from inventory
            if (item.getAmount() > 1) {
                item.setAmount(item.getAmount() - 1);
            } else {
                player.getInventory().remove(item);
            }
        }
    }
}
