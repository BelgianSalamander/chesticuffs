package salamander.chesticuffs.commands;

import org.bukkit.ChatColor;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;
import salamander.chesticuffs.Chesticuffs;
import salamander.chesticuffs.game.ChesticuffsGame;
import salamander.chesticuffs.game.GameID;
import salamander.chesticuffs.inventory.ChestKeys;

public class NewDebugGame implements CommandExecutor {
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if(!sender.isOp()){
            sender.sendMessage("You do not have permission to run this command!");
            return true;
        }

        if(!Chesticuffs.isDebugMode){
            sender.sendMessage(ChatColor.RED + "Not in debug mode!");
            return true;
        }
        Player player = (Player) sender;
        Block block = player.getTargetBlock(5);
        Chest chest = (Chest) block.getState();
        String key = GameID.next();
        ChesticuffsGame game = new ChesticuffsGame(player, chest, key, false);
        game.addPlayer(player);
        Chesticuffs.addNewGame(key, game);
        chest.getPersistentDataContainer().set(ChestKeys.idKey, PersistentDataType.STRING, key);
        chest.update();
        return true;
    }
}
