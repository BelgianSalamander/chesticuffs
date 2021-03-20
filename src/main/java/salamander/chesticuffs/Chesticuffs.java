package salamander.chesticuffs;

import org.bukkit.plugin.java.JavaPlugin;
import salamander.chesticuffs.commands.ExitGame;
import salamander.chesticuffs.commands.RegisterInventory;
import salamander.chesticuffs.commands.RegisterItem;
import salamander.chesticuffs.commands.SwitchToInv;
import salamander.chesticuffs.events.*;
import salamander.chesticuffs.game.ChesticuffsGame;
import salamander.chesticuffs.inventory.ChestKeys;
import salamander.chesticuffs.inventory.ItemHandler;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public final class Chesticuffs extends JavaPlugin {
    private static File itemsFile;

    public static File getItemsFile() {
        return itemsFile;
    }

    private static JavaPlugin plugin;
    private static Map<String, ChesticuffsGame> games = new HashMap<>();
    @Override
    public void onEnable() {
        plugin = this;
        itemsFile = new File(getDataFolder(), "items.json");
        if(!itemsFile.exists()){
            saveResource("items.json", false);
        }
        ItemHandler.init(); //ItemHandler loads items from iteminfo/items.json
        ChestKeys.init(); //Create NamespacedKeys for chest Persistent Data Container

        //Register commands
        getPlugin().getCommand("registeritem").setExecutor(new RegisterItem());
        getPlugin().getCommand("registerinventory").setExecutor(new RegisterInventory());
        getServer().getPluginManager().registerEvents(new OnPickUp(), this);
        getPlugin().getCommand("switchtoinv").setExecutor(new SwitchToInv());
        getPlugin().getCommand("exitgame").setExecutor(new ExitGame());

        //Register events
        getServer().getPluginManager().registerEvents(new CreateItem(), this);
        getServer().getPluginManager().registerEvents(new OnChestClick(), this);
        getServer().getPluginManager().registerEvents(new OnClickInInventory(), this);
        getServer().getPluginManager().registerEvents(new OnInventoryExit(), this);
        getServer().getPluginManager().registerEvents(new OnPlayerJoin(), this);
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }

    static public JavaPlugin getPlugin(){
        return plugin;
    }

    static public Map<String, ChesticuffsGame> getGames(){
        return games;
    }

    static public ChesticuffsGame getGame(String id){
        return games.get(id);
    }

    static public void addNewGame(String id, ChesticuffsGame game){
        games.put(id, game);
    }
}
