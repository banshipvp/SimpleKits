package local.simplekits;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.UUID;

public class GKitLockCommand implements CommandExecutor {

    private final GKitGemManager gemManager;

    public GKitLockCommand(GKitGemManager gemManager) {
        this.gemManager = gemManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (sender instanceof Player player) {
            if (!player.isOp() && !player.hasPermission("simplekits.admin")) {
                player.sendMessage("§cNo permission. Required: §esimplekits.admin§c or OP.");
                return true;
            }
        }

        if (args.length == 0) {
            sender.sendMessage("§6/gkitlock all");
            sender.sendMessage("§6/gkitlock player <player>");
            return true;
        }

        if (args[0].equalsIgnoreCase("all")) {
            gemManager.lockAllPlayers();
            sender.sendMessage("§aLocked all gkits for all players.");
            Bukkit.broadcastMessage("§eAll gkit unlocks have been reset by staff.");
            return true;
        }

        if (args[0].equalsIgnoreCase("player")) {
            if (args.length < 2) {
                sender.sendMessage("§cUsage: /gkitlock player <player>");
                return true;
            }

            Player target = Bukkit.getPlayerExact(args[1]);
            if (target == null) {
                sender.sendMessage("§cPlayer not found.");
                return true;
            }

            UUID playerId = target.getUniqueId();
            gemManager.lockPlayer(playerId);
            sender.sendMessage("§aLocked all gkits for " + target.getName());
            target.sendMessage("§cYour gkit unlocks were reset by staff.");
            return true;
        }

        sender.sendMessage("§cUnknown subcommand.");
        return true;
    }
}
