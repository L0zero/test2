package pt;

import error.CalculateException;
import ir.IRBlock;
import lexical.Token;
import lexical.TokenType;
import semantic.Info;
import semantic.SymbolTable;

public class AddExp extends Symbol {

    private DecType type;

    private IdType idType;

    public static Symbol of(SymbolTable table) {
        Symbol symbol;
        AddExp symbol1;
        AddExp addExp = new AddExp();
        if ((symbol = MulExp.of(table)) == null) {
            return null;
        }
        addExp.add(symbol);
        addExp.type = ((MulExp) symbol).getType();
        addExp.idType = ((MulExp) symbol).getIdType();
        while (token.getType().equals(TokenType.PLUS) ||
                token.getType().equals(TokenType.MINU)) {
            symbol1 = new AddExp();
            symbol1.add(addExp);
            symbol1.type = addExp.type;
            symbol1.idType = addExp.idType;
            addExp = symbol1;
            addExp.add(token);
            token = lexer.nextToken();
            if ((symbol = MulExp.of(table)) == null) {
                return null;
            }
            addExp.add(symbol);
        }
        return addExp;
    }

    public int cal() throws CalculateException {
        boolean pos = true;
        int ans = 0;
        for (Ele ele : list) {
            if (ele instanceof MulExp) {
                MulExp mulExp = (MulExp) ele;
                if (pos) {
                    ans += mulExp.cal();
                } else {
                    ans -= mulExp.cal();
                }
            } else if (ele instanceof AddExp) {
                AddExp addExp = (AddExp) ele;
                if (pos) {
                    ans += addExp.cal();
                } else {
                    ans -= addExp.cal();
                }
            } else if (ele instanceof Token) {
                Token token = (Token) ele;
                switch (token.getType()) {
                    case PLUS:
                        pos = true;
                        break;
                    case MINU:
                        pos = false;
                        break;
                    default:
                        break;
                }
            }
        }
        return ans;
    }

    protected DecType getType() {
        return type;
    }

    protected IdType getIdType() {
        return idType;
    }

    protected Info visit(IRBlock irBlock) {
        if (list.size() == 1) {
            MulExp mulExp = (MulExp) list.get(0);
            return mulExp.visit(irBlock);
        } else if (list.size() == 3) {
            AddExp addExp = (AddExp) list.get(0);
            Token token1 = (Token) list.get(1);
            MulExp mulExp = (MulExp) list.get(2);
            String t1 = irBlock.use(addExp.visit(irBlock));
            String t2 = irBlock.use(mulExp.visit(irBlock));
            Info info = irBlock.genTempInfo();
            String ans = irBlock.def(info);
            if (token1.getType().equals(TokenType.PLUS)) {
                irBlock.addQuadruple("add", t1, t2, ans);
            } else {
                irBlock.addQuadruple("sub", t1, t2, ans);
            }
            return info;
        }
        return null;
    }

}
