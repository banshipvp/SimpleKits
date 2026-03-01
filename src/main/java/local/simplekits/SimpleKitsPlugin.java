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

    @Override
    public void onEnable() {
        saveDefaultConfig();

        // Initialize managers
        kitManager = new KitManager(this);
        rankKitManager = new RankKitManager();
        gkitGemManager = new GKitGemManager(this, kitManager);
        spawnerManager = new MysterySpawnerManager(this);

        // Load kits from config
        kitManager.loadKits();
        rankKitManager.loadDefaultKits();

        KitsCommand kitsCommand = new KitsCommand(rankKitManager);

        // Register commands
        getCommand("gkits").setExecutor(new GKitsCommand(this, kitManager));
        getCommand("kits").setExecutor(kitsCommand);
        getCommand("gkit").setExecutor(new GKitCommand(kitManager, gkitGemManager));
        getCommand("gkitgem").setExecutor(new GKitGemCommand(kitManager, gkitGemManager));
        getCommand("gkitlock").setExecutor(new GKitLockCommand(gkitGemManager));
        getCommand("spawner").setExecutor(new SpawnerCommand(spawnerManager));

        // Register listeners
        Bukkit.getPluginManager().registerEvents(new GKitGemListener(gkitGemManager, kitManager), this);
        Bukkit.getPluginManager().registerEvents(new GKitGuiListener(kitManager, gkitGemManager), this);
        Bukkit.getPluginManager().registerEvents(new KitsGuiListener(rankKitManager, kitsCommand), this);
        Bukkit.getPluginManager().registerEvents(new MysterySpawnerListener(spawnerManager), this);

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
}
