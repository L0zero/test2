package error;

public class LexerError extends CompilerError{
    public LexerError(int line, String message) {
        super(line, message);
    }
}
