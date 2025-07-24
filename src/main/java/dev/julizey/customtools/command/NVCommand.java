package dev.julizey.customtools.command;

import dev.julizey.customtools.CustomTools;
import dev.julizey.customtools.manager.CooldownManager;
import dev.julizey.customtools.utils.Text;
import java.util.ArrayList;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.StringUtil;

public class NVCommand
  extends dev.julizey.customtools.utils.Command
  implements org.bukkit.event.Listener {

  private final CooldownManager cooldownManager;
  private boolean preventMilk = true;
  private int duration;
  private long cooldown;

  public NVCommand() {
    super(
      "nv",
      new String[] {
        "---------- Help for the NV command ----------",
        "| /nv - Toggles night vision for the player",
        "| /nv status - Displays the current Status and cooldown",
        "| /nv <other> - Toggles night vision for the specified player",
        "| /nv help - Displays this help message",
        "-----------------------------------------------",
      }
    );
    this.preventMilk = section.getBoolean("prevent-milk", true);
    this.duration = Math.max(section.getInt("duration", 14400) * 1000, -1);
    this.cooldown = Math.max(section.getLong("cooldown", 43200) * 1000L, 0);
    this.cooldownManager = new CooldownManager();
  }

  public void reload() {
    super.reload();
    this.preventMilk = section.getBoolean("prevent-milk", true);
    this.duration = section.getInt("duration", 14400) * 1000;
    this.cooldown = section.getLong("cooldown", 43200) * 1000L;
  }

  @Override
  public void execute() {
    if (!isPlayer(sender) || !hasPermission(sender, "customtools.nv.use")) {
      sendNoPermissionMessage(sender);
      return;
    }
    if (args.length == 0) {
      toggleNightVision((Player) sender, null, false);
      return;
    }

    if (args[0].equalsIgnoreCase("help")) {
      sendHelp(sender);
      return;
    }

    if (args[0].equalsIgnoreCase("status")) {
      Text.send(
        sender,
        "messages.nv-status",
        new Text.Replaceable(
          "%status%",
          ((Player) sender).hasPotionEffect(PotionEffectType.NIGHT_VISION)
            ? "enabled"
            : "disabled"
        ),
        new Text.Replaceable("%time%", (duration / 60) + " minutes")
      );
      return;
    }

    if (
      !section.getBoolean("nv-other") ||
      !hasPermission(sender, "customtools.nv.other")
    ) {
      return;
    }

    if (args[0].equalsIgnoreCase("@a")) {
      for (Player p : Bukkit.getOnlinePlayers()) {
        toggleNightVision(p, sender, false);
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
    toggleNightVision(target, sender, false);
    return;
  }

  @Override
  public ArrayList<String> complete() {
    ArrayList<String> tabComplete = new ArrayList<>();

    if (sender.hasPermission("customtools.nv.use")) {
      tabComplete.add("help");
      tabComplete.add("status");
    }
    if (sender.hasPermission("customtools.nv.other")) {
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

  @EventHandler
  public void onRespawn(PlayerRespawnEvent event) {
    Bukkit
      .getScheduler()
      .scheduleSyncDelayedTask(
        CustomTools.plugin,
        new Runnable() {
          public void run() {
            toggleNightVision(event.getPlayer(), null, true);
          }
        },
        4
      );
  }

  @EventHandler
  public void onPlayerConsume(PlayerItemConsumeEvent e) {
    ItemStack item = e.getItem();
    if (item == null) {
      return;
    }
    if (
      preventMilk &&
      item.getType() == Material.MILK_BUCKET &&
      e.getPlayer().hasPotionEffect(PotionEffectType.NIGHT_VISION)
    ) {
      Bukkit
        .getScheduler()
        .runTaskLater(
          CustomTools.plugin,
          () -> {
            e.getPlayer().sendMessage("messages.nv-milk");
            toggleNightVision(e.getPlayer(), null, true);
          },
          6
        );
      return;
    }
  }

  private void toggleNightVision(
    Player player,
    CommandSender sender,
    final boolean keepEffect
  ) {
    if (player == null) {
      return;
    }
    final boolean hasEffect = player.hasPotionEffect(
      PotionEffectType.NIGHT_VISION
    );

    if (cooldownManager.isOnCooldown(player)) {
      Text.send(
        player,
        "messages.on-cooldown",
        new Text.Replaceable("%time%", cooldownManager.getFormatTime(player))
      );
      return;
    }
    cooldownManager.use(player, cooldown);

    if (hasEffect) {
      if (keepEffect) {
        return;
      }
      player.removePotionEffect(PotionEffectType.NIGHT_VISION);
      Text.send(sender, "messages.nv-disabled");
      CustomTools.plugin.animator.play(player, "nv-disable");
    }

    player.addPotionEffect(
      new PotionEffect(PotionEffectType.NIGHT_VISION, duration, 255)
    );
    Text.send(sender, "messages.nv-enabled");
    CustomTools.plugin.animator.play(player, "nv-enable");
    if (sender == null || sender == player) {
      return;
    }
    if (hasEffect) {
      Text.send(
        sender,
        "messages.nv-disabled-other",
        new Text.Replaceable("%player%", player.getName())
      );
      Text.send(
        player,
        "messages.nv-disabled-byOther",
        new Text.Replaceable("%player%", sender.getName())
      );
    } else {
      Text.send(
        sender,
        "messages.nv-enabled-other",
        new Text.Replaceable("%player%", player.getName())
      );
      Text.send(
        player,
        "messages.nv-enabled-byOther",
        new Text.Replaceable("%player%", sender.getName())
      );
    }
  }
}
