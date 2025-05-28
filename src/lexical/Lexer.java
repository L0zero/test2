package lexical;

/**
 * 词法分析程序
 * 与语法分析交互，一次取出一个Token
 * 支持取出下一个Token，预览下一个Token，取出上一个Token，取出当前Token
 */
public interface Lexer {

    /**
     * 取出下一个Token
     * @return 下一个Token，null表示结束
     */
    public Token nextToken();

    /**
     * 取出上一个Token
     * @return 上一个Token，null表示不存在
     */
    public Token previousToken();

    /**
     * 预览下一个Token，不改变当前指针位置
     * @return 下一个Token，null表示结束
     */
    public Token previewToken();

    /**
     * 取出当前Token，不改变当前指针位置
     * @return 当前Token，null表示尚未开始，或者结束
     */
    public Token getToken();

    public LexerMark mark();

    public void back(LexerMark lexerMark);
}
