package local.simplekits;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;

public class KitEditorListener implements Listener {

    private final KitEditorManager editorManager;

    public KitEditorListener(KitEditorManager editorManager) {
        this.editorManager = editorManager;
    }

    @EventHandler
    public void onEditorClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        String title = event.getView().getTitle();
        if (title == null || !title.startsWith("§8")) return;

        boolean handled = editorManager.handleInventoryClick(
                player,
                event.getClickedInventory(),
                title,
                event.getRawSlot(),
                event.getClick(),
                event.getCurrentItem()
        );
        if (handled) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        if (!editorManager.isAwaitingName(player.getUniqueId())) return;
        event.setCancelled(true);
        editorManager.handleChatName(player, event.getMessage());
    }
}
