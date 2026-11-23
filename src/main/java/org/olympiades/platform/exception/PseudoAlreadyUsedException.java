package org.olympiades.platform.exception;

public class PseudoAlreadyUsedException extends RuntimeException {

    public PseudoAlreadyUsedException(String pseudo) {
        super("Pseudo already used: " + pseudo);
    }
}
