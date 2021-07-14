package salamander.chesticuffs.game.redstone;

import org.bukkit.NamespacedKey;
import salamander.chesticuffs.Chesticuffs;
import salamander.chesticuffs.game.ChesticuffsGame;

public abstract class RedstoneAction {
    protected final ChesticuffsGame game;
    protected final static NamespacedKey redKey;
    protected int slot;

    RedstoneAction(ChesticuffsGame game){this.game = game;}

    abstract public boolean startAction(); //Returns true if action started successfully. Otherwise, it should be discarded
    abstract public boolean handleClick(int slot); //Returns true if this action is completed
    abstract public void endAction(); //Called when handleClick() returned true

    static {
        redKey = new NamespacedKey(Chesticuffs.getPlugin(), "red");
    }
}
