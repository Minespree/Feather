package net.minespree.feather.repository.types;

public class TypeParseException extends Exception {
    public TypeParseException() {
    }

    public TypeParseException(String message) {
        super(message);
    }

    public TypeParseException(String message, Throwable cause) {
        super(message, cause);
    }

    public TypeParseException(Throwable cause) {
        super(cause);
    }
}
