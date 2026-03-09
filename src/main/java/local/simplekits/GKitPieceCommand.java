package local.simplekits;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.List;

/**
 * /gkitpiece give <player> <kit>
 * Gives one random gear piece from the specified GKit to the player.
 * Used by envoys / admin commands — does NOT require kit unlock.
 */
public class GKitPieceCommand implements CommandExecutor {

    private final KitManager kitManager;
    private final GKitGemManager gemManager;

    public GKitPieceCommand(KitManager kitManager, GKitGemManager gemManager) {
        this.kitManager = kitManager;
        this.gemManager = gemManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // Only allow console / permission holders
        if (!(sender instanceof Player player && player.hasPermission("simplekits.admin"))
                && !(sender.equals(Bukkit.getConsoleSender()))) {
            // Check if it's an OP player running as console
            if (sender instanceof Player p && !p.hasPermission("simplekits.admin")) {
                sender.sendMessage("§cYou don't have permission to use this command.");
                return true;
            }
        }

        if (args.length < 3 || !args[0].equalsIgnoreCase("give")) {
            sender.sendMessage("§eUsage: /gkitpiece give <player> <kit>");
            return true;
        }

        Player target = Bukkit.getPlayerExact(args[1]);
        if (target == null) {
            sender.sendMessage("§cPlayer §e" + args[1] + " §cis not online.");
            return true;
        }

        String kitName = args[2].toLowerCase();
        GKit kit = kitManager.getKit(kitName);
        if (kit == null) {
            sender.sendMessage("§cGKit not found: §e" + args[2]);
            sender.sendMessage("§7Available: " + kitManager.getAllKits().stream()
                    .map(GKit::getName).toList());
            return true;
        }

        ItemStack piece = gemManager.giveRandomPiece(target, kit);
        if (piece == null) {
            sender.sendMessage("§cCould not generate a piece from kit §e" + kit.getDisplayName() + "§c.");
            return true;
        }

        sender.sendMessage("§aGave a §6" + kit.getDisplayName()
                + " §aGKit piece (§f" + piece.getType().name() + "§a) to §e" + target.getName() + "§a.");
        return true;
    }
}
