package local.simplekits;

import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class RankKit {

    private final String name;
    private final String displayName;
    private final String description;
    private final RankTier requiredRank;
    private final int cooldownHours;
    private final List<ItemStack> items = new ArrayList<>();

    public RankKit(String name, String displayName, String description, RankTier requiredRank, int cooldownHours) {
        this.name = name;
        this.displayName = displayName;
        this.description = description;
        this.requiredRank = requiredRank;
        this.cooldownHours = cooldownHours;
    }

    public String getName() {
        return name;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }

    public RankTier getRequiredRank() {
        return requiredRank;
    }

    public int getCooldownHours() {
        return cooldownHours;
    }

    public List<ItemStack> getItems() {
        return Collections.unmodifiableList(items);
    }

    public void addItem(ItemStack item) {
        items.add(item.clone());
    }

    public List<ItemStack> getItemCopies() {
        List<ItemStack> copies = new ArrayList<>();
        for (ItemStack item : items) {
            copies.add(item.clone());
        }
        return copies;
    }
}
