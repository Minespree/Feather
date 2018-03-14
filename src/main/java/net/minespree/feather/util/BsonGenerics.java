package net.minespree.feather.util;

import org.bson.Document;
import org.bukkit.Location;
import org.bukkit.World;

import java.util.function.BiFunction;
import java.util.function.Function;

public final class BsonGenerics {

    public static final BiFunction<Document, World, Location> DOCUMENT_TO_LOCATION = (document, world) -> {
        double x = document.getDouble("x");
        double y = document.getDouble("y");
        double z = document.getDouble("z");
        double yaw = document.getDouble("yaw");
        double pitch = document.getDouble("pitch");

        return new Location(world, x, y, z, (float) yaw, (float) pitch);
    };

    public static final Function<Location, Document> LOCATION_TO_DOCUMENT = location -> new Document("x", location.getX())
            .append("y", location.getY())
            .append("z", location.getZ())
            .append("yaw", location.getYaw())
            .append("pitch", location.getPitch())
            .append("world", location.getWorld().getName());

}
