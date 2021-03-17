package salamander.chesticuffs.game;

import net.kyori.adventure.text.Component;
import org.bukkit.ChatColor;
import org.bukkit.block.Chest;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import salamander.chesticuffs.inventory.ChestKeys;

import java.util.ArrayList;
import java.util.List;

public class ChestHandler {
    static public void updateStickInfo(Chest chest){
        Inventory inv = chest.getBlockInventory();
        short phaseNumber = (short) 0;//chest.getPersistentDataContainer().get(ChestKeys.phaseNumberKey, PersistentDataType.SHORT);
        ItemStack firstStick = inv.getItem(4);
        ItemMeta meta = firstStick.getItemMeta();
        List<Component> lore = new ArrayList<Component>();
        switch(phaseNumber) {
            case (0):
                meta.displayName(Component.text(ChatColor.DARK_GRAY + "Phase 0:"));
                lore.add(Component.text( ChatColor.BOLD + "" + ChatColor.DARK_GRAY + "Core Placement"));
                break;
            case(1):
                meta.displayName(Component.text(ChatColor.DARK_GRAY + "Phase 1:"));
                lore.add(Component.text(ChatColor.BOLD + "" + ChatColor.DARK_GRAY + "Item Placement"));
                break;
            case(2):
                meta.displayName(Component.text(ChatColor.DARK_GRAY + "Phase 2:"));
                lore.add(Component.text(ChatColor.BOLD + "" + ChatColor.DARK_GRAY + "Declare Attackers"));
                break;
            case(3):
                meta.displayName(Component.text(ChatColor.DARK_GRAY + "Phase 3:"));
                lore.add(Component.text(ChatColor.BOLD + "" + ChatColor.DARK_GRAY + "Declare Defenders"));
                break;
            case(4):
                meta.displayName(Component.text(ChatColor.DARK_GRAY + "Phase 4:"));
                lore.add(Component.text(ChatColor.BOLD + "" + ChatColor.DARK_GRAY + "Closing Phase"));
                break;
        }
        meta.lore(lore);
        firstStick.setItemMeta(meta);

    }
}
