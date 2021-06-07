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
        if(!sender.isOp()){
            sender.sendMessage("You do not have permission to run this command!");
            return true;
        }
        //TODO This command does not work

        String key = args[0];
        Player player = (Player)sender;
        if(args[1].equalsIgnoreCase("red")){
            player.openInventory(Chesticuffs.getGame(key).getPlayerOneInventory());
        }else if(args[1].equalsIgnoreCase("blue")){
            player.openInventory(Chesticuffs.getGame(key).getPlayerTwoInventory());
        }
        return true;
    }
}
