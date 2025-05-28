package semantic;

public class VarInfo extends Info {
    public VarInfo(String ident) {
        super(ident);
        addDecoration("Var");
    }
}
