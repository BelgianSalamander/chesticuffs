package salamander.chesticuffs;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.ChunkingFilter;
import net.dv8tion.jda.api.utils.MemberCachePolicy;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.jetbrains.annotations.NotNull;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import salamander.chesticuffs.inventory.ItemHandler;
import salamander.chesticuffs.playerData.DataLoader;

import javax.security.auth.login.LoginException;
import java.awt.*;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;

public class Discord extends ListenerAdapter implements CommandExecutor, Listener {
    JDA jda;

    public Guild guild = null;
    private boolean guildChecked = false;
    public HashMap<UUID, Long> UUIDtoID;
    public HashMap<Long, UUID> IDtoUUID;
    public HashMap<UUID, String> UUIDtoCode;
    public HashMap<UUID, Long> UUIDtoIDUnverified;

    private HashMap<String, Role> leagueToRole;

    public Discord(){
        String token = null;
        try {
            Scanner reader = new Scanner(Chesticuffs.getTokenFile());
            token = reader.nextLine();

        }catch(IOException e){
            e.printStackTrace();
        }

        System.out.println(token);

        try {
            jda = JDABuilder.createDefault(token).
                    setActivity(Activity.playing("Chesticuffs")).
                    enableIntents(GatewayIntent.GUILD_MEMBERS, GatewayIntent.DIRECT_MESSAGES, GatewayIntent.GUILD_MESSAGES).
                    setChunkingFilter(ChunkingFilter.ALL).
                    setMemberCachePolicy(MemberCachePolicy.ALL).
                    build();
        }catch(LoginException e){
            e.printStackTrace();
        }

        jda.addEventListener(this);
        Chesticuffs.getPlugin().getServer().getPluginManager().registerEvents(this, Chesticuffs.getPlugin());

        Chesticuffs.getPlugin().getCommand("verify").setExecutor(this);
        Chesticuffs.getPlugin().getCommand("testlink").setExecutor(this);

        UUIDtoID = new HashMap<>();
        IDtoUUID = new HashMap<>();
        UUIDtoCode = new HashMap<>();
        UUIDtoIDUnverified = new HashMap<>();
        leagueToRole = new HashMap<>();

        //Load saved data
        JSONParser parser = new JSONParser();
        JSONObject JSONdata = null;
        try{
            InputStream is = new FileInputStream(Chesticuffs.getDiscordLinksFile());
            String data = ItemHandler.readFromInputStream(is);
            JSONdata = (JSONObject) parser.parse(data);
        }catch (ParseException e) {
            e.printStackTrace();
        } catch (IOException e){
            e.printStackTrace();
        }

        for(Object key :  JSONdata.keySet()){
            long discordID = Long.valueOf((String) key);
            JSONArray JSONUUID = (JSONArray) JSONdata.get(key);
            UUID playerUUID = new UUID((long) JSONUUID.get(0), (long) JSONUUID.get(1));

            UUIDtoID.put(playerUUID, discordID);
            IDtoUUID.put(discordID, playerUUID);
        }
    }

    private void setupRoles(){
        leagueToRole.put("Oxeye Daisy", guild.getRolesByName("Oxeye Daisy", false).get(0));
        leagueToRole.put("Netherite", guild.getRolesByName("Netherite", false).get(0));
        leagueToRole.put("Diamond", guild.getRolesByName("Diamond", false).get(0));
        leagueToRole.put("Emerald", guild.getRolesByName("Emerald", false).get(0));
        leagueToRole.put("Redstone", guild.getRolesByName("Redstone", false).get(0));
        leagueToRole.put("Lapis", guild.getRolesByName("Lapis", false).get(0));
        leagueToRole.put("Gold", guild.getRolesByName("Gold", false).get(0));
        leagueToRole.put("Iron", guild.getRolesByName("Iron", false).get(0));
        leagueToRole.put("Coal", guild.getRolesByName("Coal", false).get(0));
        leagueToRole.put("Wood", guild.getRolesByName("Wood", false).get(0));
    }

    public void updateMemberRoles(){
        if(guild == null){
            return;
        }

        for(Map.Entry<UUID, Long> entry : UUIDtoID.entrySet()){
            Member target = guild.getMemberById(entry.getValue());
            if(target == null) continue;
            String leagueName = DataLoader.getData().get(entry.getKey()).getLeague();
            Role targetRole = leagueToRole.get(leagueName);
            if(targetRole == null){
                System.out.println(ChatColor.RED + leagueName + " discord role could not be found!");
            }
            for(Role role : leagueToRole.values()){
                if(role.equals(targetRole)){
                    continue;
                }
                guild.removeRoleFromMember(target, role).queue();
            }

            guild.addRoleToMember(target, targetRole).queue();
        }
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if(!(sender instanceof Player)){
            sender.sendMessage(ChatColor.RED + "Only players can execute this command!");
            return true;
        }

        Player player = (Player) sender;

        if(command.getName().equalsIgnoreCase("verify")) {
            if (UUIDtoID.containsKey(player.getUniqueId())) {
                player.sendMessage(ChatColor.RED + "You are already verified!");
                return true;
            }

            if(!UUIDtoCode.containsKey(player.getUniqueId())){
                player.sendMessage(ChatColor.RED + "You are not pending verification");
                return true;
            }

            if(args.length < 1){
                player.sendMessage(ChatColor.RED + "Please provide a code!");
                return true;
            }

            String actualCode = UUIDtoCode.get(player.getUniqueId());
            if(!actualCode.equals(args[0])){
                player.sendMessage(ChatColor.RED + "Invalid Code!");
                return true;
            }

            long discordID = UUIDtoIDUnverified.get(player.getUniqueId());
            Member target = guild.getMemberById(discordID);
            //System.out.println("Guild Members:");
            //guild.getMembers().stream().forEach((Member member) -> {System.out.println(member.getEffectiveName());});
            if(target == null){
                UUIDtoCode.remove(player.getUniqueId());
                UUIDtoIDUnverified.remove(player.getUniqueId());
                player.sendMessage(ChatColor.RED + "You are not on the discord server!");
                return true;
            }

            UUIDtoCode.remove(player.getUniqueId());
            UUIDtoIDUnverified.remove(player.getUniqueId());
            UUIDtoID.put(player.getUniqueId(), discordID);
            Role verified = guild.getRolesByName("Verified", false).get(0);
            guild.addRoleToMember(target, verified).queue();
            player.sendMessage(ChatColor.GREEN + "Succesfully linked with " + target.getEffectiveName());
        }else if(command.getName().equalsIgnoreCase("testlink")){
            guild.getMemberById(UUIDtoID.get(player.getUniqueId())).getUser().openPrivateChannel().complete().sendMessage("Test").queue();
        }
        return true;
    }

    @Override
    public void onGuildMessageReceived(@NotNull GuildMessageReceivedEvent e) {
        if(!guildChecked){
            guild = e.getGuild();
            guildChecked = true;
            setupRoles();
        }

        if(e.getAuthor().isBot() || e.isWebhookMessage()) return;
        if(!e.getMessage().getContentRaw().startsWith("~")) return;
        String[] args = e.getMessage().getContentRaw().split(" ");
        if(args[0].equals("~verify")){
            if(e.getMember().getRoles().stream().filter(role -> role.getName().equals("Verified")).findAny().orElse(null) != null){
                e.getMessage().getChannel().sendMessage(":x: **|** Error! " + e.getAuthor().getAsMention() + ", you are already verified!").queue();
                return;
            }

            if(args.length < 2){
                e.getChannel().sendMessage(":x: **|** Error! " + e.getAuthor().getAsMention() + ", you need to specify a player!").queue();
                return;
            }

            System.out.println(args[1]);
            Player target = Bukkit.getPlayer(args[1]);

            if(target == null){
                e.getChannel().sendMessage(":x: **|** Error! " + e.getAuthor().getAsMention() + ", that player is not online!").queue();
                return;
            }

            if(UUIDtoIDUnverified.containsKey(target.getUniqueId())){
                e.getChannel().sendMessage(":x: **|** Error! " + e.getAuthor().getAsMention() + ", you already have a code generated!").queue();
                return;
            }

            String randomCode = String.valueOf(new Random().nextInt());

            while(UUIDtoCode.values().contains(randomCode)){
                randomCode = String.valueOf(new Random().nextInt());
            }
            UUIDtoCode.put(target.getUniqueId(), randomCode);
            UUIDtoIDUnverified.put(target.getUniqueId(), e.getAuthor().getIdLong());
            e.getAuthor().openPrivateChannel().complete().sendMessage("To link your discord and minecraft accounts, do this command in-game: ```/verify " + randomCode +"```").queue();
        }else if(args[0].equals("~item")){
            if(args.length == 1){
                e.getMessage().getChannel().sendMessage(":x: **|** Error! " + e.getAuthor().getAsMention() + ", example_usage : ~item oxeye_daisy").queue();
                return;
            }

            String itemName = args[1].toUpperCase();
            JSONObject itemStats =  (JSONObject) ItemHandler.itemData.get(itemName);

            if(itemStats == null){
                e.getMessage().getChannel().sendMessage(":x: **|** Error! " + e.getAuthor().getAsMention() + ", couldn't find that item!").queue();
                return;
            }

            EmbedBuilder embedBuilder = new EmbedBuilder().setTitle(args[1].toUpperCase());
            String type = (String) itemStats.get("type");
            embedBuilder.addField("Item Type", type, false);
            if(type.equals("item")){
                short ATK = (short) (long) itemStats.get("ATK");
                short DEF = (short) (long) itemStats.get("DEF");
                short HP = (short) (long) itemStats.get("HP");
                JSONArray traits = (JSONArray) itemStats.get("traits");
                String flavor = (String) itemStats.get("flavor");

                embedBuilder = embedBuilder.addField("Base Statistics", "ATK: " + ATK + "\nDEF: " + DEF + "\nHP: " + HP, false).setColor(Color.BLUE);
                embedBuilder =  embedBuilder.addField("Traits", String.join("\n", traits), false);
                embedBuilder = embedBuilder.addField("Flavor", "\"" + flavor + "\"", false);
            }else if(type.equals("core")){
                short HP = (short) (long) itemStats.get("HP");
                String buff = (String) itemStats.get("buff");
                String debuff = (String) itemStats.get("debuff");
                String flavor = (String) itemStats.get("flavor");
                int effectID = (int) (long) itemStats.get("effectID");

                embedBuilder = embedBuilder.addField("Core Health", String.valueOf(HP), false).setColor(Color.red);
                if(!buff.isEmpty()) {
                    embedBuilder = embedBuilder.addField("Buff", buff, false);
                }
                if(!debuff.isEmpty()) {
                    embedBuilder = embedBuilder.addField("Debuff", debuff, false);
                }

                embedBuilder = embedBuilder.addField("Flavor", "\"" + flavor + "\"", false);
            }

            e.getChannel().sendMessage(embedBuilder.build()).queue();
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e){
        UUIDtoCode.remove(e.getPlayer().getUniqueId());
        UUIDtoIDUnverified.remove(e.getPlayer().getUniqueId());
    }

    public void save(){
        JSONObject discordObject = new JSONObject();
        for(Map.Entry<UUID, Long> entry : UUIDtoID.entrySet()){
            JSONArray uuid = new JSONArray();
            uuid.add(entry.getKey().getMostSignificantBits());
            uuid.add(entry.getKey().getLeastSignificantBits());
            discordObject.put(String.valueOf(entry.getValue()), uuid);
        }
        try {
            FileWriter jsonWriter = new FileWriter(Chesticuffs.getDiscordLinksFile().getAbsolutePath());
            jsonWriter.write(discordObject.toJSONString());
            jsonWriter.close();
        } catch (IOException e){
            e.printStackTrace();
        }

    }

    public void stop(){
        jda.shutdownNow();
    }
}
