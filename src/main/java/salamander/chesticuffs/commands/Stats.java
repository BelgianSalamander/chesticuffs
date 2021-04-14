package salamander.chesticuffs.commands;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import salamander.chesticuffs.playerData.DataLoader;

public class Stats implements CommandExecutor {
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if(!command.getName().equalsIgnoreCase("stats")) return true;
        if(args.length > 0){
            DataLoader.getData().get(Bukkit.getOfflinePlayerIfCached(args[0]).getUniqueId()).displayStatsTo((Player) sender);
            return true;
        }
        if(!(sender instanceof Player)){
            return true;
        }
        DataLoader.getData().get(((Player) sender).getUniqueId()).displayStatsTo((Player) sender);
        return true;
    }
}
