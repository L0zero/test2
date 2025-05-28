package io;

import error.CompilerError;
import lexical.Token;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;

public class FileOutputSystem implements OutputSystem {

    private PrintStream printStream;

    private OutputSystem outputSystem;

    public FileOutputSystem(File file) {
        try {
            this.printStream = new PrintStream(new FileOutputStream(file));
        } catch (FileNotFoundException e) {
            printStream = System.err;
            printStream.println("File not found: " + file.getAbsolutePath());
        }
    }

    @Override
    public void child(OutputSystem outputSystem) {
        this.outputSystem = outputSystem;
    }

    @Override
    protected void finalize() throws Throwable {
        if (printStream != System.err) {
            printStream.close();
        }
        super.finalize();
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
        if (outputSystem != null) {
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
        if (outputSystem != null) {
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
        if (outputSystem != null) {
            outputSystem.println(s);
        }
    }

    /**
     * 换行
     */
    @Override
    public void println() {
        printStream.println();
        if (outputSystem != null) {
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
        if (outputSystem != null) {
            outputSystem.Token(token);
        }
    }

    @Override
    public void close() {
        printStream.close();
        if (outputSystem != null) {
            outputSystem.close();
        }
    }
}
