package pt;

import error.CalculateException;
import ir.IRBlock;
import lexical.TokenType;
import semantic.SymbolTable;

import java.util.ArrayList;

public class ConstInitVal extends Symbol {

    private String stringValue;

    private ArrayList<AddExp> explist = new ArrayList<>();

    private AddExp exp;

    private IdType type;

    public static Symbol of(SymbolTable table) {
        Symbol symbol;
        ConstInitVal constInitVal = new ConstInitVal();

        // not ConstExp
        if ((symbol = ConstExp.of(table)) == null) {


            if (token.getType().equals(TokenType.LBRACE)) {
                constInitVal.type = IdType.Array;
                constInitVal.add(token);
                token = lexer.nextToken();
                if ((symbol = ConstExp.of(table)) != null) {
                    constInitVal.add(symbol);
                    constInitVal.explist.add(((ConstExp) symbol).getExp());
                    while (token.getType().equals(TokenType.COMMA)) {
                        constInitVal.add(token);
                        token = lexer.nextToken();
                        if ((symbol = ConstExp.of(table)) == null) {
                            return null;
                        }
                        constInitVal.explist.add(((ConstExp) symbol).getExp());
                        constInitVal.add(symbol);
                    }
                }
                if (!token.getType().equals(TokenType.RBRACE)) {
                    return null;
                }
                constInitVal.add(token);
                token = lexer.nextToken();
                return constInitVal;
            } else {
                symbol = StringConst.of();
                if (symbol == null) {
                    return null;
                }
                constInitVal.add(symbol);
                constInitVal.type = IdType.String;
                constInitVal.stringValue = ((StringConst) symbol).getValue();
                return constInitVal;
            }
        }

        // ConstExp
        else {
            constInitVal.add(symbol);
            constInitVal.type = IdType.Var;
            constInitVal.exp = ((ConstExp) symbol).getExp();
            return constInitVal;
        }
    }

    protected IdType getType() {
        return type;
    }

    protected String getStringValue() {
        return stringValue;
    }

    protected ArrayList<AddExp> getVarList() {
        return explist;
    }

    protected AddExp getExp() {
        return exp;
    }

    protected String getValue(IRBlock irBlock) {
        StringBuilder builder = new StringBuilder();
        switch (type) {
            case Array:
                if (explist.isEmpty()) {
                    return "{}";
                }
                builder.append("{");
                for (AddExp addExp : explist) {
                    try {
                        builder.append(addExp.cal());
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    builder.append(",");
                }
                builder.deleteCharAt(builder.length() - 1);
                builder.append("}");
                break;
            case Var:
                try {
                    builder.append(exp.cal());
                } catch (Exception e) {
                    e.printStackTrace();
                }
                break;
            case String:
                builder.append(stringValue);
                break;
            default:
                break;
        }
        return builder.toString();
    }

}
