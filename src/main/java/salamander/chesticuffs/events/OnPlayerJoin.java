package salamander.chesticuffs.events;

import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.persistence.PersistentDataType;
import salamander.chesticuffs.game.ChesticuffsGame;

public class OnPlayerJoin implements Listener {
    public void onJoin(PlayerJoinEvent e){
        if(e.getPlayer().getPersistentDataContainer().has(ChesticuffsGame.playerIdKey, PersistentDataType.STRING)){
            e.getPlayer().getPersistentDataContainer().remove(ChesticuffsGame.playerIdKey);
        }
    }
}
