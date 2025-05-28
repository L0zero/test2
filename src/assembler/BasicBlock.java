package assembler;

import java.util.ArrayList;
import java.util.HashMap;

public class BasicBlock {

    private static final HashMap<String, BasicBlock> funcs = new HashMap<>();

    private String name;

    private final ArrayList<BasicBlock> froms = new ArrayList<>();

    private final ArrayList<BasicBlock> tos = new ArrayList<>();

    private final ArrayList<String> defs = new ArrayList<>();

    public BasicBlock(String name) {
        this.name = name;
    }
}
