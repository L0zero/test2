package io;

import error.BackwardException;

import java.io.*;

public class BufferedInputSystem implements InputSystem {

    private static final char[] BLANK_CHARACTERS = new char[]{' ', '\t', '\n', '\r', '\f'};

    private char[] noKeepSeperators = BLANK_CHARACTERS;

    private char[] markNoKeepSeperators = BLANK_CHARACTERS;

    private char[] keepSeperators = new char[]{};

    private char[] markKeepSeperators = new char[]{};

    protected BufferedReader bufferedReader;

    private StringBuilder stringBuilder = new StringBuilder();

    private StringBuilder markStringBuilder = new StringBuilder();

    private int row = 1;

    private int markRow = 1;

    private boolean update = false;

    private boolean markUpdate = false;

    private String lastWord = null;

    private String markLastWord = null;

    private String presentWord = null;

    private String markPresentWord = null;

    private boolean back = false;

    private boolean markBack = false;

    public BufferedInputSystem(BufferedReader bufferedReader) {
        this.bufferedReader = bufferedReader;
    }

    protected BufferedInputSystem() {

    }

    public void mark() {
        markNoKeepSeperators = noKeepSeperators.clone();
        markKeepSeperators = keepSeperators.clone();
        try {
            bufferedReader.mark(128);
        } catch (IOException e) {
            e.printStackTrace();
        }
        markStringBuilder.setLength(0);
        markStringBuilder.append(stringBuilder);
        markRow = row;
        markUpdate = update;
        markLastWord = lastWord;
        markPresentWord = presentWord;
        markBack = back;
    }

    public void back() {
        noKeepSeperators = markNoKeepSeperators.clone();
        keepSeperators = markKeepSeperators.clone();
        try {
            bufferedReader.reset();
        } catch (IOException e) {
            e.printStackTrace();
        }
        stringBuilder.setLength(0);
        stringBuilder.append(markStringBuilder);
        row = markRow;
        update = markUpdate;
        lastWord = markLastWord;
        presentWord = markPresentWord;
        back = markBack;
    }

    /**
     * 得到一行，基于评测机要求，以'\n'为截至，返回值不包含'\n'
     *
     * @return 当前行剩余内容，不包括换行符
     */
    @Override
    public String getLine() {
        if (back) {
            throw new BackwardException("getLine while still backward");
        }
        try {
            int ch;
            while ((ch = bufferedReader.read()) != -1) {
                char c = (char) ch;
                if (c == '\n') {
                    row++;
                    break;
                } else if (c == '\r') {
                    continue;
                } else {
                    stringBuilder.append(c);
                }
            }
            if (ch == -1) {
                return null;
            }
            return stringBuilder.toString();
        } catch (IOException e) {
            // TODO
            e.printStackTrace();
            return null;
        } finally {
            stringBuilder.setLength(0);
        }
    }

    /**
     * 重新声明默认分割符
     *
     * @param noKeep 跳过的分割符（空白符）
     * @param keep   保留的分割符
     */
    @Override
    public void setDefault(char[] noKeep, char[] keep) {
        this.noKeepSeperators = noKeep;
        this.keepSeperators = keep;
    }

    /**
     * 得到一个单词，默认以空白符为分割，例如'\n' '\t' '\r' '\f' ' '
     *
     * @return 以分割符隔开的单词，自动换行，不包括空白符
     */
    @Override
    public String getWord() {
        return getWord(noKeepSeperators, keepSeperators);
    }

    /**
     * 得到一个单词，但是以输入的字符为分割，默认不包括空白符
     *
     * @param noKeepSeparators 作为分割的符号集合，不作为单词输出
     * @param keepSeparators   作为分割的符号集合，作为单词输出
     * @return 以分割符隔开的单词，自动换行
     */
    @Override
    public String getWord(char[] noKeepSeparators, char[] keepSeparators) {
        if (back) {
            back = false;
            return presentWord;
        }

        try {
            if (stringBuilder.length() > 0) {
                String ans = stringBuilder.toString();
                stringBuilder.setLength(0);
                lastWord = presentWord;
                presentWord = ans;
                return ans;
            }
            int ch;
            boolean start = true;
            while ((ch = bufferedReader.read()) != -1) {
                char c = (char) ch;
                if (update) {
                    row ++;
                    update = false;
                }
                if (contains(c, keepSeparators)) {
                    String ans;
                    if (start) {
                        stringBuilder.append(c);
                        ans = stringBuilder.toString();
                        stringBuilder.setLength(0);

                    } else {
                        ans = stringBuilder.toString();
                        stringBuilder.setLength(0);
                        stringBuilder.append(c);
                    }
                    lastWord = presentWord;
                    presentWord = ans;
                    return ans;
                } else if (contains(c, noKeepSeparators)) {
                    if (start) {
                        if (c == '\n') {
                            row++;
                        }
                        continue;
                    }
                    if (c == '\n') {
                        update = true;
                    }
                    String ans = stringBuilder.toString();
                    stringBuilder.setLength(0);
                    lastWord = presentWord;
                    presentWord = ans;
                    return ans;
                } else {
                    start = false;
                    stringBuilder.append(c);
                }
            }
            if (stringBuilder.length() == 0) {
                return null;
            }
            String ans = stringBuilder.toString();
            stringBuilder.setLength(0);
            lastWord = presentWord;
            presentWord = ans;
            return ans;
        } catch (IOException e) {
            // TODO
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 返回上一个单词
     */
    @Override
    public String backward() {
        if (back) {
            return lastWord;
        } else {
            back = true;
            return lastWord;
        }
    }

    /**
     * 返回一个字符，包括空白符
     *
     * @return 得到的字符
     */
    @Override
    public String getChar() {
        if (back) {
            throw new BackwardException("getChar while still backward");
        }
        try {
            if (stringBuilder.length() > 0) {
                String ans = stringBuilder.toString();
                stringBuilder.setLength(0);
                return ans;
            }
            int ch = bufferedReader.read();
            if (ch == -1) {
                return null;
            }
            char c = (char) ch;
            stringBuilder.append(c);
            String ans = stringBuilder.toString();
            stringBuilder.setLength(0);
            return ans;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 得到当前行
     *
     * @return 当前读取的行（尚未读取的部分所在行）
     */
    @Override
    public int getRow() {
        return row;
    }

    /**
     * 内部方法，判断是否是数组中的一部分
     * @param c 目标元素
     * @param chars 数组
     * @return 是否在数组中
     */
    private boolean contains(char c, char[] chars) {
        for (char ch : chars) {
            if (c == ch) {
                return true;
            }
        }
        return false;
    }

}
