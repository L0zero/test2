package pt;

import ir.IRBlock;
import lexical.TokenType;
import semantic.SymbolTable;

import java.util.ArrayList;

public class FuncRParams extends Symbol {

    private final ArrayList<DecType> params = new ArrayList<>();

    private final ArrayList<IdType> paramsId = new ArrayList<>();


    public static Symbol of(SymbolTable table) {
        FuncRParams funcRParams = new FuncRParams();
        Symbol symbol;
        if ((symbol = Exp.of(table)) == null) {
            return null;
        }
        funcRParams.params.add(((Exp) symbol).getType());
        funcRParams.paramsId.add(((Exp) symbol).getIdType());
        funcRParams.add(symbol);
        while (token.getType().equals(TokenType.COMMA)) {
            funcRParams.add(token);
            token = lexer.nextToken();
            if ((symbol = Exp.of(table)) == null) {
                return null;
            }
            funcRParams.add(symbol);
            funcRParams.params.add(((Exp) symbol).getType());
            funcRParams.paramsId.add(((Exp) symbol).getIdType());
        }
        return funcRParams;
    }

    protected ArrayList<DecType> getParams() {
        return params;
    }

    protected ArrayList<IdType> getParamsId() {
        return paramsId;
    }

    protected ArrayList<String> visit(IRBlock irBlock) {
        ArrayList<String> paras = new ArrayList<>();
        for (Symbol symbol : symbols) {
            Exp exp = (Exp) symbol;
            String t1 = irBlock.use(exp.visit(irBlock));
            paras.add(t1);
        }
        return paras;
    }

}
