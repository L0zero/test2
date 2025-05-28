package pt;

import error.ParserError;
import ir.IRBlock;
import ir.Quadruple;
import lexical.TokenType;
import semantic.Info;
import semantic.SemanticAnalyser;

public class FuncFParam extends Symbol {

    private DecType decType;

    private IdType idType;

    private String ident;

    private int identLine;

    protected static Symbol of() {
        Symbol symbol;
        FuncFParam funcFParam = new FuncFParam();
        if ((symbol = BType.of()) == null) {
            return null;
        }
        funcFParam.decType = ((BType) symbol).getType();
        funcFParam.add(symbol);
        if ((symbol = Ident.of()) == null) {
            return null;
        }
        funcFParam.ident = ((Ident) symbol).getIdent();
        funcFParam.identLine = symbol.getLine();
        funcFParam.add(symbol);
        if (token.getType().equals(TokenType.LBRACK)) {
            funcFParam.idType = IdType.Array;
            funcFParam.add(token);
            token = lexer.nextToken();
            if (!token.getType().equals(TokenType.RBRACK)) {
                logger.log(new ParserError(symbol.getLine(), "k"));
                return funcFParam;
            }
            funcFParam.add(token);
            token = lexer.nextToken();
        } else {
            funcFParam.idType = IdType.Var;
        }
        return funcFParam;
    }

    protected DecType getDecType() {
        return decType;
    }

    protected IdType getIdType() {
        return idType;
    }

    protected String getIdent() {
        return ident;
    }

    protected int getIdentLine() {
        return identLine;
    }

    protected void visit(IRBlock irBlock) {
        String ident = getIdent();
        Info info = irBlock.getSymbolTable().getInfo(ident);
        ident = info.getVarIdent();
        int dim = 0;
        if (idType.equals(IdType.Array)) {
            dim = 1;
        }
        irBlock.push(info);
        irBlock.addQuadruple("fpara", ident, Integer.toString(dim), decType.toString());
    }
}
