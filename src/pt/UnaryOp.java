package pt;

import lexical.TokenType;

public class UnaryOp extends Symbol {

    private boolean negated = false;

    private TokenType type = null;

    public static Symbol of() {
        UnaryOp unaryOp = new UnaryOp();
        if (token.getType().equals(TokenType.PLUS) ||
            token.getType().equals(TokenType.MINU) ||
            token.getType().equals(TokenType.NOT)) {
            unaryOp.type = token.getType();
            if (token.getType().equals(TokenType.PLUS)) {
                unaryOp.negated = false;
            } else if (token.getType().equals(TokenType.MINU)) {
                unaryOp.negated = true;
            }
            unaryOp.add(token);
            token = lexer.nextToken();
            return unaryOp;
        }
        return null;
    }

    protected boolean isNegated() {
        return negated;
    }

    protected TokenType getType() {
        return type;
    }
}
