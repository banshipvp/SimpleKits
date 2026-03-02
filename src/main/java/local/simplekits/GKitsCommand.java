package local.simplekits;

import org.bukkit.Bukkit;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.Material;

import java.util.ArrayList;
import java.util.List;

/**
 * /gkits command - Open GUI to view all available kits
 */
public class GKitsCommand implements CommandExecutor {

    private final SimpleKitsPlugin plugin;
    private final KitManager kitManager;

    public GKitsCommand(SimpleKitsPlugin plugin, KitManager kitManager) {
        this.plugin = plugin;
        this.kitManager = kitManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cThis command can only be used by players.");
            return true;
        }

        Player player = (Player) sender;
        openKitsGui(player);
        return true;
    }

    /**
     * Open the gkits GUI inventory
     */
    private void openKitsGui(Player player) {
        int kitCount = kitManager.getKitCount();
        int rows = (int) Math.ceil(kitCount / 9.0);
        int inventorySize = rows * 9;

        Inventory gui = Bukkit.createInventory(null, inventorySize, "§6§lGKits");

        int slot = 0;
        for (GKit kit : kitManager.getAllKits()) {
            ItemStack kitDisplay = createKitDisplayItem(player, kit);
            gui.setItem(slot, kitDisplay);
            slot++;
        }

        player.openInventory(gui);
    }

    /**
     * Create a display item for a kit in the GUI
     */
    private ItemStack createKitDisplayItem(Player player, GKit kit) {
        boolean unlocked = plugin.getGKitGemManager().hasUnlockedKit(player.getUniqueId(), kit.getName());
        boolean canClaim = unlocked && kitManager.canClaimKit(player.getUniqueId(), kit.getName());

        ItemStack display;
        if (!unlocked) {
            display = new ItemStack(Material.BARRIER);
        } else {
            display = new ItemStack(canClaim ? Material.LIME_DYE : Material.RED_DYE);
        }
        ItemMeta meta = display.getItemMeta();

        String status;
        if (!unlocked) {
            status = "§c✗ LOCKED";
        } else if (canClaim) {
            status = "§a✓ AVAILABLE";
        } else {
            status = "§c✗ COOLDOWN";
        }
        meta.setDisplayName("§6" + kit.getDisplayName() + " " + status);

        List<String> lore = new ArrayList<>();
        lore.add("§7" + kit.getDescription());
        lore.add("§7");
        
        if (!unlocked) {
            lore.add("§cKit Locked");
            lore.add("§7Use a matching §bGKit Gem §7to unlock.");
        } else if (canClaim) {
            lore.add("§aLeft-click to claim kit");
            lore.add("§bRight-click to preview options");
            lore.add("§aCooldown: §a1 day§a");
        } else {
            int remainingHours = kitManager.getRemainingCooldownHours(player.getUniqueId(), kit.getName());
            lore.add("§cNext available in:");
            lore.add("§c" + remainingHours + " hours");
            lore.add("§bRight-click to preview options");
        }
        
        lore.add("§7");
        lore.add("§7Items: §a" + kit.getItems().length);

        meta.setLore(lore);
        display.setItemMeta(meta);

        return display;
    }
}
