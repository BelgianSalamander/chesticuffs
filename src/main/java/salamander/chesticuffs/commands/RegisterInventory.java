package salamander.chesticuffs.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import salamander.chesticuffs.inventory.ItemHandler;

public class RegisterInventory implements CommandExecutor {
    //Will register the whole inventory of the player executing the command
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if(!(sender instanceof Player)) return true;
        Player player = (Player) sender;
        if(command.getName().equals("registerinventory")) {
            for (ItemStack item : player.getInventory().getContents()) {
                ItemHandler.registerItem(item);
            }
        }
        return true;
    }
}
