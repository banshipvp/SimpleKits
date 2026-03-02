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
    private StackedSpawnerManager stackedSpawnerManager;
    private MobStackManager mobStackManager;
    private FlightPersistenceManager flightPersistenceManager;
    private FallenHeroManager fallenHeroManager;
    private RerollScrollManager rerollScrollManager;
    private BlackScrollManager blackScrollManager;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        // Initialize managers
        kitManager = new KitManager(this);
        rankKitManager = new RankKitManager();
        gkitGemManager = new GKitGemManager(this, kitManager);
        spawnerManager = new MysterySpawnerManager(this);
        stackedSpawnerManager = new StackedSpawnerManager(this);
        mobStackManager = new MobStackManager(this);
        flightPersistenceManager = new FlightPersistenceManager(this);
        fallenHeroManager = new FallenHeroManager(this);
        rerollScrollManager = new RerollScrollManager(this);
        blackScrollManager = new BlackScrollManager(this);

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
        getCommand("mysterymobspawner").setExecutor(new MysterySpawnerCommand(spawnerManager));
        getCommand("spawner").setExecutor(new SpawnerCommand(spawnerManager));
        getCommand("spawnerinfo").setExecutor(new SpawnerInfoCommand(stackedSpawnerManager));
        getCommand("spawnerhelp").setExecutor(new SpawnerStackHelpCommand(stackedSpawnerManager));

        // Register listeners
        Bukkit.getPluginManager().registerEvents(new GKitGemListener(gkitGemManager, kitManager), this);
        Bukkit.getPluginManager().registerEvents(new GKitGuiListener(kitManager, gkitGemManager), this);
        Bukkit.getPluginManager().registerEvents(new KitsGuiListener(rankKitManager, kitsCommand), this);
        Bukkit.getPluginManager().registerEvents(new MysterySpawnerListener(spawnerManager, stackedSpawnerManager), this);
        Bukkit.getPluginManager().registerEvents(new StackedMobListener(stackedSpawnerManager), this);
        Bukkit.getPluginManager().registerEvents(mobStackManager, this);
        Bukkit.getPluginManager().registerEvents(new MobXPBoostListener(), this);
        Bukkit.getPluginManager().registerEvents(flightPersistenceManager, this);
        Bukkit.getPluginManager().registerEvents(new FallenHeroListener(fallenHeroManager), this);
        Bukkit.getPluginManager().registerEvents(new ScrollListener(this, rerollScrollManager, blackScrollManager), this);

        getLogger().info("SimpleKits enabled successfully.");
        getLogger().info("Loaded " + kitManager.getKitCount() + " gkits.");
        getLogger().info("Mob stacking system enabled.");
        getLogger().info("Flight persistence enabled.");
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

    public StackedSpawnerManager getStackedSpawnerManager() {
        return stackedSpawnerManager;
    }

    public FallenHeroManager getFallenHeroManager() {
        return fallenHeroManager;
    }

    public RerollScrollManager getRerollScrollManager() {
        return rerollScrollManager;
    }

    public BlackScrollManager getBlackScrollManager() {
        return blackScrollManager;
    }
}
