package pt;

import lexical.TokenType;

public class CharConst extends Symbol {

    private int value = 0;

    public static Symbol of() {
        CharConst charConst = new CharConst();
        if (token.getType().equals(TokenType.CHRCON)) {
            charConst.add(token);
            String s = token.getValue();
            s = s.substring(1, s.length() - 1);
            charConst.value = getCharValue(s);
            token = lexer.nextToken();
            return charConst;
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

    private static int getCharValue(String s) {
        if (s.length() == 1) {
            return s.charAt(0);
        } else {
            switch (s.charAt(1)) {
                case 'a':
                    return 7;
                case 'b':
                    return 8;
                case 't':
                    return 9;
                case 'n':
                    return 10;
                case 'v':
                    return 11;
                case 'f':
                    return 12;
                case '\"':
                    return 34;
                case '\'':
                    return 39;
                case '\\':
                    return 92;
                case '0':
                    return 0;
                default:
                    return 0;
            }
        }
    }
}
