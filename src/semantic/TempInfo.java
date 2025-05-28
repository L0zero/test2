package semantic;

public class TempInfo extends Info {
    /**
     * New Info
     *
     * @param ident
     */
    public TempInfo(String ident) {
        super(ident);
        addDecoration("Temp");
        addDecoration("Int");
        addDecoration("Var");
    }
}
