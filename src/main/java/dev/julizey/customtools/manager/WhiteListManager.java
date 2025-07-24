package dev.julizey.customtools.manager;

import dev.julizey.customtools.CustomTools;
import dev.julizey.customtools.utils.Text;
import java.util.ArrayList;
import org.bukkit.configuration.ConfigurationSection;

public class WhiteListManager {

  private ArrayList<String> whitelists;
  private boolean WhitelistEnabled;
  private String notwhitelistedMSG;
  private String caseMode;
  private ConfigurationSection section;

  public WhiteListManager(ConfigurationSection section) {
    this.whitelists = new ArrayList<String>();
    this.section = section;
    reload();
  }

  public void reload() {
    whitelists = new ArrayList<String>(section.getStringList("whitelisted"));

    WhitelistEnabled = section.getBoolean("enabled-whitelist", false);

    if (section.getStringList("not_whitelisted") != null) {
      notwhitelistedMSG =
        String.join("\n", section.getStringList("not_whitelisted"));
    }
    notwhitelistedMSG =
      Text.format(
        notwhitelistedMSG.length() >= 1
          ? notwhitelistedMSG
          : "You are not whitelisted!",
        true,
        false
      );

    caseMode = section.getString("case-mode", "ignore").toLowerCase();
    if (
      !caseMode.equals("ignore") &&
      !caseMode.equals("lower") &&
      !caseMode.equals("upper")
    ) {
      throw new IllegalArgumentException("Invalid case-mode: " + caseMode);
    }
  }

  public void saveWhitelists() {
    section.set("whitelisted", (Object) this.whitelists);
    section.set("enabled-whitelist", (Object) this.isWhitelisting());
    CustomTools.plugin.saveConfig();
  }

  public boolean isWhitelisted(final String name) {
    if (!this.WhitelistEnabled) {
      return true;
    }
    if (name == null || name.length() == 0) {
      return false;
    }
    return this.whitelists.contains(toCase(name));
  }

  public void addWhitelist(String name) {
    name = toCase(name);
    if (this.whitelists.contains(name)) {
      return;
    }
    this.whitelists.add(name);
    this.saveWhitelists();
  }

  public void removeWhitelist(String name) {
    name = toCase(name);
    if (!this.whitelists.contains(name)) {
      return;
    }
    this.whitelists.remove(name);
    this.saveWhitelists();
  }

  public void setWhitelist(final Boolean state) {
    this.WhitelistEnabled = state;
    this.saveWhitelists();
  }

  public ArrayList<String> getWhiteLists() {
    return this.whitelists;
  }

  public boolean isWhitelisting() {
    return this.WhitelistEnabled;
  }

  public String getNotWhitelistMsg() {
    return this.notwhitelistedMSG;
  }

  public String toCase(String str) {
    if (this.caseMode.equals("lower")) {
      return str.toLowerCase();
    }
    if (this.caseMode.equals("upper")) {
      return str.toUpperCase();
    }
    return str;
  }
}
