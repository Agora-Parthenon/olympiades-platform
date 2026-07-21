package org.olympiades.platform.controller;

public final class ControllerPaths {

    private ControllerPaths() {
        throw new UnsupportedOperationException("ControllerPaths is a utility class");
    }

    public static final String HEALTH = "/health";
    public static final String GAME = "/game";
    public static final String INFO = "/info";
}
