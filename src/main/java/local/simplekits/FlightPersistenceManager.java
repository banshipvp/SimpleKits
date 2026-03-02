package local.simplekits;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

/**
 * Persists player flight state across logins
 */
public class FlightPersistenceManager implements Listener {
    
    private final SimpleKitsPlugin plugin;
    private final File dataFile;
    private YamlConfiguration data;
    
    public FlightPersistenceManager(SimpleKitsPlugin plugin) {
        this.plugin = plugin;
        this.dataFile = new File(plugin.getDataFolder(), "flight_data.yml");
        loadData();
    }
    
    private void loadData() {
        if (!dataFile.exists()) {
            data = new YamlConfiguration();
        } else {
            data = YamlConfiguration.loadConfiguration(dataFile);
        }
    }
    
    private void saveData() {
        try {
            data.save(dataFile);
        } catch (IOException e) {
            plugin.getLogger().warning("Failed to save flight data: " + e.getMessage());
        }
    }
    
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        
        // Save flight state
        data.set(uuid.toString() + ".flying", player.isFlying());
        data.set(uuid.toString() + ".allowFlight", player.getAllowFlight());
        
        saveData();
    }
    
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        
        // Restore flight state after 1 tick (to ensure player is fully loaded)
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (data.contains(uuid.toString() + ".allowFlight")) {
                boolean allowFlight = data.getBoolean(uuid.toString() + ".allowFlight");
                boolean wasFlying = data.getBoolean(uuid.toString() + ".flying");
                
                if (allowFlight) {
                    player.setAllowFlight(true);
                    if (wasFlying) {
                        player.setFlying(true);
                    }
                }
            }
        }, 1L);
    }
}
