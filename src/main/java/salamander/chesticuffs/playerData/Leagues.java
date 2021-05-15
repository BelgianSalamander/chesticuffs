package salamander.chesticuffs.playerData;

import net.kyori.adventure.text.Component;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class Leagues {
    static public ItemStack oxeyeLeague, netheriteLeague, diamondLeague, emeraldLeague, redstoneLeague, lapisLeague, goldLeague, ironLeague, bronzeLeague, coalLeague, woodLeague;
    static public Map<String, ItemStack> leagueMap = new HashMap<>();

    static public void init(int i){
        leagueMap.clear();
        int netheriteAmount = Math.min(i/50, 10);
        int diamondAmount = Math.min(i/10, 50);

        oxeyeLeague = new ItemStack(Material.SUNFLOWER);
        ItemMeta oxeyeMeta = oxeyeLeague.getItemMeta();
        oxeyeMeta.displayName(Component.text(ChatColor.GOLD + "The Oxeye Daisy"));
        List<Component> oxeyeLore = new LinkedList<>();
        oxeyeLore.add(Component.text(ChatColor.GRAY + "Highest Elo Rating"));
        oxeyeMeta.lore(oxeyeLore);
        oxeyeMeta.addEnchant(Enchantment.LUCK, 0, true);
        oxeyeMeta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        oxeyeLeague.setItemMeta(oxeyeMeta);

        netheriteLeague = new ItemStack(Material.SUNFLOWER);
        ItemMeta netheriteMeta = netheriteLeague.getItemMeta();
        netheriteMeta.displayName(Component.text(ChatColor.DARK_PURPLE + "Netherite League"));
        List<Component> netheriteLore = new LinkedList<>();
        netheriteLore.add(Component.text(ChatColor.GRAY + "Top " + netheriteAmount));
        netheriteMeta.lore(netheriteLore);
        netheriteLeague.setItemMeta(netheriteMeta);

        diamondLeague = new ItemStack(Material.SUNFLOWER);
        ItemMeta diamondMeta = diamondLeague.getItemMeta();
        diamondMeta.displayName(Component.text(ChatColor.BLUE + "Diamond League"));
        List<Component> diamondLore = new LinkedList<>();
        diamondLore.add(Component.text(ChatColor.GRAY + "Top " + diamondAmount));
        diamondMeta.lore(diamondLore);
        diamondLeague.setItemMeta(diamondMeta);

        emeraldLeague = new ItemStack(Material.SUNFLOWER);
        ItemMeta emeraldMeta = emeraldLeague.getItemMeta();
        emeraldMeta.displayName(Component.text(ChatColor.GREEN + "Emerald League"));
        List<Component> emeraldLore = new LinkedList<>();
        emeraldLore.add(Component.text(ChatColor.GRAY + "Top 10%"));
        emeraldMeta.lore(emeraldLore);
        emeraldLeague.setItemMeta(emeraldMeta);

        redstoneLeague = new ItemStack(Material.SUNFLOWER);
        ItemMeta redstoneMeta = redstoneLeague.getItemMeta();
        redstoneMeta.displayName(Component.text(ChatColor.RED + "Redstone League"));
        List<Component> redstoneLore = new LinkedList<>();
        redstoneLore.add(Component.text(ChatColor.GRAY + "Top 20%"));
        redstoneMeta.lore(redstoneLore);
        redstoneLeague.setItemMeta(redstoneMeta);

        lapisLeague = new ItemStack(Material.SUNFLOWER);
        ItemMeta lapisMeta = lapisLeague.getItemMeta();
        lapisMeta.displayName(Component.text(ChatColor.DARK_BLUE + "Lapis League"));
        List<Component> lapisLore = new LinkedList<>();
        lapisLore.add(Component.text(ChatColor.GRAY + "Top 30%"));
        lapisMeta.lore(lapisLore);
        lapisLeague.setItemMeta(lapisMeta);

        goldLeague = new ItemStack(Material.SUNFLOWER);
        ItemMeta goldMeta = goldLeague.getItemMeta();
        goldMeta.displayName(Component.text(ChatColor.YELLOW + "Gold League"));
        List<Component> goldLore = new LinkedList<>();
        goldLore.add(Component.text(ChatColor.GRAY + "Top 40%"));
        goldMeta.lore(goldLore);
        goldLeague.setItemMeta(goldMeta);

        ironLeague = new ItemStack(Material.SUNFLOWER);
        ItemMeta ironMeta = ironLeague.getItemMeta();
        ironMeta.displayName(Component.text(ChatColor.GRAY + "Iron League"));
        List<Component> ironLore = new LinkedList<>();
        ironLore.add(Component.text(ChatColor.GRAY + "Top 50%"));
        ironMeta.lore(ironLore);
        ironLeague.setItemMeta(ironMeta);

        bronzeLeague = new ItemStack(Material.SUNFLOWER);
        ItemMeta bronzeMeta = bronzeLeague.getItemMeta();
        bronzeMeta.displayName(Component.text(ChatColor.GOLD + "Bronze League"));
        List<Component> bronzeLore = new LinkedList<>();
        bronzeLore.add(Component.text(ChatColor.GRAY + "Top 60%"));
        bronzeMeta.lore(bronzeLore);
        bronzeLeague.setItemMeta(bronzeMeta);

        coalLeague = new ItemStack(Material.SUNFLOWER);
        ItemMeta coalMeta = coalLeague.getItemMeta();
        coalMeta.displayName(Component.text(ChatColor.BLACK + "Coal League"));
        List<Component> coalLore = new LinkedList<>();
        coalLore.add(Component.text(ChatColor.GRAY + "Top 80%"));
        coalMeta.lore(coalLore);
        coalLeague.setItemMeta(coalMeta);

        woodLeague = new ItemStack(Material.SUNFLOWER);
        ItemMeta woodMeta = woodLeague.getItemMeta();
        woodMeta.displayName(Component.text(ChatColor.GOLD + "Wood League"));
        List<Component> woodLore = new LinkedList<>();
        woodLore.add(Component.text(ChatColor.GRAY + "Top 100% :/"));
        woodMeta.lore(woodLore);
        woodLeague.setItemMeta(woodMeta);

        leagueMap.put("Oxeye Daisy", oxeyeLeague);
        leagueMap.put("Netherite", netheriteLeague);
        leagueMap.put("Diamond", diamondLeague);
        leagueMap.put("Emerald", emeraldLeague);
        leagueMap.put("Redstone", redstoneLeague);
        leagueMap.put("Lapis", lapisLeague);
        leagueMap.put("Gold", goldLeague);
        leagueMap.put("Iron", ironLeague);
        leagueMap.put("Bronze", bronzeLeague);
        leagueMap.put("Coal", coalLeague);
        leagueMap.put("Wood", woodLeague);
    }
}
