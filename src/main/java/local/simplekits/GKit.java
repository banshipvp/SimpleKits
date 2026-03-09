package local.simplekits;

import org.bukkit.inventory.ItemStack;

/**
 * Represents a gkit with custom enchanted items
 */
public class GKit {
    
    private String name;
    private String displayName;
    private String description;
    private int cooldownHours;  // Cooldown in hours
    private ItemStack[] items;  // Items in the kit (including custom enchants)
    private long createdTime;
    
    public GKit(String name, String displayName, String description, int cooldownHours) {
        this.name = name;
        this.displayName = displayName;
        this.description = description;
        this.cooldownHours = cooldownHours;
        this.items = new ItemStack[0];
        this.createdTime = System.currentTimeMillis();
    }
    
    // ===== GETTERS =====
    public String getName() { return name; }
    public String getDisplayName() { return displayName; }
    public String getDescription() { return description; }
    public int getCooldownHours() { return cooldownHours; }
    public ItemStack[] getItems() { return items; }
    public long getCreatedTime() { return createdTime; }
    
    // ===== SETTERS =====
    public void setItems(ItemStack[] items) { this.items = items; }
    public void setDescription(String desc) { this.description = desc; }
    public void setCooldownHours(int hours) { this.cooldownHours = hours; }
    
    /**
     * Add an item to the kit
     */
    public void addItem(ItemStack item) {
        ItemStack[] newItems = new ItemStack[items.length + 1];
        System.arraycopy(items, 0, newItems, 0, items.length);
        newItems[items.length] = item.clone();
        this.items = newItems;
    }
    
    /**
     * Get a copy of kit items (for giving to player)
     */
    public ItemStack[] getItemsCopy() {
        ItemStack[] copy = new ItemStack[items.length];
        for (int i = 0; i < items.length; i++) {
            if (items[i] != null) {
                copy[i] = items[i].clone();
            }
        }
        return copy;
    }
    
    /**
     * Check if this kit has items
     */
    public boolean hasItems() {
        return items.length > 0;
    }
    
    @Override
    public String toString() {
        return String.format("GKit[%s] %s - %s", name, displayName, description);
    }
}
