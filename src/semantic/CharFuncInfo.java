package semantic;

public class CharFuncInfo extends FuncInfo {

    public CharFuncInfo(String funcName) {
        super(funcName);
        addDecoration("Char");
    }

    @Override
    public String toString() {
        return "CharFunc";
    }
}
