package salamander.chesticuffs.queue;

import org.bukkit.entity.Player;

import java.util.LinkedList;
import java.util.List;

public class ChesticuffsQueue {
    private List<String> names;

    public List<String> getNames() {
        return names;
    }

    public boolean isRanked() {
        return isRanked;
    }

    public boolean isModOnly() {
        return isModOnly;
    }

    public int getLengthOfCollectionPhase() {
        return lengthOfCollectionPhase;
    }

    private boolean isRanked;
    private boolean isModOnly;
    private int lengthOfCollectionPhase;

    public List<Player> getPlayersInQueue() {
        return playersInQueue;
    }

    private List<Player> playersInQueue;

    public ChesticuffsQueue(List<String> names, boolean isRanked, boolean isModOnly, int lengthOfCollectionPhase){
        this.names = names;
        this.isRanked = isRanked;
        this.isModOnly = isModOnly;
        this.lengthOfCollectionPhase = lengthOfCollectionPhase;

        playersInQueue = new LinkedList<>();
    }
}
