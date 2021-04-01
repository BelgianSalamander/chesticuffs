package salamander.chesticuffs.toolbar;

import net.kyori.adventure.text.Component;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class ToolbarItems {
    public static ItemStack stats, watchArena;

    static public void init(){
        stats = new ItemStack(Material.COMMAND_BLOCK, 1);
        ItemMeta statsMeta = stats.getItemMeta();
        statsMeta.displayName(Component.text(ChatColor.GREEN + "Stats"));
        stats.setItemMeta(statsMeta);

        watchArena = new ItemStack(Material.CHEST, 1);
        ItemMeta watchArenaMeta = watchArena.getItemMeta();
        watchArenaMeta.displayName(Component.text("Watch Game"));
        watchArena.setItemMeta(watchArenaMeta);
    }

    static public void setupPlayer(Player player){
        player.getInventory().clear();
        player.getInventory().setItem(34, watchArena);
        player.getInventory().setItem(35, stats);
        player.updateInventory();
    }
}
