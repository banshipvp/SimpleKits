package local.simplekits;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class KitsCommand implements CommandExecutor {

    private final RankKitManager rankKitManager;

    public KitsCommand(RankKitManager rankKitManager) {
        this.rankKitManager = rankKitManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cThis command can only be used by players.");
            return true;
        }

        openGui(player);
        return true;
    }

    public void openGui(Player player) {
        int size = 9;
        Inventory inv = Bukkit.createInventory(null, size, "§b§lKits");

        int slot = 0;
        for (RankKit kit : rankKitManager.getAllKits()) {
            inv.setItem(slot++, createDisplay(player, kit));
        }

        player.openInventory(inv);
    }

    private ItemStack createDisplay(Player player, RankKit kit) {
        boolean hasAccess = rankKitManager.hasAccess(player, kit);
        boolean canClaim = rankKitManager.canClaim(player, kit);

        Material type = !hasAccess ? Material.BARRIER : (canClaim ? Material.LIME_DYE : Material.RED_DYE);
        ItemStack item = new ItemStack(type);
        ItemMeta meta = item.getItemMeta();

        meta.setDisplayName(kit.getDisplayName());
        List<String> lore = new ArrayList<>();
        lore.add("§7" + kit.getDescription());
        lore.add("§7Required: " + kit.getRequiredRank().getDisplayName());
        lore.add("§7Your Rank: " + rankKitManager.getPlayerRank(player).getDisplayName());
        lore.add("§7");

        if (!hasAccess) {
            lore.add("§cLocked - rank too low");
        } else if (!canClaim) {
            lore.add("§cOn cooldown: " + rankKitManager.getRemainingHours(player, kit) + "h");
        } else {
            lore.add("§aClick to claim");
        }

        lore.add("§7Items: §f" + kit.getItems().size());
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }
}
