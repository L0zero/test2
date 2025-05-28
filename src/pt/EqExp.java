package pt;

import ir.IRBlock;
import ir.Quadruple;
import lexical.Token;
import lexical.TokenType;
import semantic.Info;
import semantic.SymbolTable;

public class EqExp extends Symbol {

    protected static Symbol of(SymbolTable table) {
        Symbol symbol, symbol1;
        Symbol eqExp = new EqExp();
        if ((symbol = RelExp.of(table)) == null) {
            return null;
        }
        eqExp.add(symbol);
        while (token.getType().equals(TokenType.EQL) ||
                token.getType().equals(TokenType.NEQ)) {
            symbol1 = new EqExp();
            symbol1.add(eqExp);
            eqExp = symbol1;
            eqExp.add(token);
            token = lexer.nextToken();
            if ((symbol = RelExp.of(table)) == null) {
                return null;
            }
            eqExp.add(symbol);
        }
        return eqExp;
    }

    protected Info visit(IRBlock irBlock) {
        if (list.size() == 3) {
            EqExp eqExp = (EqExp) list.get(0);
            RelExp relExp = (RelExp) list.get(2);
            Token token1 = (Token) list.get(1);
            String t1 = irBlock.use(eqExp.visit(irBlock));
            String t2 = irBlock.use(relExp.visit(irBlock));
            Info info = irBlock.genTempInfo();
            String ans = irBlock.def(info);
            if (token1.getType().equals(TokenType.EQL)) {
                irBlock.addQuadruple("eql", t1, t2, ans);
            } else {
                irBlock.addQuadruple("neq", t1, t2, ans);
            }
            return info;
        } else {
            RelExp relExp = (RelExp) list.get(0);
            return relExp.visit(irBlock);
        }
    }
}
