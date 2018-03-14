package net.minespree.feather.util;

import org.bukkit.Bukkit;

import java.util.logging.Level;
import java.util.logging.Logger;

public final class LoggingUtils {

    private static final Logger logger = Bukkit.getLogger();

    public static void info(String message, Object... objects) {
        logger.log(Level.INFO, message, objects);
    }

    public static void log(Level level, String message, Object... objects) {
        logger.log(level, message, objects);
    }

}
