package salamander.chesticuffs.events;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import salamander.chesticuffs.inventory.ItemHandler;

public class OnPickUp implements Listener {
    //When a player picks up an item it is given the lore and stats it needs
    @EventHandler
    public void onPlayerPickUp(EntityPickupItemEvent e){
        if (!(e.getEntity() instanceof Player)){
            return;
        }

        ItemHandler.registerItem(e.getItem().getItemStack());
    }
}
