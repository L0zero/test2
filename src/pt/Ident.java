package pt;

import lexical.TokenType;

public class Ident extends Symbol {

    private String ident;

    public static Symbol of() {
        Ident ident = new Ident();
        if (token.getType().equals(TokenType.IDENFR)) {
            ident.add(token);
            ident.ident = token.getValue();
            token = lexer.nextToken();
            return ident;
        }
        return null;
    }

    protected String getIdent() {
        return ident;
    }

    @Override
    public String toString() {
        return "";
    }
}
