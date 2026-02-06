package edu.passau.apr.util;

import com.github.javaparser.ast.expr.Expression;

/**
 * Shared AST predicates used by both patch generation and patch application.
 */
public final class AstUtils {
    private AstUtils() {
    }

    public static boolean isReplaceableExpression(Expression expression) {
        if (expression.isLiteralExpr() || expression.isLambdaExpr()) {
            return false;
        }
        if (expression.isNameExpr() || expression.isThisExpr() || expression.isSuperExpr()) {
            return false;
        }
        if (expression.isAnnotationExpr() || expression.isTypeExpr() || expression.isClassExpr()) {
            return false;
        }
        if (expression.isMethodReferenceExpr() || expression.isArrayInitializerExpr()) {
            return false;
        }
        if (expression.isAssignExpr() || expression.isVariableDeclarationExpr()) {
            return false;
        }

        return expression.isMethodCallExpr()
            || expression.isFieldAccessExpr()
            || expression.isArrayAccessExpr()
            || expression.isBinaryExpr()
            || expression.isUnaryExpr()
            || expression.isEnclosedExpr()
            || expression.isCastExpr()
            || expression.isConditionalExpr()
            || expression.isInstanceOfExpr();
    }

    public static boolean isNegatableExpression(Expression expression) {
        if (expression.isLambdaExpr() || expression.isLiteralExpr()) {
            return false;
        }
        if (expression.isAssignExpr() || expression.isConditionalExpr()) {
            return false;
        }
        if (expression.isArrayInitializerExpr() || expression.isObjectCreationExpr()) {
            return false;
        }
        if (expression.isMethodReferenceExpr()) {
            return false;
        }
        if (expression.isClassExpr() || expression.isTypeExpr()) {
            return false;
        }
        if (expression.getParentNode().isEmpty()
            || !(expression.getParentNode().get() instanceof com.github.javaparser.ast.expr.ConditionalExpr conditional)) {
            return false;
        }

        boolean isConditionalBranch = conditional.getThenExpr() == expression || conditional.getElseExpr() == expression;
        if (!isConditionalBranch) {
            return false;
        }

        return expression.isNameExpr()
            || expression.isFieldAccessExpr()
            || expression.isArrayAccessExpr()
            || expression.isMethodCallExpr()
            || expression.isEnclosedExpr()
            || expression.isCastExpr()
            || expression.isUnaryExpr()
            || expression.isBinaryExpr();
    }
}
