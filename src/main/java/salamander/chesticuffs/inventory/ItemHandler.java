package salamander.chesticuffs.inventory;


import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import salamander.chesticuffs.Chesticuffs;
import salamander.chesticuffs.commands.RegisterItem;

import java.io.*;
import java.util.*;

public class ItemHandler {
    public static NamespacedKey getTypeKey() {
        return typeKey;
    }

    public static NamespacedKey getDamageKey() {
        return damageKey;
    }

    public static NamespacedKey getDefenceKey() {
        return defenceKey;
    }

    public static NamespacedKey getHealthKey() {
        return healthKey;
    }

    public static NamespacedKey getFlavorKey() {
        return flavorKey;
    }

    public static NamespacedKey getTraitsKey() {
        return traitsKey;
    }

    public static NamespacedKey getSideKey() {
        return sideKey;
    }

    public static NamespacedKey getBuffKey() {
        return buffKey;
    }

    public static NamespacedKey getDebuffKey() {
        return debuffKey;
    }

    static NamespacedKey typeKey, damageKey, defenceKey, healthKey, flavorKey, traitsKey, sideKey, buffKey, debuffKey;
    static JSONObject itemData;
    static public ItemStack baseCore;


    static public void init(){
        initKeys();
        //Load iteminfo from JSON file
        //The loading from file code is copied from the internet
        JSONParser parser = new JSONParser();
        InputStream is = ItemHandler.class.getResourceAsStream("/iteminfo/items.json");
        try{
            String data = readFromInputStream(is);
            itemData = (JSONObject) parser.parse(data);
        } catch (ParseException e) {
            e.printStackTrace();
        } catch (IOException e){
            e.printStackTrace();
        }
        System.out.println(itemData.values());
        JSONObject newItemData = (JSONObject) itemData.clone();

        //Create duplicate info for "similar" items for easy access
        Iterator<JSONObject> values = itemData.values().iterator();
        while(values.hasNext()){
            JSONObject item = values.next();
            JSONArray others = (JSONArray) item.get("similar");
            if(others == null) {
                continue;
            }
            item.remove("similar");
            Iterator<String> similarItems = others.iterator();
            while(similarItems.hasNext()){
                String similarItem = similarItems.next();
                newItemData.put(similarItem, item.clone());
            }
        }
        itemData = newItemData;

        baseCore = new ItemStack(Material.DIRT, 1);
        ItemMeta meta = baseCore.getItemMeta();

        meta.getPersistentDataContainer().set(typeKey, PersistentDataType.STRING, "core");
        meta.getPersistentDataContainer().set(healthKey, PersistentDataType.SHORT, (short) 15);
        meta.getPersistentDataContainer().set(flavorKey, PersistentDataType.STRING, "Default Core");
        meta.getPersistentDataContainer().set(buffKey, PersistentDataType.STRING, "");
        meta.getPersistentDataContainer().set(debuffKey, PersistentDataType.STRING, "");
        baseCore.setItemMeta(meta);
        setLore(baseCore);
    }

    static private void initKeys(){
        //Setup all namespacedkeys to store an item's info
        typeKey = new NamespacedKey(Chesticuffs.getPlugin(), "type");
        damageKey = new NamespacedKey(Chesticuffs.getPlugin(), "damage");
        defenceKey = new NamespacedKey(Chesticuffs.getPlugin(), "defence");
        healthKey = new NamespacedKey(Chesticuffs.getPlugin(), "health");
        flavorKey = new NamespacedKey(Chesticuffs.getPlugin(), "flavor");
        traitsKey = new NamespacedKey(Chesticuffs.getPlugin(), "traits");
        sideKey = new NamespacedKey(Chesticuffs.getPlugin(), "side");
        buffKey = new NamespacedKey(Chesticuffs.getPlugin(), "buff");
        debuffKey = new NamespacedKey(Chesticuffs.getPlugin(), "debuff");
    }

    static public void registerItem(ItemStack item){
        //Check if there actually is an item
        if(item == null || item.getType() == Material.AIR) return;
        ItemMeta meta = item.getItemMeta();
        //If item does not have a meta, create one
        if(meta == null) {
            System.out.println(item.getType().toString());
            meta = Bukkit.getServer().getItemFactory().getItemMeta(item.getType());
        }
        if(!meta.getPersistentDataContainer().has(typeKey, PersistentDataType.STRING)){
            if(itemData.containsKey(item.getType().toString())){
                JSONObject itemStats = (JSONObject) itemData.get(item.getType().toString());
                if(itemStats.get("type").equals("item")){
                    //Get stats from json
                    short ATK = (short) (long) itemStats.get("ATK");
                    short DEF = (short) (long) itemStats.get("DEF");
                    short HP = (short) (long) itemStats.get("HP");
                    JSONArray traits = (JSONArray) itemStats.get("traits");
                    String flavor = (String) itemStats.get("flavor");

                    //Give the actual item those stats
                    meta.getPersistentDataContainer().set(typeKey, PersistentDataType.STRING, "item");
                    meta.getPersistentDataContainer().set(damageKey, PersistentDataType.SHORT, ATK);
                    meta.getPersistentDataContainer().set(defenceKey, PersistentDataType.SHORT, DEF);
                    meta.getPersistentDataContainer().set(healthKey, PersistentDataType.SHORT, HP);
                    meta.getPersistentDataContainer().set(traitsKey, PersistentDataType.STRING, String.join(",", traits));
                    meta.getPersistentDataContainer().set(flavorKey, PersistentDataType.STRING, flavor);
                }else if(itemStats.get("type").equals("core")){
                    //Get stats from json
                    short HP = (short) (long) itemStats.get("HP");
                    String buff = (String) itemStats.get("buff");
                    String debuff = (String) itemStats.get("debuff");
                    String flavor = (String) itemStats.get("flavor");

                    //Give the actual item those stats
                    meta.getPersistentDataContainer().set(typeKey, PersistentDataType.STRING, "core");
                    meta.getPersistentDataContainer().set(healthKey, PersistentDataType.SHORT, HP);
                    meta.getPersistentDataContainer().set(flavorKey, PersistentDataType.STRING, flavor);
                    meta.getPersistentDataContainer().set(buffKey, PersistentDataType.STRING, buff);
                    meta.getPersistentDataContainer().set(debuffKey, PersistentDataType.STRING, debuff);
                }
                item.setItemMeta(meta);
                setLore(item);
            }
        }
    }

    static public void setLore(ItemStack item){
        /*Sets the lore for an item. This is a seperate function from registerItem because this will be called
        multiple times when the itme gets changed in a game (e.g it takes damage)*/
        if(item == null){
            return;
        }
        ItemMeta meta = item.getItemMeta();
        if(meta == null){
            return;
        }
        //Check if item is registered. (Unregistered items don't have a "type" key)
        if(!meta.getPersistentDataContainer().has(typeKey, PersistentDataType.STRING)){
            return;
        }

        String type = meta.getPersistentDataContainer().get(typeKey, PersistentDataType.STRING);

        if(type.equals("item")){
            short ATK = meta.getPersistentDataContainer().get(damageKey, PersistentDataType.SHORT);
            short DEF = meta.getPersistentDataContainer().get(defenceKey, PersistentDataType.SHORT);
            short HP = meta.getPersistentDataContainer().get(healthKey, PersistentDataType.SHORT);
            String traits = meta.getPersistentDataContainer().get(traitsKey, PersistentDataType.STRING);
            String flavor = meta.getPersistentDataContainer().get(flavorKey, PersistentDataType.STRING);

            List<Component> lore = new ArrayList<Component>();
            lore.add(Component.text(ChatColor.YELLOW + "Item"));
            lore.add(Component.text(""));
            lore.add(Component.text(ChatColor.GREEN + Short.toString(ATK) + " ATK"));
            lore.add(Component.text(ChatColor.GREEN + Short.toString(DEF) + " DEF"));
            lore.add(Component.text(ChatColor.GREEN + Short.toString(HP) + " HP"));
            lore.add(Component.text(ChatColor.WHITE + "\"" + flavor + "\""));
            if(!traits.equals("")) {
                lore.add(Component.text(""));
                lore.add(Component.text(ChatColor.RED + "" + ChatColor.BOLD + "Traits"));
                for (String trait : traits.split(",")) {
                    lore.add(Component.text(ChatColor.RED + " - " + ChatColor.BOLD + trait));
                }
            }
            meta.lore(lore);
            item.setItemMeta(meta);
        }else if(type.equals("core")){
            System.out.println("Setting lore for core!");
            short HP = meta.getPersistentDataContainer().get(healthKey, PersistentDataType.SHORT);
            String flavor = meta.getPersistentDataContainer().get(flavorKey, PersistentDataType.STRING);
            String buff = meta.getPersistentDataContainer().get(buffKey, PersistentDataType.STRING);
            String debuff = meta.getPersistentDataContainer().get(debuffKey, PersistentDataType.STRING);

            List<Component> lore = new ArrayList<Component>();
            lore.add(Component.text(ChatColor.YELLOW + "Core"));
            lore.add(Component.text(""));
            lore.add(Component.text(ChatColor.DARK_GREEN + Short.toString(HP) + " HP"));
            if(!buff.equals("")){
                lore.add(Component.text(ChatColor.GREEN + "- " + buff));
            }
            if(!debuff.equals("")){
                lore.add(Component.text(ChatColor.RED + "- " + debuff));
            }
            lore.add(Component.text(ChatColor.WHITE + "\"" + flavor + "\""));
            meta.lore(lore);
            item.setItemMeta(meta);
        }
    }

    static private String readFromInputStream(InputStream inputStream)
            throws IOException {
        //From internet
        StringBuilder resultStringBuilder = new StringBuilder();
        try (BufferedReader br
                     = new BufferedReader(new InputStreamReader(inputStream))) {
            String line;
            while ((line = br.readLine()) != null) {
                resultStringBuilder.append(line).append("\n");
            }
        }
        return resultStringBuilder.toString();
    }
}
