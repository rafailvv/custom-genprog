package edu.passau.apr.model;

/**
 * Represents a single edit operation in a patch.
 * An edit can be DELETE, INSERT, or REPLACE.
 */
public class Edit {
    public enum Type {
        DELETE,
        INSERT,
        REPLACE
    }

    private final Type type;
    private final int lineNumber;
    private final String content; // For INSERT/REPLACE: the statement to insert/replace with

    public Edit(Type type, int lineNumber, String content) {
        this.type = type;
        this.lineNumber = lineNumber;
        this.content = content;
    }

    public Type getType() {
        return type;
    }

    public int getLineNumber() {
        return lineNumber;
    }

    public String getContent() {
        return content;
    }

    @Override
    public String toString() {
        return String.format("%s at line %d: %s", type, lineNumber, content != null ? content : "");
    }
}

