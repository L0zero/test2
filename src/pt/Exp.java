package pt;

import error.CalculateException;
import ir.IRBlock;
import semantic.Info;
import semantic.SymbolTable;

public class Exp extends Symbol {

    private AddExp exp;

    private DecType type;

    private IdType idType;

    public static Symbol of(SymbolTable table) {
        Exp exp = new Exp();
        Symbol symbol;
        if ((symbol = AddExp.of(table)) == null) {
            return null;
        }
        exp.type = ((AddExp) symbol).getType();
        exp.idType = ((AddExp) symbol).getIdType();
        exp.exp = ((AddExp) symbol);
        exp.add(symbol);
        return exp;
    }

    public AddExp getExp() {
        return exp;
    }

    protected DecType getType() {
        return type;
    }

    protected IdType getIdType() {
        return idType;
    }

    protected int cal() throws CalculateException {
        return exp.cal();
    }

    protected Info visit(IRBlock irBlock) {
        return exp.visit(irBlock);
    }
}
