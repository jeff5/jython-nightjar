package org.python.expose;

import org.python.core.Py;
import org.python.core.PyObject;

@Exposed(name = "simpleexposed")
public class SimpleExposed extends PyObject {

    public void method() {}

    public int timesCalled;

    @Exposed
    public void simple_method() {
        timesCalled++;
    }

    @Exposed
    public void simpleexposed_prefixed() {}

    @Exposed
    public boolean __nonzero__() {
        return false;
    }

    @Exposed(name = "__repr__")
    public String toString() {
        return TO_STRING_RETURN;
    }
    
    @Exposed
    public void takesArgument(PyObject arg) {
        assert arg == Py.None;
    }
    
    @Exposed(type=MethodType.BINARY)
    public PyObject __add__(PyObject arg) {
        if(arg == Py.False) {
            return Py.One;
        }
        return null;
    }
    
    public static final String TO_STRING_RETURN = "A simple test class";
}