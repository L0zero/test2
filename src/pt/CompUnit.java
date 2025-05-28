package pt;

import error.ParserError;
import error.SemanticError;
import ir.IRBlock;
import ir.Quadruple;
import lexical.Token;
import lexical.TokenType;
import semantic.*;

public class CompUnit extends Symbol {
    
    protected static Symbol of() {
        Symbol symbol = null;
        Symbol ans = new CompUnit();
        SymbolTable table = SemanticAnalyser.newSymbolTable(SymbolTable.getDir());
        String dir = SymbolTable.getDir();

        /*
            {Decl}
         */
        while (true) {

            // ConstDecl
            if ((symbol = ConstDecl.of(table)) != null) {
                Decl decl = new Decl();
                decl.add(symbol);
                ans.add(decl);
                continue;
            }

            DecType decType;

            // VarDecl, MainFuncDef, FuncDef(int char)
            if ((symbol = BType.of()) != null) {

                // VarDecl, FuncDef
                if (token.getType().equals(TokenType.IDENFR)) {
                    token = lexer.previewToken();

                    // FuncDef
                    if (token.getType().equals(TokenType.LPARENT)) {

                        // get Type
                        decType = ((BType) symbol).getType();
                        token = lexer.getToken();
                        FuncDef funcDef = new FuncDef();
                        FuncType funcType = new FuncType();
                        funcType.add((Token) symbol.getList().get(0));
                        funcDef.add(funcType);
                        if ((symbol = Ident.of()) == null) {
                            return null;
                        }

                        // get ident
                        String ident = ((Ident) symbol).getIdent();
                        funcDef.funcName = ident;

                        // get dir, table
                        String funcDir = SymbolTable.getDir(dir, SymbolTable.SymbolType.Function, ident);
                        SymbolTable funcTable = SemanticAnalyser.newSymbolTable(funcDir);
                        funcDef.tableDir = funcDir;
                        funcDef.add(symbol);
                        int line = symbol.getLine();
                        if (!token.getType().equals(TokenType.LPARENT)) {
                            return null;
                        }
                        funcDef.add(token);
                        token = lexer.nextToken();

                        // get func info
                        FuncInfo info;
                        if (decType.equals(DecType.Char)) {
                            info = new CharFuncInfo(ident);
                        } else {
                            info = new IntFuncInfo(ident);
                        }

                        try {
                            SemanticAnalyser.addSymbolToTable(table, ident, info);
                        } catch (Exception e) {
                            logger.log(new SemanticError(line, "b"));
                        }

                        // insert funcFParams into table and info
                        if ((symbol = FuncFParams.of(funcTable, info)) != null) {
                            funcDef.add(symbol);
                            line = symbol.getLine();
                        }
                        if (!token.getType().equals(TokenType.RPARENT)) {
                            logger.log(new ParserError(line, "j"));
                        } else {
                            funcDef.add(token);
                            token = lexer.nextToken();
                        }

                        // insert symbols in block to table
                        if ((symbol = Block.of(funcDir, funcTable, info, false)) == null) {
                            return null;
                        }
                        if (!((Block) symbol).isReturned()) {
                            logger.log(new SemanticError(((Block) symbol).getTokenLine(), "g"));
                        }
                        funcDef.add(symbol);
                        ans.add(funcDef);
                        break;

                    }

                    // VarDecl
                    else {
                        token = lexer.getToken();
                        VarDecl varDecl = new VarDecl();
                        varDecl.add(symbol);
                        decType = ((BType) symbol).getType();
                        if ((symbol = VarDef.of(table)) == null) {
                            return null;
                        }
                        varDecl.add(symbol);
                        addSymbol(table, (VarDef) symbol, decType);
                        while (token.getType().equals(TokenType.COMMA)) {
                            varDecl.add(token);
                            token = lexer.nextToken();
                            if ((symbol = VarDef.of(table)) == null) {
                                return null;
                            }
                            addSymbol(table, (VarDef) symbol, decType);
                            varDecl.add(symbol);
                        }
                        if (!token.getType().equals(TokenType.SEMICN)) {
                            logger.log(new ParserError(symbol.getLine(), "i"));
                        } else {
                            varDecl.add(token);
                            token = lexer.nextToken();
                        }
                        Decl decl = new Decl();
                        decl.add(varDecl);
                        ans.add(decl);
                    }

                    // MainFuncDef
                } else if (token.getType().equals(TokenType.MAINTK)) {
                    if (((Token) symbol.getList().get(0))
                            .getType().equals(TokenType.INTTK)) {
                        MainFuncDef mainFuncDef = new MainFuncDef();
                        mainFuncDef.add((Token) symbol.getList().get(0));
                        mainFuncDef.add(token);
                        token = lexer.nextToken();
                        if (!token.getType().equals(TokenType.LPARENT)) {
                            return null;
                        }
                        mainFuncDef.add(token);
                        int line = token.getLine();
                        token = lexer.nextToken();
                        if (!token.getType().equals(TokenType.RPARENT)) {
                            logger.log(new ParserError(line, "j"));
                        } else {
                            mainFuncDef.add(token);
                            token = lexer.nextToken();
                        }
                        String mainDir = SymbolTable.getDir(dir, SymbolTable.SymbolType.Function, "main");
                        SymbolTable mainTable = SemanticAnalyser.newSymbolTable(mainDir);
                        if ((symbol = Block.of(mainDir, mainTable, new IntFuncInfo("main"), false)) == null) {
                            return null;
                        }
                        if (!((Block) symbol).isReturned()) {
                            logger.log(new SemanticError(((Block) symbol).getTokenLine(), "g"));
                        }
                        mainFuncDef.add(symbol);
                        mainFuncDef.tableDir = mainDir;
                        ans.add(mainFuncDef);
                        return ans;
                    } else {
                        return null;
                    }
                } else {
                    return null;
                }

                // FuncDef
            } else {
                if (token.getType().equals(TokenType.VOIDTK)) {
                    if ((symbol = FuncDef.of(dir, table)) == null) {
                        return null;
                    }
                    ans.add(symbol);
                    break;
                } else {
                    return null;
                }
            }
        }

        // FuncDef, MainFuncDef
        while (true) {

            // MainFuncDef
            // TODO 报错差了一行
            if (token.getType().equals(TokenType.INTTK)) {
                token = lexer.previewToken();
                if (token.getType().equals(TokenType.MAINTK)) {
                    token = lexer.getToken();
                    String mainDir = SymbolTable.getDir(dir, SymbolTable.SymbolType.Function, "main");
                    SymbolTable mainTable = SemanticAnalyser.newSymbolTable(mainDir);
                    if ((symbol = MainFuncDef.of(mainDir, mainTable)) == null) {
                        return null;
                    }
                    ans.add(symbol);
                    return ans;
                }
                token = lexer.getToken();
            }

            // FuncDef
            if ((symbol = FuncDef.of(dir, table)) == null) {
                return null;
            }
            ans.add(symbol);
        }
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

    // irBlock: globalDeclBlock
    // irBlock1: globalValueBlock
    protected void visit(IRBlock irBlock) {
        for (Ele ele : list) {
            if (ele instanceof Decl) {
                Decl decl = (Decl) ele;
                decl.visit(irBlock);
            } else if (ele instanceof FuncDef) {
                FuncDef funcDef = (FuncDef) ele;
                IRBlock newBlock = new IRBlock(funcDef.funcName, funcDef.tableDir);
                funcDef.visit(newBlock);
            } else if (ele instanceof MainFuncDef) {
                MainFuncDef mainFuncDef = (MainFuncDef) ele;
                IRBlock newBlock = new IRBlock("main", mainFuncDef.tableDir);
                mainFuncDef.visit(newBlock);
            }
        }
        irBlock.addQuadruple("be", "decl");
    }

}
