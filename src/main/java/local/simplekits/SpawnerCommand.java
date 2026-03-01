package local.simplekits;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/**
 * /spawner command - Get a mystery mob spawner
 */
public class SpawnerCommand implements CommandExecutor {

    private final MysterySpawnerManager spawnerManager;

    public SpawnerCommand(MysterySpawnerManager spawnerManager) {
        this.spawnerManager = spawnerManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cThis command can only be used by players.");
            return true;
        }

        Player player = (Player) sender;

        if (args.length == 0) {
            // Give mystery spawner
            if (player.getInventory().firstEmpty() == -1) {
                player.getWorld().dropItem(player.getLocation(), spawnerManager.createMysterySpawner());
                player.sendMessage("§aMyster Spawner dropped (inventory full).");
            } else {
                player.getInventory().addItem(spawnerManager.createMysterySpawner());
                player.sendMessage("§a✓ Given mystery spawner!");
            }
            return true;
        }

        if (args[0].equalsIgnoreCase("open")) {
            // Simulate opening a mystery spawner - get random spawner
            org.bukkit.entity.EntityType mobType = spawnerManager.getRandomSpawner();
            ItemStack spawner = spawnerManager.createSpawnerForMob(mobType);

            if (player.getInventory().firstEmpty() == -1) {
                player.getWorld().dropItem(player.getLocation(), spawner);
                player.sendMessage("§aGot: §6" + mobType.name() + " Spawner§a (dropped)");
            } else {
                player.getInventory().addItem(spawner);
                player.sendMessage("§a✓ Got: §6" + mobType.name() + " Spawner");
            }
            return true;
        }

        player.sendMessage("§c/spawner [open]");
        return true;
    }
}
