package local.simplekits;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

/**
 * /gkitunlock — admin command to grant gkit unlocks.
 *
 * Usage:
 *   /gkitunlock player <player> <kit|all>   — unlock a specific kit (or all kits) for a player
 *   /gkitunlock all <kit>                   — unlock a specific kit for every online player
 */
public class GKitUnlockCommand implements CommandExecutor, TabCompleter {

    private final KitManager kitManager;
    private final GKitGemManager gemManager;

    public GKitUnlockCommand(KitManager kitManager, GKitGemManager gemManager) {
        this.kitManager = kitManager;
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

        if (args.length < 2) {
            sendHelp(sender);
            return true;
        }

        String sub = args[0].toLowerCase();

        switch (sub) {
            case "player" -> handlePlayer(sender, args);
            case "all"    -> handleAll(sender, args);
            default       -> sendHelp(sender);
        }
        return true;
    }

    // /gkitunlock player <player> <kit|all>
    private void handlePlayer(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage("§cUsage: /gkitunlock player <player> <kit|all>");
            return;
        }

        Player target = Bukkit.getPlayerExact(args[1]);
        if (target == null) {
            sender.sendMessage("§cPlayer §e" + args[1] + " §cis not online.");
            return;
        }

        String kitArg = args[2].toLowerCase();
        UUID uuid = target.getUniqueId();

        if (kitArg.equals("all")) {
            int count = gemManager.unlockAllKitsSilent(uuid);
            sender.sendMessage("§aUnlocked §e" + count + " §akit(s) for §e" + target.getName() + "§a.");
            target.sendMessage("§aAn admin has unlocked §eall gkits §afor you!");
        } else {
            if (kitManager.getKit(kitArg) == null) {
                sender.sendMessage("§cUnknown kit: §e" + kitArg);
                sender.sendMessage("§7Available: " + getKitListString());
                return;
            }
            boolean wasNew = gemManager.unlockKitSilent(uuid, kitArg);
            if (wasNew) {
                sender.sendMessage("§aUnlocked §e" + kitArg + " §afor §e" + target.getName() + "§a.");
                target.sendMessage("§aAn admin has unlocked the §e" + kitArg + " §agkit for you!");
            } else {
                sender.sendMessage("§e" + target.getName() + " §aalready has §e" + kitArg + " §aunlocked.");
            }
        }
    }

    // /gkitunlock all <kit>
    private void handleAll(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage("§cUsage: /gkitunlock all <kit>");
            return;
        }

        String kitArg = args[1].toLowerCase();
        if (kitManager.getKit(kitArg) == null) {
            sender.sendMessage("§cUnknown kit: §e" + kitArg);
            sender.sendMessage("§7Available: " + getKitListString());
            return;
        }

        Collection<? extends Player> onlinePlayers = Bukkit.getOnlinePlayers();
        int unlocked = 0;
        for (Player p : onlinePlayers) {
            if (gemManager.unlockKitSilent(p.getUniqueId(), kitArg)) {
                p.sendMessage("§aAn admin has unlocked the §e" + kitArg + " §agkit for you!");
                unlocked++;
            }
        }

        sender.sendMessage("§aUnlocked §e" + kitArg + " §afor §e" + unlocked + "§a/"
                + onlinePlayers.size() + " §aonline player(s) (others already had it).");
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage("§6/gkitunlock player <player> <kit|all> §7- Unlock a gkit for a player");
        sender.sendMessage("§6/gkitunlock all <kit> §7- Unlock a kit for all online players");
        sender.sendMessage("§7Available kits: " + getKitListString());
    }

    private String getKitListString() {
        List<String> names = new ArrayList<>();
        for (GKit kit : kitManager.getAllKits()) {
            names.add(kit.getName());
        }
        return String.join("§7, §e", names);
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.isOp() && !sender.hasPermission("simplekits.admin")) {
            return List.of();
        }

        if (args.length == 1) {
            return List.of("player", "all");
        }

        if (args.length == 2) {
            if (args[0].equalsIgnoreCase("player")) {
                // suggest online player names
                List<String> names = new ArrayList<>();
                for (Player p : Bukkit.getOnlinePlayers()) names.add(p.getName());
                return filter(names, args[1]);
            }
            if (args[0].equalsIgnoreCase("all")) {
                return filter(getKitNames(), args[1]);
            }
        }

        if (args.length == 3 && args[0].equalsIgnoreCase("player")) {
            List<String> kits = new ArrayList<>(getKitNames());
            kits.add("all");
            return filter(kits, args[2]);
        }

        return List.of();
    }

    private List<String> getKitNames() {
        List<String> names = new ArrayList<>();
        for (GKit kit : kitManager.getAllKits()) names.add(kit.getName());
        return names;
    }

    private List<String> filter(List<String> options, String partial) {
        String lower = partial.toLowerCase();
        List<String> result = new ArrayList<>();
        for (String s : options) {
            if (s.toLowerCase().startsWith(lower)) result.add(s);
        }
        return result;
    }
}
