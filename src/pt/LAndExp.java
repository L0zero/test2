package pt;

import ir.IRBlock;
import ir.Quadruple;
import lexical.TokenType;
import semantic.Info;
import semantic.SymbolTable;

public class LAndExp extends Symbol {

    protected static Symbol of(SymbolTable table) {
        Symbol symbol, symbol1;
        Symbol lAndExp = new LAndExp();
        if ((symbol = EqExp.of(table)) == null) {
            return null;
        }
        lAndExp.add(symbol);
        while (token.getType().equals(TokenType.AND)) {
            symbol1 = new LAndExp();
            symbol1.add(lAndExp);
            lAndExp = symbol1;
            lAndExp.add(token);
            token = lexer.nextToken();
            if ((symbol = EqExp.of(table)) == null) {
                return null;
            }
            lAndExp.add(symbol);
        }
        return lAndExp;
    }

    protected IRBlock visit(IRBlock irBlock) {
        if (list.size() == 3) {
            LAndExp lAndExp = (LAndExp) list.get(0);
            EqExp eqExp = (EqExp) list.get(2);
            irBlock = lAndExp.visit(irBlock);

            IRBlock irBlock1 = new IRBlock(irBlock, irBlock.getTableDir(), "LAnd B1");
            IRBlock irBlock2 = new IRBlock(irBlock, irBlock.getTableDir(), "LAnd B2");
            irBlock.addNextBlock(irBlock1);
            irBlock.addNextBlock(irBlock2);
            irBlock.setNextBlock(irBlock1);
            String t1 = irBlock.use(Info.CondTempInfo);
            String tt1 = irBlock.def(Info.CondTempInfo);
            irBlock.addQuadruple("im", t1, tt1);
            irBlock.addQuadruple("bez", t1, "", irBlock2.getBlockName());
            String t2 = irBlock1.use(eqExp.visit(irBlock1));
            String tt2 = irBlock1.def(Info.CondTempInfo);
            irBlock1.addQuadruple("im", t2, tt2);
            irBlock1.addNextBlock(irBlock2);
            irBlock1.setNextBlock(irBlock2);
            return irBlock2;
        } else {
            EqExp eqExp = (EqExp) list.get(0);
            String t = irBlock.def(Info.CondTempInfo);
            String t1 = irBlock.use(eqExp.visit(irBlock));
            irBlock.addQuadruple("im", t1, t);
            return irBlock;
        }
    }
}
