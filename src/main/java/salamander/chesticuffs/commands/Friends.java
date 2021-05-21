package salamander.chesticuffs.commands;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import salamander.chesticuffs.ChestManager;

public class Friends implements CommandExecutor
{
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args)
    {
        if (!(sender instanceof Player))
        {
            sender.sendMessage(ChatColor.RED + "Only players can execute this command");
            return true;
        }
        if (!command.getName().equalsIgnoreCase("friends")) return true;/* ||
            !command.getName().equalsIgnoreCase("message") ||
            !command.getName().equalsIgnoreCase("msg")) return true;*/

        Player player = (Player) sender;

        /*
        * args list as of 21/5/2021
        * args[0] = command
        * args[1] usually = player
        * args[2] = message
        * */

        switch(command.getName())
        {
            case "friends":
                if (args.length == 0)
                {
                    //return help information
                    sender.sendMessage("Chesticuffs Friend Manager");
                    sender.sendMessage("=-=-=-=-=-=-=-=-=-=-=-=-=-=");
                    sender.sendMessage("Commands:");
                    sender.sendMessage("/friends list");
                    sender.sendMessage("/friends add [player]");
                    sender.sendMessage("/friends remove [player]");
                    sender.sendMessage("/friends accept [player]");
                    sender.sendMessage("/friends decline [player]");
                    sender.sendMessage("/friends msg [player] or /msg [player]");
                }

                else if (args.length == 1)
                {
                    if (args[0].equals("list"))
                    {
                        //usage: /friends list
                    }
                }

                else if (args.length == 2)
                {
                    Player recipient = getPlayerFromName(player, args[1]);
                    //switch to switch statement
                    //only if statement due to it being like 12 at night and i am very tired will work on tomorrow probably
                    if (args[0].equals("add"))
                    {
                        //is for looping the whole server the best way to go about it????

                        //usage: friend add [player]
                        //send a friend request to another player specified by args[1]
                        /*
                         * Send a message to the desired player by system with friend request details
                         * Time limit before autodecline? - probably not
                         * Allows the player to run /friend accept [player]
                         * */
                    }
                    else if (args[0].equals("remove"))
                    {
                        //usage: friend remove [player]
                        //remove player.getUniqueId() from a json or SQL database specified by args[1]
                    }
                    else if (args[0].equals("accept"))
                    {
                        //usage: friend accept [player]
                        //add player.getUniqueId() to a json or SQL database specified by args[1]
                    }
                    else if (args[0].equals("decline"))
                    {
                        //usage: friend decline [player]
                        //declines friend request from another player specified by args[1]
                    }
                }
                else if(args.length == 3)
                {
                    //if recipient is on your friends list
                    Player recipient = getPlayerFromName(player, args[1]);
                    if(args[0].equals("msg"))
                    {
                        //messagePlayer(player, recipient, args[2]);
                    }
                }
                break;
            case "msg":
            case "message":
                if(args.length == 1)
                {
                    //if in friends list
                    //messagePlayer(player, recipient, args[1]);
                }
                else
                {
                    sender.sendMessage("Please specify a player to message");
                }
                break;
        }
        return true;
    }

    public static void messagePlayer(Player p, Player recipient, String msg)
    {
        p.sendMessage(p.getName() + " -> " + msg);
        recipient.sendMessage(p.getName() + " -> " + msg);
    }

    private Player getPlayerFromName(Player sender, String name)
    {
        for(Player p : sender.getServer().getOnlinePlayers())
        {
            if(p.getName().equalsIgnoreCase(name))
            {
                return p;
            }
        }
        sender.sendMessage("Sorry, there is nobody on the server with that name.");
        return null;
    }
}
