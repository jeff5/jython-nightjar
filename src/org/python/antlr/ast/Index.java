// Autogenerated AST node
package org.python.antlr.ast;

import org.antlr.runtime.Token;
import org.python.antlr.PythonTree;
import org.python.antlr.adapter.AstAdapters;
import org.python.antlr.base.expr;
import org.python.antlr.base.slice;
import org.python.core.ArgParser;
import org.python.core.Py;
import org.python.core.PyObject;
import org.python.core.PyString;
import org.python.core.PyStringMap;
import org.python.core.PyType;
import org.python.expose.ExposedGet;
import org.python.expose.ExposedMethod;
import org.python.expose.ExposedNew;
import org.python.expose.ExposedSet;
import org.python.expose.ExposedType;

@ExposedType(name = "_ast.Index", base = slice.class)
public class Index extends slice {
public static final PyType TYPE = PyType.fromClass(Index.class);
    private expr value;
    public expr getInternalValue() {
        return value;
    }
    @ExposedGet(name = "value")
    public PyObject getValue() {
        return value;
    }
    @ExposedSet(name = "value")
    public void setValue(PyObject value) {
        this.value = AstAdapters.py2expr(value);
    }


    private final static PyString[] fields =
    new PyString[] {new PyString("value")};
    @ExposedGet(name = "_fields")
    public PyString[] get_fields() { return fields; }

    private final static PyString[] attributes = new PyString[0];
    @ExposedGet(name = "_attributes")
    public PyString[] get_attributes() { return attributes; }

    public Index(PyType subType) {
        super(subType);
    }
    public Index() {
        this(TYPE);
    }
    @ExposedNew
    @ExposedMethod
    public void Index___init__(PyObject[] args, String[] keywords) {
        ArgParser ap = new ArgParser("Index", args, keywords, new String[]
            {"value"}, 1, true);
        setValue(ap.getPyObject(0, Py.None));
    }

    public Index(PyObject value) {
        setValue(value);
    }

    public Index(Token token, expr value) {
        super(token);
        this.value = value;
        addChild(value);
    }

    public Index(Integer ttype, Token token, expr value) {
        super(ttype, token);
        this.value = value;
        addChild(value);
    }

    public Index(PythonTree tree, expr value) {
        super(tree);
        this.value = value;
        addChild(value);
    }

    @ExposedGet(name = "repr")
    public String toString() {
        return "Index";
    }

    public String toStringTree() {
        StringBuffer sb = new StringBuffer("Index(");
        sb.append("value=");
        sb.append(dumpThis(value));
        sb.append(",");
        sb.append(")");
        return sb.toString();
    }

    public <R> R accept(VisitorIF<R> visitor) throws Exception {
        return visitor.visitIndex(this);
    }

    public void traverse(VisitorIF<?> visitor) throws Exception {
        if (value != null)
            value.accept(visitor);
    }

    public PyObject __dict__;

    @Override
    public PyObject fastGetDict() {
        ensureDict();
        return __dict__;
    }

    @ExposedGet(name = "__dict__")
    public PyObject getDict() {
        return fastGetDict();
    }

    private void ensureDict() {
        if (__dict__ == null) {
            __dict__ = new PyStringMap();
        }
    }

}
