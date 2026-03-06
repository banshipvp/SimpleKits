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
    private final KitEditorManager kitEditorManager;

    public KitsCommand(RankKitManager rankKitManager, KitEditorManager kitEditorManager) {
        this.rankKitManager = rankKitManager;
        this.kitEditorManager = kitEditorManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cThis command can only be used by players.");
            return true;
        }

        if (args.length >= 1) {
            if (args[0].equalsIgnoreCase("create")) {
                if (!player.hasPermission("simplekits.admin")) {
                    player.sendMessage("§cYou do not have permission to create kits.");
                    return true;
                }
                if (args.length >= 2) {
                    StringBuilder name = new StringBuilder();
                    for (int i = 1; i < args.length; i++) {
                        if (i > 1) name.append(' ');
                        name.append(args[i]);
                    }
                    kitEditorManager.startEditor(player, KitEditorManager.CreationType.KIT, name.toString());
                } else {
                    kitEditorManager.promptForName(player, KitEditorManager.CreationType.KIT);
                }
                return true;
            }

            claimByName(player, args[0]);
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

    private void claimByName(Player player, String kitArg) {
        String kitName = kitArg.toLowerCase();
        RankKit kit = rankKitManager.getKit(kitName);
        if (kit == null) {
            player.sendMessage("§cKit not found: " + kitArg);
            return;
        }

        if (!rankKitManager.hasAccess(player, kit)) {
            player.sendMessage("§cYou need rank " + kit.getRequiredRank().getDisplayName() + "§c for this kit.");
            return;
        }

        if (!rankKitManager.canClaim(player, kit)) {
            player.sendMessage("§cKit on cooldown for §6" + rankKitManager.getRemainingHours(player, kit) + "h§c.");
            return;
        }

        if (rankKitManager.claim(player, kit)) {
            player.sendMessage("§aClaimed " + kit.getDisplayName());
        }
    }
}
