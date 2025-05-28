package semantic;

import error.NameExistsException;
import io.OutputSystem;
import ir.IRBlock;

public class SemanticAnalyser {

    public static SymbolTable newSymbolTable(String dir) {
        // System.out.println("newSymbolTable: " + dir);
        SymbolTable table = new SymbolTable(dir);
        SymbolTable.addTable(table);
        return table;
    }

    public static void addSymbolToTable(SymbolTable table, String name, Info info) throws NameExistsException {
        table.addSymbol(name, info);
    }

    public static void print(OutputSystem outputSystem) {
        SymbolTable.print(outputSystem);
    }

    public static void printAns(OutputSystem outputSystem) {
        SymbolTable.printAns(outputSystem);
    }
}
