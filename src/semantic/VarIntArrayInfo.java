package semantic;

import pt.AddExp;

import java.util.ArrayList;

public class VarIntArrayInfo extends VarInfo {

    private final ArrayList<AddExp> list;

    private final AddExp size;

    public VarIntArrayInfo(String ident, ArrayList<AddExp> list, AddExp size) {
        super(ident);
        this.list = list;
        this.size = size;
        addDecoration("Int");
        addDecoration("Array");
    }

    public VarIntArrayInfo(String ident) {
        super(ident);
        this.list = null;
        this.size = null;
        addDecoration("Int");
        addDecoration("Array");
    }

    @Override
    public String toString() {
        return "IntArray";
    }

}

