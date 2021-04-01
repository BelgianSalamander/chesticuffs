package salamander.chesticuffs.commands;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;
import salamander.chesticuffs.Chesticuffs;
import salamander.chesticuffs.game.ChesticuffsGame;

public class JoinQueue implements CommandExecutor {
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if(!(sender instanceof Player)){
            sender.sendMessage("Only players can execute this command!");
            return true;
        }
        Player player = (Player) sender;
        if(command.getName().equalsIgnoreCase("JoinQueue")){
            if(player.getPersistentDataContainer().get(ChesticuffsGame.playerInGameKey, PersistentDataType.BYTE) == null){
                player.getPersistentDataContainer().set(ChesticuffsGame.playerInGameKey, PersistentDataType.BYTE, (byte) 0);
            }
            if(player.getPersistentDataContainer().get(ChesticuffsGame.playerInGameKey, PersistentDataType.BYTE) == (byte) 1){
                sender.sendMessage(ChatColor.RED + "You are already in a game!");
                return true;
            }
            if(Chesticuffs.rankedQueue.contains(player) || Chesticuffs.unrankedQueue.contains(player)){
                sender.sendMessage(ChatColor.RED + "You are already in a queue!");
                return true;
            }else{
                if(args.length == 0){
                    sender.sendMessage(ChatColor.RED + "Please say which queue you want [ranked/casual]");
                    return true;
                }
                if(args[0].equalsIgnoreCase("ranked") || args[0].equalsIgnoreCase("competitive")){
                    Chesticuffs.rankedQueue.add(player);
                }else if(args[0].equalsIgnoreCase("friendly") || args[0].equalsIgnoreCase("unranked") || args[0].equalsIgnoreCase("casual")){
                    Chesticuffs.unrankedQueue.add(player);
                }else{
                    sender.sendMessage(ChatColor.RED + "Please provide a valid queue! (ranked/causal)");
                    return true;
                }
                sender.sendMessage(ChatColor.GREEN + "You have been added to the " + args[0] + " queue!");
                return true;
            }
        }
        return true;
    }
}
