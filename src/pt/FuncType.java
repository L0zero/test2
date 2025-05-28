package pt;

import lexical.TokenType;

public class FuncType extends Symbol {

    private DecType type;

    public static Symbol of() {
        FuncType funcType = new FuncType();
        if (token.getType().equals(TokenType.VOIDTK) ||
                token.getType().equals(TokenType.INTTK) ||
                token.getType().equals(TokenType.CHARTK)) {
            if (token.getType().equals(TokenType.VOIDTK)) {
                funcType.type = DecType.Void;
            } else if (token.getType().equals(TokenType.INTTK)) {
                funcType.type = DecType.Int;
            } else if (token.getType().equals(TokenType.CHARTK)) {
                funcType.type = DecType.Char;
            }
            funcType.add(token);
            token = lexer.nextToken();
            return funcType;
        }
        return null;
    }

    public DecType getType() {
        return type;
    }
}
