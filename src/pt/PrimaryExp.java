package pt;

import error.CalculateException;
import error.ParserError;
import ir.IRBlock;
import lexical.TokenType;
import semantic.Info;
import semantic.SymbolTable;

public class PrimaryExp extends Symbol {

    private DecType type;

    private IdType idType;

    public static Symbol of(SymbolTable table) {
        Symbol symbol;
        PrimaryExp primaryExp = new PrimaryExp();
        if (token.getType().equals(TokenType.LPARENT)) {
            primaryExp.add(token);
            token = lexer.nextToken();
            if ((symbol = Exp.of(table)) == null) {
                return null;
            }
            primaryExp.type = ((Exp) symbol).getType();
            primaryExp.idType = ((Exp) symbol).getIdType();

            primaryExp.add(symbol);
            if (!token.getType().equals(TokenType.RPARENT)) {
                logger.log(new ParserError(symbol.getLine(), "j"));
                return primaryExp;
            }
            primaryExp.add(token);
            token = lexer.nextToken();
            return primaryExp;
        }
        if ((symbol = LVal.of(table)) != null) {
            primaryExp.add(symbol);
            primaryExp.type = ((LVal) symbol).getType();
            primaryExp.idType = ((LVal) symbol).getIdType();
            return primaryExp;
        }
        if ((symbol = Number.of()) != null) {
            primaryExp.add(symbol);
            primaryExp.idType = IdType.Var;
            primaryExp.type = DecType.Int;
            return primaryExp;
        }
        if ((symbol = Char.of()) != null) {
            primaryExp.type = DecType.Char;
            primaryExp.idType = IdType.Var;
            primaryExp.add(symbol);
            return primaryExp;
        }
        return null;
    }

    protected DecType getType() {
        return type;
    }

    protected IdType getIdType() {
        return idType;
    }

    protected int cal() throws CalculateException {
        for (Ele ele : list) {
            if (ele instanceof Exp) {
                Exp exp = (Exp) ele;
                return exp.cal();
            } else if (ele instanceof LVal) {
                return ((LVal) ele).cal();
            } else if (ele instanceof Number){
                Number number = (Number) ele;
                return number.getValue();
            } else if (ele instanceof Char) {
                Char charValue = (Char) ele;
                return charValue.cal();
            }
        }
        return 0;
    }

    protected Info visit(IRBlock irBlock) {
        if (list.size() != 1) {
            Exp exp = (Exp) list.get(1);
            return exp.visit(irBlock);
        }
        if (list.get(0) instanceof LVal) {
            LVal lval = (LVal) list.get(0);
            return lval.visit(irBlock);
        } else if (list.get(0) instanceof Number) {
            Number number = (Number) list.get(0);
            Info info = irBlock.genTempInfo();
            String ans = irBlock.def(info);
            irBlock.addQuadruple("im", Integer.toString(number.getValue()), ans);
            return info;
        } else if (list.get(0) instanceof Char) {
            Char charValue = (Char) list.get(0);
            Info info = irBlock.genTempInfo();
            String ans = irBlock.def(info);
            irBlock.addQuadruple("im", Integer.toString(charValue.cal()), ans);
            return info;
        }
        return null;
    }

}

