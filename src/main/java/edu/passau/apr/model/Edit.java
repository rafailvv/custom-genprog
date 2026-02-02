package edu.passau.apr.model;

/**
 * Represents a single edit operation in a patch.
 * An edit can be DELETE, INSERT, or REPLACE.
 *
 * @param content For INSERT/REPLACE: the statement to insert/replace with
 */
public record Edit(Type type, int lineNumber, String content) {
    public enum Type {
        DELETE,
        INSERT,
        REPLACE
    }

    @Override
    public String toString() {
        return String.format("%s at line %d: %s", type, lineNumber, content != null ? content : "");
    }
}

