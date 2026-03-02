package local.simplekits;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Command to show spawner stacking help and information
 */
public class SpawnerStackHelpCommand implements CommandExecutor {

    private final StackedSpawnerManager stackedSpawnerManager;

    public SpawnerStackHelpCommand(StackedSpawnerManager stackedSpawnerManager) {
        this.stackedSpawnerManager = stackedSpawnerManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cOnly players can use this command!");
            return true;
        }

        Player player = (Player) sender;

        player.sendMessage("");
        player.sendMessage("§6╔═══════════════════════════════════════╗");
        player.sendMessage("§6║ §f§lStacked Spawner & Mob System");
        player.sendMessage("§6╚═══════════════════════════════════════╝");
        player.sendMessage("");
        player.sendMessage("§e▸ SPAWNER STACKING:");
        player.sendMessage("§7  • Place the same type of spawner on top of another");
        player.sendMessage("§7  • Stack up to 10 spawners in one block");
        player.sendMessage("§7  • Each stack increases spawn rate");
        player.sendMessage("");
        player.sendMessage("§e▸ SPAWN RATE MULTIPLIERS:");
        player.sendMessage("§7  • Base Rate: §f" + String.format("%.1f", stackedSpawnerManager.getBaseSpawnRateMultiplier()) + "x§7 faster");
        player.sendMessage("§7  • 10 Stacked: §f" + String.format("%.2f", stackedSpawnerManager.getSpawnRateMultiplier(10)) + "x§7 faster");
        player.sendMessage("§7  • Formula: 4x base + 1.5x bonus at max stacks");
        player.sendMessage("");
        player.sendMessage("§e▸ MOB STACKING:");
        player.sendMessage("§7  • Mobs automatically stack together");
        player.sendMessage("§7  • Shows count above mob (e.g., §cx2 Cow§7)");
        player.sendMessage("§7  • Killing stacked mobs drops more items");
        player.sendMessage("§7  • XP is multiplied by stack count");
        player.sendMessage("");
        player.sendMessage("§e▸ COMMANDS:");
        player.sendMessage("§7  • /spawnerinfo - View stack info (look at spawner)");
        player.sendMessage("§7  • /spawner - Get a mystery spawner");
        player.sendMessage("§7  • /spawnerhelp - Show this help message");
        player.sendMessage("");

        return true;
    }
}
