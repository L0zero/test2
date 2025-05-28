package pt;

public class Char extends Symbol {

    private int c = 0;

    public static Symbol of() {
        Char character = new Char();
        Symbol symbol;
        if ((symbol = CharConst.of()) == null) {
            return null;
        }
        character.c = ((CharConst) symbol).getValue();
        character.add(symbol);
        return character;
    }

    public String toString() {
        return "<Character>\n";
    }

    protected int cal() {
        return c;
    }

}
