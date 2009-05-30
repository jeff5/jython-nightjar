// Autogenerated AST node
package org.python.antlr.ast;
import org.antlr.runtime.CommonToken;
import org.antlr.runtime.Token;
import org.python.antlr.AST;
import org.python.antlr.PythonTree;
import org.python.antlr.adapter.AstAdapters;
import org.python.antlr.base.excepthandler;
import org.python.antlr.base.expr;
import org.python.antlr.base.mod;
import org.python.antlr.base.slice;
import org.python.antlr.base.stmt;
import org.python.core.ArgParser;
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

@ExposedType(name = "_ast.arguments", base = AST.class)
public class arguments extends PythonTree {
    public static final PyType TYPE = PyType.fromClass(arguments.class);
    private java.util.List<expr> args;
    public java.util.List<expr> getInternalArgs() {
        return args;
    }
    @ExposedGet(name = "args")
    public PyObject getArgs() {
        return new AstList(args, AstAdapters.exprAdapter);
    }
    @ExposedSet(name = "args")
    public void setArgs(PyObject args) {
        this.args = AstAdapters.py2exprList(args);
    }

    private String vararg;
    public String getInternalVararg() {
        return vararg;
    }
    @ExposedGet(name = "vararg")
    public PyObject getVararg() {
        if (vararg == null) return Py.None;
        return new PyString(vararg);
    }
    @ExposedSet(name = "vararg")
    public void setVararg(PyObject vararg) {
        this.vararg = AstAdapters.py2identifier(vararg);
    }

    private String kwarg;
    public String getInternalKwarg() {
        return kwarg;
    }
    @ExposedGet(name = "kwarg")
    public PyObject getKwarg() {
        if (kwarg == null) return Py.None;
        return new PyString(kwarg);
    }
    @ExposedSet(name = "kwarg")
    public void setKwarg(PyObject kwarg) {
        this.kwarg = AstAdapters.py2identifier(kwarg);
    }

    private java.util.List<expr> defaults;
    public java.util.List<expr> getInternalDefaults() {
        return defaults;
    }
    @ExposedGet(name = "defaults")
    public PyObject getDefaults() {
        return new AstList(defaults, AstAdapters.exprAdapter);
    }
    @ExposedSet(name = "defaults")
    public void setDefaults(PyObject defaults) {
        this.defaults = AstAdapters.py2exprList(defaults);
    }


    private final static PyString[] fields =
    new PyString[] {new PyString("args"), new PyString("vararg"), new PyString("kwarg"), new
                     PyString("defaults")};
    @ExposedGet(name = "_fields")
    public PyString[] get_fields() { return fields; }

    private final static PyString[] attributes = new PyString[0];
    @ExposedGet(name = "_attributes")
    public PyString[] get_attributes() { return attributes; }

    public arguments(PyType subType) {
        super(subType);
    }
    public arguments() {
        this(TYPE);
    }
    @ExposedNew
    @ExposedMethod
    public void arguments___init__(PyObject[] args, String[] keywords) {
        ArgParser ap = new ArgParser("arguments", args, keywords, new String[]
            {"args", "vararg", "kwarg", "defaults"}, 4);
        setArgs(ap.getPyObject(0));
        setVararg(ap.getPyObject(1));
        setKwarg(ap.getPyObject(2));
        setDefaults(ap.getPyObject(3));
    }

    public arguments(PyObject args, PyObject vararg, PyObject kwarg, PyObject defaults) {
        setArgs(args);
        setVararg(vararg);
        setKwarg(kwarg);
        setDefaults(defaults);
    }

    public arguments(Token token, java.util.List<expr> args, String vararg, String kwarg,
    java.util.List<expr> defaults) {
        super(token);
        this.args = args;
        if (args == null) {
            this.args = new ArrayList<expr>();
        }
        for(PythonTree t : this.args) {
            addChild(t);
        }
        this.vararg = vararg;
        this.kwarg = kwarg;
        this.defaults = defaults;
        if (defaults == null) {
            this.defaults = new ArrayList<expr>();
        }
        for(PythonTree t : this.defaults) {
            addChild(t);
        }
    }

    public arguments(Integer ttype, Token token, java.util.List<expr> args, String vararg, String
    kwarg, java.util.List<expr> defaults) {
        super(ttype, token);
        this.args = args;
        if (args == null) {
            this.args = new ArrayList<expr>();
        }
        for(PythonTree t : this.args) {
            addChild(t);
        }
        this.vararg = vararg;
        this.kwarg = kwarg;
        this.defaults = defaults;
        if (defaults == null) {
            this.defaults = new ArrayList<expr>();
        }
        for(PythonTree t : this.defaults) {
            addChild(t);
        }
    }

    public arguments(PythonTree tree, java.util.List<expr> args, String vararg, String kwarg,
    java.util.List<expr> defaults) {
        super(tree);
        this.args = args;
        if (args == null) {
            this.args = new ArrayList<expr>();
        }
        for(PythonTree t : this.args) {
            addChild(t);
        }
        this.vararg = vararg;
        this.kwarg = kwarg;
        this.defaults = defaults;
        if (defaults == null) {
            this.defaults = new ArrayList<expr>();
        }
        for(PythonTree t : this.defaults) {
            addChild(t);
        }
    }

    @ExposedGet(name = "repr")
    public String toString() {
        return "arguments";
    }

    public String toStringTree() {
        StringBuffer sb = new StringBuffer("arguments(");
        sb.append("args=");
        sb.append(dumpThis(args));
        sb.append(",");
        sb.append("vararg=");
        sb.append(dumpThis(vararg));
        sb.append(",");
        sb.append("kwarg=");
        sb.append(dumpThis(kwarg));
        sb.append(",");
        sb.append("defaults=");
        sb.append(dumpThis(defaults));
        sb.append(",");
        sb.append(")");
        return sb.toString();
    }

    public <R> R accept(VisitorIF<R> visitor) throws Exception {
        traverse(visitor);
        return null;
    }

    public void traverse(VisitorIF<?> visitor) throws Exception {
        if (args != null) {
            for (PythonTree t : args) {
                if (t != null)
                    t.accept(visitor);
            }
        }
        if (defaults != null) {
            for (PythonTree t : defaults) {
                if (t != null)
                    t.accept(visitor);
            }
        }
    }

}
