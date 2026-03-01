package local.simplekits;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class GKitGemCommand implements CommandExecutor {

    private final KitManager kitManager;
    private final GKitGemManager gemManager;
    private final Random random = new Random();

    public GKitGemCommand(KitManager kitManager, GKitGemManager gemManager) {
        this.kitManager = kitManager;
        this.gemManager = gemManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("simplekits.admin")) {
            sender.sendMessage("§cNo permission.");
            return true;
        }

        if (args.length < 3 || !args[0].equalsIgnoreCase("give")) {
            sender.sendMessage("§6/gkitgem give <player> <kit|random> [amount]");
            sender.sendMessage("§7Example: /gkitgem give Notch starter 1");
            sender.sendMessage("§7Example: /gkitgem give Notch random 3");
            return true;
        }

        Player target = Bukkit.getPlayerExact(args[1]);
        if (target == null) {
            sender.sendMessage("§cPlayer not found.");
            return true;
        }

        String kitInput = args[2].toLowerCase();
        int amount = 1;
        if (args.length >= 4) {
            try {
                amount = Math.max(1, Integer.parseInt(args[3]));
            } catch (NumberFormatException ex) {
                sender.sendMessage("§cAmount must be a number.");
                return true;
            }
        }

        if (kitInput.equals("random")) {
            List<GKit> kits = new ArrayList<>(kitManager.getAllKits());
            if (kits.isEmpty()) {
                sender.sendMessage("§cNo kits loaded.");
                return true;
            }

            for (int i = 0; i < amount; i++) {
                GKit randomKit = kits.get(random.nextInt(kits.size()));
                ItemStack gem = gemManager.getGem(randomKit.getName());
                if (gem != null) {
                    target.getInventory().addItem(gem);
                }
            }

            sender.sendMessage("§aGave §e" + amount + "x random gkit gems §ato " + target.getName());
            target.sendMessage("§aYou received §e" + amount + "x random gkit gems§a.");
            return true;
        }

        GKit kit = kitManager.getKit(kitInput);
        if (kit == null) {
            sender.sendMessage("§cKit not found: " + kitInput);
            return true;
        }

        ItemStack gem = gemManager.getGem(kit.getName());
        if (gem == null) {
            sender.sendMessage("§cFailed to create gem for kit: " + kitInput);
            return true;
        }

        int remaining = amount;
        while (remaining > 0) {
            int stack = Math.min(remaining, gem.getMaxStackSize());
            ItemStack give = gem.clone();
            give.setAmount(stack);
            target.getInventory().addItem(give);
            remaining -= stack;
        }

        sender.sendMessage("§aGave §e" + amount + "x §f" + kit.getName() + " §agems to " + target.getName());
        target.sendMessage("§aYou received §e" + amount + "x §f" + kit.getDisplayName() + " §agems.");
        return true;
    }
}
