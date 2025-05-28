package io;

/**
 * 标准输入方式
 * 通过标准的输入方法，隔离具体输入方式
 */
public interface InputSystem {
    /**
     * 得到一行，基于评测机要求，以'\n'为截至，返回值不包含'\n'
     * @return 当前行剩余内容，不包括换行符
     */
    public String getLine();

    /**
     * 重新声明默认分割符
     * @param noKeep 跳过的分割符（空白符）
     * @param keep 保留的分割符
     */
    public void setDefault(char[] noKeep, char[] keep);

    /**
     * 得到一个单词，默认以空白符为分割，例如'\n' '\t' '\r' '\v' '\f' ' '
     * @return 以分割符隔开的单词，自动换行，不包括空白符
     */
    public String getWord();

    /**
     * 得到一个单词，但是以输入的字符为分割，默认不包括空白符
     * 在结束时得到null
     * @param noKeepSeparators 作为分割的符号集合，不作为单词输出
     * @param keepSeparators 作为分割的符号集合，作为单词输出
     * @return 以分割符隔开的单词，自动换行
     */
    public String getWord(char[] noKeepSeparators, char[] keepSeparators);

    /**
     * 返回上一个单词，返回null表示前面没有单词，开头使用会导致getWord错误
     * 在下一次getWord之前，不要使用其他方法
     * 下一次getWord会返回当前单词
     * @return 上一个单词
     */
    public String backward();

    /**
     * 返回一个字符，包括空白符
     * @return 得到的字符
     */
    public String getChar();

    /**
     * 得到当前行
     * @return 当前读取的行（尚未读取的部分所在行）
     */
    public int getRow();

    public void mark();

    public void back();
}
