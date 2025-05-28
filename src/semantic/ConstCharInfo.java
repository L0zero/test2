package semantic;

import pt.AddExp;

public class ConstCharInfo extends ConstInfo {

    private final AddExp value;

    public ConstCharInfo(String ident, AddExp value) {
        super(ident);
        this.value = value;
        addDecoration("Char");
    }

    @Override
    public String toString() {
        return "ConstChar";
    }
}
