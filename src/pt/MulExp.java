package pt;

import error.CalculateException;
import ir.IRBlock;
import lexical.Token;
import lexical.TokenType;
import semantic.Info;
import semantic.SymbolTable;

public class MulExp extends Symbol {

    private DecType type;

    private IdType idType;

    public static Symbol of(SymbolTable table) {
        Symbol symbol;
        MulExp symbol1;
        MulExp mulExp = new MulExp();
        if ((symbol = UnaryExp.of(table)) == null) {
            return null;
        }
        mulExp.type = ((UnaryExp) symbol).getType();
        mulExp.idType = ((UnaryExp) symbol).getIdType();
        mulExp.add(symbol);
        while (token.getType().equals(TokenType.MULT) ||
                token.getType().equals(TokenType.DIV) ||
                token.getType().equals(TokenType.MOD)) {
            // TODO 输出Mul结束
            symbol1 = new MulExp();
            symbol1.add(mulExp);
            symbol1.type = mulExp.type;
            symbol1.idType = mulExp.idType;
            mulExp = symbol1;
            mulExp.add(token);
            token = lexer.nextToken();
            if ((symbol = UnaryExp.of(table)) == null) {
                return null;
            }
            mulExp.add(symbol);
        }
        return mulExp;
    }

    protected DecType getType() {
        return type;
    }

    protected IdType getIdType() {
        return idType;
    }

    protected int cal() throws CalculateException {
        MulExpType type = MulExpType.MUL;
        int ans = 1;
        for (Ele ele : list) {
            if (ele instanceof UnaryExp) {
                UnaryExp unaryExp = (UnaryExp) ele;
                switch (type) {
                    case MUL:
                        ans *= unaryExp.cal();
                        break;
                    case DIV:
                        ans /= unaryExp.cal();
                        break;
                    case MOD:
                        ans %= unaryExp.cal();
                        break;
                    default:
                        break;
                }
            } else if (ele instanceof MulExp) {
                MulExp mulExp = (MulExp) ele;
                switch (type) {
                    case MUL:
                        ans *= mulExp.cal();
                        break;
                    case DIV:
                        ans /= mulExp.cal();
                        break;
                    case MOD:
                        ans %= mulExp.cal();
                        break;
                    default:
                        break;
                }
            } else if (ele instanceof Token) {
                Token token = (Token) ele;
                switch (token.getType()) {
                    case MULT:
                        type = MulExpType.MUL;
                        break;
                    case DIV:
                        type = MulExpType.DIV;
                        break;
                    case MOD:
                        type = MulExpType.MOD;
                        break;
                    default:
                        break;
                }
            }
        }
        return ans;
    }

    protected enum MulExpType {
        MUL, DIV, MOD
    }

    protected Info visit(IRBlock irBlock) {
        if (list.size() == 1) {
            UnaryExp unaryExp = (UnaryExp) list.get(0);
            return unaryExp.visit(irBlock);
        } else if (list.size() == 3) {
            MulExp mulExp = (MulExp) list.get(0);
            Token token1 = (Token) list.get(1);
            UnaryExp unaryExp = (UnaryExp) list.get(2);
            String t1 = irBlock.use(mulExp.visit(irBlock));
            String t2 = irBlock.use(unaryExp.visit(irBlock));
            Info info = irBlock.genTempInfo();
            String ans = irBlock.def(info);
            if (token1.getType().equals(TokenType.MULT)) {
                irBlock.addQuadruple("mult", t1, t2, ans);
            } else if (token1.getType().equals(TokenType.DIV)){
                irBlock.addQuadruple("div", t1, t2, ans);
            } else {
                irBlock.addQuadruple("mod", t1, t2, ans);
            }
            return info;
        }
        return null;
    }
}
