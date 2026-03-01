package local.simplekits;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

public class KitsGuiListener implements Listener {

    private final RankKitManager rankKitManager;
    private final KitsCommand kitsCommand;

    public KitsGuiListener(RankKitManager rankKitManager, KitsCommand kitsCommand) {
        this.rankKitManager = rankKitManager;
        this.kitsCommand = kitsCommand;
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (event.getView().getTitle() == null || !event.getView().getTitle().contains("§b§lKits")) {
            return;
        }

        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player)) return;

        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || !clicked.hasItemMeta()) return;

        String display = clicked.getItemMeta().getDisplayName();
        RankKit selected = null;
        for (RankKit kit : rankKitManager.getAllKits()) {
            if (display.equals(kit.getDisplayName())) {
                selected = kit;
                break;
            }
        }

        if (selected == null) return;

        if (!rankKitManager.hasAccess(player, selected)) {
            player.sendMessage("§cYou need rank " + selected.getRequiredRank().getDisplayName() + "§c for this kit.");
            return;
        }

        if (!rankKitManager.canClaim(player, selected)) {
            player.sendMessage("§cKit on cooldown for §6" + rankKitManager.getRemainingHours(player, selected) + "h§c.");
            return;
        }

        if (rankKitManager.claim(player, selected)) {
            player.sendMessage("§aClaimed " + selected.getDisplayName());
            kitsCommand.openGui(player);
        }
    }
}
