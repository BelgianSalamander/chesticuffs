package salamander.chesticuffs.events;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerPortalEvent;
import org.bukkit.event.player.PlayerTeleportEvent;

public class PortalEvent implements Listener {
    @EventHandler
    public void onPortal(PlayerPortalEvent event){
        Player player = event.getPlayer();

        if(event.getCause().equals(PlayerTeleportEvent.TeleportCause.NETHER_PORTAL)){
            event.setCanCreatePortal(true);

            Location location = null;
            if (!player.getWorld().getName().endsWith("_nether")) {
                World netherWorld = Bukkit.getWorld(player.getWorld().getName() + "_nether");
                location = new Location(netherWorld, event.getFrom().getBlockX() / 8, event.getFrom().getBlockY(), event.getFrom().getBlockZ() / 8);

            } else {
                String worldName = player.getWorld().getName();
                location = new Location(Bukkit.getWorld(worldName.substring(0, worldName.length() - 7)), event.getFrom().getBlockX() * 8, event.getFrom().getBlockY(), event.getFrom().getBlockZ() * 8);
            }
            event.setTo(location);
        }
    }
}
