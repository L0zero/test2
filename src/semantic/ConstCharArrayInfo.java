package semantic;

import pt.AddExp;

import java.util.ArrayList;

public class ConstCharArrayInfo extends ConstInfo {

    private final ArrayList<AddExp> chars;

    private final String value;

    private final AddExp size;

    public ConstCharArrayInfo(String ident, ArrayList<AddExp> chars, AddExp size) {
        super(ident);
        this.chars = chars;
        value = null;
        this.size = size;
        addDecoration("Char");
        addDecoration("Array");
    }

    public ConstCharArrayInfo(String ident, String str, AddExp size) {
        super(ident);
        value = str;
        chars = null;
        this.size = size;
        addDecoration("Char");
        addDecoration("Array");
    }

    @Override
    public String toString() {
        return "ConstCharArray";
    }
}
