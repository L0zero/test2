package pt;

import ir.IRBlock;
import semantic.SymbolTable;

public class Decl extends Symbol {

    public static Symbol of(String dir, SymbolTable table) {
        Symbol symbol;
        if ((symbol = ConstDecl.of(table)) != null) {
            Decl decl = new Decl();
            decl.add(symbol);
            return decl;
        }
        if ((symbol = VarDecl.of(table)) != null) {
            Decl decl = new Decl();
            decl.add(symbol);
            return decl;
        }
        return null;
    }

    @Override
    public String toString() {
        return "";
    }

    protected void visit(IRBlock irBlock) {
        for (Ele ele : list) {
            if (ele instanceof ConstDecl) {
                ConstDecl constDecl = (ConstDecl) ele;
                constDecl.visit(irBlock);
            } else if (ele instanceof VarDecl) {
                VarDecl varDecl = (VarDecl) ele;
                varDecl.visit(irBlock);
            }
        }
    }

}
