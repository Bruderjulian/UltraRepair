package dev.julizey.customtools.utils;

import dev.julizey.customtools.CustomTools;
import java.util.ArrayList;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

public abstract class Command implements TabExecutor {

  private final String[] helpStrings;
  private final String sectionName;
  protected ConfigurationSection section;

  protected String[] args;
  protected CommandSender sender;

  public static String instanceName = "customtools";

  public Command(String sectionName, String[] help) {
    this.helpStrings = help;
    this.sectionName = sectionName;
    this.section =
      CustomTools.plugin.config.getConfigurationSection(sectionName);
    if (this.section == null) {
      throw new NullPointerException(
        "Section " + sectionName + " not found in config.yml"
      );
    }
  }

  public boolean onCommand(
    CommandSender sender,
    org.bukkit.command.Command command,
    String commandLabel,
    String[] args
  ) {
    this.sender = sender;
    this.args = args;
    try {
      Bukkit
        .getScheduler()
        .runTaskAsynchronously(
          CustomTools.plugin,
          new Runnable() {
            public void run() {
              execute();
            }
          }
        );
    } catch (final Exception ex) {
      Text.warn("A command failed!");
      Text.warn(ex.getMessage());
      Text.warn("Stack trace:");
      ex.printStackTrace();
    }
    return true;
  }

  public ArrayList<String> onTabComplete(
    CommandSender sender,
    org.bukkit.command.Command command,
    String label,
    String[] args
  ) {
    this.sender = sender;
    this.args = args;
    try {
      Bukkit
        .getScheduler()
        .runTaskAsynchronously(
          CustomTools.plugin,
          new Runnable() {
            public void run() {
              complete();
            }
          }
        );
    } catch (final Exception ex) {
      Text.warn("A tabComplete failed!");
      Text.warn(ex.getMessage());
      Text.warn("Stack trace:");
      ex.printStackTrace();
    }
    return null;
  }

  protected abstract void execute();

  protected abstract ArrayList<String> complete();

  protected static boolean hasPermission(
    CommandSender sender,
    String permission
  ) {
    if (sender.hasPermission(permission)) {
      return true;
    }
    Text.send(sender, "messages.no-permission");
    return false;
  }

  protected static boolean isPlayer(CommandSender sender) {
    if (sender instanceof Player) {
      return true;
    } else {
      Text.info("messages.commands.must-be-player");
      return false;
    }
  }

  protected static void sendNoPermissionMessage(CommandSender sender) {
    Text.send(sender, "messages.no-permission");
  }

  protected void sendHelp(CommandSender sender) {
    for (String str : helpStrings) {
      Text.send(sender, str);
    }
  }

  protected void reload() {
    this.section =
      CustomTools.plugin.config.getConfigurationSection(sectionName);
    if (this.section == null) {
      throw new NullPointerException(
        "Section " + sectionName + " not found in config.yml"
      );
    }
  }

  protected boolean checkArgs(int minLength) {
    if (args.length < minLength) {
      Text.send(sender, "messages.commands.wrong-arguments");
      return false;
    }
    return true;
  }
}
