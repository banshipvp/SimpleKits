package local.simplekits;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * /gkit <kitname> command - Claim a specific kit
 */
public class GKitCommand implements CommandExecutor {

    private final KitManager kitManager;
    private final GKitGemManager gemManager;
    private final KitEditorManager editorManager;
    private final GKitsCommand gKitsCommand;

    public GKitCommand(KitManager kitManager, GKitGemManager gemManager, KitEditorManager editorManager, GKitsCommand gKitsCommand) {
        this.kitManager = kitManager;
        this.gemManager = gemManager;
        this.editorManager = editorManager;
        this.gKitsCommand = gKitsCommand;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cThis command can only be used by players.");
            return true;
        }

        Player player = (Player) sender;

        if (args.length > 0 && args[0].equalsIgnoreCase("create")) {
            if (!player.hasPermission("simplekits.admin")) {
                player.sendMessage("§cYou do not have permission to create gkits.");
                return true;
            }
            if (args.length >= 2) {
                editorManager.startEditor(player, KitEditorManager.CreationType.GKIT, joinArgs(args, 1));
            } else {
                editorManager.promptForName(player, KitEditorManager.CreationType.GKIT);
            }
            return true;
        }

        if (args.length == 0) {
            gKitsCommand.openKitsGui(player);
            return true;
        }

        String kitName = args[0].toLowerCase();
        GKit kit = kitManager.getKit(kitName);

        if (kit == null) {
            player.sendMessage("§cKit not found: " + kitName);
            return true;
        }
        
        // Check if player has unlocked this kit
        if (!player.hasPermission("simplekits.admin") && !gemManager.hasUnlockedKit(player.getUniqueId(), kitName)) {
            player.sendMessage("§c§lKit Locked!");
            player.sendMessage("§7You must use a §b" + kit.getDisplayName() + " Gem §7to unlock this kit first.");
            player.sendMessage("§7Gems can be obtained from crates, events, or purchases.");
            return true;
        }

        // Check if player can claim kit (cooldown check)
        if (!kitManager.canClaimKit(player.getUniqueId(), kitName)) {
            int remainingHours = kitManager.getRemainingCooldownHours(player.getUniqueId(), kitName);
            player.sendMessage("§cKit is on cooldown for §6" + remainingHours + " more hours§c.");
            return true;
        }

        boolean ok = gemManager.giveKitItems(player, kit, !player.hasPermission("simplekits.admin"));
        if (!ok) {
            player.sendMessage("§cCould not claim this gkit (check inventory space).");
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
