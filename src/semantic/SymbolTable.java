package semantic;

import error.NameExistsException;
import io.OutputSystem;
import ir.IRBlock;

import java.util.ArrayList;
import java.util.HashMap;

public class SymbolTable {

    // Table Name Also Table Dir
    private final String name;

    // Table ID
    private int id;

    // Table(Ident -> Info)
    private final HashMap<String, Info> table = new HashMap<>();

    // Ident In This Table For Output Ans
    private final ArrayList<String> list = new ArrayList<>();

    protected SymbolTable(String dir) {
        this.name = dir;
        this.id = ID++;
    }

    /**
     * Print Symbol Table Contents To Text
     * @param system OutputSystem
     */
    protected static void print(OutputSystem system) {
        for (String s : TABLES_NAMES) {
            SymbolTable t = TABLES.get(s);
            system.print(String.valueOf(t.getId()));
            system.print(" ");
            system.print(t.getName());
            system.print("\n");
        }
    }

    /**
     * Print Symbol Table Contents To Text
     * @param system OutputSystem
     */
    protected static void printAns(OutputSystem system) {
        int i = 0;
        for (String s : TABLES_NAMES) {
            i++;
            SymbolTable t = TABLES.get(s);
            for (String ident : t.list) {
                system.print(String.valueOf(i));
                system.print(" ");
                system.print(ident);
                system.print(" ");
                system.print(t.table.get(ident).toString());
                system.print("\n");
            }
        }
    }

    /**
     * Add New Ident To Table
     * @param name Ident
     * @param info Info
     * @throws NameExistsException Has Same Ident In The Table(Redeclaration)
     */
    protected void addSymbol(String name, Info info) throws NameExistsException {
        if (table.containsKey(name)) {
            throw new NameExistsException();
        }
        table.put(name, info);
        list.add(name);
        if (this.name.equals("global")) {
            info.addDecoration("Global");
            GLOBAL_INFOS.put(info.getVarIdent(), info);
        } else {
            if (info.getDecorations().contains("Const")) {
                info.addDecoration("Global");
                GLOBAL_INFOS.put(info.getVarIdent(), info);
                return;
            }
            PART_INFOS.put(info.getVarIdent(), info);
        }
    }

    // For Generate Table ID
    private static int ID = 1;

    // Map(Dir -> Table)
    private static final HashMap<String, SymbolTable> TABLES = new HashMap<>();

    // List Of Dirs
    private static final ArrayList<String> TABLES_NAMES = new ArrayList<>();

    private static final HashMap<String, Info> GLOBAL_INFOS = new HashMap<>();

    private static final HashMap<String, Info> PART_INFOS = new HashMap<>();

    /**
     * Get Table By Table Name(Dir)
     * @param name TableName(Dir)
     * @return SymbolTable
     */
    public static SymbolTable getTable(String name) {
        if (TABLES.containsKey(name)) {
            return TABLES.get(name);
        }
        return null;
    }

    /**
     * Add Table To TABLES And TABLE_NAMES
     * @param table SymbolTable
     */
    protected static void addTable(SymbolTable table) {
        TABLES.put(table.name, table);
        TABLES_NAMES.add(table.name);
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    /**
     * Get Info From Ident(From Top Table To Search)
     * @param name Ident
     * @return Info
     */
    public Info getInfo(String name) {
        if (table.containsKey(name)) {
            return table.get(name);
        }
        else return getInfoFromFather(name);
    }

    /**
     * Get Info From Father Table(Prepared For getInfo)
     * @param name Ident
     * @return Info
     */
    private Info getInfoFromFather(String name) {
        String dir = getFatherName(this.name);
        if (dir == null) {
            return null;
        }
        SymbolTable t = TABLES.get(dir);
        if (t == null) {
            return null;
        }
        return t.getInfo(name);
    }

    /**
     * Get Dir For Global
     * @return "global"
     */
    public static String getDir() {
        return "global";
    }

    /**
     * Get Dir For Block(Func, Block, Global)
     * @param dir Father Dir(Func For global, Block For Funcs & Blocks)
     * @param type BlockType(Func, Block, Global)
     * @param name BlockName(FuncName, BlockName)
     * @return TableDir(TableName)
     */
    public static String getDir(String dir, SymbolType type, String name) {
        return dir + "/" + type + "::" + name;
    }

    /**
     * Get Father Table Dir By Parse Dir
     * @param dir Dir(Name)
     * @return Father Dir
     */
    protected static String getFatherName(String dir) {
        int index = dir.lastIndexOf('/');
        if (index == -1) {
            return null;
        } else {
            return dir.substring(0, index);
        }
    }

    public static HashMap<String, Info> getGlobalInfos() {
        return GLOBAL_INFOS;
    }

    public static HashMap<String, Info> getPartInfos() {
        return PART_INFOS;
    }

    public static Info getInfoFromVarIdent(String ident) {
        if (!PART_INFOS.containsValue(Info.CondTempInfo)) {
            PART_INFOS.put(Info.CondTempInfo.getVarIdent(), Info.CondTempInfo);
        }
        if (PART_INFOS.containsKey(ident)) {
            return PART_INFOS.get(ident);
        } else if (GLOBAL_INFOS.containsKey(ident)) {
            return GLOBAL_INFOS.get(ident);
        }
        return null;
    }

    public static void printAllVarIdent(OutputSystem system) {
        for (String s : GLOBAL_INFOS.keySet()) {
            system.println(s);
        }
        for (String s : PART_INFOS.keySet()) {
            system.println(s);
        }
    }

    public enum SymbolType {
        Function, Block, Global, CpBlock
    }
}


