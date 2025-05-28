package semantic;

import java.util.ArrayList;

public class FuncInfo extends Info {

    private final ArrayList<Info> fParams = new ArrayList<>();

    private boolean returned;

    public FuncInfo(String funcName) {
        super(funcName);
        addDecoration("Func");
    }

    public void addParam(Info param) {
        fParams.add(param);
    }

    public ArrayList<Info> getParams() {
        return fParams;
    }

    public void setReturned(boolean returned) {
        this.returned = returned;
    }

    public boolean isReturned() {
        return returned;
    }

    public int getParamsNum() {
        return fParams.size();
    }

    public ArrayList<Info> getParamsList() {
        return fParams;
    }
}
