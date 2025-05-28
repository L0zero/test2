package error;

public class SemanticError extends ParserError {


    public SemanticError(int line, String message) {
        super(line, message);
    }
}
