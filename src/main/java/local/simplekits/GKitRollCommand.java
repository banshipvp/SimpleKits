package local.simplekits;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class GKitRollCommand implements CommandExecutor {

    private final KitManager kitManager;
    private final GKitGemManager gemManager;

    public GKitRollCommand(KitManager kitManager, GKitGemManager gemManager) {
        this.kitManager = kitManager;
        this.gemManager = gemManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cThis command can only be used by players.");
            return true;
        }

        if (!canUseRoll(player)) {
            player.sendMessage("§cYou do not have permission to use /gkitroll.");
            return true;
        }

        if (args.length == 0) {
            player.sendMessage("§cUsage: /gkitroll <name>");
            return true;
        }

        String name = args[0].toLowerCase();
        GKit kit = kitManager.getKit(name);
        if (kit == null) {
            player.sendMessage("§cGKit not found: " + args[0]);
            return true;
        }

        boolean adminBypass = player.hasPermission("simplekits.admin");
        if (!adminBypass) {
            if (!gemManager.hasUnlockedKit(player.getUniqueId(), name)) {
                player.sendMessage("§cYou must unlock this gkit first.");
                return true;
            }
            if (!kitManager.canClaimKit(player.getUniqueId(), name)) {
                int remaining = kitManager.getRemainingCooldownHours(player.getUniqueId(), name);
                player.sendMessage("§cKit is on cooldown for §6" + remaining + "h§c.");
                return true;
            }
        }

        boolean ok = gemManager.giveKitItems(player, kit, !adminBypass);
        if (!ok) {
            player.sendMessage("§cCould not roll this gkit (check inventory space).");
            return true;
        }

        player.sendMessage("§aRolled gkit: §f" + kit.getDisplayName());
        return true;
    }

    private boolean canUseRoll(Player player) {
        return player.hasPermission("simplekits.gkitroll")
                || player.hasPermission("simplekits.admin")
                || player.hasPermission("group.owner")
                || player.hasPermission("group.admin")
                || player.hasPermission("group.dev")
                || player.isOp();
    }
}
