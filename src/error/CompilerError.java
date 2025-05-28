package error;

public class CompilerError {

    protected final int line;

    protected final String message;

    public CompilerError(int line, String message) {
        this.line = line;
        this.message = message;
    }

    public int getLine() {
        return line;
    }

    public String getMessage() {
        return message;
    }
}
