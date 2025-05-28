package semantic;

import pt.AddExp;

public class VarIntInfo extends VarInfo {

    private final AddExp value;

    public VarIntInfo(String ident, AddExp value) {
        super(ident);
        this.value = value;
        addDecoration("Int");
    }

    @Override
    public String toString() {
        return "Int";
    }
}
