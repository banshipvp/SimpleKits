package local.simplekits;

import org.bukkit.entity.EntityType;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;

import java.util.Map;

/**
 * Multiplies XP dropped by dangerous/nether mobs so grinders feel rewarding.
 * Runs at HIGH priority so it applies after StackedMobListener (NORMAL).
 */
public class MobXPBoostListener implements Listener {

    // XP multiplier per entity type. Stacks on top of any vanilla XP.
    private static final Map<EntityType, Integer> MULTIPLIERS = Map.ofEntries(
        Map.entry(EntityType.BLAZE,          8),
        Map.entry(EntityType.MAGMA_CUBE,     6),
        Map.entry(EntityType.WITHER_SKELETON, 10),
        Map.entry(EntityType.GHAST,          8),
        Map.entry(EntityType.PIGLIN_BRUTE,   8),
        Map.entry(EntityType.PIGLIN,         4),
        Map.entry(EntityType.ZOMBIFIED_PIGLIN, 4),
        Map.entry(EntityType.ENDERMAN,       6),
        Map.entry(EntityType.GUARDIAN,       6),
        Map.entry(EntityType.ELDER_GUARDIAN, 20),
        Map.entry(EntityType.WITCH,          5),
        Map.entry(EntityType.CAVE_SPIDER,    4),
        Map.entry(EntityType.SPIDER,         3),
        Map.entry(EntityType.SKELETON,       4),
        Map.entry(EntityType.ZOMBIE,         3),
        Map.entry(EntityType.CREEPER,        4),
        Map.entry(EntityType.SLIME,          3),
        Map.entry(EntityType.DROWNED,        4),
        Map.entry(EntityType.HUSK,           4),
        Map.entry(EntityType.STRAY,          4),
        Map.entry(EntityType.SILVERFISH,     3)
    );

    @EventHandler(priority = EventPriority.HIGH)
    public void onEntityDeath(EntityDeathEvent event) {
        int multiplier = MULTIPLIERS.getOrDefault(event.getEntityType(), 0);
        if (multiplier > 0 && event.getDroppedExp() > 0) {
            event.setDroppedExp(event.getDroppedExp() * multiplier);
        }
    }
}
