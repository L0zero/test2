package pt;

import lexical.Token;
import lexical.TokenType;

public class BType extends Symbol {

    private DecType type;

    public static Symbol of() {
        Token oldToken = null;
        BType bType = new BType();
        if (token.getType().equals(TokenType.INTTK)) {
            bType.type = DecType.Int;
            oldToken = token;
            token = lexer.nextToken();
        } else if (token.getType().equals(TokenType.CHARTK)) {
            bType.type = DecType.Char;
            oldToken = token;
            token = lexer.nextToken();
        }
        if (oldToken != null) {
            bType.add(oldToken);
            return bType;
        }
        return null;
    }

    protected DecType getType() {
        return type;
    }

    @Override
    public String toString() {
        return "";
    }
}
