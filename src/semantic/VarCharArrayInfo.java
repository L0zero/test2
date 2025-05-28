package semantic;

import pt.AddExp;

import java.util.ArrayList;

public class VarCharArrayInfo extends VarInfo {

    private final ArrayList<AddExp> chars;

    private final String value;

    private final AddExp size;

    public VarCharArrayInfo(String ident, ArrayList<AddExp> chars, AddExp size) {
        super(ident);
        this.chars = chars;
        this.value = null;
        this.size = size;
        addDecoration("Char");
        addDecoration("Array");
    }

    public VarCharArrayInfo(String ident, String str, AddExp size) {
        super(ident);
        chars = null;
        value = str;
        this.size = size;
        addDecoration("Char");
        addDecoration("Array");
    }

    public VarCharArrayInfo(String ident) {
        super(ident);
        chars = null;
        value = null;
        size = null;
        addDecoration("Char");
        addDecoration("Array");
    }

    @Override
    public String toString() {
        return "CharArray";
    }
}
