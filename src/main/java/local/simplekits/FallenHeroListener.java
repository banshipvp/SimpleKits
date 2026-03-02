package local.simplekits;

import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

/**
 * Handles Fallen Hero bone placement, interaction, and mob death rewards.
 */
public class FallenHeroListener implements Listener {

    private final FallenHeroManager fallenHeroManager;

    public FallenHeroListener(FallenHeroManager fallenHeroManager) {
        this.fallenHeroManager = fallenHeroManager;
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;

        ItemStack item = event.getItem();
        if (item == null || !fallenHeroManager.isFallenHero(item)) return;

        event.setCancelled(true);
        Player player = event.getPlayer();
        String kitName = fallenHeroManager.getFallenHeroKitName(item);

        if (kitName == null) {
            player.sendMessage("§cInvalid Fallen Hero item.");
            return;
        }

        // Spawn the mob
        fallenHeroManager.spawnFallenHeroMob(player, kitName);
        player.sendMessage("§c⚔ A Fallen §f" + capitalize(kitName) + " §chas appeared! Defeat it to claim your reward.");

        // Consume one bone
        if (item.getAmount() > 1) {
            item.setAmount(item.getAmount() - 1);
        } else {
            player.getInventory().setItemInMainHand(null);
        }
    }

    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        LivingEntity entity = event.getEntity();
        if (!entity.hasMetadata("fallen_hero_kit")) return;

        Player killer = entity.getKiller();
        if (killer == null) return;

        // Award the reward
        fallenHeroManager.awardKillReward(entity, killer);
    }

    private String capitalize(String str) {
        if (str == null || str.isEmpty()) return str;
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }
}
