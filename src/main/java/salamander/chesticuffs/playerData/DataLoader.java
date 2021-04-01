package salamander.chesticuffs.playerData;

import com.destroystokyo.paper.profile.PlayerProfile;
import net.kyori.adventure.text.Component;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Sign;
import org.bukkit.block.Skull;
import org.bukkit.block.data.type.WallSign;
import org.bukkit.entity.Player;
import org.bukkit.block.data.Directional;
import salamander.chesticuffs.Chesticuffs;
import salamander.chesticuffs.LeagueTuple;

import java.io.*;
import java.util.*;
import java.util.List;

public class DataLoader {
    private static Map<UUID, PlayerData> data = new HashMap<>();
    private static List<LeagueTuple> leagues = new LinkedList<>();
    public static final World lobby = Bukkit.getWorld("lobby");
    private static final Location[] locations = {
            new Location(lobby, -1, 200, -86),
            new Location(lobby, 3, 200, -86),
            new Location(lobby, -3, 196, -86),
            new Location(lobby, 1, 196, -86),
            new Location(lobby, 5, 196, -86)
    };
    private static final PlayerData badPlayer = new PlayerData(-69420, -69420, 69420, 0, 0L, 0L, 0L);

    public static List<LeagueTuple> getLeagues() {
        return leagues;
    }

    private static class AutoSave implements Runnable{
        @Override
        public void run() {
            saveData();
        }
    }

    public static Map<UUID, PlayerData> getData() {
        return data;
    }

    public static void addPlayer(Player player){
        data.put(player.getUniqueId(), new PlayerData());
    }

    public static void loadData(){
        try{
            FileInputStream fileIn = new FileInputStream(Chesticuffs.getPlayerFile());
            ObjectInputStream in = new ObjectInputStream(fileIn);
            data = (Map<UUID, PlayerData>) in.readObject();
            in.close();
            fileIn.close();
        }catch(IOException e){
            e.printStackTrace();
            data.clear();
            saveData();
            try{
                FileInputStream fileIn = new FileInputStream(Chesticuffs.getPlayerFile());
                ObjectInputStream in = new ObjectInputStream(fileIn);
                data = (Map<UUID, PlayerData>) in.readObject();
                in.close();
                fileIn.close();
            }catch(IOException e2){
                e2.printStackTrace();
            }catch(ClassNotFoundException e2){
                e2.printStackTrace();
            }
        }catch(ClassNotFoundException e){
            e.printStackTrace();
            data.clear();
            saveData();
            try{
                FileInputStream fileIn = new FileInputStream(Chesticuffs.getPlayerFile());
                ObjectInputStream in = new ObjectInputStream(fileIn);
                data = (Map<UUID, PlayerData>) in.readObject();
                in.close();
                fileIn.close();
            }catch(IOException e2){
                e2.printStackTrace();
            }catch(ClassNotFoundException e2){
                e2.printStackTrace();
            }
        }

        Bukkit.getConsoleSender().sendMessage("Successfully loaded player data for " + data.size() + " players!");
        Bukkit.getScheduler().runTaskTimer(Chesticuffs.getPlugin(), new AutoSave(), 6000, 6000);
        updatePercentiles();
    }

    public static void saveData(){
        try {
            FileOutputStream outFile = new FileOutputStream(Chesticuffs.getPlayerFile());//FileOutputStream(Chesticuffs.getPlayerFile());
            ObjectOutputStream out = new ObjectOutputStream(outFile);
            out.writeObject(data);
            out.close();
            outFile.close();
        }catch (IOException e){
            e.printStackTrace();
        }
    }

    public static void updatePercentiles(){
        List<Integer> eloRatings = new LinkedList<>();
        int i = 0;
        long currentTime = System.currentTimeMillis();
        for(PlayerData playerData : data.values()){
            if(currentTime - playerData.getLastOnlineAt() < 14515200000L) {
                eloRatings.add(playerData.getEloRating());
                i++;
            }
        }
        leagues.clear();
        if(i == 0){
            System.out.println("No players yet. Leagues cannot be updated!");
            leagues.add(new LeagueTuple("Oxeye Daisy", -69420));
            return;
        }
        Collections.sort(eloRatings, Collections.reverseOrder());

        int oxeyeIndex = 0;
        int netheriteIndex = (int) Math.min(Math.floor(i/100), 10);
        int diamondIndex = (int) Math.min(Math.floor(i/20), 50);
        int emeraldIndex = i/10;
        int redstoneIndex = i/5;
        int lapisIndex = i*3/10;
        int goldIndex = i*2/5;
        int ironIndex = i/2;
        int bronzeIndex = i*3/5;
        int coalIndex = i*4/5;
        int woodIndex = i-1;

        leagues.add(new LeagueTuple("Oxeye Daisy", eloRatings.get(oxeyeIndex)));
        leagues.add(new LeagueTuple("Netherite", eloRatings.get(netheriteIndex)));
        leagues.add(new LeagueTuple("Diamond", eloRatings.get(diamondIndex)));
        leagues.add(new LeagueTuple("Emerald", eloRatings.get(emeraldIndex)));
        leagues.add(new LeagueTuple("Redstone", eloRatings.get(redstoneIndex)));
        leagues.add(new LeagueTuple("Lapis", eloRatings.get(lapisIndex)));
        leagues.add(new LeagueTuple("Gold", eloRatings.get(goldIndex)));
        leagues.add(new LeagueTuple("Iron", eloRatings.get(ironIndex)));
        leagues.add(new LeagueTuple("Bronze", eloRatings.get(bronzeIndex)));
        leagues.add(new LeagueTuple("Coal", eloRatings.get(coalIndex)));
        leagues.add(new LeagueTuple("Wood", eloRatings.get(woodIndex)));
        Leagues.init(i);
    }

    public static boolean isBetter(PlayerData playerOne, PlayerData playerTwo){
        if(playerOne.getEloRating() > playerTwo.getEloRating()){
            return true;
        }else if(playerOne.getEloRating() < playerTwo.getEloRating()) {
            return false;
        }

        if(playerOne.getLastWonAt() > playerTwo.getLastWonAt()){
            return true;
        }else if(playerOne.getLastWonAt() < playerTwo.getLastWonAt()){
            return false;
        }

        return false;
    }

    private static class UpdateHeads implements Runnable{
        OfflinePlayer[] players;
        PlayerData[] playerData;
        public UpdateHeads(OfflinePlayer[] players, PlayerData[] data){
            this.players = players;
            this.playerData = data;
        }

        @Override
        public void run() {
            for(int i = 0; i < 5; i++){
                Block block = locations[i].getBlock();
                Location signLocation = locations[i].clone();
                signLocation.setX(signLocation.getX()-1);
                Block sign = signLocation.getBlock();
                if(players[i] != null){
                    block.setType(Material.PLAYER_WALL_HEAD);
                    Directional data = (Directional) block.getBlockData();
                    data.setFacing(BlockFace.SOUTH);
                    block.setBlockData(data);
                    Skull skull = (Skull) block.getState();
                    skull.setOwningPlayer(players[i]);
                    skull.update();

                    sign.setType(Material.BIRCH_WALL_SIGN);
                    Sign actualSign = (Sign) sign.getState();
                    String name = players[i].getName();
                    if (name != null) {
                        actualSign.line(1, Component.text(players[i].getName()));
                    }else{
                        actualSign.line(1, Component.text("[Unknown]"));
                    }
                    actualSign.line(2, Component.text(playerData[i].getEloRating() + " Elo"));
                    actualSign.update();

                    WallSign signData = (WallSign) sign.getBlockData();
                    signData.setFacing(BlockFace.SOUTH);
                    sign.setBlockData(signData);

                }else{
                    block.setType(Material.AIR);
                    sign.setType(Material.AIR);
                }
            }
        }
    }

    public static void updateLeaderboard(){
        Bukkit.getScheduler().runTaskAsynchronously(Chesticuffs.getPlugin() ,
            new Runnable(){
                @Override
                public void run() {
                    UUID[] topFivePlayers = {null, null, null, null, null};
                    PlayerData[] topFiveScores = {badPlayer, badPlayer, badPlayer, badPlayer, badPlayer};
                    for(Map.Entry<UUID, PlayerData> entry : data.entrySet()){
                        if(isBetter(entry.getValue(), topFiveScores[4])){
                            if(isBetter(entry.getValue(), topFiveScores[3])){
                                topFiveScores[4] = topFiveScores[3];

                                topFivePlayers[4] = topFivePlayers[3];

                                if(isBetter(entry.getValue(), topFiveScores[2])){
                                    topFiveScores[3] = topFiveScores[2];

                                    topFivePlayers[3] = topFivePlayers[2];

                                    if(isBetter(entry.getValue(), topFiveScores[1])){
                                        topFiveScores[2] = topFiveScores[1];

                                        topFivePlayers[2] = topFivePlayers[1];

                                        if(isBetter(entry.getValue(), topFiveScores[0])){
                                            topFiveScores[1] = topFiveScores[0];
                                            topFiveScores[0] = entry.getValue();

                                            topFivePlayers[1] = topFivePlayers[0];
                                            topFivePlayers[0] = entry.getKey();
                                        }else{
                                            topFiveScores[1] = entry.getValue();

                                            topFivePlayers[1] = entry.getKey();
                                        }
                                    }else{
                                        topFiveScores[2] = entry.getValue();

                                        topFivePlayers[2] = entry.getKey();
                                    }
                                }else{
                                    topFiveScores[3] = entry.getValue();

                                    topFivePlayers[3] = entry.getKey();
                                }
                            }else{
                                topFiveScores[4] = entry.getValue();

                                topFivePlayers[4] = entry.getKey();
                            }
                        }
                    }
                    OfflinePlayer[] players = {
                            Bukkit.getOfflinePlayer(topFivePlayers[0]),
                            Bukkit.getOfflinePlayer(topFivePlayers[1]),
                            Bukkit.getOfflinePlayer(topFivePlayers[2]),
                            Bukkit.getOfflinePlayer(topFivePlayers[3]),
                            Bukkit.getOfflinePlayer(topFivePlayers[4])
                    };
                    Bukkit.getScheduler().runTask(Chesticuffs.getPlugin(), new UpdateHeads(players, topFiveScores));
                }
            }
        );
    }
}
