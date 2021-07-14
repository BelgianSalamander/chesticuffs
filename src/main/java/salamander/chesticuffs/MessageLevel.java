package salamander.chesticuffs;

import org.bukkit.ChatColor;

public enum MessageLevel {
    DEBUG_INFO(0, ChatColor.LIGHT_PURPLE + "[DEBUG] "),
    INFO(1,  ChatColor.GREEN + "[INFO] "),
    WARNING(2, ChatColor.YELLOW +"[WARNING] "),
    ERROR(3, ChatColor.GOLD + "[ERROR] "),
    FATAL(4, ChatColor.RED + "[FATAL] ");

    public String getPrefix() {
        return prefix;
    }

    private int level;
    private String prefix;

    MessageLevel(int lvl, String prefix){level = lvl; this.prefix = prefix;}
}
