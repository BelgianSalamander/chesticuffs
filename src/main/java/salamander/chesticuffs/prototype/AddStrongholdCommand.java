package salamander.chesticuffs.prototype;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;
import salamander.chesticuffs.worlds.WorldHandler;

public class AddStrongholdCommand implements CommandExecutor {

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        WorldHandler.addStronghold(Integer.valueOf(args[0]), Integer.valueOf(args[1]));
        return true;
    }
}
