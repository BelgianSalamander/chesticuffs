package salamander.chesticuffs.inventory;


import net.kyori.adventure.text.Component;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import salamander.chesticuffs.Chesticuffs;
import salamander.chesticuffs.MessageLevel;
import salamander.chesticuffs.traits.Trait;
import salamander.chesticuffs.traits.TraitsHolder;

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

    public static NamespacedKey getEffectIDKey() { return effectIDKey; }

    public static NamespacedKey getDescriptionKey() {
        return descriptionKey;
    }

    public static NamespacedKey getUseLimitKey() {
        return useLimitKey;
    }

    public static NamespacedKey getTraitValueKey() {return traitValueKey; }

    static NamespacedKey typeKey, damageKey, defenceKey, healthKey, flavorKey, traitsKey, sideKey, buffKey, debuffKey, effectIDKey, descriptionKey, useLimitKey, traitValueKey, statIncreaseLeftKey;
    static public JSONObject itemData;
    static public ItemStack baseCore;


    static public void init(){

        initKeys();
        //Load iteminfo from JSON file
        //The loading from file code is copied from the internet
        JSONParser parser = new JSONParser();
        try{
            InputStream is = new FileInputStream(Chesticuffs.getItemsFile());
            String data = readFromInputStream(is);
            itemData = (JSONObject) parser.parse(data);
        } catch (ParseException | IOException e) {
            e.printStackTrace();
        }
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

        baseCore = new ItemStack(Material.CRAFTING_TABLE, 1);
        ItemMeta meta = baseCore.getItemMeta();

        meta.getPersistentDataContainer().set(typeKey, PersistentDataType.STRING, "core");
        meta.getPersistentDataContainer().set(healthKey, PersistentDataType.SHORT, (short) 10);
        meta.getPersistentDataContainer().set(flavorKey, PersistentDataType.STRING, "Default Core");
        meta.getPersistentDataContainer().set(buffKey, PersistentDataType.STRING, "");
        meta.getPersistentDataContainer().set(debuffKey, PersistentDataType.STRING, "");
        meta.getPersistentDataContainer().set(effectIDKey, PersistentDataType.INTEGER, 0);
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
        effectIDKey = new NamespacedKey(Chesticuffs.getPlugin(), "effectID");
        descriptionKey = new NamespacedKey(Chesticuffs.getPlugin(), "description");
        useLimitKey = new NamespacedKey(Chesticuffs.getPlugin(), "useLimit");
        traitValueKey = new NamespacedKey(Chesticuffs.getPlugin(), "traitlevel");
        statIncreaseLeftKey = new NamespacedKey(Chesticuffs.getPlugin(), "incr");
    }

    static public void registerItem(ItemStack item){
        //Check if there actually is an item
        if(item == null || item.getType() == Material.AIR) return;
        ItemMeta meta = item.getItemMeta();
        //If item does not have a meta, create one
        if(meta == null) {
            meta = Bukkit.getServer().getItemFactory().getItemMeta(item.getType());
        }
        if(!meta.getPersistentDataContainer().has(typeKey, PersistentDataType.STRING)){
            try {
                if (itemData.containsKey(item.getType().toString())) {
                    JSONObject itemStats = (JSONObject) itemData.get(item.getType().toString());
                    if (itemStats.get("type").equals("item")) {
                        //Get stats from json
                        short ATK = (short) (long) itemStats.get("ATK");
                        short DEF = (short) (long) itemStats.get("DEF");
                        short HP = (short) (long) itemStats.get("HP");
                        JSONArray traits = (JSONArray) itemStats.get("traits");
                        String flavor = (String) itemStats.get("flavor");

                        TraitsHolder holder = new TraitsHolder(traits);

                        byte increaseLeft = 0;
                        Object data = itemStats.get("maxIncr");
                        if(data != null) increaseLeft = (byte) (long) data;

                        //Give the actual item those stats
                        meta.getPersistentDataContainer().set(typeKey, PersistentDataType.STRING, "item");
                        meta.getPersistentDataContainer().set(damageKey, PersistentDataType.SHORT, ATK);
                        meta.getPersistentDataContainer().set(defenceKey, PersistentDataType.SHORT, DEF);
                        meta.getPersistentDataContainer().set(healthKey, PersistentDataType.SHORT, HP);
                        meta.getPersistentDataContainer().set(statIncreaseLeftKey, PersistentDataType.BYTE, increaseLeft);
                        holder.setTraitsOf(meta);
                        meta.getPersistentDataContainer().set(flavorKey, PersistentDataType.STRING, flavor);
                    } else if (itemStats.get("type").equals("core")) {
                        //Get stats from json
                        short HP = (short) (long) itemStats.get("HP");
                        String buff = (String) itemStats.get("buff");
                        String debuff = (String) itemStats.get("debuff");
                        String flavor = (String) itemStats.get("flavor");
                        int effectID = (int) (long) itemStats.get("effectID");

                        //Give the actual item those stats
                        meta.getPersistentDataContainer().set(typeKey, PersistentDataType.STRING, "core");
                        meta.getPersistentDataContainer().set(healthKey, PersistentDataType.SHORT, HP);
                        meta.getPersistentDataContainer().set(flavorKey, PersistentDataType.STRING, flavor);
                        meta.getPersistentDataContainer().set(buffKey, PersistentDataType.STRING, buff);
                        meta.getPersistentDataContainer().set(debuffKey, PersistentDataType.STRING, debuff);
                        meta.getPersistentDataContainer().set(effectIDKey, PersistentDataType.INTEGER, effectID);
                    } else if (itemStats.get("type").equals("usable")) {
                        //Get stats from JSON
                        String description = (String) itemStats.get("description");
                        short useLimit = (short) (long) itemStats.get("useLimit");
                        int effectID = (int) (long) itemStats.get("effectID");
                        String flavor = (String) itemStats.get("flavor");

                        meta.getPersistentDataContainer().set(typeKey, PersistentDataType.STRING, "usable");
                        meta.getPersistentDataContainer().set(descriptionKey, PersistentDataType.STRING, description);
                        meta.getPersistentDataContainer().set(useLimitKey, PersistentDataType.SHORT, useLimit);
                        meta.getPersistentDataContainer().set(effectIDKey, PersistentDataType.INTEGER, effectID);
                        meta.getPersistentDataContainer().set(flavorKey, PersistentDataType.STRING, flavor);
                    }
                    item.setItemMeta(meta);
                    setLore(item);
                }
            }catch (NullPointerException e){
                Chesticuffs.LOGGER.log("Error when registering " + item.getType().toString(), MessageLevel.ERROR);
                e.printStackTrace();
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
            TraitsHolder traits = new TraitsHolder(item);
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
                for (String trait : traits.getTraitsToDisplay()) {
                    lore.add(Component.text(ChatColor.RED + " - " + ChatColor.BOLD + trait));
                }
            }
            meta.lore(lore);
            item.setItemMeta(meta);
        }else if(type.equals("core")){
            short HP = meta.getPersistentDataContainer().get(healthKey, PersistentDataType.SHORT);
            String flavor = meta.getPersistentDataContainer().get(flavorKey, PersistentDataType.STRING);
            String buff = meta.getPersistentDataContainer().get(buffKey, PersistentDataType.STRING);
            String debuff = meta.getPersistentDataContainer().get(debuffKey, PersistentDataType.STRING);

            List<Component> lore = new ArrayList<Component>();
            lore.add(Component.text(ChatColor.YELLOW + "Core"));
            lore.add(Component.text(""));
            lore.add(Component.text(ChatColor.DARK_GREEN + Short.toString(HP) + " HP"));
            boolean dash = false;
            if(!buff.equals("")){
                for(String buffLine : buff.split("\n")) {
                    lore.add(Component.text(ChatColor.GREEN + (dash ? "  " : "- ") + buffLine));
                    dash = true;
                }
            }
            dash = false;
            if(!debuff.equals("")){
                for(String debuffLine : debuff.split("\n")) {
                    lore.add(Component.text(ChatColor.RED + (dash ? "  " : "- ") + debuffLine));
                    dash = true;
                }
            }
            lore.add(Component.text(ChatColor.WHITE + "\"" + flavor + "\""));
            meta.lore(lore);
            item.setItemMeta(meta);
        }else if(type.equals("usable")){
            String description = meta.getPersistentDataContainer().get(descriptionKey, PersistentDataType.STRING);
            String flavor = meta.getPersistentDataContainer().get(flavorKey, PersistentDataType.STRING);

            List<Component> lore = new ArrayList<Component>();

            lore.add(Component.text(ChatColor.YELLOW + "Usable"));
            lore.add(Component.empty());
            for(String descriptionLine : description.split("\n")){
                lore.add(Component.text(ChatColor.GREEN + descriptionLine));
            }
            lore.add(Component.text(ChatColor.WHITE + "\"" + flavor + "\""));
            meta.lore(lore);
            item.setItemMeta(meta);
        }
    }

    //Helper Methods
    static public ItemStack createItem(Material material, int count, short ATK, short DEF, short HP){
        ItemStack item = new ItemStack(material, count);
        ItemMeta meta = item.getItemMeta();
        meta.getPersistentDataContainer().set(typeKey, PersistentDataType.STRING, "item");
        setStats(meta, ATK, DEF, HP);
        item.setItemMeta(meta);
        setLore(item);
        return item;
    }

    static public ItemStack createItem(Material material, int count, int ATK, int DEF, int HP){
        return createItem(material, count, (short) ATK, (short) DEF, (short) HP);
    }

    static public void setStats(ItemMeta meta, short ATK, short DEF, short HP){
        meta.getPersistentDataContainer().set(damageKey, PersistentDataType.SHORT, ATK);
        meta.getPersistentDataContainer().set(defenceKey, PersistentDataType.SHORT, DEF);
        meta.getPersistentDataContainer().set(healthKey, PersistentDataType.SHORT, HP);
    }

    //Returns true if the item died. This does not remove it from the board
    static public boolean dealDamageTo(ItemMeta meta, short damage){
        short itemHP = getHP(meta);
        short itemDEF = getDEF(meta);
        int actualDamage = Math.max(0, damage - itemDEF);
        itemHP -= actualDamage;
        setHP(meta, itemHP);
        return itemHP <= 0;
    }

    //Returns true if the item is dead. This does not remove it from the board
    static public boolean dealTrueDamageTo(ItemMeta meta, short damage){
        short itemHP = getHP(meta);
        itemHP -= damage;
        setHP(meta, itemHP);
        return itemHP <= 0;
    }

    static public short getATK(ItemMeta meta){return meta.getPersistentDataContainer().get(damageKey, PersistentDataType.SHORT);}
    static public void setATK(ItemMeta meta, short ATK){meta.getPersistentDataContainer().set(damageKey, PersistentDataType.SHORT, ATK);}
    static public void incrementATK(ItemMeta meta){
        try {
            byte incrementAllowed = meta.getPersistentDataContainer().get(statIncreaseLeftKey, PersistentDataType.BYTE);
            if(incrementAllowed > 0){
                short ATK = getATK(meta);
                setATK(meta, (short) (1 + ATK));
                meta.getPersistentDataContainer().set(statIncreaseLeftKey, PersistentDataType.BYTE, (byte)(incrementAllowed - 1));
            }
        }catch(NullPointerException e) {

        }
    }

    static public short getDEF(ItemMeta meta){return meta.getPersistentDataContainer().get(defenceKey, PersistentDataType.SHORT);}
    static public void setDEF(ItemMeta meta, short DEF){meta.getPersistentDataContainer().set(defenceKey, PersistentDataType.SHORT, DEF);}
    static public void incrementDEF(ItemMeta meta){
        try {
            byte incrementAllowed = meta.getPersistentDataContainer().get(statIncreaseLeftKey, PersistentDataType.BYTE);
            if(incrementAllowed > 0){
                short DEF = getDEF(meta);
                setDEF(meta, (short) (1 + DEF));
                meta.getPersistentDataContainer().set(statIncreaseLeftKey, PersistentDataType.BYTE, (byte)(incrementAllowed - 1));
            }
        }catch(NullPointerException e) {

        }
    }

    static public short getHP(ItemMeta meta){return meta.getPersistentDataContainer().get(healthKey, PersistentDataType.SHORT);}
    static public void setHP(ItemMeta meta, short HP){meta.getPersistentDataContainer().set(healthKey, PersistentDataType.SHORT, HP);}
    static public void incrementHP(ItemMeta meta){
        try {
            byte incrementAllowed = meta.getPersistentDataContainer().get(statIncreaseLeftKey, PersistentDataType.BYTE);
            if(incrementAllowed > 0){
                short HP = getHP(meta);
                setHP(meta, (short) (1 + HP));
                meta.getPersistentDataContainer().set(statIncreaseLeftKey, PersistentDataType.BYTE, (byte)(incrementAllowed - 1));
            }
        }catch(NullPointerException e) {

        }
    }

    static public String readFromInputStream(InputStream inputStream)
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
