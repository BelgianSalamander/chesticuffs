package salamander.chesticuffs.commands;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

public class SetPlayerLimit implements CommandExecutor {
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if(!sender.isOp()){
            sender.sendMessage(ChatColor.RED + "You can't do that!");
            return true;
        }
        if(args.length == 0){
            sender.sendMessage(ChatColor.RED + "Please provide a number!");
            return true;
        }

        Bukkit.setMaxPlayers(Integer.valueOf(args[0]));
        sender.sendMessage("Success!");
        return true;
    }
}
