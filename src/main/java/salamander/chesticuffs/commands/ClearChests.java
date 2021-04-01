package salamander.chesticuffs.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;
import salamander.chesticuffs.ChestManager;

import java.util.LinkedList;

public class ClearChests implements CommandExecutor {
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if(!sender.isOp()){
            sender.sendMessage("You do not have permission to run this command!");
            return true;
        }

        if(command.getName().equalsIgnoreCase("ClearChests")){
            ChestManager.chests = new LinkedList<>();
            sender.sendMessage("Success!");
        }

        return true;
    }
}
