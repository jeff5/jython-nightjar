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

@ExposedType(name = "_ast.AugAssign", base = PyObject.class)
public class AugAssign extends stmtType {
public static final PyType TYPE = PyType.fromClass(AugAssign.class);
    private exprType target;
    public exprType getInternalTarget() {
        return target;
    }
    @ExposedGet(name = "target")
    public PyObject getTarget() {
        return target;
    }
    @ExposedSet(name = "target")
    public void setTarget(PyObject target) {
        this.target = AstAdapters.to_expr(target);
    }

    private operatorType op;
    public operatorType getInternalOp() {
        return op;
    }
    @ExposedGet(name = "op")
    public PyObject getOp() {
        return Py.None;//(op);
    }
    @ExposedSet(name = "op")
    public void setOp(PyObject op) {
        this.op = AstAdapters.to_operator(op);
    }

    private exprType value;
    public exprType getInternalValue() {
        return value;
    }
    @ExposedGet(name = "value")
    public PyObject getValue() {
        return value;
    }
    @ExposedSet(name = "value")
    public void setValue(PyObject value) {
        this.value = AstAdapters.to_expr(value);
    }


    private final static String[] fields = new String[] {"target", "op",
                                                          "value"};
@ExposedGet(name = "_fields")
    public String[] get_fields() { return fields; }

    public AugAssign() {
        this(TYPE);
    }
    public AugAssign(PyType subType) {
        super(subType);
    }
    @ExposedNew
    @ExposedMethod
    public void Module___init__(PyObject[] args, String[] keywords) {}
    public AugAssign(PyObject target, PyObject op, PyObject value) {
        setTarget(target);
        setOp(op);
        setValue(value);
    }

    public AugAssign(Token token, exprType target, operatorType op, exprType
    value) {
        super(token);
        this.target = target;
        addChild(target);
        this.op = op;
        this.value = value;
        addChild(value);
    }

    public AugAssign(Integer ttype, Token token, exprType target, operatorType
    op, exprType value) {
        super(ttype, token);
        this.target = target;
        addChild(target);
        this.op = op;
        this.value = value;
        addChild(value);
    }

    public AugAssign(PythonTree tree, exprType target, operatorType op,
    exprType value) {
        super(tree);
        this.target = target;
        addChild(target);
        this.op = op;
        this.value = value;
        addChild(value);
    }

    @ExposedGet(name = "repr")
    public String toString() {
        return "AugAssign";
    }

    public String toStringTree() {
        StringBuffer sb = new StringBuffer("AugAssign(");
        sb.append("target=");
        sb.append(dumpThis(target));
        sb.append(",");
        sb.append("op=");
        sb.append(dumpThis(op));
        sb.append(",");
        sb.append("value=");
        sb.append(dumpThis(value));
        sb.append(",");
        sb.append(")");
        return sb.toString();
    }

    public <R> R accept(VisitorIF<R> visitor) throws Exception {
        return visitor.visitAugAssign(this);
    }

    public void traverse(VisitorIF visitor) throws Exception {
        if (target != null)
            target.accept(visitor);
        if (value != null)
            value.accept(visitor);
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
