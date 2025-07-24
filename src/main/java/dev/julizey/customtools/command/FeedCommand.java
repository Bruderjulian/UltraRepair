package dev.julizey.customtools.command;

import dev.julizey.customtools.CustomTools;
import dev.julizey.customtools.manager.CooldownManager;
import dev.julizey.customtools.manager.CostManager;
import dev.julizey.customtools.utils.Text;
import java.util.ArrayList;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;

public class FeedCommand extends dev.julizey.customtools.utils.Command {

  private long cooldown;
  private CostManager costManager;
  private CooldownManager cooldownManager;

  public FeedCommand() {
    super(
      "feed",
      new String[] {
        "---------- Help for the Feed command ----------",
        "| /feed - fills the hunger bar",
        "| /feed status - Displays the current Hunger and cooldown",
        "| /feed <other> - feeds the specified player ",
        "| /feed help - Displays this help message",
        "-------------------------------------------------",
      }
    );
    this.cooldown = Math.max(section.getLong("cooldown") * 1000L, 0);
    this.costManager = new CostManager(this.section, false);
    this.cooldownManager = new CooldownManager();
  }

  public void reload() {
    super.reload();
    this.cooldown = section.getLong("cooldown") * 1000L;
    this.costManager.reload(section);
  }

  @Override
  public void execute() {
    if (!isPlayer(sender) || !hasPermission(sender, "customtools.feed.use")) {
      sendNoPermissionMessage(sender);
      return;
    }
    if (args.length == 0) {
      feedPlayer((Player) sender, false);
      return;
    }

    if (args[0].equalsIgnoreCase("help")) {
      sendHelp(sender);
      return;
    }

    if (args[0].equalsIgnoreCase("status")) {
      Player player = (Player) sender;
      Text.send(
        player,
        "messages.feed-status",
        new Text.Replaceable("%hunger%", String.valueOf(player.getFoodLevel())),
        new Text.Replaceable("%time%", cooldownManager.getFormatTime(player))
      );
      return;
    }

    if (
      !section.getBoolean("feed-other") ||
      !hasPermission(sender, "customtools.feed.other")
    ) {
      return;
    }
    Player target = Bukkit.getPlayer(args[0]);
    if (target == null) {
      Text.send(
        sender,
        "messages.player-not-found",
        new Text.Replaceable("%player%", args[0])
      );
      return;
    }
    feedPlayer(target, true);
    return;
  }

  @Override
  public ArrayList<String> complete() {
    ArrayList<String> tabComplete = new ArrayList<>();

    if (sender.hasPermission("customtools.feed.use")) {
      tabComplete.add("help");
      tabComplete.add("status");
    }
    if (sender.hasPermission("customtools.feed.other")) {
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

  public void feedPlayer(Player player, boolean isOtherPlayer) {
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
    if (player.getFoodLevel() >= 20) {
      Text.send(player, "messages.full-hunger");
      return;
    }

    if (CostManager.economy != null) {
      if (costManager.applyCost(player, costManager.getCost(player, 0, true))) {
        return;
      }
    }

    player.setFoodLevel(20);
    player.setSaturation(20F);
    if (isOtherPlayer) {
      Text.send(
        player,
        "messages.feeded-other",
        new Text.Replaceable("%player%", player.getName())
      );
      CustomTools.plugin.animator.play(player, "feed-other");
    } else {
      Text.send(player, "messages.feeded");
      CustomTools.plugin.animator.play(player, "feed-self");
    }
    cooldownManager.use(player, cooldown);
  }
}
