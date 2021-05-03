package salamander.chesticuffs.commands;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;
import salamander.chesticuffs.queue.ChesticuffsQueue;
import salamander.chesticuffs.queue.QueueHandler;

public class Queues implements CommandExecutor {
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        sender.sendMessage(ChatColor.GREEN + "Current Queues:");
        for(ChesticuffsQueue queue : QueueHandler.queues){
            if(queue.isModOnly() && !sender.isOp()) continue;
            sender.sendMessage("  -" + queue.getNames().get(0)  + " (" + queue.getLengthOfCollectionPhase() + "m) " + (queue.isRanked() ? ChatColor.RED + " RANKED ": ChatColor.GREEN + " CASUAL ") + ChatColor.GOLD + "" + queue.getPlayersInQueue().size() + " Players Waiting");
        }
        return true;
    }
}
