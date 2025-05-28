package pt;

import lexical.TokenType;

public class IntConst extends Symbol {

    private int value = 0;

    public static Symbol of() {
        IntConst intConst = new IntConst();
        if (token.getType().equals(TokenType.INTCON)) {
            intConst.add(token);
            intConst.value = Integer.parseInt(token.getValue());
            token = lexer.nextToken();
            return intConst;
        }
        return null;
    }

    @Override
    public String toString() {
        return "";
    }

    protected int getValue() {
        return value;
    }
}
