package net.minespree.feather.crates;

import lombok.Getter;
import org.bukkit.ChatColor;

import java.time.LocalDate;
import java.time.Month;
import java.time.Year;
import java.time.temporal.ChronoUnit;

@Getter
public enum SeasonalEvent {
    HALLOWEEN("halloween", ChatColor.GOLD, 21, Month.OCTOBER, 11, Month.NOVEMBER),
    CHIRSTMAS("christmas", ChatColor.RED, 15, Month.DECEMBER, 4, Month.JANUARY);

    private String babel;
    private ChatColor color;
    private LocalDate start;
    private LocalDate end;

    SeasonalEvent(String babel, ChatColor color, int startDay, Month startMonth, int endDay, Month endMonth) {
        int currentYear = Year.now().getValue();

        this.babel = babel;
        this.color = color;
        this.start = LocalDate.of(currentYear, startMonth, startDay);

        if (endMonth.getValue() < startMonth.getValue()) {
            currentYear++;
        }

        this.end = LocalDate.of(currentYear, endMonth, endDay);
    }

    public boolean isActive() {
        LocalDate now = LocalDate.now();

        return now.isAfter(start) && now.isBefore(end);
    }

    public boolean inTimeout(long after) {
        LocalDate now = LocalDate.now();

        return end.plus(after, ChronoUnit.MILLIS).isAfter(now);
    }
}
