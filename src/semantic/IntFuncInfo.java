package semantic;

import java.util.ArrayList;

public class IntFuncInfo extends FuncInfo {

    public IntFuncInfo(String funcName) {
        super(funcName);
        addDecoration("Int");
    }

    @Override
    public String toString() {
        return "IntFunc";
    }
}
