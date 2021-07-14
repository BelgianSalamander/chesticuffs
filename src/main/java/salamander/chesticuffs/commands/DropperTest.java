package salamander.chesticuffs.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import salamander.chesticuffs.game.ChesticuffsGame;

public class DropperTest implements CommandExecutor {
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        ((Player) sender).getInventory().addItem(ChesticuffsGame.getDropperItem());
        return true;
    } //This whole command is jsut for testing

}
