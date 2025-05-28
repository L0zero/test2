package pt;

import error.IRError;
import error.ParserError;
import ir.IRBlock;
import ir.Quadruple;
import lexical.TokenType;
import semantic.Info;
import semantic.SemanticAnalyser;
import semantic.SymbolTable;

import java.util.ArrayList;
import java.util.Objects;

public class VarDef extends Symbol {

    private String ident;

    private ConstExp constExp = null;

    private InitVal initVal = null;

    private IdType type;

    private String stringValue = null;

    private AddExp expValue = null;

    private ArrayList<AddExp> explist = null;

    private AddExp exp = null;

    private int identLine;

    public int getIdentLine() {
        return identLine;
    }

    public static Symbol of(SymbolTable table) {
        VarDef varDef = new VarDef();
        Symbol symbol;
        if ((symbol = Ident.of()) == null) {
            return null;
        }
        varDef.ident = ((Ident) symbol).getIdent();
        varDef.identLine = symbol.getLine();
        varDef.add(symbol);
        if (token.getType().equals(TokenType.LBRACK)) {
            varDef.add(token);
            token = lexer.nextToken();
            varDef.type = IdType.Array;
            if ((symbol = ConstExp.of(table)) == null) {
                return null;
            }
            varDef.constExp = (ConstExp) symbol;
            varDef.exp = ((ConstExp) symbol).getExp();
            varDef.add(symbol);
            if (!token.getType().equals(TokenType.RBRACK)) {
                logger.log(new ParserError(symbol.getLine(), "k"));
            } else {
                varDef.add(token);
                token = lexer.nextToken();
            }
        } else {
            varDef.type = IdType.Var;
        }
        if (token.getType().equals(TokenType.ASSIGN)) {
            varDef.add(token);
            token = lexer.nextToken();
            if ((symbol = InitVal.of(table)) == null) {
                return null;
            }
            varDef.initVal = (InitVal) symbol;
            if (((InitVal) symbol).getType().equals(IdType.Var)) {
                varDef.expValue = ((InitVal) symbol).getExp();
            } else if (((InitVal) symbol).getType().equals(IdType.Array)) {
                varDef.explist = ((InitVal) symbol).getVarList();
            } else if (((InitVal) symbol).getType().equals(IdType.String)) {
                varDef.stringValue = ((InitVal) symbol).getStringValue();
            }
            varDef.add(symbol);
        }
        return varDef;
    }

    protected String getIdent() {
        return ident;
    }

    protected IdType getType() {
        return type;
    }

    protected String getStringValue() {
        return stringValue;
    }

    protected AddExp getExpValue() {
        return expValue;
    }

    protected ArrayList<AddExp> getVarList() {
        return explist;
    }

    protected AddExp getExp() {
        return exp;
    }

    protected void visit(IRBlock irBlock) {
        Info info = irBlock.getSymbolTable().getInfo(ident);
        String identString = irBlock.def(info);
        irBlock.push(info);
        String value = "";
        int length = 0;
        if (initVal != null) {
            value = initVal.visit(irBlock);
        }
        if (constExp != null) {
            try {
                length = constExp.cal();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        info.setLen(length);
        if (!Objects.equals(value, "")) {
            irBlock.addQuadruple("values", identString, value);
        }
    }
}
