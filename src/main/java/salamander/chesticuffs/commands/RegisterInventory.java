package salamander.chesticuffs.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;
import salamander.chesticuffs.inventory.ItemHandler;

public class RegisterInventory implements CommandExecutor {
    //Will register the whole inventory of the player executing the command
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if(!(sender instanceof Player)) return true;
        Player player = (Player) sender;
        if(command.getName().equals("registerinventory")) {
            registerPlayersInventory(player);
        }
        return true;
    }

    public static void registerPlayersInventory(Player player){
        boolean hasCore = false;
        for (ItemStack item : player.getInventory().getContents()) {
            ItemHandler.registerItem(item);
            if (item != null) {
                if (item.getItemMeta() != null && (!hasCore)){
                    String itemType = item.getItemMeta().getPersistentDataContainer().get(ItemHandler.getTypeKey(), PersistentDataType.STRING);
                    if(itemType==null){
                        continue;
                    }
                    if (itemType.equals("core")){
                        hasCore = true;
                    }
                }
            }
        }

        if(!hasCore){
            player.getInventory().addItem(ItemHandler.baseCore);
        }
    }
}
