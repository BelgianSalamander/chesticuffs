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
import org.bukkit.persistence.PersistentDataType;
import salamander.chesticuffs.Chesticuffs;
import salamander.chesticuffs.commands.RegisterInventory;
import salamander.chesticuffs.inventory.ChestKeys;
import salamander.chesticuffs.inventory.ItemHandler;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ChesticuffsGame {
    static public NamespacedKey playerIdKey = new NamespacedKey(Chesticuffs.getPlugin(), "gameId");
    private Player playerOne, playerTwo;
    private Chest chest;
    int roundNumber, phaseNumber, turn = 0;
    int amountSkipsPlayerOne, amountSkipsPlayerTwo, playerOneAmountPlacedThisRound, playerTwoAmountPlacedThisRound;
    boolean playerOneSkipped, playerTwoSkipped;
    Map<Material, Short> playerOneItemsPlaced = new HashMap<>();
    Map<Material, Short> playerTwoItemsPlaced = new HashMap<>();
    ItemStack selectedItem;
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
            if(trait == "Gravity"){
                for(int x = 5 * turn - 5; x < 5 * turn - 1; x++){
                    for(int y = 2; y >= 0; y--){
                        if(chest.getBlockInventory().getItem(y * 9 + x).getType().equals(Material.AIR)){
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
                if(chest.getBlockInventory().getItem(y * 9 + x).getType().equals(Material.AIR)) {
                    validSpaces.add(y * 9 + x);
                }
            }
        }
        return validSpaces;
    }

    private boolean isPlayingSpaceFull(int side){
        for(int y = 0; y < 3; y++){
            for(int x = 5 * turn - 5; x < 5 * turn - 1; x++){
                if(chest.getBlockInventory().getItem(y * 9 + x).getType().equals(Material.AIR)) {
                    return false;
                }
            }
        }
        return true;
    }

    public void handleClickEvent(InventoryClickEvent e){
        if(!(e.getWhoClicked() instanceof Player)){
            return;
        }
        Player player = (Player) e.getWhoClicked();
        if(turn == 1 && (! player.equals(playerOne))){
            player.sendMessage(ChatColor.RED + "It is not your turn!");
            return;
        }

        if(turn == 2 && (! player.equals(playerTwo))){
            player.sendMessage(ChatColor.RED + "It is not your turn!");
            return;
        }

        switch(phaseNumber){
            case(0):
                e.setCancelled(true);
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
                            chest.getSnapshotInventory().setItem(6 + turn * 4, core);
                            item.setAmount(item.getAmount()-1);
                            chest.update();
                            broadcastChanges();
                            //Next Turn
                            if(turn == getPriority()){
                                turn = 3 - turn;
                            }else{
                                turn = getPriority();
                                phaseNumber = 1;
                            }
                        }else{
                            player.sendMessage(ChatColor.RED + "Please Select A Core");
                        }
                    }else{
                        player.sendMessage(ChatColor.RED + "Please Select A Core");
                    }

                }else if(e.getCurrentItem()!= null){
                    if(e.getCurrentItem().getItemMeta() != null) {
                        if (e.getCurrentItem().getItemMeta().displayName().equals(Component.text(ChatColor.GRAY + "Skip Turn"))) {
                            player.sendMessage(ChatColor.RED + "You cannot skip this phase!");
                        }
                    }
                }
                break;
            case(1):
                Inventory currentInv;
                if(turn == 1){
                    currentInv = playerOneInventory;
                }else{
                    currentInv = playerTwoInventory;
                }
                if(e.getClickedInventory().equals(player.getInventory())){
                    broadcastChanges(); //Clears all virtual green glass panes if there are any
                    ItemStack item = e.getCurrentItem();
                    if(item == null || item.getType() == Material.AIR){
                        return;
                    }
                    ItemMeta meta = item.getItemMeta();
                    if(meta == null){
                        return;
                    }
                    if(isPlayingSpaceFull(turn)){
                        player.sendMessage("Your playing space is full!");
                    }
                }
                break;
            case(2):
                break;


        }
    }
}
