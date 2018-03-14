package net.minespree.feather.player.nick;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

// OOF this class is meme worthy
public class RandomNameGenerator {
    static List<String> INITIALIZERS = Arrays.asList(
            "Sammy",
            "Gandalf",
            "Mia",
            "Pigman",
            "Super",
            "Gamer",
            "Stone",
            "Lava",
            "Water",
            "Awesome",
            "Banana",
            "Orange",
            "Girl",
            "Cute",
            "Soccer",
            "Football",
            "Basket",
            "Golf",
            "Coolest",
            "Ricky",
            "Skater",
            "Black"
    );

    static List<String> MIDDLE = Arrays.asList(
            "Gamer",
            "Man",
            "Youtube",
            "Twitch",
            "Mixer",
            "Play",
            "Plays",
            "Love",
            "TheGreat",
            "Morty",
            "Minecraft",
            "Dude",
            "warp",
            "Waffle",
            "Galaxy",
            "Meat",
            "Sam",
            "Roblox",
            "Sam",
            "Paul",
            "Lemon",
            "Cake",
            "Fancy",
            "Sick",
            "Memes"
    );

    static String generate() {
        boolean capitalize = ThreadLocalRandom.current().nextBoolean();

        String initializer = INITIALIZERS.get(ThreadLocalRandom.current().nextInt(INITIALIZERS.size()));
        if (!capitalize) initializer = initializer.toLowerCase();
        String middle = MIDDLE.get(ThreadLocalRandom.current().nextInt(MIDDLE.size()));
        if (!capitalize) middle = middle.toLowerCase();

        boolean numbers = ThreadLocalRandom.current().nextBoolean();
        String name = initializer + middle + (numbers ? Integer.toHexString(ThreadLocalRandom.current().nextInt(100)) : "");
        if (name.length() > 16) return generate();
        return name;
    }
}
