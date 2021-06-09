package salamander.chesticuffs.commands;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;
import salamander.chesticuffs.Chesticuffs;
import salamander.chesticuffs.inventory.ItemHandler;

public class ToggleLogging implements CommandExecutor {
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if(!sender.isOp()){
            sender.sendMessage(ChatColor.RED + "You do not have permission to run this command!");
            return true;
        }
        Chesticuffs.log = !Chesticuffs.log;
        sender.sendMessage(ChatColor.GREEN + "Successfully " + (Chesticuffs.log ? "enabled" : "disabled") + " chesticuffs debug console logger");
        return true;
    }
}
