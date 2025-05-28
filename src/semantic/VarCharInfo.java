package semantic;

import pt.AddExp;

public class VarCharInfo extends VarInfo {

    private final AddExp value;

    public VarCharInfo(String ident, AddExp value) {
        super(ident);
        this.value = value;
        addDecoration("Char");
    }

    public VarCharInfo(String ident) {
        super(ident);
        this.value = null;
        addDecoration("Char");
    }

    @Override
    public String toString() {
        return "Char";
    }
}
