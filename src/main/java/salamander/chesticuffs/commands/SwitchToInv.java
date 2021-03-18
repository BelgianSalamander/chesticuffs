package salamander.chesticuffs.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.jetbrains.annotations.NotNull;
import salamander.chesticuffs.Chesticuffs;
import salamander.chesticuffs.game.ChesticuffsGame;

public class SwitchToInv implements CommandExecutor {
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if(!(sender instanceof Player)){
            sender.sendMessage("Only players can run this command!");
            return true;
        }

        Player player = (Player) sender;

        if(command.getName().equalsIgnoreCase("switchtoinv")){
            ChesticuffsGame game = Chesticuffs.getGame(args[0]);
            Inventory inv;
            if(args[1].equals("1")){
                inv = game.getPlayerOneInventory();
            }else{
                inv = game.getPlayerTwoInventory();
            }
            player.openInventory(inv);
        }
        return true;
    }
}
