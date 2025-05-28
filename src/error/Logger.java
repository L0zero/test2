package error;

import io.OutputSystem;

public interface Logger {

    public void log(CompilerError error);

    public void log(String message);

    public void printError(OutputSystem outputSystem);

    public void printLog(OutputSystem outputSystem);

    public LoggerMark mark();

    public void back(LoggerMark mark);

    public boolean hasError();
}
