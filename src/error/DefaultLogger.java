package error;

import io.OutputSystem;

import java.util.ArrayList;
import java.util.HashMap;

public class DefaultLogger implements Logger {

    private HashMap<Integer, CompilerError> errors = new HashMap<>();

    private ArrayList<CompilerError> errorList = new ArrayList<>();

    private ArrayList<String> logs = new ArrayList<>();

    /**
     * @param error
     */
    @Override
    public void log(CompilerError error) {
        if (errors.containsKey(error.getLine())) {
            return;
        } else {
            errors.put(error.getLine(), error);
            errorList.add(error);
        }
        // System.out.println("log: " + error.toString());
    }

    /**
     * @param message
     */
    @Override
    public void log(String message) {
        logs.add(message);
    }

    /**
     * @param outputSystem
     */
    @Override
    public void printError(OutputSystem outputSystem) {
        for (CompilerError error : errorList) {
            outputSystem.error(error);
        }
    }

    /**
     * @param outputSystem
     */
    @Override
    public void printLog(OutputSystem outputSystem) {
        for (String log : logs) {
            outputSystem.print(log);
        }
    }

    public LoggerMark mark() {
        LoggerMark mark = new LoggerMark();
        mark.errors.clear();
        mark.logs.clear();
        mark.errorList.clear();
        mark.errorList.addAll(errorList);
        mark.logs.addAll(logs);
        mark.errors.putAll(errors);
        return mark;
    }

    public void back(LoggerMark mark) {
        errors.clear();
        logs.clear();
        errorList.clear();
        errors.putAll(mark.errors);
        logs.addAll(mark.logs);
        errorList.addAll(mark.errorList);
    }

    public boolean hasError() {
        return !errorList.isEmpty();
    }
}
