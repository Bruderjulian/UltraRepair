package dev.julizey.customtools.command;

import dev.julizey.customtools.CustomTools;
import dev.julizey.customtools.manager.WhiteListManager;
import dev.julizey.customtools.utils.Text;
import java.util.ArrayList;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.util.StringUtil;

public class WLCommand
  extends dev.julizey.customtools.utils.Command
  implements Listener {

  private boolean allowModify;
  private boolean usePreLoginEvent;
  private WhiteListManager manager;

  public WLCommand() {
    super(
      "whitelist",
      new String[] {
        "---------- Help for the Whitelist command ----------",
        "| /whitelist add <player> - Adds the specified player to the whitelist",
        "| /whitelist remove <player> - Removes the specified player from the whitelist",
        "| /whitelist on - Enables the whitelist",
        "| /whitelist off - Disables the whitelist",
        "| /whitelist list - Displays the whitelisted players",
        "| /whitelist status - Displays the status of the whitelist",
        "| /whitelist help - Displays this help message",
        "-------------------------------------------------",
      }
    );
    allowModify = section.getBoolean("allow-modify", true);
    usePreLoginEvent = section.getBoolean("use-preloginEvent", true);
    manager = new WhiteListManager(section);
  }

  public void reload() {
    super.reload();
    allowModify = section.getBoolean("allow-modify", true);
    usePreLoginEvent = section.getBoolean("use-preloginEvent", true);
    manager.reload();
  }

  @Override
  public void execute() {
    if (args.length == 0) {
      if (
        !hasPermission(sender, "customtools.whitelist.view") ||
        !hasPermission(sender, "customtools.whitelist.modify")
      ) {
        sendNoPermissionMessage(sender);
        sendHelp(sender);
        return;
      }
      Text.send(sender, "messages.commands.invalid-command");
      sendHelp(sender);
      return;
    }

    final String cmd = args[0].toLowerCase();

    switch (cmd) {
      case "reload":
        {
          CustomTools.plugin.reload(sender);
          return;
        }
      case "remove":
        {
          if (
            !hasPermission(sender, "customtools.whitelist.modify") &&
            !allowModify
          ) {
            sendNoPermissionMessage(sender);
            return;
          }
          if (args.length < 2) {
            Text.send(sender, "messages.whitelist-provide-player");
            return;
          }
          CustomTools.plugin.animator.play(sender, "wl-remove");
          if (!manager.isWhitelisted(args[1])) {
            Text.send(
              sender,
              "messages.whitelist-not-on-whitelisted",
              new Text.Replaceable("%player%", args[1])
            );
            return;
          }
          manager.removeWhitelist(args[1]);
          Text.send(
            sender,
            "messages.whitelist-remove",
            new Text.Replaceable("%player%", args[1])
          );
          return;
        }
      case "add":
        {
          if (
            !hasPermission(sender, "customtools.whitelist.modify") &&
            !allowModify
          ) {
            sendNoPermissionMessage(sender);
            return;
          }
          if (args.length < 2) {
            Text.send(sender, "messages.whitelist-provide-player");
            return;
          }
          CustomTools.plugin.animator.play(sender, "wl-add");

          if (manager.isWhitelisted(args[1])) {
            Text.send(
              sender,
              "messages.whitelist-already-on-whitelisted".replace(
                  "%player%",
                  args[1]
                )
            );
            return;
          }
          manager.addWhitelist(args[1]);
          Text.send(
            sender,
            "messages.whitelist-add",
            new Text.Replaceable("%player%", args[1])
          );
          return;
        }
      case "off":
        {
          if (
            !hasPermission(sender, "customtools.whitelist.modify") &&
            !allowModify
          ) {
            sendNoPermissionMessage(sender);
            return;
          }
          CustomTools.plugin.animator.play(sender, "wl-off");

          manager.setWhitelist(false);
          Text.send(sender, "messages.whitelist-disabled");
          return;
        }
      case "on":
        {
          if (
            !hasPermission(sender, "customtools.whitelist.modify") &&
            !allowModify
          ) {
            sendNoPermissionMessage(sender);
            return;
          }
          CustomTools.plugin.animator.play(sender, "wl-on");

          manager.setWhitelist(true);
          Text.send(sender, "messages.whitelist-enabled");
          return;
        }
      case "list":
        {
          if (!hasPermission(sender, "customtools.whitelist.view")) {
            sendNoPermissionMessage(sender);
            return;
          }
          String names = "";
          for (final String str : manager.getWhiteLists()) {
            names += str + "&e, &7";
          }
          Text.send(
            sender,
            "messages.whitelist-list",
            new Text.Replaceable("%players%", names)
          );
          return;
        }
      case "help":
        {
          if (!hasPermission(sender, "customtools.whitelist.view")) {
            sendNoPermissionMessage(sender);
            return;
          }
          sendHelp(sender);
          return;
        }
      case "status":
        {
          if (!hasPermission(sender, "customtools.whitelist.view")) {
            sendNoPermissionMessage(sender);
            return;
          }
          Text.send(
            sender,
            "messages.whitelist-status",
            new Text.Replaceable(
              "%status%",
              manager.isWhitelisting() ? "on" : "off"
            ),
            new Text.Replaceable(
              "%playerCount%",
              String.valueOf(manager.getWhiteLists().size())
            )
          );
          return;
        }
      default:
        break;
    }
  }

  @Override
  public ArrayList<String> complete() {
    ArrayList<String> tabComplete = new ArrayList<>();

    if (
      args.length == 2 &&
      (
        sender.hasPermission("customtools.whitelist.add") ||
        sender.hasPermission("customtools.whitelist.remove")
      )
    ) {
      for (Player p : Bukkit.getOnlinePlayers()) {
        tabComplete.add(p.getName());
      }
      return StringUtil.copyPartialMatches(
        args[1],
        tabComplete,
        new ArrayList<>()
      );
    } else if (args.length == 1) {
      if (sender.hasPermission("customtools.whitelist.view")) {
        tabComplete.add("help");
        tabComplete.add("status");
        tabComplete.add("list");
      }
      if (sender.hasPermission("customtools.whitelist.modify")) {
        tabComplete.add("on");
        tabComplete.add("off");
        tabComplete.add("add");
        tabComplete.add("remove");
      }
      return StringUtil.copyPartialMatches(
        args[0],
        tabComplete,
        new ArrayList<>()
      );
    }

    return null;
  }

  @EventHandler
  public void onLogin(final PlayerLoginEvent e) {
    if (usePreLoginEvent) return;
    final Player p = e.getPlayer();
    if (p == null || manager.isWhitelisted(p.getName())) {
      return;
    }
    e.disallow(
      PlayerLoginEvent.Result.KICK_WHITELIST,
      manager.getNotWhitelistMsg()
    );
  }

  @EventHandler
  public void onPreLogin(final AsyncPlayerPreLoginEvent e) {
    if (!usePreLoginEvent) return;
    final String p = e.getName();
    if (manager.isWhitelisted(p)) {
      return;
    }
    e.disallow(
      AsyncPlayerPreLoginEvent.Result.KICK_WHITELIST,
      manager.getNotWhitelistMsg()
    );
  }
}
