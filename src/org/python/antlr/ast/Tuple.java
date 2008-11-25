// Autogenerated AST node
package org.python.antlr.ast;
import java.util.ArrayList;
import org.python.antlr.PythonTree;
import org.python.antlr.adapter.AstAdapters;
import org.python.antlr.adapter.ListWrapper;
import org.antlr.runtime.CommonToken;
import org.antlr.runtime.Token;
import java.io.DataOutputStream;
import java.io.IOException;

public class Tuple extends exprType implements Context {
    private java.util.List<exprType> elts;
    public java.util.List<exprType> getInternalElts() {
        return elts;
    }
    public Object getElts() {
        return new ListWrapper(elts);
    }
    public void setElts(Object elts) {
        this.elts = AstAdapters.to_exprList(elts);
    }

    private expr_contextType ctx;
    public expr_contextType getInternalCtx() {
        return ctx;
    }
    public Object getCtx() {
        return ctx;
    }
    public void setCtx(Object ctx) {
        this.ctx = AstAdapters.to_expr_context(ctx);
    }


    private final static String[] fields = new String[] {"elts", "ctx"};
    public String[] get_fields() { return fields; }

    public Tuple() {}
    public Tuple(Object elts, Object ctx) {
        setElts(elts);
        setCtx(ctx);
    }

    public Tuple(Token token, java.util.List<exprType> elts, expr_contextType
    ctx) {
        super(token);
        this.elts = elts;
        if (elts == null) {
            this.elts = new ArrayList<exprType>();
        }
        for(PythonTree t : this.elts) {
            addChild(t);
        }
        this.ctx = ctx;
    }

    public Tuple(Integer ttype, Token token, java.util.List<exprType> elts,
    expr_contextType ctx) {
        super(ttype, token);
        this.elts = elts;
        if (elts == null) {
            this.elts = new ArrayList<exprType>();
        }
        for(PythonTree t : this.elts) {
            addChild(t);
        }
        this.ctx = ctx;
    }

    public Tuple(PythonTree tree, java.util.List<exprType> elts,
    expr_contextType ctx) {
        super(tree);
        this.elts = elts;
        if (elts == null) {
            this.elts = new ArrayList<exprType>();
        }
        for(PythonTree t : this.elts) {
            addChild(t);
        }
        this.ctx = ctx;
    }

    public String toString() {
        return "Tuple";
    }

    public String toStringTree() {
        StringBuffer sb = new StringBuffer("Tuple(");
        sb.append("elts=");
        sb.append(dumpThis(elts));
        sb.append(",");
        sb.append("ctx=");
        sb.append(dumpThis(ctx));
        sb.append(",");
        sb.append(")");
        return sb.toString();
    }

    public <R> R accept(VisitorIF<R> visitor) throws Exception {
        return visitor.visitTuple(this);
    }

    public void traverse(VisitorIF visitor) throws Exception {
        if (elts != null) {
            for (PythonTree t : elts) {
                if (t != null)
                    t.accept(visitor);
            }
        }
    }

    public void setContext(expr_contextType c) {
        this.ctx = c;
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
