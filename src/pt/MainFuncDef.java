package pt;

import error.ParserError;
import error.SemanticError;
import ir.IRBlock;
import ir.Quadruple;
import lexical.TokenType;
import semantic.IntFuncInfo;
import semantic.SymbolTable;

public class MainFuncDef extends Symbol {

    protected String tableDir = "";

    public static Symbol of(String dir, SymbolTable table) {
        Symbol symbol;
        MainFuncDef mainFuncDef = new MainFuncDef();
        if (!token.getType().equals(TokenType.INTTK)) {
            return null;
        }
        mainFuncDef.add(token);
        token = lexer.nextToken();
        if (!token.getType().equals(TokenType.MAINTK)) {
            return null;
        }
        mainFuncDef.add(token);
        token = lexer.nextToken();
        if (!token.getType().equals(TokenType.LPARENT)) {
            return null;
        }
        mainFuncDef.add(token);
        int line = token.getLine();
        token = lexer.nextToken();
        if (!token.getType().equals(TokenType.RPARENT)) {
            logger.log(new ParserError(line, "j"));
        } else {
            mainFuncDef.add(token);
            token = lexer.nextToken();
        }
        mainFuncDef.tableDir = dir;
        if ((symbol = Block.of(dir, table, new IntFuncInfo("main"), false)) == null) {
            return null;
        }
        if (!((Block) symbol).isReturned()) {
            logger.log(new SemanticError(((Block) symbol).getTokenLine(), "g"));
        }
        mainFuncDef.add(symbol);
        return mainFuncDef;
    }

    protected void visit(IRBlock irBlock) {
        irBlock.addQuadruple("def", "main");
        irBlock.addQuadruple("be", "fpara");
        IRBlock irBlock1 = new IRBlock(irBlock, tableDir, "main func content");
        irBlock.addNextBlock(irBlock1);
        irBlock.setNextBlock(irBlock1);
        Block block = (Block) list.get(list.size() - 1);
        IRBlock irBlock2 = block.visit(irBlock1, irBlock1, irBlock1);
        irBlock2.addQuadruple("ret", "none");
        IRBlock newBlock = new IRBlock(irBlock1, tableDir, "main func exit");
        irBlock2.addNextBlock(newBlock);
        irBlock2.setNextBlock(newBlock);
        newBlock.addQuadruple("label", "exit@main");
        newBlock.addQuadruple("exit");
    }
}
