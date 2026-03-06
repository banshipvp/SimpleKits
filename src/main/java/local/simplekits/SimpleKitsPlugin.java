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
        // Load any custom kits/gkits created via the editor (survives restarts)
        kitEditorManager.loadSavedKits();

        KitsCommand kitsCommand = new KitsCommand(rankKitManager, kitEditorManager);

        // Register commands
        GKitsCommand gKitsCommand = new GKitsCommand(this, kitManager, kitEditorManager);
        getCommand("gkits").setExecutor(gKitsCommand);
        getCommand("kits").setExecutor(kitsCommand);
        getCommand("gkit").setExecutor(new GKitCommand(kitManager, gkitGemManager, kitEditorManager, gKitsCommand));
        getCommand("gkitroll").setExecutor(new GKitRollCommand(kitManager, gkitGemManager));
        getCommand("kit").setExecutor(new KitCommand(rankKitManager, kitEditorManager, kitsCommand));
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
        getCommand("gkitedit").setExecutor(new GKitEditCommand(kitEditorManager));
        getCommand("gkitgem").setExecutor(new GKitGemCommand(kitManager, gkitGemManager));
        getCommand("gkitlock").setExecutor(new GKitLockCommand(gkitGemManager));
        getCommand("kitdelete").setExecutor(new KitDeleteCommand(rankKitManager, kitManager));
        getCommand("gkitdelete").setExecutor(new KitDeleteCommand(rankKitManager, kitManager));
        GKitUnlockCommand gkitUnlockCommand = new GKitUnlockCommand(kitManager, gkitGemManager);
        getCommand("gkitunlock").setExecutor(gkitUnlockCommand);
        getCommand("gkitunlock").setTabCompleter(gkitUnlockCommand);
        getCommand("spawner").setExecutor(new SpawnerCommand(spawnerManager));

            // Register tab completers
            SimpleKitsTabCompleter tabCompleter = new SimpleKitsTabCompleter(kitManager, rankKitManager);
            getCommand("gkits").setTabCompleter(tabCompleter);
            getCommand("kits").setTabCompleter(tabCompleter);
            getCommand("gkit").setTabCompleter(tabCompleter);
            getCommand("gkitroll").setTabCompleter(tabCompleter);
            getCommand("kit").setTabCompleter(tabCompleter);
            getCommand("gkitcreate").setTabCompleter(tabCompleter);
            getCommand("gkitedit").setTabCompleter(tabCompleter);
            getCommand("gkitgem").setTabCompleter(tabCompleter);
            getCommand("gkitlock").setTabCompleter(tabCompleter);
            getCommand("kitcreate").setTabCompleter(tabCompleter);
            getCommand("kitdelete").setTabCompleter(tabCompleter);
            getCommand("gkitdelete").setTabCompleter(tabCompleter);
            getCommand("spawner").setTabCompleter(tabCompleter);

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
