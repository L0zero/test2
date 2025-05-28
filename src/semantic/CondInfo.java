package semantic;

public class CondInfo extends Info {
    public CondInfo(String cond) {
        super(cond);
        addDecoration("Cond");
        addDecoration("Int");
        addDecoration("Global");
    }
}
