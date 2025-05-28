package pt;

import ir.IRBlock;
import semantic.FuncInfo;
import semantic.SymbolTable;

public class BlockItem extends Symbol {

    private boolean returned = false;

    protected static Symbol of(String dir, SymbolTable table, FuncInfo info, boolean loop) {
        Symbol symbol;
        BlockItem blockItem = new BlockItem();
        if ((symbol = Decl.of(dir, table)) != null) {
            blockItem.add(symbol);
            return blockItem;
        }
        if ((symbol = Stmt.of(dir, table, info, loop)) != null) {
            blockItem.add(symbol);
            if (((Stmt) symbol).getReturned()) {
                blockItem.returned = true;
            }
            return blockItem;
        }
        return null;
    }

    @Override
    public String toString() {
        return "";
    }

    protected boolean isReturned() {
        return returned;
    }

    protected IRBlock visit(IRBlock irBlock, IRBlock continueBlock, IRBlock breakBlock) {
        Symbol symbol = (Symbol) list.get(0);
        if (symbol instanceof Decl) {
            Decl decl = (Decl) symbol;
            decl.visit(irBlock);
        } else if (symbol instanceof Stmt) {
            Stmt stmt = (Stmt) symbol;
            return stmt.visit(irBlock, continueBlock, breakBlock);
        }
        return irBlock;
    }
}
