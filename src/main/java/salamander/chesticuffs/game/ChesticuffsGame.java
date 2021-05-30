package salamander.chesticuffs.game;

import net.kyori.adventure.text.Component;
import org.apache.commons.lang.ObjectUtils;
import org.bukkit.*;
import org.bukkit.block.Chest;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitTask;
import salamander.chesticuffs.ChestManager;
import salamander.chesticuffs.Chesticuffs;
import salamander.chesticuffs.playerData.DataLoader;
import salamander.chesticuffs.playerData.PlayerData;
import salamander.chesticuffs.commands.RegisterInventory;
import salamander.chesticuffs.inventory.ChestKeys;
import salamander.chesticuffs.inventory.ItemHandler;
import salamander.chesticuffs.traits.Trait;
import salamander.chesticuffs.traits.TraitsHolder;

import java.util.*;

public class ChesticuffsGame {
    static public final NamespacedKey playerIdKey = new NamespacedKey(Chesticuffs.getPlugin(), "gameId");      //Chests and players store a reference to a ChesticuffsGame object using this key
    static public final NamespacedKey playerInGameKey = new NamespacedKey(Chesticuffs.getPlugin(), "inGame");  //Used to easily check wether a player is in a game or not
    private final Player playerOne;
    private Player playerTwo;
    private final Chest chest;
    int roundNumber, phaseNumber, turn = 0;
    int amountSkipsPlayerOne, amountSkipsPlayerTwo, playerOneAmountPlacedThisRound, playerTwoAmountPlacedThisRound, attackersSelected;
    Integer selectedSlot;
    boolean playerOneSkipped, playerTwoSkipped;
    final Map<Material, Short> playerOneItemsPlaced = new HashMap<>(); //Stores how many of each item a player has placed in this game
    final Map<Material, Short> playerTwoItemsPlaced = new HashMap<>();
    final Map<Integer, Integer> attackersAndDefenders = new HashMap<>();
    ItemStack selectedItem;
    final ItemStack validationPane;
    final String id;
    final boolean ranked;
    final long lastActionAt;
    final long startTime; //This is to make sure people don't accidentally click esc and exit too early
    final List<BukkitTask> timerTasks = new LinkedList<>();
    boolean ended = false;
    private boolean pendingUsableSelection = false;
    private Integer usableTemporarySlot = null;

    private static class sendMessage implements Runnable{
        final Player player;
        final String message;
        public sendMessage(Player player, String message){
            this.player = player;
            this.message = message;
        }

        public void run(){
            player.sendMessage(message);
        }
    }

    public Inventory getPlayerOneInventory() {
        return playerOneInventory;
    }

    public Inventory getPlayerTwoInventory() {
        return playerTwoInventory;
    }

    Inventory playerOneInventory, playerTwoInventory;

    //The constructor takes in only a single player because originally, people would create and join games by clicking on a chest
    //Hence the method addPlayer(Player player)
    public ChesticuffsGame(Player player, Chest chest, String id, boolean ranked){
        playerOne = player;
        this.chest = chest;
        this.id = id;
        this.ranked = ranked;
        startTime = System.currentTimeMillis();
        lastActionAt = startTime;

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

    private class playerTimeOut implements Runnable{
        @Override
        public void run() {
            playerTookTooLong();
        }
    }
    private void playerTookTooLong() {
        broadcast(ChatColor.RED + "Player " + (turn == 1 ? "one" : "two") + " took too long to play!");
        endGame(3 - turn);
    }

    private void action(boolean longer){
        for(BukkitTask task : timerTasks){
            task.cancel();
        }
        timerTasks.clear();

        if(Chesticuffs.isDebugMode){
            return;
        }

        Player player = turn == 1 ? playerOne : playerTwo;
        if(!longer) {
            timerTasks.add(Bukkit.getScheduler().runTaskLater(Chesticuffs.getPlugin(), new sendMessage(player, ChatColor.DARK_RED + "15 seconds left for this turn!"), 300));
            timerTasks.add(Bukkit.getScheduler().runTaskLater(Chesticuffs.getPlugin(), new sendMessage(player, ChatColor.DARK_RED + "10 seconds left for this turn!"), 400));
            timerTasks.add(Bukkit.getScheduler().runTaskLater(Chesticuffs.getPlugin(), new sendMessage(player, ChatColor.DARK_RED + "5 seconds left for this turn!"), 500));
            timerTasks.add(Bukkit.getScheduler().runTaskLater(Chesticuffs.getPlugin(), new sendMessage(player, ChatColor.DARK_RED + "4 seconds left for this turn!"), 520));
            timerTasks.add(Bukkit.getScheduler().runTaskLater(Chesticuffs.getPlugin(), new sendMessage(player, ChatColor.DARK_RED + "3 seconds left for this turn!"), 540));
            timerTasks.add(Bukkit.getScheduler().runTaskLater(Chesticuffs.getPlugin(), new sendMessage(player, ChatColor.DARK_RED + "2 seconds left for this turn!"), 560));
            timerTasks.add(Bukkit.getScheduler().runTaskLater(Chesticuffs.getPlugin(), new sendMessage(player, ChatColor.DARK_RED + "1 second left for this turn!"), 580));
            timerTasks.add(Bukkit.getScheduler().runTaskLater(Chesticuffs.getPlugin(), new playerTimeOut(), 600));
        }else{
            timerTasks.add(Bukkit.getScheduler().runTaskLater(Chesticuffs.getPlugin(), new sendMessage(player, ChatColor.DARK_RED + "30 seconds left for this turn!"), 300));
            timerTasks.add(Bukkit.getScheduler().runTaskLater(Chesticuffs.getPlugin(), new sendMessage(player, ChatColor.DARK_RED + "15 seconds left for this turn!"), 600));
            timerTasks.add(Bukkit.getScheduler().runTaskLater(Chesticuffs.getPlugin(), new sendMessage(player, ChatColor.DARK_RED + "10 seconds left for this turn!"), 700));
            timerTasks.add(Bukkit.getScheduler().runTaskLater(Chesticuffs.getPlugin(), new sendMessage(player, ChatColor.DARK_RED + "5 seconds left for this turn!"), 800));
            timerTasks.add(Bukkit.getScheduler().runTaskLater(Chesticuffs.getPlugin(), new sendMessage(player, ChatColor.DARK_RED + "4 seconds left for this turn!"), 820));
            timerTasks.add(Bukkit.getScheduler().runTaskLater(Chesticuffs.getPlugin(), new sendMessage(player, ChatColor.DARK_RED + "3 seconds left for this turn!"), 840));
            timerTasks.add(Bukkit.getScheduler().runTaskLater(Chesticuffs.getPlugin(), new sendMessage(player, ChatColor.DARK_RED + "2 seconds left for this turn!"), 860));
            timerTasks.add(Bukkit.getScheduler().runTaskLater(Chesticuffs.getPlugin(), new sendMessage(player, ChatColor.DARK_RED + "1 second left for this turn!"), 880));
            timerTasks.add(Bukkit.getScheduler().runTaskLater(Chesticuffs.getPlugin(), new playerTimeOut(), 900));
        }
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
        meta.displayName(Component.text(ChatColor.GRAY + "End Turn"));
        midStick.setItemMeta(meta);
        chest.getSnapshotInventory().setItem(13,midStick);
        chest.getSnapshotInventory().setItem(22, new ItemStack(Material.STICK, 1));
        chest.update();
        startRound();
        broadcastChanges();
        if(!Chesticuffs.isDebugMode) {
            playerOne.openInventory(playerOneInventory);
            playerTwo.openInventory(playerTwoInventory);
        }
        action(false);
        playerOneAmountPlacedThisRound = 0;
        playerTwoAmountPlacedThisRound = 0;
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
        if(topStick == null){
            topStick = new ItemStack(Material.STICK);
            chest.getSnapshotInventory().setItem(4, topStick);
            chest.update();
        }
        if(topStick == null){
            System.out.println(ChatColor.RED + "Top stick was null :/");
            return;
        }
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
        if(bottomStick == null){
            bottomStick = new ItemStack(Material.STICK, 1);
            chest.getSnapshotInventory().setItem(22, bottomStick);
            chest.update();
        }
        if(bottomStick == null){
            System.out.println(ChatColor.RED + "Bottom stick was null :/");
            return;
        }
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
        int swapLength = 1;
        try{
            if(getCore(1).getItemMeta().getPersistentDataContainer().get(ItemHandler.getEffectIDKey(), PersistentDataType.INTEGER) == 9
            || getCore(2).getItemMeta().getPersistentDataContainer().get(ItemHandler.getEffectIDKey(), PersistentDataType.INTEGER) == 9){
                swapLength = 2;
            }
        }catch (NullPointerException e){}
        return (roundNumber - swapLength) / swapLength % 2 + 1;
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
        TraitsHolder traits = new TraitsHolder(item);
        List<Integer> validSpaces = new ArrayList<>();
        if(traits.hasTrait(Trait.GRAVITY)){
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
        validSpaces.remove(Integer.valueOf(10));
        validSpaces.remove(Integer.valueOf(16));
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
        TraitsHolder traits = new TraitsHolder(meta);
        if(x == 0 || x == 8){
            ATK = (short) Math.ceil(ATK * 0.5);
            HP = (short) Math.ceil(HP * 1.5);
        }else if(x == 3 || x == 5){
            ATK = (short) Math.ceil(ATK * 1.5);
            HP = (short) Math.ceil(HP * 0.5);
        }
        int side;
        if(x < 4){
            side = 1;
        }else{
            side = 2;
        }
        ItemStack core = getCore(side);
        if(core != null) {
            switch (core.getItemMeta().getPersistentDataContainer().get(ItemHandler.getEffectIDKey(), PersistentDataType.INTEGER)) {
                case (1):
                    HP += 1;
                    break;
                case (2):
                    if (traits.hasTrait(Trait.PLANT)) {
                        HP = (short) Math.ceil(HP * 0.5);
                    }
                    break;
                case (6):
                    if (traits.hasTrait(Trait.PLANT)) {
                        HP -= 1;
                        DEF += 1;
                        if (HP <= 0) {
                            return false;
                        }
                    }
                    break;
                case (7):
                    DEF += 1;
                    break;
                case (8):
                    HP -= 1;
                    if (HP <= 0) {
                        return false;
                    }
                    break;
                case(11):
                    if(traits.hasTrait(Trait.AQUATIC)) HP++;
                    break;
            }
        }

        ItemStack enemyCore = getCore(3 - side);
        if(enemyCore != null){
            switch (enemyCore.getItemMeta().getPersistentDataContainer().get(ItemHandler.getEffectIDKey(), PersistentDataType.INTEGER)){
                case(10):
                    if(ATK > 0) ATK--;
                    break;
            }
        }
        if(traits.hasTrait(Trait.FRAGILE)){
            HP = 1;
            DEF = 1;
        }
        try {
            boolean coalBlockCorePlaced = getCore(side).getItemMeta().getPersistentDataContainer().get(ItemHandler.getEffectIDKey(), PersistentDataType.INTEGER).equals(5) ||
                    getCore(3 - side).getItemMeta().getPersistentDataContainer().get(ItemHandler.getEffectIDKey(), PersistentDataType.INTEGER).equals(5);
            if (coalBlockCorePlaced) {

                traits.addTrait(Trait.FLAMMABLE);
            }
        }catch(NullPointerException e){

        }
        meta.getPersistentDataContainer().set(ItemHandler.getDamageKey(), PersistentDataType.SHORT, ATK);
        meta.getPersistentDataContainer().set(ItemHandler.getDefenceKey(), PersistentDataType.SHORT, DEF);
        meta.getPersistentDataContainer().set(ItemHandler.getHealthKey(), PersistentDataType.SHORT, HP);
        traits.setTraitsOf(meta);
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
                            TraitsHolder traits = new TraitsHolder(item);
                            if(item == null) continue;
                            if(item.getItemMeta().getPersistentDataContainer().get(ItemHandler.getTypeKey(), PersistentDataType.STRING) == null) continue;
                            if(item.getItemMeta().getPersistentDataContainer().get(ItemHandler.getTypeKey(), PersistentDataType.STRING).equals("item")){
                                short HP = item.getItemMeta().getPersistentDataContainer().get(ItemHandler.getHealthKey(), PersistentDataType.SHORT);
                                ItemMeta meta = item.getItemMeta();
                                if(traits.hasTrait(Trait.PLANT)){
                                    meta.getPersistentDataContainer().set(ItemHandler.getHealthKey(), PersistentDataType.SHORT, (short) (HP + 1));
                                    item.setItemMeta(meta);
                                }
                                item.setItemMeta(meta);
                                ItemHandler.setLore(item);
                            }
                        }
                    }
            }
        }

        //Check for poison damage
        chest.update();
        for(int slot = 0; slot < 27; slot++){
            if(slot % 9 == 4) continue;
            ItemStack item = chest.getSnapshotInventory().getItem(slot);
            if(item == null) continue;
            if(item.getItemMeta().getPersistentDataContainer().get(ItemHandler.getTypeKey(), PersistentDataType.STRING).equals("item")){
                short HP = item.getItemMeta().getPersistentDataContainer().get(ItemHandler.getHealthKey(), PersistentDataType.SHORT);
                ItemMeta meta = item.getItemMeta();
                TraitsHolder traits = new TraitsHolder(meta);
                if(traits.hasTrait(Trait.POISONED)){
                    meta.getPersistentDataContainer().set(ItemHandler.getHealthKey(), PersistentDataType.SHORT, (short) Math.max(HP - 1, 1));
                    item.setItemMeta(meta);
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
                TraitsHolder traits = new TraitsHolder(meta);
                if(traits.hasTrait(Trait.FIRE_RESISTANT)){
                    fireDamage = 0;
                }else if(traits.hasTrait(Trait.FIRE_RESISTANT)){
                    fireDamage = 2;
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
        int totalCoreDamage = 0;
        short coreHealth = defendingCoreData.get(ItemHandler.getHealthKey(), PersistentDataType.SHORT);
        for(Map.Entry<Integer, Integer> entry: attackersAndDefenders.entrySet()){
            if(entry.getValue() == null){
                broadcast(chest.getBlockInventory().getItem(entry.getKey()).getType().toString() + " (Slot " + entry.getKey() + ") is undefended!");
                int coreDamage = chest.getSnapshotInventory().getItem(entry.getKey()).getItemMeta().getPersistentDataContainer().get(ItemHandler.getDamageKey(), PersistentDataType.SHORT);
                totalCoreDamage += coreDamage;
                broadcast(ChatColor.GRAY +  "Core takes " + attackerColor +  coreDamage + ChatColor.GRAY +  " damage!");
                coreHealth -= coreDamage;
                ItemHandler.setLore(chest.getBlockInventory().getItem(entry.getKey()));
            }else{
                attacker = chest.getSnapshotInventory().getItem(entry.getKey());
                defender = chest.getSnapshotInventory().getItem(entry.getValue());

                int properAttackerDamage = attacker.getItemMeta().getPersistentDataContainer().get(ItemHandler.getDamageKey(), PersistentDataType.SHORT);
                int properDefenderDamage = defender.getItemMeta().getPersistentDataContainer().get(ItemHandler.getDamageKey(), PersistentDataType.SHORT);
                int attackerSlot = entry.getKey();
                int defenderSlot = entry.getValue();

                int defenderX = entry.getValue() % 9;
                int defenderY = (int) Math.floor(entry.getValue() / 9);
                int potentialSoftX[] = new int[] {defenderX + 1, defenderX, defenderX - 1, defenderX};
                int potentialSoftY[] = new int[] {defenderY, defenderY - 1, defenderY, defenderY + 1};

                for (int i = 0; i < 4; i++){
                    if(!(potentialSoftX[i] < 0 || potentialSoftX[i] > 8 || potentialSoftY[i] < 0 || potentialSoftY[i] > 2 || potentialSoftX[i] == 4)){
                        ItemStack potentialDefender = chest.getSnapshotInventory().getItem(potentialSoftY[i] * 9 + potentialSoftX[i]);
                        if(potentialDefender == null) continue;
                        if(!potentialDefender.getItemMeta().getPersistentDataContainer().get(ItemHandler.getTypeKey(), PersistentDataType.STRING).equalsIgnoreCase("item")) continue;
                        if(Trait.SOFT.isInItem(potentialDefender)){
                            broadcast(ChatColor.GREEN + potentialDefender.getType().toString() + " tanks damage for " + defender.getType().toString());
                            defender = potentialDefender;
                            break;
                        }
                    }
                }

                int attackerX = attackerSlot % 9;
                int attackerY = (int) Math.floor(attackerSlot / 9);
                int potentialSoftXForAttacker[] = new int[] {attackerX + 1, attackerX, attackerX - 1, attackerX};
                int potentialSoftYForAttacker[] = new int[] {attackerY, attackerY - 1, attackerY, attackerY + 1};
                System.out.println(attackerX + " " + attackerY);

                for (int i = 0; i < 4; i++){
                    if(!(potentialSoftXForAttacker[i] < 0 || potentialSoftXForAttacker[i] > 8 || potentialSoftYForAttacker[i] < 0 || potentialSoftYForAttacker[i] > 2 || potentialSoftXForAttacker[i] == 4)){
                        ItemStack potentialAttacker = chest.getSnapshotInventory().getItem(potentialSoftYForAttacker[i] * 9 + potentialSoftXForAttacker[i]);
                        if(potentialAttacker == null) continue;
                        if(!potentialAttacker.getItemMeta().getPersistentDataContainer().get(ItemHandler.getTypeKey(), PersistentDataType.STRING).equals("item")) continue;
                        if(Trait.SOFT.isInItem(potentialAttacker)){
                            broadcast(ChatColor.GREEN + potentialAttacker.getType().toString() + " tanks damage for " + defender.getType().toString());
                            attacker = potentialAttacker;
                            break;
                        }
                    }
                }

                attackingItemMeta = attacker.getItemMeta();
                defendingItemMeta = defender.getItemMeta();

                TraitsHolder attackingItemTraits = new TraitsHolder(attackingItemMeta);
                TraitsHolder defendingItemTraits = new TraitsHolder(defendingItemMeta);

                attackerHP = attackingItemMeta.getPersistentDataContainer().get(ItemHandler.getHealthKey(), PersistentDataType.SHORT);
                defenderHP = defendingItemMeta.getPersistentDataContainer().get(ItemHandler.getHealthKey(), PersistentDataType.SHORT);

                int attackerDamage = Math.max((properAttackerDamage - defendingItemMeta.getPersistentDataContainer().get(ItemHandler.getDefenceKey(), PersistentDataType.SHORT)), 0);
                int defenderDamage = Math.max((properDefenderDamage - attackingItemMeta.getPersistentDataContainer().get(ItemHandler.getDefenceKey(), PersistentDataType.SHORT)), 0);

                if(attackingItemTraits.hasTrait(Trait.FLAME)){
                    if(defendingItemTraits.hasTrait(Trait.FIRE_RESISTANT)){
                        attackerDamage = 0;
                    }else if(defendingItemTraits.hasTrait(Trait.FLAMMABLE)){
                        attackerDamage *= 2;
                    }
                }

                if(defendingItemTraits.hasTrait(Trait.FLAME)){
                    if(attackingItemTraits.hasTrait(Trait.FIRE_RESISTANT)){
                        defenderDamage = 0;
                    }else if(defendingItemTraits.hasTrait(Trait.FLAMMABLE)){
                        defenderDamage *= 2;
                    }
                }

                if(!defendingItemTraits.hasTrait(Trait.IMMUNE)) {
                    defenderHP -= attackerDamage;
                    broadcast(defenderColor + defender.getType().toString() + ChatColor.GRAY + " (Slot " + attackerSlot + ") " + " takes " +
                            attackerColor + attackerDamage + ChatColor.GRAY + " damage!");
                }

                if(!attackingItemTraits.hasTrait(Trait.IMMUNE)) {
                    attackerHP -= defenderDamage;
                    broadcast(attackerColor + attacker.getType().toString() + ChatColor.GRAY + " (Slot " + defenderSlot + ") " + " takes " +
                            defenderColor + defenderDamage + ChatColor.GRAY + " damage!");
                }

                if(attackerHP <= 0) {
                    if(attackingItemTraits.hasTrait(Trait.SHRAPNEL)){
                        defenderHP -= 2;
                    }
                }

                if(defenderHP <= 0){
                    if(defendingItemTraits.hasTrait(Trait.SHRAPNEL)){
                        attackerHP -= 2;
                    }
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

                attackingItemTraits.removeTrait(Trait.IMMUNE);
                attackingItemTraits.removeTrait(Trait.STUNNED);

                defendingItemTraits.removeTrait(Trait.IMMUNE);
                defendingItemTraits.removeTrait(Trait.STUNNED);

                attacker.setItemMeta(attackingItemMeta);
                defender.setItemMeta(defendingItemMeta);
                ItemHandler.setLore(attacker);
                ItemHandler.setLore(defender);
            }
        }
        defendingCoreData.set(ItemHandler.getHealthKey(), PersistentDataType.SHORT, coreHealth);
        defendingCore.setItemMeta(defendingCoreMeta);
        ItemHandler.setLore(defendingCore);
        if(coreHealth <= 0){
            //De-registers Game
            endGame(getPriority());
        }else{
            if(defendingCoreMeta.getPersistentDataContainer().get(ItemHandler.getEffectIDKey(), PersistentDataType.INTEGER) == 12){
                ItemStack attackingCore = getCore(getPriority());
                ItemMeta attackingCoreMeta = attackingCore.getItemMeta();
                short attackingCoreHealth = attackingCoreMeta.getPersistentDataContainer().get(ItemHandler.getHealthKey(), PersistentDataType.SHORT);
                short coreDamage = (short) Math.ceil(totalCoreDamage / 2.0f);
                attackingCoreHealth -= coreDamage;
                broadcast(ChatColor.RED + "Attacking core takes " + coreDamage + " from Rose Bush!");
                if(attackingCoreHealth <= 0){
                    endGame(3 - getPriority());
                }else{
                    attackingCoreMeta.getPersistentDataContainer().set(ItemHandler.getHealthKey(), PersistentDataType.SHORT, attackingCoreHealth);
                    attackingCore.setItemMeta(attackingCoreMeta);
                    ItemHandler.setLore(attackingCore);
                }
            }
        }
        chest.update();
        broadcastChanges();
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
        if(ended) return;
        ended = true;
        for(BukkitTask task : timerTasks){
            task.cancel();
        }
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
        double probabilityOfPlayerOneWin = 1 / (1 + Math.pow(10, (playerTwoElo - playerOneElo) / 400.0));
        ItemStack coreOne = getCore(1);
        ItemStack coreTwo = getCore(2);
        short coreOneHP, coreTwoHP;
        double outcome;
        if(coreOne == null || coreTwo == null){
            outcome = 2 - winner;
        }else {
            coreOneHP = coreOne.getItemMeta().getPersistentDataContainer().get(ItemHandler.getHealthKey(), PersistentDataType.SHORT);
            coreTwoHP = coreTwo.getItemMeta().getPersistentDataContainer().get(ItemHandler.getHealthKey(), PersistentDataType.SHORT);
            outcome = coreOneHP / (coreOneHP + coreTwoHP);
        }
        if(winner == 1){
            playerOne.sendMessage(ChatColor.RED + "Red wins the game!");
            playerTwo.sendMessage(ChatColor.RED + "Red wins the game!");
            if(ranked) {
                playerOneData.setLastWonAt(System.currentTimeMillis());
                playerOneData.setWinCount(playerOneData.getWinCount() + 1);
                playerOneData.setStreak(playerOneData.getStreak() + 1);

                playerTwoData.setLossCount(playerTwoData.getLossCount() + 1);
                playerTwoData.setStreak(0);
            }

            outcome = 1;
        }else if(winner == 2){
            playerOne.sendMessage(ChatColor.BLUE + "Blue wins the game!");
            playerTwo.sendMessage(ChatColor.BLUE + "Blue wins the game!");
            if(ranked) {
                playerTwoData.setLastWonAt(System.currentTimeMillis());
                playerTwoData.setWinCount(playerTwoData.getWinCount() + 1);
                playerTwoData.setStreak(playerTwoData.getStreak() + 1);

                playerOneData.setLossCount(playerOneData.getLossCount() + 1);
                playerOneData.setStreak(0);
            }
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
        playerOne.setBedSpawnLocation(worldSpawn, true);
        playerTwo.setBedSpawnLocation(worldSpawn, true);
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
        chest.update();
        switch(coreId){
            case(3):
                if(getCore(turn).getItemMeta().getPersistentDataContainer().get(ItemHandler.getHealthKey(), PersistentDataType.SHORT) <= 5){
                    broadcast(ChatColor.RED + "Cannot use enchanted golden apple, too low health!");
                }
                for(int x = turn * 5 - 5; x < turn * 5 - 1; x++){
                    for(int y = 0; y < 3; y++){
                        ItemStack currentItem = chest.getBlockInventory().getItem(y * 9 + x);
                        if(currentItem == null) continue;
                        ItemMeta itemMeta = currentItem.getItemMeta();
                        if(!itemMeta.getPersistentDataContainer().get(ItemHandler.getTypeKey(), PersistentDataType.STRING).equalsIgnoreCase("item")) {
                            continue;
                        };
                        TraitsHolder traits = new TraitsHolder(itemMeta);
                        if(traits.addTrait(Trait.IMMUNE)){
                            traits.setTraitsOf(itemMeta);
                        }
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
        chest.update();
        broadcastChanges();
    }

    private void usableClicked(ItemStack usable, int slot){
        selectedItem = usable;
        ItemMeta usableMeta = usable.getItemMeta();
        List<Component> usableLore = usableMeta.lore();
        int effectID = usableMeta.getPersistentDataContainer().get(ItemHandler.getEffectIDKey(), PersistentDataType.INTEGER);

        switch(effectID){
            case(1): //Bell
            case(2): //Stonecutter
            case(3): //Grindstone
            case(5): //Ender Chest
            case(6): //Flower Pot
                pendingUsableSelection = true;
                usableLore.set(1, Component.text(ChatColor.RED + "Select a played item!"));
                break;
            case(4):
                for(int x = 10 - turn * 5; x < 14 - turn * 5; x++){
                    for(int y  = 0; y < 3; y++){
                        try {
                            ItemStack currentItem = chest.getSnapshotInventory().getItem(y * 9 + x);
                            if(Trait.COMPOSTABLE.isInItem(currentItem)){
                                System.out.println(y * 9 + x);
                                broadcast(ChatColor.RED + currentItem.getType().toString() + " (Slot " + (y * 9 + x) + ") was killed by composter!");
                                chest.getSnapshotInventory().setItem(y * 9 + x, null);
                            }
                        }catch(NullPointerException e){}
                    }
                }
                selectedItem.setAmount(selectedItem.getAmount() - 1);
                chest.update();
                if(turn == 1){
                    playerTwo.sendMessage(ChatColor.RED + "Red used a composter! Your turn!");
                }else{
                    playerOne.sendMessage(ChatColor.BLUE + "Blue used a composter! Your turn!");
                }
                incrementUsageCountOf(selectedItem);
                itemPlacementNextTurn();
                selectedItem = null;
                broadcastChanges();
                break;
        }

        usableMeta.lore(usableLore);
        usable.setItemMeta(usableMeta);
    }

    private void setInfoLoreLine(ItemStack item, String line){
        try{
            List<Component> lore = item.getItemMeta().lore();
            lore.set(1, Component.text(line));
            ItemMeta itemMeta = item.getItemMeta();
            itemMeta.lore(lore);
            item.setItemMeta(itemMeta);
        }catch (NullPointerException e){ }
    }

    private void clearLoreInfoLine(ItemStack item){
        try {
            List<Component> lore = item.getItemMeta().lore();
            lore.set(1, Component.empty());
            ItemMeta itemMeta = item.getItemMeta();
            itemMeta.lore(lore);
            item.setItemMeta(itemMeta);
        }catch (NullPointerException e){

        }
    }

    private void itemPlacementNextTurn(){
        turn = 3 - turn;
        pendingUsableSelection = false;
        usableTemporarySlot = null;
        action(false);
        selectedItem = null;
        selectedSlot = null;
        broadcastChanges();
    }

    public void printGameState(){
        Chesticuffs.LOGGER.log("########Game State########");
        Chesticuffs.LOGGER.log("Game Identifier : " + id);
        Chesticuffs.LOGGER.log("Player One : " + playerOne.getName());
        Chesticuffs.LOGGER.log("Player Two : " + playerTwo.getName());
        Chesticuffs.LOGGER.log("Round " + roundNumber + ", Phase " + phaseNumber + ", " + (turn == 1 ? "Red" : "Blue") + "'s turn");
        Chesticuffs.LOGGER.log("Player One:");
        try{ Chesticuffs.LOGGER.log("  Skips this phase : " + amountSkipsPlayerOne);}catch (NullPointerException e){}
        try{ Chesticuffs.LOGGER.log("  Amount placed this round : " + playerOneAmountPlacedThisRound);}catch (NullPointerException e){}
        try{ Chesticuffs.LOGGER.log("  Skipped : " + playerOneSkipped);}catch (NullPointerException e){}
        Chesticuffs.LOGGER.log("Player Two:");
        try{ Chesticuffs.LOGGER.log("  Skips this phase : " + amountSkipsPlayerTwo);}catch (NullPointerException e){}
        try{ Chesticuffs.LOGGER.log("  Amount placed this round : " + playerTwoAmountPlacedThisRound);}catch (NullPointerException e){}
        try{ Chesticuffs.LOGGER.log("  Skipped : " + playerTwoSkipped);}catch (NullPointerException e){}
        try{ Chesticuffs.LOGGER.log("Selected Slot : " + selectedSlot);}catch (NullPointerException e){}
        try{ Chesticuffs.LOGGER.log("Selected Item : " + selectedItem.getType().toString());}catch (NullPointerException e){}
        Chesticuffs.LOGGER.log("Pending Usable Selection : " + pendingUsableSelection);
        try{ Chesticuffs.LOGGER.log("Usable Temporary Data Slot : " + usableTemporarySlot);}catch (NullPointerException e){}
    }

    private void usableSelectedUsableItem(int slot){
        ItemStack clickedItem = chest.getSnapshotInventory().getItem(slot);
        Integer usableInfo = usableTemporarySlot;
        usableTemporarySlot = null;
        pendingUsableSelection = false;

        if(clickedItem == null){
            clearLoreInfoLine(selectedItem);
            selectedItem = null;
            return;
        }

        ItemMeta clickedItemMeta = clickedItem.getItemMeta();

        if(clickedItemMeta == null){
            clearLoreInfoLine(selectedItem);
            selectedItem = null;
            return;
        }

        if(!clickedItemMeta.getPersistentDataContainer().get(ItemHandler.getTypeKey(), PersistentDataType.STRING).equals("item")){
            clearLoreInfoLine(selectedItem);
            selectedItem = null;
            return;
        }

        int effectID = selectedItem.getItemMeta().getPersistentDataContainer().get(ItemHandler.getEffectIDKey(), PersistentDataType.INTEGER);
        Chesticuffs.LOGGER.log("An item has been selected for a usable. Usable effect ID : " + effectID + ", item clicked : " + clickedItem.getType().toString());
        boolean succesfullyUsed = false;
        boolean clickedItemDied = false;
        boolean clearSelectedItem = true;

        switch(effectID){
            case(1): //Bell
                System.out.println("Bell used!");
                TraitsHolder traits = new TraitsHolder(clickedItemMeta);
                if(traits.addTrait(Trait.STUNNED)){
                    traits.setTraitsOf(clickedItemMeta);
                    succesfullyUsed = true;
                }
                break;
            case(2): //Stonecutter
                short ATK = clickedItemMeta.getPersistentDataContainer().get(ItemHandler.getDamageKey(), PersistentDataType.SHORT);
                short DEF = clickedItemMeta.getPersistentDataContainer().get(ItemHandler.getDefenceKey(), PersistentDataType.SHORT);
                short HP = clickedItemMeta.getPersistentDataContainer().get(ItemHandler.getHealthKey(), PersistentDataType.SHORT);

                broadcast(ChatColor.GREEN + "Stonecutter has been used!");

                ATK++;
                DEF--;
                HP--;

                if(DEF < 0) DEF  = 0;

                if(HP <= 0){
                    clickedItemDied = true;
                }else{
                    clickedItemMeta.getPersistentDataContainer().set(ItemHandler.getDamageKey(), PersistentDataType.SHORT, ATK);
                    clickedItemMeta.getPersistentDataContainer().set(ItemHandler.getDefenceKey(), PersistentDataType.SHORT, DEF);
                    clickedItemMeta.getPersistentDataContainer().set(ItemHandler.getHealthKey(), PersistentDataType.SHORT, HP);
                }

                succesfullyUsed = true;
                break;
            case(3): //Grindstone
                Map<Enchantment, Integer> enchantments = clickedItemMeta.getEnchants();
                int amount = enchantments.size();
                if(amount > 0) {
                    int index = (new Random()).nextInt(amount);
                    int i = 0;
                    for(Enchantment enchant : enchantments.keySet()){
                        if(i == index){
                            clickedItemMeta.removeEnchant(enchant);
                            succesfullyUsed = true;
                            break;
                        }

                        i++;
                    }
                }
                break;
            case(5): //Ender Chest
                if(usableInfo == null){
                    pendingUsableSelection = true;
                    usableTemporarySlot = slot;
                    setInfoLoreLine(selectedItem, ChatColor.RED + "Select a second item");
                    clearSelectedItem = false;
                }else{
                    if(usableInfo != slot){
                        ItemStack itemOne = chest.getSnapshotInventory().getItem(usableInfo).clone();
                        chest.getSnapshotInventory().setItem(usableInfo, clickedItem);
                        chest.getSnapshotInventory().setItem(slot, itemOne);
                        succesfullyUsed = true;
                        broadcast(ChatColor.GREEN + "Switched items with ender chest!");
                    }
                }
                break;
            case(6):
            case(7):
                Trait trait = (effectID == 6 ? Trait.POTTABLE : Trait.STANDABLE);
                int slotX = slot / 3;
                Chesticuffs.LOGGER.log("Slot X : " + slotX + ", Turn : " + turn);
                if(turn == 1 && slotX > 3) break;
                if(turn == 2 && slotX < 5) break;
                if(Trait.POTTABLE.isInMeta(clickedItemMeta)){
                    HashMap<Integer, ItemStack> notReturnedItems;
                    if(turn == 1){
                        notReturnedItems = playerOne.getInventory().addItem(clickedItem);
                        playerOne.updateInventory();
                    }else{
                        notReturnedItems = playerTwo.getInventory().addItem(clickedItem);
                        playerTwo.updateInventory();
                    }

                    if(notReturnedItems.isEmpty()){
                        succesfullyUsed = true;
                        chest.getSnapshotInventory().setItem(slot, null);
                    }
                }
                break;
            default:
                Chesticuffs.LOGGER.log("Reached default of usable effectID switch statement");
        }

        if(succesfullyUsed) {
            if(!clickedItemDied) {
                Chesticuffs.LOGGER.log("Usable didn't make item die!");
                clickedItem.setItemMeta(clickedItemMeta);
                ItemHandler.setLore(clickedItem);
                //chest.getSnapshotInventory().setItem(slot, clickedItem);
            }else{
                Chesticuffs.LOGGER.log("Usable made item die!");
                chest.getSnapshotInventory().setItem(slot, null);
            }
            selectedItem.setAmount(selectedItem.getAmount() - 1);
            incrementUsageCountOf(selectedItem);
        }
        if(clearSelectedItem) {
            clearLoreInfoLine(selectedItem);
            selectedItem = null;
        }
        if (succesfullyUsed) {
            itemPlacementNextTurn();
        }
        chest.update();
    }

    private void incrementUsageCountOf(ItemStack item){
        Map<Material, Short> itemPlacedMap;
        if(turn == 1){
            itemPlacedMap = playerOneItemsPlaced;
            playerOneAmountPlacedThisRound++;
        }else{
            itemPlacedMap = playerTwoItemsPlaced;
            playerTwoAmountPlacedThisRound++;
        }

        Short amountPlaced = itemPlacedMap.get(item.getType());
        if(amountPlaced == null){
            itemPlacedMap.put(item.getType(), (short) 1);
        }else{
            itemPlacedMap.put(item.getType(), (short)(amountPlaced + 1));
        }
    }

    public void placeItem(ItemStack item, int slot){
        ItemStack itemToBePlaced = new ItemStack(item);
        itemToBePlaced.setAmount(1);
        boolean placed = buffItem(itemToBePlaced, slot % 9);
        item.setAmount(item.getAmount() - 1);
        if(placed) chest.getSnapshotInventory().setItem(slot, itemToBePlaced);
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
        }else {
            playerTwoSkipped = false;
            playerTwoAmountPlacedThisRound += 1;
            if (playerTwoItemsPlaced.get(itemToBePlaced.getType()) == null) {
                playerTwoItemsPlaced.put(itemToBePlaced.getType(), (short) 1);
            } else {
                playerTwoItemsPlaced.put(itemToBePlaced.getType(), (short) (playerTwoItemsPlaced.get(itemToBePlaced.getType()) + 1));
            }
            playerOne.sendMessage(ChatColor.BLUE + "Blue" + ChatColor.WHITE + " placed their item. Your turn!");
        }
        action(false);
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
                        String type = meta.getPersistentDataContainer().get(ItemHandler.getTypeKey(), PersistentDataType.STRING);
                        if(type.equals("core")){
                            ItemStack core = item.clone();
                            core.setAmount(1);
                            chest.getSnapshotInventory().setItem(4 + turn * 6, core);
                            item.setAmount(item.getAmount()-1);
                            chest.update();
                            if(turn == 1){
                                playerTwo.sendMessage(ChatColor.RED + "Red has placed their core!");
                            }else{
                                playerOne.sendMessage(ChatColor.BLUE + "Blue has placed their core!");
                            }
                            //Next Turn
                            if(getCore(1) == null || getCore(2) == null){
                                turn = 3 - turn;
                                if(getCore(turn) != null)
                                    turn = 3 - turn;
                            }else{
                                turn = getPriority();
                                selectedItem = null;
                                phaseNumber = 1;
                                playerOneSkipped = false;
                                playerTwoSkipped = false;
                                amountSkipsPlayerOne = 0;
                                amountSkipsPlayerTwo = 0;
                            }
                            action(false);
                            broadcastChanges();
                        }else if (type.equals("item")){
                            if(turn == 1){
                                if(playerOneAmountPlacedThisRound >= 3) {
                                    playerOne.sendMessage(ChatColor.RED + "You have already placed three items this round!");
                                    return;
                                }
                            }else if(playerTwoAmountPlacedThisRound >= 3){
                                playerTwo.sendMessage(ChatColor.RED + "You have already placed three items this round!");
                                return;
                            }
                            if(Trait.JUMPSTART.isInMeta(meta)) {
                                selectedItem = item;
                                for (int i : getValidSlots(item)) {
                                    currentInv.setItem(i, validationPane);
                                }
                            }
                        }else{
                            selectedItem = null;
                            player.sendMessage(ChatColor.RED + "Please select a core or a jumpstart item");
                        }
                    }else{
                        selectedItem = null;
                        player.sendMessage(ChatColor.RED + "Please select a core or a jumpstart item");
                    }

                }else if(e.getCurrentItem() != null){
                    if(e.getCurrentItem().getItemMeta() != null) {
                        if(e.getCurrentItem().equals(validationPane)) {
                            placeItem(selectedItem, e.getSlot());
                            turn = 3 - turn;
                            if (getCore(turn) != null)
                                turn = 3 - turn;
                            broadcastChanges();
                        }else if(e.getCurrentItem().getItemMeta().displayName().equals(Component.text(ChatColor.GRAY + "End Turn"))) {
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
                    TraitsHolder itemTraits = new TraitsHolder(meta);
                    if(meta == null){
                        return;
                    }

                    if(meta.getPersistentDataContainer().equals(null)) {
                        return;
                    }

                    if(meta.getPersistentDataContainer().get(ItemHandler.getTypeKey(), PersistentDataType.STRING) == null) return;

                    String itemType = meta.getPersistentDataContainer().get(ItemHandler.getTypeKey(), PersistentDataType.STRING);

                    if(itemType.equalsIgnoreCase("core")){
                        player.sendMessage(ChatColor.RED + "Select an ITEM or a USABLE");
                        return;
                    }

                    int maxAmountPlacedPerItem = 3;
                    int maxAmountPlacedPerRound = 3;

                    if(getCore(turn).getItemMeta().getPersistentDataContainer().get(ItemHandler.getEffectIDKey(), PersistentDataType.INTEGER).equals(8)){
                        maxAmountPlacedPerRound = 4;
                    }

                    if(itemType.equalsIgnoreCase("item")) {
                        if(itemTraits.hasTrait(Trait.CAPBREAKER)){
                            maxAmountPlacedPerItem = itemTraits.getLevelOf(Trait.CAPBREAKER);
                        }
                    }else if(itemType.equalsIgnoreCase("usable")){
                        maxAmountPlacedPerItem = meta.getPersistentDataContainer().get(ItemHandler.getUseLimitKey(), PersistentDataType.SHORT);
                        System.out.println("Max amount for usable : " + maxAmountPlacedPerItem);
                    }

                    if(turn == 1){
                        if(playerOneAmountPlacedThisRound >= maxAmountPlacedPerRound){
                            player.sendMessage(ChatColor.RED + "You have already played three items this round");
                            return;
                        }
                        Short amountPlaced = playerOneItemsPlaced.get(item.getType());
                        System.out.println("Amount of this item placed : " + amountPlaced);
                        if(amountPlaced != null){
                            if(amountPlaced >= maxAmountPlacedPerItem){
                                player.sendMessage(ChatColor.RED + "You have already played this item the maximum times!");
                                return;
                            }
                        }
                    }else{
                        if(playerTwoAmountPlacedThisRound >= maxAmountPlacedPerRound){
                            player.sendMessage(ChatColor.RED + "You have already played three items this round");
                            return;
                        }
                        Short amountPlaced = playerTwoItemsPlaced.get(item.getType());
                        if(amountPlaced != null){
                            if(amountPlaced >= maxAmountPlacedPerItem){
                                player.sendMessage(ChatColor.RED + "You have already played this item the maximum times!");
                                return;
                            }
                        }
                    }

                    if(isPlayingSpaceFull(turn) & itemType.equalsIgnoreCase("item")){
                        player.sendMessage("Your playing space is full!");
                        return;
                    }

                    if(itemType.equalsIgnoreCase("item")) {
                        selectedItem = item;
                        for (int i : getValidSlots(item)) {
                            currentInv.setItem(i, validationPane);
                        }
                    }else if(itemType.equalsIgnoreCase("usable")){
                        usableClicked(item, e.getSlot());
                    }
                }else if(e.getClickedInventory().equals(currentInv)){
                    ItemStack item = e.getCurrentItem();
                    if(item == null || item.getType() == Material.AIR){
                        return;
                    }
                    if(e.getSlot() == 10 || e.getSlot() == 16){
                        if(item.getItemMeta().getPersistentDataContainer().has(ItemHandler.getEffectIDKey(), PersistentDataType.INTEGER)) {
                            Integer effectId = item.getItemMeta().getPersistentDataContainer().get(ItemHandler.getEffectIDKey(), PersistentDataType.INTEGER);
                            /*coreClicked(effectId);
                            if (turn == 1) {
                                broadcast(ChatColor.RED + "All red items gain immunity!");
                            } else {
                                broadcast(ChatColor.BLUE + "All blue items gain immunity!");
                            }*/
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
                                broadcast( ChatColor.RED + "Attacking phase has started!");
                                action(true);
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
                                action(false);
                            }
                        }else {
                            itemPlacementNextTurn();
                        }
                        broadcastChanges();
                    }else if(selectedItem == null){
                        return;
                    }else if(pendingUsableSelection){
                        usableSelectedUsableItem(e.getSlot());
                        broadcastChanges();
                    }else if(item.equals(validationPane)){
                        placeItem(selectedItem, e.getSlot());
                        itemPlacementNextTurn();
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
                                broadcast(ChatColor.RED + "Red has chosen their attackers!");
                            }else{
                                broadcast(ChatColor.BLUE + "Blue has chosen their attackers!");
                            }
                            action(true);
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
                        if(Trait.STUNNED.isInMeta(meta)){
                            (turn == 1 ? playerOne : playerTwo).sendMessage(ChatColor.RED + "That item is stunned!");
                            break;
                        }
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
                            action(false);
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
                        if(!attackersAndDefenders.containsValue(e.getSlot())){
                            selectedSlot = e.getSlot();
                            ItemStack defender =  chest.getBlockInventory().getItem(selectedSlot);
                            ItemMeta defenderMeta = defender.getItemMeta();
                            if(Trait.STUNNED.isInMeta(defenderMeta)){
                                (turn == 1 ? playerOne : playerTwo).sendMessage(ChatColor.RED + "That item is stunned!");
                                break;
                            }
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
