package org.olympiades.platform.exception;

/** The submitted profile breaks a rule; {@code code} is a stable identifier the front can rely on. */
public class InvalidProfileException extends RuntimeException {

    private final String code;

    public InvalidProfileException(String code, String message) {
        super(message);
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
