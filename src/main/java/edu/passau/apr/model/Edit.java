package edu.passau.apr.model;

/**
 * Represents a single edit operation in a patch.
 * An edit can be DELETE, INSERT, SWAP or REPLACE_EXPR.
 *
 * @param donorStatementIndex donor statement index for operations that need donor material
 * @param targetExpressionIndex index of expression inside target statement (for REPLACE_EXPR)
 * @param donorExpressionIndex index of expression inside donor statement (for REPLACE_EXPR)
 */
public record Edit(Type type, int statementIndex, Integer donorStatementIndex,
                   Integer targetExpressionIndex, Integer donorExpressionIndex) {
    public Edit(Type type, int statementIndex, Integer donorStatementIndex) {
        this(type, statementIndex, donorStatementIndex, null, null);
    }

    public enum Type {
        DELETE,
        INSERT,
        SWAP,
        REPLACE_EXPR,
        MUTATE_BINARY_OPERATOR
    }

    @Override
    public String toString() {
        if (type == Type.REPLACE_EXPR) {
            return String.format(
                "%s at statement %d (targetExpr=%s, donorStmt=%s, donorExpr=%s)",
                type,
                statementIndex,
                targetExpressionIndex != null ? targetExpressionIndex : "?",
                donorStatementIndex != null ? donorStatementIndex : "?",
                donorExpressionIndex != null ? donorExpressionIndex : "?"
            );
        }
        if (type == Type.MUTATE_BINARY_OPERATOR) {
            return String.format(
                "%s at statement %d (targetExpr=%s, opCode=%s)",
                type,
                statementIndex,
                targetExpressionIndex != null ? targetExpressionIndex : "?",
                donorExpressionIndex != null ? donorExpressionIndex : "?"
            );
        }
        return String.format(
            "%s at statement %d: %s",
            type,
            statementIndex,
            donorStatementIndex != null ? donorStatementIndex : ""
        );
    }
}
