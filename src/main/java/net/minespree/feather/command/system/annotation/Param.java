package net.minespree.feather.command.system.annotation;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface Param {

    @Deprecated
    String name() default "";

    @Deprecated
    boolean wildcard() default false;

    String defaultValue() default "\0";

    String description() default "No description provided";
}
