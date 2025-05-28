package pt;

import ir.IRBlock;
import semantic.Info;
import semantic.SymbolTable;

public class Cond extends Symbol {

    protected static Symbol of(SymbolTable table) {
        Symbol cond = new Cond();
        Symbol symbol;
        if ((symbol = LOrExp.of(table)) == null) {
            return null;
        }
        cond.add(symbol);
        return cond;
    }

    protected IRBlock visit(IRBlock irBlock) {
        LOrExp lOrExp = (LOrExp) list.get(0);
        irBlock = lOrExp.visit(irBlock);
        return irBlock;
    }
}
