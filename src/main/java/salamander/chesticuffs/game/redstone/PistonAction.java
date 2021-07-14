package salamander.chesticuffs.game.redstone;

import net.kyori.adventure.text.Component;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import salamander.chesticuffs.Chesticuffs;
import salamander.chesticuffs.MessageLevel;
import salamander.chesticuffs.game.ChesticuffsGame;
import salamander.chesticuffs.inventory.ItemHandler;

import java.util.ArrayList;
import java.util.List;

public class PistonAction extends RedstoneAction{
    private final Player player;
    private final Player opposition;
    private final Inventory playerInv;
    private final int side;

    private int fromSlot;

    boolean hasSelectedItem = false;

    static ItemStack movementPane = new ItemStack(Material.PURPLE_STAINED_GLASS_PANE, 1);

    public PistonAction(ChesticuffsGame game, int side, int slot){
        super(game);
        this.slot = slot;
        this.side = side;

        player = side == 1 ? game.getPlayerOne() : game.getPlayerTwo();
        opposition = side != 1 ? game.getPlayerOne() : game.getPlayerTwo();

        playerInv = side == 1 ? game.getPlayerOneInventory() : game.getPlayerTwoInventory();
    }

    @Override
    public boolean startAction() {
        ItemStack item = game.getChest().getSnapshotInventory().getItem(slot);
        if(item == null) return false;
        Material mat = item.getType();
        if (!(mat.equals(Material.PISTON) || mat.equals(Material.STICKY_PISTON))) return false;

        ItemMeta meta = item.getItemMeta();
        meta.addEnchant(Enchantment.LUCK, 0, true);
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        item.setItemMeta(meta);

        player.sendMessage(Component.text(ChatColor.DARK_PURPLE + "[PISTON] Click any item to move it one square. Click on anything else to skip!"));
        return true; //Started Successfully
    }

    @Override
    public boolean handleClick(int slot) {
        if(hasSelectedItem){
            if(isMovementPane(playerInv.getItem(slot))){
                Chesticuffs.LOGGER.log("Moving items with piston", MessageLevel.DEBUG_INFO);
                moveItem(fromSlot, slot);
            }
            return true;
        }else{
            if(isItemMovable(game.getChest().getSnapshotInventory().getItem(slot))){
                List<Integer> validPositions = getValidMoveLocations(slot);

                if(validPositions.size() == 0){
                    player.sendMessage(Component.text(ChatColor.DARK_PURPLE + "[PISTON] That item is unable to move!"));
                }else{
                    for(int i : validPositions){
                        playerInv.setItem(i, movementPane);
                    }
                    fromSlot = slot;
                    hasSelectedItem = true;
                }
                return false;
            }else{
                player.sendMessage(Component.text(ChatColor.DARK_PURPLE + "[PISTON] Action Skipped!"));
                return true;
            }
        }
    }

    @Override
    public void endAction() {
        ItemStack item = game.getChest().getSnapshotInventory().getItem(slot);
        if(item == null) return;

        ItemMeta meta = item.getItemMeta();
        meta.removeEnchant(Enchantment.LUCK);
        item.setItemMeta(meta);
    }

    private void moveItem(int beginSlot, int endSlot){
        game.getChest().getSnapshotInventory().setItem(endSlot, game.getChest().getSnapshotInventory().getItem(beginSlot));
        game.getChest().getSnapshotInventory().setItem(beginSlot, null);

        for(RedstoneAction action : game.getRedstoneActions()){ //Change all redstone actions that are affected
            if(action != this){
                if(action.slot == beginSlot) action.slot = endSlot;
            }
        }

        game.getChest().update();
        game.broadcastChanges();
    }

    private boolean isValidMoveLocation(int slot){
        return game.getChest().getSnapshotInventory().getItem(slot) == null;
    }

    private static boolean isItemMovable(ItemStack item){
        try{
            return item.getItemMeta().getPersistentDataContainer().get(ItemHandler.getTypeKey(), PersistentDataType.STRING).equals("item");
        }catch (NullPointerException e){return false;}
    }

    private List<Integer> getValidMoveLocations(int slot){
        List<Integer> validLocations = new ArrayList<>();

        int row = slot / 9;
        int column = slot % 9;

        if(row > 0) if(isValidMoveLocation(slot - 9)) validLocations.add(slot - 9);
        if(row < 2) if(isValidMoveLocation(slot + 9)) validLocations.add(slot + 9);
        if(column > 0) if(isValidMoveLocation(slot - 1)) validLocations.add(slot - 1);
        if(column < 8) if(isValidMoveLocation(slot + 1))validLocations.add(slot + 1);

        return validLocations;
    }

    private static boolean isMovementPane(ItemStack item){
        if(item == null) return false;
        return item.equals(movementPane);
    }

    static{
        ItemMeta meta = movementPane.getItemMeta();
        meta.displayName(Component.text(ChatColor.DARK_PURPLE + "Move Here"));
    }
}
