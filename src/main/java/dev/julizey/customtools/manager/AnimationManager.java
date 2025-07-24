package dev.julizey.customtools.manager;

import dev.julizey.customtools.CustomTools;
import dev.julizey.customtools.utils.Text;
import java.io.File;
import java.util.HashMap;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

public class AnimationManager {

  public static final Sound DEFAULT_SOUND = Sound.BLOCK_ANVIL_USE;
  public static final Particle DEFAULT_PARTICLE = Particle.DUST;
  private HashMap<String, Animation> animations;
  private String path;

  public AnimationManager(final String filePath) {
    animations = new HashMap<>();
    this.path = "";
    try {
      final File animationsFile = new File(
        CustomTools.plugin.getDataFolder().getAbsolutePath(),
        filePath + ".yml"
      );
      if (!animationsFile.exists()) {
        CustomTools.plugin.saveResource(filePath + ".yml", false);
      }
      YamlConfiguration config = new YamlConfiguration();
      config.load(animationsFile);
      reload(config);
    } catch (Exception e) {
      Text.warn("Failed to load animations!");
      e.printStackTrace();
    }
  }

  public AnimationManager(final FileConfiguration config, final String path) {
    animations = new HashMap<>();
    if (path == null || path.length() == 0) return;
    this.path = path + ".";
    reload(config);
  }

  public void reload(final FileConfiguration config) {
    animations.clear();
    for (final String key : config.getKeys(false)) {
      final Animation animation = new Animation(
        SoundContainer.fromMultiple(
          config.getConfigurationSection(path + key + ".sounds")
        ),
        ParticleContainer.fromMultiple(
          config.getConfigurationSection(path + key + ".particles")
        )
      );
      animations.put(key, animation);
    }
  }

  public void play(final CommandSender p, final String key) {
    if (!(p instanceof Player)) {
      return;
    }
    play((Player) p, key);
  }

  public void play(final Player p, final String key) {
    if (p == null) return;
    final Animation animation = animations.get(key);
    if (animation == null) return;

    for (final SoundContainer sound : animation.sounds) {
      sound.play(p);
    }
    for (final ParticleContainer particle : animation.particles) {
      particle.play(p);
    }
  }

  public record Animation(
    SoundContainer[] sounds,
    ParticleContainer[] particles
  ) {}

  public static record SoundContainer(Sound type, float pitch, float volume) {
    public static SoundContainer[] fromMultiple(
      final ConfigurationSection section
    ) {
      if (section == null) return new SoundContainer[0];
      final String[] keys = section.getKeys(false).toArray(new String[0]);
      final SoundContainer[] containers = new SoundContainer[keys.length];

      for (int i = 0; i < keys.length; i++) {
        final String key = keys[i];
        if (section.isString(key)) {
          containers[i] = fromConfig(section.getString(key), 1.0f, 1.0f);
        } else {
          containers[i] =
            fromConfig(
              section.getString(key + ".type"),
              (float) section.getDouble(key + ".pitch", 1.0D),
              (float) section.getDouble(key + ".volume", 1.0D)
            );
        }
      }
      return containers;
    }

    @SuppressWarnings("deprecation")
    public static SoundContainer fromConfig(
      final String type,
      float pitch,
      float volume
    ) {
      Sound sound = null;
      if (type == null || type.length() == 0) {
        Text.warn("Invalid sound type: " + type + "! Using Default!");
        sound = DEFAULT_SOUND;
      } else {
        sound = Sound.valueOf(type);
      }
      if (pitch < 0.0F || pitch > 2.0F) {
        Text.warn("Pitch must be between 0.0 and 2.0! Using Default!");
        pitch = 1.0F;
      }
      if (volume < 0.0F || volume > 1.0F) {
        Text.warn("Volume must be between 0.0 and 1.0! Using Default!");
        volume = 1.0F;
      }
      return new SoundContainer(sound, pitch, volume);
    }

    public void play(final Player player) {
      if (type == null) return;
      player.playSound(player.getLocation(), type, volume, pitch);
    }
  }

  public static record ParticleContainer(
    Particle type,
    int amount,
    double offsetX,
    double offsetY,
    double offsetZ,
    double extra
  ) {
    public static ParticleContainer[] fromMultiple(
      final ConfigurationSection section
    ) {
      if (section == null) return new ParticleContainer[0];
      final String[] keys = section.getKeys(false).toArray(new String[0]);
      final ParticleContainer[] containers = new ParticleContainer[keys.length];

      for (int i = 0; i < keys.length; i++) {
        final String key = keys[i];
        if (section.isString(key)) {
          containers[i] =
            fromConfig(section.getString(key), 20, 0.0d, 1.0d, 0.0d, 0.1d);
        } else {
          containers[i] =
            fromConfig(
              section.getString(key + ".type"),
              section.getInt(key + ".amount", 20),
              section.getDouble(key + ".offsetX", 0.0D),
              section.getDouble(key + ".offsetY", 1.0D),
              section.getDouble(key + ".offsetZ", 0.0D),
              section.getDouble(key + ".extra", 0.1D)
            );
        }
      }
      return containers;
    }

    public static ParticleContainer fromConfig(
      final String type,
      int amount,
      final double offsetX,
      final double offsetY,
      final double offsetZ,
      final double extra
    ) {
      Particle particle = null;
      if (type == null) {
        Text.warn("Invalid particle type: " + type + "! Using Default!");
        particle = DEFAULT_PARTICLE;
      } else {
        particle = Particle.valueOf(type);
      }
      if (amount < 0) {
        Text.warn("Amount must be non-negative! Using Default!");
        amount = 4;
      }
      return new ParticleContainer(
        particle,
        amount,
        offsetX,
        offsetY,
        offsetZ,
        extra
      );
    }

    public void play(final Player player) {
      if (type == null) return;
      player
        .getWorld()
        .spawnParticle(
          type,
          player.getLocation(),
          amount,
          offsetX,
          offsetY,
          offsetZ,
          extra
        );
    }
  }
}
