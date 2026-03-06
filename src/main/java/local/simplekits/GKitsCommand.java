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
    private final KitEditorManager editorManager;

    public GKitsCommand(SimpleKitsPlugin plugin, KitManager kitManager, KitEditorManager editorManager) {
        this.plugin = plugin;
        this.kitManager = kitManager;
        this.editorManager = editorManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cThis command can only be used by players.");
            return true;
        }

        Player player = (Player) sender;

        if (args.length >= 1) {
            if (args[0].equalsIgnoreCase("create")) {
                if (!player.hasPermission("simplekits.admin")) {
                    player.sendMessage("§cYou do not have permission to create gkits.");
                    return true;
                }
                if (args.length >= 2) {
                    StringBuilder name = new StringBuilder();
                    for (int i = 1; i < args.length; i++) {
                        if (i > 1) name.append(' ');
                        name.append(args[i]);
                    }
                    editorManager.startEditor(player, KitEditorManager.CreationType.GKIT, name.toString());
                } else {
                    editorManager.promptForName(player, KitEditorManager.CreationType.GKIT);
                }
                return true;
            }

            claimByName(player, args[0]);
            return true;
        }

        openKitsGui(player);
        return true;
    }

    /**
     * Open the gkits GUI inventory
     */
    public void openKitsGui(Player player) {
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
            lore.add("§eRight-click to preview");
            lore.add("§aCooldown: §a1 day§a");
        } else {
            int remainingHours = kitManager.getRemainingCooldownHours(player.getUniqueId(), kit.getName());
            lore.add("§cNext available in:");
            lore.add("§c" + remainingHours + " hours");
            lore.add("§eRight-click to preview");
        }
        
        lore.add("§7");
        lore.add("§7Items: §a" + kit.getItems().length);

        meta.setLore(lore);
        display.setItemMeta(meta);

        return display;
    }

    private void claimByName(Player player, String rawKitName) {
        String kitName = rawKitName.toLowerCase();
        GKit kit = kitManager.getKit(kitName);
        if (kit == null) {
            player.sendMessage("§cKit not found: " + rawKitName);
            return;
        }

        if (!player.hasPermission("simplekits.admin") && !plugin.getGKitGemManager().hasUnlockedKit(player.getUniqueId(), kitName)) {
            player.sendMessage("§c§lKit Locked!");
            player.sendMessage("§7You must use a §b" + kit.getDisplayName() + " Gem §7to unlock this kit first.");
            return;
        }

        if (!kitManager.canClaimKit(player.getUniqueId(), kitName)) {
            int remainingHours = kitManager.getRemainingCooldownHours(player.getUniqueId(), kitName);
            player.sendMessage("§cKit is on cooldown for §6" + remainingHours + " more hours§c.");
            return;
        }

        boolean ok = plugin.getGKitGemManager().giveKitItems(player, kit, !player.hasPermission("simplekits.admin"));
        if (!ok) {
            player.sendMessage("§cCould not claim this gkit (check inventory space).");
        }
    }
}
