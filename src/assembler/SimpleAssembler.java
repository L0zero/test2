package assembler;

import io.InputSystem;
import io.OutputSystem;
import debug.Debug;
import semantic.ConstInfo;
import semantic.FuncInfo;
import semantic.Info;
import semantic.SymbolTable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SimpleAssembler implements Assembler {

    private final static AbstractStack rootStack = new AbstractStack();

    private static AbstractStack stack;

    private static int beginFunc = 0;

    private static int beforeCall = 0;

    private static int rparas = 0;

    private static int funcSave = 0;

    private static int callerSave = 0;

    private static BasicBlock block;

    private static ArrayList<String> regs = new ArrayList<>();

    public SimpleAssembler() {

    }

    @Override
    public void assemble(InputSystem inputSystem, OutputSystem outputSystem) {

        if (Debug.DEBUG) {
            // System.out.println("Assembling!");
        }

        String code;
        ArrayList<String> result = new ArrayList<>();
        result.add(".data");
        result.addAll(globalDecl());
        result.add("    null: .word 0");
        result.add(".text");
        while (true) {
            code = inputSystem.getLine();
            if (code == null) break;
            if (code.isEmpty()) continue;
            if (code.charAt(0) == '#') {
                if (Debug.DEBUG) {
                    result.add(code);
                }
            } else if (code.charAt(0) == '$') {
                String[] decls = code.split(" ");
                if (decls.length >= 2) {
                    switch (decls[0]) {
                        case "$begin": {
                            if (Debug.DEBUG) {
                                result.add("# New Block:");
                            }
                            block = new BasicBlock(decls[1]);
                            result.add(genLabel(decls[1]) + ":");
                            break;
                        }
                        case "$end": {
                            if (Debug.DEBUG) {
                                result.add("# End Block:");
                            }
                            result.add("# " + decls[1]);
                            break;
                        }
                        case "$jump_to": {
                            break;
                        }
                        case "$jump_from": {
                            break;
                        }
                        case "$global": {
                            stack = rootStack;
                            break;
                        }
                        case "$func": {
                            break;
                        }
                        default:
                            break;
                    }
                }
            } else {
                if (Debug.DEBUG) {
                    result.add("# " + code);
                }
                String[] words = ProSplit(code);
                result.addAll(translate(words));
            }
        }
        outputSystem.println(result);
        outputSystem.println(printf);
        outputSystem.println(getint);
        outputSystem.println(getchar);
    }

    private static String[] ProSplit(String code) {
        String[] words = new String[4];
        StringBuilder builder = new StringBuilder();
        int pos = 0;
        for (int i = 0; i < code.length(); i++) {
            char c = code.charAt(i);
            if (c == '"') {
                do {
                    builder.append(c);
                    c = code.charAt(++i);
                } while (c != '"');
                builder.append(c);
                words[pos++] = builder.toString();
                builder = new StringBuilder();
            } else if (c == '{') {
                do {
                    builder.append(c);
                    c = code.charAt(++i);
                } while (c != '}');
                builder.append(c);
                words[pos++] = builder.toString();
                builder = new StringBuilder();
            } else if (c == ' ') {
                if (builder.length() > 0) {
                    words[pos++] = builder.toString();
                    builder = new StringBuilder();
                }
            } else {
                builder.append(c);
            }
        }
        if (builder.length() > 0) {
            words[pos] = builder.toString();
        }
        return words;
    }

    private static ArrayList<String> globalDecl() {
        ArrayList<String> result = new ArrayList<>();
        Collection<Info> infos = SymbolTable.getGlobalInfos().values();
        for (Info info : infos) {
            if (info instanceof FuncInfo) {
                continue;
            }
            String id = AbstractStack.saveGlobalIdent(info.getVarIdent(), info);
            if (info.getDecorations().contains("Int")) {
                if (info.getDecorations().contains("Array")) {
                    if (info.getDecorations().contains("Const")) {
                        StringBuilder sb = new StringBuilder();
                        sb.append("    ");
                        sb.append(id);
                        sb.append(": .word ");
                        ConstInfo constInfo = (ConstInfo) info;
                        int l = constInfo.getLen();
                        for (int i = 0; i < l; i++) {
                            sb.append(constInfo.getValue(i));
                            sb.append(", ");
                        }
                        sb.delete(sb.length() - 2, sb.length());
                        result.add(sb.toString());
                    } else {
                        result.add("    " + id + ": .word 0:" + info.getLen());
                    }
                } else {
                    if (info.getDecorations().contains("Const")) {
                        ConstInfo constInfo = (ConstInfo) info;
                        result.add("    " + id + ": .word " + constInfo.getValue());
                    } else {
                        result.add("    " + id + ": .word 0");
                    }
                }
            } else if (info.getDecorations().contains("Char")) {
                if (info.getDecorations().contains("Array")) {
                    if (info.getDecorations().contains("Const")) {
                        StringBuilder sb = new StringBuilder();
                        sb.append("    ");
                        sb.append(id);
                        sb.append(": .byte ");
                        ConstInfo constInfo = (ConstInfo) info;
                        int l = constInfo.getLen();
                        for (int i = 0; i < l; i++) {
                            sb.append(constInfo.getValue(i));
                            sb.append(", ");
                        }
                        sb.delete(sb.length() - 2, sb.length());
                        result.add(sb.toString());
                    } else {
                        result.add("    " + id + ": .byte 0:" + info.getLen());
                    }
                } else {
                    if (info.getDecorations().contains("Const")) {
                        ConstInfo constInfo = (ConstInfo) info;
                        result.add("    " + id + ": .byte " + constInfo.getValue());
                    } else {
                        result.add("    " + id + ": .byte 0");
                    }
                }
            } else {
                result.add("    " + id + ": .asciiz " + ((ConstInfo) info).getValue());
            }
        }
        String id = AbstractStack.saveGlobalIdent(Info.CondTempInfo.getVarIdent(), Info.CondTempInfo);
        result.add("    " + id + ": .word 0");
        return result;
    }

    private static ArrayList<String> translate(String[] words) {
        ArrayList<String> result = new ArrayList<>();
        String a, b, c;
        switch (words[0]) {
            case "im":
                a = words[1];
                b = words[2];
                result.addAll(getIdent(a, stack));
                result.addAll(saveIdent(b, stack));
                result.add(" ");
                break;
            case "add":
                a = words[1];
                b = words[2];
                c = words[3];
                result.addAll(getIdent(a, stack));
                result.addAll(move("$s1", "$s0"));
                result.addAll(getIdent(b, stack));
                result.addAll(move("$s2", "$s0"));
                result.add("addu $s0 $s1 $s2");
                result.addAll(saveIdent(c, stack));
                result.add(" ");
                break;
            case "sub":
                a = words[1];
                b = words[2];
                c = words[3];
                result.addAll(getIdent(a, stack));
                result.addAll(move("$s1", "$s0"));
                result.addAll(getIdent(b, stack));
                result.addAll(move("$s2", "$s0"));
                result.add("subu $s0 $s1 $s2");
                result.addAll(saveIdent(c, stack));
                result.add(" ");
                break;
            case "mult":
                a = words[1];
                b = words[2];
                c = words[3];
                result.addAll(getIdent(a, stack));
                result.addAll(move("$s1", "$s0"));
                result.addAll(getIdent(b, stack));
                result.addAll(move("$s2", "$s0"));
                result.add("mul $s0 $s1 $s2");
                // result.add("mflo $s0");
                result.addAll(saveIdent(c, stack));
                result.add(" ");
                break;
            case "div":
                a = words[1];
                b = words[2];
                c = words[3];
                result.addAll(getIdent(a, stack));
                result.addAll(move("$s1", "$s0"));
                result.addAll(getIdent(b, stack));
                result.addAll(move("$s2", "$s0"));
                result.add("div $s1 $s2");
                result.add("mflo $s0");
                result.addAll(saveIdent(c, stack));
                result.add(" ");
                break;
            case "mod":
                a = words[1];
                b = words[2];
                c = words[3];
                result.addAll(getIdent(a, stack));
                result.addAll(move("$s1", "$s0"));
                result.addAll(getIdent(b, stack));
                result.addAll(move("$s2", "$s0"));
                result.add("div $s1 $s2");
                result.add("mfhi $s0");
                result.addAll(saveIdent(c, stack));
                result.add(" ");
                break;
            case "gre":
                a = words[1];
                b = words[2];
                c = words[3];
                result.addAll(getIdent(a, stack));
                result.addAll(move("$s1", "$s0"));
                result.addAll(getIdent(b, stack));
                result.addAll(move("$s2", "$s0"));
                result.add("slt $s0 $s2 $s1");
                result.addAll(saveIdent(c, stack));
                result.add(" ");
                break;
            case "lss":
                a = words[1];
                b = words[2];
                c = words[3];
                result.addAll(getIdent(a, stack));
                result.addAll(move("$s1", "$s0"));
                result.addAll(getIdent(b, stack));
                result.addAll(move("$s2", "$s0"));
                result.add("slt $s0 $s1 $s2");
                result.addAll(saveIdent(c, stack));
                break;
            case "geq":
                a = words[1];
                b = words[2];
                c = words[3];
                result.addAll(getIdent(a, stack));
                result.addAll(move("$s1", "$s0"));
                result.addAll(getIdent(b, stack));
                result.addAll(move("$s2", "$s0"));
                result.add("slt $s0 $s1 $s2");
                result.add("xori $s0 $s0 1");
                result.addAll(saveIdent(c, stack));
                result.add(" ");
                break;
            case "leq":
                a = words[1];
                b = words[2];
                c = words[3];
                result.addAll(getIdent(a, stack));
                result.addAll(move("$s1", "$s0"));
                result.addAll(getIdent(b, stack));
                result.addAll(move("$s2", "$s0"));
                result.add("slt $s0 $s2 $s1");
                result.add("xori $s0 $s0 1");
                result.addAll(saveIdent(c, stack));
                result.add(" ");
                break;
            case "eql":
                a = words[1];
                b = words[2];
                c = words[3];
                result.addAll(getIdent(a, stack));
                result.addAll(move("$s1", "$s0"));
                result.addAll(getIdent(b, stack));
                result.addAll(move("$s2", "$s0"));
                result.add("xor $s0 $s1 $s2");
                result.add("sltiu $s0 $s0 1");
                result.addAll(saveIdent(c, stack));
                result.add(" ");
                break;
            case "neq":
                a = words[1];
                b = words[2];
                c = words[3];
                result.addAll(getIdent(a, stack));
                result.addAll(move("$s1", "$s0"));
                result.addAll(getIdent(b, stack));
                result.addAll(move("$s2", "$s0"));
                result.add("xor $s0 $s1 $s2");
                result.add("sltiu $s0 $s0 1");
                result.add("xori $s0 $s0 1");
                result.addAll(saveIdent(c, stack));
                result.add(" ");
                break;
            case "and":
                a = words[1];
                b = words[2];
                c = words[3];
                result.addAll(getIdent(a, stack));
                result.addAll(move("$s1", "$s0"));
                result.addAll(getIdent(b, stack));
                result.addAll(move("$s2", "$s0"));
                result.add("and $s0 $s1 $s2");
                result.addAll(saveIdent(c, stack));
                result.add(" ");
                break;
            case "or":
                a = words[1];
                b = words[2];
                c = words[3];
                result.addAll(getIdent(a, stack));
                result.addAll(move("$s1", "$s0"));
                result.addAll(getIdent(b, stack));
                result.addAll(move("$s2", "$s0"));
                result.add("or $s0 $s1 $s2");
                result.addAll(saveIdent(c, stack));
                result.add(" ");
                break;
            case "not":
                a = words[1];
                c = words[2];
                result.addAll(getIdent(a, stack));
                result.add("sltiu $s0 $s0 1");
                result.addAll(saveIdent(c, stack));
                result.add(" ");
                break;
            case "decl":
                a = words[1];
                b = words[2];
                c = words[3];
                int len = Integer.parseInt(c);
                if (len == 0) {
                    if (Debug.DEBUG) {
                        result.add("# decl int");
                    }
                    result.addAll(stack.push(a));
                } else {
                    if (Debug.DEBUG) {
                        result.add("# decl int[] or char[]");
                    }
                    result.addAll(stack.push(a, b.equals("Int"), len));
                }
                result.add(" ");
                break;
            case "values":
                a = words[1];
                b = words[2];
                if (isNumeric(b)) {
                    if (Debug.DEBUG) {
                        result.add("# number " + b);
                    }
                    result.add("    li $s0 " + b);
                    result.addAll(saveIdent(a, stack));
                } else if (b.charAt(0) == '{') {
                    if (Debug.DEBUG) {
                        result.add("# array " + b);
                    }
                    b = b.substring(1, b.length() - 1);
                    String[] nums = b.split(",");
                    int i = 0;
                    for (String num : nums) {
                        result.addAll(getIdent(num, stack));
                        result.addAll(put(a, stack, i++));
                    }
                } else if (b.startsWith("\"")) {
                    if (Debug.DEBUG) {
                        result.add("# string " + b);
                    }
                    b = b.substring(1, b.length() - 1);
                    int j = 0;
                    for (int i = 0; i < b.length(); i++) {
                        if (b.charAt(i) == '\\') {
                            i++;
                            int ch = getInt(b.charAt(i));
                            result.add("    li $s0 " + ch);
                            result.addAll(put(a, stack, j++));
                        } else {
                            result.add("    li $s0 " + (int) b.charAt(i));
                            result.addAll(put(a, stack, j++));
                        }
                    }
                    result.add("    li $s0 0");
                    result.addAll(put(a, stack, j));
                } else {
                    result.addAll(getIdent(b, stack));
                    result.addAll(saveIdent(a, stack));
                }
                result.add(" ");


                break;
            case "def":
                a = words[1];
                stack = new AbstractStack(rootStack, a);
                if (Debug.DEBUG) {
                    result.add("# new AS: " + a);
                }
                result.add(genLabel("entry@" + a) + ":");
                result.add(" ");
                break;
            case "fpara":
                a = words[1];
                b = words[2];
                c = words[3];
                int dim = Integer.parseInt(b);
                if (dim == 0) {
                    if (Debug.DEBUG) {
                        result.add("# fpara int");
                    }
                    result.addAll(stack.fPush(a));
                } else {
                    if (Debug.DEBUG) {
                        result.add("# fpara int* or char*");
                    }
                    result.addAll(stack.fPush(a, c.equals("Int")));
                }
                result.add(" ");
                break;
            case "ret":
                a = words[1];
                if (!a.equals("none")) {
                    if (Debug.DEBUG) {
                        result.add("# has return value");
                    }
                    result.addAll(getIdent(a, stack));
                    result.addAll(move("$v0", "$s0"));
                }
                result.add("    j " + genLabel("exit@" + stack.getName()));
                result.add(" ");
                break;
            case "exit":
                String func = stack.getName();
                result.addAll(funcLoadReg(stack, regs));
                int p = stack.getSp();
                result.addAll(stack.pop(beginFunc));
                result.add("    addiu $sp $sp " + (beginFunc - p));
                if (func.equals("main")) {
                    if (Debug.DEBUG) {
                        result.add("# main func exit");
                    }
                    result.add("    li $v0 10");
                    result.add("    syscall");
                } else {
                    result.add("    jr $ra");
                }
                result.add(" ");
                break;
            case "rpara":
                a = words[1];
                String rp = "rpara@" + rparas++;
                if (Debug.DEBUG) {
                    result.add("# new rpara " + rp);
                }
                result.addAll(getIdent(a, stack));
                result.addAll(stack.push(rp));
                result.addAll(saveIdent(rp, stack));
                if (Debug.DEBUG) {
                    result.add("# AS $sp at " + stack.getSp());
                }
                result.add(" ");
                break;
            case "call":
                a = words[1];
                result.add("    jal " + genLabel("entry@" + a));
                result.addAll(callerLoadReg(stack));
                int pp = stack.getSp();
                result.add("    addiu $sp $sp " + (beforeCall - pp));
                result.addAll(stack.pop(beforeCall));
                result.add(" ");
                if (Debug.DEBUG) {
                    result.add("# afterCall: " + beforeCall);
                }
                break;
            case "getrt":
                a = words[1];
                result.addAll(move("$s0", "$v0"));
                result.addAll(saveIdent(a, stack));
                result.add(" ");
                break;
            case "bez":
                a = words[1];
                b = words[2];
                result.addAll(getIdent(a, stack));
                result.add("    beqz $s0 " + genLabel(b));
                result.add(" ");
                break;
            case "j":
                a = words[1];
                result.add("    j " + genLabel(a));
                result.add(" ");
                break;
            case "label":
                a = words[1];
                result.add(genLabel(a) + ":");
                result.add(" ");
                break;
            case "be":
                if (words[1].equals("fpara")) {
                    if (Debug.DEBUG) {
                        result.add("# after fpara");
                    }
                    if (Debug.DEBUG) {
                        result.add("# AS $sp at " + stack.getSp());
                        result.add(" ");
                    }
                    beginFunc = stack.getSp();
                }

                if (words[1].equals("decl")) {
                    if (Debug.DEBUG) {
                        result.add("# after decls");
                        result.add("# AS $sp at " + stack.getSp());
                    }
                    analyzeRegs(regs);
                    result.addAll(funcPushReg(stack, regs));
                    if (Debug.DEBUG) {
                        result.add("# AS $sp at " + stack.getSp());
                        result.add(" ");
                    }
                    result.addAll(funcSaveReg(stack, regs));
                }

                if (words[1].equals("rpara")) {
                    beforeCall = stack.getSp();
                    if (Debug.DEBUG) {
                        result.add("# before rpara");
                        result.add("# beforeCall: " + beforeCall);
                    }
                    result.addAll(callerSaveReg(stack));
                }
                break;
            case "put":
                a = words[1];
                b = words[2];
                c = words[3];
                result.addAll(getIdent(c, stack));
                result.addAll(move("$s1", "$s0"));
                result.addAll(getIdent(b, stack));
                result.addAll(put(a, stack));
                break;
            case "get":
                a = words[1];
                b = words[2];
                c = words[3];
                result.addAll(getIdent(b, stack));
                result.addAll(get(a, stack));
                result.addAll(saveIdent(c, stack));
            default:
                break;
        }
        return result;
    }

    private static boolean isNumeric(String str) {
        // 正则表达式，匹配整数或小数
        String regex = "^[-+]?\\d*\\.?\\d+$";
        return str.matches(regex);
    }

    private static boolean isArray(String str) {
        String regex = "^.+\\[.+?]$";
        return str.matches(regex);
    }

    private static String getIndex(String str) {
        String regex = "\\[(.*)]";
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(str);
        if (matcher.find()) {
            String i = matcher.group(1);
            return i;
        }
        return "0";
    }

    private static int getInt(char c) {
        switch (c) {
            case 'a':
                return 7;
            case 'b':
                return 8;
            case 't':
                return 9;
            case 'n':
                return 10;
            case 'v':
                return 11;
            case 'f':
                return 12;
            case '"':
                return 34;
            case '\'':
                return 39;
            case '\\':
                return 92;
            case '0':
                return 0;
            default:
                return c;

        }
    }

    private static final HashMap<String, String> labels = new HashMap<>();

    private static int labelCount = 0;

    private static String genLabel(String s) {
        if (labels.containsKey(s)) {
            return labels.get(s);
        } else {
            labels.put(s, "__" + labelCount++);
            return labels.get(s);
        }
    }

    // $s0 = ident (ident or ident[pos] or ident[ident] or number)
    // use $s0
    private static ArrayList<String> getIdent(String ident, AbstractStack stack) {
        ArrayList<String> result = new ArrayList<>();
        if (isNumeric(ident)) {
            result.add("    li $s0 " + ident);
        } else {
            ident = toVarIdent(ident);
            result.addAll(stack.getPara(ident));
        }
        return result;
    }

    // ident = $s0
    // use $s0 $s1 $s2
    private static ArrayList<String> saveIdent(String ident, AbstractStack stack) {
        ident = toVarIdent(ident);
        return new ArrayList<>(stack.saveIdent(ident));
    }

    private static ArrayList<String> put(String ident, AbstractStack stack, int pos) {
        ident = toVarIdent(ident);
        return new ArrayList<>(stack.put(ident, pos));
    }
    private static ArrayList<String> put(String ident, AbstractStack stack) {
        ident = toVarIdent(ident);
        return new ArrayList<>(stack.put(ident));
    }

    private static ArrayList<String> get(String ident, AbstractStack stack, int pos) {
        ident = toVarIdent(ident);
        return new ArrayList<>(stack.get(ident, pos));
    }

    private static ArrayList<String> get(String ident, AbstractStack stack) {
        ident = toVarIdent(ident);
        return new ArrayList<>(stack.get(ident));
    }

    private static String toVarIdent(String ident) {
        if (ident.startsWith("Var@")) {
            return ident.substring(0, ident.lastIndexOf('@'));
        } else if (ident.startsWith("Temp@")) {
            return ident.substring(0, ident.lastIndexOf('@'));
        } else {
            return ident;
        }
    }

    // reg1 <== reg2
    private static ArrayList<String> move(String reg1, String reg2) {
        ArrayList<String> result = new ArrayList<>();
        result.add("    addiu " + reg1 + " " + reg2 + " 0");
        return result;
    }

    private static ArrayList<String> funcPushReg(AbstractStack stack, ArrayList<String> regs) {
        ArrayList<String> result = new ArrayList<>();
        int num = ++funcSave;
        for (String reg : regs) {
            result.addAll(stack.push(reg + "@func_" + num));
        }
        return result;
    }

    private static ArrayList<String> funcSaveReg(AbstractStack stack, ArrayList<String> regs) {
        ArrayList<String> result = new ArrayList<>();
        int num = funcSave;
        for (String reg : regs) {
            result.addAll(stack.saveReg(reg + "@func_" + num));
        }
        return result;
    }

    private static ArrayList<String> funcLoadReg(AbstractStack stack, ArrayList<String> regs) {
        ArrayList<String> result = new ArrayList<>();
        int num = funcSave;
        for (String reg : regs) {
            result.addAll(stack.loadReg(reg + "@func_" + num));
        }
        return result;
    }

    private static void analyzeRegs(ArrayList<String> regs) {
        regs.clear();
        regs.add("$t0");
        regs.add("$t1");
        regs.add("$t2");
        regs.add("$t3");
        regs.add("$ra");
    }

    private static final String[] GLOBAL_REGS = {
            "$s0",
            "$s1",
            "$s2",
            "$s3",
            "$s4",
            "$s5",
            "$s6",
            "$s7",
    };

    private static ArrayList<String> callerSaveReg(AbstractStack stack) {
        ArrayList<String> result = new ArrayList<>();
        int num = ++callerSave;
        for (String reg : GLOBAL_REGS) {
            result.addAll(stack.push(reg + "@caller_" + num));
        }
        for (String reg : GLOBAL_REGS) {
            result.addAll(stack.saveReg(reg + "@caller_" + num));
        }
        return result;
    }

    private static ArrayList<String> callerLoadReg(AbstractStack stack) {
        ArrayList<String> result = new ArrayList<>();
        int num = callerSave;
        for (String reg : GLOBAL_REGS) {
            result.addAll(stack.loadReg(reg + "@caller_" + num));
        }
        return result;
    }

    private static String printf = genLabel("entry@printf") + ":\n" +
            "addiu $sp $sp -20\n" +
            "sw $ra 16($sp)\n" +
            "sw $t0 12($sp)\n" +
            "sw $t1 8($sp)\n" +
            "sw $t2 4($sp)\n" +
            "sw $t3 0($sp)\n" +
            "\n" +
            "lw $t0 20($sp) # num\n" +
            "sll $t0 $t0 2 \n" +
            "addu $t0 $t0 $sp\n" +
            "addiu $t0 $t0 20\n" +
            "lw $s1 0($t0) # char *s\n" +
            "li $t1 37 # %\n" +
            "li $t2 99 # c\n" +
            "li $t3 100 # d\n" +
            "\n" +
            "__printf__loop1:\n" +
            "lbu $a0 0($s1) # *s\n" +
            "beq $0 $a0 __printf__loop__end # *s == 0 return\n" +
            "beq $t1 $a0 __printf__per \n" +
            "jal __putchar__\n" +
            "\n" +
            "__printf__loop2:\n" +
            "addiu $s1 $s1 1 # s++\n" +
            "j __printf__loop1\n" +
            "\n" +
            "__printf__per: # *s == '%'\n" +
            "addiu $s1 $s1 1 # s++\n" +
            "lbu $a0 0($s1) # *s\n" +
            "beq $0 $a0 __printf__per__end # *s == 0\n" +
            "beq $t2 $a0 __printf__c # *s == 'c'\n" +
            "beq $t3 $a0 __printf__d # *s == 'd' \n" +
            "\n" +
            "addu $a1 $a0 $zero\n" +
            "li $a0 37 \n" +
            "jal __putchar__ # put %\n" +
            "\n" +
            "addu $a0 $a1 $zero\n" +
            "jal __putchar__ # put *s (not c, d)\n" +
            "\n" +
            "j __printf__loop2\n" +
            "\n" +
            "__printf__per__end:\n" +
            "li $a0 37 \n" +
            "jal __putchar__ # put %\n" +
            "\n" +
            "j __printf__loop__end\n" +
            "\n" +
            "__printf__c:\n" +
            "addiu $t0 $t0 -4\n" +
            "lw $a0 0($t0)\n" +
            "jal __putchar__ \n" +
            "\n" +
            "j __printf__loop2\n" +
            "\n" +
            "__printf__d:\n" +
            "addiu $t0 $t0 -4\n" +
            "lw $a0 0($t0)\n" +
            "jal __putint__ \n" +
            "\n" +
            "j __printf__loop2\n" +
            "\n" +
            "__printf__loop__end:\n" +
            "lw $ra 16($sp)\n" +
            "lw $t0 12($sp)\n" +
            "lw $t1 8($sp)\n" +
            "lw $t2 4($sp)\n" +
            "lw $t3 0($sp)\n" +
            "\n" +
            "addiu $sp $sp 20\n" +
            "jr $ra\n" +
            "\n" +
            "__putchar__:\n" +
            "li $v0 11\n" +
            "syscall \n" +
            "jr $ra\n" +
            "\n" +
            "__putint__:\n" +
            "li $v0 1\n" +
            "syscall\n" +
            "jr $ra\n\n";

    private static String getint = genLabel("entry@getint") + ":\n" +
            "li $v0 5\n" +
            "syscall\n" +
            "jr $ra\n\n";

    private static String getchar = genLabel("entry@getchar") + ":\n" +
            "li $v0 12\n" +
            "syscall\n" +
            "jr $ra\n\n";
}
