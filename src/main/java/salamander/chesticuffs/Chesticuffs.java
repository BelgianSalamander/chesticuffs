package salamander.chesticuffs;

import org.bukkit.plugin.java.JavaPlugin;
import salamander.chesticuffs.playerData.DataLoader;
import salamander.chesticuffs.commands.*;
import salamander.chesticuffs.events.*;
import salamander.chesticuffs.game.ChesticuffsGame;
import salamander.chesticuffs.inventory.ChestKeys;
import salamander.chesticuffs.inventory.ItemHandler;
import salamander.chesticuffs.queue.QueueHandler;
import salamander.chesticuffs.queue.QueueScanner;
import salamander.chesticuffs.toolbar.ToolbarItems;
import salamander.chesticuffs.worlds.WorldHandler;

import javax.security.auth.login.LoginException;
import java.io.File;
import java.util.*;

public final class Chesticuffs extends JavaPlugin {
    private static File itemsFile, chestsFile, playerFile, queuesFile, tokenFile, discordLinksFile;
    static private boolean queueActive = true;
    static public boolean isDebugMode = false;
    static public Discord discordManager;
    public static int K = 40;

    public static File getPlayerFile() {
        return playerFile;
    }

    public static File getItemsFile() {
        return itemsFile;
    }

    public static File getDiscordLinksFile() {
        return discordLinksFile;
    }

    public static File getChestsFile(){
        return chestsFile;
    }

    public static File getQueuesFile() { return queuesFile; }

    public static void setQueueActive(boolean queueActive) {
        Chesticuffs.queueActive = queueActive;
    }

    public static boolean isQueueActive() {
        return queueActive;
    }

    private static JavaPlugin plugin;
    private static Map<String, ChesticuffsGame> games = new HashMap<>();

    public static File getTokenFile() {
        return tokenFile;
    }

    @Override
    public void onEnable() {
        plugin = this;

        //Creates file object for items
        itemsFile = new File(getPlugin().getDataFolder(), "items.json");
        if(!itemsFile.exists()){
            saveResource("items.json", false);
        }

        //Creates file object for the current registered chests
        chestsFile = new File(getPlugin().getDataFolder(), "chests.json");
        if(!chestsFile.exists()){
            saveResource("chests.json", false);
        }

        //Create file object for the player data
        playerFile = new File(getPlugin().getDataFolder(), "players.ser");
        if(!playerFile.exists()){
            DataLoader.saveData(); //Serialises an empty player data storage (does not generate an empty file)
        }

        queuesFile = new File(getPlugin().getDataFolder(), "queues.json");
        if(!queuesFile.exists()){
            saveResource("queues.json", false);
        }

        tokenFile = new File(getPlugin().getDataFolder(), "token");
        if(!tokenFile.exists()) {
            saveResource("token", false);
        }

        discordLinksFile = new File(getPlugin().getDataFolder(), "discord.json");
        if(!discordLinksFile.exists()) {
            saveResource("discord.json", false);
        }


        ItemHandler.init(); //ItemHandler loads items from items.json
        ChestManager.init(); //Loads chests
        ChestKeys.init(); //Create NamespacedKeys for chest Persistent Data Container
        //WorldHandler.init(); //Create worlds TODO Uncomment before release!
        DataLoader.loadData(); //Loads PlayerData
        ToolbarItems.init(); //Initialises items for a toolbar item menu (currently not in use)
        QueueHandler.init();

        getPlugin().getCommand("selectchest").setExecutor(new SelectChest()); //Adds the chest you are looking at to the registered chests list
        getPlugin().getCommand("clearchests").setExecutor(new ClearChests()); //Clears the registered chests list
        getPlugin().getCommand("startgame").setExecutor(new StartGame()); //Used by admins to force a game to start between two players
        getPlugin().getCommand("joinqueue").setExecutor(new JoinQueue()); //Used by players to join a queue
        getPlugin().getCommand("stats").setExecutor(new Stats()); //Used to view your and other player's stats
        getPlugin().getCommand("removechest").setExecutor(new RemoveChest()); //Removes the chest you are looking at from the registered chests list
        getPlugin().getCommand("setframe").setExecutor(new SetupItemFrame()); //Used in the jumbo-tron
        getPlugin().getCommand("resetplayer").setExecutor(new ResetPlayer()); //Completely resets a player's stats
        getPlugin().getCommand("updateleaderboard").setExecutor(new UpdateLeaderboard()); //Forces the leaderboard to update (automatically happens every minute)
        getPlugin().getCommand("setlimit").setExecutor(new SetPlayerLimit()); //Update the player limit
        getPlugin().getCommand("togglequeue").setExecutor(new ToggleQueue()); //Enables and disables the queue system. People will still be able to join, however, they will not be placed into games
        getPlugin().getCommand("toggledebug").setExecutor(new ToggleDebug()); //Enables and disables debug mode. Used to test a new feature by yourself, allows you to play games against yourself
        getPlugin().getCommand("debuggame").setExecutor(new NewDebugGame()); //Creates a game with yourself
        getPlugin().getCommand("openinv").setExecutor(new OpenInv()); //Open's the red or blue inventory of a game from a given gameID
        getPlugin().getCommand("exitgame").setExecutor(new ExitGame()); //Remove's you from your current game
        getPlugin().getCommand("queues").setExecutor(new Queues());

        //Register events
        getServer().getPluginManager().registerEvents(new OnPickUp(), this); //Registers items when players pick them up
        getServer().getPluginManager().registerEvents(new CreateItem(), this); //Registers items when crafted/smelted
        getServer().getPluginManager().registerEvents(new OnChestClick(), this); //Used to spectate games (previously used to start games)
        getServer().getPluginManager().registerEvents(new OnClickInInventory(), this); //Sends an inventory click event to the correct game object
        getServer().getPluginManager().registerEvents(new OnInventoryExit(), this);
        getServer().getPluginManager().registerEvents(new OnPlayerJoin(), this);
        getServer().getPluginManager().registerEvents(new OnPlayerLeave(), this);
        getServer().getPluginManager().registerEvents(new OnPortal(), this); //Prevents portals from being made
        getServer().getPluginManager().registerEvents(new OnPlayerDeath(), this); //Gives status effects back on death
        getServer().getPluginManager().registerEvents(new TeleportEvent(), this); //Gives correct effects and gamemode each time someone is teleported
        getServer().getPluginManager().registerEvents(new PlayerSleep(), this);
        getServer().getPluginManager().registerEvents(new PortalEvent(), this);

        getServer().getScheduler().runTaskTimer(this, new QueueScanner(), 200, 20); //Schedules queue scanner to run every second
        //getServer().getScheduler().runTaskTimer(this, new updateDataAndStuff(), 1200, 1200); //Updates percentiles and leaderboard every minute
        getServer().getConsoleSender().sendMessage("Queue Scanner will start in 10 seconds");

        //discordManager = new Discord();
    }

    @Override
    public void onDisable() {
        ChestManager.saveChests();
        DataLoader.saveData();
        discordManager.save();
        discordManager.stop();
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
            DataLoader.updatePercentiles(); //Updates the lower-bound elo for every rank
            DataLoader.updateLeaderboard();
            discordManager.updateMemberRoles();
        }
    }
}
