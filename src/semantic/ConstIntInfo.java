package semantic;

import pt.AddExp;

public class ConstIntInfo extends ConstInfo {

    private final AddExp value;

    public ConstIntInfo(String ident, AddExp value) {
        super(ident);
        this.value = value;
        addDecoration("Int");
    }

    @Override
    public String toString() {
        return "ConstInt";
    }
}
