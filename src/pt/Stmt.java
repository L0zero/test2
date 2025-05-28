package pt;

import error.ParserError;
import error.SemanticError;
import ir.IRBlock;
import lexical.LexerMark;
import lexical.Token;
import lexical.TokenType;
import semantic.*;

import java.util.ArrayList;

public class Stmt extends Symbol {

    private boolean returned = false;

    private ForStmt forStmt1 = null;

    private Cond cond = null;

    private ForStmt forStmt2 = null;

    protected static Symbol of(String dir, SymbolTable table, FuncInfo info, Boolean loop) {
        Symbol symbol;
        Stmt stmt = new Stmt();

        LexerMark mark = lexer.mark();

         /*
            LVal 开头三种
            1. LVal = Exp;
            2. LVal = getint();
            3. LVal = getchar();
         */
        if ((symbol = LVal.of(table)) != null) {

            Info info1 = table.getInfo(((LVal) symbol).getIdent());
            if (info1 instanceof ConstInfo) {
                logger.log(new SemanticError(((LVal) symbol).getIdentLine(), "h"));
            }

            // =
            if (token.getType().equals(TokenType.ASSIGN)) {
                stmt.add(symbol);
                stmt.add(token);
                token = lexer.nextToken();

                // getint
                if (token.getType().equals(TokenType.GETINTTK)) {
                    stmt.add(token);
                    token = lexer.nextToken();

                    // (
                    if (!token.getType().equals(TokenType.LPARENT)) {
                        return null;
                    }
                    stmt.add(token);
                    token = lexer.nextToken();

                    // )
                    if (!token.getType().equals(TokenType.RPARENT)) {
                        logger.log(new ParserError(symbol.getLine(), "j"));
                    } else {
                        stmt.add(token);
                        token = lexer.nextToken();
                    }

                    // ;
                    if (!token.getType().equals(TokenType.SEMICN)) {
                        logger.log(new ParserError(symbol.getLine(), "i"));
                        return stmt;
                    }
                    stmt.add(token);
                    token = lexer.nextToken();
                    return stmt;

                    // getchar
                } else if (token.getType().equals(TokenType.GETCHARTK)) {
                    stmt.add(token);
                    token = lexer.nextToken();

                    // (
                    if (!token.getType().equals(TokenType.LPARENT)) {
                        return null;
                    }
                    stmt.add(token);
                    token = lexer.nextToken();

                    // )
                    if (!token.getType().equals(TokenType.RPARENT)) {
                        logger.log(new ParserError(symbol.getLine(), "j"));
                    } else {
                        stmt.add(token);
                        token = lexer.nextToken();
                    }

                    // ;
                    if (!token.getType().equals(TokenType.SEMICN)) {
                        logger.log(new ParserError(symbol.getLine(), "i"));
                        return stmt;
                    }
                    stmt.add(token);
                    token = lexer.nextToken();
                    return stmt;

                    // Exp
                } else if ((symbol = Exp.of(table)) != null) {
                    stmt.add(symbol);

                    // ;
                    if (!token.getType().equals(TokenType.SEMICN)) {
                        logger.log(new ParserError(symbol.getLine(), "i"));
                        return stmt;
                    }
                    stmt.add(token);
                    token = lexer.nextToken();
                    return stmt;
                }
            }
        }

        lexer.back(mark);
        token = lexer.getToken();

        /*
            Exp;
         */
        if ((symbol = Exp.of(table)) != null) {
            // ;
            if (token.getType().equals(TokenType.SEMICN)) {
                stmt.add(symbol);
                stmt.add(token);
                token = lexer.nextToken();
                return stmt;
            } else {
                stmt.add(symbol);
                logger.log(new ParserError(symbol.getLine(), "i"));
                return stmt;
            }
        }

        /*
            ;
         */
        if (token.getType().equals(TokenType.SEMICN)) {
            stmt.add(token);
            token = lexer.nextToken();
            return stmt;
        }

        /*
            Block
         */
        if ((symbol = Block.of(dir, info, loop)) != null) {
            stmt.add(symbol);
            return stmt;
        }

        /*
            if(Cond) Stmt [else Stmt]
         */
        if (token.getType().equals(TokenType.IFTK)) {
            stmt.add(token);
            token = lexer.nextToken();

            // (
            if (!token.getType().equals(TokenType.LPARENT)) {
                return null;
            }
            stmt.add(token);
            token = lexer.nextToken();

            // Cond
            if ((symbol = Cond.of(table)) == null) {
                return null;
            }
            stmt.add(symbol);

            // )
            if (!token.getType().equals(TokenType.RPARENT)) {
                logger.log(new ParserError(symbol.getLine(), "j"));
            } else {
                stmt.add(token);
                token = lexer.nextToken();
            }

            // Stmt
            if ((symbol = Stmt.of(dir, table, info, loop)) == null) {
                return null;
            }
            stmt.add(symbol);

            // else
            if (token.getType().equals(TokenType.ELSETK)) {
                stmt.add(token);
                token = lexer.nextToken();

                // Stmt
                if ((symbol = Stmt.of(dir, table, info, loop)) == null) {
                    return null;
                }
                stmt.add(symbol);
            }
            return stmt;
        }

        /*
            for(ForStmt; Cond; ForStmt) Stmt
         */
        if (token.getType().equals(TokenType.FORTK)) {
            stmt.add(token);
            token = lexer.nextToken();

            // (
            if (!token.getType().equals(TokenType.LPARENT)) {
                return null;
            }
            stmt.add(token);
            token = lexer.nextToken();

            // ForStmt
            if ((symbol = ForStmt.of(table)) != null) {
                stmt.add(symbol);
                stmt.forStmt1 = (ForStmt) symbol;
            }

            // ;
            if (!token.getType().equals(TokenType.SEMICN)) {
                return null;
            }
            stmt.add(token);
            token = lexer.nextToken();

            // Cond
            if ((symbol = Cond.of(table)) != null) {
                stmt.add(symbol);
                stmt.cond = (Cond) symbol;
            }

            // ;
            if (!token.getType().equals(TokenType.SEMICN)) {
                return null;
            }
            stmt.add(token);
            token = lexer.nextToken();

            // ForStmt
            if ((symbol = ForStmt.of(table)) != null) {
                stmt.add(symbol);
                stmt.forStmt2 = (ForStmt) symbol;
            }

            // )
            if (!token.getType().equals(TokenType.RPARENT)) {
                return null;
            }
            stmt.add(token);
            token = lexer.nextToken();

            // Stmt
            if ((symbol = Stmt.of(dir, table, info, true)) == null) {
                return null;
            }
            stmt.add(symbol);
            return stmt;
        }

        /*
            break;
         */
        if (token.getType().equals(TokenType.BREAKTK)) {
            if (!loop) {
                logger.log(new SemanticError(token.getLine(), "m"));
            }
            int line = token.getLine();
            stmt.add(token);
            token = lexer.nextToken();

            // ;
            if (!token.getType().equals(TokenType.SEMICN)) {
                // TODO
                logger.log(new ParserError(line, "i"));
                return stmt;
            }
            stmt.add(token);
            token = lexer.nextToken();
            return stmt;
        }

        /*
            continue;
         */
        if (token.getType().equals(TokenType.CONTINUETK)) {
            if (!loop) {
                logger.log(new SemanticError(token.getLine(), "m"));
            }
            int line = token.getLine();
            stmt.add(token);
            token = lexer.nextToken();

            // ;
            if (!token.getType().equals(TokenType.SEMICN)) {
                // TODO
                logger.log(new ParserError(line, "i"));
                return stmt;
            }
            stmt.add(token);
            token = lexer.nextToken();
            return stmt;
        }

        /*
            return [Exp];
         */
        if (token.getType().equals(TokenType.RETURNTK)) {
            stmt.add(token);
            int returnLine = token.getLine();
            token = lexer.nextToken();
            stmt.returned = true;
            // Exp
            if ((symbol = Exp.of(table)) != null) {
                stmt.add(symbol);
                if (info instanceof VoidFuncInfo) {
                    logger.log(new SemanticError(returnLine, "f"));
                }
            }

            // ;
            if (!token.getType().equals(TokenType.SEMICN)) {
                if (symbol == null) {
                    logger.log(new ParserError(returnLine, "i"));
                } else {
                    logger.log(new ParserError(symbol.getLine(), "i"));
                }
                return stmt;
            }
            stmt.add(token);
            token = lexer.nextToken();
            return stmt;
        }

        /*
            print(StringConst {, Exp});
         */
        if (token.getType().equals(TokenType.PRINTFTK)) {
            int printfLine = token.getLine();

            stmt.add(token);
            token = lexer.nextToken();

            // (
            if (!token.getType().equals(TokenType.LPARENT)) {
                return null;
            }
            stmt.add(token);
            token = lexer.nextToken();

            // StringConst
            if ((symbol = StringConst.of()) == null) {
                return null;
            }
            String pattern = ((StringConst) symbol).getValue();
            stmt.add(symbol);
            int num = getNum(pattern);
            int stmtNum = 0;
            // ,
            while (token.getType().equals(TokenType.COMMA)) {
                stmt.add(token);
                token = lexer.nextToken();
                stmtNum++;
                // Stmt
                if ((symbol = Exp.of(table)) == null) {
                    return null;
                }
                stmt.add(symbol);
            }

            if (stmtNum != num) {
                logger.log(new SemanticError(printfLine, "l"));
            }
            // )
            if (!token.getType().equals(TokenType.RPARENT)) {
                logger.log(new ParserError(symbol.getLine(), "j"));
            } else {
                stmt.add(token);
                token = lexer.nextToken();
            }

            // ;
            if (!(token.getType().equals(TokenType.SEMICN))) {
                logger.log(new ParserError(symbol.getLine(), "i"));
                return stmt;
            }
            stmt.add(token);
            token = lexer.nextToken();
            return stmt;
        }
        return null;
    }

    public boolean getReturned() {
        return returned;
    }

    private static int getNum(String pattern) {
        String replaced = pattern.replaceAll("%d|%c", " ");
        return pattern.length() - replaced.length();
    }

    protected IRBlock visit(IRBlock irBlock, IRBlock continueBlock, IRBlock breakBlock) {
        Ele ele = list.get(0);
        String t1 = "0";
        if (ele instanceof LVal) {
            LVal lval = (LVal) ele;
            Ele ele1 = list.get(2);
            if (ele1 instanceof Exp) {
                Exp exp = (Exp) ele1;
                t1 = irBlock.use(exp.visit(irBlock));
            } else if (ele1 instanceof Token) {
                Token token = (Token) ele1;
                if (token.getType().equals(TokenType.GETCHARTK)) {
                    irBlock.addQuadruple("be", "rpara");
                    irBlock.addQuadruple("call", "getchar");
                } else if (token.getType().equals(TokenType.GETINTTK)) {
                    irBlock.addQuadruple("be", "rpara");
                    irBlock.addQuadruple("call", "getint");
                }
                Info info = irBlock.genTempInfo();
                t1 = irBlock.def(info);
                irBlock.addQuadruple("getrt", t1);
                t1 = irBlock.use(info);
            }
            lval.assign(irBlock, t1);
            return irBlock;
        } else if (ele instanceof Exp) {
            Exp exp = (Exp) ele;
            exp.visit(irBlock);
            return irBlock;
        } else if (ele instanceof Block) {
            Block block = (Block) ele;
            String dir = block.tableDir;
            IRBlock newBlock = new IRBlock(irBlock, dir, "new irBlock for block");
            irBlock.addNextBlock(newBlock);
            irBlock.setNextBlock(newBlock);
            dir = irBlock.getTableDir();
            irBlock = block.visit(newBlock, continueBlock, breakBlock);
            newBlock = new IRBlock(irBlock, dir, "new irBlock after block");
            irBlock.addNextBlock(newBlock);
            irBlock.setNextBlock(newBlock);
            return newBlock;
        } else if (ele instanceof Token) {
            Token token = (Token) ele;
            String dir = irBlock.getTableDir();
            Stmt stmt;
            switch (token.getType()) {
                case IFTK:
                    Cond cond = (Cond) list.get(2);
                    irBlock = cond.visit(irBlock);
                    if (list.get(3) instanceof Stmt) {
                        stmt = (Stmt) list.get(3);
                    } else {
                        stmt = (Stmt) list.get(4);
                    }
                    if (list.size() == 5) {
                        IRBlock block1 = new IRBlock(irBlock, dir, "if B1");
                        IRBlock block2 = new IRBlock(irBlock, dir, "if B2");

                        /*
                            B0 -> B1 true to stmt
                            B0 -> B2 false to next
                         */
                        irBlock.addNextBlock(block1);
                        irBlock.addNextBlock(block2);
                        irBlock.setNextBlock(block1);
                        t1 = irBlock.use(Info.CondTempInfo);
                        irBlock.addQuadruple("bez", t1, block2.getBlockName());
                        block1 = stmt.visit(block1, continueBlock, breakBlock);
                        block1.addNextBlock(block2);
                        block1.setNextBlock(block2);

                        return block2;
                    } else {
                        Stmt stmt1 = (Stmt) list.get(list.size() - 1);

                        IRBlock block1 = new IRBlock(irBlock, dir, "if else B1");
                        IRBlock block2 = new IRBlock(irBlock, dir, "if else B2");
                        IRBlock block3 = new IRBlock(irBlock, dir, "if else B3");

                        /*
                            B0 -> B1 true to stmt
                            B0 -> B2 false to else
                         */
                        irBlock.addNextBlock(block1);
                        irBlock.addNextBlock(block2);
                        irBlock.setNextBlock(block1);
                        t1 = irBlock.use(Info.CondTempInfo);
                        irBlock.addQuadruple("bez", t1, block2.getBlockName());

                        /*
                            B1 -> B3 stmt skip else
                         */
                        block1 = stmt.visit(block1, continueBlock, breakBlock);
                        block1.addQuadruple("j", block3.getBlockName());
                        block1.addNextBlock(block3);
                        block1.setNextBlock(block2);

                        /*
                            B2 -> B3 else
                         */
                        block2 = stmt1.visit(block2, continueBlock, breakBlock);
                        block2.addNextBlock(block3);
                        block2.setNextBlock(block3);

                        return block3;
                    }
                case FORTK:
                    stmt = (Stmt) list.get(list.size() - 1);
                    IRBlock block1 = new IRBlock(irBlock, dir, "for B1");
                    IRBlock block2 = new IRBlock(irBlock, dir, "for B2");
                    IRBlock block3 = new IRBlock(irBlock, dir, "for B3");
                    IRBlock block4 = new IRBlock(irBlock, dir, "for B4");

                    /*
                        B0 -> B1 for_stmt1 to loop
                     */
                    if (forStmt1 != null) {
                        forStmt1.visit(irBlock);
                    }
                    irBlock.addNextBlock(block1);
                    irBlock.setNextBlock(block1);

                    /*
                        B1 -> B2 true to stmt and for_stmt2
                        B1 -> B4 false skip loop
                     */
                    if (this.cond != null) {
                        block1 = this.cond.visit(block1);
                        t1 = block1.use(Info.CondTempInfo);
                        block1.addQuadruple("bez", t1, block4.getBlockName());
                        block1.addNextBlock(block2);
                        block1.addNextBlock(block4);
                        block1.setNextBlock(block2);
                    }

                    /*
                        B1 -> B2 infinity loop
                     */
                    else {
                        block1.addNextBlock(block2);
                        block1.setNextBlock(block2);
                    }

                    /*
                        B2 -> B3 loop, continue
                        B2 -> B4 break
                     */
                    block2 = stmt.visit(block2, block3, block4);
                    block2.addNextBlock(block3);
                    block2.addNextBlock(block4);
                    block2.setNextBlock(block3);

                    /*
                        B3 -> B1 loop
                     */
                    if (forStmt2 != null) {
                        forStmt2.visit(block3);
                    }
                    block3.addQuadruple("j", block1.getBlockName());
                    block3.addNextBlock(block1);
                    block3.setNextBlock(block4);

                    return block4;
                case BREAKTK:
                    irBlock.addQuadruple("j", breakBlock.getBlockName());
                    irBlock.addNextBlock(breakBlock);
                    IRBlock irBlock1 = new IRBlock(irBlock, irBlock.getTableDir(), "After Break");
                    irBlock.setNextBlock(irBlock1);
                    return irBlock1;
                case CONTINUETK:
                    irBlock.addQuadruple("j", continueBlock.getBlockName());
                    irBlock.addNextBlock(continueBlock);
                    irBlock1 = new IRBlock(irBlock, irBlock.getTableDir(), "After Continue");
                    irBlock.setNextBlock(irBlock1);
                    return irBlock1;
                case RETURNTK:
                    if (list.get(1) instanceof Exp) {
                        Exp exp = (Exp) list.get(1);
                        Info info = exp.visit(irBlock);
                        irBlock.addQuadruple("ret", irBlock.use(info));
                        return irBlock;
                    } else {
                        irBlock.addQuadruple("ret", "none");
                        return irBlock;
                    }
                case PRINTFTK:
                    ArrayList<String> paras = new ArrayList<>();
                    for (Ele ele1 : list) {
                        if (ele1 instanceof Exp) {
                            Exp exp = (Exp) ele1;
                            Info info = exp.visit(irBlock);
                            t1 = irBlock.use(info);
                            paras.add(t1);
                        } else if (ele1 instanceof StringConst) {
                            String s = ((StringConst) ele1).getValue();
                            IRBlock globalBlock = IRBlock.getGlobalDecl();
                            String ident = IRBlock.genGlobalStringIdent();
                            Info info = new ConstStringInfo(ident, s);
                            try {
                                SemanticAnalyser.addSymbolToTable(globalBlock.getSymbolTable(), ident, info);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                            globalBlock.def(info);
                            ident = irBlock.use(info);
                            paras.add(ident);
                        }
                    }
                    Info info = irBlock.genTempInfo();
                    String num = irBlock.def(info);
                    irBlock.addQuadruple("im", Integer.toString(paras.size()), num);
                    irBlock.addQuadruple("be", "rpara");
                    for (String para : paras) {
                        irBlock.addQuadruple("rpara", para);
                    }
                    num = irBlock.use(info);
                    irBlock.addQuadruple("rpara", num);
                    irBlock.addQuadruple("call", "printf");
                    return irBlock;
                default:
                    return irBlock;
            }
        }
        return irBlock;
    }

}
