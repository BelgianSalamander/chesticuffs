package salamander.chesticuffs.events;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.persistence.PersistentDataType;
import salamander.chesticuffs.Chesticuffs;
import salamander.chesticuffs.game.ChesticuffsGame;

public class OnInventoryExit implements Listener {
    private class OpenInv implements Runnable{
        Inventory inv;
        Player player;
        public OpenInv(Inventory inv, Player player){
            this.inv = inv;
            this.player = player;
        }

        @Override
        public void run() {
            player.openInventory(inv);
        }
    }

    @EventHandler
    public void onExit(InventoryCloseEvent e){
        if(!e.getPlayer().getPersistentDataContainer().has(ChesticuffsGame.playerIdKey, PersistentDataType.STRING)) {
            return;
        }
        String gameId = e.getPlayer().getPersistentDataContainer().get(ChesticuffsGame.playerIdKey, PersistentDataType.STRING);
        ChesticuffsGame game = Chesticuffs.getGame(gameId);
        if(game==null) {
            return;
        }
        if(System.currentTimeMillis() -  game.getStartTime() < 10 * 1000){
            Bukkit.getScheduler().runTaskLater(Chesticuffs.getPlugin(), new OpenInv(e.getInventory(), (Player) e.getPlayer()), 1);
            e.getPlayer().sendMessage(ChatColor.RED + "Exited too fast. Putting you back in chest.");
            e.getPlayer().sendMessage(ChatColor.GREEN + "You can exit after ten seconds.");
            return;
        }
        game.handleExitEvent((Player) e.getPlayer());
    }
}
