package net.minespree.feather.player.stats.local;

import lombok.Getter;
import lombok.Setter;

import javax.xml.stream.Location;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

public class SessionStatData {
    @Getter
    protected long firstTriggerMs;
    @Getter
    protected long lastUpdatedMs;
    @Getter
    @Setter
    protected Object data;

    public SessionStatData() {
        firstTriggerMs = System.currentTimeMillis();
        update();
    }

    public void update() {
        lastUpdatedMs = System.currentTimeMillis();
    }

    @SuppressWarnings("unchecked")
    public void add(Object object) {
        if (object instanceof Location) {
            if (data == null)
                data = new ArrayList<Location>();
            ((List<Location>) data).add((Location) object);
        } else if (object instanceof Integer) {
            data = data == null ? object : ((int) data) + (int) object;
        } else if (object instanceof Float) {
            data = data == null ? object : ((float) data) + (float) object;
        } else if (object instanceof Double) {
            data = data == null ? object : ((double) data) + (double) object;
        } else {
            data = object;
        }
    }

    public long get() {
        return get(data);
    }

    public long get(Object data) {
        if (data instanceof List) {
            return (long) ((List) data).size();
        } else if (data instanceof Boolean) {
            return ((boolean) data) ? 1L : 0L;
        } else if (data instanceof Integer) {
            return (long) (int) data;
        } else if (data instanceof Long) {
            return (long) data;
        }
        return 0L;
    }

    public long getMsFromStart(long startTimeMs) {
        return firstTriggerMs - startTimeMs;
    }

    public String getTimeFromStart(long startTimeMs) {
        long ms = getMsFromStart(startTimeMs);
        double seconds = ms / 1000.0;
        return new DecimalFormat("#.00").format(seconds);
    }
}
