package com.mojang.api.profiles;

public interface ProfileRepository {
    Profile[] findProfilesByNames(String... names);
}
