// Autogenerated AST node
package org.python.antlr.ast;
import org.python.antlr.PythonTree;
import org.antlr.runtime.Token;
import java.io.DataOutputStream;
import java.io.IOException;

public class ImportFrom extends stmtType {
    public String module;
    public aliasType[] names;
    public int level;

    public static final String[] _fields = new String[]
    {"module","names","level"};

    public ImportFrom(PythonTree tree, String module, aliasType[] names, int
    level) {
        super(tree);
        this.module = module;
        this.names = names;
        if (names != null) {
            for(int inames=0;inames<names.length;inames++) {
                addChild(names[inames]);
            }
        }
        this.level = level;
    }

    public String toString() {
        return "ImportFrom";
    }

    public <R> R accept(VisitorIF<R> visitor) throws Exception {
        return visitor.visitImportFrom(this);
    }

    public void traverse(VisitorIF visitor) throws Exception {
        if (names != null) {
            for (int i = 0; i < names.length; i++) {
                if (names[i] != null)
                    names[i].accept(visitor);
            }
        }
    }

    public int getLineno() {
        return getLine();
    }

    public int getCol_offset() {
        return getCharPositionInLine();
    }

}
