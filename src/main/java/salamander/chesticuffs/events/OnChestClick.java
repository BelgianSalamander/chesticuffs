package salamander.chesticuffs.events;

import org.bukkit.Material;
import org.bukkit.block.Chest;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import salamander.chesticuffs.inventory.ChestKeys;
import salamander.chesticuffs.inventory.ItemHandler;

public class OnChestClick implements Listener {
    @EventHandler
    public void OnClick(PlayerInteractEvent e){
        if(!e.getAction().equals(Action.RIGHT_CLICK_BLOCK)) return;
        if(!e.getClickedBlock().getType().equals(Material.CHEST)) return;
        Chest chest = (Chest) e.getClickedBlock();
        PersistentDataContainer chestData = chest.getPersistentDataContainer();
        if(!chestData.has(ChestKeys.playersInChestKey, PersistentDataType.SHORT)){
            chestData.set(ChestKeys.playersInChestKey, PersistentDataType.SHORT, (short) 1);
        }
    }
}
