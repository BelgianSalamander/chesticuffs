package salamander.chesticuffs.events;


import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.FurnaceSmeltEvent;
import org.bukkit.event.player.PlayerBucketEvent;
import org.bukkit.inventory.ItemStack;
import salamander.chesticuffs.inventory.ItemHandler;

public class CreateItem implements Listener {
    //As soon as an item is crafted or smelted it is registered and given the stats and lore it needs

    @EventHandler
    public void OnItemCraft(CraftItemEvent e){
        ItemHandler.registerItem(e.getCurrentItem());
    }

    @EventHandler
    public void onItemSmelt(FurnaceSmeltEvent e){
        ItemHandler.registerItem(e.getResult());
    }

    /*@EventHandler
    public void onBucket(PlayerBucketEvent e) {
        ItemHandler.registerItem(e.getItemStack());
    }*/
}
