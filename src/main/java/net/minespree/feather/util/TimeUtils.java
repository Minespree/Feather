package net.minespree.feather.util;

import java.util.Calendar;
import java.util.GregorianCalendar;

public final class TimeUtils {
    private static final String[] MONTHS = new String[]{"January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December"};

    public static boolean elapsed(long from, long required) {
        return System.currentTimeMillis() - from >= required;
    }

    public static String formatTime(int secs) {
        int remainder = secs % 86400;

        int days = secs / 86400;
        int hours = remainder / 3600;
        int minutes = (remainder / 60) - (hours * 60);
        int seconds = (remainder % 3600) - (minutes * 60);

        if (days > 0) {
            return days + "d " + hours + "h " + minutes + "m " + seconds + "s";
        } else if (hours > 0) {
            return hours + "h " + minutes + "m " + seconds + "s";
        } else if (minutes > 0) {
            return minutes + "m " + seconds + "s";
        } else {
            return String.valueOf(seconds + "s");
        }
    }

    public static long getTimeInMS(String time, String unit) {
        long sec;
        String newUnit = unit.toLowerCase();

        try {
            sec = Integer.parseInt(time) * 60;
        } catch (NumberFormatException ex) {
            return (0);
        }

        if (newUnit.startsWith("hour") || newUnit.startsWith("h")) {
            sec *= 60;
        } else if (newUnit.startsWith("day") || newUnit.startsWith("d")) {
            sec *= (60 * 24);
        } else if (newUnit.startsWith("week") || newUnit.startsWith("w")) {
            sec *= (7 * 60 * 24);
        } else if (newUnit.startsWith("month") || newUnit.startsWith("M")) {
            sec *= (30 * 60 * 24);
        } else if (newUnit.startsWith("min") || newUnit.startsWith("m")) {
            sec *= (1);
        } else if (newUnit.startsWith("sec") || newUnit.startsWith("s")) {
            sec /= 60;
        }

        return (sec) * 1000;
    }

    public static int getDay() {
        Calendar cal = new GregorianCalendar();
        return cal.get(Calendar.DAY_OF_YEAR);
    }

    public static int getWeek() {
        Calendar cal = new GregorianCalendar();
        return cal.get(Calendar.WEEK_OF_YEAR);
    }

    public static String getMonth() {
        Calendar cal = new GregorianCalendar();
        return MONTHS[cal.get(Calendar.MONTH)];
    }
}
