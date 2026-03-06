package local.simplekits;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;

public class SimpleKitsTabCompleter implements TabCompleter {

    private final KitManager kitManager;
    private final RankKitManager rankKitManager;

    public SimpleKitsTabCompleter(KitManager kitManager, RankKitManager rankKitManager) {
        this.kitManager = kitManager;
        this.rankKitManager = rankKitManager;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        String cmd = command.getName().toLowerCase(Locale.ROOT);

        return switch (cmd) {
            case "gkit", "gkits" -> completeGkit(sender, args);
            case "gkitroll" -> completeGkitRoll(sender, args);
            case "kit", "kits" -> completeKit(sender, args);
            case "gkitgem" -> completeGkitGem(sender, args);
            case "gkitlock" -> completeGkitLock(sender, args);
            case "gkitedit", "gkitcreate", "kitcreate" -> completeAdminKitNames(sender, args);
            case "kitdelete" -> completeKitDelete(sender, args);
            case "gkitdelete" -> completeGkitDelete(sender, args);
            case "spawner" -> completeSpawner(args);
            default -> List.of();
        };
    }

    private List<String> completeGkit(CommandSender sender, String[] args) {
        if (args.length == 1) {
            List<String> out = new ArrayList<>();
            for (GKit kit : kitManager.getAllKits()) out.add(kit.getName());
            if (hasAdmin(sender)) out.add("create");
            return filter(out, args[0]);
        }
        if (args.length >= 2 && args[0].equalsIgnoreCase("create") && hasAdmin(sender)) {
            return List.of("<name>");
        }
        return List.of();
    }

    private List<String> completeGkitRoll(CommandSender sender, String[] args) {
        if (!canUseGkitRoll(sender)) return List.of();
        if (args.length == 1) {
            List<String> out = new ArrayList<>();
            for (GKit kit : kitManager.getAllKits()) out.add(kit.getName());
            return filter(out, args[0]);
        }
        return List.of();
    }

    private List<String> completeKit(CommandSender sender, String[] args) {
        if (args.length == 1) {
            List<String> out = new ArrayList<>();
            for (RankKit kit : rankKitManager.getAllKits()) out.add(kit.getName());
            if (hasAdmin(sender)) out.add("create");
            return filter(out, args[0]);
        }
        if (args.length >= 2 && args[0].equalsIgnoreCase("create") && hasAdmin(sender)) {
            return List.of("<name>");
        }
        return List.of();
    }

    private List<String> completeGkitGem(CommandSender sender, String[] args) {
        if (!hasAdmin(sender)) return List.of();

        if (args.length == 1) return filter(List.of("give"), args[0]);
        if (args.length == 2 && args[0].equalsIgnoreCase("give")) return filter(onlinePlayerNames(), args[1]);
        if (args.length == 3 && args[0].equalsIgnoreCase("give")) {
            List<String> out = new ArrayList<>();
            out.add("random");
            for (GKit kit : kitManager.getAllKits()) out.add(kit.getName());
            return filter(out, args[2]);
        }
        if (args.length == 4 && args[0].equalsIgnoreCase("give")) {
            return filter(List.of("1", "2", "3", "5", "10", "16", "32", "64"), args[3]);
        }

        return List.of();
    }

    private List<String> completeGkitLock(CommandSender sender, String[] args) {
        if (!hasAdmin(sender)) return List.of();

        if (args.length == 1) return filter(List.of("all", "player"), args[0]);
        if (args.length == 2 && args[0].equalsIgnoreCase("player")) return filter(onlinePlayerNames(), args[1]);
        return List.of();
    }

    private List<String> completeAdminKitNames(CommandSender sender, String[] args) {
        if (!hasAdmin(sender)) return List.of();
        if (args.length == 1) {
            List<String> out = new ArrayList<>();
            for (GKit kit : kitManager.getAllKits()) out.add(kit.getName());
            return filter(out, args[0]);
        }
        return List.of();
    }

    private List<String> completeKitDelete(CommandSender sender, String[] args) {
        if (!hasAdmin(sender)) return List.of();

        if (args.length == 1) {
            List<String> out = new ArrayList<>();
            out.add("kit");
            out.add("gkit");
            for (RankKit kit : rankKitManager.getAllKits()) out.add(kit.getName());
            for (GKit kit : kitManager.getAllKits()) out.add(kit.getName());
            return filter(out, args[0]);
        }

        if (args.length == 2 && args[0].equalsIgnoreCase("kit")) {
            List<String> out = new ArrayList<>();
            for (RankKit kit : rankKitManager.getAllKits()) out.add(kit.getName());
            return filter(out, args[1]);
        }

        if (args.length == 2 && args[0].equalsIgnoreCase("gkit")) {
            List<String> out = new ArrayList<>();
            for (GKit kit : kitManager.getAllKits()) out.add(kit.getName());
            return filter(out, args[1]);
        }

        return List.of();
    }

    private List<String> completeSpawner(String[] args) {
        if (args.length == 1) return filter(List.of("open"), args[0]);
        return List.of();
    }

    private List<String> completeGkitDelete(CommandSender sender, String[] args) {
        if (!hasAdmin(sender)) return List.of();

        if (args.length == 1) {
            List<String> out = new ArrayList<>();
            out.add("gkit");
            for (GKit kit : kitManager.getAllKits()) out.add(kit.getName());
            return filter(out, args[0]);
        }

        if (args.length == 2 && args[0].equalsIgnoreCase("gkit")) {
            List<String> out = new ArrayList<>();
            for (GKit kit : kitManager.getAllKits()) out.add(kit.getName());
            return filter(out, args[1]);
        }

        return List.of();
    }

    private List<String> onlinePlayerNames() {
        Collection<? extends Player> players = Bukkit.getOnlinePlayers();
        List<String> out = new ArrayList<>();
        for (Player player : players) out.add(player.getName());
        return out;
    }

    private boolean hasAdmin(CommandSender sender) {
        return sender.hasPermission("simplekits.admin") || sender.isOp();
    }

    private boolean canUseGkitRoll(CommandSender sender) {
        return sender.hasPermission("simplekits.gkitroll")
                || sender.hasPermission("simplekits.admin")
                || sender.hasPermission("group.owner")
                || sender.hasPermission("group.admin")
                || sender.hasPermission("group.dev")
                || sender.isOp();
    }

    private List<String> filter(List<String> options, String prefix) {
        String lower = prefix == null ? "" : prefix.toLowerCase(Locale.ROOT);
        List<String> out = new ArrayList<>();
        for (String option : options) {
            if (option.toLowerCase(Locale.ROOT).startsWith(lower)) {
                out.add(option);
            }
        }
        return out;
    }
}
