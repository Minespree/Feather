package net.minespree.feather.achievements.impl;

public enum PlayTime {
    ONE(1),
    TEN(10),
    HUNDRED(100),
    THOUSAND(1000);

    private int hours;

    PlayTime(int hours) {
        this.hours = hours;
    }
}
