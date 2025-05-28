package semantic;

import ir.IRBlock;

import java.util.ArrayList;

public class Info implements Cloneable {

    // Variable Ident
    private final String ident;

    // For Generate SSA VarIdent
    private static int count = 0;

    private int len = 0;

    // Saves Ident Type Such As
    // (Func, Var, FParam)
    // (Array)
    // (Const)
    // (Global)
    // (GlobalString)
    // (Int Char String Void)
    private final ArrayList<String> decorations = new ArrayList<>();

    /**
     * New Info
     * @param ident
     */
    public Info(String ident) {
        this.ident = ident;
    }

    public String getIdent() {
        return ident;
    }

    private String varIdent = null;

    /**
     * Generate Var Ident For SSA Only Used In IRBlock
     * @return
     */
    public String getVarIdent() {
        if (varIdent == null) {
            if (decorations.contains("Global")) {
                varIdent = "Global@" + ident + "@" + count++;
            } else if (decorations.contains("Cond")) {
                varIdent = ident;
            } else if (decorations.contains("Temp")) {
                varIdent = ident;
            } else if (decorations.contains("GlobalString")) {
                varIdent = ident;
            } else {
                varIdent = "Var@" + ident + "@" + count++;
            }
        }
        return varIdent;
    }

    public String genSSA() {
        if (varIdent == null) {
            getVarIdent();
        }
        return varIdent + "@" + count++;
    }

    public void addDecoration(String decoration) {
        decorations.add(decoration);
    }

    public ArrayList<String> getDecorations() {
        return (ArrayList<String>) decorations.clone();
    }

    public void setLen(int len) {
        this.len = len;
    }

    public int getLen() {
        return len;
    }

    private final ArrayList<IRBlock> defs = new ArrayList<>();

    public void def(IRBlock block) {
        if (defs.contains(block)) {
            return;
        }
        defs.add(block);
    }

    public ArrayList<IRBlock> getDefs() {
        return defs;
    }

    @Override
    public String toString() {
        return "";
    }

    public static final Info CondTempInfo = new CondInfo("Cond@Global");
}
