package salamander.chesticuffs.commands;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import salamander.chesticuffs.Chesticuffs;

public class OpenInv implements CommandExecutor {
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {

        if(!sender.isOp()){
            sender.sendMessage(ChatColor.RED + "You do not have permission to run this command!");
            return true;
        }
        if(!(sender instanceof Player)){
            sender.sendMessage(ChatColor.RED + "Only players can execute this command");
            return true;
        }

        String key = args[0];
        Player player = (Player)sender;

        Block block =  player.getTargetBlock(5);
        if(!block.getLocation().getWorld().equals(player.getWorld()))
        {
            player.sendMessage("Please select a block in the lobby!");
            return true;
        }

        if(block.getType().equals(Material.CHEST))
        {
            Location location = block.getLocation();
        }

        //TODO This command does not work / is not usable

        if(args[1].equalsIgnoreCase("red")){
            player.openInventory(Chesticuffs.getGame(key).getPlayerOneInventory());
        }else if(args[1].equalsIgnoreCase("blue")){
            player.openInventory(Chesticuffs.getGame(key).getPlayerTwoInventory());
        }
        return true;
    }
}
