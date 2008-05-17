// Autogenerated AST node
package org.python.antlr.ast;
import org.python.antlr.PythonTree;
import org.antlr.runtime.Token;
import java.io.DataOutputStream;
import java.io.IOException;

public class Dict extends exprType {
    public exprType[] keys;
    public exprType[] values;

    public static final String[] _fields = new String[] {"keys","values"};

    public Dict(PythonTree tree, exprType[] keys, exprType[] values) {
        super(tree);
        this.keys = keys;
        for(int ikeys=0;ikeys<keys.length;ikeys++) {
            addChild(keys[ikeys]);
        }
        this.values = values;
        for(int ivalues=0;ivalues<values.length;ivalues++) {
            addChild(values[ivalues]);
        }
    }

    public String toString() {
        return "Dict";
    }

    public <R> R accept(VisitorIF<R> visitor) throws Exception {
        return visitor.visitDict(this);
    }

    public void traverse(VisitorIF visitor) throws Exception {
        if (keys != null) {
            for (int i = 0; i < keys.length; i++) {
                if (keys[i] != null)
                    keys[i].accept(visitor);
            }
        }
        if (values != null) {
            for (int i = 0; i < values.length; i++) {
                if (values[i] != null)
                    values[i].accept(visitor);
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
