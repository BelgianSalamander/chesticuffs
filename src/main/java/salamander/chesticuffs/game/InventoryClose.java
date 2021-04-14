package salamander.chesticuffs.game;

import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

import java.util.ArrayList;

public class InventoryClose implements Runnable{
    Inventory inv;
    public InventoryClose(Inventory inv){
        this.inv = inv;
    }
    @Override
    public void run() {
        for(HumanEntity human : new ArrayList<>(inv.getViewers())){
            Player player = (Player) human;
            player.closeInventory();
        }
    }
}
