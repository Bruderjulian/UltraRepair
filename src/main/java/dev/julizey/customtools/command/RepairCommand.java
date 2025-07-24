package dev.julizey.customtools.command;

import dev.julizey.customtools.CustomTools;
import dev.julizey.customtools.manager.CooldownManager;
import dev.julizey.customtools.manager.CostManager;
import dev.julizey.customtools.utils.Text;
import java.util.ArrayList;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.StringUtil;

public class RepairCommand extends dev.julizey.customtools.utils.Command {

  private long handCooldown;
  private long allCooldown;
  private CostManager costManager;
  private CooldownManager cooldownManager;

  public RepairCommand() {
    super(
      "repair",
      new String[] {
        "---------- Help for the Repair command ----------",
        "| /repair - Alias for /repair hand",
        "| /repair hand - Repair the item in your hand",
        "| /repair all- Repair all items in your inventory",
        "| /repair status - Displays the current Status and cooldown",
        "| /repair help - Displays this help message",
        "-------------------------------------------------",
      }
    );
    this.handCooldown = Math.max(section.getLong("cooldown.hand") * 1000L, 0);
    this.allCooldown = Math.max(section.getLong("cooldown.all") * 1000L, 0);
    this.costManager = new CostManager(section, true);
    this.cooldownManager = new CooldownManager();
  }

  public void reload() {
    super.reload();
    this.handCooldown = section.getLong("cooldown.hand") * 1000L;
    this.allCooldown = section.getLong("cooldown.all") * 1000L;
    this.costManager.reload(section);
  }

  @Override
  public void execute() {
    if (!isPlayer(sender) || !hasPermission(sender, "customtools.repair.use")) {
      sendNoPermissionMessage(sender);
      return;
    }
    Player player = (Player) sender;
    if (args.length == 0) {
      repairHand(player);
      return;
    }

    switch (args[0].toLowerCase()) {
      case "help" -> sendHelp(sender);
      case "status" -> displayStatus(player);
      case "hand" -> repairHand(player);
      case "all" -> repairAll(player);
      default -> repairHand(player);
    }
    return;
  }

  @SuppressWarnings("deprecation")
  public void displayStatus(Player p) {
    p.closeInventory();

    ItemStack stack = p.getInventory().getItemInMainHand();
    String remainingDurability = "N/A";

    if (stack != null && stack.getType() != Material.AIR) {
      int maxDurability = stack.getType().getMaxDurability();
      if (maxDurability > 0) {
        remainingDurability =
          (maxDurability - stack.getDurability()) + "/" + maxDurability;
      }
    }

    Text.send(
      p,
      "messages.repair-status",
      new Text.Replaceable("%time%", cooldownManager.getFormatTime(p)),
      new Text.Replaceable("%durability%", remainingDurability)
    );
  }

  @Override
  public ArrayList<String> complete() {
    ArrayList<String> tabComplete = new ArrayList<>();

    if (sender.hasPermission("customtools.repair.use")) {
      tabComplete.add("help");
      tabComplete.add("hand");
      tabComplete.add("status");
    }
    if (sender.hasPermission("customtools.repair.all")) {
      tabComplete.add("all");
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

  @SuppressWarnings("deprecation")
  private void repairHand(Player p) {
    p.closeInventory();

    if (cooldownManager.isOnCooldown(p)) {
      Text.send(
        p,
        "messages.on-cooldown",
        new Text.Replaceable("%time%", cooldownManager.getFormatTime(p))
      );
      return;
    }
    cooldownManager.use(p, handCooldown);

    final ItemStack stack = p.getInventory().getItemInMainHand();
    if (!isRepairable(stack)) {
      Text.send(p, "messages.invalid-items");
      return;
    }
    if (CostManager.economy != null) {
      costManager.applyCost(p, costManager.getItemCost(p, stack, true));
    }
    stack.setDurability((short) 0);
    Text.send(p, "messages.repaired");
    CustomTools.plugin.animator.play(p, "repair-hand");
  }

  @SuppressWarnings("deprecation")
  private void repairAll(Player p) {
    if (!hasPermission(p, "customtools.repair.all")) {
      Text.send(p, "messages.no-permission");
      return;
    }
    p.closeInventory();

    if (cooldownManager.isOnCooldown(p)) {
      Text.send(
        p,
        "messages.on-cooldown",
        new Text.Replaceable("%time%", cooldownManager.getFormatTime(p))
      );
      return;
    }
    cooldownManager.use(p, allCooldown);

    boolean hasRepaired = false;
    for (ItemStack stack : p.getInventory().getContents()) {
      if (isRepairable(stack)) {
        stack.setDurability((short) 0);
        hasRepaired = true;
      }
    }

    if (!hasRepaired) {
      Text.send(p, "messages.invalid-items");
      return;
    }
    if (CostManager.economy != null) {
      costManager.applyCost(p, calculateInventoryCost(p));
    }
    Text.send(p, "messages.repaired-all");
    CustomTools.plugin.animator.play(p, "repair-all");
  }

  @SuppressWarnings("deprecation")
  public boolean isRepairable(ItemStack stack) {
    return (
      stack != null &&
      stack.getType() != Material.AIR &&
      !stack.getType().isAir() &&
      !stack.getType().isBlock() &&
      !stack.getType().isEdible() &&
      stack.getType().getMaxDurability() > 0 &&
      stack.getDurability() != 0
    );
  }

  public double calculateInventoryCost(Player p) {
    double cost = 0;
    for (ItemStack stack : p.getInventory().getContents()) {
      if (!isRepairable(stack)) {
        cost += 0;
      } else {
        cost += costManager.getItemCost(p, stack, false);
      }
    }
    return cost * this.costManager.getCostDiscount(p);
  }
}
