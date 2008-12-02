// Autogenerated AST node
package org.python.antlr.ast;
import org.antlr.runtime.CommonToken;
import org.antlr.runtime.Token;
import org.python.antlr.PythonTree;
import org.python.antlr.adapter.AstAdapters;
import org.python.core.AstList;
import org.python.core.Py;
import org.python.core.PyObject;
import org.python.core.PyString;
import org.python.core.PyType;
import org.python.expose.ExposedGet;
import org.python.expose.ExposedMethod;
import org.python.expose.ExposedNew;
import org.python.expose.ExposedSet;
import org.python.expose.ExposedType;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;

@ExposedType(name = "_ast.BinOp", base = PyObject.class)
public class BinOp extends exprType {
public static final PyType TYPE = PyType.fromClass(BinOp.class);
    private exprType left;
    public exprType getInternalLeft() {
        return left;
    }
    @ExposedGet(name = "left")
    public PyObject getLeft() {
        return left;
    }
    @ExposedSet(name = "left")
    public void setLeft(PyObject left) {
        this.left = AstAdapters.py2expr(left);
    }

    private operatorType op;
    public operatorType getInternalOp() {
        return op;
    }
    @ExposedGet(name = "op")
    public PyObject getOp() {
        return AstAdapters.op2py(op);
    }
    @ExposedSet(name = "op")
    public void setOp(PyObject op) {
        this.op = AstAdapters.py2operator(op);
    }

    private exprType right;
    public exprType getInternalRight() {
        return right;
    }
    @ExposedGet(name = "right")
    public PyObject getRight() {
        return right;
    }
    @ExposedSet(name = "right")
    public void setRight(PyObject right) {
        this.right = AstAdapters.py2expr(right);
    }


    private final static String[] fields = new String[] {"left", "op", "right"};
@ExposedGet(name = "_fields")
    public String[] get_fields() { return fields; }

    public BinOp() {
        this(TYPE);
    }
    public BinOp(PyType subType) {
        super(subType);
    }
    @ExposedNew
    @ExposedMethod
    public void Module___init__(PyObject[] args, String[] keywords) {}
    public BinOp(PyObject left, PyObject op, PyObject right) {
        setLeft(left);
        setOp(op);
        setRight(right);
    }

    public BinOp(Token token, exprType left, operatorType op, exprType right) {
        super(token);
        this.left = left;
        addChild(left);
        this.op = op;
        this.right = right;
        addChild(right);
    }

    public BinOp(Integer ttype, Token token, exprType left, operatorType op,
    exprType right) {
        super(ttype, token);
        this.left = left;
        addChild(left);
        this.op = op;
        this.right = right;
        addChild(right);
    }

    public BinOp(PythonTree tree, exprType left, operatorType op, exprType
    right) {
        super(tree);
        this.left = left;
        addChild(left);
        this.op = op;
        this.right = right;
        addChild(right);
    }

    @ExposedGet(name = "repr")
    public String toString() {
        return "BinOp";
    }

    public String toStringTree() {
        StringBuffer sb = new StringBuffer("BinOp(");
        sb.append("left=");
        sb.append(dumpThis(left));
        sb.append(",");
        sb.append("op=");
        sb.append(dumpThis(op));
        sb.append(",");
        sb.append("right=");
        sb.append(dumpThis(right));
        sb.append(",");
        sb.append(")");
        return sb.toString();
    }

    public <R> R accept(VisitorIF<R> visitor) throws Exception {
        return visitor.visitBinOp(this);
    }

    public void traverse(VisitorIF visitor) throws Exception {
        if (left != null)
            left.accept(visitor);
        if (right != null)
            right.accept(visitor);
    }

    private int lineno = -1;
@ExposedGet(name = "lineno")
    public int getLineno() {
        if (lineno != -1) {
            return lineno;
        }
        return getLine();
    }

@ExposedSet(name = "lineno")
    public void setLineno(int num) {
        lineno = num;
    }

    private int col_offset = -1;
@ExposedGet(name = "col_offset")
    public int getCol_offset() {
        if (col_offset != -1) {
            return col_offset;
        }
        return getCharPositionInLine();
    }

@ExposedSet(name = "col_offset")
    public void setCol_offset(int num) {
        col_offset = num;
    }

}
