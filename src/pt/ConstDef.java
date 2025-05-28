package pt;

import error.CalculateException;
import error.ParserError;
import ir.IRBlock;
import ir.Quadruple;
import lexical.TokenType;
import semantic.ConstInfo;
import semantic.Info;
import semantic.SemanticAnalyser;
import semantic.SymbolTable;

import java.util.ArrayList;

public class ConstDef extends Symbol {

    private String ident;

    private ConstExp constExp = null;

    private ConstInitVal constInitVal = null;

    private IdType type;

    private String stringValue = null;

    private AddExp expValue = null;

    private ArrayList<AddExp> explist = null;

    private AddExp exp = null;

    private int line;

    public int getIdentLine() {
        return line;
    }

    public String getIdent() {
        return ident;
    }

    public IdType getType() {
        return type;
    }

    public String getStringValue() {
        return stringValue;
    }

    public AddExp getExpValue() {
        return expValue;
    }

    public ArrayList<AddExp> getVarList() {
        return explist;
    }

    public AddExp getExp() {
        return exp;
    }

    public static Symbol of(SymbolTable table) {
        Symbol symbol;
        ConstDef constDef = new ConstDef();

        // Ident
        if ((symbol = Ident.of()) == null) {
            return null;
        }
        constDef.add(symbol);
        constDef.ident = ((Ident) symbol).getIdent();
        constDef.line = symbol.getLine();

        // if [
        if (token.getType().equals(TokenType.LBRACK)) {
            constDef.add(token);
            token = lexer.nextToken();

            // ConstExp
            symbol = ConstExp.of(table);
            if (symbol == null) {
                return null;
            }
            constDef.exp = ((ConstExp) symbol).getExp();
            constDef.add(symbol);
            constDef.type = IdType.Array;
            constDef.constExp = (ConstExp) symbol;

            // ]
            if (!token.getType().equals(TokenType.RBRACK)) {
                logger.log(new ParserError(symbol.getLine(), "k"));
            } else {
                constDef.add(token);
                token = lexer.nextToken();
            }

            // no [, var
        } else {
            constDef.type = IdType.Var;
        }

        // =
        if (!token.getType().equals(TokenType.ASSIGN)) {
            return null;
        }
        constDef.add(token);
        token = lexer.nextToken();

        // ConstInitVal
        symbol = ConstInitVal.of(table);
        if (symbol == null) {
            return null;
        }
        constDef.constInitVal = (ConstInitVal) symbol;
        if (((ConstInitVal) symbol).getType().equals(IdType.Var)) {
            constDef.expValue = ((ConstInitVal) symbol).getExp();
        } else if (((ConstInitVal) symbol).getType().equals(IdType.Array)) {
            constDef.explist = ((ConstInitVal) symbol).getVarList();
        } else if (((ConstInitVal) symbol).getType().equals(IdType.String)) {
            constDef.stringValue = ((ConstInitVal) symbol).getStringValue();
        }
        constDef.add(symbol);
        return constDef;
    }

    protected void visit(IRBlock irBlock) throws CalculateException {
        Info info = irBlock.getSymbolTable().getInfo(ident);
        String value = constInitVal.getValue(irBlock);
        irBlock.def(info);
        int len = 0;
        if (constExp != null) {
            len = constExp.cal();
        }
        ConstInfo constInfo = (ConstInfo) info;
        constInfo.setValue(value);
        constInfo.setLen(len);
    }
}
