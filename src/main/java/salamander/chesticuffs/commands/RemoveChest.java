package salamander.chesticuffs.commands;

import org.bukkit.ChatColor;
import org.bukkit.Color;
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
            sender.sendMessage("You do not have permission to run this command!");
            return true;
        }

        if(!(sender instanceof Player)){
            sender.sendMessage(ChatColor.RED + "Only players can execute this command");
            return true;
        }

        if(!command.getName().equalsIgnoreCase("removechest")) return true;
        Player player = (Player) sender;
        try
        {
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
        }
        catch(NullPointerException e)
        {
            sender.sendMessage("You must be looking at a chest to run this command!");
        }

        return true;
    }
}
