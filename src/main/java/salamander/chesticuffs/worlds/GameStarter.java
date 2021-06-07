package salamander.chesticuffs.worlds;

import org.bukkit.*;
import org.bukkit.block.Chest;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;
import salamander.chesticuffs.ChestManager;
import salamander.chesticuffs.Chesticuffs;
import salamander.chesticuffs.inventory.ChestKeys;
import salamander.chesticuffs.playerData.DataLoader;
import salamander.chesticuffs.playerData.PlayerData;
import salamander.chesticuffs.game.ChesticuffsGame;
import salamander.chesticuffs.game.GameID;

import java.util.*;

public class GameStarter {
    static private class SendMessage implements Runnable{
        Player player;
        String message;

        public SendMessage(Player player, String message){
            this.player = player;
            this.message = message;
        }

        @Override
        public void run() {
            player.sendMessage(message);
        }
    }

    static private class BringPlayersToChest implements Runnable{
        Player playerOne, playerTwo;
        Chest chest;
        boolean ranked;

        public BringPlayersToChest(Player playerOne, Player playerTwo, Chest chest, boolean ranked){
            this.playerOne = playerOne;
            this.playerTwo = playerTwo;
            this.chest = chest;
            this.ranked = ranked;
        }

        public void run(){
            String key = playerOne.getPersistentDataContainer().get(gameKey, PersistentDataType.STRING);
            if(!DataLoader.getData().containsKey(playerOne.getUniqueId())) DataLoader.getData().put(playerOne.getUniqueId(), new PlayerData());
            if(!DataLoader.getData().containsKey(playerTwo.getUniqueId())) DataLoader.getData().put(playerTwo.getUniqueId(), new PlayerData());
            playerOne.setHealth(20);
            playerTwo.setHealth(20);
            events.remove(key);
            reservedChests.remove(key);
            Location chestLocation = chest.getLocation();
            chestLocation.setX(chestLocation.getX() + 0.5);
            chestLocation.setZ(chestLocation.getZ() + 0.5);
            Location redLocation = chestLocation.clone();
            Location blueLocation = chestLocation.clone();

            redLocation.setX(redLocation.getX() + 1);
            blueLocation.setX(blueLocation.getX() - 1);
            Random rand = new Random();
            String id = GameID.next();
            ChesticuffsGame game;
            if(rand.nextBoolean()){
                playerOne.sendMessage(ChatColor.RED + "You are red!");
                playerTwo.sendMessage(ChatColor.BLUE + "You are blue!");
                playerOne.teleport(lookAt(blueLocation, chestLocation));
                playerTwo.teleport(lookAt(redLocation, chestLocation));
                game = new ChesticuffsGame(playerOne, chest, id, ranked);
                game.addPlayer(playerTwo);
            }else{
                playerTwo.sendMessage(ChatColor.RED + "You are red!");
                playerOne.sendMessage(ChatColor.BLUE + "You are blue!");
                playerOne.teleport(lookAt(redLocation, chestLocation));
                playerTwo.teleport(lookAt(blueLocation, chestLocation));
                game = new ChesticuffsGame(playerTwo, chest, id, ranked);
                game.addPlayer(playerOne);
            }
            chest.open();
            Chesticuffs.addNewGame(id, game);

            Location worldSpawn = playerOne.getWorld().getSpawnLocation();
            chest.getPersistentDataContainer().set(ChestKeys.idKey, PersistentDataType.STRING, id);
            playerOne.setBedSpawnLocation(worldSpawn, true);
            playerTwo.setBedSpawnLocation(worldSpawn, true);

            playerOne.setGameMode(GameMode.ADVENTURE);
            playerTwo.setGameMode(GameMode.ADVENTURE);
        }
    }

    static public NamespacedKey gameKey = new NamespacedKey(Chesticuffs.getPlugin(), "game");
    static public Map<String, List<BukkitTask>> events = new HashMap<>();
    static public Map<String, Chest> reservedChests = new HashMap<>();
    static private List<BukkitTask> eventsThatMightBeCancelled = new LinkedList<>();

    static public void broadcastMessageIn(String message, long delay, Player playerOne, Player playerTwo){
        eventsThatMightBeCancelled.add(Bukkit.getScheduler().runTaskLater(Chesticuffs.getPlugin(), new SendMessage(playerOne, message), delay));
        eventsThatMightBeCancelled.add(Bukkit.getScheduler().runTaskLater(Chesticuffs.getPlugin(), new SendMessage(playerTwo, message), delay));
    }
    public static Location lookAt(Location loc, Location lookat) {
        //Clone the loc to prevent applied changes to the input loc
        loc = loc.clone();

        // Values of change in distance (make it relative)
        double dx = lookat.getX() - loc.getX();
        double dy = lookat.getY() - loc.getY();
        double dz = lookat.getZ() - loc.getZ();

        // Set yaw
        if (dx != 0) {
            // Set yaw start value based on dx
            if (dx < 0) {
                loc.setYaw((float) (1.5 * Math.PI));
            } else {
                loc.setYaw((float) (0.5 * Math.PI));
            }
            loc.setYaw((float) loc.getYaw() - (float) Math.atan(dz / dx));
        } else if (dz < 0) {
            loc.setYaw((float) Math.PI);
        }

        // Get the distance from dx/dz
        double dxz = Math.sqrt(Math.pow(dx, 2) + Math.pow(dz, 2));

        // Set pitch
        loc.setPitch((float) -Math.atan(dy / dxz));

        // Set values, convert to degrees (invert the yaw since Bukkit uses a different yaw dimension format)
        loc.setYaw(-loc.getYaw() * 180f / (float) Math.PI);
        loc.setPitch(loc.getPitch() * 180f / (float) Math.PI);

        return loc;
    }


    static public void startGame(Player playerOne, Player playerTwo, Chest chest, boolean ranked, int lengthOfGame){
        playerOne.sendMessage("Started game with " + playerTwo.getName());
        playerTwo.sendMessage("Started game with " + playerOne.getName());
        playerOne.setHealth(20);
        playerTwo.setHealth(20);
        playerOne.setFoodLevel(20);
        playerTwo.setFoodLevel(20);
        Location playerOneLocation = WorldHandler.generateGameStartingPosition();
        int chunkX = playerOneLocation.getBlockX() / 16;
        int chunkZ = playerOneLocation.getBlockZ() / 16;
        WorldHandler.createStrongholdAroundPosition(chunkX, chunkZ, 30,60);
        Location playerTwoLocation = playerOneLocation.clone();
        playerTwoLocation.setWorld(Bukkit.getWorld("world_two"));
        playerOne.setBedSpawnLocation(playerOneLocation, true);
        playerTwo.setBedSpawnLocation(playerTwoLocation, true);
        playerOne.teleport(playerOneLocation);
        playerTwo.teleport(playerTwoLocation);
        playerOne.setGameMode(GameMode.SURVIVAL);
        playerTwo.setGameMode(GameMode.SURVIVAL);
        /*playerOne.setExp(0);
        playerOne.setLevel(15);
        playerTwo.setExp(0);
        playerTwo.setLevel(15);*/
        for(PotionEffect potion : playerOne.getActivePotionEffects()){
            playerOne.removePotionEffect(potion.getType());
        }
        for(PotionEffect potion : playerTwo.getActivePotionEffects()){
            playerTwo.removePotionEffect(potion.getType());
        }
        playerOne.addPotionEffect(new PotionEffect(PotionEffectType.DAMAGE_RESISTANCE, 400, 255));
        playerTwo.addPotionEffect(new PotionEffect(PotionEffectType.DAMAGE_RESISTANCE, 400, 255));
        playerOne.addPotionEffect(new PotionEffect(PotionEffectType.SATURATION, lengthOfGame * 60 * 20, 0));
        playerTwo.addPotionEffect(new PotionEffect(PotionEffectType.SATURATION, lengthOfGame * 60 * 20, 0));
        playerOne.getInventory().clear();
        playerTwo.getInventory().clear();

        DataLoader.getData().get(playerOne.getUniqueId()).setGamesPlayed(DataLoader.getData().get(playerOne.getUniqueId()).getGamesPlayed() + 1);
        DataLoader.getData().get(playerTwo.getUniqueId()).setGamesPlayed(DataLoader.getData().get(playerTwo.getUniqueId()).getGamesPlayed() + 1);

        playerOne.getPersistentDataContainer().set(ChesticuffsGame.playerInGameKey, PersistentDataType.BYTE, (byte) 1);
        playerTwo.getPersistentDataContainer().set(ChesticuffsGame.playerInGameKey, PersistentDataType.BYTE, (byte) 1);

        //Schedule Messages
        eventsThatMightBeCancelled.clear();
        eventsThatMightBeCancelled.add(Bukkit.getScheduler().runTask(Chesticuffs.getPlugin(), new SendMessage(playerOne, ChatColor.GREEN + "" + lengthOfGame + " Minute" + (lengthOfGame > 1 ? "s" : "") + " Remain!")));
        eventsThatMightBeCancelled.add(Bukkit.getScheduler().runTask(Chesticuffs.getPlugin(), new SendMessage(playerTwo, ChatColor.GREEN + "" + lengthOfGame + " Minute" + (lengthOfGame > 1 ? "s" : "") +  "Remain!")));

        broadcastMessageIn(ChatColor.RED + "10 Seconds Remain!", 20 * (60 * lengthOfGame - 10), playerOne, playerTwo);
        broadcastMessageIn(ChatColor.DARK_RED + "5 Seconds Remain!", 20 * (60 * lengthOfGame - 5), playerOne, playerTwo);
        broadcastMessageIn(ChatColor.DARK_RED + "4 Seconds Remain!", 20 * (60 * lengthOfGame - 4), playerOne, playerTwo);
        broadcastMessageIn(ChatColor.DARK_RED + "3 Seconds Remain!", 20 * (60 * lengthOfGame - 3), playerOne, playerTwo);
        broadcastMessageIn(ChatColor.DARK_RED + "2 Seconds Remain!", 20 * (60 * lengthOfGame - 2), playerOne, playerTwo);
        broadcastMessageIn(ChatColor.DARK_RED + "1 Second Remains!", 20 * (60 * lengthOfGame - 1), playerOne, playerTwo);

        if(lengthOfGame > 2) {
            broadcastMessageIn(ChatColor.RED + "2 Minutes Remain!", 20 * 60 * lengthOfGame - 120, playerOne, playerTwo);
        }
        if(lengthOfGame > 1) {
            broadcastMessageIn(ChatColor.RED + "1 Minute Remains!", 20 * 60 * lengthOfGame - 60, playerOne, playerTwo);
        }

        int timeLeft = 5;
        while(true){
            if(lengthOfGame > timeLeft){
                ChatColor color;
                double ratio = ((double) timeLeft)/ ((double) lengthOfGame);
                if(ratio >= 0.5) color = ChatColor.GREEN;
                else if(ratio >= 0.3) color = ChatColor.YELLOW;
                else if(ratio >= 0.1) color = ChatColor.GOLD;
                else color = ChatColor.RED;

                broadcastMessageIn(color + "" + timeLeft + " Minutes Remain!", 1200L * (lengthOfGame - timeLeft), playerOne, playerTwo);
            }else{
                break;
            }
            timeLeft += 5;
        }

        eventsThatMightBeCancelled.add(Bukkit.getScheduler().runTaskLater(Chesticuffs.getPlugin(), new BringPlayersToChest(playerOne, playerTwo, chest, ranked), 20 * 60 * lengthOfGame));

        String eventsID = GameID.next();
        playerOne.getPersistentDataContainer().set(gameKey, PersistentDataType.STRING, eventsID);
        playerTwo.getPersistentDataContainer().set(gameKey, PersistentDataType.STRING, eventsID);

        events.put(eventsID, eventsThatMightBeCancelled);
        reservedChests.put(eventsID, chest);

        chest.getPersistentDataContainer().set(ChestManager.reservedKey, PersistentDataType.BYTE, (byte) 1);
        chest.update();

        Bukkit.getConsoleSender().sendMessage("Started a game between " + playerOne.getName() + " and " + playerTwo.getName());
    }
}
