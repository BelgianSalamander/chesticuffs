package salamander.chesticuffs;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.persistence.PersistentDataType;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import salamander.chesticuffs.inventory.ItemHandler;

import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedList;
import java.util.List;

public class ChestManager {
    static public List<Location> chests = new LinkedList<>();
    static public NamespacedKey reservedKey = new NamespacedKey(Chesticuffs.getPlugin(), "reserved");
    static public void init(){
        JSONParser parser = new JSONParser();
        JSONArray JSONChests;
        JSONObject JSONdata = null;
        try{
            InputStream is = new FileInputStream(Chesticuffs.getChestsFile());
            String data = ItemHandler.readFromInputStream(is);
            JSONdata = (JSONObject) parser.parse(data);
        }catch (ParseException e) {
            e.printStackTrace();
        } catch (IOException e){
            e.printStackTrace();
        }
        JSONChests = (JSONArray) JSONdata.get("chests");
        for (Object coordinates : JSONChests){
            JSONArray JSONCoords = (JSONArray) coordinates;
            Location chestLocation = new Location(Bukkit.getWorld((String) JSONCoords.get(3)), (Long) JSONCoords.get(0), (Long) JSONCoords.get(1), (Long) JSONCoords.get(2));
            Block block =  Bukkit.getWorld((String) JSONCoords.get(3)).getBlockAt(chestLocation);
            if(block.getType().equals(Material.CHEST)){
                chests.add(chestLocation.toBlockLocation());
                Chest chest = (Chest) block.getState();
                chest.getPersistentDataContainer().set(ChestManager.reservedKey, PersistentDataType.BYTE, (byte) 0 );
                chest.update();

            }
        }

        if(chests.size() == 0){
            Bukkit.getServer().getConsoleSender().sendMessage("No chests are selected! Make sure to select a few with /selectchest!");
        }
    }

    static public void saveChests(){
        JSONObject chestObject = new JSONObject();
        JSONArray chestArray = new JSONArray();
        for(Location location : chests){
            JSONArray locationArray = new JSONArray();
            locationArray.add(location.getBlockX());
            locationArray.add(location.getBlockY());
            locationArray.add(location.getBlockZ());
            locationArray.add(location.getWorld().getName());
            chestArray.add(locationArray);
        }
        chestObject.put("chests", chestArray);
        try {
            FileWriter jsonWriter = new FileWriter(Chesticuffs.getChestsFile().getAbsolutePath());
            jsonWriter.write(chestObject.toJSONString());
            jsonWriter.close();
        } catch (IOException e){
            e.printStackTrace();
        }
    }
}
