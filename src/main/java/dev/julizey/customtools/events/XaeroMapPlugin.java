package dev.julizey.customtools.events;

import com.google.common.io.ByteStreams;
import dev.julizey.customtools.CustomTools;
import dev.julizey.customtools.utils.Text;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.util.Random;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerRegisterChannelEvent;

public class XaeroMapPlugin implements Listener {

  private static final String worldmapChannel = "xaeroworldmap:main";
  private static final String minimapChannel = "xaerominimap:main";
  private int serverLevelId;

  public void load() {
    try {
      this.serverLevelId = this.initializeServerLevelId();
      CustomTools.plugin
        .getServer()
        .getMessenger()
        .registerOutgoingPluginChannel(CustomTools.plugin, worldmapChannel);
      CustomTools.plugin
        .getServer()
        .getMessenger()
        .registerOutgoingPluginChannel(CustomTools.plugin, minimapChannel);
    } catch (Exception e) {
      Text.warn("Failed to load Xaero Integration: " + e);
    }
  }

  public void disable() {
    CustomTools.plugin
      .getServer()
      .getMessenger()
      .unregisterOutgoingPluginChannel(CustomTools.plugin);
  }

  // Use PlayerRegisterChannelEvent instead of PlayerLoginEvent because
  // the client mod might not have registered to events on the channel yet
  // so the packet won't get picked up by the mod.
  @EventHandler
  public void onPlayerRegisterChannel(PlayerRegisterChannelEvent event) {
    var channel = event.getChannel();
    if (!channel.equals(worldmapChannel) && !channel.equals(minimapChannel)) {
      return;
    }

    this.sendPlayerWorldId(event.getPlayer(), channel);
  }

  @EventHandler
  public void onPlayerChangedWorld(PlayerChangedWorldEvent event) {
    var player = event.getPlayer();
    this.sendPlayerWorldId(player, worldmapChannel);
    this.sendPlayerWorldId(player, minimapChannel);
  }

  private void sendPlayerWorldId(Player player, String channel) {
    var bytes = ByteStreams.newDataOutput();
    bytes.writeByte(0);
    bytes.writeInt(this.serverLevelId);
    player.sendPluginMessage(CustomTools.plugin, channel, bytes.toByteArray());
  }

  private int initializeServerLevelId() throws Exception {
    String worldFolder = CustomTools.plugin
      .getServer()
      .getWorldContainer()
      .getCanonicalPath();
    File xaeromapFile = new File(worldFolder + File.separator + "xaeromap.txt");
    if (!xaeromapFile.exists()) {
      FileOutputStream xaeromapFileStream = new FileOutputStream(
        xaeromapFile,
        false
      );
      int id = (new Random()).nextInt();
      String idString = "id:" + id;
      xaeromapFileStream.write(idString.getBytes());
      xaeromapFileStream.close();
      return id;
    } else {
      try (
        FileReader fileReader = new FileReader(xaeromapFile);
        BufferedReader bufferedReader = new BufferedReader(fileReader)
      ) {
        String line = bufferedReader.readLine();
        String[] args = line.split(":");
        if (!"id".equals(args[0])) {
          throw new Exception("Failed to read id from xaeromap.txt");
        }
        return Integer.parseInt(args[1]);
      }
    }
  }
}
