package lexical;

import java.util.HashMap;

public class Lexical {

    public static final char[] KEEP_CHARACTERS = new char[]{'!',
            '&', '|', '+', '-', '*', '/', '%', '<', '>', '=', ';',
            ',', '(', ')', '[', ']', '{', '}', '\'', '\"'};

    public static final char[] NO_KEEP_CHARACTERS = new char[]{' ',
            '\t', '\n', '\r', '\f'};

    public static final String[] RESERVED_WORDS = new String[]{"main",
            "const", "int", "char", "break", "continue", "if", "else",
            "for", "getint", "getchar", "printf", "return", "void"};

    public static final HashMap<String, Token> RESERVED_WORDS_MAP = new HashMap<>();

    public static final HashMap<String, Character> ESCAPED_CHARACTERS_MAP = new HashMap<>();

    static {
        RESERVED_WORDS_MAP.put("main", new Token(TokenType.MAINTK, "main"));
        RESERVED_WORDS_MAP.put("const", new Token(TokenType.CONSTTK, "const"));
        RESERVED_WORDS_MAP.put("int", new Token(TokenType.INTTK, "int"));
        RESERVED_WORDS_MAP.put("char", new Token(TokenType.CHARTK, "char"));
        RESERVED_WORDS_MAP.put("break", new Token(TokenType.BREAKTK, "break"));
        RESERVED_WORDS_MAP.put("continue", new Token(TokenType.CONTINUETK, "continue"));
        RESERVED_WORDS_MAP.put("if", new Token(TokenType.IFTK, "if"));
        RESERVED_WORDS_MAP.put("else", new Token(TokenType.ELSETK, "else"));
        RESERVED_WORDS_MAP.put("for", new Token(TokenType.FORTK, "for"));
        RESERVED_WORDS_MAP.put("getint", new Token(TokenType.GETINTTK, "getint"));
        RESERVED_WORDS_MAP.put("getchar", new Token(TokenType.GETCHARTK, "getchar"));
        RESERVED_WORDS_MAP.put("printf", new Token(TokenType.PRINTFTK, "printf"));
        RESERVED_WORDS_MAP.put("return", new Token(TokenType.RETURNTK, "return"));
        RESERVED_WORDS_MAP.put("void", new Token(TokenType.VOIDTK, "void"));
        RESERVED_WORDS_MAP.put("+", new Token(TokenType.PLUS, "+"));
        RESERVED_WORDS_MAP.put("-", new Token(TokenType.MINU, "-"));
        RESERVED_WORDS_MAP.put("*", new Token(TokenType.MULT, "*"));
        RESERVED_WORDS_MAP.put("%", new Token(TokenType.MOD, "%"));
        RESERVED_WORDS_MAP.put(";", new Token(TokenType.SEMICN, ";"));
        RESERVED_WORDS_MAP.put(",", new Token(TokenType.COMMA, ","));
        RESERVED_WORDS_MAP.put("(", new Token(TokenType.LPARENT, "("));
        RESERVED_WORDS_MAP.put(")", new Token(TokenType.RPARENT, ")"));
        RESERVED_WORDS_MAP.put("[", new Token(TokenType.LBRACK, "["));
        RESERVED_WORDS_MAP.put("]", new Token(TokenType.RBRACK, "]"));
        RESERVED_WORDS_MAP.put("{", new Token(TokenType.LBRACE, "{"));
        RESERVED_WORDS_MAP.put("}", new Token(TokenType.RBRACE, "}"));

        ESCAPED_CHARACTERS_MAP.put("a", (char) 7);
        ESCAPED_CHARACTERS_MAP.put("b", (char) 8);
        ESCAPED_CHARACTERS_MAP.put("t", (char) 9);
        ESCAPED_CHARACTERS_MAP.put("n", (char) 10);
        ESCAPED_CHARACTERS_MAP.put("v", (char) 11);
        ESCAPED_CHARACTERS_MAP.put("f", (char) 12);
        ESCAPED_CHARACTERS_MAP.put("\"", (char) 34);
        ESCAPED_CHARACTERS_MAP.put("'", (char) 39);
        ESCAPED_CHARACTERS_MAP.put("\\", (char) 92);
        ESCAPED_CHARACTERS_MAP.put("\0", (char) 0);
    }
}
