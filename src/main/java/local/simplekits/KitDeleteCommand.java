package local.simplekits;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class KitDeleteCommand implements CommandExecutor {

    private final RankKitManager rankKitManager;
    private final KitManager kitManager;

    public KitDeleteCommand(RankKitManager rankKitManager, KitManager kitManager) {
        this.rankKitManager = rankKitManager;
        this.kitManager = kitManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("simplekits.admin")) {
            sender.sendMessage("§cYou do not have permission.");
            return true;
        }

        if (args.length == 0) {
            sender.sendMessage("§cUsage: /kitdelete <name>");
            sender.sendMessage("§cUsage: /kitdelete kit <name>");
            sender.sendMessage("§cUsage: /kitdelete gkit <name>");
            return true;
        }

        if (args.length == 1) {
            String name = args[0].toLowerCase();
            boolean removedKit = rankKitManager.unregisterKit(name);
            boolean removedGkit = kitManager.unregisterKit(name);
            if (!removedKit && !removedGkit) {
                sender.sendMessage("§cNo kit/gkit found with name: " + args[0]);
                return true;
            }
            sender.sendMessage("§aDeleted " + (removedKit ? "kit " : "") + (removedKit && removedGkit ? "and " : "") + (removedGkit ? "gkit " : "") + "§f" + args[0]);
            return true;
        }

        String type = args[0].toLowerCase();
        String name = args[1].toLowerCase();
        if (type.equals("kit")) {
            if (rankKitManager.unregisterKit(name)) {
                sender.sendMessage("§aDeleted kit §f" + name);
            } else {
                sender.sendMessage("§cKit not found: " + name);
            }
            return true;
        }
        if (type.equals("gkit")) {
            if (kitManager.unregisterKit(name)) {
                sender.sendMessage("§aDeleted gkit §f" + name);
            } else {
                sender.sendMessage("§cGKit not found: " + name);
            }
            return true;
        }

        sender.sendMessage("§cFirst argument must be 'kit' or 'gkit'.");
        return true;
    }
}
