package salamander.chesticuffs.playerData;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.Nullable;
import salamander.chesticuffs.LeagueTuple;

import java.io.Serializable;
import java.text.Format;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

public class PlayerData implements Serializable {
    private int eloRating, winCount, lossCount, streak, gamesPlayed;
    private long joinedAt, lastWonAt, lastOnlineAt;

    static Format format = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");

    public void setGamesPlayed(int gamesPlayed) {
        this.gamesPlayed = gamesPlayed;
    }

    public int getGamesPlayed() {
        return gamesPlayed;
    }

    public int getEloRating() {
        return eloRating;
    }

    public int getWinCount() {
        return winCount;
    }

    public int getLossCount() {
        return lossCount;
    }

    public int getStreak() {
        return streak;
    }

    public long getJoinedAt() {
        return joinedAt;
    }

    public long getLastWonAt() {
        return lastWonAt;
    }

    public void setEloRating(int eloRating) {
        this.eloRating = eloRating;
    }

    public void setWinCount(int winCount) {
        this.winCount = winCount;
    }

    public void setLossCount(int lossCount) {
        this.lossCount = lossCount;
    }

    public void setStreak(int streak) {
        this.streak = streak;
    }

    public void setJoinedAt(long joinedAt) {
        this.joinedAt = joinedAt;
    }

    public void setLastWonAt(long lastWonAt) {
        this.lastWonAt = lastWonAt;
    }

    public PlayerData(int eloRating, int winCount, int lossCount, int streak, long joinedAt, long lastWonAt, long lastOnlineAt){
        this.eloRating = eloRating;
        this.winCount = winCount;
        this.lossCount = lossCount;
        this.streak = streak;
        this.joinedAt = joinedAt;
        this.lastWonAt = lastWonAt;
        this.lastOnlineAt = lastOnlineAt;
    }

    public long getLastOnlineAt() {
        return lastOnlineAt;
    }

    public void setLastOnlineAt(long lastOnlineAt) {
        this.lastOnlineAt = lastOnlineAt;
    }

    public PlayerData(){
        this.eloRating = 1000;
        this.winCount = 0;
        this.lossCount = 0;
        this.streak = 0;
        this.joinedAt = System.currentTimeMillis();
        this.lastWonAt = 0;
        this.lastOnlineAt = System.currentTimeMillis();
    }

    public void displayStatsTo(Player player){
        Inventory inventory = Bukkit.createInventory(null, 9, Component.text(ChatColor.GREEN + "Stats"));
        ItemStack eloItem, winsItem, lossItem, streakItem, gamesPlayedItem, joinedAtItem, lastWonAtItem;
        ItemMeta eloMeta, winsMeta, lossMeta, streakMeta, gamesPlayedMeta, joinedAtMeta, lastWonAtMeta;

        eloItem = new ItemStack(Material.LADDER);
        winsItem = new ItemStack(Material.DIAMOND);
        lossItem = new ItemStack(Material.COAL);
        streakItem = new ItemStack(Material.CHEST_MINECART);
        gamesPlayedItem = new ItemStack(Material.CHEST);
        joinedAtItem = new ItemStack(Material.WHEAT_SEEDS);
        lastWonAtItem = new ItemStack(Material.CLOCK);

        eloMeta = eloItem.getItemMeta();
        winsMeta = winsItem.getItemMeta();
        lossMeta = lossItem.getItemMeta();
        streakMeta = streakItem.getItemMeta();
        gamesPlayedMeta = gamesPlayedItem.getItemMeta();
        joinedAtMeta = joinedAtItem.getItemMeta();
        lastWonAtMeta = lastWonAtItem.getItemMeta();

        eloMeta.displayName(Component.text(ChatColor.GREEN + "Elo Rating"));
        winsMeta.displayName(Component.text(ChatColor.GREEN + "Win Count"));
        lossMeta.displayName(Component.text(ChatColor.RED + "Loss Count"));
        streakMeta.displayName(Component.text(ChatColor.GREEN + "Win Streak"));
        gamesPlayedMeta.displayName(Component.text(ChatColor.BLUE + "Games Played"));
        joinedAtMeta.displayName(Component.text(ChatColor.BLUE + "Joined On"));
        lastWonAtMeta.displayName(Component.text(ChatColor.GREEN + "Time of Last Win"));

        List<Component> eloLore = new LinkedList<>();
        eloLore.add(Component.text(ChatColor.DARK_GREEN + String.valueOf(eloRating)));
        eloMeta.lore(eloLore);

        List<Component> winsLore = new LinkedList<>();
        winsLore.add(Component.text(ChatColor.DARK_GREEN + String.valueOf(winCount)));
        winsMeta.lore(winsLore);

        List<Component> lossLore = new LinkedList<>();
        lossLore.add(Component.text(ChatColor.RED + String.valueOf(lossCount)));
        lossMeta.lore(lossLore);

        List<Component> streakLore = new LinkedList<>();
        streakLore.add(Component.text(ChatColor.DARK_GREEN + String.valueOf(streak)));
        streakMeta.lore(streakLore);

        List<Component> gamesPlayedLore = new LinkedList<>();
        gamesPlayedLore.add(Component.text(ChatColor.DARK_GREEN + String.valueOf(gamesPlayed)));
        gamesPlayedMeta.lore(gamesPlayedLore);


        Date joinDate = new Date(joinedAt);
        List<Component> joinedAtLore = new LinkedList<>();
        joinedAtLore.add(Component.text(ChatColor.DARK_BLUE + format.format(joinDate) + " (GMT-4)"));
        joinedAtMeta.lore(joinedAtLore);

        List<Component> lastWonLore = new LinkedList<>();
        if(lastWonAt == 0){
            lastWonLore.add(Component.text(ChatColor.RED + "You have not won a game yet!"));
        }else {
            Date winDate = new Date(lastWonAt);
            lastWonLore.add(Component.text(format.format(winDate) + " (GMT-4)"));
        }
        lastWonAtMeta.lore(lastWonLore);

        eloItem.setItemMeta(eloMeta);
        winsItem.setItemMeta(winsMeta);
        lossItem.setItemMeta(lossMeta);
        streakItem.setItemMeta(streakMeta);
        gamesPlayedItem.setItemMeta(gamesPlayedMeta);
        joinedAtItem.setItemMeta(joinedAtMeta);
        lastWonAtItem.setItemMeta(lastWonAtMeta);

        ItemStack leagueItem = Leagues.leagueMap.get(getLeague());

        inventory.setItem(0, leagueItem);
        inventory.setItem(1, eloItem);
        inventory.setItem(2, winsItem);
        inventory.setItem(3, lossItem);
        inventory.setItem(4, streakItem);
        inventory.setItem(5, gamesPlayedItem);
        inventory.setItem(6, joinedAtItem);
        inventory.setItem(7, lastWonAtItem);
        inventory.setItem(8, leagueItem);

        player.openInventory(inventory);
    }

    public @Nullable String getLeague(){
        for(LeagueTuple leagueTuple : DataLoader.getLeagues()){
            if(leagueTuple.threshold <= eloRating){
                return leagueTuple.name;
            }
        }
        return null;
    }
}
