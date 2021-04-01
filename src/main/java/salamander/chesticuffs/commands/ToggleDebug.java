package salamander.chesticuffs.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;
import salamander.chesticuffs.Chesticuffs;

public class ToggleDebug implements CommandExecutor {
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        Chesticuffs.isDebugMode = !Chesticuffs.isDebugMode;
        sender.sendMessage(Chesticuffs.isDebugMode ? "Debug mode enabled" : "Debug mode disabled");
        return true;
    }
}
