package pt;

import error.CalculateException;
import ir.IRBlock;
import lexical.TokenType;
import semantic.Info;
import semantic.SymbolTable;

import java.util.ArrayList;

public class InitVal extends Symbol {

    private String stringValue;

    private ArrayList<AddExp> explist = new ArrayList<>();

    private AddExp exp;

    private IdType type;

    public static Symbol of(SymbolTable table) {
        Symbol symbol;
        InitVal initVal = new InitVal();
        if ((symbol = Exp.of(table)) != null) {
            initVal.add(symbol);
            initVal.exp = ((Exp) symbol).getExp();
            initVal.type = IdType.Var;
            return initVal;
        } else if (token.getType().equals(TokenType.LBRACE)) {
            initVal.add(token);
            token = lexer.nextToken();
            initVal.type = IdType.Array;
            if ((symbol = Exp.of(table)) != null) {
                initVal.add(symbol);
                initVal.explist.add(((Exp) symbol).getExp());
                while (token.getType().equals(TokenType.COMMA)) {
                    initVal.add(token);
                    token = lexer.nextToken();
                    if ((symbol = Exp.of(table)) == null) {
                        return null;
                    }
                    initVal.add(symbol);
                    initVal.explist.add(((Exp) symbol).getExp());
                }
            }
            if (!token.getType().equals(TokenType.RBRACE)) {
                return null;
            }
            initVal.add(token);
            token = lexer.nextToken();
        } else if ((symbol = StringConst.of()) != null) {
            initVal.add(symbol);
            initVal.type = IdType.String;
            initVal.stringValue = ((StringConst) symbol).getValue();
        } else {
            return null;
        }
        return initVal;
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

    protected IdType getType() {
        return type;
    }

    protected String visit(IRBlock irBlock) {
        StringBuilder builder = new StringBuilder();
        Info info;
        switch (type) {
            case Var:
                try {
                    int ans = exp.cal();
                    builder.append(ans);
                } catch (CalculateException e) {
                    info = exp.visit(irBlock);
                    builder.append(irBlock.use(info));
                }
                break;
            case Array:
                builder.append('{');
                for (AddExp addExp : explist) {
                    try {
                        int ans = addExp.cal();
                        builder.append(ans);
                    } catch (CalculateException e) {
                        info = addExp.visit(irBlock);
                        builder.append(irBlock.use(info));
                    }
                    builder.append(",");
                }
                builder.delete(builder.length() - 1, builder.length());
                builder.append('}');
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
