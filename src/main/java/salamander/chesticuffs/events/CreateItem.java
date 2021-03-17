package salamander.chesticuffs.events;

import com.google.common.collect.Lists;
import io.papermc.paper.event.player.PlayerTradeEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.VillagerAcquireTradeEvent;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.FurnaceExtractEvent;
import org.bukkit.event.inventory.FurnaceSmeltEvent;
import org.bukkit.inventory.MerchantRecipe;
import salamander.chesticuffs.inventory.ItemHandler;

import java.util.ArrayList;
import java.util.List;

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
}
