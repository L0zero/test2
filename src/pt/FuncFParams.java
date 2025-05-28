package pt;

import error.SemanticError;
import ir.IRBlock;
import lexical.TokenType;
import semantic.*;

public class FuncFParams extends Symbol {

    public static Symbol of(SymbolTable table, FuncInfo info) {
        Symbol symbol;
        Symbol funcFParams = new FuncFParams();
        if ((symbol = FuncFParam.of()) == null) {
            return null;
        }
        funcFParams.add(symbol);
        info.addParam(addSymbol(table, (FuncFParam) symbol));
        while (token.getType().equals(TokenType.COMMA)) {
            funcFParams.add(token);
            token = lexer.nextToken();
            if ((symbol = FuncFParam.of()) == null) {
                return null;
            }
            info.addParam(addSymbol(table, (FuncFParam) symbol));
            funcFParams.add(symbol);
        }
        return funcFParams;
    }

    private static Info addSymbol(SymbolTable table, FuncFParam param) {
        DecType decType = param.getDecType();
        IdType idType = param.getIdType();
        String ident = param.getIdent();
        Info info;
        if (decType.equals(DecType.Int)) {
            if (idType.equals(IdType.Var)) {
                info = new VarIntInfo(ident,
                        null);
            } else {
                info = new VarIntArrayInfo(ident);
            }
        } else {
            if (idType.equals(IdType.Var)) {
                info = new VarCharInfo(ident);
            } else {
                info = new VarCharArrayInfo(ident);
            }
        }
        info.addDecoration("FParam");
        try {
            SemanticAnalyser.addSymbolToTable(table, ident, info);
        } catch (Exception e) {
            logger.log(new SemanticError(param.getIdentLine(), "b"));
        }
        return info;
    }

    protected void visit(IRBlock irBlock) {
        for (Ele ele : list) {
            if (ele instanceof FuncFParam) {
                FuncFParam param = (FuncFParam) ele;
                param.visit(irBlock);
            }
        }
    }
}
