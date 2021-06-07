package salamander.chesticuffs.prototype;

import net.minecraft.server.v1_16_R3.ChunkCoordIntPair;
import net.minecraft.server.v1_16_R3.ChunkGenerator;
import org.bukkit.craftbukkit.v1_16_R3.CraftWorld;

import java.lang.reflect.Field;
import java.util.List;

public class StrongholdModifier {
    private final List<ChunkCoordIntPair> strongholds;

    static Field strongholdsField;

    public StrongholdModifier(org.bukkit.World world){
        List<ChunkCoordIntPair> strongholdsTemp;
        ChunkGenerator generator = ((CraftWorld) world).getHandle().getChunkProvider().getChunkGenerator();
        if(generator == null){
            System.out.println("Generator is null :(");
        }
        try {
            strongholdsTemp = (List<ChunkCoordIntPair>) strongholdsField.get(generator);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
            strongholdsTemp = null;
        }
        this.strongholds = strongholdsTemp;
    }

    public void addChunk(int chunkX, int chunkZ){
        strongholds.add(new ChunkCoordIntPair(chunkX, chunkZ));
    }

    static {
        try {
            strongholdsField = ChunkGenerator.class.getDeclaredField("f");
            strongholdsField.setAccessible(true);
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        }
    }
}
