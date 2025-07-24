package dev.julizey.customtools.command;

import dev.julizey.customtools.CustomTools;
import dev.julizey.customtools.manager.CooldownManager;
import dev.julizey.customtools.utils.Text;
import java.util.ArrayList;
import org.bukkit.Bukkit;
import org.bukkit.attribute.Attribute;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;

public class GodCommand extends dev.julizey.customtools.utils.Command {

  private final CooldownManager cooldownManager;
  private int duration;
  private long cooldown;

  public GodCommand() {
    super(
      "god",
      new String[] {
        "---------- Help for the NV command ----------",
        "| /god - Toggles god for the player",
        "| /god status - Displays the current Status and cooldown",
        "| /god <other> - Toggles god for the specified player",
        "| /god help - Displays this help message",
        "-----------------------------------------------",
      }
    );
    duration = Math.max(section.getInt("duration", 1800) * 1000, -1);
    cooldown = Math.max(section.getLong("cooldown", 14400) * 1000L, 0);
    cooldownManager = new CooldownManager();
  }

  public void reload() {
    super.reload();
    duration = section.getInt("duration", 14400) * 1000;
    cooldown = section.getLong("cooldown", 43200) * 1000L;
  }

  @Override
  public void execute() {
    if (!isPlayer(sender) || !hasPermission(sender, "customtools.god.use")) {
      sendNoPermissionMessage(sender);
      return;
    }
    if (args.length == 0) {
      toggleGod((Player) sender, null, false);
      return;
    }

    if (args[0].equalsIgnoreCase("help")) {
      sendHelp(sender);
      return;
    }

    if (args[0].equalsIgnoreCase("status")) {
      Text.send(
        sender,
        "messages.god-status",
        new Text.Replaceable(
          "%status%",
          ((Player) sender).isInvulnerable() ? "enabled" : "disabled"
        ),
        new Text.Replaceable("%time%", (duration / 60) + " minutes")
      );
      return;
    }

    if (
      !section.getBoolean("god-other") ||
      !hasPermission(sender, "customtools.god.other")
    ) {
      return;
    }

    if (args[0].equalsIgnoreCase("@a")) {
      for (Player p : Bukkit.getOnlinePlayers()) {
        toggleGod(p, sender, false);
      }
      return;
    }

    Player target = Bukkit.getPlayerExact(args[0]);
    if (target == null) {
      Text.send(
        sender,
        "messages.player-not-found",
        new Text.Replaceable("%player%", args[0])
      );
      return;
    }
    toggleGod(target, sender, false);
    return;
  }

  @Override
  public ArrayList<String> complete() {
    ArrayList<String> tabComplete = new ArrayList<>();

    if (sender.hasPermission("customtools.god.use")) {
      tabComplete.add("help");
      tabComplete.add("status");
    }
    if (sender.hasPermission("customtools.god.other")) {
      tabComplete.add("@a");
      for (Player p : Bukkit.getOnlinePlayers()) {
        tabComplete.add(p.getName());
      }
    }

    if (args.length == 1) {
      return StringUtil.copyPartialMatches(
        args[0],
        tabComplete,
        new ArrayList<>()
      );
    }
    return null;
  }

  private void toggleGod(
    Player player,
    CommandSender sender,
    final boolean keep
  ) {
    if (player == null) {
      return;
    }

    if (cooldownManager.isOnCooldown(player)) {
      Text.send(
        player,
        "messages.on-cooldown",
        new Text.Replaceable("%time%", cooldownManager.getFormatTime(player))
      );
      return;
    }
    cooldownManager.use(player, cooldown);

    final boolean isInvulnerable = player.isInvulnerable();
    if (isInvulnerable) {
      if (keep) {
        return;
      }
      player.setInvulnerable(false);
      CustomTools.plugin.animator.play(player, "god-disable");
    } else {
      if (duration > 0) {
        Bukkit
          .getScheduler()
          .runTaskLater(
            CustomTools.plugin,
            () -> {
              if (player.isInvulnerable()) {
                player.setInvulnerable(false);
                Text.send(player, "messages.god-disabled");
                CustomTools.plugin.animator.play(player, "god-disable");
              }
            },
            duration / 50L
          );
      }
      player.setInvulnerable(true);
      if (section.getBoolean("feed-on-godmode")) {
        player.setFoodLevel(20);
      }
      if (section.getBoolean("heal-on-godmode")) {
        player.setHealth(
          player.getAttribute(Attribute.MAX_HEALTH).getBaseValue()
        );
      }
      CustomTools.plugin.animator.play(player, "god-enable");
    }

    if (sender == null || sender == player) {
      if (isInvulnerable) {
        Text.send(player, "messages.god-disabled");
      } else {
        Text.send(player, "messages.god-enabled");
      }
    } else {
      if (isInvulnerable) {
        Text.send(
          sender,
          "messages.god-disabled-other",
          new Text.Replaceable("%player%", player.getName())
        );
        Text.send(
          player,
          "messages.god-disabled-byOther",
          new Text.Replaceable("%player%", sender.getName())
        );
      } else {
        Text.send(
          sender,
          "messages.god-enabled-other",
          new Text.Replaceable("%player%", player.getName())
        );
        Text.send(
          player,
          "messages.god-enabled-byOther",
          new Text.Replaceable("%player%", sender.getName())
        );
      }
    }
  }
}
