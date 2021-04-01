package salamander.chesticuffs.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import salamander.chesticuffs.Chesticuffs;

public class OpenInv implements CommandExecutor {
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        String key = args[0];
        if(args[1].equalsIgnoreCase("red")){
            ((Player) sender).openInventory(Chesticuffs.getGame(key).getPlayerOneInventory());
        }else{
            ((Player) sender).openInventory(Chesticuffs.getGame(key).getPlayerTwoInventory());
        }
        return true;
    }
}
