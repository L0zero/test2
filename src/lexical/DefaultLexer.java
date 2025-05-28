package lexical;

import error.Logger;
import error.LexerError;
import io.InputSystem;

import java.util.ArrayList;

import static lexical.Lexical.*;

public class DefaultLexer implements Lexer {

    private InputSystem inputSystem;

    private Logger logger;

    private int pos = 0;

    private ArrayList<Token> tokens = new ArrayList<>();

    public DefaultLexer(InputSystem inputSystem, Logger logger) {
        this.inputSystem = inputSystem;
        this.logger = logger;
        this.inputSystem.setDefault(NO_KEEP_CHARACTERS, KEEP_CHARACTERS);
    }

    public void changeInputSystem(InputSystem inputSystem) {
        this.inputSystem = inputSystem;
    }

    /**
     * 取出下一个Token
     *
     * @return 下一个Token，null表示结束
     */
    @Override
    public Token nextToken() {
        if (pos >= tokens.size()) {
            tokens.add(getTokenFromInput());
        }
        return tokens.get(pos++);
    }

    /**
     * 取出上一个Token不改变位置
     *
     * @return 上一个Token，null表示不存在
     */
    @Override
    public Token previousToken() {
        if (pos - 2 >= 0 && pos - 2 < tokens.size()) {
            return tokens.get(pos - 2);
        }
        return null;
    }

    /**
     * 预览下一个Token，不改变当前指针位置
     *
     * @return 下一个Token，null表示结束
     */
    @Override
    public Token previewToken() {
        if (pos >= tokens.size()) {
            tokens.add(getTokenFromInput());
        }
        return tokens.get(pos);
    }

    /**
     * 取出当前Token，不改变当前指针位置
     *
     * @return 当前Token，null表示尚未开始，或者结束
     */
    @Override
    public Token getToken() {
        if (pos <= tokens.size() && pos >= 1) {
            return tokens.get(pos - 1);
        }
        return null;
    }

    /**
     * 从输入获取Token，如果结束则返回null
     *
     * @return
     */
    private Token getTokenFromInput() {
        String word = inputSystem.getWord();
        if (word == null || word.isEmpty()) {
            return null;
        }
        StateMachine machine = new StateMachine(inputSystem, logger);
        Token token = machine.distribute(State.START, word);
        if (token == null) {
            return null;
        }
        int row = inputSystem.getRow();
        token.setLine(row);
        // System.out.println(token);
        return token;
    }

    public LexerMark mark() {
        LexerMark mark = new LexerMark();
        mark.loggerMark = logger.mark();
        mark.pos = pos;
        return mark;
    }

    public void back(LexerMark lexerMark) {
        logger.back(lexerMark.loggerMark);
        pos = lexerMark.pos;
    }

}

class StateMachine {

    private InputSystem inputSystem;

    private Logger logger;

    public StateMachine(InputSystem inputSystem, Logger log) {
        this.inputSystem = inputSystem;
        this.logger = log;
    }

    public Token distribute(State state, String word) {
        StringBuilder builder;
        switch (state) {
            case START:
                return start(word);
            case OR:
                word = inputSystem.getWord();
                if (word != null && word.equals("|")) {
                    return new Token(TokenType.OR, "||");
                } else {
                    // TODO
                    logger.log(new LexerError(inputSystem.getRow(), "a"));
                    inputSystem.backward();
                    return new Token(TokenType.OR, "|");
                }
            case AND:
                word = inputSystem.getWord();
                if (word != null && word.equals("&")) {
                    return new Token(TokenType.AND, "&&");
                } else {
                    logger.log(new LexerError(inputSystem.getRow(), "a"));
                    inputSystem.backward();
                    return new Token(TokenType.AND, "&");
                }
            case EQL:
                word = inputSystem.getWord();
                if (word != null && word.equals("=")) {
                    return new Token(TokenType.EQL, "==");
                } else {
                    inputSystem.backward();
                    return new Token(TokenType.ASSIGN, "=");
                }
            case INT:
                for (char c : word.toCharArray()) {
                    if (!Character.isDigit(c)) {
                        // TODO
                        logger.log(new LexerError(inputSystem.getRow(), "a"));
                        return null;
                    }
                }
                return new Token(TokenType.INTCON, word);
            case CHAR:
                builder = new StringBuilder();
                while((word = inputSystem.getWord(new char[]{}, new char[]{'\'', '\\'})) != null) {
                    if (word.equals("'")) {
                        if (builder.length() > 2) {
                            return null;
                        }
                        return new Token(TokenType.CHRCON, "'" + builder + "'");
                    } else if (word.equals("\\")) {
                        word = inputSystem.getChar();
                        builder.append("\\").append(word);
                    } else {
                        builder.append(word);
                    }
                }
                return null;
            case LESS:
                word = inputSystem.getWord();
                if (word != null && word.equals("=")) {
                    return new Token(TokenType.LEQ, "<=");
                } else {
                    inputSystem.backward();
                    return new Token(TokenType.LSS, "<");
                }
            case IDENT:
                for (char c : word.toCharArray()) {
                    if (!Character.isLetter(c) && !Character.isDigit(c) && c != '_') {
                        // TODO
                        logger.log(new LexerError(inputSystem.getRow(), "a"));
                        return null;
                    }
                }
                return new Token(TokenType.IDENFR, word);
            case STRING:
                builder = new StringBuilder();
                while ((word = inputSystem.getWord(new char[]{}, new char[]{'\"', '\\'})) != null) {
                    if (word.equals("\"")) {
                        return new Token(TokenType.STRCON, "\"" + builder +"\"");
                    } else if (word.equals("\\")) {
                        word = inputSystem.getChar();
                        builder.append("\\").append(word);
                    } else {
                        builder.append(word);
                    }
                }
                return new Token(TokenType.STRCON, "\"\"");
            case GREATER:
                word = inputSystem.getWord();
                if (word != null && word.equals("=")) {
                    return new Token(TokenType.GEQ, ">=");
                } else {
                    inputSystem.backward();
                    return new Token(TokenType.GRE, ">");
                }
            case EXCLAMATION_MARK:
                word = inputSystem.getWord();
                if (word != null && word.equals("=")) {
                    return new Token(TokenType.NEQ, "!=");
                } else {
                    inputSystem.backward();
                    return new Token(TokenType.NOT, "!");
                }
            case LEFT_SLASH:
                word = inputSystem.getWord();
                if (word != null && word.equals("*")) {
                    while ((word = inputSystem.getWord(new char[]{}, new char[]{'*'})) != null) {
                        if (word.equals("*")) {
                            inputSystem.backward();
                        }
                        while ((word = inputSystem.getWord()) != null && word.equals("*"))
                            ;
                        if (word != null && word.equals("/")) {
                            word = inputSystem.getWord();
                            if (word != null && !word.isEmpty()) {
                                return distribute(State.START, word);
                            } else {
                                return null;
                            }
                        }
                    }
                } else if (word != null && word.equals("/")) {
                    inputSystem.getLine();
                    word = inputSystem.getWord();
                    if (word != null && !word.isEmpty()) {
                        return distribute(State.START, word);
                    } else {
                        return null;
                    }
                } else {
                    inputSystem.backward();
                    return new Token(TokenType.DIV, "/");
                }
            default:
                // TODO
                logger.log(new LexerError(inputSystem.getRow(), "a"));
                return null;
        }
    }

    private Token start(String word) {
        if (RESERVED_WORDS_MAP.containsKey(word)) {
            return RESERVED_WORDS_MAP.get(word).of();
        } else if (Character.isAlphabetic(word.charAt(0)) || word.charAt(0) == '_') {
            return distribute(State.IDENT, word);
        } else if (Character.isDigit(word.charAt(0))) {
            return distribute(State.INT, word);
        } else {
            switch (word.charAt(0)) {
                case '!':
                    return distribute(State.EXCLAMATION_MARK, word);
                case '&':
                    return distribute(State.AND, word);
                case '|':
                    return distribute(State.OR, word);
                case '<':
                    return distribute(State.LESS, word);
                case '>':
                    return distribute(State.GREATER, word);
                case '=':
                    return distribute(State.EQL, word);
                case '\"':
                    return distribute(State.STRING, word);
                case '\'':
                    return distribute(State.CHAR, word);
                case '/':
                    return distribute(State.LEFT_SLASH, word);
                default:
                    return null;
            }
        }
    }

    private char getEscape(String letter) {
        if (ESCAPED_CHARACTERS_MAP.containsKey(letter)) {
            return ESCAPED_CHARACTERS_MAP.get(letter);
        } else {
            return '\0';
        }
    }
}

enum State {
    START, IDENT, INT, STRING, CHAR, EXCLAMATION_MARK, AND, OR, LESS, GREATER, EQL, LEFT_SLASH
}
