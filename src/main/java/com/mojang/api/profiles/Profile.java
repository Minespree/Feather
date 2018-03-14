package com.mojang.api.profiles;

import java.util.UUID;

public class Profile {
    private String id;
    private String name;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public UUID getUUID() {
        StringBuilder builder = new StringBuilder(getId());
        builder.insert(8, "-");
        builder.insert(13, "-");
        builder.insert(18, "-");
        builder.insert(23, "-");
        return UUID.fromString(builder.toString());
    }
}
