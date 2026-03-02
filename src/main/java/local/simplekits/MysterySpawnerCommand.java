package local.simplekits;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/**
 * /mysterymobspawner command - Get a mystery mob spawner
 */
public class MysterySpawnerCommand implements CommandExecutor {

    private final MysterySpawnerManager spawnerManager;

    public MysterySpawnerCommand(MysterySpawnerManager spawnerManager) {
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
                player.sendMessage("§aMystery Spawner dropped (inventory full).");
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
                player.sendMessage("§aGot: §6" + formatMobName(mobType) + " Spawner§a (dropped)");
            } else {
                player.getInventory().addItem(spawner);
                player.sendMessage("§a✓ Got: §6" + formatMobName(mobType) + " Spawner");
            }
            return true;
        }

        player.sendMessage("§c/mysterymobspawner [open]");
        return true;
    }
    
    private String formatMobName(org.bukkit.entity.EntityType type) {
        String[] words = type.name().toLowerCase().split("_");
        StringBuilder result = new StringBuilder();
        for (String word : words) {
            if (!word.isEmpty()) {
                result.append(Character.toUpperCase(word.charAt(0)));
                result.append(word.substring(1));
                result.append(" ");
            }
        }
        return result.toString().trim();
    }
}
