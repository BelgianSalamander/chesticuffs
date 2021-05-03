package salamander.chesticuffs.events;

import org.bukkit.ChatColor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerBedEnterEvent;

public class PlayerSleep implements Listener {
    @EventHandler
    public void OnPlayerSleep(PlayerBedEnterEvent e){
        e.getPlayer().sendMessage(ChatColor.RED + "Sleeping is disabled");
        e.setCancelled(true);
    }
}
