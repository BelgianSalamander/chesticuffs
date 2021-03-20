package salamander.chesticuffs.events;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.block.Chest;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import salamander.chesticuffs.Chesticuffs;
import salamander.chesticuffs.game.ChesticuffsGame;
import salamander.chesticuffs.game.GameID;
import salamander.chesticuffs.inventory.ChestKeys;

public class OnChestClick implements Listener {
    //Detects when players click on chest and when a game should start. The final code is probably gonna be very different
    @EventHandler
    public void OnClick(PlayerInteractEvent e){
        if(!e.getAction().equals(Action.RIGHT_CLICK_BLOCK)) return;
        if(!e.getClickedBlock().getType().equals(Material.CHEST)) return;
        Chest chest = (Chest) e.getClickedBlock().getState();
        PersistentDataContainer chestData = chest.getPersistentDataContainer();
        if(!chestData.has(ChestKeys.idKey, PersistentDataType.STRING)){
            String id = GameID.next();
            chestData.set(ChestKeys.idKey, PersistentDataType.STRING, id);
            chest.update();
            Chesticuffs.addNewGame(id, new ChesticuffsGame(e.getPlayer(), chest, id));
            e.getPlayer().sendMessage(ChatColor.GREEN + "Successfully Joined Game!");
        }else{
            String id = chestData.get(ChestKeys.idKey, PersistentDataType.STRING);
            ChesticuffsGame game = Chesticuffs.getGame(id);
            if(game.isFull()) return;
            if(game.isPlayerInGame(e.getPlayer())){
                e.setCancelled(true);
                e.getPlayer().sendMessage(ChatColor.RED + "You are already in that game!");
                return;
            }
            game.addPlayer(e.getPlayer());
            e.getPlayer().sendMessage(ChatColor.GREEN + "Successfully Joined Game!");
            System.out.println("Chest Clicked For The Second Time");
        }
        e.setCancelled(true);
    }
}
