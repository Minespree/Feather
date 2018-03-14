package net.minespree.feather.command.system.objects;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.minespree.feather.command.system.annotation.Param;

import java.lang.reflect.Parameter;

public class ParamData {

    private final String name;

    private final boolean wildcard;

    private final boolean varArgs;

    private final String defaultValue;

    private final Class<?> paramaterClass;

    private final String description;

    private final TextComponent usage;

    public ParamData(Parameter parameter) {
        Param annot = parameter.getAnnotation(Param.class);
        String name = annot.name();
        if (name.isEmpty()) {
            name = parameter.getName();
        }
        this.name = name;
        this.wildcard = annot.wildcard() || parameter.isVarArgs();
        this.varArgs = parameter.isVarArgs();
        this.defaultValue = annot.defaultValue();
        this.paramaterClass = parameter.getType();
        this.description = annot.description();
        this.usage = new TextComponent(this.name);
        TextComponent hoverText = new TextComponent(this.description);
        hoverText.setColor(ChatColor.AQUA);
        HoverEvent hover = new HoverEvent(HoverEvent.Action.SHOW_TEXT, new BaseComponent[]{hoverText});
        this.usage.setHoverEvent(hover);
    }

    public String getName() {
        return this.name;
    }

    public boolean isWildcard() {
        return this.wildcard;
    }

    @Deprecated
    public boolean isVarArgs() {
        return this.varArgs;
    }

    public String getDefaultValue() {
        return this.defaultValue;
    }

    public boolean isRequired() {
        return "\0".equals(this.defaultValue);
    }

    public Class<?> getParameterClass() {
        return this.paramaterClass;
    }

    public String getDescription() {
        return this.description;
    }


    public TextComponent getUsage() {
        return this.usage;
    }
}
