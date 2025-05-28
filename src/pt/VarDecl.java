package pt;

import error.ParserError;
import error.SemanticError;
import ir.IRBlock;
import lexical.TokenType;
import semantic.*;

public class VarDecl extends Symbol {

    public static Symbol of(SymbolTable table) {
        Symbol symbol;
        Symbol varDecl = new VarDecl();
        if ((symbol = BType.of()) == null) {
            return null;
        }
        DecType type = ((BType) symbol).getType();
        varDecl.add(symbol);
        if ((symbol = VarDef.of(table)) == null) {
            return null;
        }
        varDecl.add(symbol);
        addSymbol(table, (VarDef) symbol, type);
        while (token.getType().equals(TokenType.COMMA)) {
            varDecl.add(token);
            token = lexer.nextToken();
            if ((symbol = VarDef.of(table)) == null) {
                return null;
            }
            varDecl.add(symbol);
            addSymbol(table, (VarDef) symbol, type);
        }
        if (!token.getType().equals(TokenType.SEMICN)) {
            logger.log(new ParserError(symbol.getLine(), "i"));
            return varDecl;
        }
        varDecl.add(token);
        token = lexer.nextToken();
        return varDecl;
    }

    private static void addSymbol(SymbolTable table, VarDef varDef, DecType decType) {
        if (decType.equals(DecType.Char)) {
            if (varDef.getType().equals(IdType.Var)) {

                String ident = varDef.getIdent();
                try {
                    SemanticAnalyser.addSymbolToTable(table, ident,
                            new VarCharInfo(ident, varDef.getExpValue()));
                } catch (Exception e) {
                    logger.log(new SemanticError(varDef.getIdentLine(), "b"));
                }
            } else if (varDef.getType().equals(IdType.Array)) {

                String ident = varDef.getIdent();
                try {
                    if (varDef.getVarList() != null) {
                        SemanticAnalyser.addSymbolToTable(table, ident,
                                new VarCharArrayInfo(ident, varDef.getVarList(), varDef.getExp()));
                    } else if (varDef.getStringValue() != null) {
                        SemanticAnalyser.addSymbolToTable(table, ident,
                                new VarCharArrayInfo(ident, varDef.getStringValue(), varDef.getExp()));
                    } else {
                        SemanticAnalyser.addSymbolToTable(table, ident,
                                new VarCharArrayInfo(ident));
                    }
                } catch (Exception e) {
                    logger.log(new SemanticError(varDef.getIdentLine(), "b"));
                }
            }
        } else if (decType.equals(DecType.Int)) {
            if (varDef.getType().equals(IdType.Var)) {

                String ident = varDef.getIdent();
                try {
                    SemanticAnalyser.addSymbolToTable(table, ident,
                            new VarIntInfo(ident, varDef.getExpValue()));
                } catch (Exception e) {
                    logger.log(new SemanticError(varDef.getIdentLine(), "b"));
                }
            } else if (varDef.getType().equals(IdType.Array)) {

                String ident = varDef.getIdent();
                try {
                    SemanticAnalyser.addSymbolToTable(table, ident,
                            new VarIntArrayInfo(ident, varDef.getVarList(), varDef.getExp()));
                } catch (Exception e) {
                    logger.log(new SemanticError(varDef.getIdentLine(), "b"));
                }
            }
        }
    }

    protected void visit(IRBlock irBlock) {
        for (Ele ele : list) {
            if (ele instanceof VarDef) {
                VarDef varDef = (VarDef) ele;
                varDef.visit(irBlock);
            }
        }
    }
}
