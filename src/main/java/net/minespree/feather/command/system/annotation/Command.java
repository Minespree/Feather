package net.minespree.feather.command.system.annotation;

import net.minespree.feather.player.rank.Rank;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface Command {

    String[] names();

    String[] flags() default ("");

    Rank requiredRank();

    boolean async() default false;

    long cooldown() default 0;

    boolean hideFromHelp() default false;
}
