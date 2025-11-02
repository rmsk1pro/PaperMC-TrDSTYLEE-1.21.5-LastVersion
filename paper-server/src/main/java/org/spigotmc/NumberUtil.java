package org.spigotmc;

public class NumberUtil {

    public static String getProgressBar(double current, double max, int totalBars, String symbol, String completedColor, String notCompletedColor) {
        float percent = (float) current / (float) max;
        int progressBars = (int) ((float) totalBars * percent);
        int leftOver = totalBars - progressBars;
        StringBuilder stringBuilder = new StringBuilder();

        stringBuilder.append(TXT.parse(completedColor));

        int index;
        for (index = 0; index < progressBars; ++index) stringBuilder.append(symbol);

        stringBuilder.append(TXT.parse(notCompletedColor));

        for (index = 0; index < leftOver; ++index) stringBuilder.append(symbol);

        return stringBuilder.toString();
    }
}
