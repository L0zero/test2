package semantic;

public class ConstStringInfo extends ConstInfo {
    public ConstStringInfo(String ident, String value) {
        super(ident);
        super.setValue(value);
        addDecoration("GlobalString");
    }
}
