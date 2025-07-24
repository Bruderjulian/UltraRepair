package dev.julizey.customtools.command;

import dev.julizey.customtools.CustomTools;
import dev.julizey.customtools.manager.CooldownManager;
import dev.julizey.customtools.manager.CostManager;
import dev.julizey.customtools.utils.Text;
import java.util.ArrayList;
import org.bukkit.Bukkit;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;

public class HealCommand extends dev.julizey.customtools.utils.Command {

  private long cooldown;
  private CostManager costManager;
  private CooldownManager cooldownManager;

  public HealCommand() {
    super(
      "heal",
      new String[] {
        "---------- Help for the Heal command ----------",
        "| /heal - Heals the player and fills their hunger bar",
        "| /heal status - Displays the current health and cooldown",
        "| /heal <other> - Heals the specified player ",
        "| /heal help - Displays this help message",
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
    if (!isPlayer(sender) || !hasPermission(sender, "customtools.heal.use")) {
      sendNoPermissionMessage(sender);
      return;
    }
    if (args.length == 0) {
      healPlayer((Player) sender, false);
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
        "messages.heal-status",
        new Text.Replaceable("%health%", String.valueOf(player.getHealth())),
        new Text.Replaceable("%time%", cooldownManager.getFormatTime(player))
      );
      return;
    }

    if (
      !section.getBoolean("heal-other") ||
      !hasPermission(sender, "customtools.heal.other")
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
    healPlayer(target, true);
    return;
  }

  @Override
  public ArrayList<String> complete() {
    ArrayList<String> tabComplete = new ArrayList<>();

    if (sender.hasPermission("customtools.heal.use")) {
      tabComplete.add("help");
      tabComplete.add("status");
    }
    if (sender.hasPermission("customtools.heal.other")) {
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

  public void healPlayer(Player player, boolean isOtherPlayer) {
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
    if (
      player.getHealth() >= player.getAttribute(Attribute.MAX_HEALTH).getValue()
    ) {
      Text.send(player, "messages.full-health");
      return;
    }

    player.setHealth(20.0);
    player.setFoodLevel(20);
    if (isOtherPlayer) {
      Text.send(
        player,
        "messages.healed-other",
        new Text.Replaceable("%player%", player.getName())
      );
      CustomTools.plugin.animator.play(player, "heal-other");
    } else {
      Text.send(player, "messages.healed");
      CustomTools.plugin.animator.play(player, "heal-self");
    }
    if (CostManager.economy != null) {
      costManager.applyCost(player, costManager.getCost(player, 0, true));
    }
    cooldownManager.use(player, cooldown);
  }
}
