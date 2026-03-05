package local.simplekits;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class KitCommand implements CommandExecutor {

    private final RankKitManager rankKitManager;
    private final KitEditorManager kitEditorManager;

    public KitCommand(RankKitManager rankKitManager, KitEditorManager kitEditorManager) {
        this.rankKitManager = rankKitManager;
        this.kitEditorManager = kitEditorManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cThis command can only be used by players.");
            return true;
        }

        if (args.length == 0) {
            player.sendMessage("§cUsage: /kit <name>");
            player.sendMessage("§cUsage: /kit create [name]");
            return true;
        }

        if (args[0].equalsIgnoreCase("create")) {
            if (!player.hasPermission("simplekits.admin")) {
                player.sendMessage("§cYou do not have permission to create kits.");
                return true;
            }
            if (args.length >= 2) {
                kitEditorManager.startEditor(player, KitEditorManager.CreationType.KIT, joinArgs(args, 1));
            } else {
                kitEditorManager.promptForName(player, KitEditorManager.CreationType.KIT);
            }
            return true;
        }

        String kitName = args[0].toLowerCase();
        RankKit kit = rankKitManager.getKit(kitName);
        if (kit == null) {
            player.sendMessage("§cKit not found: " + args[0]);
            return true;
        }

        if (!rankKitManager.hasAccess(player, kit)) {
            player.sendMessage("§cYou need rank " + kit.getRequiredRank().getDisplayName() + "§c for this kit.");
            return true;
        }

        if (!rankKitManager.canClaim(player, kit)) {
            player.sendMessage("§cKit on cooldown for §6" + rankKitManager.getRemainingHours(player, kit) + "h§c.");
            return true;
        }

        if (rankKitManager.claim(player, kit)) {
            player.sendMessage("§aClaimed " + kit.getDisplayName());
        }
        return true;
    }

    private String joinArgs(String[] args, int startIndex) {
        StringBuilder sb = new StringBuilder();
        for (int i = startIndex; i < args.length; i++) {
            if (i > startIndex) sb.append(' ');
            sb.append(args[i]);
        }
        return sb.toString();
    }
}
