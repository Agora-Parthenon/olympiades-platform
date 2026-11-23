package org.olympiades.platform.model;

import java.util.List;

/** The predefined avatars a player can pick from. The front shows one picture per id. */
public final class Avatars {

    public static final List<String> ALL = List.of(
            "fox", "owl", "cat", "bear", "wolf", "panda", "frog", "lion");

    private Avatars() {
        throw new UnsupportedOperationException("Avatars is a utility class");
    }

    public static boolean isValid(String id) {
        return ALL.contains(id);
    }
}
