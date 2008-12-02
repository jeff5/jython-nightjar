// Autogenerated AST node
package org.python.antlr.ast;

import org.python.antlr.AST;
import org.python.core.PyObject;
import org.python.core.PyType;
import org.python.expose.ExposedType;

@ExposedType(name = "_ast.In", base = PyObject.class)
public class In extends AST {
    public static final PyType TYPE = PyType.fromClass(In.class);
    public int asIndex() {
        return asIndex(null);
    }
    public int asIndex(PyObject error) {
        return 8;
    }
}
