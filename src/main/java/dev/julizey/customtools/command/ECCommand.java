package dev.julizey.customtools.command;

import dev.julizey.customtools.CustomTools;
import dev.julizey.customtools.manager.CooldownManager;
import dev.julizey.customtools.manager.CostManager;
import dev.julizey.customtools.utils.Text;
import java.util.ArrayList;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.StringUtil;

public class ECCommand extends dev.julizey.customtools.utils.Command {

  private final CooldownManager cooldownManager;
  private final CostManager costManager;
  private long cooldown;
  private boolean allowOther;
  private double defaultCost;

  public ECCommand() {
    super(
      "ec",
      new String[] {
        "---------- Help for the Enderchest command ----------",
        "| /ec - Opens the players Enderchest",
        "| /ec status - Displays the current Status of the Enderchest",
        "| /ec <other> -  Opens the Enderchest for the specified player",
        "| /ec help - Displays this help message",
        "-----------------------------------------------",
      }
    );
    this.allowOther = section.getBoolean("allow-other", false);
    this.defaultCost = section.getDouble("default-cost", 0l);
    this.cooldown = Math.max(section.getLong("cooldown", -1) * 1000L, 0);
    this.cooldownManager = new CooldownManager();
    this.costManager = new CostManager(section, true);
  }

  public void reload() {
    super.reload();
    this.allowOther = section.getBoolean("allow-other", false);
    this.defaultCost = section.getDouble("default-cost", 0l);
    this.cooldown = Math.max(section.getLong("cooldown", -1) * 1000L, 0);
    this.costManager.reload(section);
  }

  @Override
  public void execute() {
    if (!isPlayer(sender) || !hasPermission(sender, "customtools.ec.use")) {
      return;
    }
    Player player = (Player) sender;
    if (args.length == 0) {
      CustomTools.plugin.animator.play(player, "ec-self-open");
      openEC(player, player);
      return;
    }

    if (args[0].equalsIgnoreCase("help")) {
      sendHelp(player);
      return;
    }

    if (args[0].equalsIgnoreCase("status")) {
      int count = 0;
      int maxCapacity = 0;
      for (ItemStack stack : player.getEnderChest().getContents()) {
        if (stack != null) {
          count += stack.getAmount();
          maxCapacity += stack.getMaxStackSize();
        } else {
          maxCapacity += 64;
        }
      }

      Text.send(
        player,
        "messages.ec-status",
        new Text.Replaceable("%items%", String.valueOf(count)),
        new Text.Replaceable("%left%", String.valueOf(maxCapacity - count)),
        new Text.Replaceable(
          "%time%",
          this.cooldownManager.getFormatTime(player)
        )
      );
      return;
    }

    if (!allowOther || !hasPermission(player, "customtools.ec.other")) {
      return;
    }

    Player target = Bukkit.getPlayerExact(args[0]);
    if (target == null) {
      Text.send(
        player,
        "messages.player-not-found",
        new Text.Replaceable("%player%", args[0])
      );
      return;
    }
    CustomTools.plugin.animator.play(player, "ec-other-open");
    openEC(player, target);
    return;
  }

  @Override
  public ArrayList<String> complete() {
    ArrayList<String> tabComplete = new ArrayList<>();

    if (sender.hasPermission("customtools.ec.use")) {
      tabComplete.add("help");
      tabComplete.add("status");
    }
    if (sender.hasPermission("customtools.ec.other")) {
      for (Player p : Bukkit.getOnlinePlayers()) {
        tabComplete.add(p.getName());
      }
    }
    if (sender.hasPermission("customtools.reload")) {
      tabComplete.add("reload");
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

  public void openEC(Player sender, Player player) {
    if (player == null || sender == null) {
      return;
    }
    Bukkit
      .getScheduler()
      .runTaskLater(
        CustomTools.plugin,
        new Runnable() {
          @Override
          public void run() {
            if (cooldownManager.isOnCooldown(sender)) {
              Text.send(
                sender,
                "messages.on-cooldown",
                new Text.Replaceable(
                  "%time%",
                  cooldownManager.getFormatTime(sender)
                )
              );
              return;
            }
            if (CostManager.economy != null) {
              costManager.applyCost(
                sender,
                costManager.getCost(sender, defaultCost, true)
              );
            }
            cooldownManager.use(sender, cooldown);
            sender.openInventory(player.getEnderChest());
            Text.send(sender, "messages.ec-opened");
          }
        },
        1
      );
  }
}
