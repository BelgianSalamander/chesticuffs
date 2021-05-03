package salamander.chesticuffs.inventory;

import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

public class TraitsField {
    private boolean isImmune, isFlame, isFlammable, isFireResistant;

    public TraitsField(ItemStack item){
        for(String trait : item.getItemMeta().getPersistentDataContainer().get(ItemHandler.traitsKey, PersistentDataType.STRING).split(",")){
            if(trait.equalsIgnoreCase("Immunity"));
        }
    }
}
