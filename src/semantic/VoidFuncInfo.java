package semantic;

public class VoidFuncInfo extends FuncInfo {

    public VoidFuncInfo(String funcName) {
        super(funcName);
        addDecoration("Void");
    }

    @Override
    public String toString() {
        return "VoidFunc";
    }

}
