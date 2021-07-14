package salamander.chesticuffs.game;

import com.google.common.collect.Queues;
import net.kyori.adventure.text.Component;
import org.bukkit.*;
import org.bukkit.block.Chest;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitTask;
import salamander.chesticuffs.ChestManager;
import salamander.chesticuffs.Chesticuffs;
import salamander.chesticuffs.MessageLevel;
import salamander.chesticuffs.game.redstone.NoteBlockAction;
import salamander.chesticuffs.game.redstone.PistonAction;
import salamander.chesticuffs.game.redstone.RedstoneAction;
import salamander.chesticuffs.game.redstone.TNTAction;
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
    static public final NamespacedKey redstoneTriggerKey = new NamespacedKey(Chesticuffs.getPlugin(), "triggered");

    private final Player playerOne;
    private Player playerTwo;
    private final Chest chest;
    int roundNumber, turn = 0;
    Phase phase;
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
    boolean lavaBucketCorePlaced = false;
    boolean coalBlockCorePlaced = false;
    boolean saddleCorePlaced = false;
    Queue<RedstoneAction> redstoneActions;

    static UUID[] staffUUIDs;

    private boolean[] triggered; //Used for redstone items that can only be triggered once per round

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

        phase = Phase.CORE_PLACEMENT;

        triggered = new boolean[27];

        validationPane = new ItemStack(Material.LIME_STAINED_GLASS_PANE);
        ItemMeta meta = validationPane.getItemMeta();
        meta.displayName(Component.text(ChatColor.GREEN + "Place Item Here"));
        validationPane.setItemMeta(meta);
    }

    public boolean isGameFull(){
        return (playerTwo != null);
    }

    public void addPlayer(Player player){
        if(isGameFull()) return;
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
        nextTurn();
    }

    private void action(boolean longer){
        for(BukkitTask task : timerTasks){
            task.cancel();
        }
        timerTasks.clear();

        if(Chesticuffs.isDebugMode){
            return; //TODO Uncomment this
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
        chest.getSnapshotInventory().clear();
        chest.update();

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
        topLore.add(Component.text(ChatColor.GRAY + "" + ChatColor.BOLD + "Phase " + phase.getPhaseNumber() + ":"));
        switch(phase) {
            case CORE_PLACEMENT:
                topLore.add(Component.text( ChatColor.BOLD + "" + ChatColor.GRAY + "Core Placement"));
                break;
            case REDSTONE:
                topLore.add(Component.text(ChatColor.BOLD + "" + ChatColor.GRAY + "Redstone"));
            case OPENING_PHASE:
                topLore.add(Component.text(ChatColor.BOLD + "" + ChatColor.GRAY + "Item Placement"));
                break;
            case ATTACKER_SELECTION:
                topLore.add(Component.text(ChatColor.BOLD + "" + ChatColor.GRAY + "Declare Attackers"));
                break;
            case DEFENDER_SELECTION:
                topLore.add(Component.text(ChatColor.BOLD + "" + ChatColor.GRAY + "Declare Defenders"));
                break;
            case CLOSING_PHASE:
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

        List<Component> bottomStickLore = new ArrayList<>();
        bottomStickLore.add(Component.text(ChatColor.GRAY + "Print game state"));
        bottomStickMeta.lore(bottomStickLore);
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
            if(saddleCorePlaced){//getCore(1).getItemMeta().getPersistentDataContainer().get(ItemHandler.getEffectIDKey(), PersistentDataType.INTEGER) == 9
            //|| getCore(2).getItemMeta().getPersistentDataContainer().get(ItemHandler.getEffectIDKey(), PersistentDataType.INTEGER) == 9){
                swapLength = 2;
            }
        }catch (NullPointerException e){}
        return (roundNumber - swapLength) / swapLength % 2 + 1;
    }

    public void broadcastChanges(){
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

    private boolean buffItem(ItemStack item, int column){
        ItemMeta meta = item.getItemMeta();
        short ATK = meta.getPersistentDataContainer().get(ItemHandler.getDamageKey(), PersistentDataType.SHORT);
        short DEF = meta.getPersistentDataContainer().get(ItemHandler.getDefenceKey(), PersistentDataType.SHORT);
        short HP = meta.getPersistentDataContainer().get(ItemHandler.getHealthKey(), PersistentDataType.SHORT);
        TraitsHolder traits = new TraitsHolder(meta);
        if(column == 0 || column == 8){
            ATK = (short) Math.ceil(ATK * 0.5);
            HP = (short) Math.ceil(HP * 1.5);
        }else if(column == 3 || column == 5){
            ATK = (short) Math.ceil(ATK * 1.5);
            HP = (short) Math.ceil(HP * 0.5);
        }
        int side;
        if(column < 4){
            side = 1;
        }else{
            side = 2;
        }

        //Your core
        ItemStack core = getCore(side);
        if(core != null) {
            //Old ratshit switch
            //core.getItemMeta().getPersistentDataContainer().get(ItemHandler.getEffectIDKey(), PersistentDataType.INTEGER)

            //New chad switch
            switch (core.getType()) {
                case IRON_BLOCK:
                    HP += 1;
                    break;
                case OAK_SAPLING:
                case BIRCH_SAPLING:
                case ACACIA_SAPLING:
                case BAMBOO_SAPLING:
                case DARK_OAK_SAPLING:
                case JUNGLE_SAPLING:
                case SPRUCE_SAPLING:
                    if (traits.hasTrait(Trait.PLANT)) {
                        HP = (short) Math.ceil(HP * 0.5);
                    }
                    break;
                //put all core effects into this method
                //may not be optimal, but improves readability
                case LAVA_BUCKET:
                    lavaBucketCorePlaced = true;
                    break;
                case COAL_BLOCK:
                    coalBlockCorePlaced = true;
                    break;
                case SADDLE:
                    saddleCorePlaced = true;
                    break;
                case OXEYE_DAISY:
                    if (traits.hasTrait(Trait.PLANT)) {
                        HP -= 1;
                        DEF += 1;
                        if (HP <= 0) {
                            return false;
                        }
                    }
                    break;
                case DIAMOND_BLOCK:
                    DEF += 1;
                    break;
                case GOLD_BLOCK:
                    HP -= 1;
                    if (HP <= 0) {
                        return false;
                    }
                    break;
                case DRIED_KELP_BLOCK:
                    if(traits.hasTrait(Trait.AQUATIC)) HP++;
                    break;
                case ROSE_BUSH:
                    //GAME LOGIC FOR ROSE_BUSH IN COMBAT METHOD
                    break;
                case BONE_BLOCK:
                    //GAME LOGIC FOR BONE_BLOCK IN COMBAT METHOD
                    break;

            }
        }

        //Enemy core logic
        ItemStack enemyCore = getCore(3 - side);
        if(enemyCore != null){
            switch (enemyCore.getType()){//enemyCore.getItemMeta().getPersistentDataContainer().get(ItemHandler.getEffectIDKey(), PersistentDataType.INTEGER)){
                case JACK_O_LANTERN:
                    if(ATK > 0) ATK--;
                    break;
            }
        }
        if(traits.hasTrait(Trait.FRAGILE)){
            HP = 1;
            DEF = 0;
        }
        try {
            //boolean coalBlockCorePlaced = getCore(side).getItemMeta().getPersistentDataContainer().get(ItemHandler.getEffectIDKey(), PersistentDataType.INTEGER).equals(5) ||
            //        getCore(3 - side).getItemMeta().getPersistentDataContainer().get(ItemHandler.getEffectIDKey(), PersistentDataType.INTEGER).equals(5);
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

                            if(item == null) continue;
                            if(item.getItemMeta().getPersistentDataContainer().get(ItemHandler.getTypeKey(), PersistentDataType.STRING) == null) continue;
                            if(item.getItemMeta().getPersistentDataContainer().get(ItemHandler.getTypeKey(), PersistentDataType.STRING).equals("item")){
                                short HP = item.getItemMeta().getPersistentDataContainer().get(ItemHandler.getHealthKey(), PersistentDataType.SHORT);
                                TraitsHolder traits = new TraitsHolder(item);
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

        playerTwoAmountPlacedThisRound = 0;
        playerOneAmountPlacedThisRound = 0;
        playerOneSkipped = false;
        playerTwoSkipped = false;

        doFireDamage();
        broadcastChanges();
        //checkForDraw();
    }

    public Player getPlayerOne() {
        return playerOne;
    }

    public Player getPlayerTwo() {
        return playerTwo;
    }

    public Chest getChest() {
        return chest;
    }

    private void doFireDamage(){
        chest.update();
        /*boolean lavaBucketCorePlaced = false;
        for(int i : new int[]{10, 16}){
            if(chest.getBlockInventory().getItem(i).getType()==Material.LAVA_BUCKET){//.getItemMeta().getPersistentDataContainer().get(ItemHandler.getEffectIDKey(), PersistentDataType.INTEGER).equals(4)){
                lavaBucketCorePlaced = true;
                break;
            }
        }*/

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

    public void broadcast(String message){
        playerOne.sendMessage(message);
        if(playerOne != playerTwo) {
            playerTwo.sendMessage(message);
        }
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
                doCombatBetween(entry.getKey(), entry.getValue());
            }
        }

        for(int i = 0; i < 27; i++){
            ItemStack item = chest.getSnapshotInventory().getItem(i);
            if(item != null){
                TraitsHolder traits = new TraitsHolder(item);
                traits.removeTrait(Trait.IMMUNE);
                traits.removeTrait(Trait.STUNNED);
                traits.removeTrait(Trait.FACADE);
                traits.setTraitsOf(item);
                ItemHandler.setLore(item);
            }
        }

        defendingCoreData.set(ItemHandler.getHealthKey(), PersistentDataType.SHORT, coreHealth);
        defendingCore.setItemMeta(defendingCoreMeta);
        ItemHandler.setLore(defendingCore);
        if(coreHealth <= 0){
            //De-registers Game
            endGame(getPriority());
        }else{
            if(defendingCore.getType() == Material.ROSE_BUSH){//defendingCoreMeta.getPersistentDataContainer().get(ItemHandler.getEffectIDKey(), PersistentDataType.INTEGER) == 12){
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
        //TODO remove coreID as parameter, pass in either ItemStack or Material
        // make this switch statement use the material
        chest.update();
        switch(coreId){
            case(3):
                if(getCore(turn).getItemMeta().getPersistentDataContainer().get(ItemHandler.getHealthKey(), PersistentDataType.SHORT) <= 5){
                    broadcast(ChatColor.RED + "Cannot use enchanted golden apple, too low health!");
                    break;
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

        switch(selectedItem.getType()){
            case BELL: //Bell
            case STONECUTTER: //Stonecutter
            case GRINDSTONE: //Grindstone
            case ENDER_CHEST: //Ender Chest
            case FLOWER_POT: //Flower Pot
            case ARMOR_STAND:
                pendingUsableSelection = true;
                usableLore.set(1, Component.text(ChatColor.RED + "Select a played item!"));
                break;
            case SPONGE:
            case WET_SPONGE:
                pendingUsableSelection = true;
                usableLore.set(1, Component.text(ChatColor.RED + "Select a played aquatic item!"));
                break;
            case PAINTING:
                pendingUsableSelection = true;
                usableLore.set(1, Component.text(ChatColor.RED + "Select an item to become a facade!"));
                break;
            case COMPOSTER:
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

    private boolean isValidGameItem(ItemStack item){
        try{
            return item.getItemMeta().getPersistentDataContainer().get(ItemHandler.getTypeKey(), PersistentDataType.STRING).equals("item");
        }catch (NullPointerException e){
            return false;
        }
    }

    public int dealDamageTo(int slot, short ATK, boolean isTrueDamage){ //Returns -1 if damage couldn't be dealt, 0 if item was killed otherwise it returns 1
        ItemStack attackedItem = chest.getSnapshotInventory().getItem(slot);

        if(!isValidGameItem(attackedItem)) return -1;

        ItemMeta meta = attackedItem.getItemMeta();
        TraitsHolder traits = new TraitsHolder(meta);

        boolean killed;
        if(isTrueDamage){
            killed = ItemHandler.dealTrueDamageTo(meta, ATK);
        }else{
            killed = ItemHandler.dealDamageTo(meta, ATK);
        }

        if(killed){
            killItem(slot);
            return 0;
        }else{
            attackedItem.setItemMeta(meta);
        }

        return 1;
    }

    private Integer findTanker(int startSlot){
        int row = startSlot / 9;
        int col = startSlot % 9;

        if(row > 0) if(Trait.SOAK.isInItem(chest.getSnapshotInventory().getItem(startSlot - 9))) return startSlot - 9;
        if(row < 2) if(Trait.SOAK.isInItem(chest.getSnapshotInventory().getItem(startSlot + 9))) return startSlot + 9;
        if(col > 0) if(Trait.SOAK.isInItem(chest.getSnapshotInventory().getItem(startSlot - 1))) return startSlot - 1;
        if(col < 8) if(Trait.SOAK.isInItem(chest.getSnapshotInventory().getItem(startSlot + 1))) return startSlot + 1;
        return null;
    }

    public void doCombatBetween(int attackerSlot, int defenderSlot){
        Integer attackerTankerSlot = findTanker(attackerSlot);
        Integer defenderTankerSlot = findTanker(defenderSlot);

        ItemStack attackerItem = chest.getSnapshotInventory().getItem(attackerSlot);
        ItemStack defenderItem = chest.getSnapshotInventory().getItem(defenderSlot);

        ItemMeta attackerMeta = attackerItem.getItemMeta();
        ItemMeta defenderMeta = defenderItem.getItemMeta();

        TraitsHolder attackerTraits = new TraitsHolder(attackerMeta);
        TraitsHolder defenderTraits = new TraitsHolder(defenderMeta);

        ItemStack attackerTakingDamage = attackerItem;
        ItemMeta attackerMetaTakingDamage = attackerMeta;
        TraitsHolder attackerTraitsTakingDamage = attackerTraits;
        int attackerSlotTakingDamage = attackerSlot;
        if(attackerTankerSlot != null){
            attackerSlotTakingDamage = attackerTankerSlot;
            attackerTakingDamage = chest.getSnapshotInventory().getItem(attackerSlotTakingDamage);
            attackerMetaTakingDamage = attackerTakingDamage.getItemMeta();
            attackerTraitsTakingDamage = new TraitsHolder(attackerMetaTakingDamage);

            broadcast(ChatColor.GREEN + attackerTakingDamage.getType().toString() + " (Slot " + attackerSlotTakingDamage + ") tanks damage for " + attackerItem.getType() + " (Slot " + attackerSlot + ")");
        }

        ItemStack defenderTakingDamage = defenderItem;
        ItemMeta defenderMetaTakingDamage = defenderMeta;
        TraitsHolder defenderTraitsTakingDamage = defenderTraits;
        int defenderSlotTakingDamage = defenderSlot;
        if(defenderTankerSlot != null){
            defenderSlotTakingDamage = defenderTankerSlot;
            defenderTakingDamage = chest.getSnapshotInventory().getItem(defenderSlotTakingDamage);
            defenderMetaTakingDamage = defenderTakingDamage.getItemMeta();
            defenderTraitsTakingDamage = new TraitsHolder(defenderMetaTakingDamage);

            broadcast(ChatColor.GREEN + attackerTakingDamage.getType().toString() + " (Slot " + attackerSlotTakingDamage + ") tanks damage for " + attackerItem.getType() + " (Slot " + attackerSlot + ")");
        }

        short damageToDefender = calculateDamage(defenderMetaTakingDamage, attackerMeta, defenderTraitsTakingDamage, attackerTraits);
        short damageToAttacker = calculateDamage(attackerMetaTakingDamage, defenderMeta, attackerTraitsTakingDamage, defenderTraits);

        boolean didDefenderDie = dealDamageAndTriggerDetect(defenderSlotTakingDamage, defenderMetaTakingDamage, damageToDefender);
        boolean didAttackerDie = dealDamageAndTriggerDetect(attackerSlotTakingDamage, attackerMetaTakingDamage, damageToAttacker);

        if(attackerTakingDamage == attackerItem) attackerMeta = attackerMetaTakingDamage;
        if(defenderTakingDamage == defenderItem) defenderMeta = defenderMetaTakingDamage;

        if(damageToDefender > 0){
            broadcast(ChatColor.RED + defenderTakingDamage.getType().toString() + " (Slot " + defenderSlotTakingDamage + ") takes " + damageToDefender + " damage!");
        }

        if(damageToAttacker > 0){
            broadcast(ChatColor.RED + attackerTakingDamage.getType().toString() + " (Slot " + attackerSlotTakingDamage + ") takes " + damageToAttacker + " damage!");
        }

        if(didDefenderDie){
            killItem(defenderSlotTakingDamage);

            if(defenderTraitsTakingDamage.hasTrait(Trait.SHRAPNEL)){
                didAttackerDie = ItemHandler.dealTrueDamageTo(attackerMeta, (short) 2);
            }

            if(getCore(getSideFromSlot(attackerSlot)).getType().equals(Material.BONE_BLOCK)){ //When an ally kills an enemy, that ally gains one HP
                ItemHandler.setHP(attackerMeta, (short) (ItemHandler.getHP(attackerMeta) + 1));
            }

            if(getCore(getSideFromSlot(defenderSlot)).getType().equals(Material.BONE_BLOCK)){
                Integer randSlot = getRandomItemFromSide(getSideFromSlot(defenderSlot));
                if(randSlot != null) {
                    ItemStack unluckyItem = chest.getSnapshotInventory().getItem(randSlot);
                    ItemMeta unluckyItemsMeta = unluckyItem.getItemMeta();

                    if(ItemHandler.dealTrueDamageTo(unluckyItemsMeta, (short) 1)){
                        killItem(randSlot);
                        broadcast(ChatColor.RED + unluckyItem.getType().toString() + " (Slot " + randSlot + ") was killed because of bone block lol");
                    }

                    unluckyItem.setItemMeta(unluckyItemsMeta);
                    ItemHandler.setLore(unluckyItem);
                }
            }
        }

        if(didAttackerDie){
            killItem(attackerSlotTakingDamage);

            if(attackerTraitsTakingDamage.hasTrait(Trait.SHRAPNEL)){
                if(ItemHandler.dealTrueDamageTo(defenderMeta, (short) 2) && !didDefenderDie){ //I don't know how else to do this
                    killItem(defenderSlotTakingDamage);

                    if(defenderTraitsTakingDamage.hasTrait(Trait.SHRAPNEL)){
                        didAttackerDie = ItemHandler.dealTrueDamageTo(attackerMeta, (short) 2);
                    }


                    if(getCore(getSideFromSlot(attackerSlot)).getType().equals(Material.BONE_BLOCK)){ //When an ally kills an enemy, that ally gains one HP
                        ItemHandler.setHP(attackerMeta, (short) (ItemHandler.getHP(attackerMeta) + 1));
                    }

                    if(getCore(getSideFromSlot(defenderSlot)).getType().equals(Material.BONE_BLOCK)){
                        Integer randSlot = getRandomItemFromSide(getSideFromSlot(defenderSlot));
                        if(randSlot != null) {
                            ItemStack unluckyItem = chest.getSnapshotInventory().getItem(randSlot);
                            ItemMeta unluckyItemsMeta = unluckyItem.getItemMeta();

                            if(ItemHandler.dealTrueDamageTo(unluckyItemsMeta, (short) 1)){
                                killItem(randSlot);
                                broadcast(ChatColor.RED + unluckyItem.getType().toString() + " (Slot " + randSlot + ") was killed because of bone block lol");
                            }

                            unluckyItem.setItemMeta(unluckyItemsMeta);
                            ItemHandler.setLore(unluckyItem);
                        }
                    }
                }
            }

            if(getCore(getSideFromSlot(defenderSlot)).getType().equals(Material.BONE_BLOCK)){ //When an ally kills an enemy, that ally gains one HP
                ItemHandler.setHP(defenderMeta, (short) (ItemHandler.getHP(defenderMeta) + 1));
            }

            if(getCore(getSideFromSlot(attackerSlot)).getType().equals(Material.BONE_BLOCK)){
                Integer randSlot = getRandomItemFromSide(getSideFromSlot(attackerSlot));
                if(randSlot != null) {
                    ItemStack unluckyItem = chest.getSnapshotInventory().getItem(randSlot);
                    ItemMeta unluckyItemsMeta = unluckyItem.getItemMeta();

                    if(ItemHandler.dealTrueDamageTo(unluckyItemsMeta, (short) 1)){
                        killItem(randSlot);
                        broadcast(ChatColor.RED + unluckyItem.getType().toString() + " (Slot " + randSlot + ") was killed because of bone block lol");
                    }

                    unluckyItem.setItemMeta(unluckyItemsMeta);
                    ItemHandler.setLore(unluckyItem);
                }
            }
        }

        attackerItem.setItemMeta(attackerMeta);
        defenderItem.setItemMeta(defenderMeta);

        ItemHandler.setLore(attackerItem);
        ItemHandler.setLore(defenderItem);

        if(attackerItem != attackerTakingDamage){
            attackerTakingDamage.setItemMeta(attackerMetaTakingDamage);
            ItemHandler.setLore(attackerTakingDamage);
        }

        if(defenderItem != defenderTakingDamage){
            defenderTakingDamage.setItemMeta(defenderMetaTakingDamage);
            ItemHandler.setLore(defenderTakingDamage);
        }
    }

    private Integer getRandomItemFromSide(int side){
        List<Integer> itemsOnSide = new ArrayList<>();

        int lowerBound = side == 1 ? 0 : 5;
        int upperBound = side == 1 ? 4 : 9;

        for(int row = 0; row < 3; row++){
            for(int column = lowerBound; column < upperBound; column++){
                int index = row * 9 + column;
                ItemStack item = chest.getSnapshotInventory().getItem(index);

                if(isValidGameItem(item)){
                    itemsOnSide.add(index);
                }
            }
        }

        Random rand = new Random();

        if(itemsOnSide.size() == 0) return null;

        return itemsOnSide.get(rand.nextInt(itemsOnSide.size()));
    }

    private boolean dealDamageAndTriggerDetect(int slot, ItemMeta meta, short damage){//TODO Actually detect the redstone signal
        return ItemHandler.dealTrueDamageTo(meta, damage);
    }

    public short calculateDamage(ItemMeta attackedMeta, ItemMeta attackingMeta, TraitsHolder attackedTraits, TraitsHolder attackingTraits){
        short baseDamage = ItemHandler.getATK(attackingMeta);

        if(attackingTraits.hasTrait(Trait.FLAME)){
            if(attackedTraits.hasTrait(Trait.FLAMMABLE)){
                baseDamage *= 2;
            }

            if(attackedTraits.hasTrait(Trait.FIRE_RESISTANT)){
                baseDamage = 0;
            }
        }

        if(attackedTraits.hasTrait(Trait.AQUATIC)){
            if(attackedTraits.hasTrait(Trait.AQUA_RESISTANT)){
                baseDamage = 0;
            }
        }

        if(attackedTraits.hasTrait(Trait.FACADE)){
            baseDamage = 0;
        }

        if(attackedTraits.hasTrait(Trait.IMMUNE)){
            baseDamage = 0;
        }

        return (short) Math.max(0, baseDamage - ItemHandler.getDEF(attackedMeta));
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
        Chesticuffs.LOGGER.log("########Game State########", MessageLevel.INFO);
        Chesticuffs.LOGGER.log("Game Identifier : " + id, MessageLevel.INFO);
        Chesticuffs.LOGGER.log("Player One : " + playerOne.getName(), MessageLevel.INFO);
        Chesticuffs.LOGGER.log("Player Two : " + playerTwo.getName(), MessageLevel.INFO);
        Chesticuffs.LOGGER.log("Round " + roundNumber + ", Phase " + phase + ", " + (turn == 1 ? "Red" : "Blue") + "'s turn", MessageLevel.INFO);
        Chesticuffs.LOGGER.log("Priority: " + (getPriority() == 1 ? "red" : "blue"), MessageLevel.INFO);
        Chesticuffs.LOGGER.log("Player One:", MessageLevel.INFO);
        try{ Chesticuffs.LOGGER.log("  Skips this phase : " + amountSkipsPlayerOne, MessageLevel.INFO);}catch (NullPointerException e){}
        try{ Chesticuffs.LOGGER.log("  Amount placed this round : " + playerOneAmountPlacedThisRound, MessageLevel.INFO);}catch (NullPointerException e){}
        try{ Chesticuffs.LOGGER.log("  Skipped : " + playerOneSkipped, MessageLevel.INFO);}catch (NullPointerException e){}
        Chesticuffs.LOGGER.log("Player Two:", MessageLevel.INFO);
        try{ Chesticuffs.LOGGER.log("  Skips this phase : " + amountSkipsPlayerTwo, MessageLevel.INFO);}catch (NullPointerException e){}
        try{ Chesticuffs.LOGGER.log("  Amount placed this round : " + playerTwoAmountPlacedThisRound, MessageLevel.INFO);}catch (NullPointerException e){}
        try{ Chesticuffs.LOGGER.log("  Skipped : " + playerTwoSkipped, MessageLevel.INFO);}catch (NullPointerException e){}
        try{ Chesticuffs.LOGGER.log("Selected Slot : " + selectedSlot, MessageLevel.INFO);}catch (NullPointerException e){}
        try{ Chesticuffs.LOGGER.log("Selected Item : " + selectedItem.getType().toString(), MessageLevel.INFO);}catch (NullPointerException e){}
        Chesticuffs.LOGGER.log("Pending Usable Selection : " + pendingUsableSelection, MessageLevel.INFO);
        try{ Chesticuffs.LOGGER.log("Usable Temporary Data Slot : " + usableTemporarySlot, MessageLevel.INFO);}catch (NullPointerException e){}
    }

    private void applyTrait(int slot, Trait trait)
    {
        ItemStack clickedItem = chest.getSnapshotInventory().getItem(slot);
        ItemMeta clickedItemMeta = clickedItem.getItemMeta();
        TraitsHolder traits = new TraitsHolder(clickedItemMeta);
        if(!traits.hasTrait(trait))
        {
            if(traits.addTrait(trait))
            {
                traits.setTraitsOf(clickedItemMeta);
                broadcast(ChatColor.GREEN + "Trait: " + trait + "has been applied to " + clickedItem.getType());
            }
        }
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

        //int effectID = selectedItem.getItemMeta().getPersistentDataContainer().get(ItemHandler.getEffectIDKey(), PersistentDataType.INTEGER);

        Chesticuffs.LOGGER.log("An item has been selected for a usable. " +
                               "Usable item clicked : " +
                                clickedItem.getType().toString(), MessageLevel.DEBUG_INFO);
        boolean succesfullyUsed = false;
        boolean clickedItemDied = false;
        boolean clearSelectedItem = true;
        TraitsHolder traits = new TraitsHolder(clickedItemMeta);

        //ItemStack oldItem = selectedItem;

        short ATK = clickedItemMeta.getPersistentDataContainer().get(ItemHandler.getDamageKey(), PersistentDataType.SHORT);
        short DEF = clickedItemMeta.getPersistentDataContainer().get(ItemHandler.getDefenceKey(), PersistentDataType.SHORT);
        short HP = clickedItemMeta.getPersistentDataContainer().get(ItemHandler.getHealthKey(), PersistentDataType.SHORT);

        switch(selectedItem.getType()){
            case BELL: //Bell
                if(traits.addTrait(Trait.STUNNED))
                {
                    traits.setTraitsOf(clickedItemMeta);
                    succesfullyUsed = true;
                    broadcast(ChatColor.GREEN + "Bell has been used!");
                }
                break;
            case STONECUTTER: //Stonecutter
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
                broadcast(ChatColor.GREEN + "Stonecutter has been used!");
                break;
            case GRINDSTONE: //Grindstone
                Map<Enchantment, Integer> enchantments = clickedItemMeta.getEnchants();
                int amount = enchantments.size();
                if(amount > 0) {
                    int index = (new Random()).nextInt(amount);
                    int i = 0;
                    for(Enchantment enchant : enchantments.keySet()){
                        if(i == index){
                            clickedItemMeta.removeEnchant(enchant);
                            succesfullyUsed = true;
                            broadcast("Grindstone has been used!");
                            break;
                        }

                        i++;
                    }
                }
                break;
            case ENDER_CHEST: //Ender Chest
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
                        broadcast(ChatColor.GREEN + "Ender chest has been used!");
                        broadcast(ChatColor.GREEN + "Switched items with ender chest!");
                    }
                }
                break;
            case FLOWER_POT://Flower Pot
            case ARMOR_STAND://Armour Stand

                String broadcast = selectedItem.getType() == Material.FLOWER_POT ?
                "Flower pot has been used!" :
                "Armor stand has been used!";
                broadcast(ChatColor.GREEN + broadcast);

                Trait trait = (selectedItem.getType() == Material.FLOWER_POT ? Trait.POTTABLE : Trait.STANDABLE);
                int slotX = slot / 3;
                Chesticuffs.LOGGER.log("Slot X : " + slotX + ", Turn : " + turn, MessageLevel.DEBUG_INFO);
                if(turn == 1 && slotX > 3) break;
                if(turn == 2 && slotX < 5) break;

                //TODO is a || Trait.STANDABLE.isInMeta(clickedItemMeta) needed
                // in the below if statement?  Seems like the armor stand is not yet added but not
                // entirely sure what the HashMap is for in future so leaving for now.

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
            case SPONGE://Sponge
                if(Trait.AQUATIC.isInMeta(clickedItemMeta))
                {
                    if(!Trait.DRIED.isInMeta(clickedItemMeta))
                    {
                        if(traits.addTrait(Trait.DRIED))
                        {
                            traits.setTraitsOf(clickedItemMeta);
                            succesfullyUsed = true;
                            broadcast(ChatColor.GREEN + "Sponge has been used!");
                        }
                    }
                    else
                    {
                        setInfoLoreLine(selectedItem, ChatColor.RED + "Item already dried");
                        //cannot use sponge already dried
                    }
                }
                else
                {
                    setInfoLoreLine(selectedItem, ChatColor.RED + "Item must be aquatic");
                    //item not aquatic
                }
                break;
            case WET_SPONGE://Wet Sponge
                if(Trait.AQUATIC.isInMeta(clickedItemMeta))
                {
                    if( Trait.DRIED.isInMeta(clickedItemMeta))
                    {
                        if(traits.removeTrait(Trait.DRIED))
                        {
                            traits.setTraitsOf(clickedItemMeta);
                            succesfullyUsed = true;
                            broadcast(ChatColor.GREEN + "Wet sponge has been used!");
                        }
                    }
                    else
                    {
                        setInfoLoreLine(selectedItem, ChatColor.RED + "Item already dried");
                    }

                }
                else
                {
                    setInfoLoreLine(selectedItem, ChatColor.RED + "Item must be aquatic");
                }
                break;
            case PAINTING:
                if(!Trait.FACADE.isInMeta(clickedItemMeta))//doesn't have a facade
                {
                    //turn the selected item into the facade.
                    //Not replacing the item with the facade may be too overpowered???
                    // At end of turn, turn it back
                    // does it work for cores?
                    if(traits.addTrait(Trait.FACADE))
                    {
                        traits.setTraitsOf(clickedItemMeta);
                        setInfoLoreLine(clickedItem, ChatColor.RED + "Facade: Defends against all attacks for 1 turn");
                        succesfullyUsed = true;
                        broadcast(ChatColor.GREEN + "Painting has been used!");
                    }
                }
                else
                {
                    //probably not needed due to only allowing 1 facade per player per game
                    setInfoLoreLine(selectedItem, ChatColor.RED + "Item already facade");
                }
                break;
            /*case LEATHER_HELMET:
                DEF++;
                clickedItemMeta.getPersistentDataContainer().set(ItemHandler.getDefenceKey(), PersistentDataType.SHORT, DEF);
                succesfullyUsed = true;
                broadcast(ChatColor.GREEN + "Leather helmet has been used!");
                break;
            case LEATHER_CHESTPLATE:
                DEF+=3;
                clickedItemMeta.getPersistentDataContainer().set(ItemHandler.getDefenceKey(), PersistentDataType.SHORT, DEF);
                succesfullyUsed = true;
                broadcast(ChatColor.GREEN + "Leather chestplate has been used!");
                break;
            case LEATHER_LEGGINGS:
                DEF+=2;
                clickedItemMeta.getPersistentDataContainer().set(ItemHandler.getDefenceKey(), PersistentDataType.SHORT, DEF);
                succesfullyUsed = true;
                broadcast(ChatColor.GREEN + "Leather leggings have been used!");
                break;
            case LEATHER_BOOTS:
                DEF++;
                clickedItemMeta.getPersistentDataContainer().set(ItemHandler.getDefenceKey(), PersistentDataType.SHORT, DEF);
                succesfullyUsed = true;
                broadcast(ChatColor.GREEN + "Leather boots have been used!");
                break;
            case CHAINMAIL_HELMET:
                DEF+=2;
                clickedItemMeta.getPersistentDataContainer().set(ItemHandler.getDefenceKey(), PersistentDataType.SHORT, DEF);
                succesfullyUsed = true;
                broadcast(ChatColor.GREEN + "Chainmail helmet has been used!");
                break;
            case CHAINMAIL_CHESTPLATE:
                DEF+=5;
                clickedItemMeta.getPersistentDataContainer().set(ItemHandler.getDefenceKey(), PersistentDataType.SHORT, DEF);
                succesfullyUsed = true;
                broadcast(ChatColor.GREEN + "Chainmail chestplate has been used!");
                break;
            case CHAINMAIL_LEGGINGS:
                DEF+=4;
                clickedItemMeta.getPersistentDataContainer().set(ItemHandler.getDefenceKey(), PersistentDataType.SHORT, DEF);
                succesfullyUsed = true;
                broadcast(ChatColor.GREEN + "Chainmail leggings have been used!");
                break;
            case CHAINMAIL_BOOTS:
                DEF+=1;
                clickedItemMeta.getPersistentDataContainer().set(ItemHandler.getDefenceKey(), PersistentDataType.SHORT, DEF);
                succesfullyUsed = true;
                broadcast(ChatColor.GREEN + "Chainmail boots have been used!");
                break;
            case IRON_HELMET:
                DEF+=2;
                clickedItemMeta.getPersistentDataContainer().set(ItemHandler.getDefenceKey(), PersistentDataType.SHORT, DEF);
                succesfullyUsed = true;
                broadcast(ChatColor.GREEN + "Iron helmet has been used!");
                break;
            case IRON_CHESTPLATE:
                DEF+=6;
                clickedItemMeta.getPersistentDataContainer().set(ItemHandler.getDefenceKey(), PersistentDataType.SHORT, DEF);
                succesfullyUsed = true;
                broadcast(ChatColor.GREEN + "Iron chestplate has been used!");
                break;
            case IRON_LEGGINGS:
                DEF+=5;
                clickedItemMeta.getPersistentDataContainer().set(ItemHandler.getDefenceKey(), PersistentDataType.SHORT, DEF);
                succesfullyUsed = true;
                broadcast(ChatColor.GREEN + "Iron leggings have been used!");
                break;
            case IRON_BOOTS:
                DEF+=2;
                clickedItemMeta.getPersistentDataContainer().set(ItemHandler.getDefenceKey(), PersistentDataType.SHORT, DEF);
                succesfullyUsed = true;
                broadcast(ChatColor.GREEN + "Iron boots have been used!");
                break;
            case GOLDEN_HELMET:
                DEF+=2;
                clickedItemMeta.getPersistentDataContainer().set(ItemHandler.getDefenceKey(), PersistentDataType.SHORT, DEF);
                succesfullyUsed = true;
                broadcast(ChatColor.GREEN + "Golden helmet has been used!");
                break;
            case GOLDEN_CHESTPLATE:
                DEF+=5;
                clickedItemMeta.getPersistentDataContainer().set(ItemHandler.getDefenceKey(), PersistentDataType.SHORT, DEF);
                succesfullyUsed = true;
                broadcast(ChatColor.GREEN + "Golden chestplate has been used!");
                break;
            case GOLDEN_LEGGINGS:
                DEF+=3;
                clickedItemMeta.getPersistentDataContainer().set(ItemHandler.getDefenceKey(), PersistentDataType.SHORT, DEF);
                succesfullyUsed = true;
                broadcast(ChatColor.GREEN + "Golden leggings have been used!");
                break;
            case GOLDEN_BOOTS:
                DEF+=1;
                clickedItemMeta.getPersistentDataContainer().set(ItemHandler.getDefenceKey(), PersistentDataType.SHORT, DEF);
                succesfullyUsed = true;
                broadcast(ChatColor.GREEN + "Golden boots have been used!");
                break;
            case DIAMOND_HELMET:
                DEF+=3;
                clickedItemMeta.getPersistentDataContainer().set(ItemHandler.getDefenceKey(), PersistentDataType.SHORT, DEF);
                succesfullyUsed = true;
                broadcast(ChatColor.GREEN + "Diamond helmet has been used!");
                break;
            case DIAMOND_CHESTPLATE:
                DEF+=8;
                clickedItemMeta.getPersistentDataContainer().set(ItemHandler.getDefenceKey(), PersistentDataType.SHORT, DEF);
                succesfullyUsed = true;
                broadcast(ChatColor.GREEN + "Diamond chestplate has been used!");
                break;
            case DIAMOND_LEGGINGS:
                DEF+=6;
                clickedItemMeta.getPersistentDataContainer().set(ItemHandler.getDefenceKey(), PersistentDataType.SHORT, DEF);
                succesfullyUsed = true;
                broadcast(ChatColor.GREEN + "Diamond leggings have been used!");
                break;
            case DIAMOND_BOOTS:
                DEF+=3;
                clickedItemMeta.getPersistentDataContainer().set(ItemHandler.getDefenceKey(), PersistentDataType.SHORT, DEF);
                succesfullyUsed = true;
                broadcast(ChatColor.GREEN + "Diamond boots have been used!");
                break;
            case NETHERITE_HELMET:
                DEF+=3;
                clickedItemMeta.getPersistentDataContainer().set(ItemHandler.getDefenceKey(), PersistentDataType.SHORT, DEF);

                if(traits.addTrait(Trait.FIRE_RESISTANT))traits.setTraitsOf(clickedItemMeta);

                succesfullyUsed = true;
                broadcast(ChatColor.GREEN + "Netherite helmet has been used!");
                break;
            case NETHERITE_CHESTPLATE:
                DEF+=8;
                clickedItemMeta.getPersistentDataContainer().set(ItemHandler.getDefenceKey(), PersistentDataType.SHORT, DEF);

                if(traits.addTrait(Trait.FIRE_RESISTANT))traits.setTraitsOf(clickedItemMeta);

                succesfullyUsed = true;
                broadcast(ChatColor.GREEN + "Netherite chestplate has been used!");
                break;
            case NETHERITE_LEGGINGS:
                DEF+=6;
                clickedItemMeta.getPersistentDataContainer().set(ItemHandler.getDefenceKey(), PersistentDataType.SHORT, DEF);

                if(traits.addTrait(Trait.FIRE_RESISTANT))traits.setTraitsOf(clickedItemMeta);

                succesfullyUsed = true;
                broadcast(ChatColor.GREEN + "Netherite leggings have been used!");
                break;
            case NETHERITE_BOOTS:
                DEF+=3;
                clickedItemMeta.getPersistentDataContainer().set(ItemHandler.getDefenceKey(), PersistentDataType.SHORT, DEF);

                if(traits.addTrait(Trait.FIRE_RESISTANT))traits.setTraitsOf(clickedItemMeta);

                succesfullyUsed = true;
                broadcast(ChatColor.GREEN + "Netherite boots have been used!");
                break;
            case TURTLE_HELMET:
                DEF+=2;
                clickedItemMeta.getPersistentDataContainer().set(ItemHandler.getDefenceKey(), PersistentDataType.SHORT, DEF);

                if(traits.addTrait(Trait.AQUA_RESISTANT))traits.setTraitsOf(clickedItemMeta);

                succesfullyUsed = true;
                broadcast(ChatColor.GREEN + "Turtle helmet has been used!");
                break;
            //TODO clean up armour cases, might reduce readbility but
            // will reduce size of file to make scrolling easier
            case CHAIN://needs implementing
                break;
            //TODO finish white dye
            // needs to return stats to prior to dying the item
            // not necessarily default as item could have modified stats from another item
            // just needs to remove another dye's effect.
            case WHITE_DYE:
                if(Trait.DYED.isInMeta(clickedItemMeta))
                {
                    if(traits.removeTrait(Trait.DYED))
                    {
                        traits.setTraitsOf(clickedItemMeta);

                        /*int a = clickedItemMeta.getPersistentDataContainer().get(ItemHandler.getDamageKey(), PersistentDataType.SHORT);
                        int d = clickedItemMeta.getPersistentDataContainer().get(ItemHandler.getDefenceKey(), PersistentDataType.SHORT);
                        int h = clickedItemMeta.getPersistentDataContainer().get(ItemHandler.getHealthKey(), PersistentDataType.SHORT);


                        succesfullyUsed = true;
                        broadcast(ChatColor.GREEN + "White dye has been used!");
                    }
                }
                else
                {
                    setInfoLoreLine(selectedItem, ChatColor.RED + "Item not dyed");
                }
                break;*/
            default:
                Chesticuffs.LOGGER.log("Reached default of usables switch statement", MessageLevel.ERROR);
        }

        if(succesfullyUsed) {
            if(!clickedItemDied) {
                Chesticuffs.LOGGER.log("Usable didn't make item die!", MessageLevel.DEBUG_INFO);
                clickedItem.setItemMeta(clickedItemMeta);
                ItemHandler.setLore(clickedItem);
                //chest.getSnapshotInventory().setItem(slot, clickedItem);
            }else{
                Chesticuffs.LOGGER.log("Usable made item die!", MessageLevel.DEBUG_INFO);
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

    //why did I think this would be a good idea this is rat shit
    private int getSideFromSlot(int slot)
    {
        //if on left side
        if(slot < 4 || slot > 8 && slot < 13 ||
           slot > 17 && slot < 22)
        {
            return 1;
        }
        else
        {
            return 2;
        }
    }

    private String getColorFromSide(int side){
        return side == 1 ? "red" : "blue";
    }

    private ChatColor getChatColorFromSide(int side){
        return side == 1 ? ChatColor.RED : ChatColor.BLUE;
    }

    public void placeItem(ItemStack item, int slot){
        ItemStack itemToBePlaced = new ItemStack(item);
        itemToBePlaced.setAmount(1);

        TraitsHolder traits = new TraitsHolder(itemToBePlaced.getItemMeta());

        boolean placed = buffItem(itemToBePlaced, slot % 9);
        item.setAmount(item.getAmount() - 1);
        if(placed) {
            chest.getSnapshotInventory().setItem(slot, itemToBePlaced);

            //TODO This causes an error when placing jumpstart items because of getCore() returning null. Commenting out for now
            //if you're clicking on your side and your core is a music disc...
            /*if(getSideFromSlot(slot) == 1)
            {
                //The one time where using the effectidKey is better is when there's multiple variants of the item
                //Checking if the core is a music disc
                if(getCore(1).getItemMeta().getPersistentDataContainer().get(ItemHandler.getEffectIDKey(), PersistentDataType.INTEGER).equals(13))
                {
                    //applyTrait(slot, Trait.RAGE);
                }
            }
            else
            {
                if(getCore(2).getItemMeta().getPersistentDataContainer().get(ItemHandler.getEffectIDKey(), PersistentDataType.INTEGER).equals(13))
                {
                    //applyTrait(slot, Trait.RAGE);
                }
            }*/

        }
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

    int redstonePlayed = 0;

    void nextTurn(){nextTurn(false);}
    void nextTurn(boolean skipped){
        switch (phase){
            case CORE_PLACEMENT:
                if(getCore(1) == null || getCore(2) == null){
                    turn = 3 - turn;
                    if(getCore(turn) != null)
                        turn = 3 - turn;
                }else{
                    playerOneSkipped = false;
                    playerTwoSkipped = false;
                    amountSkipsPlayerOne = 0;
                    amountSkipsPlayerTwo = 0;
                    selectedItem = null;
                    turn = getPriority();
                    phase = Phase.OPENING_PHASE;
                }
                action(false);
                broadcastChanges();
                break;
            case REDSTONE:
                redstonePlayed++;
                if(redstonePlayed == 2){
                    triggered = new boolean[27];
                    playerOneSkipped = false;
                    playerTwoSkipped = false;
                    amountSkipsPlayerOne = 0;
                    amountSkipsPlayerTwo = 0;
                    selectedItem = null;
                    turn = getPriority();
                    phase = Phase.OPENING_PHASE;
                    broadcast(ChatColor.DARK_PURPLE + "Redstone phase ended!");
                }else {
                    turn = 3 - turn;
                    beginRedstoneTurn(turn);
                }
                action(true);
                broadcastChanges();
                break;
            case OPENING_PHASE:
            case CLOSING_PHASE:
                if(skipped) {
                    if (turn == 1) {
                        playerOneSkipped = true;
                        amountSkipsPlayerOne += 1;
                        playerTwo.sendMessage(ChatColor.RED + "Red" + ChatColor.WHITE + " skipped. Your turn!");
                    } else {
                        playerTwoSkipped = true;
                        amountSkipsPlayerTwo += 1;
                        playerOne.sendMessage(ChatColor.BLUE + "Blue" + ChatColor.WHITE + " skipped. Your turn!");
                    }
                }
                selectedItem = null;
                if(playerOneSkipped && playerTwoSkipped){
                    if(phase.equals(Phase.OPENING_PHASE)){
                        phase = Phase.ATTACKER_SELECTION;
                        selectedItem = null;
                        turn = getPriority();
                        attackersAndDefenders.clear();
                        attackersSelected = 0;
                        broadcast( ChatColor.RED + "Attacking phase has started!");
                        action(true);
                    }else if(phase.equals(Phase.CLOSING_PHASE)){
                        endRound();
                        phase = Phase.REDSTONE;
                        selectedItem = null;
                        roundNumber += 1;
                        turn = getPriority();
                        redstonePlayed = 0;
                        broadcast(ChatColor.GREEN + "Round " + roundNumber + " has started!");
                        if(getPriority() == 1){
                            broadcast(ChatColor.RED + "Red Priority");
                        }else{
                            broadcast(ChatColor.BLUE + "Blue Priority");
                        }
                        beginRedstoneTurn(turn);
                        action(true);
                    }
                }else {
                    itemPlacementNextTurn();
                }
                broadcastChanges();
                break;
            case ATTACKER_SELECTION:
                turn = 3 - turn;
                phase = Phase.DEFENDER_SELECTION;
                selectedSlot = null;
                if(getPriority() == 1){
                    broadcast(ChatColor.RED + "Red has chosen their attackers!");
                }else{
                    broadcast(ChatColor.BLUE + "Blue has chosen their attackers!");
                }
                action(true);
                broadcastChanges();
                break;
            case DEFENDER_SELECTION:
                phase = Phase.CLOSING_PHASE;
                turn = 3 - getPriority();
                selectedItem = null;
                playerOneSkipped = false;
                playerTwoSkipped = false;
                broadcastChanges();
                combat();
                action(false);
                break;
        }

        pendingUsableSelection = false;
        usableTemporarySlot = null;
        selectedItem = null;
        selectedSlot = null;

        if(playerOne == playerTwo){
            if(Chesticuffs.isDebugMode){
                playerOne.openInventory(turn == 1 ? playerOneInventory : playerTwoInventory);
                playerOne.sendMessage(Component.text(ChatColor.GRAY + "Auto switching! :)"));
            }
        }
    }

    @SuppressWarnings("deprecation")
    static public ItemStack getDropperItem(){
        Random rand = new Random();
        int n = rand.nextInt(46);

        if(n <= 0){
            ItemStack sixtyThreeDiamondAxes = ItemHandler.createItem(Material.DIAMOND_AXE, 63, 10, 3, 8);
            ItemMeta meta = sixtyThreeDiamondAxes.getItemMeta();
            meta.addEnchant(Enchantment.LUCK, 0, true);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            sixtyThreeDiamondAxes.setItemMeta(meta);
            return sixtyThreeDiamondAxes;
        }

        n -= 5;
        if(n <= 0){
            ItemStack staffHead = ItemHandler.createItem(Material.PLAYER_HEAD, 1, 3, 2, 1);
            SkullMeta meta = (SkullMeta) staffHead.getItemMeta();
            int index = rand.nextInt(staff.length);
            meta.setOwner(staff[index]);
            staffHead.setItemMeta(meta);
            return staffHead;
        }

        n -= 5;
        if(n <= 0){
            return ItemHandler.createItem(Material.COD, 1, 1, 0, 1);
        }

        n -= 5;
        if(n <= 0){
            return ItemHandler.createItem(Material.TROPICAL_FISH, 1, 1, 0, 1);
        }

        n -= 5;
        if(n <= 0){
            return ItemHandler.createItem(Material.SALMON, 1, 1, 0, 1);
        }

        n -= 25;
        if(n <= 0){
            return ItemHandler.createItem(Material.DIRT, 1, 1, 0, 1);
        }

        return null; //TODO Actually implement this
    }

    private boolean hasAlreadyBeenPowered[] = null;
    private void powerItem(int slot, boolean reset, boolean triggerNextRound){
        int roundOffset = triggerNextRound ? 1 : 0;

        Chesticuffs.LOGGER.log("Attempting to power slot " + slot, MessageLevel.DEBUG_INFO);
        if(reset) {
            hasAlreadyBeenPowered = new boolean[27];
            Chesticuffs.LOGGER.log("Resetted already powered list", MessageLevel.DEBUG_INFO);
        }

        if(hasAlreadyBeenPowered[slot]) {
            Chesticuffs.LOGGER.log("Slot has already been powered this round", MessageLevel.DEBUG_INFO);
            return;
        }
        hasAlreadyBeenPowered[slot] = true;

        ItemStack item = chest.getSnapshotInventory().getItem(slot);
        if(item == null) return;
        ItemMeta meta = item.getItemMeta();
        if(meta == null) return;

        int row = slot / 9;
        int column = slot % 9;

        Chesticuffs.LOGGER.log("Powering slot " + slot + " ( " + row + ", " + column + " ) ", MessageLevel.DEBUG_INFO);
        try {
            if (meta.getPersistentDataContainer().get(ItemHandler.getTypeKey(), PersistentDataType.STRING).equals("item")) {
                TraitsHolder traits = new TraitsHolder(meta);
                if (!traits.hasTrait(Trait.REDSTONE)) return;
                if (traits.hasTrait(Trait.WIRE)) {
                    powerAround(row, column, false, triggerNextRound);
                }
                switch (item.getType()) {
                    case NOTE_BLOCK:
                    case PISTON:
                    case STICKY_PISTON:
                    case TNT:
                        meta.getPersistentDataContainer().set(redstoneTriggerKey, PersistentDataType.INTEGER, roundNumber + roundOffset); //Trigger key says when it should be triggered
                        break;
                    case DROPPER:
                        int newColumn = column + (column < 4 ? 1 : -1);
                        if (newColumn != -1 && newColumn != 4 && newColumn != 9) {
                            int newSlot = slot + (column < 4 ? 1 : -1);
                            if (chest.getSnapshotInventory().getItem(newSlot) == null) {
                                chest.getSnapshotInventory().setItem(newSlot, getDropperItem());
                            }
                        }
                        break;
                    case ACACIA_TRAPDOOR:
                    case BIRCH_TRAPDOOR:
                    case CRIMSON_TRAPDOOR:
                    case DARK_OAK_TRAPDOOR:
                    case JUNGLE_TRAPDOOR:
                    case OAK_TRAPDOOR:
                    case SPRUCE_TRAPDOOR:
                    case WARPED_TRAPDOOR:
                        ItemHandler.incrementATK(meta);
                        break;
                    case IRON_TRAPDOOR:
                        ItemHandler.incrementDEF(meta);
                        break;
                    case DISPENSER:
                        int dir = (column < 4 ? 1 : -1);
                        newColumn = column + dir;
                        while (newColumn >= 0 && newColumn < 9) {
                            ItemStack potentialTarget = chest.getSnapshotInventory().getItem(row * 9 + newColumn);
                            try {
                                ItemMeta attackedMeta = potentialTarget.getItemMeta();
                                if (meta.getPersistentDataContainer().has(ItemHandler.getTypeKey(), PersistentDataType.STRING)) {
                                    if (ItemHandler.dealDamageTo(attackedMeta, ItemHandler.getATK(meta))) {
                                        if (attackedMeta.getPersistentDataContainer().get(ItemHandler.getTypeKey(), PersistentDataType.STRING).equals("core")) {
                                            broadcast(
                                                    ChatColor.RED + getColorFromSide(getSideFromSlot(slot)) +
                                                            "'s dispenser killed " +
                                                            getColorFromSide(getSideFromSlot(row * 9 + newColumn)) +
                                                            "'s core!"
                                            );
                                            endGame(3 - getSideFromSlot(row * 9 + newColumn));
                                        } else { //A regular item was killed
                                            killItem(row * 9 + newColumn); //TODO Add notification
                                        }
                                    }
                                    break;
                                }
                            } catch (NullPointerException e) {
                            }
                            newColumn += dir;
                        }
                        break;
                }
                traits.setTraitsOf(meta);
                item.setItemMeta(meta);
                ItemHandler.setLore(item);
            }
        }catch (NullPointerException e){}
    }

    private void powerAround(int row, int col, boolean reset, boolean triggerNextRound){
        Chesticuffs.LOGGER.log("Powering around " + row + ", " + col, MessageLevel.DEBUG_INFO);
        int slot = row * 9 + col;
        if(col > 0) powerItem(slot - 1, reset, triggerNextRound);
        if(col < 8) powerItem(slot + 1, false, triggerNextRound);
        if(row > 0) powerItem(slot - 9, false, triggerNextRound);
        if(row < 2) powerItem(slot + 9, false, triggerNextRound);
    }

    private boolean isTriggered(ItemStack item){
        try{
            return item.getItemMeta().getPersistentDataContainer().get(redstoneTriggerKey, PersistentDataType.INTEGER) == roundNumber;
        }catch (NullPointerException e) {
            return false;
        }
    }

    public Queue<RedstoneAction> getRedstoneActions() {
        return redstoneActions;
    }

    private void beginRedstoneTurn(int side){
        redstoneActions = new LinkedList<>();

        int lowerBound = side == 1 ? 0 : 5;
        int upperBound = side == 1 ? 4 : 9;

        for(int row = 0; row < 3; row++){
            for(int column = lowerBound; column < upperBound; column++){
                int index = row * 9 + column;
                ItemStack item = chest.getSnapshotInventory().getItem(index);
                if(item == null) continue;
                switch(item.getType()){
                    case LEVER:
                        if(side == getPriority()){
                            powerAround(row, column, true, false);
                        }
                        break;
                }
            }
        }

        for(int row = 0; row < 3; row++){
            for(int column = lowerBound; column < upperBound; column++){
                int index = row * 9 + column;

                ItemStack item = chest.getSnapshotInventory().getItem(index);
                if(isTriggered(item)){
                    switch(item.getType()){
                        case NOTE_BLOCK:
                            redstoneActions.add(new NoteBlockAction(this, side, index));
                            break;
                        case TNT:
                            redstoneActions.add(new TNTAction(this, side, index));
                            break;
                        case STICKY_PISTON:
                            redstoneActions.add(new PistonAction(this, side, index)); //Lack of break statement is intentional
                        case PISTON:
                            redstoneActions.add(new PistonAction(this, side, index));
                            break;
                    }
                }
            }
        }

        nextRedstoneAction();
        chest.update();
        broadcastChanges();
    }

    private void nextRedstoneAction(){
        boolean cont = true;
        while(cont) {
            cont = false;
            if (redstoneActions.isEmpty()) {
                nextTurn();
            } else {
                if (!redstoneActions.peek().startAction()) {
                    //Action could not start
                    Chesticuffs.LOGGER.log("Redstone action could not start!", MessageLevel.DEBUG_INFO);
                    cont = true;
                }
                chest.update();
                broadcastChanges();
            }
        }
    }

    public void killItem(int slot){
        chest.getSnapshotInventory().setItem(slot, null);
    }

    public void handleClickEvent(InventoryClickEvent e){
        if(!(e.getWhoClicked() instanceof Player)){
            return;
        }

        if(e.getClickedInventory().equals(playerOneInventory) || e.getClickedInventory().equals(playerTwoInventory))
            if(e.getSlot() == 22) {
                Chesticuffs.LOGGER.log("Player requested game state dump", MessageLevel.INFO);
                printGameState();
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

        switch(phase){
            case CORE_PLACEMENT://Placing of cores
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
                            nextTurn();
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
            case REDSTONE:
                Chesticuffs.LOGGER.log("Player clicked something during redstone phase!", MessageLevel.DEBUG_INFO);
                if(e.getClickedInventory().equals(currentInv)){
                    Chesticuffs.LOGGER.log("Player clicked in current inventory", MessageLevel.DEBUG_INFO);
                    if(redstoneActions.peek().handleClick(e.getSlot())){
                        redstoneActions.peek().endAction();
                        Chesticuffs.LOGGER.log("Action Ended", MessageLevel.DEBUG_INFO);
                        redstoneActions.remove();

                        chest.update();
                        broadcastChanges();

                        nextRedstoneAction();
                    }
                }
                break;
            case OPENING_PHASE://opening phase
            case CLOSING_PHASE://closing phase
                if(e.getClickedInventory().equals(player.getInventory())){
                    Chesticuffs.LOGGER.log("Player has clicked in their inventory!", MessageLevel.DEBUG_INFO);
                    broadcastChanges(); //Clears all virtual green glass panes if there are any (Because the panes aren't in the actual chest)
                    selectedItem = null;
                    ItemStack item = e.getCurrentItem();
                    if(item == null || item.getType() == Material.AIR){
                        Chesticuffs.LOGGER.log("They clicked on an empty square", MessageLevel.DEBUG_INFO);
                        return;
                    }
                    ItemMeta meta = item.getItemMeta();
                    TraitsHolder itemTraits = new TraitsHolder(meta);
                    if(meta == null){
                        Chesticuffs.LOGGER.log("The item did not have metadata", MessageLevel.WARNING);
                        return;
                    }

                    if(meta.getPersistentDataContainer().equals(null)) {
                        Chesticuffs.LOGGER.log("There was no PDC", MessageLevel.WARNING);
                        return;
                    }

                    if(meta.getPersistentDataContainer().get(ItemHandler.getTypeKey(), PersistentDataType.STRING) == null) {
                        Chesticuffs.LOGGER.log("Could not check item's type!", MessageLevel.WARNING);
                        return;
                    }

                    String itemType = meta.getPersistentDataContainer().get(ItemHandler.getTypeKey(), PersistentDataType.STRING);

                    if(itemType.equalsIgnoreCase("core")){
                        Chesticuffs.LOGGER.log("Player clicked on a core", MessageLevel.DEBUG_INFO);
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
                        nextTurn(true);
                    }else if(selectedItem == null){
                        return;
                    }else if(pendingUsableSelection){
                        usableSelectedUsableItem(e.getSlot());
                        broadcastChanges();
                    }else if(item.equals(validationPane)){
                        placeItem(selectedItem, e.getSlot());
                        nextTurn();
                    }else{
                        selectedItem = null;
                        broadcastChanges();
                    }
                }
                break;
            case ATTACKER_SELECTION:

                //Check if they clicked on their side
                if(e.getClickedInventory().equals(currentInv)){
                    if(e.getSlot() % 9 == 4){
                        if(e.getSlot() == 13){
                            nextTurn(true);
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
                        else if(Trait.DRIED.isInMeta(meta)){
                            (turn == 1 ? playerOne : playerTwo).sendMessage(ChatColor.RED + "That item is dried!");
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
            case DEFENDER_SELECTION: //defense phase

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
                            nextTurn(true);
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
                            if(Trait.DRIED.isInMeta(defenderMeta)){
                                (turn == 1 ? playerOne : playerTwo).sendMessage(ChatColor.RED + "That item is dried!");
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

    public static String[] staff;

    static{
        staffUUIDs = new UUID[]{
                UUID.fromString("321b4833-b57e-4092-a309-fdf1ae9c069d"), //BigSalamanderMan
                UUID.fromString("9db91e76-ffa6-43f2-8a1c-b6539943114e"), //JJMahiro
                UUID.fromString("1b6f97c6-03ec-42da-8e57-1cab6a237abc"), //PaulTaranto
                UUID.fromString("f6720fc4-c294-4ffb-b2d3-ebc3d2d36539"), //Sninja
                UUID.fromString("744c3b2a-5c18-4807-be4d-ccdd672b7391"), //Skeleturge
                UUID.fromString("af817b9a-b520-48e0-9337-29053bafaceb"), //Taniwha_
                UUID.fromString("e7186f7a-0a94-4d8b-837d-6ea57b207da5"), //goldenskaz
                UUID.fromString("45148565-47de-47d8-9fdc-751d843de238"), //illusionWark
                UUID.fromString("736534e6-0c1e-4211-9068-9ca0f6f86d95"), //TNT_Man3 (Jdrocksu)
                UUID.fromString("83651b2e-b133-47ba-bf05-6700e5d059a3"), //ScootBoot534
                UUID.fromString("a7673d79-d577-41bd-a1ee-e7aad0baec16"), //Ted_Nivision (Seefop)
                UUID.fromString("ee4127ea-9d9a-406c-8227-5d39fbf3ce33"), //SuperMiner
        };

        staff = new String[]{
                "BigSalamanderMan",
                "JJMahiro",
                "PaulTaranto",
                "Sninja",
                "Skeleturge",
                "Taniwha_",
                "goldenskaz",
                "illusionwWark",
                "TNT_Man3",
                "ScootBoot534",
                "Ted_Nivision",
                "SuperMiner8055"
        };

        for(int i = 0; i < staffUUIDs.length; i++){
            System.out.println(staffUUIDs[i]);
            System.out.println(staffUUIDs[i].getMostSignificantBits());
        }
    }
}
