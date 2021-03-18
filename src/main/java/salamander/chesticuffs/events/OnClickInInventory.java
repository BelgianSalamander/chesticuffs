package salamander.chesticuffs.events;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.persistence.PersistentDataType;
import salamander.chesticuffs.Chesticuffs;
import salamander.chesticuffs.game.ChesticuffsGame;

public class OnClickInInventory implements Listener {
    @EventHandler
    public void onClick(InventoryClickEvent e){
        System.out.println("Click in inv!");
        if(!e.getWhoClicked().getPersistentDataContainer().has(ChesticuffsGame.playerIdKey, PersistentDataType.STRING)){
            return;
        }
        String gameId = e.getWhoClicked().getPersistentDataContainer().get(ChesticuffsGame.playerIdKey, PersistentDataType.STRING);
        ChesticuffsGame game = Chesticuffs.getGame(gameId);
        game.handleClickEvent(e);
    }
}
