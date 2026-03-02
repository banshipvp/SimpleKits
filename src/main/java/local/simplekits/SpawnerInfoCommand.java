package local.simplekits;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.CreatureSpawner;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Command to check spawner stack information
 */
public class SpawnerInfoCommand implements CommandExecutor {

    private final StackedSpawnerManager stackedSpawnerManager;

    public SpawnerInfoCommand(StackedSpawnerManager stackedSpawnerManager) {
        this.stackedSpawnerManager = stackedSpawnerManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cOnly players can use this command!");
            return true;
        }

        Player player = (Player) sender;
        Block targetBlock = player.getTargetBlockExact(5);

        if (targetBlock == null || targetBlock.getType() != Material.SPAWNER) {
            player.sendMessage("§c✗ Must be looking at a spawner within 5 blocks!");
            return true;
        }

        CreatureSpawner spawner = (CreatureSpawner) targetBlock.getState();
        int stackCount = stackedSpawnerManager.getSpawnerStackCount(targetBlock);
        double multiplier = stackedSpawnerManager.getSpawnRateMultiplier(stackCount);

        player.sendMessage("§6=== Spawner Information ===");
        player.sendMessage("§7Mob Type: §f" + spawner.getSpawnedType().name());
        player.sendMessage("§7Stack Count: §f" + stackCount + "/" + stackedSpawnerManager.getMaxStacks());
        player.sendMessage("§7Spawn Rate Multiplier: §f" + String.format("%.2fx", multiplier));
        player.sendMessage("§7Base Delay: §f" + spawner.getMinSpawnDelay() + "-" + spawner.getMaxSpawnDelay() + "ms");
        player.sendMessage("§7Max Nearby Entities: §f" + spawner.getMaxNearbyEntities());
        
        if (stackCount == stackedSpawnerManager.getMaxStacks()) {
            player.sendMessage("§a✓ MAXIMUM STACK REACHED!");
        }

        return true;
    }
}
