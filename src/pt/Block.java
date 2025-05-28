package pt;

import ir.IRBlock;
import lexical.TokenType;
import semantic.FuncInfo;
import semantic.SemanticAnalyser;
import semantic.SymbolTable;

public class Block extends Symbol {

    private static int num = 1;

    private boolean returned = false;

    protected String tableDir = "";

    private int line;

    protected static Symbol of(String dir, SymbolTable table, FuncInfo info, boolean loop) {
        Symbol symbol;
        Block block = new Block();
        block.tableDir = dir;
        boolean returned = false;
        if (!token.getType().equals(TokenType.LBRACE)) {
            return null;
        }
        block.add(token);
        token = lexer.nextToken();
        while ((symbol = BlockItem.of(dir, table, info, loop)) != null) {
            block.add(symbol);
            returned = ((BlockItem) symbol).isReturned();
        }
        if (returned) {
            block.returned = true;
        }
        if (!token.getType().equals(TokenType.RBRACE)) {
            return null;
        }
        block.line = token.getLine();
        block.add(token);
        token = lexer.nextToken();
        return block;
    }

    protected static Symbol of(String rootDir, FuncInfo info, boolean loop) {
        if (token.getType().equals(TokenType.LBRACE)) {
            String dir = SymbolTable.getDir(rootDir, SymbolTable.SymbolType.Block, String.valueOf(num++));
            SymbolTable table = SemanticAnalyser.newSymbolTable(dir);
            return of(dir, table, info, loop);
        }
        return null;
    }

    protected boolean isReturned() {
        return returned;
    }

    protected int getTokenLine() {
        return line;
    }

    protected IRBlock visit(IRBlock irBlock, IRBlock continueBlock, IRBlock breakBlock) {
        for (Symbol symbol : symbols) {
            BlockItem blockItem = (BlockItem) symbol;
            irBlock = blockItem.visit(irBlock, continueBlock, breakBlock);
        }
        return irBlock;
    }
}
