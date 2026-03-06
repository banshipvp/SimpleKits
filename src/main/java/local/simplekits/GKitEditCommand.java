package local.simplekits;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class GKitEditCommand implements CommandExecutor {

    private final KitEditorManager editorManager;

    public GKitEditCommand(KitEditorManager editorManager) {
        this.editorManager = editorManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cThis command can only be used by players.");
            return true;
        }

        if (!player.hasPermission("simplekits.admin")) {
            player.sendMessage("§cYou do not have permission to edit gkits.");
            return true;
        }

        if (args.length == 0) {
            player.sendMessage("§cUsage: /gkitedit <name>");
            return true;
        }

        editorManager.startEditGKit(player, String.join(" ", args));
        return true;
    }
}
