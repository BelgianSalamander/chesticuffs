package salamander.chesticuffs.events;

import org.bukkit.ChatColor;
import org.bukkit.block.Chest;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitTask;
import salamander.chesticuffs.ChestManager;
import salamander.chesticuffs.Chesticuffs;
import salamander.chesticuffs.playerData.DataLoader;
import salamander.chesticuffs.game.ChesticuffsGame;
import salamander.chesticuffs.worlds.GameStarter;
import salamander.chesticuffs.worlds.WorldHandler;

public class OnPlayerLeave implements Listener {
    @EventHandler
    public void onPlayerLeave(PlayerQuitEvent e){
        DataLoader.getData().get(e.getPlayer().getUniqueId()).setLastOnlineAt(System.currentTimeMillis());
        PersistentDataContainer data =  e.getPlayer().getPersistentDataContainer();
        System.out.println("A Player Has Disconnected!");
        if(data.has(GameStarter.gameKey, PersistentDataType.STRING)) {
            System.out.println("Player Was In A Game");
            String key = data.get(GameStarter.gameKey, PersistentDataType.STRING);
            data.remove(GameStarter.gameKey);
            Chest chest = GameStarter.reservedChests.get(key);
            if(chest == null){
                return;
            }
            if (e.getPlayer().getWorld() == WorldHandler.getCollectionWorldOne()) {
                for (Player potentialOpposition : WorldHandler.getCollectionWorldTwo().getPlayers()) { //Find player playing against them
                    if (potentialOpposition.getPersistentDataContainer().get(GameStarter.gameKey, PersistentDataType.STRING) == key) {
                        potentialOpposition.sendMessage(ChatColor.RED + "Your enemy has left the game. You win by default");
                        potentialOpposition.getPersistentDataContainer().remove(GameStarter.gameKey);
                        potentialOpposition.teleport(chest.getWorld().getSpawnLocation());
                        potentialOpposition.setBedSpawnLocation(chest.getWorld().getSpawnLocation(), false);
                        potentialOpposition.getPersistentDataContainer().set(ChesticuffsGame.playerInGameKey, PersistentDataType.BYTE, (byte) 0);
                        break;
                    }
                }
            } else {
                for (Player potentialOpposition : WorldHandler.getCollectionWorldOne().getPlayers()) { //Find player playing against them
                    if (potentialOpposition.getPersistentDataContainer().get(GameStarter.gameKey, PersistentDataType.STRING) == key) {
                        potentialOpposition.sendMessage(ChatColor.RED + "Your enemy has left the game. You win by default");
                        potentialOpposition.getPersistentDataContainer().remove(GameStarter.gameKey);
                        potentialOpposition.teleport(chest.getWorld().getSpawnLocation());
                        potentialOpposition.setBedSpawnLocation(chest.getWorld().getSpawnLocation(), false);
                        potentialOpposition.getPersistentDataContainer().set(ChesticuffsGame.playerInGameKey, PersistentDataType.BYTE, (byte) 0);
                        break;
                    }
                }
            }
            e.getPlayer().teleport(chest.getWorld().getSpawnLocation());
            e.getPlayer().setBedSpawnLocation(chest.getWorld().getSpawnLocation(), false);

            for(BukkitTask task : GameStarter.events.get(key)){
                task.cancel();
            }
            GameStarter.events.remove(key);
            chest.getPersistentDataContainer().set(ChestManager.reservedKey, PersistentDataType.BYTE, (byte) 0);
            GameStarter.reservedChests.remove(key);
            chest.update();
        }else if(Chesticuffs.rankedQueue.contains(e.getPlayer())){
            Chesticuffs.rankedQueue.remove(e.getPlayer());
        }
    }
}
