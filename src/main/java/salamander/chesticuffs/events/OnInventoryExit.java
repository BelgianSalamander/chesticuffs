package salamander.chesticuffs.events;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.persistence.PersistentDataType;
import salamander.chesticuffs.Chesticuffs;
import salamander.chesticuffs.game.ChesticuffsGame;

public class OnInventoryExit implements Listener {
    @EventHandler
    public void onExit(InventoryCloseEvent e){
        if(!e.getPlayer().getPersistentDataContainer().has(ChesticuffsGame.playerIdKey, PersistentDataType.STRING)){
            return;
        }
        String gameId = e.getPlayer().getPersistentDataContainer().get(ChesticuffsGame.playerIdKey, PersistentDataType.STRING);
        ChesticuffsGame game = Chesticuffs.getGame(gameId);
        game.handleExitEvent((Player) e.getPlayer());
    }
}
