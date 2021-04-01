package salamander.chesticuffs;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import salamander.chesticuffs.playerData.DataLoader;
import salamander.chesticuffs.commands.*;
import salamander.chesticuffs.events.*;
import salamander.chesticuffs.game.ChesticuffsGame;
import salamander.chesticuffs.inventory.ChestKeys;
import salamander.chesticuffs.inventory.ItemHandler;
import salamander.chesticuffs.toolbar.ToolbarItems;
import salamander.chesticuffs.worlds.WorldHandler;

import java.io.File;
import java.util.*;

public final class Chesticuffs extends JavaPlugin {
    private static File itemsFile, chestsFile, playerFile;
    static public List<Player> rankedQueue = new LinkedList<>();
    static public List<Player> unrankedQueue = new LinkedList<>();
    static private boolean queueActive = true;
    static public boolean isDebugMode = false;
    public static int K = 40;

    public static File getPlayerFile() {
        return playerFile;
    }
    //}

    public static File getItemsFile() {
        return itemsFile;
    }

    public static File getChestsFile(){
        return chestsFile;
    }

    public static void setQueueActive(boolean queueActive) {
        Chesticuffs.queueActive = queueActive;
    }

    public static boolean isQueueActive() {
        return queueActive;
    }

    private static JavaPlugin plugin;
    private static Map<String, ChesticuffsGame> games = new HashMap<>();
    @Override
    public void onEnable() {
        plugin = this;

        itemsFile = new File(getPlugin().getDataFolder(), "items.json");
        if(!itemsFile.exists()){
            saveResource("items.json", false);
        }

        chestsFile = new File(getPlugin().getDataFolder(), "chests.json");
        if(!chestsFile.exists()){
            saveResource("chests.json", false);
        }

        playerFile = new File(getPlugin().getDataFolder(), "players.ser");
        if(!playerFile.exists()){
            //saveResource("players.ser", false);
            DataLoader.saveData();
        }

        ItemHandler.init(); //ItemHandler loads items from items.json
        ChestManager.init(); //Loads chests
        ChestKeys.init(); //Create NamespacedKeys for chest Persistent Data Container
        WorldHandler.init(); //Create worlds
        DataLoader.loadData();
        ToolbarItems.init();

        getPlugin().getCommand("selectchest").setExecutor(new SelectChest());
        getPlugin().getCommand("clearchests").setExecutor(new ClearChests());
        getPlugin().getCommand("startgame").setExecutor(new StartGame());
        getPlugin().getCommand("joinqueue").setExecutor(new JoinQueue());
        getPlugin().getCommand("stats").setExecutor(new Stats());
        getPlugin().getCommand("removechest").setExecutor(new RemoveChest());
        getPlugin().getCommand("setframe").setExecutor(new SetupItemFrame());
        getPlugin().getCommand("resetplayer").setExecutor(new ResetPlayer());
        getPlugin().getCommand("updateleaderboard").setExecutor(new UpdateLeaderboard());
        getPlugin().getCommand("setlimit").setExecutor(new SetPlayerLimit());
        getPlugin().getCommand("togglequeue").setExecutor(new ToggleQueue());
        getPlugin().getCommand("toggledebug").setExecutor(new ToggleDebug());
        getPlugin().getCommand("debuggame").setExecutor(new NewDebugGame());
        getPlugin().getCommand("openinv").setExecutor(new OpenInv());
        getPlugin().getCommand("exitgame").setExecutor(new ExitGame());

        //Register events
        getServer().getPluginManager().registerEvents(new OnPickUp(), this);
        getServer().getPluginManager().registerEvents(new CreateItem(), this);
        getServer().getPluginManager().registerEvents(new OnChestClick(), this);
        getServer().getPluginManager().registerEvents(new OnClickInInventory(), this);
        getServer().getPluginManager().registerEvents(new OnInventoryExit(), this);
        getServer().getPluginManager().registerEvents(new OnPlayerJoin(), this);
        getServer().getPluginManager().registerEvents(new OnPlayerLeave(), this);
        getServer().getPluginManager().registerEvents(new OnPortal(), this);

        getServer().getScheduler().runTaskTimer(this, new QueueScanner(), 200, 20);
        getServer().getScheduler().runTaskTimer(this, new updateDataAndStuff(), 1200, 1200);
        getServer().getConsoleSender().sendMessage("Queue Scanner will start in 10 seconds");
    }

    @Override
    public void onDisable() {
        ChestManager.saveChests();
        DataLoader.saveData();
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

    private static class updateDataAndStuff implements Runnable{
        @Override
        public void run(){
            DataLoader.updatePercentiles();
            DataLoader.updateLeaderboard();
        }
    }
}
