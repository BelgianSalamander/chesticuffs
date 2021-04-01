package salamander.chesticuffs.commands;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import salamander.chesticuffs.playerData.DataLoader;

public class ResetPlayer implements CommandExecutor {
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if(!sender.isOp()) return true;

        if(!command.getName().equalsIgnoreCase("resetplayer")) return true;

        Player player = Bukkit.getPlayer(args[0]);
        DataLoader.addPlayer(player);

        return true;
    }
}
