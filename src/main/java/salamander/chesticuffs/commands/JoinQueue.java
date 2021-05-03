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
import salamander.chesticuffs.queue.ChesticuffsQueue;
import salamander.chesticuffs.queue.QueueHandler;

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
            for(ChesticuffsQueue queue : QueueHandler.queues) {
                if (queue.getPlayersInQueue().contains(player)) {
                    sender.sendMessage(ChatColor.RED + "You are already in a queue!");
                    return true;
                }
            }
            if(args.length == 0){
                sender.sendMessage(ChatColor.RED + "Please say which queue you want. You can check the queues with /queues");
                return true;
            }
            boolean addedToQueue = false;
            for(ChesticuffsQueue queue1 : QueueHandler.queues){
                if(queue1.getNames().contains(args[0].toLowerCase())){
                    if(queue1.isModOnly()){
                        if(!player.isOp()){
                            player.sendMessage(ChatColor.RED + "You do not have the required permissions to join this queue!");
                            return true;
                        }
                    }
                    queue1.getPlayersInQueue().add(player);
                    addedToQueue = true;
                }
            }
            if(!addedToQueue){
                sender.sendMessage(ChatColor.RED + "Please provide a valid queue! (ranked/causal)");
                return true;
            }
            if(Chesticuffs.isQueueActive()) {
                sender.sendMessage(ChatColor.GREEN + "You have been added to the " + args[0] + " queue!");
            } else{
                sender.sendMessage(ChatColor.RED + "You have been added to the " + args[0] + "queue, however, the queue system is currently disabled and you will not be placed in a game. " +
                        "If you believe this is an error, please contact the staff team.");
            }
            return true;
        }
        return true;
    }
}
