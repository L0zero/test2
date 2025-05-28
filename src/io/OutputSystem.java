package io;

import error.CompilerError;
import lexical.Token;

import java.util.ArrayList;

public interface OutputSystem {

    public void child(OutputSystem outputSystem);

    /**
     * 打印所有字符串
     * @param s 字符串数组
     */
    public void print(ArrayList<String> s);

    /**
     * 基本打印函数
     * @param s 打印字符串
     */
    public void print(String s);

    /**
     * 换行打印所有字符串
     * @param s 字符串数组
     */
    public void println(ArrayList<String> s);

    /**
     * 基本换行打印函数
     * @param s 打印字符串
     */
    public void println(String s);

    /**
     * 换行
     */
    public void println();

    /**
     * 错误打印函数
     * @param error 编译错误
     */
    public void error(CompilerError error);

    /**
     * Token打印函数
     * @param token Token
     */
    public void Token(Token token);

    public void close();

}
