package local.simplekits;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/**
 * /spawner command - Give spawners to players
 */
public class SpawnerCommand implements CommandExecutor {

    private final MysterySpawnerManager spawnerManager;

    public SpawnerCommand(MysterySpawnerManager spawnerManager) {
        this.spawnerManager = spawnerManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("simplekits.spawner.give")) {
            sender.sendMessage("§cYou don't have permission to use this command.");
            return true;
        }

        if (args.length < 3) {
            sender.sendMessage("§c/spawner give <player> <mob>");
            sender.sendMessage("§7Example: /spawner give Steve pig");
            return true;
        }

        if (!args[0].equalsIgnoreCase("give")) {
            sender.sendMessage("§c/spawner give <player> <mob>");
            return true;
        }

        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            sender.sendMessage("§cPlayer '" + args[1] + "' not found.");
            return true;
        }

        String mobName = args[2].toUpperCase();
        EntityType entityType;
        
        try {
            entityType = EntityType.valueOf(mobName);
        } catch (IllegalArgumentException e) {
            sender.sendMessage("§cInvalid mob type '" + args[2] + "'");
            return true;
        }

        ItemStack spawner = spawnerManager.createSpawnerForMob(entityType);
        
        if (target.getInventory().firstEmpty() == -1) {
            target.getWorld().dropItem(target.getLocation(), spawner);
            target.sendMessage("§aReceived §6" + formatMobName(entityType) + " Spawner§a (dropped - inventory full)");
        } else {
            target.getInventory().addItem(spawner);
            target.sendMessage("§a✓ Received §6" + formatMobName(entityType) + " Spawner");
        }
        
        sender.sendMessage("§aGave §6" + formatMobName(entityType) + " Spawner §ato " + target.getName());
        return true;
    }
    
    private String formatMobName(EntityType type) {
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
