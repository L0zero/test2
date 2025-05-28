import assembler.Assembler;
import assembler.SimpleAssembler;
import ir.IRBlock;
import pt.ParseTree;
import error.Logger;
import error.DefaultLogger;
import io.*;
import lexical.DefaultLexer;
import lexical.Lexer;
import semantic.SemanticAnalyser;
import semantic.SymbolTable;
import syntax.DefaultParser;
import syntax.Parser;

import java.io.File;

public class Compiler {

    public static Logger logger;

    public static void main(String[] args) {
        InputSystem inputSystem = new FileInputSystem(new File("testfile.txt"));
        OutputSystem parserOutputSystem = new FileOutputSystem(new File("parser.txt"));
        OutputSystem errorOutputSystem = new FileOutputSystem(new File("error.txt"));
        OutputSystem symbolOutputSystem = new FileOutputSystem(new File("symbol.txt"));
        OutputSystem irOutputSystem = new FileOutputSystem(new File("ir.txt"));
        OutputSystem mipsOutputSystem = new FileOutputSystem(new File("mips.txt"));
        OutputSystem varIdentOutputSystem = new FileOutputSystem(new File("ident.txt"));
        OutputSystem consoleOutputSystem = new StreamOutputSystem();
        logger = new DefaultLogger();

        Lexer lexer = new DefaultLexer(inputSystem, logger);
        Parser parser = new DefaultParser(lexer, logger);
        ParseTree tree;
        try {
            tree = parser.parse();
            tree.printAns();

        } catch (Exception e) {
            e.printStackTrace();
            return;
        }
        if (logger.hasError()) {
            logger.printLog(parserOutputSystem);
            logger.printError(errorOutputSystem);
            return;
        }
        tree.makeIR();
        IRBlock.phiAnalyze();
        IRBlock.removePhis();
        SymbolTable.printAllVarIdent(varIdentOutputSystem);
        logger.printLog(parserOutputSystem);
        logger.printError(errorOutputSystem);
        IRBlock.printAns(irOutputSystem);
        SemanticAnalyser.printAns(symbolOutputSystem);
        irOutputSystem.close();
        InputSystem mipsInputSystem = new FileInputSystem(new File("ir.txt"));
        Assembler assembler = new SimpleAssembler();
        assembler.assemble(mipsInputSystem, mipsOutputSystem);
    }
}
