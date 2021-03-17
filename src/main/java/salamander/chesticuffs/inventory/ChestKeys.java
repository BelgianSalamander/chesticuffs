package salamander.chesticuffs.inventory;

import org.bukkit.NamespacedKey;
import salamander.chesticuffs.Chesticuffs;

public class ChestKeys {
    static public NamespacedKey idKey, playersInChestKey;

    static public void init(){
        idKey = new NamespacedKey(Chesticuffs.getPlugin(), "GameID");
        playersInChestKey = new NamespacedKey(Chesticuffs.getPlugin(), "PlayersInChest");
    }
}
