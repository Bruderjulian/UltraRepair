package dev.julizey.customtools.manager;

import dev.julizey.customtools.CustomTools;
import dev.julizey.customtools.utils.TimeUnit;
import java.util.HashMap;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public class CooldownManager {

  private static final String COOLDOWN_BYPASS_PERMISSION =
    "customtools.bypass.cooldown";
  private static boolean allowBypass = false;
  public static boolean disabled = true;
  private final HashMap<Player, Long> cooldowns = new HashMap<>();

  public CooldownManager() {
    reload();
  }

  public static void reload() {
    allowBypass =
      CustomTools.plugin.config.getBoolean("allowBypassCooldown", true);
    disabled = !CustomTools.plugin.config.getBoolean("withCooldowns", true);
  }

  public void use(Player p, long time) {
    if (isBypassingCooldown(p)) {
      return;
    }
    if (time <= 0L) {
      return;
    }
    cooldowns.put(p, System.currentTimeMillis() + time);
    Bukkit
      .getScheduler()
      .runTaskLater(CustomTools.plugin, () -> cooldowns.remove(p), time / 50L);
  }

  public boolean isOnCooldown(Player p) {
    return getRemainingCooldownMs(p) > 0L && !isBypassingCooldown(p);
  }

  public String getFormatTime(Player p) {
    return TimeUnit.format(getRemainingCooldownMs(p), true);
  }

  public long getRemainingCooldownMs(Player p) {
    Long remaining = cooldowns.get(p);
    if (remaining == null) {
      return 0L;
    }
    remaining -= System.currentTimeMillis();
    if (remaining <= 0) {
      cooldowns.remove(p);
      return 0L;
    }
    return remaining;
  }

  public boolean isBypassingCooldown(Player p) {
    return (
      disabled || allowBypass && p.hasPermission(COOLDOWN_BYPASS_PERMISSION)
    );
  }
}
