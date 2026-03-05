package local.simplekits;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * SimpleKits - Handles gkits, mystery spawners, and gkit gems
 */
public class SimpleKitsPlugin extends JavaPlugin {

    private KitManager kitManager;
    private RankKitManager rankKitManager;
    private GKitGemManager gkitGemManager;
    private MysterySpawnerManager spawnerManager;
    private KitEditorManager kitEditorManager;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        // Initialize managers
        kitManager = new KitManager(this);
        rankKitManager = new RankKitManager();
        gkitGemManager = new GKitGemManager(this, kitManager);
        spawnerManager = new MysterySpawnerManager(this);
        kitEditorManager = new KitEditorManager(this, rankKitManager, kitManager);

        // Load kits from config
        kitManager.loadKits();
        rankKitManager.loadDefaultKits();

        KitsCommand kitsCommand = new KitsCommand(rankKitManager);

        // Register commands
        getCommand("gkits").setExecutor(new GKitsCommand(this, kitManager));
        getCommand("kits").setExecutor(kitsCommand);
        getCommand("gkit").setExecutor(new GKitCommand(kitManager, gkitGemManager, kitEditorManager));
        getCommand("gkitroll").setExecutor(new GKitRollCommand(kitManager, gkitGemManager));
        getCommand("kit").setExecutor(new KitCommand(rankKitManager, kitEditorManager));
        getCommand("kitcreate").setExecutor((sender, command, label, args) -> {
            if (!(sender instanceof org.bukkit.entity.Player player)) {
                sender.sendMessage("§cThis command can only be used by players.");
                return true;
            }
            if (!player.hasPermission("simplekits.admin")) {
                player.sendMessage("§cYou do not have permission to create kits.");
                return true;
            }
            if (args.length == 0) {
                kitEditorManager.promptForName(player, KitEditorManager.CreationType.KIT);
            } else {
                kitEditorManager.startEditor(player, KitEditorManager.CreationType.KIT, String.join(" ", args));
            }
            return true;
        });
        getCommand("gkitcreate").setExecutor((sender, command, label, args) -> {
            if (!(sender instanceof org.bukkit.entity.Player player)) {
                sender.sendMessage("§cThis command can only be used by players.");
                return true;
            }
            if (!player.hasPermission("simplekits.admin")) {
                player.sendMessage("§cYou do not have permission to create gkits.");
                return true;
            }
            if (args.length == 0) {
                kitEditorManager.promptForName(player, KitEditorManager.CreationType.GKIT);
            } else {
                kitEditorManager.startEditor(player, KitEditorManager.CreationType.GKIT, String.join(" ", args));
            }
            return true;
        });
        getCommand("gkitgem").setExecutor(new GKitGemCommand(kitManager, gkitGemManager));
        getCommand("gkitlock").setExecutor(new GKitLockCommand(gkitGemManager));
        getCommand("kitdelete").setExecutor(new KitDeleteCommand(rankKitManager, kitManager));
        GKitUnlockCommand gkitUnlockCommand = new GKitUnlockCommand(kitManager, gkitGemManager);
        getCommand("gkitunlock").setExecutor(gkitUnlockCommand);
        getCommand("gkitunlock").setTabCompleter(gkitUnlockCommand);
        getCommand("spawner").setExecutor(new SpawnerCommand(spawnerManager));

        // Register listeners
        Bukkit.getPluginManager().registerEvents(new GKitGemListener(gkitGemManager, kitManager), this);
        Bukkit.getPluginManager().registerEvents(new GKitGuiListener(kitManager, gkitGemManager), this);
        Bukkit.getPluginManager().registerEvents(new KitsGuiListener(rankKitManager, kitsCommand), this);
        Bukkit.getPluginManager().registerEvents(new MysterySpawnerListener(spawnerManager), this);
        Bukkit.getPluginManager().registerEvents(new KitEditorListener(kitEditorManager), this);

        getLogger().info("SimpleKits enabled successfully.");
        getLogger().info("Loaded " + kitManager.getKitCount() + " gkits.");
    }

    @Override
    public void onDisable() {
        getLogger().info("SimpleKits disabled.");
    }

    public KitManager getKitManager() {
        return kitManager;
    }

    public RankKitManager getRankKitManager() {
        return rankKitManager;
    }

    public GKitGemManager getGKitGemManager() {
        return gkitGemManager;
    }

    public MysterySpawnerManager getSpawnerManager() {
        return spawnerManager;
    }

    public KitEditorManager getKitEditorManager() {
        return kitEditorManager;
    }
}
