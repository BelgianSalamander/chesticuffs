package salamander.chesticuffs.events;

import org.bukkit.ChatColor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.PortalCreateEvent;

public class OnPortal implements Listener {
    @EventHandler
    public void onPortalLight(PortalCreateEvent e){
        e.setCancelled(true);
        if(e.getEntity() != null){
            e.getEntity().sendMessage(ChatColor.RED + "No");
        }
    }
}
