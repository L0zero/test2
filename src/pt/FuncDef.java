package pt;

import error.ParserError;
import error.SemanticError;
import ir.IRBlock;
import ir.Quadruple;
import lexical.TokenType;
import semantic.*;

public class FuncDef extends Symbol {

    protected String tableDir = "";

    protected String funcName = "";

    public static Symbol of(String dir, SymbolTable table) {
        Symbol symbol;
        FuncDef funcDef = new FuncDef();
        if ((symbol = FuncType.of()) == null) {
            return null;
        }
        DecType decType = ((FuncType) symbol).getType();
        FuncInfo info;
        funcDef.add(symbol);
        if ((symbol = Ident.of()) == null) {
            return null;
        }
        String ident = ((Ident) symbol).getIdent();
        funcDef.funcName = ident;
        if (decType.equals(DecType.Void)) {
            info = new VoidFuncInfo(ident);
        } else if (decType.equals(DecType.Int)) {
            info = new IntFuncInfo(ident);
        } else {
            info = new CharFuncInfo(ident);
        }
        String funcDir = SymbolTable.getDir(dir, SymbolTable.SymbolType.Function, ident);
        SymbolTable funcTable = SemanticAnalyser.newSymbolTable(funcDir);
        funcDef.tableDir = funcDir;
        int line = symbol.getLine();
        funcDef.add(symbol);
        if (!token.getType().equals(TokenType.LPARENT)) {
            return null;
        }
        funcDef.add(token);
        token = lexer.nextToken();
        try {
            SemanticAnalyser.addSymbolToTable(table, ident, info);
        } catch (Exception e) {
            logger.log(new SemanticError(line, "b"));
        }
        if ((symbol = FuncFParams.of(funcTable, info)) != null) {
            funcDef.add(symbol);
            line = symbol.getLine();
        }
        if (!token.getType().equals(TokenType.RPARENT)) {
            logger.log(new ParserError(line, "j"));
        } else {
            funcDef.add(token);
            token = lexer.nextToken();
        }
        if ((symbol = Block.of(funcDir, funcTable, info, false)) == null) {
            return null;
        }
        if (!(info instanceof VoidFuncInfo)) {
            if (!((Block) symbol).isReturned()) {
                logger.log(new SemanticError(((Block) symbol).getTokenLine(), "g"));
            }
        }
        funcDef.add(symbol);
        return funcDef;
    }

    protected void visit(IRBlock irBlock) {
        String ident = ((Ident) list.get(1)).getIdent();
        irBlock.addQuadruple("def", ident);
        if (list.get(3) instanceof FuncFParams) {
            FuncFParams funcFParams = (FuncFParams) list.get(3);
            funcFParams.visit(irBlock);
        }
        irBlock.addQuadruple("be", "fpara");
        IRBlock irBlock1 = new IRBlock(irBlock, tableDir, "func content");
        irBlock.addNextBlock(irBlock1);
        irBlock.setNextBlock(irBlock1);
        Block block = (Block) list.get(list.size() - 1);
        IRBlock newBlock = block.visit(irBlock1, irBlock1, irBlock1);
        newBlock.addQuadruple("ret", "none");
        IRBlock irBlock2 = new IRBlock(irBlock, tableDir, "func exit");
        newBlock.addNextBlock(irBlock2);
        newBlock.setNextBlock(irBlock2);
        irBlock2.addQuadruple("label", "exit@" + funcName);
        irBlock2.addQuadruple("exit");
    }
}
