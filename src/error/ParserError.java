package error;

public class ParserError extends CompilerError {
    public ParserError(int line, String message) {
        super(line, message);
    }
}
