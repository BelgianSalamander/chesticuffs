package salamander.chesticuffs.events;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import salamander.chesticuffs.playerData.DataLoader;
import salamander.chesticuffs.playerData.PlayerData;
import salamander.chesticuffs.game.ChesticuffsGame;
import salamander.chesticuffs.toolbar.ToolbarItems;

public class OnPlayerJoin implements Listener {

    @EventHandler
    public void onJoin(PlayerJoinEvent e){
        if(e.getPlayer().getPersistentDataContainer().has(ChesticuffsGame.playerIdKey, PersistentDataType.STRING)){
            e.getPlayer().getPersistentDataContainer().remove(ChesticuffsGame.playerIdKey);
        }
        if(e.getPlayer().isOp()){
            e.getPlayer().setGameMode(GameMode.CREATIVE);
        }else {
            e.getPlayer().setGameMode(GameMode.ADVENTURE);
            e.getPlayer().getInventory().clear();
        }
        e.getPlayer().getPersistentDataContainer().set(ChesticuffsGame.playerInGameKey, PersistentDataType.BYTE, (byte) 0);
        e.getPlayer().addPotionEffect(new PotionEffect(PotionEffectType.DAMAGE_RESISTANCE, 999999, 255));
        if(!DataLoader.getData().containsKey(e.getPlayer().getUniqueId())){
            DataLoader.getData().put(e.getPlayer().getUniqueId(), new PlayerData());
        }else{
            DataLoader.getData().get(e.getPlayer().getUniqueId()).setLastOnlineAt(System.currentTimeMillis());
        }
        //ToolbarItems.setupPlayer(e.getPlayer());
        e.getPlayer().teleport(Bukkit.getWorld("lobby").getSpawnLocation());
    }
}
