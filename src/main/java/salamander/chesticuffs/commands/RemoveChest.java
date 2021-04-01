package salamander.chesticuffs.commands;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;
import salamander.chesticuffs.ChestManager;

public class RemoveChest implements CommandExecutor {
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if(!sender.isOp()){
            return true;
        }
        if(!command.getName().equalsIgnoreCase("removechest")) return true;
        Player player = (Player) sender;
        Block block =  player.getTargetBlock(5);
        if(block.getType().equals(Material.CHEST)){
            Location location = block.getLocation();
            if(ChestManager.chests.contains(location.toBlockLocation())){
                ChestManager.chests.remove(block.getLocation());
                Chest chest = (Chest) player.getWorld().getBlockAt(location).getState();
                chest.getPersistentDataContainer().remove(ChestManager.reservedKey);
                chest.update();
                player.sendMessage("Chest removed!");
            }else{
                sender.sendMessage("That chest isn't selected!");
            }
        }else{
            player.sendMessage("This isn't a chest!");
            return true;
        }
        return true;
    }
}
