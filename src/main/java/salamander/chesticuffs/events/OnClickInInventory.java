package salamander.chesticuffs.events;

import org.bukkit.ChatColor;
import org.bukkit.block.Chest;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.persistence.PersistentDataType;
import salamander.chesticuffs.ChestManager;
import salamander.chesticuffs.Chesticuffs;
import salamander.chesticuffs.game.ChesticuffsGame;
import salamander.chesticuffs.toolbar.ToolbarItems;

public class OnClickInInventory implements Listener {
    @EventHandler
    public void onClick(InventoryClickEvent e){
        if(!e.getWhoClicked().getPersistentDataContainer().has(ChesticuffsGame.playerIdKey, PersistentDataType.STRING)){
            if(e.getCurrentItem() != null){
                if(e.getCurrentItem() == ToolbarItems.stats || e.getCurrentItem() == ToolbarItems.watchArena){
                    e.setCancelled(true);
                    return;
                }
            }
            InventoryHolder holder = e.getInventory().getHolder();
            if(holder instanceof Chest){
                Chest chest = (Chest) holder;
                if(chest.getPersistentDataContainer().has(ChestManager.reservedKey, PersistentDataType.BYTE)){
                    e.setCancelled(true);
                }
            }
            if(e.getInventory().getType().equals(InventoryType.CHEST) && e.getInventory().getSize() == 9){
                e.setCancelled(true);
            }
            return;
        }
        String gameId = e.getWhoClicked().getPersistentDataContainer().get(ChesticuffsGame.playerIdKey, PersistentDataType.STRING);
        ChesticuffsGame game = Chesticuffs.getGame(gameId);
        game.handleClickEvent(e);
    }
}
