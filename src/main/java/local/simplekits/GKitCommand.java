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

    public GKitCommand(KitManager kitManager, GKitGemManager gemManager) {
        this.kitManager = kitManager;
        this.gemManager = gemManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cThis command can only be used by players.");
            return true;
        }

        Player player = (Player) sender;

        if (args.length == 0) {
            player.sendMessage("§c/gkit <kitname>");
            player.sendMessage("§7Available kits:");
            for (GKit kit : kitManager.getAllKits()) {
                player.sendMessage("§7  • §6" + kit.getName());
            }
            return true;
        }

        String kitName = args[0].toLowerCase();
        GKit kit = kitManager.getKit(kitName);

        if (kit == null) {
            player.sendMessage("§cKit not found: " + kitName);
            return true;
        }
        
        // Check if player has unlocked this kit
        if (!gemManager.hasUnlockedKit(player.getUniqueId(), kitName)) {
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

        // Claim the kit
        gemManager.unlockKit(player, kitName);
        return true;
    }
}
