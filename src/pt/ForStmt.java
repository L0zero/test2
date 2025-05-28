package pt;

import error.SemanticError;
import ir.IRBlock;
import ir.Quadruple;
import lexical.TokenType;
import semantic.ConstInfo;
import semantic.Info;
import semantic.SymbolTable;

public class ForStmt extends Symbol {

    public static Symbol of(SymbolTable table) {
        Symbol symbol;
        Symbol forStmt = new ForStmt();
        if ((symbol = LVal.of(table)) == null) {
            return null;
        }
        Info info = table.getInfo(((LVal) symbol).getIdent());
        if (info instanceof ConstInfo) {
            logger.log(new SemanticError(((LVal) symbol).getIdentLine(), "h"));
        }
        forStmt.add(symbol);
        if (!token.getType().equals(TokenType.ASSIGN)) {
            return null;
        }
        forStmt.add(token);
        token = lexer.nextToken();
        if ((symbol = Exp.of(table)) == null) {
            return null;
        }
        forStmt.add(symbol);
        return forStmt;
    }

    protected void visit(IRBlock irBlock) {
        LVal lVal = (LVal) list.get(0);
        Exp exp = (Exp) list.get(2);
        String t1 = irBlock.use(exp.visit(irBlock));
        lVal.assign(irBlock, t1);
    }

}
