// Autogenerated AST node
package org.python.antlr.ast;
import org.python.antlr.PythonTree;
import org.antlr.runtime.CommonToken;
import org.antlr.runtime.Token;
import java.io.DataOutputStream;
import java.io.IOException;

public class Yield extends exprType {
    public exprType value;

    private final static String[] fields = new String[] {"value"};
    public String[] get_fields() { return fields; }

    public Yield(exprType value) {
        this.value = value;
        addChild(value);
    }

    public Yield(Token token, exprType value) {
        super(token);
        this.value = value;
        addChild(value);
    }

    public Yield(int ttype, Token token, exprType value) {
        super(ttype, token);
        this.value = value;
        addChild(value);
    }

    public Yield(PythonTree tree, exprType value) {
        super(tree);
        this.value = value;
        addChild(value);
    }

    public String toString() {
        return "Yield";
    }

    public String toStringTree() {
        StringBuffer sb = new StringBuffer("Yield(");
        sb.append("value=");
        sb.append(dumpThis(value));
        sb.append(",");
        sb.append(")");
        return sb.toString();
    }

    public <R> R accept(VisitorIF<R> visitor) throws Exception {
        return visitor.visitYield(this);
    }

    public void traverse(VisitorIF visitor) throws Exception {
        if (value != null)
            value.accept(visitor);
    }

    private int lineno = -1;
    public int getLineno() {
        if (lineno != -1) {
            return lineno;
        }
        return getLine();
    }

    public void setLineno(int num) {
        lineno = num;
    }

    private int col_offset = -1;
    public int getCol_offset() {
        if (col_offset != -1) {
            return col_offset;
        }
        return getCharPositionInLine();
    }

    public void setCol_offset(int num) {
        col_offset = num;
    }

}
