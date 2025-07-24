package dev.julizey.customtools.command;

import dev.julizey.customtools.CustomTools;
import dev.julizey.customtools.manager.CostManager;
import dev.julizey.customtools.utils.Text;
import dev.julizey.customtools.utils.TimeUnit;
import java.io.File;
import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.StringUtil;

public class AdminCommand extends dev.julizey.customtools.utils.Command {

  private static final int MB_Divisor = 1024 * 1024;
  private List<String> helperCmds = List.of(
    "eco",
    "worlds",
    "specs",
    "reload",
    "status",
    "help"
  );
  private List<String> adminCmds = List.of("on", "off", "rank");
  private List<String> alwaysActive = List.of(
    "on",
    "off",
    "help",
    "reload",
    "status",
    "stop"
  );

  private boolean enabled = true;
  private boolean canBeToggled = true;

  public AdminCommand() {
    super(
      "admin",
      new String[] {
        "---------- Help for the Admin command ----------",
        "| /admin - Toggles Super Rank mode",
        "| /admin on - Enables admin mode",
        "| /admin off - Disables admin mode",
        "| /admin rank promote <player> - Promotes a player to admin",
        "| /admin rank demote <player> - Demotes a player from admin",
        "| /admin rank info - Displays the Info about the ranks in a track",
        "| /admin rank set <player> <rank> - Sets a player's rank",
        "| /admin eco total - Displays the total coins",
        "| /admin eco top - Displays the players with the most coins",
        "| /admin reload - Reloads the plugin configuration",
        "| /admin stop - stops the server",
        "| /admin restart - restarts the server",
        "| /admin status - Displays the current admin status",
        "| /admin specs - Displays Server Specs",
        "| /admin help - Displays this help message",
        "-------------------------------------------------",
      }
    );
    this.canBeToggled = !section.getBoolean("always-on", false);
    this.alwaysActive = section.getStringList("always-on-commands");
    this.helperCmds =
      section.getConfigurationSection("commands").getStringList("helper");
    this.adminCmds =
      section.getConfigurationSection("commands").getStringList("admin");
  }

  public void reload() {
    super.reload();
    this.canBeToggled = !section.getBoolean("always-on", false);
    this.alwaysActive = section.getStringList("always-on-commands");
    this.helperCmds =
      section.getConfigurationSection("commands").getStringList("helper");
    this.adminCmds =
      section.getConfigurationSection("commands").getStringList("admin");
  }

  @Override
  public void execute() {
    if (!enabled && !alwaysActive.contains(args[0].toLowerCase())) {
      Text.send(sender, "messages.admin-disabled");
      return;
    }
    if (canNotExecute(sender, args[0].toLowerCase())) {
      sendNoPermissionMessage(sender);
      return;
    }

    switch (args[0].toLowerCase()) {
      case "help" -> sendHelp(sender);
      case "status" -> {
        sendStatus(sender);
      }
      case "specs" -> {
        sendSpecs(sender);
      }
      case "reload" -> {
        CustomTools.plugin.getLogger().info("Reload");
        CustomTools.plugin.reload(sender);
      }
      case "stop" -> Bukkit.shutdown();
      case "on" -> {
        if (!canBeToggled) {
          Text.send(sender, "messages.admin-cannot-be-toggled");
          return;
        }
        this.enabled = true;
        Text.send(sender, "messages.admin-enabled");
        CustomTools.plugin.animator.play(sender, "admin-enable");
      }
      case "off" -> {
        if (!canBeToggled) {
          Text.send(sender, "messages.admin-cannot-be-toggled");
          return;
        }
        this.enabled = false;
        Text.send(sender, "messages.admin-disabled");
        CustomTools.plugin.animator.play(sender, "admin-disable");
      }
      case "rank" -> {
        final String trackName = section.getString("track-name", "ranks");
        CustomTools.plugin.getLogger().info(trackName);
        if (args[1].equalsIgnoreCase("promote")) {
          if (args.length < 3) {
            Text.send(sender, "messages.commands.wrong-arguments");
            return;
          }
          execute(sender, "lp user " + args[2] + " promote " + trackName, true);
        } else if (args[1].equalsIgnoreCase("demote")) {
          if (args.length < 3) {
            Text.send(sender, "messages.commands.wrong-arguments");
            return;
          }
          execute(sender, "lp user " + args[2] + " demote " + trackName, true);
        } else if (args[1].equalsIgnoreCase("set")) {
          if (args.length < 4) {
            Text.send(sender, "messages.commands.wrong-arguments");
            return;
          }
          execute(
            sender,
            "lp user " + args[2] + " parent set " + args[3],
            true
          );
        } else if (args[1].equalsIgnoreCase("info")) {
          execute(sender, "lp listgroups", true);
        }
      }
      case "eco" -> {
        if (args.length < 2) {
          Text.send(sender, "messages.commands.wrong-arguments");
          return;
        }
        if (args[1].equalsIgnoreCase("total")) {
          double total = 0;
          List<Player> players = new ArrayList<>();
          for (Player p : CustomTools.plugin.getServer().getOnlinePlayers()) {
            total += CostManager.economy.getBalance(p);
            players.add(p);
          }
          for (OfflinePlayer p : CustomTools.plugin
            .getServer()
            .getOfflinePlayers()) {
            if (players.contains(p)) {
              continue;
            }
            total += CostManager.economy.getBalance(p);
          }
          players.clear();
          Text.send(
            sender,
            "messages.admin-coins-total",
            new Text.Replaceable("%total%", String.valueOf(Math.round(total)))
          );
        } else if (args[1].equalsIgnoreCase("top")) {
          Text.send(sender, "todo");
        }
      }
      default -> {
        Text.send(sender, "messages.commands.invalid-command");
      }
    }
    return;
  }

  @Override
  public ArrayList<String> complete() {
    ArrayList<String> tabComplete = new ArrayList<>();
    if (args.length == 2) {
      if (sender.hasPermission("customtools.admin")) {
        if (args[0].equalsIgnoreCase("rank")) {
          tabComplete.addAll(List.of("promote", "demote", "set", "info"));
        }
        if (args[0].equalsIgnoreCase("eco")) {
          tabComplete.addAll(List.of("total", "top"));
        }
      }

      return StringUtil.copyPartialMatches(
        args[1],
        tabComplete,
        new ArrayList<>()
      );
    } else if (args.length == 1) {
      if (sender.hasPermission("customtools.helper")) {
        tabComplete.addAll(helperCmds);
      }
      if (sender.hasPermission("customtools.admin")) {
        tabComplete.addAll(adminCmds);
      }
      return StringUtil.copyPartialMatches(
        args[0],
        tabComplete,
        new ArrayList<>()
      );
    }

    return null;
  }

  // todo: Optimize!!!!
  public static void sendStatus(CommandSender sender) {
    final long totalMemory = Runtime.getRuntime().totalMemory() / MB_Divisor;
    final long usedMemory =
      totalMemory - Runtime.getRuntime().freeMemory() / MB_Divisor;
    final String uptime = TimeUnit.format(
      ManagementFactory.getRuntimeMXBean().getUptime(),
      true
    );

    String worldList = "";
    for (World world : CustomTools.plugin.getServer().getWorlds()) {
      if (worldList == "") {
        worldList =
          "(" +
          CustomTools.plugin.getServer().getWorlds().size() +
          "): &b" +
          world.getName();
      } else {
        worldList += ", " + world.getName();
      }
    }

    String banned = "";
    for (OfflinePlayer player : Bukkit.getBannedPlayers()) {
      if (banned == "") {
        banned =
          "(" + Bukkit.getBannedPlayers().size() + "): &b" + player.getName();
      } else {
        banned += ", " + player.getName();
      }
    }
    final String onlinePlayers = String.valueOf(
      CustomTools.plugin.getServer().getOnlinePlayers().size()
    );
    final String maxPlayers = String.valueOf(
      CustomTools.plugin.getServer().getMaxPlayers()
    );
    final String ops = String.valueOf(
      CustomTools.plugin.getServer().getOperators().size()
    );
    final String name = CustomTools.plugin.getServer().getName();
    final String ip = CustomTools.plugin.getServer().getIp().isEmpty()
      ? "127.0.0.1"
      : CustomTools.plugin.getServer().getIp();
    final String port = String.valueOf(
      CustomTools.plugin.getServer().getPort()
    );
    final String ping = String.valueOf(
      sender instanceof Player ? ((Player) sender).getPing() : "-1"
    );
    final String version = CustomTools.plugin.getServer().getVersion();

    Text.send(
      sender,
      "messages.admin-status",
      new Text.Replaceable("%online-players%", onlinePlayers),
      new Text.Replaceable("%max-players%", maxPlayers),
      new Text.Replaceable("%banned-players%", banned),
      new Text.Replaceable("%ops-players%", ops),
      new Text.Replaceable("%worlds%", worldList),
      new Text.Replaceable(
        "%ram-current%",
        String.valueOf(Math.round(usedMemory))
      ),
      new Text.Replaceable(
        "%ram-total%",
        String.valueOf(Math.round(totalMemory))
      ),
      new Text.Replaceable(
        "%whitelistStatus%",
        CustomTools.plugin.getServer().isWhitelistEnforced()
          ? "enabled"
          : "disabled"
      ),
      new Text.Replaceable("%uptime%", uptime),
      new Text.Replaceable("%ping%", ping),
      new Text.Replaceable("%name%", name),
      new Text.Replaceable("%ip%", ip),
      new Text.Replaceable("%port%", port),
      new Text.Replaceable("%version%", version)
    );
  }

  public static void sendSpecs(CommandSender sender) {
    final String os =
      System.getProperty("os.name") +
      " " +
      System.getProperty("os.version") +
      " " +
      System.getProperty("os.arch");
    final int coreCount = Runtime.getRuntime().availableProcessors();
    String diskSpace = "";
    try {
      diskSpace =
        String.valueOf(File.listRoots()[0].getUsableSpace() / MB_Divisor) +
        "mb / " +
        String.valueOf(File.listRoots()[0].getTotalSpace() / MB_Divisor) +
        "mb";
    } catch (Exception e) {
      diskSpace = "-1mb / -1mb";
    }
    final String enc = System.getProperty("native.encoding");
    final String javaVersion = System.getProperty("java.version");
    final String vm =
      System.getProperty("java.vm.name") +
      " " +
      System.getProperty("java.vm.version");
    final String totalMemory = String.valueOf(
      Math.round(Runtime.getRuntime().totalMemory() / MB_Divisor)
    );
    final String maxMemory = String.valueOf(
      Math.round(
        Runtime.getRuntime().maxMemory() == Long.MAX_VALUE
          ? -1
          : Runtime.getRuntime().maxMemory() / MB_Divisor
      )
    );

    final String version = CustomTools.plugin.getServer().getVersion();
    final String name = CustomTools.plugin.getServer().getName();
    final String ip = CustomTools.plugin.getServer().getIp().isEmpty()
      ? "127.0.0.1"
      : CustomTools.plugin.getServer().getIp();
    final String port = String.valueOf(
      CustomTools.plugin.getServer().getPort()
    );

    String plugins = "";
    for (Plugin plugin : CustomTools.plugin
      .getServer()
      .getPluginManager()
      .getPlugins()) {
      if (plugins == "") {
        plugins = plugin.getName();
      } else {
        plugins += ", " + plugin.getName();
      }
    }
    final String pluginsCount = String.valueOf(
      CustomTools.plugin.getServer().getPluginManager().getPlugins().length
    );
    Text.send(
      sender,
      "messages.admin-specs",
      new Text.Replaceable("%version%", version),
      new Text.Replaceable("%coreCount%", String.valueOf(coreCount)),
      new Text.Replaceable("%os%", os),
      new Text.Replaceable("%enc%", enc),
      new Text.Replaceable("%javaVersion%", javaVersion),
      new Text.Replaceable("%vm%", vm),
      new Text.Replaceable("%ram%", totalMemory + "mb / " + maxMemory + "mb"),
      new Text.Replaceable("%disk%", diskSpace),
      new Text.Replaceable("%version%", version),
      new Text.Replaceable("%name%", name),
      new Text.Replaceable("%ip%", ip),
      new Text.Replaceable("%port%", port),
      new Text.Replaceable("%plugins%", plugins),
      new Text.Replaceable("%plugins-count%", pluginsCount)
    );
  }

  private static void execute(
    CommandSender sender,
    String command,
    boolean mustBeAdmin
  ) {
    if (
      mustBeAdmin &&
      !sender.hasPermission("customtools.admin") ||
      !sender.hasPermission("customtools.helper")
    ) {
      sendNoPermissionMessage(sender);
    }
    try {
      CustomTools.plugin.getServer().dispatchCommand(sender, command);
    } catch (Exception e) {
      Text.send(sender, "messages.commands.error-occurred");
    }
  }

  private boolean canNotExecute(CommandSender sender, String cmd) {
    final boolean hasAdmin = sender.hasPermission("customtools.admin");
    if (hasAdmin) {
      return false;
    }
    if (adminCmds.contains(cmd)) {
      return true;
    }
    return !(
      helperCmds.contains(cmd) && sender.hasPermission("customtools.helper")
    );
  }
}
