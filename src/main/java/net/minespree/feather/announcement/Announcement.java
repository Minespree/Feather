package net.minespree.feather.announcement;

import lombok.Value;
import net.minespree.babel.BabelMessageType;

@Value
public class Announcement {
    private final BabelMessageType message;
}
