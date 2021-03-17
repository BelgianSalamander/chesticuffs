package salamander.chesticuffs;

import org.bukkit.plugin.java.JavaPlugin;
import salamander.chesticuffs.commands.RegisterInventory;
import salamander.chesticuffs.commands.RegisterItem;
import salamander.chesticuffs.events.CreateItem;
import salamander.chesticuffs.events.OnChestClick;
import salamander.chesticuffs.events.OnPickUp;
import salamander.chesticuffs.inventory.ChestKeys;
import salamander.chesticuffs.inventory.ItemHandler;

public final class Chesticuffs extends JavaPlugin {

    private static JavaPlugin plugin;

    @Override
    public void onEnable() {
        plugin = this;
        ItemHandler.init(); //ItemHandler loads items from iteminfo/items.json
        ChestKeys.init(); //Create NamespacedKeys for chest Persistent Data Container
        getPlugin().getCommand("registeritem").setExecutor(new RegisterItem());

        //Register commands
        getPlugin().getCommand("registerinventory").setExecutor(new RegisterInventory());
        getServer().getPluginManager().registerEvents(new OnPickUp(), this);

        //Register events
        getServer().getPluginManager().registerEvents(new CreateItem(), this);
        getServer().getPluginManager().registerEvents(new OnChestClick(), this);
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }

    static public JavaPlugin getPlugin(){
        return plugin;
    }
}
