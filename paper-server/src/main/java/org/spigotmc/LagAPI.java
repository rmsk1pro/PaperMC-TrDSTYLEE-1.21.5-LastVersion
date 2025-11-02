package org.spigotmc;

import java.text.NumberFormat;
import java.util.Locale;

public class LagAPI implements Runnable {
    public static int TICK_COUNT;
    public static long[] TICKS;
    public static long LAST_TICK;

    static {
        LagAPI.TICK_COUNT = 0;
        LagAPI.TICKS = new long[600];
        LagAPI.LAST_TICK = 0L;
    }

    public static String format(final Double banco) {
        final NumberFormat numberFormat = NumberFormat.getNumberInstance();
        if (banco <= 1.0) {
            return numberFormat.format((double) banco).concat(" ").concat("");
        }
        return numberFormat.format((double) banco).concat(" ").concat("")
            .replace(",", ".").replace(" ", "");
    }

    public static String getTPSCOLOR() {
        final Double tps = getTPS();
        final String t = format(tps);
        if (tps > 15.0) {
            return "§a" + t;
        }
        if (tps <= 15.0 && tps > 10.0) {
            return "§e" + t;
        }
        return "§c" + t;
    }

    public static double getTPS() {
        return getTPS(100);
    }

    public static double getTPS(final int ticks) {
        if (LagAPI.TICK_COUNT < ticks) {
            return 20.0;
        }
        final int target = (LagAPI.TICK_COUNT - 1 - ticks)
            % LagAPI.TICKS.length;
        final long elapsed = System.currentTimeMillis() - LagAPI.TICKS[target];
        return ticks / (elapsed / 1000.0);
    }

    @SuppressWarnings("unused")
    public static long getElapsed(final int tickID) {
        final int length = LagAPI.TICKS.length;
        final long time = LagAPI.TICKS[tickID % LagAPI.TICKS.length];
        return System.currentTimeMillis() - time;
    }

    @Override
    public void run() {
        LagAPI.TICKS[LagAPI.TICK_COUNT % LagAPI.TICKS.length] = System
            .currentTimeMillis();
        ++LagAPI.TICK_COUNT;
    }
}
