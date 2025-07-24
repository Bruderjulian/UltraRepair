package dev.julizey.customtools.utils;

import dev.julizey.customtools.CustomTools;
import java.io.File;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
//import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

public class Text {

  public record Replaceable(String placeholder, String value) {}

  public static String prefix = "&7[&6CustomTools&7]";
  public static boolean hasHexSupport = true;
  private static final HashMap<String, String> colorMap;
  private static HashMap<String, String> messageCache = new HashMap<>();

  public static boolean hasPlaceholderAPI = false;

  static {
    colorMap = new HashMap<>();
    colorMap.put("&0", "<black>");
    colorMap.put("&1", "<dark_blue>");
    colorMap.put("&2", "<dark_green>");
    colorMap.put("&3", "<dark_aqua>");
    colorMap.put("&4", "<dark_red>");
    colorMap.put("&5", "<dark_purple>");
    colorMap.put("&6", "<gold>");
    colorMap.put("&7", "<gray>");
    colorMap.put("&8", "<dark_gray>");
    colorMap.put("&9", "<blue>");
    colorMap.put("&a", "<green>");
    colorMap.put("&b", "<aqua>");
    colorMap.put("&c", "<red>");
    colorMap.put("&d", "<light_purple>");
    colorMap.put("&e", "<yellow>");
    colorMap.put("&f", "<white>");
    colorMap.put("&k", "<obfuscated>");
    colorMap.put("&l", "<bold>");
    colorMap.put("&m", "<strikethrough>");
    colorMap.put("&n", "<underline>");
    colorMap.put("&o", "<italic>");
    colorMap.put("&r", "<reset>");
  }

  private static final Pattern HEX_PATTERN = Pattern.compile(
    "(?:<#|#)([A-Fa-f0-9]{6})>?"
  );

  public static String convertColor(String str) {
    for (final HashMap.Entry<String, String> entry : colorMap.entrySet()) {
      str = str.replace(entry.getValue(), entry.getKey());
    }
    if (hasHexSupport) {
      for (
        Matcher matcher = HEX_PATTERN.matcher(str);
        matcher.find();
        matcher = HEX_PATTERN.matcher(str)
      ) {
        final String hexCode = matcher.group(1).toLowerCase();
        String magic = "&x";
        final int len = hexCode.length();
        for (int i = 0; i < len; ++i) {
          magic += '&' + hexCode.substring(i, i + 1);
        }
        str =
          str.substring(0, matcher.start()) +
          magic +
          str.substring(matcher.end());
      }
    }
    return ChatColor.translateAlternateColorCodes('&', str);
  }

  public static String format(
    String msg,
    final boolean withColor,
    final boolean withPrefix,
    final Replaceable... replaceables
  ) {
    if (msg.startsWith("messages.")) {
      final String lookup = messageCache.get(msg.substring(9));
      if (lookup == null) {
        msg = "Missing message: " + msg;
        Text.warn(msg);
      } else {
        msg = lookup;
      }
    }
    if (msg == null || msg.length() == 0) return null;

    for (final Replaceable replaceable : replaceables) {
      msg = msg.replaceAll(replaceable.placeholder(), replaceable.value());
    }

    if (withPrefix) {
      msg = (prefix + " " + msg).replace("\n", "\n" + prefix + " ");
    }
    return withColor
      ? convertColor(msg)
      : ChatColor.stripColor(convertColor(msg));
  }

  public static void send(
    final CommandSender sender,
    String msg,
    final Replaceable... replaceables
  ) {
    if (sender instanceof Player && hasPlaceholderAPI) {
      //msg = PlaceholderAPI.setPlaceholders((Player) sender, msg);
    }
    final boolean hasColor =
      sender instanceof Player || sender instanceof ConsoleCommandSender;
    msg = format(msg, hasColor, true, replaceables);

    if (msg == null) return;
    sender.sendMessage(msg);
  }

  public static void sendTitle(Player p, String title, String subtitle) {
    p.sendTitle(
      format(title, true, false),
      format(subtitle, true, false),
      20,
      3 * 20,
      10
    );
  }

  public static void info(String msg, final Replaceable... replaceables) {
    msg = format(msg, false, true, replaceables);
    if (msg == null) return;
    Bukkit.getLogger().info(msg);
  }

  public static void warn(String msg, final Replaceable... replaceables) {
    msg = format(msg, false, true, replaceables);
    if (msg == null) return;
    Bukkit.getLogger().warning(msg);
  }

  public static void severe(String msg, final Replaceable... replaceables) {
    msg = format(msg, false, true, replaceables);
    if (msg == null) return;
    Bukkit.getLogger().severe(msg);
  }

  public static void error(
    final Throwable throwable,
    final String description,
    final boolean disable
  ) {
    if (throwable != null) {
      throwable.printStackTrace();
    }

    severe("*-----------------------------------------------------*");
    severe(
      "An error has occurred in " +
      CustomTools.plugin.getDescription().getName() +
      "."
    );
    severe("Description: " + description);
    severe("Contact the plugin author if you cannot fix this issue.");
    severe("*-----------------------------------------------------*");
    if (
      disable && Bukkit.getPluginManager().isPluginEnabled(CustomTools.plugin)
    ) {
      Bukkit.getPluginManager().disablePlugin(CustomTools.plugin);
    }
  }

  public static void reload() {
    hasPlaceholderAPI =
      Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI");

    try {
      final String bukkitVersion = CustomTools.plugin
        .getServer()
        .getBukkitVersion();
      final String[] parts = bukkitVersion.split("\\.");
      hasHexSupport = Integer.parseInt(parts[1]) >= 16;
    } catch (final Exception e) {
      hasHexSupport = false;
    }
    final File messageFile = new File(
      CustomTools.plugin.getDataFolder().getAbsolutePath(),
      "messages.yml"
    );
    if (!messageFile.exists()) {
      CustomTools.plugin.saveResource("messages.yml", false);
    }
    final YamlConfiguration messagesConfig = YamlConfiguration.loadConfiguration(
      messageFile
    );
    if (messagesConfig == null) {
      Text.warn("Failed to load messages.yml");
      return;
    }
    messageCache.clear();
    for (final String key : messagesConfig.getKeys(false)) {
      String value;
      if (messagesConfig.isList(key)) {
        value = String.join("\n", messagesConfig.getStringList(key));
      } else {
        value = messagesConfig.getString(key);
      }
      if (value == null) {
        Text.warn("Missing message in messages.yml: " + key);
      }
      messageCache.put(key, value);
    }

    prefix = messagesConfig.getString("prefix", "&7[&6CustomTools&7]");
  }
}
