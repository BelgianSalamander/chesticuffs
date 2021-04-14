package salamander.chesticuffs.events;

import org.bukkit.GameMode;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class TeleportEvent implements Listener {
    @EventHandler
    public void onTeleport(PlayerTeleportEvent e){
        if(e.getFrom().getWorld().getName().equalsIgnoreCase("lobby")) return;
        if(e.getTo().getWorld().getName().equalsIgnoreCase("lobby")){
            for(PotionEffect potion : e.getPlayer().getActivePotionEffects()){
                e.getPlayer().removePotionEffect(potion.getType());
            }
            e.getPlayer().addPotionEffect(new PotionEffect(PotionEffectType.DAMAGE_RESISTANCE, 999999, 255));
            e.getPlayer().setGameMode(GameMode.ADVENTURE);
        }
    }
}
