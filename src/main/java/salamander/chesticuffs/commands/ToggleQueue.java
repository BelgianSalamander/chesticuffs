package salamander.chesticuffs.commands;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;
import salamander.chesticuffs.Chesticuffs;

public class ToggleQueue implements CommandExecutor {
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        Chesticuffs.setQueueActive(!Chesticuffs.isQueueActive());
        if(Chesticuffs.isQueueActive()){
            sender.sendMessage(ChatColor.GREEN + "Enabled Queue");
        }else{
            sender.sendMessage(ChatColor.GREEN + "Disabled Queue");
        }
        return true;
    }
}
