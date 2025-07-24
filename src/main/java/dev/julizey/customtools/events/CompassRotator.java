package dev.julizey.customtools.events;

import dev.julizey.customtools.CustomTools;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.Directional;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

public class CompassRotator implements Listener {

  @EventHandler(ignoreCancelled = false, priority = EventPriority.HIGHEST)
  public void onPlayerInteract(PlayerInteractEvent event) {
    if (!event.getAction().equals(Action.RIGHT_CLICK_BLOCK)) {
      return;
    }
    if (
      !event.getHand().equals(EquipmentSlot.HAND) &&
      !event.getHand().equals(EquipmentSlot.OFF_HAND)
    ) {
      return;
    }

    Block block = event.getClickedBlock();
    if (block == null) {
      return;
    }

    if (!block.getType().name().contains("GLAZED")) {
      return;
    }

    ItemStack item = event.getPlayer().getInventory().getItemInMainHand();
    if (item.getType() != Material.COMPASS) {
      item = event.getPlayer().getInventory().getItemInOffHand();
      if (item.getType() != Material.COMPASS) {
        return;
      }
    }

    if (!(block.getBlockData() instanceof Directional directional)) {
      return;
    }

    CustomTools.plugin
      .getServer()
      .getScheduler()
      .runTaskLater(
        CustomTools.plugin,
        () -> {
          BlockFace facing = directional.getFacing();
          directional.setFacing(
            switch (facing) {
              case NORTH -> BlockFace.EAST;
              case EAST -> BlockFace.SOUTH;
              case SOUTH -> BlockFace.WEST;
              case WEST -> BlockFace.NORTH;
              default -> facing;
            }
          );
          block.setBlockData(directional);
        },
        0L
      );

    event.setCancelled(true);
  }
}
