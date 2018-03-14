package net.minespree.feather.announcement;

import net.minespree.feather.player.NetworkPlayer;
import net.minespree.wizard.util.Chat;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.List;

public class AnnouncementTask implements Runnable {
    private int idx = 0;

    @Override
    public void run() {
        List<Announcement> announcements = AnnouncementManager.getInstance().getAnnouncements();
        if (announcements.isEmpty()) {
            return;
        }
        if (idx >= announcements.size()) {
            idx = 0;
        }

        Announcement announcement = announcements.get(idx);
        for (Player player : Bukkit.getOnlinePlayers()) {
            NetworkPlayer np = NetworkPlayer.of(player);
            if (!np.sendAnnouncements()) continue;

            player.sendMessage("");
            Object msgProduced = announcement.getMessage().toString(player);

            if (msgProduced instanceof String) {
                player.sendMessage(Chat.center((String) msgProduced));
            } else if (msgProduced instanceof List) {
                List<String> s = (List<String>) msgProduced;
                s.forEach(m -> player.sendMessage(Chat.center(m)));
            }

            player.sendMessage("");
        }

        idx++;
    }
}
