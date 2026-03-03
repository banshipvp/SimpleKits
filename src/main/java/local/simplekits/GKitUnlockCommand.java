package local.simplekits;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.UUID;

public class GKitUnlockCommand implements CommandExecutor {

    private final GKitGemManager gemManager;
    private final KitManager kitManager;

    public GKitUnlockCommand(GKitGemManager gemManager, KitManager kitManager) {
        this.gemManager = gemManager;
        this.kitManager = kitManager;
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
            sender.sendMessage("§6/gkitunlock player <player> <kit|all>");
            sender.sendMessage("§6/gkitunlock all <kit>");
            return true;
        }

        if (args[0].equalsIgnoreCase("all")) {
            if (args.length < 2) {
                sender.sendMessage("§cUsage: /gkitunlock all <kit>");
                return true;
            }
            String kitName = args[1].toLowerCase();
            GKit kit = kitManager.getKit(kitName);
            if (kit == null) {
                sender.sendMessage("§cKit not found: " + kitName);
                return true;
            }
            for (Player p : Bukkit.getOnlinePlayers()) {
                gemManager.unlockKit(p, kit.getName());
            }
            Bukkit.broadcastMessage("§eStaff unlocked kit §f" + kit.getDisplayName() + " §efor all online players.");
            return true;
        }

        if (args[0].equalsIgnoreCase("player")) {
            if (args.length < 3) {
                sender.sendMessage("§cUsage: /gkitunlock player <player> <kit|all>");
                return true;
            }

            Player target = Bukkit.getPlayerExact(args[1]);
            if (target == null) {
                sender.sendMessage("§cPlayer not found.");
                return true;
            }

            String kitArg = args[2].toLowerCase();
            if (kitArg.equals("all")) {
                for (GKit k : kitManager.getAllKits()) {
                    gemManager.unlockKit(target, k.getName());
                }
                sender.sendMessage("§aUnlocked all gkits for " + target.getName());
                target.sendMessage("§aA staff member unlocked all gkits for you.");
                return true;
            }

            GKit kit = kitManager.getKit(kitArg);
            if (kit == null) {
                sender.sendMessage("§cKit not found: " + kitArg);
                return true;
            }

            gemManager.unlockKit(target, kit.getName());
            sender.sendMessage("§aUnlocked gkit §f" + kit.getDisplayName() + " §afor " + target.getName());
            target.sendMessage("§aA staff member unlocked the §6" + kit.getDisplayName() + " §afor you.");
            return true;
        }

        sender.sendMessage("§cUnknown subcommand.");
        return true;
    }
}
