package salamander.chesticuffs.events;

import org.bukkit.Material;
import org.bukkit.block.Chest;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import salamander.chesticuffs.game.ChestHandler;
import salamander.chesticuffs.game.GameID;
import salamander.chesticuffs.inventory.ChestKeys;
import salamander.chesticuffs.inventory.ItemHandler;

public class OnChestClick implements Listener {
    //Detects when players click on chest and when a game should start. The final code is probably gonna be very different
    @EventHandler
    public void OnClick(PlayerInteractEvent e){
        if(!e.getAction().equals(Action.RIGHT_CLICK_BLOCK)) return;
        if(!e.getClickedBlock().getType().equals(Material.CHEST)) return;
        Chest chest = (Chest) e.getClickedBlock().getState();
        PersistentDataContainer chestData = chest.getPersistentDataContainer();
        System.out.println("Chest Clicked");
        if(!chestData.has(ChestKeys.playersInChestKey, PersistentDataType.SHORT)){
            System.out.println("Chest Clicked For The First Time");
            chestData.set(ChestKeys.playersInChestKey, PersistentDataType.SHORT, (short) 1);
            chest.update();
        }else if(chestData.get(ChestKeys.playersInChestKey, PersistentDataType.SHORT).equals((short) 1)){
            chestData.set(ChestKeys.playersInChestKey, PersistentDataType.SHORT, (short) 2);
            chest.getSnapshotInventory().setItem(4, new ItemStack(Material.STICK, 1));
            chest.getSnapshotInventory().setItem(13, new ItemStack(Material.STICK, 1));
            chest.getSnapshotInventory().setItem(22, new ItemStack(Material.STICK, 1));
            System.out.println("Chest Clicked For The Second Time");
            chestData.set(ChestKeys.idKey, PersistentDataType.STRING, GameID.next());
            chest.update();
            ChestHandler.updateStickInfo(chest);
        }
    }
}
