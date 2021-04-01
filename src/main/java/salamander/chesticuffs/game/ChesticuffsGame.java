package salamander.chesticuffs.game;

import net.kyori.adventure.text.Component;
import org.bukkit.*;
import org.bukkit.block.Chest;
import org.bukkit.block.Container;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import salamander.chesticuffs.ChestManager;
import salamander.chesticuffs.Chesticuffs;
import salamander.chesticuffs.playerData.DataLoader;
import salamander.chesticuffs.playerData.PlayerData;
import salamander.chesticuffs.commands.RegisterInventory;
import salamander.chesticuffs.inventory.ChestKeys;
import salamander.chesticuffs.inventory.ItemHandler;

import java.util.*;
import java.util.List;

public class ChesticuffsGame {
    static public NamespacedKey playerIdKey = new NamespacedKey(Chesticuffs.getPlugin(), "gameId");
    static public NamespacedKey playerInGameKey = new NamespacedKey(Chesticuffs.getPlugin(), "inGame");
    private Player playerOne, playerTwo;
    private Chest chest;
    int roundNumber, phaseNumber, turn = 0;
    int amountSkipsPlayerOne, amountSkipsPlayerTwo, playerOneAmountPlacedThisRound, playerTwoAmountPlacedThisRound, attackersSelected;
    Integer selectedSlot;
    boolean playerOneSkipped, playerTwoSkipped;
    Map<Material, Short> playerOneItemsPlaced = new HashMap<>();
    Map<Material, Short> playerTwoItemsPlaced = new HashMap<>();
    Map<Integer, Integer> attackersAndDefenders = new HashMap<>();
    ItemStack selectedItem, validationPane;
    String id;
    boolean ranked;
    long startTime; //This is to make sure people don't exit too early

    public Inventory getPlayerOneInventory() {
        return playerOneInventory;
    }

    public Inventory getPlayerTwoInventory() {
        return playerTwoInventory;
    }

    Inventory playerOneInventory, playerTwoInventory;

    public ChesticuffsGame(Player player, Chest chest, String id, boolean ranked){
        playerOne = player;
        this.chest = chest;
        this.id = id;
        this.ranked = ranked;
        startTime = System.currentTimeMillis();

        validationPane = new ItemStack(Material.LIME_STAINED_GLASS_PANE);
        ItemMeta meta = validationPane.getItemMeta();
        meta.displayName(Component.text(ChatColor.GREEN + "Place Item Here"));
        validationPane.setItemMeta(meta);
    }


    public boolean isFull(){
        return (playerTwo != null);
    }

    public void addPlayer(Player player){
        if(isFull()) return;
        playerTwo = player;
        setupGame();
    }

    public boolean isPlayerInGame(Player player){
        return (playerOne == player || playerTwo == player);
    }

    private void setupGame(){
        playerOne.getPersistentDataContainer().set(playerIdKey, PersistentDataType.STRING, id);
        playerTwo.getPersistentDataContainer().set(playerIdKey, PersistentDataType.STRING, id);

        RegisterInventory.registerPlayersInventory(playerOne);
        RegisterInventory.registerPlayersInventory(playerTwo);
        playerOneInventory = Bukkit.createInventory(chest, 27, Component.text(ChatColor.RED + "Chesticuffs"));
        playerTwoInventory = Bukkit.createInventory(chest, 27, Component.text(ChatColor.BLUE + "Chesticuffs"));
        chest.getSnapshotInventory().setItem(4, new ItemStack(Material.STICK, 1));
        ItemStack midStick = new ItemStack(Material.STICK, 1);
        ItemMeta meta = midStick.getItemMeta();
        meta.displayName(Component.text(ChatColor.GRAY + "Skip Turn"));
        midStick.setItemMeta(meta);
        chest.getSnapshotInventory().setItem(13,midStick);
        chest.getSnapshotInventory().setItem(22, new ItemStack(Material.STICK, 1));
        chest.update();
        startRound();
        broadcastChanges();
        if(!Chesticuffs.isDebugMode) {
            playerOne.openInventory(playerOneInventory);
        }
    }

    private void startRound(){
        roundNumber += 1;
        turn = getPriority();
        writeToSticks();

        playerOneAmountPlacedThisRound = 0;
        playerTwoAmountPlacedThisRound = 0;
        amountSkipsPlayerOne = 0;
        amountSkipsPlayerTwo = 0;
        playerOneSkipped = false;
        playerTwoSkipped = false;
    }

    public void writeToSticks(){
        ItemStack topStick = chest.getBlockInventory().getItem(4);
        ItemMeta topStickMeta = topStick.getItemMeta();
        List<Component> topLore = new ArrayList<Component>();
        topStickMeta.displayName(Component.text(ChatColor.YELLOW +  "Round " + roundNumber));
        if(turn == 1){
            topLore.add(Component.text(ChatColor.RED + "Red's Turn"));
        }else{
            topLore.add(Component.text(ChatColor.BLUE + "Blue's Turn"));
        }
        topLore.add(Component.text(ChatColor.GRAY + "" + ChatColor.BOLD + "Phase " + phaseNumber + ":"));
        switch(phaseNumber) {
            case (0):
                topLore.add(Component.text( ChatColor.BOLD + "" + ChatColor.GRAY + "Core Placement"));
                break;
            case(1):
                topLore.add(Component.text(ChatColor.BOLD + "" + ChatColor.GRAY + "Item Placement"));
                break;
            case(2):
                topLore.add(Component.text(ChatColor.BOLD + "" + ChatColor.GRAY + "Declare Attackers"));
                break;
            case(3):
                topLore.add(Component.text(ChatColor.BOLD + "" + ChatColor.GRAY + "Declare Defenders"));
                break;
            case(4):
                topLore.add(Component.text(ChatColor.BOLD + "" + ChatColor.GRAY + "Closing Phase"));
                break;
            default:
                topLore.add(Component.text(ChatColor.RED + "Invalid Phase"));
        }
        topStickMeta.lore(topLore);
        topStick.setItemMeta(topStickMeta);

        ItemStack bottomStick = chest.getBlockInventory().getItem(22);
        ItemMeta bottomStickMeta = bottomStick.getItemMeta();
        if(roundNumber % 2 == 1){
            bottomStickMeta.displayName(Component.text(ChatColor.RED + "Red Priority"));
        }else{
            bottomStickMeta.displayName(Component.text(ChatColor.BLUE + "Blue Priority"));
        }
        bottomStick.setItemMeta(bottomStickMeta);
    }

    private void broadcastToSpectators(String message){
        for(HumanEntity humanEntity : chest.getBlockInventory().getViewers()){
            humanEntity.sendMessage(Component.text(message));
        }
    }

    private int getPriority(){
        return (roundNumber + 1) % 2 + 1;
    }

    private void broadcastChanges(){
        writeToSticks();
        for(int i = 0; i < 27; i++){
            ItemStack item = chest.getBlockInventory().getItem(i);
            playerOneInventory.setItem(i, item);
            playerTwoInventory.setItem(i, item);
        }
    }

    private List<Integer> getValidSlots(ItemStack item){
        String[] traits = item.getItemMeta().getPersistentDataContainer().get(ItemHandler.getTraitsKey(), PersistentDataType.STRING).split(",");
        List<Integer> validSpaces = new ArrayList<>();
        for(String trait : traits){
            System.out.println(trait);
            System.out.println(trait.equals("Gravity"));
            if(trait.equals("Gravity")){
                for(int x = 5 * turn - 5; x < 5 * turn - 1; x++){
                    for(int y = 2; y >= 0; y--){
                        ItemStack itemToCheck = chest.getBlockInventory().getItem(y * 9 + x);
                        if(itemToCheck == null){
                            validSpaces.add(y * 9 + x);
                            break;
                        }
                        if(itemToCheck.getType().equals(Material.AIR)){
                            validSpaces.add(y * 9 + x);
                            break;
                        }
                    }
                }
                return validSpaces;
            }
        }
        for(int y = 0; y < 3; y++){
            for(int x = 5 * turn - 5; x < 5 * turn - 1; x++){
                ItemStack itemToCheck = chest.getBlockInventory().getItem(y * 9 + x);
                if(itemToCheck == null){
                    validSpaces.add(y * 9 + x);
                    continue;
                }
                if(itemToCheck.getType().equals(Material.AIR)) {
                    validSpaces.add(y * 9 + x);
                }
            }
        }
        return validSpaces;
    }

    private boolean isPlayingSpaceFull(int side){
        for(int y = 0; y < 3; y++){
            for(int x = 5 * turn - 5; x < 5 * turn - 1; x++){
                ItemStack item = chest.getBlockInventory().getItem(y * 9 + x);
                if(item == null) {
                    return false;
                }
                if(item.getType().equals(Material.AIR)) {
                    return false;
                }
            }
        }
        return true;
    }

    private ItemStack getCore(int side){
        return chest.getBlockInventory().getItem(4 + side * 6);
    }

    public long getStartTime() {
        return startTime;
    }

    private boolean buffItem(ItemStack item, int x){
        ItemMeta meta = item.getItemMeta();
        short ATK = meta.getPersistentDataContainer().get(ItemHandler.getDamageKey(), PersistentDataType.SHORT);
        short DEF = meta.getPersistentDataContainer().get(ItemHandler.getDefenceKey(), PersistentDataType.SHORT);
        short HP = meta.getPersistentDataContainer().get(ItemHandler.getHealthKey(), PersistentDataType.SHORT);
        String[] traits = meta.getPersistentDataContainer().get(ItemHandler.getTraitsKey(), PersistentDataType.STRING).split(",");
        if(x == 0 || x == 8){
            System.out.println("In defence position!");
            ATK = (short) Math.ceil(ATK * 0.5);
            DEF = (short) Math.ceil(DEF * 1.5);
        }else if(x == 3 || x == 5){
            System.out.println("In attack position!");
            ATK = (short) Math.ceil(ATK * 1.5);
            DEF = (short) Math.ceil(DEF * 0.5);
        }
        int side;
        if(x < 4){
            side = 1;
        }else{
            side = 2;
        }
        System.out.println(meta.getPersistentDataContainer().get(ItemHandler.getEffectIDKey(), PersistentDataType.INTEGER));
        switch(getCore(side).getItemMeta().getPersistentDataContainer().get(ItemHandler.getEffectIDKey(), PersistentDataType.INTEGER)){
            case(1):
                HP += 1;
                for(String trait : item.getItemMeta().getPersistentDataContainer().get(ItemHandler.getTraitsKey(), PersistentDataType.STRING).split(",")){
                    if(trait.equalsIgnoreCase("Fragile")){
                        HP -= 1;
                        break;
                    }
                }
                break;
            case(2):
                for(String trait : traits){
                    if(trait.equalsIgnoreCase("Plant")){
                        HP = (short) Math.ceil(HP * 0.5);
                        break;
                    }
                }
                break;
            case(6):
                for(String trait : traits){
                    if(trait.equalsIgnoreCase("Plant")){
                        HP -= 1;
                        DEF += 1;
                        if(HP <= 0){
                            return false;
                        }
                        break;
                    }
                }
                break;
        }
        boolean coalBlockCorePlaced = getCore(side).getItemMeta().getPersistentDataContainer().get(ItemHandler.getEffectIDKey(), PersistentDataType.INTEGER).equals(5) ||
                getCore(3 - side).getItemMeta().getPersistentDataContainer().get(ItemHandler.getEffectIDKey(), PersistentDataType.INTEGER).equals(5);
        System.out.println(coalBlockCorePlaced ? "Coal block found" : "Coal block not found");
        if(coalBlockCorePlaced){
            List<String> traitsList = new LinkedList<>();
            boolean flammable = false;
            for(String trait : traits){
                if(trait.equalsIgnoreCase("flammable")){
                    flammable = true;
                    break;
                }
                traitsList.add(trait);
            }

            if(!flammable){
                traitsList.add("Flammable");
                meta.getPersistentDataContainer().set(ItemHandler.getTraitsKey(), PersistentDataType.STRING, String.join(",", traitsList));
            }
        }
        meta.getPersistentDataContainer().set(ItemHandler.getDamageKey(), PersistentDataType.SHORT, ATK);
        meta.getPersistentDataContainer().set(ItemHandler.getDefenceKey(), PersistentDataType.SHORT, DEF);
        meta.getPersistentDataContainer().set(ItemHandler.getHealthKey(), PersistentDataType.SHORT, HP);
        item.setItemMeta(meta);
        ItemHandler.setLore(item);

        return true;
    }



    private void endRound(){
        chest.update();
        for(int side  = 1; side <= 2; side++){
            ItemMeta coreMeta = chest.getSnapshotInventory().getItem(4 + side * 6).getItemMeta();
            switch (coreMeta.getPersistentDataContainer().get(ItemHandler.getEffectIDKey(), PersistentDataType.INTEGER)){
                case(2):
                    for(int x = side * 5 - 5; x < side * 5 - 1; x++){
                        for(int y = 0; y < 3; y++){
                            ItemStack item = chest.getSnapshotInventory().getItem(y * 9 + x);
                            if(item == null) continue;
                            if(item.getItemMeta().getPersistentDataContainer().get(ItemHandler.getTypeKey(), PersistentDataType.STRING) == null) continue;
                            if(item.getItemMeta().getPersistentDataContainer().get(ItemHandler.getTypeKey(), PersistentDataType.STRING).equals("item")){
                                short HP = item.getItemMeta().getPersistentDataContainer().get(ItemHandler.getHealthKey(), PersistentDataType.SHORT);
                                ItemMeta meta = item.getItemMeta();
                                for(String trait : item.getItemMeta().getPersistentDataContainer().get(ItemHandler.getTraitsKey(), PersistentDataType.STRING).split(",")){
                                    if(trait.equalsIgnoreCase("plant")){
                                        System.out.println(item.getType() + " is a plant!");
                                        meta.getPersistentDataContainer().set(ItemHandler.getHealthKey(), PersistentDataType.SHORT, (short) (HP + 1));
                                        item.setItemMeta(meta);
                                        break;
                                    }
                                }
                                item.setItemMeta(meta);
                                ItemHandler.setLore(item);
                            }
                        }
                    }
            }
        }
        doFireDamage();
        broadcastChanges();
        //checkForDraw();
    }

    private void doFireDamage(){
        chest.update();
        boolean lavaBucketCorePlaced = false;
        for(int i : new int[]{10, 16}){
            if(chest.getBlockInventory().getItem(i).getItemMeta().getPersistentDataContainer().get(ItemHandler.getEffectIDKey(), PersistentDataType.INTEGER).equals(4)){
                lavaBucketCorePlaced = true;
                break;
            }
        }

        if(lavaBucketCorePlaced){
            broadcast(ChatColor.RED + "All items take fire damage from lava bucket core!");
            for(int i = 0; i < 27; i++){
                ItemStack item = chest.getSnapshotInventory().getItem(i);
                if(item == null) continue;
                ItemMeta meta = item.getItemMeta();
                if(meta.getPersistentDataContainer().has(ItemHandler.getTypeKey(), PersistentDataType.STRING)) {
                    if (!meta.getPersistentDataContainer().get(ItemHandler.getTypeKey(), PersistentDataType.STRING).equals("item"))
                        continue;
                }else{
                    continue;
                }
                short HP = meta.getPersistentDataContainer().get(ItemHandler.getHealthKey(), PersistentDataType.SHORT);
                int fireDamage = 1;
                for(String trait : meta.getPersistentDataContainer().get(ItemHandler.getTraitsKey(), PersistentDataType.STRING).split(",")){
                    if(trait.equalsIgnoreCase("flammable")){
                        fireDamage = 2;
                        break;
                    }else if(trait.equalsIgnoreCase("Fire Resistance")){
                        fireDamage = 0;
                        break;
                    }
                }
                if(fireDamage >= HP){
                    chest.getSnapshotInventory().setItem(i, null);
                    broadcast((i % 9 < 4 ? ChatColor.RED : ChatColor.BLUE) + item.getType().toString() + " dies from fire damage!");
                }else{
                    HP -= fireDamage;
                    meta.getPersistentDataContainer().set(ItemHandler.getHealthKey(), PersistentDataType.SHORT, HP);
                    item.setItemMeta(meta);
                    ItemHandler.setLore(item);
                }
            }
            chest.update();
        }
    }

    private void broadcast(String message){
        playerOne.sendMessage(message);
        playerTwo.sendMessage(message);
        broadcastToSpectators(message);
    }

    private void combat(){
        ItemStack defender, attacker;
        ItemMeta defendingItemMeta, attackingItemMeta;
        short defenderHP, attackerHP;

        ChatColor attackerColor, defenderColor;
        if(getPriority() == 1){
            attackerColor = ChatColor.RED;
            defenderColor = ChatColor.BLUE;
        }else{
            attackerColor = ChatColor.BLUE;
            defenderColor = ChatColor.RED;
        }

        ItemStack defendingCore = chest.getSnapshotInventory().getItem(22 - 6 * getPriority());
        ItemMeta defendingCoreMeta = defendingCore.getItemMeta();
        PersistentDataContainer defendingCoreData = defendingCoreMeta.getPersistentDataContainer();
        short coreHealth = defendingCoreData.get(ItemHandler.getHealthKey(), PersistentDataType.SHORT);
        for(Map.Entry<Integer, Integer> entry: attackersAndDefenders.entrySet()){
            if(entry.getValue() == null){
                broadcast(chest.getBlockInventory().getItem(entry.getKey()).getType().toString() + " (Slot " + entry.getKey() + ") is undefended!");
                int coreDamage = chest.getSnapshotInventory().getItem(entry.getKey()).getItemMeta().getPersistentDataContainer().get(ItemHandler.getDamageKey(), PersistentDataType.SHORT);
                broadcast(ChatColor.GRAY +  "Core takes " + attackerColor +  coreDamage + ChatColor.GRAY +  " damage!");
                coreHealth -= coreDamage;
                ItemHandler.setLore(chest.getBlockInventory().getItem(entry.getKey()));
            }else{
                attacker = chest.getSnapshotInventory().getItem(entry.getKey());
                defender = chest.getSnapshotInventory().getItem(entry.getValue());
                boolean isAttackerImmune = false;
                boolean isDefenderImmune = false;
                attackingItemMeta = attacker.getItemMeta();
                defendingItemMeta = defender.getItemMeta();

                String[] attackerTraits = attackingItemMeta.getPersistentDataContainer().get(ItemHandler.getTraitsKey(), PersistentDataType.STRING).split(",");
                for(String trait : attackerTraits){
                    if(trait.equalsIgnoreCase("Immunity")){
                        isAttackerImmune = true;
                        List<String> newTraits = Arrays.asList(attackerTraits.clone());
                        newTraits.remove("Immunity");
                        attackingItemMeta.getPersistentDataContainer().set(ItemHandler.getTraitsKey(), PersistentDataType.STRING, String.join(",", newTraits));
                        break;
                    }
                }

                String[] defenderTraits = defendingItemMeta.getPersistentDataContainer().get(ItemHandler.getTraitsKey(), PersistentDataType.STRING).split(",");
                for(String trait : defenderTraits){
                    if(trait.equalsIgnoreCase("Immunity")){
                        isDefenderImmune = true;
                        List<String> newTraits = Arrays.asList(defenderTraits.clone());
                        newTraits.remove("Immunity");
                        defendingItemMeta.getPersistentDataContainer().set(ItemHandler.getTraitsKey(), PersistentDataType.STRING, String.join(",", newTraits));
                        break;
                    }
                }

                attackerHP = attackingItemMeta.getPersistentDataContainer().get(ItemHandler.getHealthKey(), PersistentDataType.SHORT);
                defenderHP = defendingItemMeta.getPersistentDataContainer().get(ItemHandler.getHealthKey(), PersistentDataType.SHORT);

                int attackerDamage = Math.max((attackingItemMeta.getPersistentDataContainer().get(ItemHandler.getDamageKey(), PersistentDataType.SHORT) - defendingItemMeta.getPersistentDataContainer().get(ItemHandler.getDefenceKey(), PersistentDataType.SHORT)), 0);
                int defenderDamage = Math.max((defendingItemMeta.getPersistentDataContainer().get(ItemHandler.getDamageKey(), PersistentDataType.SHORT) - attackingItemMeta.getPersistentDataContainer().get(ItemHandler.getDefenceKey(), PersistentDataType.SHORT)), 0);

                if(!isDefenderImmune) {
                    defenderHP -= attackerDamage;
                    broadcast(defenderColor + defender.getType().toString() + ChatColor.GRAY + " (Slot " + entry.getValue() + ") " + " takes " +
                            attackerColor + attackerDamage + ChatColor.GRAY + " damage!");
                }

                if(!isAttackerImmune) {
                    attackerHP -= defenderDamage;
                    broadcast(attackerColor + attacker.getType().toString() + ChatColor.GRAY + " (Slot " + entry.getKey() + ") " + " takes " +
                            defenderColor + defenderDamage + ChatColor.GRAY + " damage!");
                }

                if(defenderHP <= 0){
                    chest.getSnapshotInventory().setItem(entry.getValue(), null);
                }else{
                    defendingItemMeta.getPersistentDataContainer().set(ItemHandler.getHealthKey(), PersistentDataType.SHORT, defenderHP);
                }
                if(attackerHP <= 0){
                    chest.getSnapshotInventory().setItem(entry.getKey(), null);
                }else{
                    attackingItemMeta.getPersistentDataContainer().set(ItemHandler.getHealthKey(), PersistentDataType.SHORT, attackerHP);
                }
                attacker.setItemMeta(attackingItemMeta);
                defender.setItemMeta(defendingItemMeta);
                ItemHandler.setLore(attacker);
                ItemHandler.setLore(defender);
            }
        }
        defendingCoreData.set(ItemHandler.getHealthKey(), PersistentDataType.SHORT, coreHealth);
        defendingCore.setItemMeta(defendingCoreMeta);
        ItemHandler.setLore(defendingCore);
        chest.update();
        broadcastChanges();
        if(coreHealth <= 0){
            //De-registers Game
            endGame(getPriority());
        }
    }

    public void handleExitEvent(Player player){
        if(Chesticuffs.isDebugMode) return;
        if(player.getUniqueId().equals(playerOne.getUniqueId())){
            endGame(2);
            broadcast(ChatColor.RED + "Red exited the chest and forfeited!");
        }else{
            endGame(1);
            broadcast(ChatColor.BLUE + "Blue exited the chest and forfeited!");
        }

    }

    private void endGame(int winner){
        chest.close();
        Chesticuffs.getGames().remove(id);
        chest.getPersistentDataContainer().remove(ChestKeys.idKey);
        playerOne.getPersistentDataContainer().remove(playerIdKey);
        playerTwo.getPersistentDataContainer().remove(playerIdKey);

        Bukkit.getServer().getScheduler().runTask(Chesticuffs.getPlugin(), new InventoryClose(playerOneInventory));
        Bukkit.getServer().getScheduler().runTask(Chesticuffs.getPlugin(), new InventoryClose(playerTwoInventory));

        PlayerData playerOneData = DataLoader.getData().get(playerOne.getUniqueId());
        PlayerData playerTwoData = DataLoader.getData().get(playerTwo.getUniqueId());
        int playerOneElo = playerOneData.getEloRating();
        int playerTwoElo = playerTwoData.getEloRating();
        double probabilityOfPlayerOneWin = 1 / (1 + Math.pow(10, (playerTwoElo - playerOneElo) / 400));
        ItemStack coreOne = getCore(1);
        ItemStack coreTwo = getCore(2);
        short coreOneHP, coreTwoHP;
        double outcome;
        if(coreOne == null){
            outcome = 2 - winner;
        }else {
            coreOneHP = coreOne.getItemMeta().getPersistentDataContainer().get(ItemHandler.getHealthKey(), PersistentDataType.SHORT);
            coreTwoHP = coreTwo.getItemMeta().getPersistentDataContainer().get(ItemHandler.getHealthKey(), PersistentDataType.SHORT);
            outcome = coreOneHP / (coreOneHP + coreTwoHP);
        }
        if(winner == 1){
            playerOne.sendMessage(ChatColor.RED + "Red wins the game!");
            playerTwo.sendMessage(ChatColor.RED + "Red wins the game!");

            playerOneData.setLastWonAt(System.currentTimeMillis());
            playerOneData.setWinCount(playerOneData.getWinCount() + 1);
            playerOneData.setStreak(playerOneData.getStreak() + 1);

            playerTwoData.setLossCount(playerTwoData.getLossCount() + 1);
            playerTwoData.setStreak(0);

            outcome = 1;
        }else if(winner == 2){
            playerOne.sendMessage(ChatColor.BLUE + "Blue wins the game!");
            playerTwo.sendMessage(ChatColor.BLUE + "Blue wins the game!");

            playerTwoData.setLastWonAt(System.currentTimeMillis());
            playerTwoData.setWinCount(playerTwoData.getWinCount() + 1);
            playerTwoData.setStreak(playerTwoData.getStreak() + 1);

            playerOneData.setLossCount(playerOneData.getLossCount() + 1);
            playerOneData.setStreak(0);
            outcome = 0;
        }else{
            playerOne.sendMessage(ChatColor.GREEN + "The game is a draw!");
            playerTwo.sendMessage(ChatColor.GREEN + "The game is a draw!");
            outcome = 0.5;
        }

        int change = (int) Math.round( Chesticuffs.K * (outcome - probabilityOfPlayerOneWin));
        if(ranked) {
            playerOneData.setEloRating(playerOneElo + change);
            playerTwoData.setEloRating(playerTwoElo - change);
        }

        Location worldSpawn = playerOne.getWorld().getSpawnLocation();
        playerOne.teleport(worldSpawn);
        playerTwo.teleport(worldSpawn);
        playerOne.getPersistentDataContainer().set(playerInGameKey, PersistentDataType.BYTE, (byte) 0);
        playerTwo.getPersistentDataContainer().set(playerInGameKey, PersistentDataType.BYTE, (byte) 0);
        chest.getPersistentDataContainer().set(ChestManager.reservedKey, PersistentDataType.BYTE, (byte) 0);
        chest.getSnapshotInventory().clear();
        chest.update();

        playerOne.getInventory().clear();
        playerTwo.getInventory().clear();
    }

    public void checkForDraw(){
        if(true) return;
        /* Draws can occur in two different ways
        1. The board is empty and people can no longer place items
        2. The board is full and no items are capable of damaging other items
         */

        boolean typeOne = true;
        boolean typeTwo = true;

        boolean playerOneHasItems = false;
        boolean playerTwoHasItems = false;

        //Check for type one
        Inventory[] inventories = {chest.getBlockInventory(), playerOneInventory, playerTwoInventory};
        for(Inventory inventory : inventories) {
            for (int index = 0; index < inventory.getSize(); index++) {
                ItemStack item = inventory.getItem(index);
                if (item == null) continue;
                String type = item.getItemMeta().getPersistentDataContainer().get(ItemHandler.getTypeKey(), PersistentDataType.STRING);
                if (type == null) continue;
                if (type.equals("item")) {
                    typeOne = false;
                    if(inventory == playerOneInventory){
                        playerOneHasItems = true;
                    }else if(inventory == playerTwoInventory){
                        playerTwoHasItems = true;
                    }
                    break;
                }

            }
            if (!typeOne) break;
        }

        if(typeOne){
            broadcast(ChatColor.GREEN + "No more items in play or in hand!");
            endGame(0); //Endgame 0 makes a draw
            return;
        }

        //Check for type two
        for(int index = 0; index < 27; index++){
            if(chest.getBlockInventory().getItem(index) == null){
                typeTwo = false;
                break;
            }
        }

        if(typeTwo){
            List<Short> playerOneAttacks = new ArrayList<>();
            List<Short> playerOneDefence = new ArrayList<>();
            List<Short> playerTwoAttacks = new ArrayList<>();
            List<Short> playerTwoDefence = new ArrayList<>();
            for(int x = 0; x < 4; x++){
                for(int y = 0; y < 3; y++){
                    ItemStack itemOne = chest.getBlockInventory().getItem(y * 9 + x);
                    ItemStack itemTwo = chest.getBlockInventory().getItem(y * 9 + x  + 5);
                    if(itemOne != null) {
                        Short damageOne = itemOne.getItemMeta().getPersistentDataContainer().get(ItemHandler.getDamageKey(), PersistentDataType.SHORT);
                        Short defenceOne = itemOne.getItemMeta().getPersistentDataContainer().get(ItemHandler.getDefenceKey(), PersistentDataType.SHORT);
                        if (damageOne != null) {
                            playerOneAttacks.add(damageOne);
                            playerOneDefence.add(defenceOne);
                        }
                    }
                    if(itemTwo != null){
                        Short damageTwo = itemTwo.getItemMeta().getPersistentDataContainer().get(ItemHandler.getDamageKey(), PersistentDataType.SHORT);
                        Short defenceTwo = itemTwo.getItemMeta().getPersistentDataContainer().get(ItemHandler.getDefenceKey(), PersistentDataType.SHORT);
                        if (damageTwo != null) {
                            playerTwoAttacks.add(damageTwo);
                            playerTwoDefence.add(defenceTwo);
                        }
                    }
                }
            }

            short playerOneBiggestAttack = Collections.max(playerOneAttacks);
            short playerTwoBiggestAttack = Collections.max(playerTwoAttacks);
            short playerOneImportantDefence = select(playerOneDefence, Math.min(5, playerTwoAttacks.size() - 1));
            short playerTwoImportantDefence = select(playerTwoDefence, Math.min(5, playerOneAttacks.size() - 1));


        }

        if(typeTwo){
            broadcast(ChatColor.GREEN + "Stalemate");
            endGame(0);
        }
    }

    private short select(List<Short> list, int k){
        for(int i = 0; i < k; i++){
            int maxIndex = i;
            int maxValue = list.get(maxIndex);
            for(int j = i + 1; j < list.size(); j++){
                if(list.get(j) > maxValue){
                    maxIndex = j;
                    maxValue = list.get(maxIndex);
                }
            }
            short temp = list.get(i);
            list.set(i, list.get(maxIndex));
            list.set(maxIndex, temp);
        }
        return list.get(k);
    }

    private void coreClicked(int coreId){
        System.out.println("Core Clicked Is Run!");
        switch(coreId){
            case(3):
                System.out.println("E gap used!");
                if(getCore(turn).getItemMeta().getPersistentDataContainer().get(ItemHandler.getHealthKey(), PersistentDataType.SHORT) <= 5){
                    broadcast(ChatColor.RED + "Cannot use enchanted golden apple, too low health!");
                }
                for(int x = turn * 5 - 5; x < turn * 5 - 1; x++){
                    for(int y = 0; y < 3; y++){
                        ItemStack currentItem = chest.getBlockInventory().getItem(y * 9 + x);
                        if(currentItem == null) continue;
                        System.out.println("Giving " + currentItem.getType().toString() + " immunity!");
                        ItemMeta itemMeta = currentItem.getItemMeta();
                        if(!itemMeta.getPersistentDataContainer().get(ItemHandler.getTypeKey(), PersistentDataType.STRING).equalsIgnoreCase("item")) {
                            System.out.println(currentItem.getType().toString() + " is not an item!");
                            continue;
                        };
                        List<String> traits = new LinkedList<>();
                        for(String trait : itemMeta.getPersistentDataContainer().get(ItemHandler.getTraitsKey(), PersistentDataType.STRING).split(",")){
                            traits.add(trait);
                        }
                        traits.add("Immunity");
                        System.out.println(traits);
                        itemMeta.getPersistentDataContainer().set(ItemHandler.getTraitsKey(), PersistentDataType.STRING, String.join(",", traits));
                        currentItem.setItemMeta(itemMeta);
                        ItemHandler.setLore(currentItem);
                    }
                }
                ItemStack core = getCore(turn);
                ItemMeta coreMeta = core.getItemMeta();
                short HP = coreMeta.getPersistentDataContainer().get(ItemHandler.getHealthKey(), PersistentDataType.SHORT);
                coreMeta.getPersistentDataContainer().set(ItemHandler.getHealthKey(), PersistentDataType.SHORT, (short) Math.min(5, HP));
                core.setItemMeta(coreMeta);
                ItemHandler.setLore(core);
                break;
        }
        broadcastChanges();
    }

    public void handleClickEvent(InventoryClickEvent e){
        if(!(e.getWhoClicked() instanceof Player)){
            return;
        }
        Player player = (Player) e.getWhoClicked();
        e.setCancelled(true);
        if(turn == 1 && (! player.equals(playerOne))){
            player.sendMessage(ChatColor.RED + "It is not your turn!");
            return;
        }

        if(turn == 2 && (! player.equals(playerTwo))){
            player.sendMessage(ChatColor.RED + "It is not your turn!");
            return;
        }

        if(e.getClickedInventory() == null){
            return;
        }

        Inventory currentInv;
        if(turn == 1){
            currentInv = playerOneInventory;
        }else{
            currentInv = playerTwoInventory;
        }

        ChatColor attackerColor, defenderColor;
        if(getPriority() == 1){
            attackerColor = ChatColor.RED;
            defenderColor = ChatColor.BLUE;
        }else{
            attackerColor = ChatColor.BLUE;
            defenderColor = ChatColor.RED;
        }

        switch(phaseNumber){
            case(0):
                if(e.getClickedInventory().equals(player.getInventory())){
                    ItemStack item = e.getCurrentItem();
                    if(item == null || item.getType() == Material.AIR){
                        return;
                    }
                    ItemMeta meta = item.getItemMeta();
                    if(meta == null){
                        return;
                    }
                    if(meta.getPersistentDataContainer().has(ItemHandler.getTypeKey(), PersistentDataType.STRING)){
                        if(meta.getPersistentDataContainer().get(ItemHandler.getTypeKey(), PersistentDataType.STRING).equals("core")){
                            ItemStack core = item.clone();
                            core.setAmount(1);
                            chest.getSnapshotInventory().setItem(4 + turn * 6, core);
                            item.setAmount(item.getAmount()-1);
                            chest.update();
                            //Next Turn
                            if(turn == getPriority()){
                                turn = 3 - turn;
                                playerTwo.sendMessage(ChatColor.RED + "Red has placed their core!");
                            }else{
                                turn = getPriority();
                                selectedItem = null;
                                phaseNumber = 1;
                                playerOneSkipped = false;
                                playerTwoSkipped = false;
                                amountSkipsPlayerOne = 0;
                                amountSkipsPlayerTwo = 0;
                                playerOneAmountPlacedThisRound = 0;
                                playerTwoAmountPlacedThisRound = 0;
                                playerOne.sendMessage(ChatColor.BLUE + "Blue has placed their core!");
                            }
                            broadcastChanges();
                        }else{
                            player.sendMessage(ChatColor.RED + "Please Select A Core");
                        }
                    }else{
                        player.sendMessage(ChatColor.RED + "Please Select A Core");
                    }

                }else if(e.getCurrentItem() != null){
                    if(e.getCurrentItem().getItemMeta() != null) {
                        if (e.getCurrentItem().getItemMeta().displayName().equals(Component.text(ChatColor.GRAY + "Skip Turn"))) {
                            player.sendMessage(ChatColor.RED + "You cannot skip this phase!");
                        }
                    }
                }
                break;
            case(1):
            case(4):
                if(e.getClickedInventory().equals(player.getInventory())){
                    broadcastChanges(); //Clears all virtual green glass panes if there are any (Because the panes aren't in the actual chest)
                    selectedItem = null;
                    ItemStack item = e.getCurrentItem();
                    if(item == null || item.getType() == Material.AIR){
                        return;
                    }
                    ItemMeta meta = item.getItemMeta();
                    if(meta == null){
                        return;
                    }

                    if(meta.getPersistentDataContainer().equals(null)) {
                        return;
                    }

                    if(!meta.getPersistentDataContainer().get(ItemHandler.getTypeKey(), PersistentDataType.STRING).equals("item")){
                        player.sendMessage(ChatColor.RED + "Select an ITEM");
                        return;
                    }

                    if(isPlayingSpaceFull(turn)){
                        player.sendMessage("Your playing space is full!");
                        return;
                    }

                    int maxAmountPlaced = 3;
                    for(String trait :  item.getItemMeta().getPersistentDataContainer().get(ItemHandler.getTraitsKey(), PersistentDataType.STRING).split(",")){
                        if(trait.length() < 9) continue;
                        String[] traitStuff = trait.split(" ");
                        if(traitStuff[0].equalsIgnoreCase("Capbreaker")){
                            maxAmountPlaced = Integer.valueOf(traitStuff[1]);
                        }
                    }

                    if(turn == 1){
                        if(playerOneAmountPlacedThisRound >= 3){
                            player.sendMessage(ChatColor.RED + "You have already placed three items this round");
                            return;
                        }
                        Short amountPlaced = playerOneItemsPlaced.get(item.getType());
                        if(amountPlaced != null){
                            if(amountPlaced >= maxAmountPlaced){
                                player.sendMessage(ChatColor.RED + "You have already placed this item the maximum times!");
                                return;
                            }
                        }
                    }else{
                        if(playerTwoAmountPlacedThisRound >= 3){
                            player.sendMessage(ChatColor.RED + "You have already placed three items this round");
                            return;
                        }
                        Short amountPlaced = playerTwoItemsPlaced.get(item.getType());
                        if(amountPlaced != null){
                            if(amountPlaced >= maxAmountPlaced){
                                player.sendMessage(ChatColor.RED + "You have already placed this item the maximum times!");
                                return;
                            }
                        }
                    }
                    selectedItem = item;
                    for(int i : getValidSlots(item)){
                        currentInv.setItem(i, validationPane);
                    }
                }else if(e.getClickedInventory().equals(currentInv)){
                    System.out.println("Placing Item");
                    ItemStack item = e.getCurrentItem();
                    if(item == null || item.getType() == Material.AIR){
                        System.out.println("Item is empty");
                        return;
                    }
                    if(e.getSlot() == 10 || e.getSlot() == 16){
                        System.out.println("Core clicked");
                        System.out.println(e.getSlot());
                        if(item.getItemMeta().getPersistentDataContainer().has(ItemHandler.getEffectIDKey(), PersistentDataType.INTEGER)) {
                            Integer effectId = item.getItemMeta().getPersistentDataContainer().get(ItemHandler.getEffectIDKey(), PersistentDataType.INTEGER);
                            coreClicked(effectId);
                            if (turn == 1) {
                                broadcast(ChatColor.RED + "All red items gain immunity!");
                            } else {
                                broadcast(ChatColor.BLUE + "All blue items gain immunity!");
                            }
                        }
                    }else if(e.getSlot() == 13){
                        if(turn == 1){
                            playerOneSkipped = true;
                            amountSkipsPlayerOne += 1;
                            playerTwo.sendMessage(ChatColor.RED + "Red" + ChatColor.WHITE +  " skipped. Your turn!");
                        }else{
                            playerTwoSkipped = true;
                            amountSkipsPlayerTwo += 1;
                            playerOne.sendMessage(ChatColor.BLUE + "Blue" + ChatColor.WHITE +  " skipped. Your turn!");
                        }
                        selectedItem = null;
                        if(playerOneSkipped && playerTwoSkipped){
                            if(phaseNumber == 1){
                                phaseNumber = 2;
                                selectedItem = null;
                                turn = getPriority();
                                attackersAndDefenders.clear();
                                attackersSelected = 0;
                            }else if(phaseNumber == 4){
                                endRound();
                                phaseNumber = 1;
                                selectedItem = null;
                                roundNumber += 1;
                                turn = getPriority();
                                playerOneAmountPlacedThisRound = 0;
                                playerTwoAmountPlacedThisRound = 0;
                                playerOneSkipped = false;
                                playerTwoSkipped = false;
                                broadcast(ChatColor.GREEN + "Round " + roundNumber + " has started!");
                                if(getPriority() == 1){
                                    broadcast(ChatColor.RED + "Red Priority");
                                }else{
                                    broadcast(ChatColor.BLUE + "Blue Priority");
                                }
                            }
                        }else {
                            turn = 3 - turn;
                        }
                        broadcastChanges();
                    }else if(selectedItem == null){
                        return;
                    }else if(item.equals(validationPane)){
                        ItemStack itemToBePlaced = new ItemStack(selectedItem);
                        System.out.println(itemToBePlaced.getItemMeta().displayName());
                        itemToBePlaced.setAmount(1);
                        boolean placed = buffItem(itemToBePlaced, e.getSlot() % 9);
                        selectedItem.setAmount(selectedItem.getAmount() - 1);
                        if(placed) chest.getSnapshotInventory().setItem(e.getSlot(), itemToBePlaced);
                        else{
                            broadcast(ChatColor.RED + "Item was placed but died immediately. LOL!");
                        }
                        chest.update();
                        if (turn == 1){
                            playerOneSkipped = false;
                            playerOneAmountPlacedThisRound += 1;
                            if(playerOneItemsPlaced.get(itemToBePlaced.getType()) == null){
                                playerOneItemsPlaced.put(itemToBePlaced.getType(), (short) 1);
                            }else{
                                playerOneItemsPlaced.put(itemToBePlaced.getType(), (short) (playerOneItemsPlaced.get(itemToBePlaced.getType()) + 1));
                            }
                            playerTwo.sendMessage(ChatColor.RED + "Red" + ChatColor.WHITE + " placed their item. Your turn!");
                        }else{
                            playerTwoSkipped = false;
                            playerTwoAmountPlacedThisRound += 1;
                            if(playerTwoItemsPlaced.get(itemToBePlaced.getType()) == null){
                                playerTwoItemsPlaced.put(itemToBePlaced.getType(), (short) 1);
                            }else{
                                playerTwoItemsPlaced.put(itemToBePlaced.getType(), (short) (playerTwoItemsPlaced.get(itemToBePlaced.getType()) + 1));
                            }
                            playerOne.sendMessage(ChatColor.BLUE + "Blue" + ChatColor.WHITE + " placed their item. Your turn!");
                        }
                        turn = 3 - turn;
                        broadcastChanges();
                    }else{
                        selectedItem = null;
                        broadcastChanges();
                    }
                }
                break;
            case(2):
                //Check if they clicked on their side
                if(e.getClickedInventory().equals(currentInv)){
                    if(e.getSlot() % 9 == 4){
                        if(e.getSlot() == 13){
                            turn = 3 - turn;
                            phaseNumber = 3;
                            selectedSlot = null;
                            if(getPriority() == 1){
                                playerTwo.sendMessage(ChatColor.RED + "Red has chosen their attackers!");
                            }
                            broadcastChanges();
                        }
                        return;
                    }
                    if(turn == 1){
                        if(e.getSlot() % 9 >= 4){
                            return;
                        }
                    }else{
                        if(e.getSlot() % 9 <= 4){
                            return;
                        }
                    }
                    if(e.getCurrentItem() == null) return;
                    if(e.getCurrentItem().getType() == Material.AIR) return;
                    if(e.getCurrentItem().getItemMeta().getPersistentDataContainer().get(ItemHandler.getTypeKey(), PersistentDataType.STRING).equals("item")){
                        ItemStack itemInChest = chest.getBlockInventory().getItem(e.getSlot());
                        ItemMeta meta =  itemInChest.getItemMeta();
                        List<Component> lore = meta.lore();
                        if(!attackersAndDefenders.containsKey(e.getSlot())){
                            if(attackersSelected >= 6) {
                                player.sendMessage(Component.text(ChatColor.RED + "You have already declared six attackers!"));
                                return;
                            }
                            attackersSelected += 1;
                            attackersAndDefenders.put(e.getSlot(), null);
                            lore.set(1, Component.text(ChatColor.RED + "Attacking"));
                        }else{
                            attackersSelected -= 1;
                            attackersAndDefenders.remove(e.getSlot());
                            lore.set(1, Component.text(""));
                        }
                        meta.lore(lore);
                        itemInChest.setItemMeta(meta);
                        broadcastChanges();
                    }
                }

                break;
            case(3):
                if(e.getCurrentItem() == null) {
                    selectedSlot = null;
                    return;
                }
                if(e.getCurrentItem().getType() == Material.AIR) {
                    selectedSlot = null;
                    return;
                }
                if(e.getClickedInventory().equals(currentInv)) {
                    if(e.getCurrentItem().getItemMeta() == null){
                        return;
                    }
                    if(e.getCurrentItem().getItemMeta().getPersistentDataContainer().has(ItemHandler.getTypeKey(), PersistentDataType.STRING)){
                        if(!e.getCurrentItem().getItemMeta().getPersistentDataContainer().get(ItemHandler.getTypeKey(), PersistentDataType.STRING).equals("item")){
                            return;
                        }
                    }
                    if(e.getSlot() % 9 == 4){
                        if(e.getSlot() == 13){
                            phaseNumber = 4;
                            turn = 3 - getPriority();
                            selectedItem = null;
                            playerOneSkipped = false;
                            playerTwoSkipped = false;
                            broadcastChanges();
                            combat();
                        }
                        return;
                    }
                    boolean clickedInOwnSide = true;
                    if(turn == 1){
                        if(e.getSlot() % 9 >= 4){
                            clickedInOwnSide = false;
                        }
                    }else{
                        if(e.getSlot() % 9 <= 4){
                            clickedInOwnSide = false;
                        }
                    }

                    if(clickedInOwnSide){
                        System.out.println("Checking if item already defending!");
                        if(!attackersAndDefenders.containsValue(e.getSlot())){
                            selectedSlot = e.getSlot();
                            ItemStack defender =  chest.getBlockInventory().getItem(selectedSlot);
                            ItemMeta defenderMeta = defender.getItemMeta();
                            List<Component> lore = defenderMeta.lore();
                            lore.set(1,Component.text(ChatColor.BLUE + "Defending"));
                            defenderMeta.lore(lore);
                            defender.setItemMeta(defenderMeta);
                        }else{

                            /*ItemStack defender =  e.getCurrentItem();
                            ItemMeta defenderMeta = defender.getItemMeta();
                            List<Component> lore = defenderMeta.lore();
                            lore.set(1,Component.text(""));
                            defenderMeta.lore(lore);
                            defender.setItemMeta(defenderMeta);
                            for(int keySlot :attackersAndDefenders.keySet()){
                                if(attackersAndDefenders.get(keySlot).equals(e.getSlot())){
                                    attackersAndDefenders.put(keySlot, null);
                                    ItemStack attacker = chest.getBlockInventory().getItem(keySlot);
                                    ItemMeta attackerMeta = attacker.getItemMeta();
                                    List<Component> attackerLore = attackerMeta.lore();
                                    attackerLore.set(1,Component.text(ChatColor.RED + "Attacking"));
                                    break;
                                }
                            }*/
                            player.sendMessage(ChatColor.RED + "That item is already defending!");
                            selectedSlot = null;
                        }
                        broadcastChanges();
                        return;
                    }else{
                        if(selectedSlot == null){
                            player.sendMessage(ChatColor.RED + "Select a defender first!");
                            return;
                        }else{
                            if(attackersAndDefenders.containsKey(e.getSlot())){
                                if(!(attackersAndDefenders.get(e.getSlot()) == null)){
                                    player.sendMessage(ChatColor.RED + "Item is already being defended!");
                                    return;
                                }
                                attackersAndDefenders.put(e.getSlot(), selectedSlot);
                                ItemStack defender =  chest.getBlockInventory().getItem(selectedSlot);
                                ItemMeta defenderMeta = defender.getItemMeta();
                                List<Component> lore = defenderMeta.lore();
                                lore.set(1,Component.text(ChatColor.BLUE + "Defending " + e.getCurrentItem().getType().name() + " (Slot " + e.getSlot() + ")"));
                                defenderMeta.lore(lore);
                                defender.setItemMeta(defenderMeta);

                                ItemStack defendedItem = chest.getBlockInventory().getItem(e.getSlot());
                                ItemMeta defendedItemMeta = defendedItem.getItemMeta();
                                lore = defendedItemMeta.lore();
                                lore.set(1, Component.text(ChatColor.RED + "Attacking (Defended)"));
                                defendedItemMeta.lore(lore);
                                defendedItem.setItemMeta(defendedItemMeta);

                                broadcastChanges();
                                selectedSlot = null;
                            }else{
                                player.sendMessage(ChatColor.RED + "That item is not attacking!");
                                ItemStack defender =  chest.getBlockInventory().getItem(selectedSlot);
                                ItemMeta defenderMeta = defender.getItemMeta();
                                List<Component> lore = defenderMeta.lore();
                                lore.set(1,Component.text( ""));
                                defenderMeta.lore(lore);
                                defender.setItemMeta(defenderMeta);
                                broadcastChanges();
                                selectedSlot = null;
                                return;
                            }
                        }
                    }
                }
                break;
        }
    }
}
