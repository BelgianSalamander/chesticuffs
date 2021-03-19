package salamander.chesticuffs.game;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Chest;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.scheduler.BukkitTask;
import salamander.chesticuffs.Chesticuffs;
import salamander.chesticuffs.commands.RegisterInventory;
import salamander.chesticuffs.inventory.ChestKeys;
import salamander.chesticuffs.inventory.ItemHandler;

import java.awt.*;
import java.util.*;
import java.util.List;

public class ChesticuffsGame {
    static public NamespacedKey playerIdKey = new NamespacedKey(Chesticuffs.getPlugin(), "gameId");
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

    public Inventory getPlayerOneInventory() {
        return playerOneInventory;
    }

    public Inventory getPlayerTwoInventory() {
        return playerTwoInventory;
    }

    Inventory playerOneInventory, playerTwoInventory;

    public ChesticuffsGame(Player player, Chest chest, String id){
        playerOne = player;
        this.chest = chest;
        this.id = id;

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
        return chest.getBlockInventory().getItem(4 + turn * 6);
    }

    private void buffItem(ItemStack item, int x){
        ItemMeta meta = item.getItemMeta();
        short ATK = meta.getPersistentDataContainer().get(ItemHandler.getDamageKey(), PersistentDataType.SHORT);
        short DEF = meta.getPersistentDataContainer().get(ItemHandler.getDefenceKey(), PersistentDataType.SHORT);
        short HP = meta.getPersistentDataContainer().get(ItemHandler.getHealthKey(), PersistentDataType.SHORT);
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
        System.out.println(getCore(side).getItemMeta().getPersistentDataContainer().get(ItemHandler.getEffectIDKey(), PersistentDataType.INTEGER));
        switch(getCore(side).getItemMeta().getPersistentDataContainer().get(ItemHandler.getEffectIDKey(), PersistentDataType.INTEGER)){
            case(1):
                System.out.println("Boosted by iron block!");
                HP += 1;
                break;
        }
        meta.getPersistentDataContainer().set(ItemHandler.getDamageKey(), PersistentDataType.SHORT, ATK);
        meta.getPersistentDataContainer().set(ItemHandler.getDefenceKey(), PersistentDataType.SHORT, DEF);
        meta.getPersistentDataContainer().set(ItemHandler.getHealthKey(), PersistentDataType.SHORT, HP);
        item.setItemMeta(meta);
        ItemHandler.setLore(item);
    }

    private void combat(){
        ItemStack defender, attacker;
        ItemMeta defendingItemMeta, attackingItemMeta;
        short defenderHP, attackerHP;

        ItemStack defendingCore =  getCore(3 - turn);
        ItemMeta defendingCoreMeta = defendingCore.getItemMeta();
        PersistentDataContainer defendingCoreData = defendingCoreMeta.getPersistentDataContainer();
        short coreHealth = defendingCoreData.get(ItemHandler.getHealthKey(), PersistentDataType.SHORT);
        for(Map.Entry<Integer, Integer> entry: attackersAndDefenders.entrySet()){
            if(entry.getValue() == null){
                coreHealth -= chest.getBlockInventory().getItem(entry.getKey()).getItemMeta().getPersistentDataContainer().get(ItemHandler.getDamageKey(), PersistentDataType.SHORT);
                ItemHandler.setLore(chest.getBlockInventory().getItem(entry.getKey()));
            }else{
                attacker = chest.getBlockInventory().getItem(entry.getKey());
                defender = chest.getBlockInventory().getItem(entry.getValue());
                attackingItemMeta = attacker.getItemMeta();
                defendingItemMeta = defender.getItemMeta();
                attackerHP = attackingItemMeta.getPersistentDataContainer().get(ItemHandler.getHealthKey(), PersistentDataType.SHORT);
                defenderHP = defendingItemMeta.getPersistentDataContainer().get(ItemHandler.getHealthKey(), PersistentDataType.SHORT);

                defenderHP -= Math.max((attackingItemMeta.getPersistentDataContainer().get(ItemHandler.getDamageKey(), PersistentDataType.SHORT) - defendingItemMeta.getPersistentDataContainer().get(ItemHandler.getDamageKey(), PersistentDataType.SHORT)), 0);
                attackerHP -=Math.max((defendingItemMeta.getPersistentDataContainer().get(ItemHandler.getDamageKey(), PersistentDataType.SHORT) - attackingItemMeta.getPersistentDataContainer().get(ItemHandler.getDamageKey(), PersistentDataType.SHORT)), 0);

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
        chest.update();
        defendingCoreData.set(ItemHandler.getHealthKey(), PersistentDataType.SHORT, coreHealth);
        defendingCore.setItemMeta(defendingCoreMeta);
        ItemHandler.setLore(defendingCore);
        broadcastChanges();
        if(coreHealth <= 0){
            //De-registers Game
            Chesticuffs.getGames().remove(id);
            chest.getPersistentDataContainer().remove(ChestKeys.idKey);
            playerOne.getPersistentDataContainer().remove(playerIdKey);
            playerTwo.getPersistentDataContainer().remove(playerIdKey);

            Bukkit.getServer().getScheduler().runTask(Chesticuffs.getPlugin(), new InventoryClose(playerOneInventory));
            Bukkit.getServer().getScheduler().runTask(Chesticuffs.getPlugin(), new InventoryClose(playerTwoInventory));

            if(getPriority() == 1){
                playerOne.sendMessage(ChatColor.RED + "Red wins the game!");
                playerTwo.sendMessage(ChatColor.RED + "Red wins the game!");
            }else{
                playerOne.sendMessage(ChatColor.BLUE + "Blue wins the game!");
                playerTwo.sendMessage(ChatColor.BLUE + "Blue wins the game!");
            }
        }
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
        System.out.println(phaseNumber);
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
                if(e.getClickedInventory().equals(player.getInventory())){
                    broadcastChanges(); //Clears all virtual green glass panes if there are any (Because the panes aren't in the actual chest)
                    selectedItem = null;
                    ItemStack item = e.getCurrentItem();
                    System.out.println("Player clicked in own inventory!");
                    if(item == null || item.getType() == Material.AIR){
                        System.out.println("Clicked on no item");
                        return;
                    }
                    ItemMeta meta = item.getItemMeta();
                    if(meta == null){
                        System.out.println("Item has no meta");
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
                    if(turn == 1){
                        if(playerOneAmountPlacedThisRound >= 3){
                            player.sendMessage(ChatColor.RED + "You have already placed three items this round");
                            return;
                        }
                        Short amountPlaced = playerOneItemsPlaced.get(item.getType());
                        if(amountPlaced != null){
                            if(amountPlaced >= 3){
                                player.sendMessage(ChatColor.RED + "You have already placed this item three times!");
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
                            if(amountPlaced >= 3){
                                player.sendMessage(ChatColor.RED + "You have already placed this item three times!");
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

                    if(e.getSlot() == 13){
                        if(turn == 1){
                            playerOneSkipped = true;
                            amountSkipsPlayerOne += 1;
                            playerOne.sendMessage(ChatColor.GREEN + "Red has skipped!");
                            playerTwo.sendMessage(ChatColor.GREEN + "Red has skipped!");
                        }else{
                            playerTwoSkipped = true;
                            amountSkipsPlayerTwo += 1;
                            playerOne.sendMessage(ChatColor.GREEN + "Blue has skipped!");
                            playerTwo.sendMessage(ChatColor.GREEN + "Blue has skipped!");
                        }
                        selectedItem = null;
                        System.out.println("Which players have just skipped");
                        System.out.println(playerOneSkipped);
                        System.out.println(playerTwoSkipped);
                        if(playerOneSkipped && playerTwoSkipped){
                            phaseNumber = 2;
                            selectedItem = null;
                            turn = getPriority();
                            attackersAndDefenders.clear();
                            attackersSelected = 0;
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
                        buffItem(itemToBePlaced, e.getSlot() % 9);
                        selectedItem.setAmount(selectedItem.getAmount() - 1);
                        chest.getSnapshotInventory().setItem(e.getSlot(), itemToBePlaced);
                        chest.update();
                        if (turn == 1){
                            playerOneSkipped = false;
                            playerOneAmountPlacedThisRound += 1;
                            if(playerOneItemsPlaced.get(itemToBePlaced.getType()) == null){
                                playerOneItemsPlaced.put(itemToBePlaced.getType(), (short) 1);
                            }else{
                                playerOneItemsPlaced.put(itemToBePlaced.getType(), (short) (playerOneItemsPlaced.get(itemToBePlaced.getType()) + 1));
                            }
                        }else{
                            playerTwoSkipped = false;
                            playerTwoAmountPlacedThisRound += 1;
                            if(playerTwoItemsPlaced.get(itemToBePlaced.getType()) == null){
                                playerTwoItemsPlaced.put(itemToBePlaced.getType(), (short) 1);
                            }else{
                                playerTwoItemsPlaced.put(itemToBePlaced.getType(), (short) (playerTwoItemsPlaced.get(itemToBePlaced.getType()) + 1));
                            }
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
                        if(attackersSelected >= 6) {
                            player.sendMessage(Component.text(ChatColor.RED + "You have already declared six attackers!"));
                            return;
                        }
                        ItemStack itemInChest = chest.getBlockInventory().getItem(e.getSlot());
                        ItemMeta meta =  itemInChest.getItemMeta();
                        List<Component> lore = meta.lore();
                        if(!attackersAndDefenders.containsKey(e.getSlot())){
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
                        selectedSlot = null;
                        System.out.println("Checking if item already defending!");
                        if(!attackersAndDefenders.containsValue(e.getSlot())){
                            selectedSlot = e.getSlot();
                            ItemStack defender =  chest.getBlockInventory().getItem(selectedSlot);
                            ItemMeta defenderMeta = defender.getItemMeta();
                            List<Component> lore = defenderMeta.lore();
                            lore.set(1,Component.text(ChatColor.BLUE + "Defending"));
                            defenderMeta.lore(lore);
                            defender.setItemMeta(defenderMeta);
                            broadcastChanges();
                            return;
                        }else{
                            player.sendMessage(ChatColor.RED + "That item is already defending!");
                        }
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
