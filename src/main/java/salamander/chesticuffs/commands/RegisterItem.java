package salamander.chesticuffs.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import salamander.chesticuffs.Chesticuffs;
import salamander.chesticuffs.inventory.ItemHandler;

public class RegisterItem implements CommandExecutor {
    //Will register the item the player is holding
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if(!(sender instanceof Player)){
            System.out.println("Only players can run this command!");
            return true;
        }
        Player player = (Player) sender;
        if(command.getName().equalsIgnoreCase("registeritem")){
            ItemHandler.registerItem(player.getInventory().getItemInMainHand());
        }
        return true;
    }
}
