package net.minespree.feather.util;

import java.util.NavigableMap;
import java.util.TreeMap;

public final class RomanNumeral {
    private static final NavigableMap<Integer, String> MAP = new TreeMap<Integer, String>() {{
        put(1000, "M");
        put(900, "CM");
        put(500, "D");
        put(400, "CD");
        put(100, "C");
        put(50, "L");
        put(40, "XL");
        put(10, "X");
        put(9, "IX");
        put(5, "V");
        put(4, "IV");
        put(1, "I");
    }};

    public static String toRoman(int arabic) {
        int index = MAP.floorKey(arabic);
        if (arabic == index) {
            return MAP.get(arabic);
        }

        return MAP.get(index) + toRoman(arabic - index);
    }

}
