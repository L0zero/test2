package error;

import java.util.ArrayList;
import java.util.HashMap;

public class LoggerMark {
    protected ArrayList<CompilerError> errorList = new ArrayList<>();
    protected ArrayList<String> logs = new ArrayList<>();
    protected HashMap<Integer, CompilerError> errors = new HashMap<>();
}
