package salamander.chesticuffs.events;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.player.PlayerPickupItemEvent;
import salamander.chesticuffs.inventory.ItemHandler;

public class OnPickUp implements Listener {
    @EventHandler
    public void onPlayerPickUp(EntityPickupItemEvent e){
        if (!(e.getEntity() instanceof Player)){
            System.out.println("Not a player");
            return;
        }

        ItemHandler.registerItem(e.getItem().getItemStack());
    }
}
