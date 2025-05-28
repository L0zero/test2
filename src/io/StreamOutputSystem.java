package io;

import error.CompilerError;
import lexical.Token;

import java.io.OutputStream;
import java.io.PrintStream;
import java.util.ArrayList;

/**
 * 默认流输出
 */
public class StreamOutputSystem implements OutputSystem {

    private OutputStream outputStream;

    private PrintStream printStream;

    private OutputSystem outputSystem = null;

    public StreamOutputSystem() {
        this.outputStream = System.out;
        this.printStream = new PrintStream(outputStream);
    }

    public StreamOutputSystem(OutputStream outputStream) {
        this.outputStream = outputStream;
        this.printStream = new PrintStream(outputStream);
    }

    public void changeOutputStream(OutputStream outputStream) {
        this.outputStream = outputStream;
        this.printStream = new PrintStream(outputStream);
    }

    @Override
    public void child(OutputSystem outputSystem) {
        this.outputSystem = outputSystem;
    }

    /**
     * 打印所有字符串
     *
     * @param s 字符串数组
     */
    @Override
    public void print(ArrayList<String> s) {
        for (String str : s) {
            printStream.print(str);
        }
        if (outputSystem != null) {
            outputSystem.print(s);
        }
    }

    /**
     * 基本打印函数
     *
     * @param s 打印字符串
     */
    @Override
    public void print(String s) {
        printStream.print(s);
        if (outputStream != null) {
            outputSystem.print(s);
        }
    }

    /**
     * 换行打印所有字符串
     *
     * @param s 字符串数组
     */
    @Override
    public void println(ArrayList<String> s) {
        for (String str : s) {
            printStream.println(str);
        }
        if (outputStream != null) {
            outputSystem.println(s);
        }
    }

    /**
     * 基本换行打印函数
     *
     * @param s 打印字符串
     */
    @Override
    public void println(String s) {
        printStream.println(s);
        if (outputStream != null) {
            outputSystem.println(s);
        }
    }

    /**
     * 换行
     */
    @Override
    public void println() {
        printStream.println();
        if (outputStream != null) {
            outputSystem.println();
        }
    }

    /**
     * 错误打印函数
     *
     * @param error 编译错误
     */
    @Override
    public void error(CompilerError error) {
        printStream.println(error.getLine() + " " + error.getMessage());
        if (outputSystem != null) {
            outputSystem.error(error);
        }
    }

    /**
     * Token打印函数
     *
     * @param token Token
     */
    @Override
    public void Token(Token token) {
        printStream.println(token.getType().toString() + " " + token.getValue());
        if (outputStream != null) {
            outputSystem.Token(token);
        }
    }

    @Override
    public void close() {
        printStream.close();
    }
}
