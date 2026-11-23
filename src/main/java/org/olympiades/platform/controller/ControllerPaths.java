package org.olympiades.platform.controller;

public final class ControllerPaths {

    private ControllerPaths() {
        throw new UnsupportedOperationException("ControllerPaths is a utility class");
    }

    public static final String HEALTH = "/health";
    public static final String GAME = "/game";
    public static final String INFO = "/info";
    public static final String PROFILES = "/profiles";
    public static final String ME = "/me";

    // Identity of the caller, set by the gateway from the verified token
    public static final String USER_ID_HEADER = "X-User-Id";
    public static final String USER_NAME_HEADER = "X-User-Name";
}
