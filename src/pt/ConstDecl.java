package pt;

import error.NameExistsException;
import error.ParserError;
import error.SemanticError;
import ir.IRBlock;
import lexical.TokenType;
import semantic.*;

public class ConstDecl extends Symbol {

    public static Symbol of(SymbolTable table) {
        // Const
        if (!token.getType().equals(TokenType.CONSTTK)) {
            return null;
        }
        Symbol constDecl = new ConstDecl();
        constDecl.add(token);
        token = lexer.nextToken();


        Symbol symbol;
        DecType decType;
        // BType
        if ((symbol = BType.of()) == null) {
            return null;
        }
        decType = ((BType) symbol).getType();
        constDecl.add(symbol);

        // ConstDef
        if ((symbol = ConstDef.of(table)) == null) {
            return null;
        }
        constDecl.add(symbol);
        addSymbol(table, decType, (ConstDef) symbol);

        // next ConstDef
        while (token.getType().equals(TokenType.COMMA)) {
            constDecl.add(token);
            token = lexer.nextToken();

            // ConstDef
            if ((symbol = ConstDef.of(table)) == null) {
                return null;
            }
            constDecl.add(symbol);
            addSymbol(table, decType, (ConstDef) symbol);
        }
        if (!token.getType().equals(TokenType.SEMICN)) {
            logger.log(new ParserError(symbol.getLine(), "i"));
            return constDecl;
        }
        constDecl.add(token);
        token = lexer.nextToken();
        return constDecl;
    }

    private static void addSymbol(SymbolTable table, DecType decType, ConstDef constDecl) {

        IdType idType = constDecl.getType();

        // if int
        if (decType.equals(DecType.Int)) {

            // if var
            if (idType.equals(IdType.Var)) {

                String ident = constDecl.getIdent();
                try {
                    SemanticAnalyser.addSymbolToTable(table,
                            ident,
                            new ConstIntInfo(ident, constDecl.getExpValue()));
                } catch (NameExistsException e) {
                    logger.log(new SemanticError(constDecl.getIdentLine(), "b"));
                }
            }

            // if array
            if (idType.equals(IdType.Array)) {

                String ident = constDecl.getIdent();
                try {
                    SemanticAnalyser.addSymbolToTable(table,
                            ident,
                            new ConstIntArrayInfo(ident,
                                    constDecl.getVarList(),
                                    constDecl.getExp()));
                } catch (NameExistsException e) {
                    logger.log(new SemanticError(constDecl.getIdentLine(), "b"));
                }
            }
        }

        // if char
        else if (decType.equals(DecType.Char)) {

            // if var
            if (idType.equals(IdType.Var)) {

                String ident = constDecl.getIdent();
                try {
                    SemanticAnalyser.addSymbolToTable(table,
                            ident,
                            new ConstCharInfo(ident, constDecl.getExpValue()));
                } catch (NameExistsException e) {
                    logger.log(new SemanticError(constDecl.getIdentLine(), "b"));
                }
            }

            // if array
            if (idType.equals(IdType.Array)) {

                String ident = constDecl.getIdent();
                try {
                    if (constDecl.getVarList() != null) {
                        SemanticAnalyser.addSymbolToTable(table,
                                ident,
                                new ConstCharArrayInfo(ident,
                                        constDecl.getVarList(),
                                        constDecl.getExp()));
                    } else if (constDecl.getStringValue() != null) {
                        SemanticAnalyser.addSymbolToTable(table,
                                ident,
                                new ConstCharArrayInfo(ident,
                                        constDecl.getStringValue(),
                                        constDecl.getExp()));
                    }
                } catch (NameExistsException e) {
                    logger.log(new SemanticError(constDecl.getIdentLine(), "b"));
                }
            }
        }
    }

    protected void visit(IRBlock irBlock) {
        for (Ele ele : list) {
            if (ele instanceof ConstDef) {
                try {
                    ConstDef constDef = (ConstDef) ele;
                    constDef.visit(irBlock);
                } catch (Exception e) {
                    e.printStackTrace();
                    logger.log(new SemanticError(ele.getLine(), "z"));
                }
            }
        }
    }
}
