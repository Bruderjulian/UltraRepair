package dev.julizey.customtools;

import dev.julizey.customtools.command.*;
import dev.julizey.customtools.manager.*;
import dev.julizey.customtools.utils.Command;
import dev.julizey.customtools.utils.Text;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

public final class CustomTools extends JavaPlugin {

  public static CustomTools plugin;

  public FileConfiguration config;
  public AnimationManager animator;

  @Override
  public void onEnable() {
    plugin = this;
    Text.reload();
    saveDefaultConfig();
    config = getConfig();
    animator = new AnimationManager("effects");

    CostManager.reloadEconomy();
    if (CostManager.economy == null) {
      Text.warn("Vault and/or economy plugin not found!");
    }
    CooldownManager.reload();

    registerCommands("repair", RepairCommand.class, false);
    registerCommands("heal", HealCommand.class, false);
    registerCommands("feed", FeedCommand.class, false);
    registerCommands("nv", NVCommand.class, true);
    registerCommands("god", GodCommand.class, false);
    registerCommands("ec", ECCommand.class, false);
    registerCommands("whitelist", WLCommand.class, true);
    registerCommands("admin", AdminCommand.class, false);

    Text.info(
      "&aCustomTools v" +
      getDescription().getVersion() +
      " by Julizey has been enabled."
    );
  }

  @Override
  public void onDisable() {
    Text.info(
      "&cCustomTools v" +
      getDescription().getVersion() +
      " by Julizey has been disabled."
    );
  }

  private void registerCommands(
    String name,
    Class<? extends Command> Executor,
    boolean isListener
  ) {
    try {
      if (config.getBoolean(name + ".enabled", true) == false) {
        Text.info(name + " command is disabled in config.yml.");
        return;
      }
      Command cls = Executor.getDeclaredConstructor().newInstance();
      PluginCommand command = getCommand(name);
      command.setExecutor(cls);
      command.setTabCompleter(cls);
      if (isListener) {
        getServer().getPluginManager().registerEvents((Listener) cls, this);
      }
    } catch (Exception e) {
      Text.warn(
        "Failed to register " + name + " command. Command will not work!"
      );
      Text.warn("Error: " + e.getMessage() + ", " + e.getCause());
    }
  }

  public void reload(CommandSender sender) {
    if (!sender.hasPermission("customtools.reload")) {
      Text.send(sender, "messages.no-permission");
    }
    Text.reload();
    saveDefaultConfig();
    reloadConfig();
    config = getConfig();
    animator.reload(config);

    try {
      CostManager.reloadEconomy();
      CooldownManager.reload();
    } catch (Exception ex) {
      Text.error(ex, "Failed to reload managers!", true);
    }
    if (sender instanceof Player) {
      Text.send(sender, "messages.reloaded");
    }
    Text.info("messages.reloaded");
    ((RepairCommand) getCommand("repair").getExecutor()).reload();
    ((HealCommand) getCommand("heal").getExecutor()).reload();
    ((FeedCommand) getCommand("feed").getExecutor()).reload();
    ((NVCommand) getCommand("nv").getExecutor()).reload();
    ((GodCommand) getCommand("god").getExecutor()).reload();
    ((ECCommand) getCommand("ec").getExecutor()).reload();
    ((WLCommand) getCommand("whitelist").getExecutor()).reload();
    ((AdminCommand) getCommand("admin").getExecutor()).reload();
  }
}
