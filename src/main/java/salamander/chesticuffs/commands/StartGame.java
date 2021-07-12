package salamander.chesticuffs.commands;

import org.bukkit.Bukkit;
import org.bukkit.block.Chest;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import salamander.chesticuffs.ChestManager;
import salamander.chesticuffs.worlds.GameStarter;

public class StartGame implements CommandExecutor {
    //Temporary so doesn't throw an error
    @Override
    public boolean onCommand(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String[] strings) {
        return false;
    }
    /*@Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {

        if(!sender.isOp()){
            sender.sendMessage("You do not have permission to run this command!");
            return true;
        }

        if(command.getName().equalsIgnoreCase("StartGame")){
            Chest chest;

            try
            {
                //change to get block from player facing
                chest = (Chest) ((Player) sender).getWorld().getBlockAt(Integer.parseInt(args[2]), Integer.parseInt(args[3]), Integer.parseInt(args[4])).getState();
            }
            catch(Exception e)
            {
                sender.sendMessage("You must be looking at a chest");
                return true;
            }
            if(!ChestManager.chests.contains(chest.getLocation())){
                sender.sendMessage("That chest is not registered!");
                return true;
            }
            if(args.length == 0) {
                GameStarter.startGame(Bukkit.getPlayer(args[0]), Bukkit.getPlayer(args[1]), chest, true, Integer.parseInt(args[6]));
            }else{
                GameStarter.startGame(Bukkit.getPlayer(args[0]), Bukkit.getPlayer(args[1]), chest, args[5].equalsIgnoreCase("unranked") || args[5].equalsIgnoreCase("friendly"), Integer.parseInt(args[6]));
            }
        }
        return true;
    }*/
}
