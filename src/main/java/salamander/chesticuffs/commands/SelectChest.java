package salamander.chesticuffs.commands;

import org.bukkit.ChatColor;
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
import salamander.chesticuffs.Chesticuffs;

public class SelectChest implements CommandExecutor {
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if(!command.getName().equalsIgnoreCase("SelectChest"))
        if(!sender.isOp()){
            sender.sendMessage(ChatColor.RED + "You do not have permission to run this command");
            return true;
        }
        if(!(sender instanceof Player)){
            sender.sendMessage(ChatColor.RED + "Only players can execute this command");
            return true;
        }

        Player player = (Player) sender;

        if(args.length >= 3){
            Location location = new Location(player.getWorld(), Double.parseDouble(args[0]), Double.parseDouble(args[1]), Double.parseDouble(args[2]));
            if(player.getWorld().getBlockAt(location).getType().equals(Material.CHEST)){
                if(!ChestManager.chests.contains(location.toBlockLocation())) {
                    ChestManager.chests.add(location);
                    sender.sendMessage("Chest added!");
                    Chest chest = (Chest) player.getWorld().getBlockAt(location).getState();
                    chest.getPersistentDataContainer().set(ChestManager.reservedKey, PersistentDataType.BYTE, (byte) 0);
                    chest.update();
                }else{
                    sender.sendMessage("That chest is already selected!");
                }
            }else{
                sender.sendMessage("This is not a chest!");
            }
        }else{
            Block block =  player.getTargetBlock(5);
            if(!block.getLocation().getWorld().equals(player.getWorld())){
                player.sendMessage("Please select a block in the lobby!");
                return true;
            }
            if(block.getType().equals(Material.CHEST)){
                Location location = block.getLocation();
                if(!ChestManager.chests.contains(location.toBlockLocation())){
                    ChestManager.chests.add(block.getLocation());
                    Chest chest = (Chest) player.getWorld().getBlockAt(location).getState();
                    chest.getPersistentDataContainer().set(ChestManager.reservedKey, PersistentDataType.BYTE, (byte) 0);
                    chest.update();
                    player.sendMessage("Chest added!");
                }else{
                    sender.sendMessage("That chest is already selected!");
                }
            }else{
                player.sendMessage("This isn't a chest!");
                return true;
            }
        }
        return true;
    }
}
