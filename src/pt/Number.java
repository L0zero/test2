package pt;

public class Number extends Symbol {

    private int value = 0;

    public static Symbol of() {
        Number number = new Number();
        Symbol symbol;
        if ((symbol = IntConst.of()) == null) {
            return null;
        }
        number.add(symbol);
        IntConst intConst = ((IntConst) symbol);
        number.value = intConst.getValue();
        return number;
    }

    protected int getValue() {
        return value;
    }
}
