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
    AQUA_RESISTANT ("Aqua Resistant"),
    FRAGILE ("Fragile"),
    SHRAPNEL ("Shrapnel"),
    PLANT ("Plant"),
    THORNS ("Thorns"), //Unimplemented
    SOFT ("Soft"),
    IMMUNE ("Immune"),
    STUNNED ("Stunned"),
    POISONED ("Poisoned"), //Unimplemented
    CAPBREAKER ("Capbreaker", 0),
    JUMPSTART ("Jumpstart"),
    BREAK ("Break"), //Unimplemented
    AQUATIC("Aquatic"),
    WITHER("Wither"), //Unimplemented
    SOAK("Soak"), //Unimplemented
    OVERGROWTH("Overgrowth"), //Unimplemented
    STICKY("Sticky"), //Unimplemented
    LIGHT("Light"), //Unimplemented
    COMPOSTABLE("Compostable", false),
    POTTABLE("Pottable", false),
    STANDABLE("Standable", false),
    DRIED("Dried"),
    FACADE("Facade"),
    DYED("Dyed"),
    RAGE("Rage"),
    REDSTONE("Redstone"),
    WIRE("Wire"),
    SHOOT("Shoot");

    private String displayName;
    public static final int amountOfValues = 1;
    private int valueIndex;
    private boolean displayed = true;
    public final static int length = Trait.values().length;

    private Trait(String displayName){
        this.displayName = displayName;
        this.valueIndex = -1;
    }
    private Trait(String displayName, boolean displayed){
        this.displayName = displayName;
        this.valueIndex = -1;
        this.displayed = false;
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
        try {
            return isInMeta(item.getItemMeta());
        }catch (NullPointerException e){return false;}
    }

    public boolean shouldBeDisplayed(){
        return displayed;
    }

    public boolean isInMeta(ItemMeta meta){
        try {
            int target = this.toInt();
            int[] traits = meta.getPersistentDataContainer().get(ItemHandler.getTraitsKey(), PersistentDataType.INTEGER_ARRAY);
            for (int trait : traits) {
                if (trait == target) {
                    return true;
                }
            }
        }catch (NullPointerException e){}

        return false;
    }

    public String getDisplayName(){
        return displayName;
    }

    public int getValueIndex() {return valueIndex;}
}
