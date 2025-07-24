package dev.julizey.customtools.manager;

import dev.julizey.customtools.CustomTools;
import dev.julizey.customtools.utils.Text;
import java.util.HashMap;
import java.util.Objects;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.RegisteredServiceProvider;

public class CostManager {

  private static final String COST_BYPASS_PERMISSION =
    "customtools.bypass.cost";
  private static final String COST_DISCOUNT_PERMISSION =
    "customtools.discount.";
  public static Economy economy = null;

  private final HashMap<String, Double> costExceptions = new HashMap<>();
  private ConfigurationSection section;
  private double defaultCost;
  private double multiplier;
  private boolean allowDiscount;
  private boolean withCostExceptions;
  private boolean allowBypass = false;

  public CostManager(ConfigurationSection section, boolean withCostExceptions) {
    this.withCostExceptions = withCostExceptions;
    reload(section);
  }

  public static void reloadEconomy() {
    if (!CustomTools.plugin.config.getBoolean("withCosts", true)) {
      return;
    }

    if (
      CustomTools.plugin.getServer().getPluginManager().getPlugin("Vault") ==
      null
    ) {
      economy = null;
      return;
    }
    final RegisteredServiceProvider<Economy> provider = CustomTools.plugin
      .getServer()
      .getServicesManager()
      .getRegistration(Economy.class);
    if (provider == null) {
      economy = null;
      return;
    }
    economy = provider.getProvider();
  }

  public void reload(ConfigurationSection section) {
    this.section = section;
    this.defaultCost = section.getDouble("default-cost", 0.00);
    this.multiplier = section.getDouble("multiplier", 0.00);
    this.allowDiscount = section.getBoolean("allowDiscount", true);
    this.allowBypass =
      CustomTools.plugin.config.getBoolean("allowBypassCost", true);

    costExceptions.clear();
    if (!withCostExceptions) {
      return;
    }
    ConfigurationSection costExceptionsSection = section.getConfigurationSection(
      "cost-exceptions"
    );
    if (costExceptionsSection == null) {
      Text.info("&eMissing Cost exceptions section");
      return;
    }

    for (String key : costExceptionsSection.getKeys(false)) {
      parseException(key);
    }
  }

  public boolean applyCost(Player p, double cost) {
    if (!economy.has(p, cost)) {
      Text.send(
        p,
        Objects
          .requireNonNull("messages.insufficient-funds")
          .replace("%cost%", String.format("%.2f", cost))
      );
      return true;
    }
    economy.withdrawPlayer(p, cost);
    return false;
  }

  public double getCost(Player p, double base, boolean applyDiscount) {
    // Check if the Player name has a cost exception
    double cost = defaultCost;
    if (withCostExceptions && costExceptions.size() != 0) {
      Double exception = costExceptions.get(p.getUniqueId().toString());
      if (exception != null) {
        cost = exception.doubleValue();
      }
    }
    cost = (cost + base) + (cost + base) * multiplier * p.getLevel();
    if (applyDiscount) {
      return cost * getCostDiscount(p);
    }
    return cost;
  }

  @SuppressWarnings("deprecation")
  public double getItemCost(Player p, ItemStack stack, boolean applyDiscount) {
    // Check if the item's material and custom name have a cost exception
    String customName = stack.getItemMeta() != null &&
      stack.getItemMeta().hasDisplayName()
      ? stack.getItemMeta().getDisplayName()
      : null;
    Double cost = costExceptions.get(
      createUniqueKey(stack.getType(), customName)
    );
    if (cost == null) {
      cost = costExceptions.get(stack.getType().name());
    }
    if (cost == null) {
      cost = defaultCost;
    }
    cost =
      (cost + (cost * stack.getDurability() * multiplier)) * stack.getAmount();
    if (applyDiscount) {
      return cost * getCostDiscount(p);
    }
    return cost;
  }

  // calculate the discount percentage
  public double getCostDiscount(Player p) {
    if (!allowDiscount) {
      return 1;
    }
    return p
      .getEffectivePermissions()
      .stream()
      .map(perm -> perm.getPermission())
      .filter(perm -> perm.startsWith(COST_DISCOUNT_PERMISSION))
      .map(perm -> {
        try {
          return 1.0 - Double.parseDouble(perm.substring(21)) / 100.0;
        } catch (NumberFormatException ignored) {
          return 1.0; // Ignore invalid discount permissions
        }
      })
      .max(Double::compare) // Use the highest discount if multiple are present
      .orElse(1.0); // Default to 1.0 (no discount) if none found
  }

  public boolean isBypassingCost(Player p) {
    return allowBypass && p.hasPermission(COST_BYPASS_PERMISSION);
  }

  private void parseException(String key) {
    try {
      Material material = Material.valueOf(key.toUpperCase());
      String customName = section.getString(key + ".custom-name", null);
      String uniqueKey = createUniqueKey(material, customName);
      costExceptions.put(uniqueKey, section.getDouble(key + ".cost"));
    } catch (IllegalArgumentException e) {
      CustomTools.plugin
        .getLogger()
        .warning("Invalid material name in cost exceptions: " + key);
    }
  }

  private static String createUniqueKey(
    org.bukkit.Material material,
    String customName
  ) {
    return material.name() + (customName != null ? ":" + customName : "");
  }
}
