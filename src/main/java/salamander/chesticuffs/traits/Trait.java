package salamander.chesticuffs.traits;

import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import salamander.chesticuffs.inventory.ItemHandler;

public enum Trait {
    GRAVITY ("Gravity"),
    FLAMMABLE ("Flammable"),
    FLAME ("Flame"),
    FIRE_RESISTANT ("Fire Resistant"),
    FRAGILE ("Fragile"),
    SHRAPNEL ("Shrapnel"),
    PLANT ("Plant"),
    THORNS ("Thorns"),
    SOFT ("Soft"),
    IMMUNE ("Immune"),
    STUNNED ("Stunned"),
    POISONED ("Poisoned"),
    CAPBREAKER ("Capbreaker", 0),
    JUMPSTART ("Jumpstart"),
    BREAK ("Break"),
    AQUATIC("Aquatic"),
    WITHER("Wither"),
    SOAK("Soak"),
    OVERGROWTH("Overgrowth"),
    STICKY("Sticky"),
    LIGHT("Light");

    private String displayName;
    static final int amountOfValues = 1;
    private int valueIndex;
    public final static int length = Trait.values().length;

    private Trait(String displayName){
        this.displayName = displayName;
        this.valueIndex = -1;
    }
    private Trait(String displayName, int valueIndex){
        this.displayName = displayName;
        this.valueIndex = valueIndex;
    }

    static public Trait fromInt(int n){
        return Trait.values()[n];
    }

    public int toInt(){
        return this.ordinal();
    }

    public boolean isInItem(ItemStack item){
        return isInMeta(item.getItemMeta());
    }

    public boolean isInMeta(ItemMeta meta){
        int target = this.toInt();
        int[] traits = meta.getPersistentDataContainer().get(ItemHandler.getTraitsKey(), PersistentDataType.INTEGER_ARRAY);
        assert traits != null;
        for(int trait : traits){
            if(trait == target){
                return true;
            }
        }

        return false;
    }

    public String getDisplayName(){
        return displayName;
    }

    public int getValueIndex() {return valueIndex;}
}
