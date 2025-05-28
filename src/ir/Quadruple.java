package ir;

import java.util.ArrayList;

public class Quadruple {

    private boolean ssa = false;

    private final String op;

    private String arg1;

    private String arg2;

    private String arg3;

    protected Quadruple(String op, String arg1, String arg2, String arg3) {
        this.op = op;
        this.arg1 = arg1;
        this.arg2 = arg2;
        this.arg3 = arg3;
    }

    protected Quadruple(String op, String arg1, String arg2) {
        this.op = op;
        this.arg1 = arg1;
        this.arg2 = arg2;
        this.arg3 = "";
    }

    protected Quadruple(String op, String arg1) {
        this.op = op;
        this.arg1 = arg1;
        this.arg2 = "";
        this.arg3 = "";
    }

    protected Quadruple(String op) {
        this.op = op;
        this.arg1 = "";
        this.arg2 = "";
        this.arg3 = "";
    }

    @Override
    public String toString() {
        return "    " + op + " " + arg1 + " " + arg2 + " " + arg3;
    }

    public void changeSSA(IRBlock irBlock) {
        if (ssa) {
            return;
        }
        ssa = true;
        switch (op) {
            case "not":
            case "im": {
                arg1 = use(arg1, irBlock);
                arg2 = def(arg2, irBlock);
                break;
            }
            case "sub":
            case "mult":
            case "div":
            case "mod":
            case "gre":
            case "lss":
            case "geq":
            case "leq":
            case "eql":
            case "neq":
            case "and":
            case "or":
            case "get":
            case "add": {
                arg1 = use(arg1, irBlock);
                arg2 = use(arg2, irBlock);
                arg3 = def(arg3, irBlock);
                break;
            }
            case "values": {
                arg1 = def(arg1, irBlock);
                if (arg2.startsWith("\"")) {
                    break;
                } else if (arg2.startsWith("{")) {
                    String n = arg2.substring(1, arg2.length() - 1);
                    if (n.isEmpty()) {
                        break;
                    }
                    String[] w = n.split(",");
                    StringBuilder sb = new StringBuilder();
                    sb.append("{");
                    for (String w1 : w) {
                        sb.append(w1);
                        sb.append(",");
                    }
                    sb.replace(sb.length() - 1, sb.length(), "}");
                    arg2 = sb.toString();
                } else {
                    arg2 = use(arg2, irBlock);
                }

                break;
            }
            case "bez":
            case "rpara":
            case "ret": {
                arg1 = use(arg1, irBlock);
                break;
            }
            case "getrt": {
                arg1 = def(arg1, irBlock);
                break;
            }
            case "put": {
                arg1 = def(arg1, irBlock);
                arg2 = use(arg2, irBlock);
                arg3 = use(arg3, irBlock);
                break;
            }
        }
    }

    private String use(String varIdent, IRBlock irBlock) {
        if (varIdent.contains("@") && !varIdent.startsWith("\"")) {
            return irBlock.useSSA(varIdent);
        }
        return varIdent;
    }

    private String def(String varIdent, IRBlock irBlock) {
        if (varIdent.contains("@") && !varIdent.startsWith("\"")) {
            return irBlock.defSSA(varIdent);
        }
        return varIdent;
    }

    public void changeJump(String dest, IRBlock irBlock) {
        if (op.equals("j")) {
            if (arg1.equals(irBlock.getBlockName())) {
                arg1 = dest;
            }
        } else if (op.equals("bez")) {
            if (arg2.equals(irBlock.getBlockName())) {
                arg2 = dest;
            }
        }
    }
}
