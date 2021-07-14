package salamander.chesticuffs.game.redstone;

import net.kyori.adventure.text.Component;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import salamander.chesticuffs.game.ChesticuffsGame;
import salamander.chesticuffs.inventory.ItemHandler;
import salamander.chesticuffs.traits.Trait;
import salamander.chesticuffs.traits.TraitsHolder;

public class TNTAction extends RedstoneAction{
    private final Player player;
    private final Player opposition;
    private final int side;

    public TNTAction(ChesticuffsGame game, int side, int slot) {
        super(game);
        this.slot = slot;
        this.side = side;

        player = side == 1 ? game.getPlayerOne() : game.getPlayerTwo();
        opposition = side != 1 ? game.getPlayerOne() : game.getPlayerTwo();
    }

    @Override
    public boolean startAction() {
        ItemStack item = game.getChest().getSnapshotInventory().getItem(slot);
        if(item == null) return false;
        if (!game.getChest().getSnapshotInventory().getItem(slot).getType().equals(Material.TNT)) return false;

        ItemMeta meta = item.getItemMeta();
        meta.addEnchant(Enchantment.LUCK, 0, true);
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        item.setItemMeta(meta);

        player.sendMessage(Component.text(ChatColor.DARK_PURPLE + "[TNT] Click any item to deal 5 true damage to it! Click anything else to skip this action"));
        game.getChest().update();
        game.broadcastChanges();

        return true; //Started Successfully
    }

    @Override
    public boolean handleClick(int slot) {
        ItemStack item = game.getChest().getSnapshotInventory().getItem(slot);
        int result = game.dealDamageTo(slot, (short) 5, true);

        int side = (slot % 9) < 4 ? 1 : 2;
        Player playerAttacked = side == 1 ? player : opposition;

        if(result == -1){
            player.sendMessage(Component.text(ChatColor.DARK_PURPLE + "[TNT] Action Skipped"));
        }else {
            game.killItem(this.slot);
            if(result == 1){
                playerAttacked.sendMessage(ChatColor.DARK_RED + "TNT harmed your " + item.getType() + " (Slot " + slot + "). It took 5 true damage");
            }else{
                playerAttacked.sendMessage(ChatColor.DARK_RED + "TNT killed your " + item.getType());
            }
        }
        return true;
    }

    @Override
    public void endAction() {
        ItemStack item = game.getChest().getSnapshotInventory().getItem(slot);
        if(item == null) return;

        ItemMeta meta = item.getItemMeta();
        meta.removeEnchant(Enchantment.LUCK);
        item.setItemMeta(meta);
    }
}
