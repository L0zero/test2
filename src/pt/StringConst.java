package pt;

import lexical.TokenType;

public class StringConst extends Symbol {

    private String value;

    public static Symbol of() {
        StringConst stringConst = new StringConst();
        if (token.getType().equals(TokenType.STRCON)) {
            stringConst.add(token);
            stringConst.value = token.getValue();
            token = lexer.nextToken();
            return stringConst;
        }
        return null;
    }

    public String getValue() {
        return value;
    }

    @Override
    public String toString() {
        return "";
    }
}
