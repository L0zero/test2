package pt;

import error.CalculateException;
import semantic.SymbolTable;

public class ConstExp extends Symbol {

    private AddExp exp;

    public static Symbol of(SymbolTable table) {
        Symbol symbol;
        ConstExp constExp = new ConstExp();
        if ((symbol = AddExp.of(table)) == null) {
            return null;
        }
        constExp.exp = (AddExp) symbol;
        constExp.add(symbol);
        return constExp;
    }

    protected AddExp getExp() {
        return exp;
    }

    protected int cal() throws CalculateException {
        return exp.cal();
    }

}
