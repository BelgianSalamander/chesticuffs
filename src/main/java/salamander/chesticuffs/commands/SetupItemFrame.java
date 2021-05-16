package salamander.chesticuffs.commands;

import org.apache.commons.lang.ObjectUtils;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Chest;
import org.bukkit.command.BlockCommandSender;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

public class SetupItemFrame implements CommandExecutor {
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if(!sender.isOp()){
            sender.sendMessage("You do not have permission to run this command!");
            return true;
        }
        int x = Integer.valueOf(args[0]);
        int y = Integer.valueOf(args[1]);
        int side = Integer.valueOf(args[2]);
        World world;
        if(sender instanceof BlockCommandSender){
            world = ((BlockCommandSender) sender).getBlock().getWorld();
        }
        else if(sender instanceof Player){
            world = ((Player) sender).getWorld();
        }else{
            return true;
        }
        Chest chest = (Chest) world.getBlockAt(0, 199, 0).getState();
        Location location = null;
        if(side == 0) {
            location = new Location(world, 4.5 - x, 209.5 - y, -1.03125);
        }else if(side == 1){
            location = new Location(world, -3.5 + x, 209.5 - y, 2.03125);
        }
        
        //Setting here to be accessible outside of try-catch.
        ItemFrame frame = null;
        try
        {
             frame = (ItemFrame) location.getNearbyEntities(0.25, 0.25, 0.25).toArray()[0];
        }
        catch(NullPointerException e)
        {
            sender.sendMessage("NullPointerException!  No local entities detected! Exception message: " + e.getMessage());
        }
        
        ItemStack item = chest.getBlockInventory().getItem(y * 9 + x);
        if(item == null){
            frame.setItem(null, false);
        }else{
            frame.setItem(item.asOne(), false);
        }
        return true;
    }
}
