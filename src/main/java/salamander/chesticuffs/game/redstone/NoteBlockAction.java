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

public class NoteBlockAction extends RedstoneAction{
    private final Player player;
    private final Player opposition;

    public NoteBlockAction(ChesticuffsGame game, int side, int slot){
        super(game);
        this.slot = slot;

        player = side == 1 ? game.getPlayerOne() : game.getPlayerTwo();
        opposition = side != 1 ? game.getPlayerOne() : game.getPlayerTwo();
    }

    @Override
    public boolean startAction() {
        ItemStack item = game.getChest().getSnapshotInventory().getItem(slot);
        if(item == null) return false;
        if (!game.getChest().getSnapshotInventory().getItem(slot).getType().equals(Material.NOTE_BLOCK)) return false;

        ItemMeta meta = item.getItemMeta();
        meta.addEnchant(Enchantment.LUCK, 0, true);
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        item.setItemMeta(meta);

        player.sendMessage(Component.text(ChatColor.DARK_PURPLE + "[Note Block] Click any item to stun it. Click anything else to skip!"));
        return true; //Started Successfully
    }

    @Override
    public boolean handleClick(int slot) {
        ItemStack item = game.getChest().getSnapshotInventory().getItem(slot);
        try{
            ItemMeta meta = item.getItemMeta();
            if(meta.getPersistentDataContainer().get(ItemHandler.getTypeKey(), PersistentDataType.STRING).equals("item")){
                TraitsHolder traits = new TraitsHolder(meta);
                traits.addTrait(Trait.STUNNED);
                player.sendMessage(Component.text(ChatColor.DARK_PURPLE + "[Note Block] Stunned Item!"));
                opposition.sendMessage(Component.text(ChatColor.DARK_PURPLE + "[Note Block] Your " + item.getType() + " (Slot " + slot + ") has been stunned!"));
                traits.setTraitsOf(meta);
                item.setItemMeta(meta);
                ItemHandler.setLore(item);

                game.getChest().update();
                game.broadcastChanges();

                return true;
            }
        }catch (NullPointerException e){ }
        player.sendMessage(Component.text(ChatColor.DARK_PURPLE + "[Note Block] Action Skipped"));
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
