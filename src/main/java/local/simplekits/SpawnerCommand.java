package local.simplekits;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * /spawner command - Get a mystery mob spawner
 *
 * Usage (player):
 *   /spawner           — gives player a mystery spawner
 *   /spawner open      — opens mystery spawner (gives random specific mob)
 *
 * Usage (console / op):
 *   /spawner give <player> <mobtype|mystery>   — admin give specific or mystery spawner
 */
public class SpawnerCommand implements CommandExecutor, TabCompleter {

    private static final List<String> SUPPORTED_MOBS = Arrays.asList(
            "mystery",
            "pig", "cow", "chicken", "sheep",
            "blaze", "enderman", "creeper",
            "guardian", "magma_cube", "ghast",
            "iron_golem", "dolphin", "warden"
    );

    private final MysterySpawnerManager spawnerManager;

    public SpawnerCommand(MysterySpawnerManager spawnerManager) {
        this.spawnerManager = spawnerManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // ── Admin give (console or op) ────────────────────────────────────────
        if (args.length >= 3 && args[0].equalsIgnoreCase("give")) {
            if (!sender.hasPermission("simplekits.admin")) {
                sender.sendMessage("§cYou don't have permission to use this.");
                return true;
            }
            Player target = Bukkit.getPlayerExact(args[1]);
            if (target == null) {
                sender.sendMessage("§cPlayer §e" + args[1] + " §cis not online.");
                return true;
            }
            String mobArg = args[2].toLowerCase();
            if (mobArg.equals("mystery")) {
                giveItem(target, spawnerManager.createMysterySpawner());
                sender.sendMessage("§aGave §e" + target.getName() + " §aa Mystery Spawner.");
                return true;
            }
            EntityType type;
            try {
                type = EntityType.valueOf(mobArg.toUpperCase());
            } catch (IllegalArgumentException e) {
                sender.sendMessage("§cUnknown mob type: §e" + args[2]);
                sender.sendMessage("§7Supported: " + SUPPORTED_MOBS);
                return true;
            }
            ItemStack spawner = spawnerManager.createSpawnerForMob(type);
            giveItem(target, spawner);
            sender.sendMessage("§aGave §e" + target.getName() + " §aa §6" + type.name() + " Spawner§a.");
            return true;
        }

        // ── Player-only commands ──────────────────────────────────────────────
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cUsage: /spawner give <player> <mobtype|mystery>");
            return true;
        }

        if (args.length == 0) {
            // Give mystery spawner to self
            giveItem(player, spawnerManager.createMysterySpawner());
            player.sendMessage("§a✓ Given a Mystery Spawner!");
            return true;
        }

        if (args[0].equalsIgnoreCase("open")) {
            EntityType mobType = spawnerManager.getRandomSpawner();
            ItemStack spawner  = spawnerManager.createSpawnerForMob(mobType);
            giveItem(player, spawner);
            player.sendMessage("§a✓ Got: §6" + mobType.name() + " Spawner");
            return true;
        }

        player.sendMessage("§c/spawner [open]");
        return true;
    }

    private void giveItem(Player target, ItemStack item) {
        if (target.getInventory().firstEmpty() == -1) {
            target.getWorld().dropItem(target.getLocation(), item);
            target.sendMessage("§e⚠ Your inventory is full — spawner dropped at your feet.");
        } else {
            target.getInventory().addItem(item);
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> out = new ArrayList<>();
        if (args.length == 1) {
            for (String s : Arrays.asList("open", "give")) {
                if (s.startsWith(args[0].toLowerCase())) out.add(s);
            }
            return out;
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("give")) {
            Bukkit.getOnlinePlayers().forEach(p -> {
                if (p.getName().toLowerCase().startsWith(args[1].toLowerCase())) out.add(p.getName());
            });
            return out;
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("give")) {
            for (String s : SUPPORTED_MOBS) {
                if (s.startsWith(args[2].toLowerCase())) out.add(s);
            }
            return out;
        }
        return out;
    }
}

