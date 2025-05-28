package pt;

import ir.IRBlock;
import lexical.TokenType;
import semantic.Info;
import semantic.SymbolTable;

public class LOrExp extends Symbol {

    protected static Symbol of(SymbolTable table) {
        Symbol symbol, symbol1;
        Symbol lOrExp = new LOrExp();
        if ((symbol = LAndExp.of(table)) == null) {
            return null;
        }
        lOrExp.add(symbol);
        while (token.getType().equals(TokenType.OR)) {
            symbol1 = new LOrExp();
            symbol1.add(lOrExp);
            lOrExp = symbol1;
            lOrExp.add(token);
            token = lexer.nextToken();
            if ((symbol = LAndExp.of(table)) == null) {
                return null;
            }
            lOrExp.add(symbol);
        }
        return lOrExp;
    }

    protected IRBlock visit(IRBlock irBlock) {
        if (list.size() == 3) {
            LOrExp lOrExp = (LOrExp) list.get(0);
            LAndExp lAndExp = (LAndExp) list.get(2);
            irBlock = lOrExp.visit(irBlock);

            IRBlock irBlock1 = new IRBlock(irBlock, irBlock.getTableDir(), "LAnd B1");
            IRBlock irBlock2 = new IRBlock(irBlock, irBlock.getTableDir(), "LAnd B2");
            irBlock.addNextBlock(irBlock1);
            irBlock.addNextBlock(irBlock2);
            irBlock.setNextBlock(irBlock1);
            String t1 = irBlock.use(Info.CondTempInfo);
            String tt1 = irBlock.def(Info.CondTempInfo);

            Info info = irBlock.genTempInfo();
            String t2 = irBlock.def(info);
            irBlock.addQuadruple("not", t1, t2);
            irBlock.addQuadruple("im", t1, tt1);
            irBlock.addQuadruple("bez", t2, irBlock2.getBlockName());
            irBlock1 = lAndExp.visit(irBlock1);

            String t3 = irBlock1.use(Info.CondTempInfo);
            String tt3 = irBlock1.def(Info.CondTempInfo);
            irBlock1.addQuadruple("im", t3, tt3);
            irBlock1.addNextBlock(irBlock2);
            irBlock1.setNextBlock(irBlock2);

            return irBlock2;
        } else {
            LAndExp lAndExp = (LAndExp) list.get(0);
            return lAndExp.visit(irBlock);
        }
    }
}
