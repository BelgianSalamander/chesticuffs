package salamander.chesticuffs.events;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import salamander.chesticuffs.Chesticuffs;
import salamander.chesticuffs.worlds.GameStarter;

public class OnPlayerDeath implements Listener {
    @EventHandler
    public void onDeath(PlayerDeathEvent e){
        if(e.getEntity().getPersistentDataContainer().has(GameStarter.gameKey, PersistentDataType.STRING)){
            Bukkit.getScheduler().runTaskLater(Chesticuffs.getPlugin(), new ReapplyEffects(e.getEntity()), 1);
        }
    }

    private static class ReapplyEffects implements Runnable{
        Player player;
        public ReapplyEffects(Player player){
            this.player = player;
        }

        @Override
        public void run() {
            player.addPotionEffect(new PotionEffect(PotionEffectType.DAMAGE_RESISTANCE, 18000, 0));
            player.addPotionEffect(new PotionEffect(PotionEffectType.SATURATION, 18000, 0));
        }
    }
}
