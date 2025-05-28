package pt;

import error.Logger;
import lexical.Lexer;
import lexical.Token;

import java.util.ArrayList;

public class Symbol implements Ele {

    protected final ArrayList<Symbol> symbols = new ArrayList<>();

    protected final ArrayList<Token> tokens = new ArrayList<>();

    public static Token token = null;

    public static Logger logger = null;

    public static Lexer lexer = null;

    protected final ArrayList<Ele> list = new ArrayList<>();

    protected Symbol() {}

    protected void add(Symbol symbol) {
        this.symbols.add(symbol);
        this.list.add(symbol);
    }

    protected void add(Token token) {
        this.tokens.add(token);
        this.list.add(token);
    }

    public int getLine() {
        if (list.isEmpty()) return 0;
        int len = list.size();
        Ele ele = list.get(len - 1);
        return ele.getLine();
    }

    public ArrayList<Ele> getList() {
        return list;
    }

    public String toString() {
        return "<" + getClass().getName().substring(4) + ">\n";
    }

    protected enum DecType {
        Int, Char, Void
    }

    protected enum IdType {
        Array, Var, Func, String
    }
}
