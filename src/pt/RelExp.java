package pt;

import ir.IRBlock;
import lexical.Token;
import lexical.TokenType;
import semantic.Info;
import semantic.SymbolTable;

public class RelExp extends Symbol {

    protected static Symbol of(SymbolTable table) {
        Symbol symbol, symbol1;
        Symbol relExp = new RelExp();
        if ((symbol = AddExp.of(table)) == null) {
            return null;
        }
        relExp.add(symbol);
        while (token.getType().equals(TokenType.LSS) ||
                token.getType().equals(TokenType.LEQ) ||
                token.getType().equals(TokenType.GRE) ||
                token.getType().equals(TokenType.GEQ)) {
            // TODO 输出Rel输出
            symbol1 = new RelExp();
            symbol1.add(relExp);
            relExp = symbol1;
            relExp.add(token);
            token = lexer.nextToken();
            if ((symbol = AddExp.of(table)) == null) {
                return null;
            }
            relExp.add(symbol);
        }
        return relExp;
    }

    protected Info visit(IRBlock irBlock) {
        if (list.size() == 3) {
            RelExp relExp = (RelExp) list.get(0);
            Token token1 = (Token) list.get(1);
            AddExp addExp = (AddExp) list.get(2);
            String t1 = irBlock.use(relExp.visit(irBlock));
            String t2 = irBlock.use(addExp.visit(irBlock));
            Info info = irBlock.genTempInfo();
            String t3 = irBlock.def(info);
            switch (token1.getType()) {
                case LSS:
                    irBlock.addQuadruple("lss", t1, t2, t3);
                    break;
                case LEQ:
                    irBlock.addQuadruple("leq", t1, t2, t3);
                    break;
                case GRE:
                    irBlock.addQuadruple("gre", t1, t2, t3);
                    break;
                case GEQ:
                    irBlock.addQuadruple("geq", t1, t2, t3);
                    break;
                default:
                    return null;
            }
            return info;
        } else {
            AddExp addExp = (AddExp) list.get(0);
            return addExp.visit(irBlock);
        }
    }
}
