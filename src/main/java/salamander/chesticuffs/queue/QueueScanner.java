package salamander.chesticuffs.queue;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataType;
import salamander.chesticuffs.ChestManager;
import salamander.chesticuffs.Chesticuffs;
import salamander.chesticuffs.worlds.GameStarter;

import java.util.LinkedList;
import java.util.List;
import java.util.Random;

public class QueueScanner implements Runnable{
    //Will run every second
    public void run(){
        Random rand = new Random();
        if(!Chesticuffs.isQueueActive()) return; //Return immediately if queue isn't active
        List<ChesticuffsQueue> queuesWithTwoPlayersOrMore = new LinkedList<>();
        int totalPlayers = 0;

        for(ChesticuffsQueue queue : QueueHandler.queues){
            if(queue.getPlayersInQueue().size() >= 2){
                queuesWithTwoPlayersOrMore.add(queue);
                totalPlayers += queue.getPlayersInQueue().size();
            }
        }

        if(queuesWithTwoPlayersOrMore.size() == 0) return;

        double randomN = rand.nextDouble() * totalPlayers;
        double total = 0.0;
        ChesticuffsQueue queue = null;

        for(ChesticuffsQueue possibleQueue : queuesWithTwoPlayersOrMore){
            total += possibleQueue.getPlayersInQueue().size();
            if(randomN < total){
                queue = possibleQueue;
                break;
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
                    GameStarter.startGame(queue.getPlayersInQueue().get(0), queue.getPlayersInQueue().get(1), chest, queue.isRanked(), queue.getLengthOfCollectionPhase());
                    queue.getPlayersInQueue().remove(0);
                    queue.getPlayersInQueue().remove(0);
                    break;
                }
            }else{
                ChestManager.chests.remove(mostLikelyAChest);
            }
        }
    }
}
