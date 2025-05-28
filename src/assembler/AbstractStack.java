package assembler;

import debug.Debug;
import semantic.Info;
import semantic.SymbolTable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Objects;
import java.util.Stack;

public class AbstractStack {

    private final HashMap<String, Integer> stack = new HashMap<>();

    private final HashMap<String, String> types = new HashMap<>();

    private final String name;

    private final Stack<String> stackString = new Stack<>();

    private static final HashMap<String, String> globals = new HashMap<>();

    private static final HashMap<String, String> globalTypes = new HashMap<>();

    private int sp = 0;

    protected AbstractStack() {
        this.sp = 0;
        this.name = "$root";
    }

    protected AbstractStack(AbstractStack stack, String name) {
        this.sp = stack.sp;
        this.name = name;
        this.stack.putAll(stack.stack);
        this.types.putAll(stack.types);
        this.stackString.addAll(stack.stackString);
    }

    // 普通变量，int或者char类型，sp-4
    protected ArrayList<String> push(String ident) {
        ArrayList<String> result = new ArrayList<>();
        if (name.equals("$root")) {
            result.add("# wrong!!! push ident to $root");
            return result;
        }
        while (sp % 4 != 0) {
            sp--;
            result.add("    addiu $sp $sp -1");
        }
        sp -= 4;
        Info info = SymbolTable.getInfoFromVarIdent(ident);
        stack.put(ident, sp);
        stackString.push(ident);
        if (info != null) {
            types.put(ident, info.getDecorations().contains("Int") ? "int" : "char");
        } else {
            types.put(ident, "int");
        }

        if (Debug.DEBUG) {
            result.add("# push int ident: " + ident + " at " + sp);
            result.add(" ");
        }
        result.add("    addiu $sp $sp -4");
        return result;
    }

    // 指针类型，用于数组传参，sp-4
    protected ArrayList<String> push(String ident, boolean integer) {
        ArrayList<String> result = new ArrayList<>();
        while (sp % 4 != 0) {
            sp--;
            result.add("    addiu $sp $sp -1");
        }
        sp -= 4;
        stack.put(ident, sp);
        stackString.push(ident);
        if (integer) {
            types.put(ident, "int*");
            if (Debug.DEBUG) {
                result.add("# push int* ident: " + ident + " at " + sp);
                result.add(" ");
            }
        } else {
            types.put(ident, "char*");
            if (Debug.DEBUG) {
                result.add("# push char* ident: " + ident + " at " + sp);
                result.add(" ");
            }
        }
        result.add("    addiu $sp $sp -4");
        return result;
    }

    protected ArrayList<String> fPush(String ident) {
        ArrayList<String> result = new ArrayList<>();
        while (sp % 4 != 0) {
            sp--;
        }
        sp -= 4;
        stack.put(ident, sp);
        stackString.push(ident);
        types.put(ident, Objects.requireNonNull(SymbolTable.getInfoFromVarIdent(ident)).getDecorations().contains("Int") ? "int" : "char");
        if (Debug.DEBUG) {
            result.add("# push int ident: " + ident + " at " + sp);
            result.add(" ");
        }
        return result;
    }

    protected ArrayList<String> fPush(String ident, boolean integer) {
        ArrayList<String> result = new ArrayList<>();
        while (sp % 4 != 0) {
            sp--;
        }
        sp -= 4;
        stack.put(ident, sp);
        stackString.push(ident);
        if (integer) {
            types.put(ident, "int*");
            if (Debug.DEBUG) {
                result.add("# push int* ident: " + ident + " at " + sp);
                result.add(" ");
            }
        } else {
            types.put(ident, "char*");
            if (Debug.DEBUG) {
                result.add("# push char* ident: " + ident + " at " + sp);
                result.add(" ");
            }
        }
        return result;
    }

    // 数组类型，例如新声明的数组，sp-len*(integer?4:1)
    protected ArrayList<String> push(String ident, boolean integer, int len) {
        ArrayList<String> result = new ArrayList<>();

        if (integer) {
            if (name.equals("$root")) {
                result.add("# wrong!!! push ident to $root!");
                return result;
            }
            while (sp % 4 != 0) {
                sp--;
                result.add("    addiu $sp $sp -1");
            }
            sp -= len * 4;
            stack.put(ident, sp);
            stackString.push(ident);
            types.put(ident, "int[]");
            if (Debug.DEBUG) {
                result.add("# push int[] ident: " + ident + " at " + sp + " len " + (len * 4));
                result.add(" ");
            }
            result.add("    addiu $sp $sp -" + len*4);
        } else {
            if (name.equals("$root")) {
                result.add("# wrong!!! push ident to $root!");
                return result;
            }
            sp -= len;
            stack.put(ident, sp);
            stackString.push(ident);
            types.put(ident, "char[]");
            if (Debug.DEBUG) {
                result.add("# push char[] ident: " + ident + " at " + sp + " len " + len);
                result.add(" ");
            }
            result.add("    addiu $sp $sp -" + len);
        }
        return result;
    }

    protected ArrayList<String> pop(int pp) {
        ArrayList<String> result = new ArrayList<>();
        pp = pp - sp;
        sp = sp + pp;
        String ident = stackString.peek();
        int p = stack.get(ident);
        while (p < sp) {
            if (Debug.DEBUG) {
                result.add("# pop " + types.get(ident) + " " + ident + " at " + p);
            }
            stackString.pop();
            stack.remove(ident);
            types.remove(ident);
            if (stack.isEmpty()) {
                return result;
            }
            ident = stackString.peek();
            p = stack.get(ident);

        }
        return result;
    }

    protected ArrayList<String> getPara(String ident) {
        ArrayList<String> result = new ArrayList<>();
        int p;
        String type;
        if (stack.containsKey(ident)) {
            p = stack.get(ident);
            type = types.get(ident);
            if (Debug.DEBUG) {
                result.add("# getPara " + ident + " at " + p);
            }
            p = p - sp;

        } else if (ident.startsWith("Global@") || ident.startsWith("String@") || ident.startsWith("Cond@")) {
            String id = globals.get(ident);
            type = globalTypes.get(ident);
            switch (type) {
                case "int": {
                    result.add("    lw $s0 " + id);
                    break;
                }
                case "char": {
                    result.add("    lbu $s0 " + id);
                    break;
                }
                case "char[]":
                case "int[]": {
                    result.add("    la $s0 " + id);
                    break;
                }
                default: {
                    break;
                }
            }
            return result;
        } else {
            result.add("# Wrong!!! can't find " + ident);
            result.add("");
            return result;
        }
        switch (type) {
            case "int":
            case "char*":
            case "int*":
                result.add("    lw $s0 " + p + "($sp)");
                break;
            case "char[]":
            case "int[]":
                result.add("    addiu $s0 $sp " + p);
                break;
            case "char":
                result.add("    lbu $s0 " + p + "($sp)");
                break;
            default:
                break;
        }
        if (Debug.DEBUG) {
            result.add(" ");
        }
        return result;
    }

    // ident = $s0
    protected ArrayList<String> saveIdent(String ident) {
        ArrayList<String> result = new ArrayList<>();
        int p;
        String type;
        if (stack.containsKey(ident)) {
            p = stack.get(ident);
            if (Debug.DEBUG) {
                result.add("# saveIdent " + ident + " at " + p);
            }
            p = p - sp;
        } else if (ident.startsWith("Global@") || ident.startsWith("String@") || ident.startsWith("Cond@")) {
            String id = globals.get(ident);
            type = globalTypes.get(ident);
            switch (type) {
                case "int":
                case "int[]": {
                    result.add("    sw $s0 " + id);
                    break;
                }
                case "char":
                case "char[]": {
                    result.add("    sb $s0 " + id);
                    break;
                }
                default: {
                    break;
                }
            }
            return result;
        } else {
            result.add("# Wrong!!! can't find " + ident);
            result.add("");
            return result;
        }
        type = types.get(ident);
        switch (type) {
            case "int*":
            case "char*":
            case "int[]":
            case "int": {
                result.add("    sw $s0 " + p + "($sp)");
                break;
            }
            case "char[]":
            case "char": {
                result.add("    sb $s0 " + p + "($sp)");
            }
        }

        if (Debug.DEBUG) {
            result.add(" ");
        }
        return result;
    }

    // save ident[pos] = $s0
    // use $s0 $s1
    protected ArrayList<String> put(String ident, int pos) {
        ArrayList<String> result = new ArrayList<>();
        int p;
        String type;
        if (stack.containsKey(ident)) {
            p = stack.get(ident);
            if (Debug.DEBUG) {
                result.add("# put " + ident + " at " + p);
            }
            p = p - sp;
        } else if (ident.startsWith("Global@") || ident.startsWith("String@") || ident.startsWith("Cond@")) {
            String id = globals.get(ident);
            type = globalTypes.get(ident);
            switch (type) {
                case "int[]": {
                    result.add("    sw $s0 " + id + "+" + pos*4);
                    break;
                }
                case "char[]": {
                    result.add("    sb $s0 " + id + "+" + pos);
                    break;
                }
                default: {
                    result.add("# Wrong!!! Try to put to a int or char");
                    result.add("");
                    break;
                }
            }
            return result;
        } else {
            result.add("# Wrong!!! can't find " + ident);
            result.add("");
            return result;
        }
        type = types.get(ident);
        switch (type) {
            case "int[]": {
                result.add("    sw $s0 " + (p + pos*4) + "($sp)");
                break;
            }
            case "char[]": {
                result.add("    sb $s0 " + (p + pos) + "($sp)");
                break;
            }
            case "int*": {
                result.add("    lw $s1 " + p + "($sp)");
                result.add("    sw $s0 " + (pos*4) + "($s1)");
                break;
            }
            case "char*": {
                result.add("    lw $s1 " + p + "($sp)");
                result.add("    sb $s0 " + pos + "($s1)");
                break;
            }
            default: {
                result.add("# Wrong!!! Try to put to a int or char");
                result.add("");
            }
        }

        if (Debug.DEBUG) {
            result.add(" ");
        }
        return result;
    }

    // save ident[$s0] = $s1
    // use $s0 $s1
    protected ArrayList<String> put(String ident) {
        ArrayList<String> result = new ArrayList<>();
        int p;
        String type;
        if (stack.containsKey(ident)) {
            p = stack.get(ident);
            if (Debug.DEBUG) {
                result.add("# put " + ident + " at " + p);
            }
            p = p - sp;
        } else if (ident.startsWith("Global@") || ident.startsWith("String@") || ident.startsWith("Cond@")) {
            String id = globals.get(ident);
            type = globalTypes.get(ident);
            switch (type) {
                case "int[]": {
                    result.add("    sll $s0 $s0 2");
                    result.add("    sw $s1 " + id + "($s0)");
                    break;
                }
                case "char[]": {
                    result.add("    sb $s1 " + id + "($s0)");
                    break;
                }
                default: {
                    result.add("# Wrong!!! Try to put to a int or char");
                    result.add("");
                    break;
                }
            }
            return result;
        } else {
            result.add("# Wrong!!! can't find " + ident);
            result.add("");
            return result;
        }
        type = types.get(ident);
        switch (type) {
            case "int[]": {
                result.add("    sll $s0 $s0 2");
                result.add("    addu $s0 $s0 $sp");
                result.add("    sw $s1 " + p + "($s0)");
                break;
            }
            case "char[]": {
                result.add("    addu $s0 $s0 $sp");
                result.add("    sb $s1 " + p + "($s0)");
                break;
            }
            case "int*": {
                result.add("    lw $s2 " + p + "($sp)");
                result.add("    sll $s0 $s0 2");
                result.add("    addu $s0 $s0 $s2");
                result.add("    sw $s1 ($s0)");
                break;
            }
            case "char*": {
                result.add("    lw $s2 " + p + "($sp)");
                result.add("    addu $s0 $s0 $s2");
                result.add("    sb $s1 ($s0)");
                break;
            }
            default: {
                result.add("# Wrong!!! Try to put to a int or char");
                result.add("");
            }
        }

        if (Debug.DEBUG) {
            result.add(" ");
        }
        return result;
    }

    // $s0 = ident[pos]
    // use $s0 $s1
    protected ArrayList<String> get(String ident, int pos) {
        ArrayList<String> result = new ArrayList<>();
        int p;
        String type;
        if (stack.containsKey(ident)) {
            p = stack.get(ident);
            if (Debug.DEBUG) {
                result.add("# get " + ident + " at " + p);
            }
            p = p - sp;
        } else if (ident.startsWith("Global@") || ident.startsWith("String@") || ident.startsWith("Cond@")) {
            String id = globals.get(ident);
            type = globalTypes.get(ident);
            switch (type) {
                case "int[]": {
                    result.add("    lw $s0 " + id + "+" + pos*4);
                    break;
                }
                case "char[]": {
                    result.add("    lbu $s0 " + id + "+" + pos);
                    break;
                }
                default: {
                    result.add("# Wrong!!! Try to get a int or char");
                    result.add("");
                    break;
                }
            }
            return result;
        } else {
            result.add("# Wrong!!! can't find " + ident);
            result.add("");
            return result;
        }
        type = types.get(ident);
        switch (type) {
            case "int[]": {
                result.add("    lw $s0 " + (p + pos*4) + "($sp)");
                break;
            }
            case "char[]": {
                result.add("    lbu $s0 " + (p + pos) + "($sp)");
                break;
            }
            case "int*": {
                result.add("    lw $s1 " + p + "($sp)");
                result.add("    lw $s0 " + (pos*4) + "($s1)");
                break;
            }
            case "char*": {
                result.add("    lw $s1 " + p + "($sp)");
                result.add("    lbu $s0 " + pos + "($s1)");
                break;
            }
            default: {
                result.add("# Wrong!!! Try to get a int or char");
                result.add("");
            }
        }

        if (Debug.DEBUG) {
            result.add(" ");
        }
        return result;
    }

    // $s0 = ident[$s0]
    // use $s0 $s1
    protected ArrayList<String> get(String ident) {
        ArrayList<String> result = new ArrayList<>();
        int p;
        String type;
        if (stack.containsKey(ident)) {
            p = stack.get(ident);
            if (Debug.DEBUG) {
                result.add("# get " + ident + " at " + p);
            }
            p = p - sp;
        } else if (ident.startsWith("Global@") || ident.startsWith("String@") || ident.startsWith("Cond@")) {
            String id = globals.get(ident);
            type = globalTypes.get(ident);
            switch (type) {
                case "int[]": {
                    result.add("    sll $s0 $s0 2");
                    result.add("    lw $s0 " + id + "($s0)");
                    break;
                }
                case "char[]": {
                    result.add("    lbu $s0 " + id + "($s0)");
                    break;
                }
                default: {
                    result.add("# Wrong!!! Try to get a int or char");
                    result.add("");
                    break;
                }
            }
            return result;
        } else {
            result.add("# Wrong!!! can't find " + ident);
            result.add("");
            return result;
        }
        type = types.get(ident);
        switch (type) {
            case "int[]": {
                result.add("    sll $s0 $s0 2");
                result.add("    addu $s0 $s0 $sp");
                result.add("    lw $s0 " + p + "($s0)");
                break;
            }
            case "char[]": {
                result.add("    addu $s0 $s0 $sp");
                result.add("    lbu $s0 " + p + "($sp)");
                break;
            }
            case "int*": {
                result.add("    lw $s1 " + p + "($sp)");
                result.add("    sll $s0 $s0 2");
                result.add("    addu $s0 $s0 $s1");
                result.add("    lw $s0 " + "($s0)");
                break;
            }
            case "char*": {
                result.add("    lw $s1 " + p + "($sp)");
                result.add("    addu $s0 $s0 $s1");
                result.add("    lbu $s0 " + "($s0)");
                break;
            }
            default: {
                result.add("# Wrong!!! Try to get a int or char");
                result.add("");
            }
        }

        if (Debug.DEBUG) {
            result.add(" ");
        }
        return result;
    }

    protected ArrayList<String> saveReg(String reg) {
        ArrayList<String> result = new ArrayList<>();
        int p;
        if (stack.containsKey(reg)) {
            p = stack.get(reg);
            if (Debug.DEBUG) {
                result.add("# saveReg " + reg + " at " + p);
            }
            p = p - sp;
        } else {
            result.add("# wrong!!!");
            result.add("# try to find " + reg);
            result.add(" ");
            return result;
        }
        reg = reg.substring(0, 3);
        result.add("    sw " + reg + " " + p + "($sp)");
        if (Debug.DEBUG) {
            result.add(" ");
        }
        return result;
    }

    protected ArrayList<String> loadReg(String reg) {
        ArrayList<String> result = new ArrayList<>();
        int p;
        if (stack.containsKey(reg)) {
            p = stack.get(reg);
            if (Debug.DEBUG) {
                result.add("# loadReg " + reg + " at " + p);
            }
            p = p - sp;
        } else {
            result.add("# wrong!!!");
            result.add("# try to find " + reg);
            result.add(" ");
            return result;
        }
        reg = reg.substring(0, 3);
        result.add("    lw " + reg + " " + p + "($sp)");
        if (Debug.DEBUG) {
            result.add(" ");
        }
        return result;
    }

    protected String getName() {
        return name;
    }

    protected int getSp() {
        return sp;
    }

    private static int count = 0;

    protected static String saveGlobalIdent(String ident, Info info) {
        globals.put(ident, "__global__" + count++);
        if (info.getDecorations().contains("Int")) {
            if (info.getDecorations().contains("Array")) {
                globalTypes.put(ident, "int[]");
            } else {
                globalTypes.put(ident, "int");
            }
        } else if (info.getDecorations().contains("Char")) {
            if (info.getDecorations().contains("Array")) {
                globalTypes.put(ident, "char[]");
            } else {
                globalTypes.put(ident, "char");
            }
        } else {
            globalTypes.put(ident, "char[]");
        }
        return globals.get(ident);
    }

}
