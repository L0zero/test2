package pt;

public interface pt {

    /**
     * 开始解析
     */
    public void parse();

    public void makeIR();

    /**
     * 按树状结构打印内容
     */
    public void print();

    /**
     * 按平面格式打印内容（忽略课程要求内容）
     */
    public void printAns();

    /**
     * 返回抽象语法树的根节点（CompUnit）
     * @return 根节点
     */
    public Symbol getSymbol();
}
