package salamander.chesticuffs.worlds;

import org.bukkit.*;
import org.bukkit.block.Biome;
import salamander.chesticuffs.prototype.StrongholdModifier;

import java.io.File;
import java.util.Random;

public class WorldHandler {
    public static World getCollectionWorldOne() {
        return collectionWorldOne;
    }

    public static World getCollectionWorldTwo() {
        return collectionWorldTwo;
    }

    static World collectionWorldOne;
    static World collectionWorldTwo;
    static World netherWorldOne;
    static World netherWorldTwo;

    static StrongholdModifier worldOneStrongholdModifier;
    static StrongholdModifier worldTwoStrongholdModifier;

    public static World getNetherWorldOne() {
        return netherWorldOne;
    }

    public static World getNetherWorldTwo() {
        return netherWorldTwo;
    }

    static double max = 29000000.0;
    static double min = -29000000.0;

    static public void init(){
        if(Bukkit.getWorld("world_one") != null){
            File worldOnePath = Bukkit.getServer().getWorld("world_one").getWorldFolder();
            deleteWorld(worldOnePath);
            Bukkit.unloadWorld("world_one", false);

            File worldTwoPath = Bukkit.getServer().getWorld("world_two").getWorldFolder();
            deleteWorld(worldTwoPath);
            Bukkit.unloadWorld("world_two", false);
        }

        if(Bukkit.getWorld("world_one_nether") != null){
            File worldOnePath = Bukkit.getServer().getWorld("world_one_nether").getWorldFolder();
            deleteWorld(worldOnePath);
            Bukkit.unloadWorld("world_one_nether", false);

            File worldTwoPath = Bukkit.getServer().getWorld("world_two_nether").getWorldFolder();
            deleteWorld(worldTwoPath);
            Bukkit.unloadWorld("world_two_nether", false);
        }

        Random rand = new Random();
        long seed = rand.nextLong();

        WorldCreator worldOneCreator = new WorldCreator("world_one");
        WorldCreator worldTwoCreator = new WorldCreator("world_two");
        WorldCreator worldOneCreatorNether = new WorldCreator("world_one_nether");
        WorldCreator worldTwoCreatorNether = new WorldCreator("world_two_nether");

        worldOneCreatorNether.environment(World.Environment.NETHER);
        worldTwoCreatorNether.environment(World.Environment.NETHER);

        worldOneCreator.seed(seed);
        worldTwoCreator.seed(seed);
        worldOneCreatorNether.seed(seed);
        worldTwoCreatorNether.seed(seed);

        collectionWorldOne =  Bukkit.createWorld(worldOneCreator);
        collectionWorldTwo =  Bukkit.createWorld(worldTwoCreator);
        netherWorldOne = Bukkit.createWorld(worldOneCreatorNether);
        netherWorldTwo = Bukkit.createWorld(worldTwoCreatorNether);

        collectionWorldOne.setGameRule(GameRule.KEEP_INVENTORY, true);
        collectionWorldTwo.setGameRule(GameRule.KEEP_INVENTORY, true);

        collectionWorldOne.setTime(0);
        collectionWorldTwo.setTime(0);

        worldOneStrongholdModifier = new StrongholdModifier(collectionWorldOne);
        worldTwoStrongholdModifier = new StrongholdModifier(collectionWorldTwo);
    }

    static private boolean deleteWorld(File path) {
        if(path.exists()) {
            File files[] = path.listFiles();
            for(int i=0; i<files.length; i++) {
                if(files[i].isDirectory()) {
                    deleteWorld(files[i]);
                } else {
                    files[i].delete();
                }
            }
        }
        return(path.delete());
    }

    static public void addStronghold(int chunkX, int chunkZ){
        worldTwoStrongholdModifier.addChunk(chunkX, chunkZ);
        worldOneStrongholdModifier.addChunk(chunkX, chunkZ);
    }

    static public void createStrongholdAroundPosition(int chunkX, int chunkZ, int minDistance, int maxDistance){
        Random random = new Random();
        double t = 2 * Math.PI * random.nextDouble();
        double u = random.nextDouble() + random.nextDouble();
        double r = u > 1 ? 2 - u : u;
        r = r * (maxDistance - minDistance) / maxDistance + minDistance;
        chunkX = (int) (chunkX + r * Math.cos(t));
        chunkZ = (int) (chunkZ + r * Math.sin(t));
        int origChunkX = chunkX;
        int origChunkZ = chunkZ;
        while(!isValidStrongholdPosition(chunkX, chunkZ) && (Math.abs(chunkZ - origChunkZ) + Math.abs(chunkX - origChunkX) < 20)){
            chunkZ++;
            chunkX--;
        }

        if(!isValidStrongholdPosition(chunkX, chunkZ) && (Math.abs(chunkZ - origChunkZ) + Math.abs(chunkX - origChunkX) >= 20)){
            chunkZ = origChunkZ;
            chunkX = origChunkX;
        }

        addStronghold(chunkX, chunkZ);
    }

    static private boolean isValidStrongholdPosition(int chunkX, int chunkY){
        if(collectionWorldOne.isChunkGenerated(chunkX, chunkY)){
            return false;
        }
        Biome biome = collectionWorldOne.getBiome(chunkX * 16,30, chunkY * 16);
        if(biome.equals(Biome.DEEP_OCEAN) || biome.equals(Biome.DEEP_COLD_OCEAN) || biome.equals(Biome.DEEP_FROZEN_OCEAN) || biome.equals(Biome.DEEP_WARM_OCEAN) || biome.equals(Biome.DEEP_LUKEWARM_OCEAN)){
            return false;
        }
        return true;
    }

    static public Location generateGameStartingPosition() {
        Random random = new Random();
        Material below = null;
        Location location = null;
        int x = (int) (random.nextInt((int) (max-min)) + min);
        int z = (int) (random.nextInt((int) (max-min)) + min);
        int y = 255;
        while (below == null || !(below.isSolid())) {
            x += 16;//(int) (random.nextInt((int) (max-min)) + min);
            //z = (int) (random.nextInt((int) (max-min)) + min);
            location = new Location(collectionWorldOne, x, collectionWorldOne.getHighestBlockYAt(x,z), z);
            y = (int) (location.getY() + 1);
            below = collectionWorldOne.getBlockAt(location).getType();
        }
        location.setY(y);
        return location;
    }
}
