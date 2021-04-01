package salamander.chesticuffs;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataType;
import salamander.chesticuffs.worlds.GameStarter;

import java.util.List;
import java.util.Random;

public class QueueScanner implements Runnable{
    public void run(){
        Random rand = new Random();
        if(!Chesticuffs.isQueueActive()) return;
        if(Chesticuffs.rankedQueue.size() < 2 && Chesticuffs.unrankedQueue.size() < 2) return;
        boolean ranked;
        List<Player> queue;
        if(Chesticuffs.rankedQueue.size() < 2){
            queue = Chesticuffs.unrankedQueue;
            ranked = false;
        }else if(Chesticuffs.unrankedQueue.size() < 2){
            queue = Chesticuffs.rankedQueue;
            ranked = true;
        }else{
            if(rand.nextDouble() * (Chesticuffs.rankedQueue.size() + Chesticuffs.unrankedQueue.size()) < Chesticuffs.rankedQueue.size()){
                queue = Chesticuffs.rankedQueue;
                ranked = true;
            }else{
                queue = Chesticuffs.unrankedQueue;
                ranked = false;
            }
        }

        for(Location chestLocation : ChestManager.chests){
            Block mostLikelyAChest = chestLocation.getWorld().getBlockAt(chestLocation);
            if(mostLikelyAChest.getType().equals(Material.CHEST)){
                Chest chest = (Chest) mostLikelyAChest.getState();
                if(!chest.getPersistentDataContainer().has(ChestManager.reservedKey, PersistentDataType.BYTE)){
                    chest.getPersistentDataContainer().set(ChestManager.reservedKey, PersistentDataType.BYTE, (byte) 0);
                }
                if(chest.getPersistentDataContainer().get(ChestManager.reservedKey, PersistentDataType.BYTE).equals((byte) 0)){
                    GameStarter.startGame(queue.get(0), queue.get(1), chest, ranked);
                    queue.remove(0);
                    queue.remove(0);
                    break;
                }
            }else{
                ChestManager.chests.remove(mostLikelyAChest);
            }
        }
    }
}
