package ir;

import io.OutputSystem;
import semantic.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class IRBlock {

    // BlockName (FuncName@Number Or @global@Number)
    private final String blockName;

    // Comment
    private final String comment;

    // First Block Of This Func(Usually Decl Block)
    private final IRBlock funcBlock;

    // NextBlocks For CFG
    private final ArrayList<IRBlock> nextBlocks = new ArrayList<>();

    // LastBlocks For CFG
    private final ArrayList<IRBlock> lastBlocks = new ArrayList<>();

    // NextBlock In Linear Process
    private IRBlock nextBlock = null;

    // Defined Variables For CSE(Info -> SSA Ident)
    private final HashMap<Info, String> defs = new HashMap<>();

    // FuncName of This Block
    private final String funcName;

    // SymbolTable Of This Block(Might In Blocks)
    private final SymbolTable symbolTable;

    // Can Be Empty (IF STMT BLOCK)
    // Keep Empty Blocks For Branch, Keep New Block For Real Table Dir
    private final ArrayList<Quadruple> quadruples = new ArrayList<>();

    // IRBlock IN A Func
    private final ArrayList<IRBlock> inFunc;

    private static int count = 0;

    /**
     * New Block In Func
     * @param irBlock FatherBlock(In Same Func)
     * @param dir BlockDIr
     * @param comment BlockComment
     */
    public IRBlock(IRBlock irBlock, String dir, String comment) {
        this.funcName = irBlock.funcName;
        this.blockName = IRBlock.getBlockName(funcName);
        this.comment = comment;
        this.funcBlock = irBlock.funcBlock;
        this.symbolTable = SymbolTable.getTable(dir);
        this.inFunc = irBlock.inFunc;
        this.inFunc.add(this);
    }

    /**
     * New Funcs
     * @param funcName FuncName
     * @param dir FuncDir
     */
    public IRBlock(String funcName, String dir) {
        this.blockName = IRBlock.getBlockName(funcName);
        this.funcName = funcName;
        this.funcBlock = this;
        this.comment = "--------New Function " + funcName + " Defined--------";
        this.symbolTable = SymbolTable.getTable(dir);
        addFunc(funcName, this);
        this.inFunc = new ArrayList<>();
        this.inFunc.add(this);
    }

    /**
     * Global Block (Both Decl And Value)
     */
    public IRBlock() {
        this.blockName = IRBlock.getBlockName("@global");
        this.funcName = "@global";
        this.funcBlock = null;
        this.comment = "--------Global--------";
        this.symbolTable = SymbolTable.getTable(SymbolTable.getDir());
        this.inFunc = new ArrayList<>();
        this.inFunc.add(this);
    }

    /**
     * Get Symbol Table Dir
     * @return TableDir
     */
    public String getTableDir() {
        return symbolTable.getName();
    }

    /**
     * Add Next Block That Might Branch Or Jump To(For If And Just After)
     * @param nextBlock NextBlock In CFG
     */
    public void addNextBlock(IRBlock nextBlock) {
        nextBlocks.add(nextBlock);
        nextBlock.addLastBlock(this);
    }

    /**
     * Add Last Block That Might Come From
     * @param lastBlock LastBlock In CFG
     */
    private void addLastBlock(IRBlock lastBlock) {
        lastBlocks.add(lastBlock);
    }

    /**
     * Set Next Block That After This(Linear Process)
     * @param nextBlock NextBlock After
     */
    public void setNextBlock(IRBlock nextBlock) {
        this.nextBlock = nextBlock;
    }

    /**
     * Generate Temp SSA Variables(Should Be Declared After CSE)
     * @return
     */
    public Info genTempInfo() {
        String t = "Temp@" + count++;
        Info info = new TempInfo(t);
        try {
            SemanticAnalyser.addSymbolToTable(symbolTable, t, info);
            push(info);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return info;
    }

    /**
     * Generate In Block Label(Deprecated)
     * @return InBlockLabel
     */
    @Deprecated
    public String genInBlockLabel() {
        return blockName + "_" + count++;
    }

    /**
     * Add Next Indicate
     * @param op Operation
     */
    public void addQuadruple(String op) {
        quadruples.add(new Quadruple(op));
    }

    /**
     * Add Next Indicate
     * @param op Operation
     * @param arg1 Argue1
     */
    public void addQuadruple(String op, String arg1) {
        quadruples.add(new Quadruple(op, arg1));
    }

    /**
     * Add Next Indicate
     * @param op Operation
     * @param arg1 Argue1
     * @param arg2 Argue2
     */
    public void addQuadruple(String op, String arg1, String arg2) {
        quadruples.add(new Quadruple(op, arg1, arg2));
    }

    /**
     * Add Next Indicate
     * @param op Operation
     * @param arg1 Argue1
     * @param arg2 Argue2
     * @param arg3 Argue3
     */
    public void addQuadruple(String op, String arg1, String arg2, String arg3) {
        quadruples.add(new Quadruple(op, arg1, arg2, arg3));
    }

    /**
     * Print This IRBlock To File
     * @param system OutputSystem
     */
    public void output(OutputSystem system) {
        system.println("$begin " + blockName);
        for (IRBlock lastBlock : lastBlocks) {
            system.println("$jump_from " + lastBlock.getBlockName());
        }
        for (IRBlock nextBlock : nextBlocks) {
            system.println("$jump_to " + nextBlock.getBlockName());
        }
        for (Info info : defs.keySet()) {
            system.println("$def " + info.getIdent() + " " + defs.get(info));
        }
        for (IRBlock dom : doms) {
            system.println("$dom " + dom.getBlockName());
        }
        for (IRBlock dom : strictDoms) {
            system.println("$sDom " + dom.getBlockName());
        }
        if (immediateDom != null) {
            system.println("$iDom " + immediateDom.getBlockName());
        }
        for (IRBlock block : df) {
            system.println("$df " + block.getBlockName());
        }
        system.println("# " + comment);
        for (Info info : phis.keySet()) {
            ArrayList<IRBlock> from = phis.get(info);
            system.print("$phi " + phiDefs.get(info) + " ");
            for (IRBlock irBlock : from) {
                system.print("{" + irBlock.defs.get(info) + ", " + irBlock.blockName + "} ");
            }
            system.println();
        }
        for (Quadruple quadruple : quadruples) {
            system.println(quadruple.toString());
        }
        if (Objects.equals(funcBlock, this)) {
            if (stack.containsKey(funcName)) {
                for (Info info : stack.get(funcName)) {
                    if (info.getDecorations().contains("FParam")) {
                        continue;
                    }
                    system.println("    decl " + info.getVarIdent() + " " + (info.getDecorations().contains("Int") ? "Int" : "Char") + " " + info.getLen());
                }
            }
            system.println("    be decl");
        }
        system.println("$end " + blockName);
        system.println();
        system.println();
        if (nextBlock != null) {
            nextBlock.output(system);
        }
    }

    /**
     * If A Statement Want To Use A Variable
     *      1. Never Generated And Used -> Put In uses(For Phi)
     *      2. Never Generated But Used -> Get Same Ident
     *      3. Generated -> Get New Ident(If Generate Next Time, Update defs)
     * @param info
     * @return
     */
    public String use(Info info) {
        return info.getVarIdent();
    }

    /**
     * If A Statement Want To Define A Variable(Or Array)
     *      1. Never Generated -> Generate And Put To defs
     *      2. Generated -> Generate New Ident(SSA) And Replace In defs
     * @param info
     * @return
     */
    public String def(Info info) {
        info.def(this);
        return info.getVarIdent();
    }

    private static final HashMap<String, ArrayList<Info>> stack = new HashMap<>();

    public void push(Info info) {
        if (!stack.containsKey(funcName)) {
            stack.put(funcName, new ArrayList<>());
        }
        stack.get(funcName).add(info);
    }

    public String getBlockName() {
        return blockName;
    }

    public String getComment() {
        return comment;
    }

    public String getFuncName() {
        return funcName;
    }

    public SymbolTable getSymbolTable() {
        return symbolTable;
    }

    public IRBlock getFuncBlock() {
        return funcBlock;
    }

    private static int COUNT = 0;

    private static int STRING_COUNT = 0;

    // FuncBlocks(FuncName -> FirstBlock)
    private static final HashMap<String, IRBlock> funcs = new HashMap<>();

    // GlobalDeclBlock
    private static final IRBlock globalDecl = new IRBlock();

    // GlobalValueBlock
    private static final IRBlock globalValue = new IRBlock();

    /**
     * Get Block Name By Func Name
     * @param funcName FuncName
     * @return BlockName
     */
    private static String getBlockName(String funcName) {
        return funcName + "@" + COUNT++;
    }

    /**
     * Deprecated
     * @param blockName
     * @return
     */
    @Deprecated
    private static String getFuncName(String blockName) {
        Pattern pattern = Pattern.compile("Function::([A-Za-z_0-9]+)(?:/[A-Za-z]+::[0-9]+)*@[0-9]+");
        Matcher matcher = pattern.matcher(blockName);
        // System.out.println(blockName);
        if (matcher.find()) {
            // System.out.println("find");
            return matcher.group(1);
        } else {
            // System.out.println("not find");
            return null;
        }
    }

    /**
     * Generate Global Ident For Strings(In Printf)
     * @return new String Ident(Should Be Decl In Global Decl Block And Value In Value Block)
     */
    public static String genGlobalStringIdent() {
        return "String@" + STRING_COUNT++;
    }

    /**
     * Get Global Decl Block
     * @return GlobalDeclBlock
     */
    public static IRBlock getGlobalDecl() {
        return globalDecl;
    }

    /**
     * Get Global Value Block
     * @return Global Value Block
     */
    public static IRBlock getGlobalValue() {
        return globalValue;
    }

    /**
     * Add New Func Block To Funcs(Should Be Called Only In Func Defs)
     * @param name FuncName
     * @param block FuncBlock
     */
    private static void addFunc(String name, IRBlock block) {
        funcs.put(name, block);
    }

    /**
     * Get Func Block By Func Name(@global To Global Decl)
     * @param name FuncName
     * @return FuncBlock Or GlobalDecl
     */
    public static IRBlock getFunc(String name) {
        if (funcs.containsKey(name)) {
            return funcs.get(name);
        } else {
            return globalDecl;
        }
    }

    /**
     * Print IR To Text(After Optimized)
     * @param system OutputSystem
     */
    public static void printAns(OutputSystem system) {
        system.println("# Global Declarations:");
        system.println("# ------------------------global------------------------");
        system.println("$global start");
        /*
        for (Info info : SymbolTable.getGlobalInfos().values()) {

            if (info instanceof FuncInfo) {
                continue;
            }
            system.println("    decl " + info.getVarIdent() + " " + (info.getVarIdent().contains("Int") ? "Int" : "Char") + " " + info.getLen());
            if (info instanceof ConstInfo) {
                ConstInfo constInfo = (ConstInfo) info;
                system.println("    value " + info.getVarIdent() + " " + constInfo.getValue());
            }
        }

         */
        getGlobalDecl().output(system);

        IRBlock mainBlock = funcs.get("main");
        system.println("# Main Func Declarations:");
        system.println("# ------------------------entry------------------------");
        system.println("$func start");
        mainBlock.output(system);
        system.println("# ------------------------cpBlocks------------------------");
        if (funcCpBlocks.containsKey("main")) {
            for (IRBlock irBlock : funcCpBlocks.get("main")) {
                irBlock.output(system);
            }
        }

        system.println("# ------------------------func------------------------");
        for (String name : funcs.keySet()) {
            if (name == "main") {
                continue;
            }
            system.println("# Func Declaration:");
            IRBlock block = funcs.get(name);
            block.output(system);
            system.println("# ------------------------cpBlocks------------------------");
            if (funcCpBlocks.containsKey(name)) {
                for (IRBlock irBlock : funcCpBlocks.get(name)) {
                    irBlock.output(system);
                }
            }
        }
    }

    public Info getInfo(String varIdent) {
        return SymbolTable.getInfoFromVarIdent(varIdent);
    }

    // Doms For A IRBlock
    private final ArrayList<IRBlock> doms = new ArrayList<>();

    private final ArrayList<IRBlock> strictDoms = new ArrayList<>();

    private IRBlock immediateDom = null;

    private final HashMap<Info, ArrayList<IRBlock>> phis = new HashMap<>();

    private final HashMap<Info, String> phiDefs = new HashMap<>();

    private static final HashMap<String, ArrayList<IRBlock>> funcCpBlocks = new HashMap<>();

    public static void phiAnalyze() {
        for (IRBlock block : funcs.values()) {
            block.domAnalyze();
            block.dfAnalyze();
        }
        HashMap<String, Info> parts = SymbolTable.getPartInfos();
        for (Info info : parts.values()) {
            ArrayList<IRBlock> F = new ArrayList<>();
            ArrayList<IRBlock> W = new ArrayList<>(info.getDefs());
            while (!W.isEmpty()) {
                IRBlock block = W.get(0);
                W.remove(0);
                for (IRBlock irBlock : block.df) {
                    if (!F.contains(irBlock)) {
                        if (!irBlock.phis.containsKey(info)) {
                            irBlock.phis.put(info, new ArrayList<>());
                        }
                        F.add(irBlock);
                        if (!info.getDefs().contains(irBlock)) {
                            if (!W.contains(irBlock)) {
                                W.add(irBlock);
                            }
                        }
                    }
                }
            }
        }
        for (IRBlock func : funcs.values()) {
            func.updateSSA(null);
        }
    }

    public static void removePhis() {
        for (IRBlock block : funcs.values()) {
            block.removePhi();
        }
    }

    private final HashMap<IRBlock, IRBlock> cpBlocks = new HashMap<>();

    public void removePhi() {
        for (Info info : phis.keySet()) {
            IRBlock cpBlock = null;
            ArrayList<IRBlock> blocks = phis.get(info);
            for (IRBlock block : blocks) {
                if (!cpBlocks.containsKey(block)) {
                    cpBlocks.put(block, new IRBlock(block,
                            SymbolTable.getDir(block.getTableDir(), SymbolTable.SymbolType.CpBlock, blockName),
                            "From " + block.blockName + " To " + blockName));
                    cpBlock = cpBlocks.get(block);
                    block.changeJumpToBlock(cpBlock, this);
                }
                cpBlock = cpBlocks.get(block);
                cpBlock.addQuadruple("cp", block.defs.get(info), phiDefs.get(info));

                if (!funcCpBlocks.containsKey(funcName)) {
                    funcCpBlocks.put(funcName, new ArrayList<>());
                }
                if (!funcCpBlocks.get(funcName).contains(cpBlock)) {
                    funcCpBlocks.get(funcName).add(cpBlock);
                }
            }
        }
        for (IRBlock irBlock : cpBlocks.values()) {
            irBlock.addQuadruple("j", blockName);
        }
        if (nextBlock != null) {
            nextBlock.removePhi();
        }
    }

    private void changeJumpToBlock(IRBlock irBlock, IRBlock irBlock1) {
        if (nextBlock.equals(irBlock1)) {
            addQuadruple("j", irBlock.blockName);
        }
        nextBlocks.remove(irBlock1);
        nextBlocks.add(irBlock);
        for (Quadruple quadruple : quadruples) {
            quadruple.changeJump(irBlock.getBlockName(), irBlock1);
        }
    }

    private HashMap<IRBlock, ArrayList<IRBlock>> maps = new HashMap<>();

    public void updateSSA(IRBlock irBlock) {
        if (maps.containsKey(irBlock)) {
            if (maps.get(irBlock).contains(this)) {
                return;
            }
            maps.get(irBlock).add(this);
        } else {
            maps.put(irBlock, new ArrayList<>());
            maps.get(irBlock).add(this);
        }

        for (Info info : phis.keySet()) {
            if (irBlock.defs.containsKey(info)) {
                phis.get(info).add(irBlock);
            }
            if (defs.containsKey(info)) {
                continue;
            }
            defs.put(info, info.genSSA());
            phiDefs.put(info, defs.get(info));
        }
        if (irBlock != null) {
            for (Info info : irBlock.defs.keySet()) {
                if (!defs.containsKey(info)) {
                    defs.put(info, irBlock.defs.get(info));
                }
            }
        }
        for (Quadruple quadruple : quadruples) {
            quadruple.changeSSA(this);
        }
        for (IRBlock irBlock1 : nextBlocks) {
            irBlock1.updateSSA(this);
        }
    }

    public String defSSA(String varIdent) {
        Info info = SymbolTable.getInfoFromVarIdent(varIdent);
        if (info.getDecorations().contains("Global")) {
            return varIdent;
        }
        if (defs.containsKey(info)) {
            defs.replace(info, info.genSSA());
        } else {
            defs.put(info, info.genSSA());
        }
        return defs.get(info);
    }

    public String useSSA(String varIdent) {
        Info info = SymbolTable.getInfoFromVarIdent(varIdent);
        if (info.getDecorations().contains("Global")) {
            return varIdent;
        }
        if (defs.containsKey(info)) {
            return defs.get(info);
        }
        return defSSA(varIdent);
    }

    /**
     * Dom Analyzing For Each Func
     */
    public void domAnalyze() {
        for (IRBlock block : inFunc) {
            block.doms.add(block);
        }
        boolean flag = true;
        while (flag) {
            flag = false;
            for (IRBlock block : inFunc) {
                if (block.lastBlocks.isEmpty()) {
                    continue;
                }
                ArrayList<IRBlock> dom = (ArrayList<IRBlock>) block.lastBlocks.get(0).doms.clone(); // Get First Prev
                for (IRBlock block1 : block.lastBlocks) {
                    ArrayList<IRBlock> dom1 = block1.doms;
                    dom.removeIf(block2 -> !dom1.contains(block2)); // Union
                }
                // IF Has New Dom Block
                for (IRBlock block1 : dom) {
                    if (!block.doms.contains(block1)) {
                        block.doms.add(block1);
                        flag = true;
                    }
                }
            }

        }
        // sDom
        for (IRBlock block : inFunc) {
            block.strictDoms.addAll(block.doms);
            block.strictDoms.remove(block);
        }
        // iDom
        for (IRBlock block : inFunc) {
            flag = true;
            for (IRBlock block1 : block.strictDoms) {
                for (IRBlock block2 : block.strictDoms) {
                    if (block1.equals(block2)) {
                        continue;
                    }
                    if (block2.strictDoms.contains(block1)) {
                        flag = false;
                        break;
                    }
                }
                if (flag) {
                    block.immediateDom = block1;
                    break;
                }
            }
        }
    }

    // DF
    private final ArrayList<IRBlock> df = new ArrayList<>();

    // DF Analyzing For A Func
    public void dfAnalyze() {
        for (IRBlock block : inFunc) {
            // b -> b1
            for (IRBlock block1 : block.nextBlocks) {
                IRBlock b = block;
                while (b != null && !block1.strictDoms.contains(b)) {
                    if (!b.df.contains(block1)) {
                        b.df.add(block1);
                    }
                    b = b.immediateDom;
                }
            }
        }
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof IRBlock) {
            IRBlock irBlock = (IRBlock) o;
            return irBlock.blockName.equals(blockName);
        }
        return false;
    }
}
