package salamander.chesticuffs.inventory;

import org.bukkit.NamespacedKey;
import salamander.chesticuffs.Chesticuffs;

public class ChestKeys {
    static public NamespacedKey playersInChestKey, roundNumber, phaseNumber;

    static public void init(){
        playersInChestKey = new NamespacedKey(Chesticuffs.getPlugin(), "players_in_chest");
    }
}
