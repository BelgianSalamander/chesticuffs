package salamander.chesticuffs;

import org.bukkit.plugin.java.JavaPlugin;
import salamander.chesticuffs.commands.RegisterInventory;
import salamander.chesticuffs.commands.RegisterItem;
import salamander.chesticuffs.events.CreateItem;
import salamander.chesticuffs.events.OnPickUp;
import salamander.chesticuffs.inventory.ChestKeys;
import salamander.chesticuffs.inventory.ItemHandler;

public final class Chesticuffs extends JavaPlugin {

    private static JavaPlugin plugin;

    @Override
    public void onEnable() {
        plugin = this;
        ItemHandler.init();
        ChestKeys.init();
        getPlugin().getCommand("registeritem").setExecutor(new RegisterItem());
        getPlugin().getCommand("registerinventory").setExecutor(new RegisterInventory());
        getServer().getPluginManager().registerEvents(new OnPickUp(), this);
        getServer().getPluginManager().registerEvents(new CreateItem(), this);
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }

    static public JavaPlugin getPlugin(){
        return plugin;
    }
}
