package dev.julizey.customtools.utils;

import java.time.temporal.ChronoUnit;

public final class TimeUnit {

  public static final TimeUnit[] UNITS = new TimeUnit[] {
    new TimeUnit(ChronoUnit.YEARS),
    new TimeUnit(ChronoUnit.MONTHS),
    new TimeUnit(ChronoUnit.WEEKS),
    new TimeUnit(ChronoUnit.DAYS),
    new TimeUnit(ChronoUnit.HOURS),
    new TimeUnit(ChronoUnit.MINUTES),
    new TimeUnit(ChronoUnit.SECONDS),
  };
  public final long duration;
  private final String formalStringPlural;
  private final String formalStringSingular;
  private final String conciseString;

  TimeUnit(final ChronoUnit unit) {
    this.duration = unit.getDuration().getSeconds();
    this.formalStringPlural = " " + unit.name().toLowerCase();
    this.formalStringSingular =
      " " + unit.name().substring(0, unit.name().length() - 1).toLowerCase();
    this.conciseString =
      String.valueOf(Character.toLowerCase(unit.name().charAt(0)));
  }

  public String toString(final boolean concise, final long n) {
    if (concise) {
      return this.conciseString;
    }
    return n == 1 ? this.formalStringSingular : this.formalStringPlural;
  }

  public static String format(long duration, final boolean concise) {
    return format(java.time.Duration.ofMillis(duration), concise);
  }

  public static String format(
    final java.time.Duration duration,
    final boolean concise
  ) {
    long seconds = duration.getSeconds();
    final StringBuilder output = new StringBuilder();
    int outputSize = 0;

    for (final TimeUnit unit : TimeUnit.UNITS) {
      final long n = seconds / unit.duration;
      if (n > 0) {
        seconds -= unit.duration * n;
        output.append(' ').append(n).append(unit.toString(concise, n));
        outputSize++;
      }
      if (seconds <= 0 || outputSize >= Integer.MAX_VALUE) {
        break;
      }
    }

    if (output.length() == 0) {
      return (
        "0" + (TimeUnit.UNITS[TimeUnit.UNITS.length - 1].toString(concise, 0))
      );
    }
    return output.substring(1);
  }
}
