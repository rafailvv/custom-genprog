package edu.passau.apr.model;

import com.github.javaparser.ast.stmt.Statement;

/**
 * Represents a single edit operation in a patch.
 * An edit can be DELETE, INSERT, or REPLACE.
 *
 * @param donorStatementIndex For INSERT/REPLACE: the statement to insert/replace with
 */
public record Edit(Type type, int statementIndex, Integer donorStatementIndex,
                   Integer targetExpressionIndex, Integer donorExpressionIndex) {
    public Edit(Type type, int statementIndex, Integer donorStatementIndex) {
        this(type, statementIndex, donorStatementIndex, null, null);
    }
    public enum Type {
        DELETE,
        INSERT,
        SWAP
    }

    @Override
    public String toString() {
        return String.format("%s at statement %d: %s", type, statementIndex, donorStatementIndex != null ? donorStatementIndex : "");
    }
}

