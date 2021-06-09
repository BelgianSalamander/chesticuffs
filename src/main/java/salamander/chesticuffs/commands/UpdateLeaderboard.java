package salamander.chesticuffs.commands;

import net.md_5.bungee.api.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;
import salamander.chesticuffs.Chesticuffs;
import salamander.chesticuffs.playerData.DataLoader;

public class UpdateLeaderboard implements CommandExecutor {
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if(!sender.isOp()){
            sender.sendMessage(ChatColor.RED + "You do not have permission to run this command!");
            return true;
        }
        DataLoader.updateLeaderboard();
        Chesticuffs.discordManager.updateMemberRoles();
        return true;
    }
}
