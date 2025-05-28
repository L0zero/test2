package lexical;

import pt.Ele;

/**
 * 一个标准Token
 */
public class Token implements Ele {

    // 类别码
    private final TokenType type;

    // 单词的字符，字符串，数字
    private final String value;

    private int line;

    /**
     * 声明一个标准的Token
     * @param type Token类别码
     * @param value Token的具体内容
     */
    public Token(TokenType type, String value) {
        this.type = type;
        this.value = value;
    }

    public TokenType getType() {
        return type;
    }

    public String getValue() {
        return value;
    }

    public void setLine(int line) {
        this.line = line;
    }

    public int getLine() {
        return line;
    }

    public Token of() {
        return new Token(type, value);
    }

    @Override
    public String toString() {
        return type + " " + value + "\n";
    }
}
