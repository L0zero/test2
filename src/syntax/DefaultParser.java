package syntax;

import pt.ParseTree;
import error.Logger;
import lexical.Lexer;

public class DefaultParser implements Parser {

    private final ParseTree tree;

    public DefaultParser(Lexer lexer, Logger logger) {
        tree = new ParseTree(lexer, logger);
    }

    /**
     * @return
     * @throws Exception
     */
    @Override
    public ParseTree parse() throws Exception {
        tree.parse();
        return tree;
    }
}
